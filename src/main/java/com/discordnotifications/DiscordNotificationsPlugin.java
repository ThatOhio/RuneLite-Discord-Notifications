package com.discordnotifications;

import com.discordnotifications.notifiers.ClueNotifier;
import com.discordnotifications.notifiers.CollectionLogNotifier;
import com.discordnotifications.notifiers.CombatAchievementNotifier;
import com.discordnotifications.notifiers.DeathNotifier;
import com.discordnotifications.notifiers.DiaryNotifier;
import com.discordnotifications.notifiers.HeldNotifications;
import com.discordnotifications.notifiers.KillCountNotifier;
import com.discordnotifications.notifiers.LeaguesNotifier;
import com.discordnotifications.notifiers.LevelNotifier;
import com.discordnotifications.notifiers.LootNotifier;
import com.discordnotifications.notifiers.ManualScreenshotNotifier;
import com.discordnotifications.notifiers.PetNotifier;
import com.discordnotifications.notifiers.QuestNotifier;
import com.google.common.base.Strings;
import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;

import javax.inject.Inject;

import static com.discordnotifications.notifiers.BaseNotifier.sanitize;

@PluginDescriptor(name = "Discord Notifications")
public class DiscordNotificationsPlugin extends Plugin
{
	@Inject
	private LevelNotifier levelNotifier;

	@Inject
	private QuestNotifier questNotifier;

	@Inject
	private ClueNotifier clueNotifier;

	@Inject
	private DeathNotifier deathNotifier;

	@Inject
	private PetNotifier petNotifier;

	@Inject
	private CollectionLogNotifier collectionLogNotifier;

	@Inject
	private CombatAchievementNotifier combatAchievementNotifier;

	@Inject
	private DiaryNotifier diaryNotifier;

	@Inject
	private KillCountNotifier killCountNotifier;

	@Inject
	private LootNotifier lootNotifier;

	@Inject
	private LeaguesNotifier leaguesNotifier;

	@Inject
	private ManualScreenshotNotifier manualScreenshotNotifier;

	@Inject
	private HeldNotifications heldNotifications;

	@Inject
	private KillCountService killCountService;

	@Inject
	private NotificationQueue notificationQueue;

	@Inject
	private GameSettingsWarning gameSettingsWarning;

	private GameState gameState;

	@Provides
	DiscordNotificationsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordNotificationsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		gameState = null;
		resetState();
		notificationQueue.start();
		manualScreenshotNotifier.start();
	}

	@Override
	protected void shutDown() throws Exception
	{
		manualScreenshotNotifier.stop();
		notificationQueue.shutdown();
	}

	@Subscribe
	public void onUsernameChanged(UsernameChanged usernameChanged)
	{
		resetState();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState newState = gameStateChanged.getGameState();
		// LOADING fires on every region change, then we land back in the same state
		if (newState == GameState.LOADING || newState == gameState)
		{
			return;
		}
		gameState = newState;

		diaryNotifier.onGameStateChanged(gameStateChanged);
		if (newState == GameState.LOGIN_SCREEN)
		{
			resetState();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		gameSettingsWarning.onConfigChanged(event);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		heldNotifications.onGameTick();
		clueNotifier.onGameTick();
		levelNotifier.onGameTick();
		petNotifier.onGameTick();
		collectionLogNotifier.onGameTick();
		lootNotifier.onGameTick();
		diaryNotifier.onGameTick();
		killCountNotifier.onGameTick();
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		levelNotifier.onStatChanged(statChanged);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		diaryNotifier.onVarbitChanged(event);
		gameSettingsWarning.onVarbitChanged(event);
	}

	@Subscribe(priority = 1) // before core's loot tracker, raid loot needs the mode from chat
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (type == ChatMessageType.CLAN_MESSAGE || type == ChatMessageType.CLAN_GUEST_MESSAGE || type == ChatMessageType.CLAN_GIM_MESSAGE)
		{
			petNotifier.onClanMessage(sanitize(event.getMessage()));
			return;
		}

		if (type == ChatMessageType.MESBOX)
		{
			diaryNotifier.onMessageBox(sanitize(event.getMessage()));
			return;
		}

		if (type == ChatMessageType.FRIENDSCHATNOTIFICATION)
		{
			killCountNotifier.onFriendsChatNotification(sanitize(event.getMessage()));
			return;
		}

		if (type == ChatMessageType.SPAM)
		{
			killCountService.notePickpocket(sanitize(event.getMessage()));
			return;
		}

		if (type != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String source = Strings.isNullOrEmpty(event.getName()) ? event.getSender() : event.getName();
		if ("runelite".equals(source))
		{
			return;
		}

		String message = sanitize(event.getMessage());
		killCountNotifier.onGameMessage(message, killCountService.onGameMessage(message));
		petNotifier.onGameMessage(message);
		collectionLogNotifier.onGameMessage(message);
		combatAchievementNotifier.onGameMessage(message);
		clueNotifier.onGameMessage(message);
		deathNotifier.onGameMessage(message);
		lootNotifier.onGameMessage(message);
		leaguesNotifier.onGameMessage(message);
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		deathNotifier.onActorDeath(actorDeath);
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		deathNotifier.onInteractingChanged(event);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		killCountService.onWidgetLoaded(event);
		questNotifier.onWidgetLoaded(event);
		clueNotifier.onWidgetLoaded(event);
		gameSettingsWarning.onWidgetLoaded(event);
	}

	// Before the loot tracker, so its saved kills don't already include this one
	@Subscribe(priority = 1)
	public void onServerNpcLoot(ServerNpcLoot event)
	{
		if (killCountService.onServerNpcLoot(event))
		{
			lootNotifier.onServerNpcLoot(event);
		}
	}

	@Subscribe(priority = 1)
	public void onNpcLootReceived(NpcLootReceived event)
	{
		if (killCountService.onNpcLootReceived(event))
		{
			lootNotifier.onNpcLootReceived(event);
		}
	}

	@Subscribe(priority = 1)
	public void onPlayerLootReceived(PlayerLootReceived event)
	{
		killCountService.onPlayerLootReceived(event);
		lootNotifier.onPlayerLootReceived(event);
	}

	@Subscribe(priority = 1)
	public void onLootReceived(LootReceived event)
	{
		killCountService.onLootReceived(event);
		lootNotifier.onLootReceived(event);
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		collectionLogNotifier.onScriptPreFired(scriptPreFired);
		petNotifier.onScriptPreFired(scriptPreFired);
		deathNotifier.onScriptPreFired(scriptPreFired);
	}

	private void resetState()
	{
		heldNotifications.reset();
		killCountService.reset();
		levelNotifier.reset();
		clueNotifier.reset();
		deathNotifier.reset();
		petNotifier.reset();
		collectionLogNotifier.reset();
		lootNotifier.reset();
		diaryNotifier.reset();
		killCountNotifier.reset();
	}
}
