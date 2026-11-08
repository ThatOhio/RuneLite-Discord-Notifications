package com.discordnotifications;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.events.ConfigChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class GameSettingsWarning
{
	private static final String CONFIG_GROUP = "discordnotifications";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DiscordNotificationsConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.SETTINGS)
		{
			warn(config.includeCollectionLogs(), config.includePets(), config.includeLoot(), config.includeCombatAchievements(), config.includeKillCounts());
		}
	}

	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()) || !"true".equals(event.getNewValue()) || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		String key = event.getKey();
		// Config changes don't arrive on the client thread
		clientThread.invokeLater(() -> warn(key.equals("includeCollectionLogs"), key.equals("includePets"), key.equals("includeLoot"), key.equals("includeCombatAchievements"), key.equals("includeKillCounts")));
	}

	private void warn(boolean collectionLog, boolean pets, boolean loot, boolean combatAchievements, boolean killCounts)
	{
		List<String> problems = new ArrayList<>();
		if (collectionLog && client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM) == 0)
		{
			problems.add("Turn on Collection log - New addition notification or new log slots won't post.");
		}
		if (pets && (client.getVarbitValue(VarbitID.OPTION_LOOTNOTIFICATION_ON) == 0 || client.getVarbitValue(VarbitID.OPTION_LOOTNOTIFICATION_UNTRADEABLES) == 0))
		{
			problems.add("Turn on Loot drop notifications and Untradeable loot notifications or pets can post without a name.");
		}
		if ((loot || pets || collectionLog || killCounts) && client.getVarbitValue(VarbitID.BOSS_KILLCOUNT_FILTERED) > 0)
		{
			problems.add("Turn off Filter out boss kill-count with spam-filter or kill counts can be missing from posts.");
		}
		if (combatAchievements && client.getVarbitValue(VarbitID.CA_TASK_RECOMPLETION_NOTIFICATIONS) > 0)
		{
			problems.add("Turn off Combat Achievement Tasks - Repeat completion or repeated tasks will post again.");
		}

		if (!problems.isEmpty())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage("Discord Notifications: " + String.join(" ", problems))
				.build());
		}
	}
}
