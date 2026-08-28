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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

/**
 * Owns all outbound traffic for the plugin. Everything else builds message text and hands it here,
 * so the Discord webhook call exists in exactly one place.
 */
@Slf4j
@Singleton
class DiscordWebhookService
{
	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private DrawManager drawManager;

	@Inject
	private DiscordNotificationsConfig config;

	void sendMessage(String message, boolean sendScreenshot)
	{
		DiscordWebhookBody body = new DiscordWebhookBody();
		body.setContent(message);
		send(body, sendScreenshot);
	}

	private void send(DiscordWebhookBody discordWebhookBody, boolean sendScreenshot)
	{
		for (String webhookUrl : configuredWebhookUrls())
		{
			HttpUrl url = HttpUrl.parse(webhookUrl);
			MultipartBody.Builder requestBodyBuilder =
				new MultipartBody
						.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("payload_json", GSON.toJson(discordWebhookBody));

			if (sendScreenshot)
			{
				sendWithScreenshot(url, requestBodyBuilder);
			}
			else
			{
				buildRequestAndSend(url, requestBodyBuilder);
			}
		}
	}

	private List<String> configuredWebhookUrls()
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return Collections.emptyList();
		}

		return Arrays.stream(configUrl.split("\n"))
			.filter(u -> u.length() > 0)
			.map(String::trim)
			.collect(Collectors.toList());
	}

	private void sendWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		drawManager.requestNextFrameListener(
			image ->
			{
				BufferedImage bufferedImage = (BufferedImage) image;
				byte[] imageBytes;
				try
				{
					imageBytes = convertImageToByteArray(bufferedImage);
				}
				catch (IOException e)
				{
					log.warn("Error converting image to byte array", e);
					return;
				}

				requestBodyBuilder.addFormDataPart(
					"file",
					"image.png",
					RequestBody.create(
						MediaType.parse("image/png"),
						imageBytes
					)
				);

				buildRequestAndSend(url, requestBodyBuilder);
			}
		);
	}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();

		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.build();

		sendRequest(request);
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
				public void onResponse(Call call, Response response) throws IOException
				{
					response.close();
				}
			}
		);
	}

	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}
}
