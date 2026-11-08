package com.discordnotifications.notifiers;

import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.WildcardList;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class CollectionLogNotifier extends BaseNotifier
{
	static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<item>.*)");

	// Any other value that gives a message also shows the popup
	private static final int NEW_ITEM_CHAT_ONLY = 1;

	@Inject
	private ClientThread clientThread;

	private final Set<String> entriesThisTick = new HashSet<>();
	private int entriesTick = -1;
	private int completed = 0;
	private boolean popupStarted = false;

	@Override
	public void reset()
	{
		entriesThisTick.clear();
		entriesTick = -1;
		completed = 0;
		popupStarted = false;
	}

	public void onGameTick()
	{
		// The varp lags our local count, max stops it pulling the count back
		completed = Math.max(completed, client.getVarpValue(VarPlayerID.COLLECTION_COUNT));
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

				String topText = sanitize(client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT));
				if (topText.equalsIgnoreCase("Collection log") && config.includeCollectionLogs())
				{
					queueEntry(popupSubject(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT)));
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

		int total = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
		if (completed > 0 && total > 0)
		{
			String completedText = QuantityFormatter.formatNumber(completed);
			String totalText = QuantityFormatter.formatNumber(total);
			placeholders.put("completed", completedText);
			placeholders.put("total", totalText);
			placeholders.put("progress", " (" + completedText + "/" + totalText + ")");
		}

		send(Notification.builder()
			.type(NotificationType.COLLECTION_LOG)
			.text(config.collectionLogMessage())
			.placeholders(placeholders)
			.screenshot(config.sendCollectionLogScreenshot())
			.build());
	}

	static String parseEntry(String message)
	{
		Matcher matcher = COLLECTION_LOG_REGEX.matcher(message);
		return matcher.matches() ? matcher.group("item") : null;
	}
}
