package com.discordnotifications;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LeagueTaskTier
{
	EASY("Easy", 10),
	MEDIUM("Medium", 30),
	HARD("Hard", 80),
	ELITE("Elite", 200),
	MASTER("Master", 400);

	private final String displayName;
	@Getter
	private final int points;

	public static LeagueTaskTier fromName(String name)
	{
		for (LeagueTaskTier tier : values())
		{
			if (tier.displayName.equalsIgnoreCase(name))
			{
				return tier;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
