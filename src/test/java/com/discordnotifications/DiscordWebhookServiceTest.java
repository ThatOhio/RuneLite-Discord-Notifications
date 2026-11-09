package com.discordnotifications;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscordWebhookServiceTest
{
	private static List<String> parse(String configured)
	{
		return DiscordWebhookService.parseWebhookUrls(configured);
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

	// secret config fields are single line, Swing turns saved newlines into spaces
	@Test
	public void splitsOnSpaces()
	{
		assertEquals(
			List.of("https://a.example/1", "https://a.example/2"),
			parse("https://a.example/1 https://a.example/2"));
	}

	@Test
	public void dropsEntriesThatAreNotUrls()
	{
		assertEquals(
			List.of("https://discord.com/api/webhooks/1/abc"),
			parse("not a url\nhttps://discord.com/api/webhooks/1/abc\nftp://nope.example/x"));
	}

	private static List<String> resolve(String override, String primary)
	{
		return DiscordWebhookService.resolveWebhookUrls(override, primary);
	}

	@Test
	public void overrideReplacesPrimary()
	{
		assertEquals(
			List.of("https://a.example/deaths", "https://a.example/deaths2"),
			resolve("https://a.example/deaths\nhttps://a.example/deaths2", "https://a.example/main"));
	}

	@Test
	public void blankOverrideUsesPrimary()
	{
		assertEquals(List.of("https://a.example/main"), resolve(null, "https://a.example/main"));
		assertEquals(List.of("https://a.example/main"), resolve("", "https://a.example/main"));
		assertEquals(List.of("https://a.example/main"), resolve(" \n ", "https://a.example/main"));
	}

	@Test
	public void overrideWithNoValidUrlUsesPrimary()
	{
		assertEquals(List.of("https://a.example/main"), resolve("not a url", "https://a.example/main"));
	}

	@Test
	public void overrideWorksWithNoPrimary()
	{
		assertEquals(List.of("https://a.example/deaths"), resolve("https://a.example/deaths", null));
	}

	@Test
	public void backoffDoublesWithoutRetryAfter()
	{
		assertEquals(2_000, DiscordWebhookService.retryDelayMs(null, 0));
		assertEquals(4_000, DiscordWebhookService.retryDelayMs(null, 1));
		assertEquals(8_000, DiscordWebhookService.retryDelayMs(null, 2));
	}

	@Test
	public void retryAfterSecondsReplaceBackoff()
	{
		assertEquals(5_000, DiscordWebhookService.retryDelayMs("5", 0));
		assertEquals(1_250, DiscordWebhookService.retryDelayMs(" 1.25 ", 2));
		assertEquals(0, DiscordWebhookService.retryDelayMs("0", 1));
	}

	@Test
	public void unusableRetryAfterFallsBackToBackoff()
	{
		assertEquals(2_000, DiscordWebhookService.retryDelayMs("Wed, 21 Oct 2026 07:28:00 GMT", 0));
		assertEquals(4_000, DiscordWebhookService.retryDelayMs("", 1));
		assertEquals(4_000, DiscordWebhookService.retryDelayMs("-3", 1));
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

	private static String repeat(char c, int count)
	{
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	@Test
	public void contentAtTheLimitIsLeftAlone()
	{
		String text = repeat('a', DiscordWebhookService.MAX_CONTENT_LENGTH);
		assertEquals(text, DiscordWebhookService.truncate(text));
	}

	@Test
	public void longContentIsCutAtASpace()
	{
		String head = repeat('a', 1990);
		assertEquals(head + "\u2026", DiscordWebhookService.truncate(head + " " + repeat('b', 50)));
	}

	@Test
	public void longContentIsCutAtALineBreak()
	{
		String head = "first event " + repeat('a', 1900);
		assertEquals(head + "\u2026", DiscordWebhookService.truncate(head + "\n" + repeat('b', 200)));
	}

	@Test
	public void spaceRightAtTheLimitStillFits()
	{
		String head = repeat('a', DiscordWebhookService.MAX_CONTENT_LENGTH - 1);
		String result = DiscordWebhookService.truncate(head + " more");
		assertEquals(head + "\u2026", result);
		assertEquals(DiscordWebhookService.MAX_CONTENT_LENGTH, result.length());
	}

	@Test
	public void contentWithNoBreakIsHardCut()
	{
		String result = DiscordWebhookService.truncate(repeat('a', 3000));
		assertEquals(DiscordWebhookService.MAX_CONTENT_LENGTH, result.length());
		assertTrue(result.endsWith("\u2026"));
	}

	@Test
	public void missingImageIsNotJpeg()
	{
		assertFalse(DiscordWebhookService.isJpeg(null));
		assertFalse(DiscordWebhookService.isJpeg(new byte[0]));
	}
}
