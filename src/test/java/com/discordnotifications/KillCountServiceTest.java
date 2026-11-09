package com.discordnotifications;

import com.discordnotifications.KillCountService.Drop;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.game.ItemStack;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
	public void petsOnlyTakeAFreshKill()
	{
		Instant time = Instant.parse("2026-01-01T00:00:00Z");
		Drop drop = new Drop("Goblin", LootRecordType.NPC, Collections.emptyList(), time);
		assertTrue(KillCountService.isRecentForPet(drop, time.plusSeconds(6)));
		assertFalse(KillCountService.isRecentForPet(drop, time.plusSeconds(7)));
		assertFalse(KillCountService.isRecentForPet(null, time));
	}

	@Test
	public void petsKeepTheLongWindowForEventDrops()
	{
		Instant time = Instant.parse("2026-01-01T00:00:00Z");
		Drop drop = new Drop("Chambers of Xeric", LootRecordType.EVENT, Collections.emptyList(), time);
		assertTrue(KillCountService.isRecentForPet(drop, time.plusSeconds(30)));
		assertFalse(KillCountService.isRecentForPet(drop, time.plusSeconds(31)));
	}

	@Test
	public void onlyTheCoxKcLineWaitsForTheChest()
	{
		Instant time = Instant.parse("2026-01-01T00:00:00Z");
		assertTrue(KillCountService.isAwaitingCoxChest(new Drop("Chambers of Xeric", LootRecordType.EVENT, Collections.emptyList(), time)));
		assertTrue(KillCountService.isAwaitingCoxChest(new Drop("Chambers of Xeric Challenge Mode", LootRecordType.EVENT, Collections.emptyList(), time)));
		assertFalse(KillCountService.isAwaitingCoxChest(new Drop("Chambers of Xeric", LootRecordType.EVENT, Collections.singletonList(new ItemStack(ItemID.COINS, 1)), time)));
		assertFalse(KillCountService.isAwaitingCoxChest(new Drop("Theatre of Blood", LootRecordType.EVENT, Collections.emptyList(), time)));
		assertFalse(KillCountService.isAwaitingCoxChest(new Drop("Goblin", LootRecordType.NPC, Collections.emptyList(), time)));
		assertFalse(KillCountService.isAwaitingCoxChest(null));
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
