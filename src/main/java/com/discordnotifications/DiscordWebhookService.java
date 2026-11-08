package com.discordnotifications;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
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

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Image;
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
	private static final long CAPTURE_TIMEOUT_MS = 2_000;
	private static final MediaType PNG = MediaType.parse("image/png");

	@Inject
	private OkHttpClient okHttpClient;

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
	private void connectQueue(NotificationQueue queue)
	{
		this.queue = queue;
		queue.connect(this::captureScreenshot, this::postToAll);
	}

	public void send(Notification notification)
	{
		List<String> urls = parseWebhookUrls(config.webhook()).stream()
			.map(HttpUrl::toString)
			.collect(Collectors.toList());
		if (urls.isEmpty())
		{
			return;
		}

		queue.add(urls, notification);
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
				executor.execute(() -> onImage.accept(encodePng(image)));
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

	private static byte[] encodePng(Image image)
	{
		if (!(image instanceof BufferedImage))
		{
			return null;
		}

		try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write((BufferedImage) image, "png", out);
			return out.toByteArray();
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Error converting image to byte array", e);
			return null;
		}
	}

	private void postToAll(List<String> urls, String text, byte[] image)
	{
		DiscordWebhookBody body = new DiscordWebhookBody();
		body.setContent(text);
		String payload = GSON.toJson(body);

		RequestBody imageBody = image == null ? null : RequestBody.create(PNG, image);

		for (String url : urls)
		{
			MultipartBody.Builder builder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", payload);

			if (imageBody != null)
			{
				builder.addFormDataPart("file", "image.png", imageBody);
			}

			Request request = new Request.Builder()
				.url(url)
				.post(builder.build())
				.build();

			sendRequest(request);
		}
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(
			new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e)
				{
					log.debug("Error submitting webhook", e);
				}

				@Override
				public void onResponse(Call call, Response response)
				{
					response.close();
				}
			}
		);
	}
}
