package com.discordnotifications;

import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
public class CompletedClue
{
	private static final Pattern CLUE_REGEX = Pattern.compile("You have completed (?<count>\\d+) (?<tier>\\w+) Treasure Trails?\\.");

	ClueTier tier;
	int count;

	public static CompletedClue parse(String message)
	{
		Matcher matcher = CLUE_REGEX.matcher(message);
		if (!matcher.find())
		{
			return null;
		}

		ClueTier tier = ClueTier.fromName(matcher.group("tier"));
		return tier == null ? null : new CompletedClue(tier, Integer.parseInt(matcher.group("count")));
	}
}
