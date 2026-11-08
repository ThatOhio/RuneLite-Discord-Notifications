package com.discordnotifications;

import com.discordnotifications.notifiers.ClueNotifier;
import com.discordnotifications.notifiers.CollectionLogNotifier;
import com.discordnotifications.notifiers.CombatAchievementNotifier;
import com.discordnotifications.notifiers.DeathNotifier;
import com.discordnotifications.notifiers.LevelNotifier;
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
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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
	private NotificationQueue notificationQueue;

	@Provides
	DiscordNotificationsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordNotificationsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		resetState();
		notificationQueue.start();
	}

	@Override
	protected void shutDown() throws Exception
	{
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
		if (gameStateChanged.getGameState().equals(GameState.LOGIN_SCREEN))
		{
			resetState();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		clueNotifier.onGameTick();
		questNotifier.onGameTick();
		levelNotifier.onGameTick();
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		levelNotifier.onStatChanged(statChanged);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String source = Strings.isNullOrEmpty(event.getName()) ? event.getSender() : event.getName();
		if ("runelite".equals(source))
		{
			return;
		}

		petNotifier.onGameMessage(sanitize(event.getMessage()));
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		deathNotifier.onActorDeath(actorDeath);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		questNotifier.onWidgetLoaded(event);
		clueNotifier.onWidgetLoaded(event);
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		collectionLogNotifier.onScriptPreFired(scriptPreFired);
		combatAchievementNotifier.onScriptPreFired(scriptPreFired);
	}

	private void resetState()
	{
		levelNotifier.reset();
		questNotifier.reset();
		clueNotifier.reset();
		collectionLogNotifier.reset();
		combatAchievementNotifier.reset();
	}
}
