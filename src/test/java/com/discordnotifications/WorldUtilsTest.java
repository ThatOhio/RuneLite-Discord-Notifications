package com.discordnotifications;

import net.runelite.api.WorldType;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class WorldUtilsTest
{
	@Test
	public void normalWorldsAreNotIgnored()
	{
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.noneOf(WorldType.class), false));
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.MEMBERS, WorldType.SKILL_TOTAL), false));
	}

	@Test
	public void specialWorldsAreIgnored()
	{
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.MEMBERS, WorldType.PVP_ARENA), false));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.QUEST_SPEEDRUNNING), false));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.BETA_WORLD), false));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.NOSAVE_MODE), false));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.TOURNAMENT_WORLD), false));
	}

	@Test
	public void seasonalWorldsAreNotIgnored()
	{
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.SEASONAL), false));
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.DEADMAN), false));
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.DEADMAN, WorldType.TOURNAMENT_WORLD), false));
	}

	@Test
	public void betaSeasonalWorldsAreStillIgnored()
	{
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.SEASONAL, WorldType.BETA_WORLD), false));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.DEADMAN, WorldType.TOURNAMENT_WORLD, WorldType.BETA_WORLD), false));
	}

	@Test
	public void gridMasterIsTheOnlyTournamentWorldLetThrough()
	{
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.TOURNAMENT_WORLD), true));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.TOURNAMENT_WORLD), false));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.TOURNAMENT_WORLD, WorldType.BETA_WORLD), true));
	}

	@Test
	public void prefixNamesTheGameMode()
	{
		assertEquals("", WorldUtils.gameModePrefix(EnumSet.of(WorldType.MEMBERS), false));
		assertEquals("[Leagues] ", WorldUtils.gameModePrefix(EnumSet.of(WorldType.SEASONAL), false));
		assertEquals("[DMM] ", WorldUtils.gameModePrefix(EnumSet.of(WorldType.DEADMAN), false));
		assertEquals("[DMM] ", WorldUtils.gameModePrefix(EnumSet.of(WorldType.SEASONAL, WorldType.DEADMAN), false));
		assertEquals("[Grid Master] ", WorldUtils.gameModePrefix(EnumSet.of(WorldType.TOURNAMENT_WORLD), true));
	}

	@Test
	public void separateStatsIgnoresOrdinaryWorldFlags()
	{
		assertEquals(WorldUtils.separateStatsTypes(EnumSet.of(WorldType.MEMBERS)),
			WorldUtils.separateStatsTypes(EnumSet.of(WorldType.PVP, WorldType.SKILL_TOTAL)));
		assertNotEquals(WorldUtils.separateStatsTypes(EnumSet.of(WorldType.MEMBERS)),
			WorldUtils.separateStatsTypes(EnumSet.of(WorldType.MEMBERS, WorldType.SEASONAL)));
	}
}
