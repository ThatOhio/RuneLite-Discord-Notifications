package com.discordnotifications;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LeagueTaskTier
{
	EASY("Easy"),
	MEDIUM("Medium"),
	HARD("Hard"),
	ELITE("Elite"),
	MASTER("Master");

	private final String displayName;

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
