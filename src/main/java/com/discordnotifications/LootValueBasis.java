package com.discordnotifications;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LootValueBasis
{
	ITEM("Single item"),
	STACK("Whole stack"),
	DROP("Whole drop");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
