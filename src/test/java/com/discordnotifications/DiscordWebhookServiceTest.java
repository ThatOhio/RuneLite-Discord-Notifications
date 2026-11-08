package com.discordnotifications;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import okhttp3.HttpUrl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscordWebhookServiceTest
{
	private static List<String> parse(String configured)
	{
		return DiscordWebhookService.parseWebhookUrls(configured).stream()
			.map(HttpUrl::toString)
			.collect(Collectors.toList());
	}

	@Test
	public void emptyConfigGivesNoUrls()
	{
		assertTrue(parse(null).isEmpty());
		assertTrue(parse("").isEmpty());
		assertTrue(parse(" \n , ; \n").isEmpty());
	}

	@Test
	public void splitsOnNewlineCommaAndSemicolon()
	{
		assertEquals(
			List.of("https://a.example/1", "https://a.example/2", "https://a.example/3", "https://a.example/4"),
			parse("https://a.example/1, https://a.example/2\nhttps://a.example/3;\n  https://a.example/4  "));
	}

	@Test
	public void dropsEntriesThatAreNotUrls()
	{
		assertEquals(
			List.of("https://discord.com/api/webhooks/1/abc"),
			parse("not a url\nhttps://discord.com/api/webhooks/1/abc\nftp://nope.example/x"));
	}

	@Test
	public void smallScreenshotStaysPng() throws IOException
	{
		byte[] encoded = DiscordWebhookService.encodeUnderLimit(new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB));

		assertFalse(DiscordWebhookService.isJpeg(encoded));
		assertEquals(100, ImageIO.read(new ByteArrayInputStream(encoded)).getWidth());
	}

	@Test
	public void oversizedScreenshotBecomesScaledJpeg() throws IOException
	{
		// Noise doesn't compress, 2000x2000 is about 12 MB as PNG
		BufferedImage image = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
		Random random = new Random(1);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				image.setRGB(x, y, 0xFF000000 | random.nextInt(0x1000000));
			}
		}

		byte[] encoded = DiscordWebhookService.encodeUnderLimit(image);

		assertTrue(DiscordWebhookService.isJpeg(encoded));
		assertTrue(encoded.length <= DiscordWebhookService.MAX_IMAGE_BYTES);
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(encoded));
		assertTrue(decoded.getWidth() < 2000);
		assertEquals(decoded.getWidth(), decoded.getHeight());
	}

	@Test
	public void missingImageIsNotJpeg()
	{
		assertFalse(DiscordWebhookService.isJpeg(null));
		assertFalse(DiscordWebhookService.isJpeg(new byte[0]));
	}
}
