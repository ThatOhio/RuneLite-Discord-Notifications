package com.discordnotifications;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
}
