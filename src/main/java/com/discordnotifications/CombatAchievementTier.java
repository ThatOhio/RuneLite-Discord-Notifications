package com.discordnotifications;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.VarbitID;

@RequiredArgsConstructor
public enum CombatAchievementTier
{
	EASY("Easy", VarbitID.CA_THRESHOLD_EASY),
	MEDIUM("Medium", VarbitID.CA_THRESHOLD_MEDIUM),
	HARD("Hard", VarbitID.CA_THRESHOLD_HARD),
	ELITE("Elite", VarbitID.CA_THRESHOLD_ELITE),
	MASTER("Master", VarbitID.CA_THRESHOLD_MASTER),
	GRANDMASTER("Grandmaster", VarbitID.CA_THRESHOLD_GRANDMASTER);

	private final String displayName;

	@Getter
	private final int thresholdVarbit;

	public int getPoints()
	{
		return ordinal() + 1;
	}

	public static CombatAchievementTier fromName(String name)
	{
		for (CombatAchievementTier tier : values())
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
