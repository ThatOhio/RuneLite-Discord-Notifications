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
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.noneOf(WorldType.class)));
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.MEMBERS, WorldType.SKILL_TOTAL)));
	}

	@Test
	public void specialWorldsAreIgnored()
	{
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.MEMBERS, WorldType.PVP_ARENA)));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.QUEST_SPEEDRUNNING)));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.BETA_WORLD)));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.NOSAVE_MODE)));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.TOURNAMENT_WORLD)));
	}

	@Test
	public void seasonalWorldsAreNotIgnored()
	{
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.SEASONAL)));
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.DEADMAN)));
		assertFalse(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.DEADMAN, WorldType.TOURNAMENT_WORLD)));
	}

	@Test
	public void betaSeasonalWorldsAreStillIgnored()
	{
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.SEASONAL, WorldType.BETA_WORLD)));
		assertTrue(WorldUtils.isIgnoredWorld(EnumSet.of(WorldType.DEADMAN, WorldType.TOURNAMENT_WORLD, WorldType.BETA_WORLD)));
	}

	@Test
	public void prefixNamesTheGameMode()
	{
		assertEquals("", WorldUtils.gameModePrefix(EnumSet.of(WorldType.MEMBERS)));
		assertEquals("[Leagues] ", WorldUtils.gameModePrefix(EnumSet.of(WorldType.SEASONAL)));
		assertEquals("[DMM] ", WorldUtils.gameModePrefix(EnumSet.of(WorldType.DEADMAN)));
		assertEquals("[DMM] ", WorldUtils.gameModePrefix(EnumSet.of(WorldType.SEASONAL, WorldType.DEADMAN)));
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
