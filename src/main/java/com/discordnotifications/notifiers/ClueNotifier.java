package com.discordnotifications.notifiers;

import com.discordnotifications.ClueTier;
import com.discordnotifications.ItemStackFormatter;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import lombok.Value;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class ClueNotifier extends BaseNotifier
{
	private static final Pattern CLUE_REGEX = Pattern.compile("You have completed (?<count>\\d+) (?<tier>\\w+) Treasure Trails?\\.");

	@Inject
	private ItemStackFormatter itemStackFormatter;

	private CompletedClue pending;
	private int badTicks;

	@Override
	public void reset()
	{
		pending = null;
		badTicks = 0;
	}

	public void onGameMessage(String chatMessage)
	{
		if (!config.includeClues())
		{
			return;
		}

		CompletedClue completed = parse(chatMessage);
		if (completed != null && completed.getTier().ordinal() >= config.minClueTier().ordinal())
		{
			pending = completed;
			badTicks = 0;
		}
	}

	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.TRAIL_REWARDSCREEN || pending == null)
		{
			return;
		}

		Widget items = client.getWidget(InterfaceID.TrailRewardscreen.ITEMS);
		Widget[] children = items == null ? null : items.getChildren();
		if (children != null)
		{
			sendClue(pending, rewards(children));
			reset();
		}
	}

	// The chat message comes first, so a screen that never loaded can't pair with the next clue
	public void onGameTick()
	{
		if (pending != null && ++badTicks > 1)
		{
			reset();
		}
	}

	private List<ItemStackFormatter.Stack> rewards(Widget[] children)
	{
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		for (Widget child : children)
		{
			if (child != null && child.getItemId() > -1 && child.getItemQuantity() > 0)
			{
				quantities.merge(child.getItemId(), child.getItemQuantity(), Integer::sum);
			}
		}

		return quantities.entrySet().stream()
			.map(entry -> itemStackFormatter.stack(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList());
	}

	private void sendClue(CompletedClue completed, List<ItemStackFormatter.Stack> rewards)
	{
		long value = ItemStackFormatter.totalValue(rewards);
		if (value < config.minClueValue())
		{
			return;
		}

		send(Notification.builder()
			.type(NotificationType.CLUE)
			.text(config.clueMessage())
			.placeholders(placeholders(completed, rewards))
			.screenshot(config.sendClueScreenshot() && value >= config.clueScreenshotMinValue())
			.build());
	}

	static CompletedClue parse(String message)
	{
		Matcher matcher = CLUE_REGEX.matcher(message);
		if (!matcher.find())
		{
			return null;
		}

		ClueTier tier = ClueTier.fromName(matcher.group("tier"));
		return tier == null ? null : new CompletedClue(tier, Integer.parseInt(matcher.group("count")));
	}

	static Map<String, String> placeholders(CompletedClue completed, List<ItemStackFormatter.Stack> rewards)
	{
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("tier", completed.getTier().toString());
		placeholders.put("count", QuantityFormatter.formatNumber(completed.getCount()));
		placeholders.put("items", ItemStackFormatter.format(rewards));
		placeholders.put("value", QuantityFormatter.quantityToStackSize(ItemStackFormatter.totalValue(rewards)));
		return placeholders;
	}

	@Value
	static class CompletedClue
	{
		ClueTier tier;
		int count;
	}
}
