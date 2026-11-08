package com.discordnotifications;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
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

	public void send(Notification notification)
	{
		List<HttpUrl> urls = parseWebhookUrls(config.webhook());
		if (urls.isEmpty())
		{
			return;
		}

		DiscordWebhookBody body = new DiscordWebhookBody();
		body.setContent(notification.getText());
		String payload = GSON.toJson(body);

		if (notification.isScreenshot())
		{
			captureScreenshot(image -> postToAll(urls, payload, image));
		}
		else
		{
			postToAll(urls, payload, null);
		}
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

		// DrawManager drops its listeners without a word when it can't draw (minimised, loading screen)
		executor.schedule(() ->
		{
			if (delivered.compareAndSet(false, true))
			{
				log.debug("No frame within {}ms, sending without a screenshot", CAPTURE_TIMEOUT_MS);
				onImage.accept(null);
			}
		}, CAPTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

		drawManager.requestNextFrameListener(image ->
		{
			if (!delivered.compareAndSet(false, true))
			{
				return;
			}
			executor.execute(() -> onImage.accept(encodePng(image)));
		});
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
		catch (IOException e)
		{
			log.warn("Error converting image to byte array", e);
			return null;
		}
	}

	private void postToAll(List<HttpUrl> urls, String payload, byte[] image)
	{
		RequestBody imageBody = image == null ? null : RequestBody.create(PNG, image);

		for (HttpUrl url : urls)
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
