package com.discordnotifications.notifiers;

import com.discordnotifications.CollectionLogRank;
import com.discordnotifications.KillCountService;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.WildcardList;
import com.discordnotifications.WorldUtils;
import net.runelite.api.ScriptID;
import net.runelite.api.WorldType;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class CollectionLogNotifier extends BaseNotifier
{
	static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<item>.*)");

	// Any other value that gives a message also shows the popup
	private static final int NEW_ITEM_CHAT_ONLY = 1;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private KillCountService killCountService;

	private final Set<String> entriesThisTick = new HashSet<>();
	private int entriesTick = -1;
	private int completed = 0;
	private Set<WorldType> countedWorldTypes = null;
	private boolean popupStarted = false;

	@Override
	public void reset()
	{
		entriesThisTick.clear();
		entriesTick = -1;
		completed = 0;
		countedWorldTypes = null;
		popupStarted = false;
	}

	public void onGameTick()
	{
		resetIfWorldChanged();

		// The varp lags our local count, max stops it pulling the count back
		completed = Math.max(completed, client.getVarpValue(VarPlayerID.COLLECTION_COUNT));
	}

	// Worlds with their own stats have their own log, max would keep the larger count
	private void resetIfWorldChanged()
	{
		Set<WorldType> worldTypes = WorldUtils.separateStatsTypes(client.getWorldType());
		if (!worldTypes.equals(countedWorldTypes))
		{
			completed = 0;
			countedWorldTypes = worldTypes;
		}
	}

	public void onGameMessage(String chatMessage)
	{
		if (!config.includeCollectionLogs() || client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM) != NEW_ITEM_CHAT_ONLY)
		{
			return;
		}

		String entry = parseEntry(chatMessage);
		if (entry != null)
		{
			queueEntry(entry);
		}
	}

	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		switch (scriptPreFired.getScriptId())
		{
			case ScriptID.NOTIFICATION_START:
				popupStarted = true;
				break;
			case ScriptID.NOTIFICATION_DELAY:
				if (!popupStarted)
				{
					return;
				}
				popupStarted = false;

				String topText = sanitize(client.getVarcStrValue(VarClientID.NOTIFICATION_TITLE));
				if (topText.equalsIgnoreCase("Collection log") && config.includeCollectionLogs())
				{
					queueEntry(popupSubject(client.getVarcStrValue(VarClientID.NOTIFICATION_MAIN)));
				}
				break;
		}
	}

	private void queueEntry(String entry)
	{
		int tick = client.getTickCount();
		if (tick != entriesTick)
		{
			entriesTick = tick;
			entriesThisTick.clear();
		}

		if (entriesThisTick.add(entry))
		{
			clientThread.invokeAtTickEnd(() -> sendEntry(entry));
		}
	}

	private void sendEntry(String entry)
	{
		// Before the denylist check, a skipped item still fills a slot
		if (completed > 0)
		{
			completed++;
		}

		if (WildcardList.parse(config.collectionLogDenylist()).matches(entry))
		{
			return;
		}

		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("entry", entry);

		CollectionLogRank rankUp = null;
		int total = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
		if (completed > 0 && total > 0)
		{
			String completedText = QuantityFormatter.formatNumber(completed);
			String totalText = QuantityFormatter.formatNumber(total);
			placeholders.put("completed", completedText);
			placeholders.put("total", totalText);
			placeholders.put("progress", " (" + completedText + "/" + totalText + ")");

			ToIntFunction<CollectionLogRank> thresholds = r -> r.threshold(client, total);
			CollectionLogRank rank = CollectionLogRank.at(completed, thresholds);
			if (rank != null)
			{
				placeholders.put("rank", rank.toString());
			}
			if (rank != CollectionLogRank.at(completed - 1, thresholds))
			{
				rankUp = rank;
			}
		}

		KillCountService.Drop drop = killCountService.getRecentDrop();
		if (drop != null && cameFrom(entry, itemNames(drop)))
		{
			placeholders.putAll(killCountService.sourcePlaceholders(drop));
		}

		send(NotificationType.COLLECTION_LOG, config.collectionLogMessage(), placeholders, config.sendCollectionLogScreenshot());

		// Its own notification because the queue drops the clog line when it merges into a loot or pet line
		if (rankUp != null)
		{
			send(Notification.builder()
				.type(NotificationType.COLLECTION_LOG_RANK)
				.text("$name has just reached " + rankUp + " rank in the collection log")
				.build());
		}
	}

	private List<String> itemNames(KillCountService.Drop drop)
	{
		return drop.getItems().stream()
			.map(item -> itemManager.getItemComposition(item.getId()).getMembersName())
			.collect(Collectors.toList());
	}

	// A drop set from chat has no items yet, pets are never part of a drop
	static boolean cameFrom(String entry, List<String> dropItems)
	{
		return dropItems.isEmpty() || PetNotifier.isPet(entry) || dropItems.stream().anyMatch(entry::equalsIgnoreCase);
	}

	static String parseEntry(String message)
	{
		Matcher matcher = COLLECTION_LOG_REGEX.matcher(message);
		return matcher.matches() ? matcher.group("item") : null;
	}
}
