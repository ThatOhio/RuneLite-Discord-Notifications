package com.discordnotifications;

import java.util.List;
import java.util.stream.Collectors;
import okhttp3.HttpUrl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
}
