package com.discordnotifications;

import com.discordnotifications.NpcLootPairing.Signal;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NpcLootPairingTest
{
	private static final Instant START = Instant.parse("2026-11-08T12:00:00Z");
	private static final int COINS = 995;
	private static final int BONES = 526;
	private static final int SCROLL_BOX = 24362;
	private static final int RUNES = 562;

	@Mock
	private ItemManager itemManager;

	@InjectMocks
	private NpcLootPairing pairing;

	private static Collection<ItemStack> loot(int... idsAndQuantities)
	{
		ItemStack[] stacks = new ItemStack[idsAndQuantities.length / 2];
		for (int i = 0; i < stacks.length; i++)
		{
			stacks[i] = new ItemStack(idsAndQuantities[i * 2], idsAndQuantities[i * 2 + 1]);
		}
		return Arrays.asList(stacks);
	}

	private void named(int id, String name)
	{
		ItemComposition item = mock(ItemComposition.class);
		when(item.getName()).thenReturn(name);
		when(itemManager.getItemComposition(id)).thenReturn(item);
	}

	private boolean see(String source, Collection<ItemStack> items, Signal signal, long seconds)
	{
		return pairing.isNewKill(source, items, signal, START.plusSeconds(seconds));
	}

	@Test
	public void bothEventsAreOneKillInEitherOrder()
	{
		assertTrue(see("Vorkath", loot(COINS, 10), Signal.SERVER, 0));
		assertFalse(see("Vorkath", loot(COINS, 10), Signal.TILE_SCAN, 0));

		assertTrue(see("Vorkath", loot(COINS, 10), Signal.TILE_SCAN, 1));
		assertFalse(see("Vorkath", loot(COINS, 10), Signal.SERVER, 1));
	}

	@Test
	public void serverOnlyBossCountsEveryKill()
	{
		assertTrue(see("Yama", loot(COINS, 10), Signal.SERVER, 0));
		assertTrue(see("Yama", loot(COINS, 10), Signal.SERVER, 1));
	}

	@Test
	public void identicalKillsEachPairOnce()
	{
		assertTrue(see("Goblin", loot(COINS, 1), Signal.SERVER, 0));
		assertTrue(see("Goblin", loot(COINS, 1), Signal.SERVER, 0));
		assertFalse(see("Goblin", loot(COINS, 1), Signal.TILE_SCAN, 0));
		assertFalse(see("Goblin", loot(COINS, 1), Signal.TILE_SCAN, 0));
		assertTrue(see("Goblin", loot(COINS, 1), Signal.TILE_SCAN, 0));
	}

	@Test
	public void otherNpcOrLootIsAnotherKill()
	{
		assertTrue(see("Vorkath", loot(COINS, 10), Signal.SERVER, 0));
		assertTrue(see("Zulrah", loot(COINS, 10), Signal.TILE_SCAN, 0));
		assertTrue(see("Vorkath", loot(RUNES, 10), Signal.TILE_SCAN, 0));
	}

	@Test
	public void stackingDoesNotMatter()
	{
		assertTrue(see("Mithril dragon", loot(COINS, 1, COINS, 1, COINS, 1), Signal.TILE_SCAN, 0));
		assertFalse(see("Mithril dragon", loot(COINS, 3), Signal.SERVER, 0));
	}

	@Test
	public void tileScanMissingBonesStillPairs()
	{
		assertTrue(see("Mithril dragon", loot(COINS, 3, BONES, 1), Signal.SERVER, 0));
		assertFalse(see("Mithril dragon", loot(COINS, 3), Signal.TILE_SCAN, 0));
	}

	@Test
	public void exactMatchWinsOverEarlierSubset()
	{
		assertTrue(see("Vorkath", loot(COINS, 3, RUNES, 1), Signal.SERVER, 0));
		assertTrue(see("Vorkath", loot(COINS, 3), Signal.SERVER, 0));
		assertFalse(see("Vorkath", loot(COINS, 3), Signal.TILE_SCAN, 0));
		assertFalse(see("Vorkath", loot(COINS, 3, RUNES, 1), Signal.TILE_SCAN, 0));
	}

	@Test
	public void oneSidedItemsPairAcrossBothLists()
	{
		named(BONES, "Big bones");
		named(SCROLL_BOX, "Scroll box (medium)");

		assertTrue(see("Scurrius", loot(COINS, 5, BONES, 1), Signal.SERVER, 0));
		assertFalse(see("Scurrius", loot(COINS, 5, SCROLL_BOX, 1), Signal.TILE_SCAN, 0));
	}

	@Test
	public void overlapOnOrdinaryItemsIsAnotherKill()
	{
		named(BONES, "Big bones");
		named(RUNES, "Chaos rune");

		assertTrue(see("Vorkath", loot(COINS, 5, BONES, 1), Signal.SERVER, 0));
		assertTrue(see("Vorkath", loot(COINS, 5, RUNES, 1), Signal.TILE_SCAN, 0));
	}

	@Test
	public void pairHoldsThroughDelayedTileScanThenExpires()
	{
		assertTrue(see("The Nightmare", loot(COINS, 1), Signal.SERVER, 0));
		assertFalse(see("The Nightmare", loot(COINS, 1), Signal.TILE_SCAN, 6));

		assertTrue(see("The Nightmare", loot(COINS, 1), Signal.SERVER, 10));
		assertTrue(see("The Nightmare", loot(COINS, 1), Signal.TILE_SCAN, 19));
	}

	@Test
	public void resetForgetsUnpairedKills()
	{
		assertTrue(see("Vorkath", Collections.singletonList(new ItemStack(COINS, 1)), Signal.SERVER, 0));
		pairing.reset();
		assertTrue(see("Vorkath", Collections.singletonList(new ItemStack(COINS, 1)), Signal.TILE_SCAN, 0));
	}
}
