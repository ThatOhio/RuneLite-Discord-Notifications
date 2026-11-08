package com.discordnotifications;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Template
{
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$([a-zA-Z]+)");
	private static final Pattern MARKDOWN = Pattern.compile("[\\\\*_~`|>]");

	private Template()
	{
	}

	public static String render(String template, Map<String, String> values)
	{
		return PLACEHOLDER.matcher(template).replaceAll(match ->
			Matcher.quoteReplacement(escapeMarkdown(values.getOrDefault(match.group(1), ""))));
	}

	private static String escapeMarkdown(String value)
	{
		return MARKDOWN.matcher(value).replaceAll("\\\\$0");
	}
}
