package com.discordnotifications.notifiers;

import com.discordnotifications.DangerLevel;
import com.discordnotifications.ItemStackFormatter;
import com.discordnotifications.KillCountService;
import com.discordnotifications.LootValueBasis;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.WildcardList;
import com.discordnotifications.WorldUtils;
import com.google.common.collect.ImmutableMap;
import lombok.Value;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Singleton
public class LootNotifier extends BaseNotifier
{
	private static final String BA_GAMBLE = "Barbarian Assault high gamble";
	private static final String CLUE_SCROLL = "Clue Scroll";
	private static final String LOOT_CHEST = "Loot Chest";
	private static final String TRAWLING = "Deep sea trawling";

	// Core has no loot event for these, the chat line is all there is
	static final Map<String, ChatDrop> CHAT_DROPS = ImmutableMap.<String, ChatDrop>builder()
		.put("You have found a Pharaoh's sceptre! It fell on the floor.", new ChatDrop(ItemID.PHARAOHS_SCEPTRE, "Pyramid Plunder"))
		.put("You catch a giant blue krill!", new ChatDrop(ItemID.POH_TROPHYDROP_GIANT_KRILL, TRAWLING))
		.put("You catch a golden haddock!", new ChatDrop(ItemID.POH_TROPHYDROP_HADDOCK, TRAWLING))
		.put("You catch a orangefin!", new ChatDrop(ItemID.POH_TROPHYDROP_YELLOWFIN, TRAWLING))
		.put("You catch a huge halibut!", new ChatDrop(ItemID.POH_TROPHYDROP_HALIBUT, TRAWLING))
		.put("You catch a purplefin!", new ChatDrop(ItemID.POH_TROPHYDROP_BLUEFIN, TRAWLING))
		.put("You catch a swift marlin!", new ChatDrop(ItemID.POH_TROPHYDROP_MARLIN, TRAWLING))
		.build();

	@Inject
	private ItemStackFormatter itemStackFormatter;

	@Inject
	private KillCountService killCountService;

	@Inject
	private ChatMessageManager chatMessageManager;

	private final List<PendingLoot> pendingEventLoot = new ArrayList<>();
	private Map<Integer, Integer> wornLastTick = Collections.emptyMap();
	private boolean warnedLootTrackerOff;

	@Override
	public void reset()
	{
		pendingEventLoot.clear();
		wornLastTick = Collections.emptyMap();
	}

	public void onGameTick()
	{
		flushEventLoot();

		if (warnedLootTrackerOff || !config.includeLoot() || !killCountService.lootTrackerDisabled())
		{
			return;
		}

		warnedLootTrackerOff = true;
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage("Discord Notifications: turn on the Loot Tracker plugin to get notified for raids, chests and pickpocketing.")
			.build());
	}

	public void onNpcLootReceived(NpcLootReceived event)
	{
		String name = event.getNpc().getName();
		if (config.includeLoot() && KillCountService.usesNpcLootEvent(event.getNpc().getId(), name))
		{
			handleLoot(event.getItems(), name, LootRecordType.NPC);
		}
	}

	public void onServerNpcLoot(ServerNpcLoot event)
	{
		NPCComposition npc = event.getComposition();
		if (config.includeLoot() && KillCountService.usesServerLoot(npc.getId(), npc.getName()))
		{
			handleLoot(event.getItems(), npc.getName(), LootRecordType.NPC);
		}
	}

	public void onGameMessage(String message)
	{
		ChatDrop drop = CHAT_DROPS.get(message);
		if (drop != null && config.includeLoot())
		{
			handleLoot(Collections.singletonList(new ItemStack(drop.getItemId(), 1)), drop.getSource(), LootRecordType.EVENT);
		}
	}

	public void onPlayerLootReceived(PlayerLootReceived event)
	{
		String name = event.getPlayer().getName();
		if (config.includeLoot() && config.includePlayerLoot() && name != null && !inSafeArea())
		{
			handleLoot(event.getItems(), name, LootRecordType.PLAYER);
		}
	}

	public void onLootReceived(LootReceived event)
	{
		if (!config.includeLoot() || isSkippedEvent(event.getName()))
		{
			return;
		}

		// The loot tracker reposts every NPC and PvP kill, those came through the events above
		LootRecordType type = event.getType();
		if (type == LootRecordType.EVENT || type == LootRecordType.PICKPOCKET
			|| (type == LootRecordType.NPC && KillCountService.LOOT_RECEIVED_NPCS.contains(event.getName())))
		{
			String source = killCountService.getStandardizedSource(event);
			LootRecordType recordType = KillCountService.standardizedType(source, type);
			if (type == LootRecordType.EVENT)
			{
				// KC is read now, the loot tracker has saved this kill by the next tick
				pendingEventLoot.add(new PendingLoot(event.getItems(), source, killCountService.getKillCount(recordType, source)));
			}
			else
			{
				handleLoot(event.getItems(), source, recordType);
			}
		}
	}

	// Event loot is an inventory diff, so gear taken off on the same tick looks like loot
	private void flushEventLoot()
	{
		Map<Integer, Integer> worn = wornItems();
		Map<Integer, Integer> unequipped = removedItems(wornLastTick, worn);
		wornLastTick = worn;

		for (PendingLoot loot : pendingEventLoot)
		{
			handleLoot(withoutUnequipped(loot.getItems(), unequipped), loot.getSource(), loot::getKc);
		}
		pendingEventLoot.clear();
	}

	private Map<Integer, Integer> wornItems()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return Collections.emptyMap();
		}

		Map<Integer, Integer> quantities = new HashMap<>();
		for (Item item : worn.getItems())
		{
			if (item.getQuantity() > 0)
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return quantities;
	}

	static Map<Integer, Integer> removedItems(Map<Integer, Integer> before, Map<Integer, Integer> after)
	{
		Map<Integer, Integer> removed = new HashMap<>();
		before.forEach((id, quantity) ->
		{
			int gone = quantity - after.getOrDefault(id, 0);
			if (gone > 0)
			{
				removed.put(id, gone);
			}
		});
		return removed;
	}

	// Takes the count and not the whole stack, you can take off one tome and win another
	static List<ItemStack> withoutUnequipped(Collection<ItemStack> items, Map<Integer, Integer> unequipped)
	{
		List<ItemStack> kept = new ArrayList<>(items.size());
		for (ItemStack item : items)
		{
			int taken = Math.min(item.getQuantity(), unequipped.getOrDefault(item.getId(), 0));
			unequipped.merge(item.getId(), -taken, Integer::sum);
			if (item.getQuantity() > taken)
			{
				kept.add(new ItemStack(item.getId(), item.getQuantity() - taken));
			}
		}
		return kept;
	}

	private boolean inSafeArea()
	{
		Player player = client.getLocalPlayer();
		return player == null
			|| WorldUtils.getDangerLevel(client, WorldUtils.getLocation(client, player).getRegionID()) == DangerLevel.SAFE;
	}

	private void handleLoot(Collection<ItemStack> items, String source, LootRecordType type)
	{
		handleLoot(items, source, () -> killCountService.getKillCount(type, source));
	}

	private void handleLoot(Collection<ItemStack> items, String source, Supplier<Integer> kc)
	{
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		for (ItemStack item : items)
		{
			quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
		}

		List<ItemStackFormatter.Stack> stacks = new ArrayList<>(quantities.size());
		quantities.forEach((id, quantity) -> stacks.add(itemStackFormatter.stackWithStorePrice(id, quantity)));

		List<ItemStackFormatter.Stack> qualifying = qualifying(stacks, source, config.minLootValue(), config.lootValueBasis(),
			WildcardList.parse(config.lootAllowlist()), WildcardList.parse(config.lootDenylist()));
		if (qualifying.isEmpty())
		{
			return;
		}

		send(Notification.builder()
			.type(NotificationType.LOOT)
			.text(config.lootMessage())
			.placeholders(placeholders(qualifying, stacks, sanitize(source), kc.get()))
			.itemNames(qualifying.stream().map(ItemStackFormatter.Stack::getName).collect(Collectors.toList()))
			.screenshot(config.sendLootScreenshot())
			.build());
	}

	static boolean isSkippedEvent(String name)
	{
		return BA_GAMBLE.equals(name) || name.regionMatches(true, 0, CLUE_SCROLL, 0, CLUE_SCROLL.length());
	}

	static List<ItemStackFormatter.Stack> qualifying(List<ItemStackFormatter.Stack> stacks, String source, long minValue, LootValueBasis basis, WildcardList allowlist, WildcardList denylist)
	{
		List<ItemStackFormatter.Stack> eligible = stacks.stream()
			.filter(stack -> !denylist.matches(stack.getName()))
			.collect(Collectors.toList());

		if (basis == LootValueBasis.DROP && ItemStackFormatter.totalValue(eligible) >= minValue)
		{
			return eligible;
		}

		List<ItemStackFormatter.Stack> qualifying = eligible.stream()
			.filter(stack -> allowlist.matches(stack.getName())
				|| (basis == LootValueBasis.ITEM ? stack.getUnitPrice() : stack.getTotalPrice()) >= minValue)
			.collect(Collectors.toList());

		return qualifying.isEmpty() && basis != LootValueBasis.DROP && LOOT_CHEST.equalsIgnoreCase(source)
			? lootChestStandIn(eligible, minValue)
			: qualifying;
	}

	// PK loot keys hold lots of small stacks, so the whole chest counts and its best stack gets listed
	static List<ItemStackFormatter.Stack> lootChestStandIn(List<ItemStackFormatter.Stack> stacks, long minValue)
	{
		if (ItemStackFormatter.totalValue(stacks) < minValue)
		{
			return Collections.emptyList();
		}

		return stacks.stream()
			.max(Comparator.comparingLong(ItemStackFormatter.Stack::getTotalPrice))
			.map(Collections::singletonList)
			.orElse(Collections.emptyList());
	}

	static Map<String, String> placeholders(List<ItemStackFormatter.Stack> qualifying, List<ItemStackFormatter.Stack> drop, String source, Integer kc)
	{
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("items", ItemStackFormatter.format(qualifying));
		placeholders.put("value", QuantityFormatter.quantityToStackSize(ItemStackFormatter.totalValue(drop)));
		placeholders.put("source", source);
		placeholders.put("kc", KillCountService.kcText(kc));
		return placeholders;
	}

	@Value
	private static class PendingLoot
	{
		Collection<ItemStack> items;
		String source;
		Integer kc;
	}

	@Value
	static class ChatDrop
	{
		int itemId;
		String source;
	}
}
