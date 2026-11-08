package com.discordnotifications;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("discordnotifications")
public interface DiscordNotificationsConfig extends Config {

	// Webhook config section
	@ConfigSection(
			name = "Webhook Settings",
			description = "The config for webhook content notifications",
			position = 0,
			closedByDefault = true
	)
	String webhookConfig = "webhookConfig";

	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL(s)",
			description = "The Discord Webhook URL(s) to send messages to. Put each on its own line, or separate them with commas.",
			section = webhookConfig,
			position = 0
	)
	String webhook();

	@ConfigItem(
			keyName = "chatPrivacy",
			name = "Hide chat in screenshots",
			description = "What to hide when taking a screenshot. Hide split PMs only does something if you have split private chat turned on in game. The chat might flicker for a frame while it's hidden.",
			section = webhookConfig,
			position = 1
	)
	default ChatPrivacy chatPrivacy() {
		return ChatPrivacy.HIDE_SPLIT_PM;
	}

	@ConfigItem(
			keyName = "seasonalWorlds",
			name = "Leagues and Deadman worlds",
			description = "Notify puts [Leagues] or [DMM] at the start of each message. Ignore doesn't send anything from those worlds.",
			section = webhookConfig,
			position = 2
	)
	default SeasonalWorlds seasonalWorlds() {
		return SeasonalWorlds.NOTIFY;
	}

	// Levelling config section
	@ConfigSection(
			name = "Levelling",
			description = "The config for levelling notifications",
			position = 1,
			closedByDefault = true
	)
	String levellingConfig = "levellingConfig";

	@ConfigItem(
			keyName = "includeLevelling",
			name = "Send Levelling Notifications",
			description = "Send messages when you level up a skill.",
			section = levellingConfig,
			position = 1
	)
	default boolean includeLevelling() {
		return false;
	}

	@ConfigItem(
			keyName = "minimumLevel",
			name = "Minimum level",
			description = "Levels greater than or equal to this value will send a message.",
			section = levellingConfig,
			position = 2
	)
	default int minLevel() {
		return 0;
	}

	@ConfigItem(
			keyName = "levelInterval",
			name = "Send every X levels",
			description = "Only levels that are a multiple of this value are sent. Level 99 will always be sent regardless of this value.",
			section = levellingConfig,
			position = 3
	)
	default int levelInterval() {
		return 1;
	}

	@ConfigItem(
			keyName = "linearLevelModifier",
			name = "Linear Level Modifier",
			description = "Send every `max(-.1x + linearLevelMax, 1)` levels. Will override `Send every X levels` if set to above zero.",
			section = levellingConfig,
			position = 4
	)
	default double linearLevelMax() {
		return 0;
	}

	@ConfigItem(
			keyName = "sendLevellingScreenshot",
			name = "Include levelling screenshots",
			description = "Include a screenshot when leveling up.",
			section = levellingConfig,
			position = 100
	)
	default boolean sendLevellingScreenshot() {
		return false;
	}
	// End levelling config section

	// Questing config section
	@ConfigSection(
			name = "Questing",
			description = "The config for questing notifications",
			position = 2,
			closedByDefault = true
	)
	String questingConfig = "questingConfig";

	@ConfigItem(
			keyName = "includeQuests",
			name = "Send Quest Notifications",
			description = "Send messages when you complete a quest.",
			section = questingConfig
	)
	default boolean includeQuestComplete() {
		return false;
	}

	@ConfigItem(
			keyName = "sendQuestingScreenshot",
			name = "Include quest screenshots",
			description = "Include a screenshot with the Discord notification when leveling up.",
			section = questingConfig,
			position = 100
	)
	default boolean sendQuestingScreenshot() {
		return false;
	}
	// End questing config section

	// Death config section
	@ConfigSection(
			name = "Deaths",
			description = "The config for death notifications",
			position = 3,
			closedByDefault = true
	)
	String deathConfig = "deathConfig";

	@ConfigItem(
			keyName = "includeDeaths",
			name = "Send Death Notifications",
			description = "Send messages when you die to Discord.",
			section = deathConfig
	)
	default boolean includeDeaths() { return false; }

	@ConfigItem(
			keyName = "sendDeathScreenshot",
			name = "Include death screenshots",
			description = "Include a screenshot with the Discord notification when you die.",
			section = deathConfig,
			position = 100
	)
	default boolean sendDeathScreenshot() {
		return false;
	}
	// End death config section

	// Clue config section
	@ConfigSection(
			name = "Clue Scrolls",
			description = "The config for clue scroll notifications",
			position = 4,
			closedByDefault = true
	)
	String clueConfig = "clueConfig";

	@ConfigItem(
			keyName = "includeClues",
			name = "Send Clue Notifications",
			description = "Send messages when you complete a clue scroll.",
			section = clueConfig
	)
	default boolean includeClues() { return false; }

	@ConfigItem(
			keyName = "sendClueScreenshot",
			name = "Include Clue screenshots",
			description = "Include a screenshot with the Discord notification when you complete a clue.",
			section = clueConfig,
			position = 100
	)
	default boolean sendClueScreenshot() {
		return false;
	}
	// End clue config section

	// Pet config section
	@ConfigSection(
			name = "Pets",
			description = "The config for pet notifications",
			position = 5,
			closedByDefault = true
	)
	String petConfig = "petConfig";

	@ConfigItem(
			keyName = "includePets",
			name = "Send Pet Notifications",
			description = "Send messages when you receive a pet.",
			section = petConfig
	)
	default boolean includePets() { return false; }

	@ConfigItem(
			keyName = "notifyDuplicatePets",
			name = "Notify duplicate pets",
			description = "Also send a message when you would have got a pet you already have.",
			section = petConfig,
			position = 1
	)
	default boolean notifyDuplicatePets() { return true; }

	@ConfigItem(
			keyName = "sendPetScreenshot",
			name = "Include Pet screenshots",
			description = "Include a screenshot with the Discord notification when you receive a pet.",
			section = petConfig,
			position = 100
	)
	default boolean sendPetScreenshot() {
		return false;
	}

	@ConfigSection(
		name = "Collection logs",
		description = "The config for collection logs",
		position = 6,
		closedByDefault = true
	)
	String collectionLogsConfig = "collectionLogsConfig";

	@ConfigItem(
		keyName = "includeCollectionLogs",
		name = "Collection Log Notifications",
		description = "Message to send to Discord on collection logs completions",
		section = collectionLogsConfig,
		position = 1
	)
	default boolean includeCollectionLogs() { return false; }

	@ConfigItem(
		keyName = "collectionLogDenylist",
		name = "Never notify for these items",
		description = "Items that shouldn't send a message. Put each on its own line, or separate them with commas. Use * as a wildcard, like Clue scroll*",
		section = collectionLogsConfig,
		position = 2
	)
	default String collectionLogDenylist() { return ""; }

	@ConfigItem(
		keyName = "sendCollectionLogScreenshot",
		name = "Include collection log screenshots",
		description = "Include a screenshot with the Discord notification when you fill a new collection log slot",
		section = collectionLogsConfig,
		position = 3
	)
	default boolean sendCollectionLogScreenshot() {
		return false;
	}

	@ConfigSection(
		name = "Combat Achievements",
		description = "The config for combat achievements",
		position = 6,
		closedByDefault = true
	)
	String combatAchievementsConfig = "combatAchievementsConfig";

	@ConfigItem(
		keyName = "includeCombatAchievements",
		name = "Combat Achievements Notifications",
		description = "Message to send to Discord on combat achievements completions",
		section = combatAchievementsConfig,
		position = 1
	)
	default boolean includeCombatAchievements() { return false; }

	@ConfigItem(
		keyName = "minCombatAchievementTier",
		name = "Minimum tier",
		description = "Only tasks of this tier or higher will send a message.",
		section = combatAchievementsConfig,
		position = 2
	)
	default CombatAchievementTier minCombatAchievementTier() { return CombatAchievementTier.EASY; }

	@ConfigItem(
		keyName = "sendCombatAchievementScreenshot",
		name = "Include combat achievements screenshots",
		description = "Include a screenshot with the Discord notification when you complete a combat achievement",
		section = combatAchievementsConfig,
		position = 100
	)
	default boolean sendCombatAchievementsScreenshot() {
		return false;
	}

	@ConfigSection(
		name = "Webhook overrides",
		description = "Send an event to its own channel instead of the main webhook. Leave one blank to use the main webhook. Put each URL on its own line if you want more than one.",
		position = 999,
		closedByDefault = true
	)
	String webhookOverridesConfig = "webhookOverridesConfig";

	@ConfigItem(
		keyName = "levelWebhook",
		name = "Levels",
		description = "Webhook URL(s) for level ups.",
		section = webhookOverridesConfig,
		position = 1
	)
	default String levelWebhook() { return ""; }

	@ConfigItem(
		keyName = "questWebhook",
		name = "Quests",
		description = "Webhook URL(s) for quests.",
		section = webhookOverridesConfig,
		position = 2
	)
	default String questWebhook() { return ""; }

	@ConfigItem(
		keyName = "deathWebhook",
		name = "Deaths",
		description = "Webhook URL(s) for deaths.",
		section = webhookOverridesConfig,
		position = 3
	)
	default String deathWebhook() { return ""; }

	@ConfigItem(
		keyName = "clueWebhook",
		name = "Clue scrolls",
		description = "Webhook URL(s) for clue scrolls.",
		section = webhookOverridesConfig,
		position = 4
	)
	default String clueWebhook() { return ""; }

	@ConfigItem(
		keyName = "petWebhook",
		name = "Pets",
		description = "Webhook URL(s) for pets.",
		section = webhookOverridesConfig,
		position = 5
	)
	default String petWebhook() { return ""; }

	@ConfigItem(
		keyName = "collectionLogWebhook",
		name = "Collection log",
		description = "Webhook URL(s) for collection log slots.",
		section = webhookOverridesConfig,
		position = 6
	)
	default String collectionLogWebhook() { return ""; }

	@ConfigItem(
		keyName = "combatAchievementWebhook",
		name = "Combat achievements",
		description = "Webhook URL(s) for combat achievements.",
		section = webhookOverridesConfig,
		position = 7
	)
	default String combatAchievementWebhook() { return ""; }

	@ConfigSection(
		name = "Advanced: message templates",
		description = "Change the text of each message",
		position = 1000,
		closedByDefault = true
	)
	String messageTemplatesConfig = "messageTemplatesConfig";

	@ConfigItem(
		keyName = "levelTemplate",
		name = "Level Message",
		description = "Sent when you level up. Use $name, $skills (like Attack to 70 and Strength to 72) and $total. $skill and $level only give the first skill.",
		section = messageTemplatesConfig,
		position = 1
	)
	default String levelMessage() { return "$name leveled $skills - Total Level: $total"; }

	@ConfigItem(
		keyName = "questMessage",
		name = "Quest Message",
		description = "Sent when you finish a quest. Use $name and $quest.",
		section = messageTemplatesConfig,
		position = 2
	)
	default String questMessage() { return "$name has just completed: $quest"; }

	@ConfigItem(
		keyName = "deathMessage",
		name = "Death Message",
		description = "Sent when you die. Use $name.",
		section = messageTemplatesConfig,
		position = 3
	)
	default String deathMessage() { return "$name has just died!"; }

	@ConfigItem(
		keyName = "clueMessage",
		name = "Clue Message",
		description = "Sent when you finish a clue scroll. Use $name.",
		section = messageTemplatesConfig,
		position = 4
	)
	default String clueMessage() { return "$name has just completed a clue scroll!"; }

	@ConfigItem(
		keyName = "petTemplate",
		name = "Pet Message",
		description = "Sent when you get a pet. Use $name, $pet, $kc (like: at 1,234 kills) and $duplicate. Put $kc and $duplicate straight after another word, they add their own space and are blank when unknown.",
		section = messageTemplatesConfig,
		position = 5
	)
	default String petMessage() { return "$name just got a pet: $pet$kc$duplicate"; }

	@ConfigItem(
		keyName = "collectionLogMessage",
		name = "Collection log Message",
		description = "Sent when you fill a collection log slot. Use $name, $entry, $completed, $total and $progress (like: (123/1,692)). Put $progress straight after another word, it adds its own space. The counts are blank if the game hasn't sent them yet, opening the Character Summary tab fixes that.",
		section = messageTemplatesConfig,
		position = 6
	)
	default String collectionLogMessage() { return "$name has just completed a collection log: $entry$progress"; }

	@ConfigItem(
		keyName = "combatAchievementsMessage",
		name = "Combat Achievement Message",
		description = "Sent when you finish a combat task. Use $name, $task, $tier, $points (what the task was worth), $totalPoints and $tierUnlocked (like: and unlocked the Hard tier). Put $tierUnlocked straight after another word, it adds its own space and is blank unless the task got you to a new tier.",
		section = messageTemplatesConfig,
		position = 7
	)
	default String combatAchievementsMessage() { return "$name has just completed a combat achievement: $task ($tier)$tierUnlocked"; }
}
