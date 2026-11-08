package com.discordnotifications;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Template
{
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$([a-zA-Z]+)");

	private Template()
	{
	}

	public static String render(String template, Map<String, String> values)
	{
		return PLACEHOLDER.matcher(template).replaceAll(match ->
			Matcher.quoteReplacement(values.getOrDefault(match.group(1), "")));
	}
}
