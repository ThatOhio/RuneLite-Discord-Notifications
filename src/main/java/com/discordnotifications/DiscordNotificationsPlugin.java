package com.discordnotifications;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(name = "Discord Notifications")
public class DiscordNotificationsPlugin extends Plugin
{
	private final Map<String, Integer> currentLevels = new HashMap<>();
	private final Set<String> leveledSkills = new LinkedHashSet<>();
	private boolean shouldSendLevelMessage = false;
	private boolean shouldSendQuestMessage = false;
	private boolean shouldSendClueMessage = false;
	private boolean notificationStarted = false;
	private int ticksWaited = 0;

	private static final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: quest)?[!.]?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: quest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$", Pattern.CASE_INSENSITIVE);
	private static final ImmutableList<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
	private static final ImmutableList<String> WORD_QUEST_IN_NAME_TAGS =
		ImmutableList.of(
			"Another Cook",
			"Doric",
			"Heroes",
			"Legends",
			"Observatory",
			"Olaf",
			"Waterfall"
		);
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
		boolean didCompleteClue = client.getWidget(InterfaceID.TrailRewardscreen.ITEMS) != null;

		if (shouldSendClueMessage && didCompleteClue && config.includeClues()) {
			shouldSendClueMessage = false;
			sendClueMessage();
		}

		if (shouldSendQuestMessage
				&& config.includeQuestComplete()
				&& client.getWidget(InterfaceID.Questscroll.QUEST_TITLE) != null
		) {
			shouldSendQuestMessage = false;
			String text = client.getWidget(InterfaceID.Questscroll.QUEST_TITLE).getText();
			String questName = parseQuestCompletedWidget(text);
			sendQuestMessage(questName);
		}

		if (!shouldSendLevelMessage)
		{
			return;
		}

		if (ticksWaited < 2)
		{
			ticksWaited++;
			return;
		}

		shouldSendLevelMessage = false;
		ticksWaited = 0;
		sendLevelMessage();
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		String skillName = statChanged.getSkill().getName();
		int newLevel = statChanged.getLevel();

		// The first StatChanged per skill after login seeds the cache. A level that is lower than the
		// cached one is stale data, not a level-up, so only the cache is updated.
		Integer previousLevel = currentLevels.put(skillName, newLevel);
		if (previousLevel == null || previousLevel == 0 || newLevel <= previousLevel || !config.includeLevelling())
		{
			return;
		}

		// Certain activities can multilevel, check if any of the levels are valid for the message.
		for (int level = previousLevel + 1; level <= newLevel; level++)
		{
			if (shouldSendForThisLevel(level))
			{
				leveledSkills.add(skillName);
				shouldSendLevelMessage = true;
				break;
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String chatMessage = event.getMessage();
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
		int groupId = event.getGroupId();

		if (groupId == InterfaceID.QUESTSCROLL) {
			shouldSendQuestMessage = true;
		}

		if (groupId == InterfaceID.TRAIL_REWARDSCREEN) {
			shouldSendClueMessage = true;
		}
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

				String topText = client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT);
				String bottomText = client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT);

				if (topText.equalsIgnoreCase("Collection log")
						&& config.includeCollectionLogs())
				{
					String entry = Text.removeTags(bottomText).substring("New item:".length());
					sendCollectionLogMessage(entry);
				}

				if (topText.equalsIgnoreCase("Combat Task Completed!")
						&& config.includeCombatAchievements()
						&& client.getVarbitValue(Varbits.COMBAT_ACHIEVEMENTS_POPUP) == 0)
				{
					String[] s = bottomText.split("<.*?>");
					String task = s[1].replaceAll("[:?]", "");
					sendCombatAchievementMessage(task);
				}

				notificationStarted = false;
				break;
		}
	}

	private boolean shouldSendForThisLevel(int level) {
		return level >= config.minLevel()
				&& levelMeetsIntervalRequirement(level);
	}

	private boolean levelMeetsIntervalRequirement(int level) {
		int levelInterval = config.levelInterval();

		if (config.linearLevelMax() > 0) {
			levelInterval = (int) Math.max(Math.ceil(-.1*level + config.linearLevelMax()), 1);
		}

		return levelInterval <= 1
				|| level == 99
				|| level % levelInterval == 0;
	}

	private void sendQuestMessage(String questName)
	{
		String localName = client.getLocalPlayer().getName();

		String questMessageString = config.questMessage().replaceAll("\\$name", localName)
														 .replaceAll("\\$quest", questName);

		webhookService.sendMessage(questMessageString, config.sendQuestingScreenshot());
	}

	private void sendCombatAchievementMessage(String task) {
		String localName = client.getLocalPlayer().getName();

		String combatAchievementMessageString =
			config.combatAchievementsMessage()
				  .replaceAll("\\$name", localName)
				  .replaceAll("\\$achievement", task);

		webhookService.sendMessage(combatAchievementMessageString, config.sendCombatAchievementsScreenshot());
	}

	private void sendCollectionLogMessage(String entry) {
		String localName = client.getLocalPlayer().getName();

		String collectionLogMessageString =
			config.collectionLogMessage()
				.replaceAll("\\$name", localName)
				.replaceAll("\\$entry", entry);

		webhookService.sendMessage(collectionLogMessageString, config.sendCollectionLogScreenshot());
	}

	private void sendDeathMessage()
	{
		String localName = client.getLocalPlayer().getName();

		String deathMessageString = config.deathMessage().replaceAll("\\$name", localName);

		webhookService.sendMessage(deathMessageString, config.sendDeathScreenshot());
	}

	private void sendClueMessage()
	{
		String localName = client.getLocalPlayer().getName();

		String clueMessage = config.clueMessage().replaceAll("\\$name", localName);

		webhookService.sendMessage(clueMessage, config.sendClueScreenshot());
	}

	private void sendLevelMessage()
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (String skill : leveledSkills)
		{
			levels.put(skill, currentLevels.get(skill));
		}
		leveledSkills.clear();

		String message = buildLevelMessage(
			client.getLocalPlayer().getName(),
			levels,
			client.getTotalLevel(),
			config.levelMessage(),
			config.andLevelMessage(),
			config.includeTotalLevel() ? config.totalLevelMessage() : "");

		webhookService.sendMessage(message, config.sendLevellingScreenshot());
	}

	static String buildLevelMessage(String name, Map<String, Integer> levels, int totalLevel,
		String levelTemplate, String andTemplate, String totalTemplate)
	{
		StringBuilder message = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, Integer> entry : levels.entrySet())
		{
			message.append((first ? levelTemplate : andTemplate)
				.replace("$skill", entry.getKey())
				.replace("$level", entry.getValue().toString()));
			first = false;
		}
		message.append(totalTemplate);

		return message.toString()
			.replace("$name", name)
			.replace("$total", Integer.toString(totalLevel));
	}

	private void sendPetMessage()
	{
		String localName = client.getLocalPlayer().getName();

		String petMessageString = config.petMessage().replaceAll("\\$name", localName);

		webhookService.sendMessage(petMessageString, config.sendPetScreenshot());
	}

	private void resetState()
	{
		currentLevels.clear();
		leveledSkills.clear();
		shouldSendLevelMessage = false;
		shouldSendQuestMessage = false;
		shouldSendClueMessage = false;
		ticksWaited = 0;
	}

	static String parseQuestCompletedWidget(final String text)
	{
		// "You have completed The Corsair Curse!"
		final Matcher questMatch1 = QUEST_PATTERN_1.matcher(text);
		// "'One Small Favour' completed!"
		final Matcher questMatch2 = QUEST_PATTERN_2.matcher(text);
		final Matcher questMatchFinal = questMatch1.matches() ? questMatch1 : questMatch2;

		if (!questMatchFinal.matches())
		{
			return "Unable to find quest name!";
		}

		String quest = questMatchFinal.group("quest");
		String verb = questMatchFinal.group("verb") != null ? questMatchFinal.group("verb") : "";

		if (verb.contains("kind of"))
		{
			quest += " partial completion";
		}
		else if (verb.contains("completely"))
		{
			quest += " II";
		}

		if (RFD_TAGS.stream().anyMatch((quest + verb)::contains))
		{
			quest = "Recipe for Disaster - " + quest;
		}

		if (WORD_QUEST_IN_NAME_TAGS.stream().anyMatch(quest::contains))
		{
			quest += " Quest";
		}

		return quest;
	}
}
