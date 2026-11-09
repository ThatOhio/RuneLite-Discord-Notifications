package com.discordnotifications;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.VarbitChanged;
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
			warn(config.includeCollectionLogs(), config.includePets(), readsKillCounts(), config.includeCombatAchievements());
		}
	}

	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()) || !"true".equals(event.getNewValue()) || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		String key = event.getKey();
		boolean collectionLog = key.equals("includeCollectionLogs");
		boolean pets = key.equals("includePets");
		boolean killCountFilter = collectionLog || pets || key.equals("includeLoot") || key.equals("includeKillCounts");
		boolean combatAchievements = key.equals("includeCombatAchievements");
		// Config changes don't arrive on the client thread
		clientThread.invokeLater(() -> warn(collectionLog, pets, killCountFilter, combatAchievements));
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarbitId();
		boolean collectionLog = id == VarbitID.OPTION_COLLECTION_NEW_ITEM && config.includeCollectionLogs();
		boolean pets = (id == VarbitID.OPTION_LOOTNOTIFICATION_ON || id == VarbitID.OPTION_LOOTNOTIFICATION_UNTRADEABLES) && config.includePets();
		boolean killCountFilter = id == VarbitID.BOSS_KILLCOUNT_FILTERED && readsKillCounts();
		boolean combatAchievements = id == VarbitID.CA_TASK_RECOMPLETION_NOTIFICATIONS && config.includeCombatAchievements();

		if ((collectionLog || pets || killCountFilter || combatAchievements) && WorldUtils.isVisible(client, InterfaceID.Settings.UNIVERSE))
		{
			warn(collectionLog, pets, killCountFilter, combatAchievements);
		}
	}

	private boolean readsKillCounts()
	{
		return config.includeLoot() || config.includePets() || config.includeCollectionLogs() || config.includeKillCounts();
	}

	private void warn(boolean collectionLog, boolean pets, boolean killCountFilter, boolean combatAchievements)
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
		if (killCountFilter && client.getVarbitValue(VarbitID.BOSS_KILLCOUNT_FILTERED) > 0)
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
