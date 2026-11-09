package com.discordnotifications;

import com.discordnotifications.KillCountService.Drop;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KillCountServiceTest
{
	private static final String GROUP = "discordnotifications";
	private static final String ZULRAH_SAVED = "kc_zulrah";

	@Mock
	private Client client;

	@Mock
	private ClientThread clientThread;

	@Mock
	private ConfigManager configManager;

	@Mock
	private CoreKillCounts coreKillCounts;

	@InjectMocks
	private KillCountService service;

	private final Map<String, Integer> saved = new HashMap<>();

	@Before
	public void setUp()
	{
		lenient().doAnswer(call -> saved.get(call.<String>getArgument(1)))
			.when(configManager).getRSProfileConfiguration(eq(GROUP), anyString(), eq(int.class));
		lenient().doAnswer(call -> saved.put(call.getArgument(1), call.getArgument(2)))
			.when(configManager).setRSProfileConfiguration(eq(GROUP), anyString(), any());
	}

	private void killZulrah()
	{
		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Zulrah");
		when(npc.getId()).thenReturn(NpcID.SNAKEBOSS_BOSS_RANGED);
		service.onNpcLootReceived(new NpcLootReceived(npc, Collections.emptyList()));
	}

	private void chatZulrah(int kc)
	{
		service.onGameMessage("Your Zulrah kill count is: " + kc + ".");
	}

	private Integer zulrahKc()
	{
		return service.getKillCount(LootRecordType.NPC, "Zulrah");
	}

	@Test
	public void noPriorCountStaysUnknown()
	{
		killZulrah();
		killZulrah();
		assertNull(zulrahKc());
		assertTrue(saved.isEmpty());
	}

	@Test
	public void lootBeforeChat()
	{
		saved.put(ZULRAH_SAVED, 100);
		killZulrah();
		assertEquals(Integer.valueOf(101), zulrahKc());

		chatZulrah(101);
		assertEquals(Integer.valueOf(101), zulrahKc());
		assertEquals(Integer.valueOf(101), saved.get(ZULRAH_SAVED));
	}

	@Test
	public void chatBeforeLoot()
	{
		saved.put(ZULRAH_SAVED, 100);
		chatZulrah(101);
		assertEquals(Integer.valueOf(101), zulrahKc());

		killZulrah();
		assertEquals(Integer.valueOf(101), zulrahKc());

		chatZulrah(102);
		killZulrah();
		assertEquals(Integer.valueOf(102), zulrahKc());
	}

	@Test
	public void firstChatLineStartsTheCount()
	{
		chatZulrah(5);
		killZulrah();
		assertEquals(Integer.valueOf(5), zulrahKc());
		assertEquals(Integer.valueOf(5), saved.get(ZULRAH_SAVED));
	}

	@Test
	public void varpAlreadyIncludingTheKillIsNotCountedTwice()
	{
		when(client.getVarpValue(VarPlayerID.TOTAL_SNAKEBOSS_KILLS)).thenReturn(101);
		killZulrah();
		assertEquals(Integer.valueOf(101), zulrahKc());

		when(client.getVarpValue(VarPlayerID.TOTAL_SNAKEBOSS_KILLS)).thenReturn(102);
		killZulrah();
		assertEquals(Integer.valueOf(102), zulrahKc());
	}

	@Test
	public void chatCommandsCountAlreadyIncludingTheKillIsNotCountedTwice()
	{
		when(coreKillCounts.chatCommandsKc("zulrah")).thenReturn(101);
		killZulrah();
		assertEquals(Integer.valueOf(101), zulrahKc());
	}

	@Test
	public void lootTrackerCountNeverIncludesTheKillBeingHandled()
	{
		when(coreKillCounts.lootTrackerKills(LootRecordType.NPC, "Zulrah")).thenReturn(100);
		killZulrah();
		assertEquals(Integer.valueOf(101), zulrahKc());
	}

	@Test
	public void gameCountsAreNotCopiedIntoAnEmptySave()
	{
		when(client.getVarpValue(VarPlayerID.TOTAL_SNAKEBOSS_KILLS)).thenReturn(101);
		killZulrah();
		assertTrue(saved.isEmpty());
	}

	@Test
	public void playerKillsDontShareAnNpcCount()
	{
		saved.put(ZULRAH_SAVED, 100);
		when(coreKillCounts.lootTrackerKills(LootRecordType.PLAYER, "Zulrah")).thenReturn(5);

		Player player = mock(Player.class);
		when(player.getName()).thenReturn("Zulrah");
		service.onPlayerLootReceived(new PlayerLootReceived(player, Collections.emptyList()));

		assertEquals(Integer.valueOf(6), service.getKillCount(LootRecordType.PLAYER, "Zulrah"));
		assertEquals(Integer.valueOf(100), zulrahKc());
		assertEquals(Integer.valueOf(100), saved.get(ZULRAH_SAVED));
	}

	@Test
	public void resetForgetsCachedCounts()
	{
		when(client.getVarpValue(VarPlayerID.TOTAL_SNAKEBOSS_KILLS)).thenReturn(101);
		killZulrah();

		service.reset();
		when(client.getVarpValue(VarPlayerID.TOTAL_SNAKEBOSS_KILLS)).thenReturn(0);
		assertNull(zulrahKc());
	}

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
