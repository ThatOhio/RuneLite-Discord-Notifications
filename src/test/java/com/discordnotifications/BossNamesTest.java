package com.discordnotifications;

import com.discordnotifications.BossNames.BossKc;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BossNamesTest
{
	@Test
	public void parsesKillCounts()
	{
		assertEquals(new BossKc("Zulrah", 1234), BossNames.parseBoss("Your Zulrah kill count is: 1,234."));
		assertEquals(new BossKc("Kree'arra", 5), BossNames.parseBoss("Your Kree'arra kill count is: 5."));
		assertEquals(new BossKc("Herbiboar", 12), BossNames.parseBoss("Your herbiboar harvest count is: 12."));
	}

	@Test
	public void parsesChestsAndCompletions()
	{
		assertEquals(new BossKc("Barrows", 77), BossNames.parseBoss("Your Barrows chest count is: 77."));
		assertEquals(new BossKc("Lunar Chest", 40), BossNames.parseBoss("Your Lunar Chest count is: 40."));
		assertEquals(new BossKc("Gauntlet", 9), BossNames.parseBoss("Your Gauntlet completion count is: 9."));
		assertEquals(new BossKc("Corrupted Gauntlet", 3), BossNames.parseBoss("Your Corrupted Gauntlet completion count is: 3."));
	}

	@Test
	public void parsesRaidsAndWintertodt()
	{
		assertEquals(new BossKc("Chambers of Xeric", 50), BossNames.parseBoss("Your completed Chambers of Xeric count is: 50."));
		assertEquals(new BossKc("Chambers of Xeric Challenge Mode", 2), BossNames.parseBoss("Your completed Chambers of Xeric Challenge Mode count is: 2."));
		assertEquals(new BossKc("Tombs of Amascut: Expert Mode", 7), BossNames.parseBoss("Your completed Tombs of Amascut: Expert Mode count is: 7."));
		assertEquals(new BossKc("Wintertodt", 300), BossNames.parseBoss("Your subdued Wintertodt count is: 300."));
		assertEquals(new BossKc("Fractured Archive", 12), BossNames.parseBoss("Your completed Fractured Archive count is: 12."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(BossNames.parseBoss("Your reward is: 5"));
		assertNull(BossNames.parseBoss("Your Ardougne Rooftop lap count is: 200."));
		assertNull(BossNames.parseBoss("Your Agility Arena Total Ticket count is: 1,000."));
		assertNull(BossNames.parseBoss("Your Treasure chest count is: 5."));
		assertNull(BossNames.parseBoss("Your completed Quest count is: 5."));
		assertNull(BossNames.parseBoss("Your Zulrah kill count is: 99999999999."));
	}

	@Test
	public void cleansNamesToChatCommandKeys()
	{
		assertEquals("gauntlet", BossNames.cleanBossName("The Gauntlet"));
		assertEquals("gauntlet", BossNames.cleanBossName("Gauntlet"));
		assertEquals("gauntlet", BossNames.cleanBossName("Crystalline Hunllef"));
		assertEquals("corrupted gauntlet", BossNames.cleanBossName("Corrupted Hunllef"));
		assertEquals("corrupted gauntlet", BossNames.cleanBossName("Corrupted Gauntlet"));
		assertEquals("barrows chests", BossNames.cleanBossName("Barrows"));
		assertEquals("whisperer", BossNames.cleanBossName("The Whisperer"));
		assertEquals("nightmare", BossNames.cleanBossName("The Nightmare"));
		assertEquals("royal titans", BossNames.cleanBossName("Eldric the Ice King"));
		assertEquals("tempoross", BossNames.cleanBossName("Reward pool (Tempoross)"));
		assertEquals("wintertodt", BossNames.cleanBossName("Supply crate (Wintertodt)"));
		assertEquals("tombs of amascut expert mode", BossNames.cleanBossName("Tombs of Amascut: Expert Mode"));
		assertEquals("kree'arra", BossNames.cleanBossName("Kree'arra"));
	}

	@Test
	public void varpKeysAreCleanNames()
	{
		for (String key : BossNames.BOSS_VARPS.keySet())
		{
			assertEquals(key, BossNames.cleanBossName(key));
		}
	}
}
