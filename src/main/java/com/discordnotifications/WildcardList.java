package com.discordnotifications;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class WildcardList
{
	private final List<Pattern> patterns;

	private WildcardList(List<Pattern> patterns)
	{
		this.patterns = patterns;
	}

	public static WildcardList parse(String text)
	{
		return new WildcardList(Arrays.stream(text.split("[\n,;]"))
			.map(String::trim)
			.filter(entry -> !entry.isEmpty())
			.map(WildcardList::toPattern)
			.collect(Collectors.toList()));
	}

	public boolean matches(String value)
	{
		return patterns.stream().anyMatch(pattern -> pattern.matcher(value).matches());
	}

	private static Pattern toPattern(String wildcard)
	{
		String regex = Arrays.stream(wildcard.split("\\*", -1))
			.map(Pattern::quote)
			.collect(Collectors.joining(".*"));
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}
}
