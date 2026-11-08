package com.discordnotifications;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.DrawManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

/**
 * Owns all outbound traffic for the plugin. Everything else builds message text and hands it here,
 * so the Discord webhook call exists in exactly one place.
 */
@Slf4j
@Singleton
public class DiscordWebhookService
{
	static final int MAX_CONTENT_LENGTH = 2_000;
	private static final String ELLIPSIS = "\u2026";
	private static final long CAPTURE_TIMEOUT_MS = 2_000;
	static final int MAX_IMAGE_BYTES = 8_000_000;
	private static final float JPEG_QUALITY = 0.85f;
	private static final MediaType PNG = MediaType.parse("image/png");
	private static final MediaType JPEG = MediaType.parse("image/jpeg");
	private static final int MAX_RETRIES = 3;
	private static final long RETRY_BASE_DELAY_MS = 2_000;
	private static final long MAX_RETRY_DELAY_MS = 60_000;
	private static final long TIMEOUT_SECONDS = 15;
	private static final long IMAGE_WRITE_TIMEOUT_SECONDS = 30;
	// build.gradle doesn't ship, so the real plugin version isn't available at runtime
	private static final String USER_AGENT = RuneLite.USER_AGENT + " (DiscordNotifications/1.x)";

	private OkHttpClient httpClient;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private DiscordNotificationsConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Client client;

	private NotificationQueue queue;

	@Inject
	private void configureHttpClient(OkHttpClient okHttpClient)
	{
		httpClient = okHttpClient.newBuilder()
			.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build();
	}

	@Inject
	private void connectQueue(NotificationQueue queue)
	{
		this.queue = queue;
		queue.connect(this::captureScreenshot, this::postToAll);
	}

	public void send(Notification notification)
	{
		List<String> urls = resolveWebhookUrls(overrideFor(notification.getType()), config.webhook()).stream()
			.map(HttpUrl::toString)
			.collect(Collectors.toList());
		if (urls.isEmpty())
		{
			return;
		}

		queue.add(urls, notification);
	}

	private String overrideFor(NotificationType type)
	{
		switch (type)
		{
			case PET:
				return config.petWebhook();
			case COLLECTION_LOG:
				return config.collectionLogWebhook();
			case LOOT:
				return config.lootWebhook();
			case COMBAT_ACHIEVEMENT:
				return config.combatAchievementWebhook();
			case CLUE:
				return config.clueWebhook();
			case QUEST:
				return config.questWebhook();
			case LEVEL:
				return config.levelWebhook();
			case DEATH:
				return config.deathWebhook();
			default:
				return null;
		}
	}

	// An override with no usable URL in it counts as blank, a typo shouldn't lose the message
	static List<HttpUrl> resolveWebhookUrls(String override, String primary)
	{
		List<HttpUrl> urls = parseWebhookUrls(override);
		return urls.isEmpty() ? parseWebhookUrls(primary) : urls;
	}

	static List<HttpUrl> parseWebhookUrls(String configured)
	{
		if (Strings.isNullOrEmpty(configured))
		{
			return Collections.emptyList();
		}

		return Arrays.stream(configured.split("[\n,;]"))
			.map(String::trim)
			.filter(u -> !u.isEmpty())
			.map(HttpUrl::parse)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private void captureScreenshot(Consumer<byte[]> onImage)
	{
		AtomicBoolean delivered = new AtomicBoolean();
		// Client thread only
		List<Integer> hidden = new ArrayList<>();

		// DrawManager drops its listeners without a word when it can't draw (minimised, loading screen)
		executor.schedule(() ->
		{
			if (delivered.compareAndSet(false, true))
			{
				log.debug("No frame within {}ms, sending without a screenshot", CAPTURE_TIMEOUT_MS);
				clientThread.invoke(() -> unhideWidgets(hidden));
				onImage.accept(null);
			}
		}, CAPTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

		// Tick end so the popup or chat line that caused this is on screen
		clientThread.invokeAtTickEnd(() ->
		{
			// After the timeout nothing would unhide these
			if (delivered.get())
			{
				return;
			}

			hideChat(hidden);
			drawManager.requestNextFrameListener(image ->
			{
				if (!delivered.compareAndSet(false, true))
				{
					return;
				}
				clientThread.invoke(() -> unhideWidgets(hidden));
				executor.execute(() -> onImage.accept(encodeScreenshot(image)));
			});
		});
	}

	private void hideChat(List<Integer> hidden)
	{
		ChatPrivacy privacy = config.chatPrivacy();
		if (privacy != ChatPrivacy.SHOW_ALL)
		{
			hideWidget(InterfaceID.PmChat.CONTAINER, hidden);
		}
		if (privacy == ChatPrivacy.HIDE_ALL)
		{
			hideWidget(InterfaceID.Chatbox.CHATAREA, hidden);
		}
	}

	private void hideWidget(int component, List<Integer> hidden)
	{
		Widget widget = client.getWidget(component);
		if (widget != null && !widget.isHidden())
		{
			widget.setHidden(true);
			hidden.add(component);
		}
	}

	private void unhideWidgets(List<Integer> hidden)
	{
		for (int component : hidden)
		{
			Widget widget = client.getWidget(component);
			if (widget != null)
			{
				widget.setHidden(false);
			}
		}
	}

	private static byte[] encodeScreenshot(Image image)
	{
		if (!(image instanceof BufferedImage))
		{
			return null;
		}

		try
		{
			return encodeUnderLimit((BufferedImage) image);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Error converting image to byte array", e);
			return null;
		}
	}

	static byte[] encodeUnderLimit(BufferedImage image) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		byte[] png = out.toByteArray();
		if (png.length <= MAX_IMAGE_BYTES)
		{
			return png;
		}

		byte[] jpeg = encodeJpeg(scaleToRgb(image, Math.sqrt((double) MAX_IMAGE_BYTES / png.length)));
		if (jpeg.length > MAX_IMAGE_BYTES)
		{
			// Discord would reject the whole post, the text is still worth sending
			log.warn("Screenshot still {} bytes after scaling, sending without it", jpeg.length);
			return null;
		}
		return jpeg;
	}

	private static BufferedImage scaleToRgb(BufferedImage image, double factor)
	{
		int width = Math.max(1, (int) (image.getWidth() * factor));
		int height = Math.max(1, (int) (image.getHeight() * factor));

		// The JPEG writer writes nothing at all for an image with alpha
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(image, 0, 0, width, height, null);
		graphics.dispose();
		return scaled;
	}

	private static byte[] encodeJpeg(BufferedImage image) throws IOException
	{
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageOutputStream stream = ImageIO.createImageOutputStream(out))
		{
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
			writer.setOutput(stream);
			writer.write(null, new IIOImage(image, null, null), param);
			stream.flush();
			return out.toByteArray();
		}
		finally
		{
			writer.dispose();
		}
	}

	// The queue only carries bytes. PNG starts with 0x89, JPEG with FF D8
	static boolean isJpeg(byte[] image)
	{
		return image != null && image.length >= 2 && image[0] == (byte) 0xFF && image[1] == (byte) 0xD8;
	}

	static String truncate(String text)
	{
		if (text.length() <= MAX_CONTENT_LENGTH)
		{
			return text;
		}

		// Newlines count too, a batched post has one event per line
		int limit = MAX_CONTENT_LENGTH - ELLIPSIS.length();
		int cut = Math.max(text.lastIndexOf(' ', limit), text.lastIndexOf('\n', limit));
		return text.substring(0, cut > 0 ? cut : limit) + ELLIPSIS;
	}

	private void postToAll(List<String> urls, String text, byte[] image)
	{
		DiscordWebhookBody body = new DiscordWebhookBody();
		body.setContent(truncate(text));
		String payload = GSON.toJson(body);

		boolean jpeg = isJpeg(image);
		RequestBody imageBody = image == null ? null : RequestBody.create(jpeg ? JPEG : PNG, image);
		OkHttpClient client = imageBody == null ? httpClient : httpClient.newBuilder()
			.writeTimeout(IMAGE_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build();

		for (String url : urls)
		{
			MultipartBody.Builder builder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", payload);

			if (imageBody != null)
			{
				builder.addFormDataPart("file", jpeg ? "image.jpg" : "image.png", imageBody);
			}

			Request request = new Request.Builder()
				.url(url)
				.header("User-Agent", USER_AGENT)
				.post(builder.build())
				.build();

			sendRequest(client, request, 0);
		}
	}

	private void sendRequest(OkHttpClient client, Request request, int attempt)
	{
		client.newCall(request).enqueue(
			new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e)
				{
					retryOrGiveUp(client, request, attempt, e.toString(), null);
				}

				@Override
				public void onResponse(Call call, Response response)
				{
					response.close();
					int code = response.code();
					if (response.isSuccessful())
					{
						return;
					}

					if (code >= 400 && code < 500 && code != 429)
					{
						log.warn("Webhook rejected: HTTP {}", code);
						return;
					}

					retryOrGiveUp(client, request, attempt, "HTTP " + code, response.header("Retry-After"));
				}
			}
		);
	}

	private void retryOrGiveUp(OkHttpClient client, Request request, int attempt, String reason, String retryAfter)
	{
		long delay = retryDelayMs(retryAfter, attempt);

		// Don't log the request, the webhook URL is a secret
		if (attempt >= MAX_RETRIES || delay > MAX_RETRY_DELAY_MS)
		{
			log.warn("Webhook failed after {} retries: {}", attempt, reason);
			return;
		}

		log.debug("Webhook failed, retrying in {}ms: {}", delay, reason);
		executor.schedule(() -> sendRequest(client, request, attempt + 1), delay, TimeUnit.MILLISECONDS);
	}

	// Seconds only, an HTTP-date falls back to the normal backoff
	static long retryDelayMs(String retryAfter, int attempt)
	{
		long backoff = RETRY_BASE_DELAY_MS << attempt;
		if (retryAfter == null)
		{
			return backoff;
		}

		try
		{
			double seconds = Double.parseDouble(retryAfter.trim());
			return seconds >= 0 ? (long) Math.ceil(seconds * 1000) : backoff;
		}
		catch (NumberFormatException e)
		{
			return backoff;
		}
	}
}
