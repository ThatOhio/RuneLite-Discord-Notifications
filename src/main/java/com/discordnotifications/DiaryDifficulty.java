package com.discordnotifications;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DiaryDifficulty
{
	EASY("Easy"),
	MEDIUM("Medium"),
	HARD("Hard"),
	ELITE("Elite");

	private final String displayName;

	public static DiaryDifficulty fromName(String name)
	{
		for (DiaryDifficulty difficulty : values())
		{
			if (difficulty.displayName.equalsIgnoreCase(name))
			{
				return difficulty;
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
