package com.discordnotifications;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SeasonalWorlds
{
	NOTIFY("Notify"),
	IGNORE("Ignore");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
