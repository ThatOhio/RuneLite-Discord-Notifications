package com.discordnotifications;

import com.discordnotifications.KillCountService.BossKc;
import com.discordnotifications.KillCountService.Drop;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KillCountServiceTest
{
	@Test
	public void dropsGoStaleAfterThirtySeconds()
	{
		Instant time = Instant.parse("2026-11-08T12:00:00Z");
		Drop drop = new Drop("Zulrah", LootRecordType.NPC, Collections.emptyList(), time);
		assertTrue(KillCountService.isRecent(drop, time.plusSeconds(30)));
		assertFalse(KillCountService.isRecent(drop, time.plusSeconds(31)));
		assertFalse(KillCountService.isRecent(null, time));
	}

	@Test
	public void sourceAndKcAddTheirOwnSpace()
	{
		assertEquals("Got a fang from Zulrah (KC 1,234)",
			Template.render("Got a fang$from$kc", KillCountService.sourcePlaceholders("Zulrah", 1234)));
	}

	@Test
	public void unknownKcLeavesNothingBehind()
	{
		assertEquals("Got a fang from Zulrah!",
			Template.render("Got a fang$from$kc!", KillCountService.sourcePlaceholders("Zulrah", null)));
	}

	@Test
	public void parsesKillCounts()
	{
		assertEquals(new BossKc("Zulrah", 1234), KillCountService.parseBoss("Your Zulrah kill count is: 1,234."));
		assertEquals(new BossKc("Kree'arra", 5), KillCountService.parseBoss("Your Kree'arra kill count is: 5."));
		assertEquals(new BossKc("herbiboar", 12), KillCountService.parseBoss("Your herbiboar harvest count is: 12."));
	}

	@Test
	public void parsesChestsAndCompletions()
	{
		assertEquals(new BossKc("Barrows", 77), KillCountService.parseBoss("Your Barrows chest count is: 77."));
		assertEquals(new BossKc("Lunar Chest", 40), KillCountService.parseBoss("Your Lunar Chest count is: 40."));
		assertEquals(new BossKc("Gauntlet", 9), KillCountService.parseBoss("Your Gauntlet completion count is: 9."));
		assertEquals(new BossKc("Corrupted Gauntlet", 3), KillCountService.parseBoss("Your Corrupted Gauntlet completion count is: 3."));
	}

	@Test
	public void parsesRaidsAndWintertodt()
	{
		assertEquals(new BossKc("Chambers of Xeric", 50), KillCountService.parseBoss("Your completed Chambers of Xeric count is: 50."));
		assertEquals(new BossKc("Chambers of Xeric Challenge Mode", 2), KillCountService.parseBoss("Your completed Chambers of Xeric Challenge Mode count is: 2."));
		assertEquals(new BossKc("Tombs of Amascut: Expert Mode", 7), KillCountService.parseBoss("Your completed Tombs of Amascut: Expert Mode count is: 7."));
		assertEquals(new BossKc("Wintertodt", 300), KillCountService.parseBoss("Your subdued Wintertodt count is: 300."));
		assertEquals(new BossKc("Fractured Archive", 12), KillCountService.parseBoss("Your completed Fractured Archive count is: 12."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(KillCountService.parseBoss("Your reward is: 5"));
		assertNull(KillCountService.parseBoss("Your Treasure chest count is: 5."));
		assertNull(KillCountService.parseBoss("Your completed Quest count is: 5."));
		assertNull(KillCountService.parseBoss("Your Zulrah kill count is: 99999999999."));
	}

	@Test
	public void cleansNamesToChatCommandKeys()
	{
		assertEquals("gauntlet", KillCountService.cleanBossName("The Gauntlet"));
		assertEquals("gauntlet", KillCountService.cleanBossName("Gauntlet"));
		assertEquals("gauntlet", KillCountService.cleanBossName("Crystalline Hunllef"));
		assertEquals("corrupted gauntlet", KillCountService.cleanBossName("Corrupted Hunllef"));
		assertEquals("corrupted gauntlet", KillCountService.cleanBossName("Corrupted Gauntlet"));
		assertEquals("barrows chests", KillCountService.cleanBossName("Barrows"));
		assertEquals("whisperer", KillCountService.cleanBossName("The Whisperer"));
		assertEquals("nightmare", KillCountService.cleanBossName("The Nightmare"));
		assertEquals("royal titans", KillCountService.cleanBossName("Eldric the Ice King"));
		assertEquals("tempoross", KillCountService.cleanBossName("Reward pool (Tempoross)"));
		assertEquals("wintertodt", KillCountService.cleanBossName("Supply crate (Wintertodt)"));
		assertEquals("tombs of amascut expert mode", KillCountService.cleanBossName("Tombs of Amascut: Expert Mode"));
		assertEquals("kree'arra", KillCountService.cleanBossName("Kree'arra"));
	}

	@Test
	public void varpKeysAreCleanNames()
	{
		for (String key : KillCountService.BOSS_VARPS.keySet())
		{
			assertEquals(key, KillCountService.cleanBossName(key));
		}
	}

	@Test
	public void newBossesHaveAVarp()
	{
		assertEquals(Integer.valueOf(VarPlayerID.TOTAL_COMPLETED_THEFRACTUREDARCHIVE), KillCountService.BOSS_VARPS.get(KillCountService.cleanBossName("Fractured Archive")));
		assertEquals(Integer.valueOf(VarPlayerID.TOTAL_MAGGOT_KING_KILLS), KillCountService.BOSS_VARPS.get(KillCountService.cleanBossName("Maggot King")));
	}

	@Test
	public void eachNpcUsesOneLootEvent()
	{
		assertTrue(KillCountService.usesServerLoot(NpcID.MAGGOT_KING_CORPSE, "Maggot King"));
		assertFalse(KillCountService.usesNpcLootEvent(NpcID.MAGGOT_KING_CORPSE, "Maggot King"));
		assertTrue(KillCountService.usesServerLoot(NpcID.YAMA, "Yama"));
		assertTrue(KillCountService.usesServerLoot(-1, "Hallowed Sepulchre (Floor 5)"));
		assertFalse(KillCountService.usesServerLoot(NpcID.ARAXXOR, "Araxxor"));
		assertFalse(KillCountService.usesNpcLootEvent(NpcID.ARAXXOR, "Araxxor"));
		assertFalse(KillCountService.usesServerLoot(1, "Goblin"));
		assertTrue(KillCountService.usesNpcLootEvent(1, "Goblin"));
		assertFalse(KillCountService.usesNpcLootEvent(1, null));
	}

	@Test
	public void parsesSlayerLogCounts()
	{
		assertEquals(1234, KillCountService.parseSlayerCount("1,234"));
		assertEquals(65535, KillCountService.parseSlayerCount("Lots!"));
		assertEquals(0, KillCountService.parseSlayerCount("-"));
	}
}
