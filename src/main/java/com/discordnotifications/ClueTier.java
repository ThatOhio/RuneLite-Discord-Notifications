package com.discordnotifications;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ClueTier
{
	BEGINNER("Beginner"),
	EASY("Easy"),
	MEDIUM("Medium"),
	HARD("Hard"),
	ELITE("Elite"),
	MASTER("Master");

	private final String displayName;

	public static ClueTier fromName(String name)
	{
		for (ClueTier tier : values())
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
