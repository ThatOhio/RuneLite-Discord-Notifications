package com.discordnotifications.notifiers;

import com.discordnotifications.ItemStackFormatter.Stack;
import com.discordnotifications.Template;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LootNotifierTest
{
	private static final Stack WHIP = new Stack("Abyssal whip", 1, 1_500_000);
	private static final Stack LAWS = new Stack("Law rune", 10, 200);

	@Test
	public void onlyStacksOverTheMinimumQualify()
	{
		assertEquals(Collections.singletonList(WHIP), LootNotifier.qualifying(Arrays.asList(WHIP, LAWS), 100_000));
		assertTrue(LootNotifier.qualifying(Collections.singletonList(LAWS), 100_000).isEmpty());
	}

	@Test
	public void minimumIsInclusive()
	{
		assertEquals(1, LootNotifier.qualifying(Collections.singletonList(new Stack("Item", 1, 100_000)), 100_000).size());
		assertTrue(LootNotifier.qualifying(Collections.singletonList(new Stack("Item", 1, 99_999)), 100_000).isEmpty());
	}

	@Test
	public void stackValueDoesNotOverflow()
	{
		Stack huge = new Stack("Item", 2_000_000, 5_000);
		assertEquals(10_000_000_000L, huge.getTotalPrice());
		assertEquals(1, LootNotifier.qualifying(Collections.singletonList(huge), 100_000).size());
	}

	@Test
	public void itemsAreQualifyingStacksAndValueIsWholeDrop()
	{
		List<Stack> qualifying = Collections.singletonList(WHIP);
		assertEquals("1 x Abyssal whip (1.5M) from Vorkath (KC 1,234) worth 1.59M",
			Template.render("$items from $source$kc worth $value",
				LootNotifier.placeholders(qualifying, Arrays.asList(WHIP, new Stack("Coins", 90_000, 1)), "Vorkath", 1234)));
	}

	@Test
	public void unknownKillCountLeavesNothingBehind()
	{
		List<Stack> drop = Collections.singletonList(WHIP);
		assertEquals("from Vorkath, done", Template.render("from $source$kc, done", LootNotifier.placeholders(drop, drop, "Vorkath", null)));
	}

	@Test
	public void lootChestListsItsBestStackWhenTheWholeChestIsWorthIt()
	{
		List<Stack> chest = Arrays.asList(new Stack("Shark", 50, 800), new Stack("Rune platebody", 1, 38_000), new Stack("Coins", 30_000, 1));
		assertEquals(Collections.singletonList(chest.get(0)), LootNotifier.lootChestStandIn(chest, 100_000));
		assertTrue(LootNotifier.lootChestStandIn(chest, 200_000).isEmpty());
		assertTrue(LootNotifier.lootChestStandIn(Collections.emptyList(), 0).isEmpty());
	}

	@Test
	public void chatDropsCoverSceptreAndTrawling()
	{
		assertEquals("Pyramid Plunder", LootNotifier.CHAT_DROPS.get("You have found a Pharaoh's sceptre! It fell on the floor.").getSource());
		assertEquals("Deep sea trawling", LootNotifier.CHAT_DROPS.get("You catch a swift marlin!").getSource());
		assertEquals(7, LootNotifier.CHAT_DROPS.size());
	}

	@Test
	public void skipsGamblesAndClueCaskets()
	{
		assertTrue(LootNotifier.isSkippedEvent("Barbarian Assault high gamble"));
		assertTrue(LootNotifier.isSkippedEvent("Clue Scroll (Master)"));
		assertTrue(LootNotifier.isSkippedEvent("clue scroll (easy)"));
		assertFalse(LootNotifier.isSkippedEvent("Chambers of Xeric"));
		assertFalse(LootNotifier.isSkippedEvent("Clue"));
	}
}
