package com.discordnotifications;

import com.discordnotifications.notifiers.ClueNotifier;
import com.discordnotifications.notifiers.LevelNotifier;
import com.discordnotifications.notifiers.QuestNotifier;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.regex.Pattern;

import static com.discordnotifications.notifiers.BaseNotifier.sanitize;

@Slf4j
@PluginDescriptor(name = "Discord Notifications")
public class DiscordNotificationsPlugin extends Plugin
{
	private boolean notificationStarted = false;

	private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile("New item added to your collection log:.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMBAT_TASK_REGEX = Pattern.compile("Congratulations, you've completed an? (?:\\w+) combat task:.*", Pattern.CASE_INSENSITIVE);

	private static final ImmutableList<String> PET_MESSAGES =
		ImmutableList.of(
			"You have a funny feeling like you're being followed",
			"You feel something weird sneaking into your backpack",
			"You have a funny feeling like you would have been followed"
		);

	@Inject
	private Client client;

	@Inject
	private DiscordNotificationsConfig config;

	@Inject
	private DiscordWebhookService webhookService;

	@Inject
	private LevelNotifier levelNotifier;

	@Inject
	private QuestNotifier questNotifier;

	@Inject
	private ClueNotifier clueNotifier;

	@Provides
	DiscordNotificationsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordNotificationsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		resetState();
	}

	@Override
	protected void shutDown() throws Exception
	{
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

		String chatMessage = sanitize(event.getMessage());
		if (config.includePets() && PET_MESSAGES.stream().anyMatch(chatMessage::contains))
		{
			sendPetMessage();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		if (!config.includeDeaths()) {
			return;
		}

		Actor actor = actorDeath.getActor();
		if (actor instanceof Player) {
			Player player = (Player) actor;
			if (player == client.getLocalPlayer())
			{
				sendDeathMessage();
			}
		}
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
		switch (scriptPreFired.getScriptId())
		{
			case ScriptID.NOTIFICATION_START:
				notificationStarted = true;
				break;
			case ScriptID.NOTIFICATION_DELAY:
				if (!notificationStarted)
				{
					return;
				}

				String topText = sanitize(client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT));
				String bottomText = client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT);

				if (topText.equalsIgnoreCase("Collection log")
						&& config.includeCollectionLogs())
				{
					sendCollectionLogMessage(popupSubject(bottomText));
				}

				if (topText.equalsIgnoreCase("Combat Task Completed!")
						&& config.includeCombatAchievements()
						&& client.getVarbitValue(Varbits.COMBAT_ACHIEVEMENTS_POPUP) == 0)
				{
					sendCombatAchievementMessage(popupSubject(bottomText));
				}

				notificationStarted = false;
				break;
		}
	}

	private void sendCombatAchievementMessage(String task)
	{
		sendMessage(config.combatAchievementsMessage().replace("$achievement", task), config.sendCombatAchievementsScreenshot());
	}

	private void sendCollectionLogMessage(String entry)
	{
		sendMessage(config.collectionLogMessage().replace("$entry", entry), config.sendCollectionLogScreenshot());
	}

	private void sendDeathMessage()
	{
		sendMessage(config.deathMessage(), config.sendDeathScreenshot());
	}

	private void sendPetMessage()
	{
		sendMessage(config.petMessage(), config.sendPetScreenshot());
	}

	// Same as BaseNotifier.send; goes away once the remaining four notifiers move into notifiers/.
	private void sendMessage(String message, boolean sendScreenshot)
	{
		Player player = client.getLocalPlayer();
		String localName = player != null ? player.getName() : null;
		if (Strings.isNullOrEmpty(localName))
		{
			log.debug("Dropping notification, local player name not available yet");
			return;
		}

		webhookService.sendMessage(message.replace("$name", localName), sendScreenshot);
	}

	// Popup body is "New item: <col=..>Item</col>" or "Task Completed: <col=..>Task</col>".
	static String popupSubject(String bottomText)
	{
		String text = sanitize(bottomText);
		int colon = text.indexOf(':');
		return colon < 0 ? text : text.substring(colon + 1).trim();
	}

	private void resetState()
	{
		levelNotifier.reset();
		questNotifier.reset();
		clueNotifier.reset();
	}
}
