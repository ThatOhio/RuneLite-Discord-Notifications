package com.discordnotifications.notifiers;

import com.discordnotifications.DangerLevel;
import com.discordnotifications.ItemStackFormatter;
import com.discordnotifications.KillCountService;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.WorldUtils;
import com.google.common.collect.ImmutableMap;
import lombok.Value;
import net.runelite.api.ChatMessageType;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
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

	private boolean warnedLootTrackerOff;

	public void onGameTick()
	{
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
			handleLoot(event.getItems(), source, KillCountService.standardizedType(source, type));
		}
	}

	private boolean inSafeArea()
	{
		Player player = client.getLocalPlayer();
		return player == null
			|| WorldUtils.getDangerLevel(client, WorldUtils.getLocation(client, player).getRegionID()) == DangerLevel.SAFE;
	}

	private void handleLoot(Collection<ItemStack> items, String source, LootRecordType type)
	{
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		for (ItemStack item : items)
		{
			quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
		}

		List<ItemStackFormatter.Stack> stacks = new ArrayList<>(quantities.size());
		quantities.forEach((id, quantity) -> stacks.add(itemStackFormatter.stackWithStorePrice(id, quantity)));

		List<ItemStackFormatter.Stack> qualifying = qualifying(stacks, config.minLootValue());
		if (qualifying.isEmpty() && LOOT_CHEST.equalsIgnoreCase(source))
		{
			qualifying = lootChestStandIn(stacks, config.minLootValue());
		}

		if (qualifying.isEmpty())
		{
			return;
		}

		send(Notification.builder()
			.type(NotificationType.LOOT)
			.text(config.lootMessage())
			.placeholders(placeholders(qualifying, stacks, sanitize(source), killCountService.getKillCount(type, source)))
			.screenshot(config.sendLootScreenshot())
			.build());
	}

	static boolean isSkippedEvent(String name)
	{
		return BA_GAMBLE.equals(name) || name.regionMatches(true, 0, CLUE_SCROLL, 0, CLUE_SCROLL.length());
	}

	static List<ItemStackFormatter.Stack> qualifying(List<ItemStackFormatter.Stack> stacks, long minValue)
	{
		return stacks.stream().filter(stack -> stack.getTotalPrice() >= minValue).collect(Collectors.toList());
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
		placeholders.put("kc", kc == null ? "" : " (KC " + QuantityFormatter.formatNumber(kc) + ")");
		return placeholders;
	}

	@Value
	static class ChatDrop
	{
		int itemId;
		String source;
	}
}
