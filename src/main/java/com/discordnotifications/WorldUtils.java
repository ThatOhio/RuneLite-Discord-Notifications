package com.discordnotifications;

import com.google.common.collect.Sets;
import net.runelite.api.WorldType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class WorldUtils
{
	private static final Set<WorldType> IGNORED = EnumSet.of(
		WorldType.PVP_ARENA,
		WorldType.QUEST_SPEEDRUNNING,
		WorldType.BETA_WORLD,
		WorldType.NOSAVE_MODE,
		WorldType.TOURNAMENT_WORLD);

	// Seasonal Deadman worlds carry the tournament flag too
	private static final Set<WorldType> IGNORED_ON_DEADMAN = Sets.difference(IGNORED, EnumSet.of(WorldType.TOURNAMENT_WORLD));

	private static final Set<WorldType> SEPARATE_STATS = Sets.union(IGNORED, EnumSet.of(WorldType.DEADMAN, WorldType.SEASONAL));

	private WorldUtils()
	{
	}

	public static boolean isIgnoredWorld(Set<WorldType> worldTypes)
	{
		return !Collections.disjoint(worldTypes.contains(WorldType.DEADMAN) ? IGNORED_ON_DEADMAN : IGNORED, worldTypes);
	}

	public static String gameModePrefix(Set<WorldType> worldTypes)
	{
		if (worldTypes.contains(WorldType.DEADMAN))
		{
			return "[DMM] ";
		}

		return worldTypes.contains(WorldType.SEASONAL) ? "[Leagues] " : "";
	}

	public static Set<WorldType> separateStatsTypes(Set<WorldType> worldTypes)
	{
		return Sets.intersection(worldTypes, SEPARATE_STATS).immutableCopy();
	}
}
