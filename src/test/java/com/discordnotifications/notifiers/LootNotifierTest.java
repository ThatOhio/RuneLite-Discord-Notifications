package com.discordnotifications.notifiers;

import com.discordnotifications.ItemStackFormatter.Stack;
import com.discordnotifications.LootValueBasis;
import com.discordnotifications.Template;
import com.discordnotifications.WildcardList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.client.game.ItemStack;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LootNotifierTest
{
	private static final Stack WHIP = new Stack("Abyssal whip", 1, 1_500_000);
	private static final Stack LAWS = new Stack("Law rune", 10, 200);
	private static final Stack ONYX_TIPS = new Stack("Onyx bolt tips", 1_000, 9_000);
	private static final List<Stack> CHEST = Arrays.asList(new Stack("Shark", 50, 800), new Stack("Rune platebody", 1, 38_000), new Stack("Coins", 30_000, 1));

	private static List<Stack> qualifying(List<Stack> stacks, LootValueBasis basis, long minValue, String allowlist, String denylist)
	{
		return LootNotifier.qualifying(stacks, "Vorkath", minValue, basis, WildcardList.parse(allowlist), WildcardList.parse(denylist));
	}

	private static List<Stack> chestQualifying(LootValueBasis basis, long minValue, String denylist)
	{
		return LootNotifier.qualifying(CHEST, "Loot Chest", minValue, basis, WildcardList.parse(""), WildcardList.parse(denylist));
	}

	private static Map<Integer, Integer> kept(Map<Integer, Integer> unequipped, ItemStack... items)
	{
		return LootNotifier.withoutUnequipped(Arrays.asList(items), new HashMap<>(unequipped)).stream()
			.collect(Collectors.toMap(ItemStack::getId, ItemStack::getQuantity));
	}

	@Test
	public void removedItemsAreWhatLeftTheContainer()
	{
		assertEquals(Map.of(1, 1, 2, 30), LootNotifier.removedItems(Map.of(1, 1, 2, 100, 3, 1), Map.of(2, 70, 3, 1, 4, 1)));
	}

	@Test
	public void unequippedGearIsNotLoot()
	{
		assertEquals(Map.of(2, 5), kept(Map.of(1, 1), new ItemStack(1, 1), new ItemStack(2, 5)));
	}

	@Test
	public void onlyTheUnequippedCountIsSubtracted()
	{
		assertEquals(Map.of(1, 1), kept(Map.of(1, 1), new ItemStack(1, 2)));
	}

	@Test
	public void anUnequippedItemIsOnlySubtractedOnce()
	{
		Map<Integer, Integer> unequipped = new HashMap<>(Map.of(1, 1));
		assertTrue(LootNotifier.withoutUnequipped(List.of(new ItemStack(1, 1)), unequipped).isEmpty());
		assertEquals(1, LootNotifier.withoutUnequipped(List.of(new ItemStack(1, 1)), unequipped).size());
	}

	@Test
	public void onlyStacksOverTheMinimumQualify()
	{
		assertEquals(Collections.singletonList(WHIP), qualifying(Arrays.asList(WHIP, LAWS), LootValueBasis.STACK, 100_000, "", ""));
		assertTrue(qualifying(Collections.singletonList(LAWS), LootValueBasis.STACK, 100_000, "", "").isEmpty());
	}

	@Test
	public void minimumIsInclusive()
	{
		assertEquals(1, qualifying(Collections.singletonList(new Stack("Item", 1, 100_000)), LootValueBasis.STACK, 100_000, "", "").size());
		assertTrue(qualifying(Collections.singletonList(new Stack("Item", 1, 99_999)), LootValueBasis.STACK, 100_000, "", "").isEmpty());
	}

	@Test
	public void stackValueDoesNotOverflow()
	{
		Stack huge = new Stack("Item", 2_000_000, 5_000);
		assertEquals(10_000_000_000L, huge.getTotalPrice());
		assertEquals(1, qualifying(Collections.singletonList(huge), LootValueBasis.STACK, 100_000, "", "").size());
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
	public void itemBasisComparesThePriceOfOne()
	{
		assertTrue(qualifying(Collections.singletonList(ONYX_TIPS), LootValueBasis.ITEM, 100_000, "", "").isEmpty());
		assertEquals(Collections.singletonList(WHIP), qualifying(Arrays.asList(WHIP, ONYX_TIPS), LootValueBasis.ITEM, 100_000, "", ""));
	}

	@Test
	public void stackBasisComparesPriceTimesQuantity()
	{
		assertEquals(Collections.singletonList(ONYX_TIPS), qualifying(Collections.singletonList(ONYX_TIPS), LootValueBasis.STACK, 100_000, "", ""));
	}

	@Test
	public void dropBasisAddsEverythingUpAndListsIt()
	{
		List<Stack> drop = Arrays.asList(new Stack("Shark", 50, 800), new Stack("Coins", 70_000, 1));
		assertEquals(drop, qualifying(drop, LootValueBasis.DROP, 100_000, "", ""));
		assertTrue(qualifying(drop, LootValueBasis.DROP, 110_001, "", "").isEmpty());
	}

	@Test
	public void deniedStacksDontCountTowardTheDrop()
	{
		List<Stack> drop = Arrays.asList(ONYX_TIPS, LAWS);
		assertTrue(qualifying(drop, LootValueBasis.DROP, 100_000, "", "Onyx bolt tips").isEmpty());
		assertEquals(Collections.singletonList(LAWS), qualifying(drop, LootValueBasis.DROP, 1_000, "", "onyx*"));
	}

	@Test
	public void denylistBlocksAValuableStack()
	{
		assertTrue(qualifying(Collections.singletonList(ONYX_TIPS), LootValueBasis.STACK, 100_000, "", "Onyx bolt tips").isEmpty());
	}

	@Test
	public void denylistWinsOverAllowlist()
	{
		assertTrue(qualifying(Collections.singletonList(ONYX_TIPS), LootValueBasis.STACK, 100_000, "Onyx*", "*bolt tips").isEmpty());
	}

	@Test
	public void allowlistIgnoresPriceOnEveryBasis()
	{
		Stack shadow = new Stack("Tumeken's shadow (uncharged)", 1, 0);
		for (LootValueBasis basis : LootValueBasis.values())
		{
			assertEquals(Collections.singletonList(shadow), qualifying(Arrays.asList(shadow, LAWS), basis, 100_000, "Tumeken's shadow*", ""));
		}
	}

	@Test
	public void singleItemChatDropGetsThroughTheAllowlist()
	{
		Stack marlin = new Stack("Swift marlin", 1, 6_000);
		List<Stack> drop = Collections.singletonList(marlin);
		assertTrue(qualifying(drop, LootValueBasis.STACK, 100_000, "", "").isEmpty());
		assertEquals(drop, LootNotifier.qualifying(drop, "Deep sea trawling", 100_000, LootValueBasis.STACK, WildcardList.parse("Swift marlin"), WildcardList.parse("")));
	}

	@Test
	public void lootChestFallbackOnlyAppliesToItemAndStack()
	{
		assertEquals(Collections.singletonList(CHEST.get(0)), chestQualifying(LootValueBasis.ITEM, 100_000, ""));
		assertEquals(Collections.singletonList(CHEST.get(0)), chestQualifying(LootValueBasis.STACK, 100_000, ""));
		assertEquals(CHEST, chestQualifying(LootValueBasis.DROP, 100_000, ""));
		assertTrue(chestQualifying(LootValueBasis.DROP, 200_000, "").isEmpty());
	}

	@Test
	public void lootChestFallbackNeverPicksADeniedStack()
	{
		assertEquals(Collections.singletonList(CHEST.get(1)), chestQualifying(LootValueBasis.STACK, 60_000, "Shark"));
		assertTrue(chestQualifying(LootValueBasis.STACK, 100_000, "Shark").isEmpty());
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
		assertFalse(LootNotifier.isSkippedEvent(null));
	}

	@Test
	public void deniedSourcesMatchIgnoringCaseAndWithWildcards()
	{
		WildcardList denylist = WildcardList.parse("barrows\n*Tempoross*");
		assertTrue(LootNotifier.isDeniedSource("Barrows", LootRecordType.EVENT, denylist));
		assertTrue(LootNotifier.isDeniedSource("Reward pool (Tempoross)", LootRecordType.EVENT, denylist));
		assertFalse(LootNotifier.isDeniedSource("Vorkath", LootRecordType.NPC, denylist));
	}

	@Test
	public void sourceDenylistIgnoresPlayers()
	{
		WildcardList denylist = WildcardList.parse("Zezima");
		assertFalse(LootNotifier.isDeniedSource("Zezima", LootRecordType.PLAYER, denylist));
		assertTrue(LootNotifier.isDeniedSource("Zezima", LootRecordType.NPC, denylist));
	}

	@Test
	public void deniedSourceIsMatchedWithoutTags()
	{
		assertTrue(LootNotifier.isDeniedSource("<col=ff0000>Vorkath</col>", LootRecordType.NPC, WildcardList.parse("Vorkath")));
	}

	@Test
	public void emptySourceDenylistDeniesNothing()
	{
		assertFalse(LootNotifier.isDeniedSource("Vorkath", LootRecordType.NPC, WildcardList.parse("")));
	}

	@Test
	public void missingSourceIsNotAnError()
	{
		assertFalse(LootNotifier.isDeniedSource(null, LootRecordType.NPC, WildcardList.parse("Vorkath")));
	}
}
