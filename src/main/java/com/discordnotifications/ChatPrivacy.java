package com.discordnotifications;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ChatPrivacy
{
	SHOW_ALL("Show all"),
	HIDE_SPLIT_PM("Hide split PMs"),
	HIDE_ALL("Hide all chat");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
