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
			position = 0
	)
	String webhookConfig = "webhookConfig";

	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL(s)",
			description = "The Discord Webhook URL(s) to send messages to. If you have more than one, separate them with commas.",
			section = webhookConfig,
			secret = true,
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
			description = "Only levels that are a multiple of this value are sent. Level 99 and Max XP are always sent, and so is anything at or above Always send from level.",
			section = levellingConfig,
			position = 3
	)
	default int levelInterval() {
		return 10;
	}

	@ConfigItem(
			keyName = "levelIntervalOverride",
			name = "Always send from level",
			description = "Every level at or above this one is sent, no matter what Send every X levels is set to. 0 turns it off.",
			section = levellingConfig,
			position = 4
	)
	default int levelIntervalOverride() {
		return 90;
	}

	@ConfigItem(
			keyName = "notifyVirtualLevels",
			name = "Virtual levels",
			description = "Also send levels past 99, and when a skill hits 200M XP.",
			section = levellingConfig,
			position = 5
	)
	default boolean notifyVirtualLevels() {
		return true;
	}

	@ConfigItem(
			keyName = "notifyCombatLevel",
			name = "Combat level",
			description = "Also send when your combat level goes up.",
			section = levellingConfig,
			position = 6
	)
	default boolean notifyCombatLevel() {
		return false;
	}

	@ConfigItem(
			keyName = "sendLevellingScreenshot",
			name = "Include levelling screenshots",
			description = "Include a screenshot when leveling up.",
			section = levellingConfig,
			position = 100
	)
	default boolean sendLevellingScreenshot() {
		return true;
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
			section = questingConfig,
			position = 1
	)
	default boolean includeQuestComplete() {
		return false;
	}

	@ConfigItem(
			keyName = "sendQuestingScreenshot",
			name = "Include quest screenshots",
			description = "Include a screenshot with the Discord notification when you complete a quest.",
			section = questingConfig,
			position = 100
	)
	default boolean sendQuestingScreenshot() {
		return true;
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
			section = deathConfig,
			position = 1
	)
	default boolean includeDeaths() { return false; }

	@ConfigItem(
			keyName = "ignoreSafeDeaths",
			name = "Ignore safe deaths",
			description = "Don't send deaths that cost you nothing, like minigames, raids and your house. The Inferno, Fight Caves and raid wipes still get sent, and hardcore accounts still get deaths that count against them.",
			section = deathConfig,
			position = 2
	)
	default boolean ignoreSafeDeaths() { return true; }

	@ConfigItem(
			keyName = "deathIgnoredRegions",
			name = "Ignored regions",
			description = "Region IDs to never send deaths from, separated by commas. Handy for UIM death piling.",
			section = deathConfig,
			position = 3
	)
	default String deathIgnoredRegions() { return ""; }

	@ConfigItem(
			keyName = "sendDeathScreenshot",
			name = "Include death screenshots",
			description = "Include a screenshot with the Discord notification when you die.",
			section = deathConfig,
			position = 100
	)
	default boolean sendDeathScreenshot() {
		return true;
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
			section = clueConfig,
			position = 1
	)
	default boolean includeClues() { return false; }

	@ConfigItem(
			keyName = "minClueTier",
			name = "Minimum tier",
			description = "Only clues of this tier or higher will send a message.",
			section = clueConfig,
			position = 2
	)
	default ClueTier minClueTier() { return ClueTier.BEGINNER; }

	@ConfigItem(
			keyName = "minClueValue",
			name = "Minimum value",
			description = "Only clues with a total reward worth at least this much will send a message.",
			section = clueConfig,
			position = 3
	)
	default int minClueValue() { return 0; }

	@ConfigItem(
			keyName = "sendClueScreenshot",
			name = "Include clue screenshots",
			description = "Include a screenshot with the Discord notification when you complete a clue.",
			section = clueConfig,
			position = 100
	)
	default boolean sendClueScreenshot() {
		return true;
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
			section = petConfig,
			position = 1
	)
	default boolean includePets() { return false; }

	@ConfigItem(
			keyName = "notifyDuplicatePets",
			name = "Notify duplicate pets",
			description = "Also send a message when you would have got a pet you already have.",
			section = petConfig,
			position = 2
	)
	default boolean notifyDuplicatePets() { return true; }

	@ConfigItem(
			keyName = "sendPetScreenshot",
			name = "Include pet screenshots",
			description = "Include a screenshot with the Discord notification when you receive a pet.",
			section = petConfig,
			position = 100
	)
	default boolean sendPetScreenshot() {
		return true;
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
		name = "Send Collection Log Notifications",
		description = "Send messages when you fill a new collection log slot.",
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
		position = 100
	)
	default boolean sendCollectionLogScreenshot() {
		return true;
	}

	@ConfigSection(
		name = "Combat Achievements",
		description = "The config for combat achievements",
		position = 7,
		closedByDefault = true
	)
	String combatAchievementsConfig = "combatAchievementsConfig";

	@ConfigItem(
		keyName = "includeCombatAchievements",
		name = "Send Combat Achievement Notifications",
		description = "Send messages when you complete a combat achievement.",
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
		name = "Include combat achievement screenshots",
		description = "Include a screenshot with the Discord notification when you complete a combat achievement",
		section = combatAchievementsConfig,
		position = 100
	)
	default boolean sendCombatAchievementsScreenshot() {
		return true;
	}

	@ConfigSection(
		name = "Loot",
		description = "The config for loot notifications",
		position = 8,
		closedByDefault = true
	)
	String lootConfig = "lootConfig";

	@ConfigItem(
		keyName = "includeLoot",
		name = "Send Loot Notifications",
		description = "Send messages when you get a valuable drop. Raids, chests and pickpocketing need the Loot Tracker plugin turned on.",
		section = lootConfig,
		position = 1
	)
	default boolean includeLoot() { return false; }

	@ConfigItem(
		keyName = "minLootValue",
		name = "Minimum value",
		description = "A drop is only sent if it's worth at least this much. Value basis picks what gets compared.",
		section = lootConfig,
		position = 2
	)
	default int minLootValue() { return 100000; }

	@ConfigItem(
		keyName = "lootValueBasis",
		name = "Value basis",
		description = "What has to reach the minimum. Single item is the price of one, whole stack is price times quantity, whole drop adds up everything and lists all of it.",
		section = lootConfig,
		position = 3
	)
	default LootValueBasis lootValueBasis() { return LootValueBasis.STACK; }

	@ConfigItem(
		keyName = "lootAllowlist",
		name = "Always send",
		description = "Items that send a message no matter what they're worth. Put each on its own line, or separate them with commas. Use * as a wildcard, like Tumeken's shadow*",
		section = lootConfig,
		position = 4
	)
	default String lootAllowlist() { return ""; }

	@ConfigItem(
		keyName = "lootDenylist",
		name = "Never send",
		description = "Items that never send a message or count toward the minimum, even if they're on Always send. Same format as Always send.",
		section = lootConfig,
		position = 5
	)
	default String lootDenylist() { return ""; }

	@ConfigItem(
		keyName = "lootSourceDenylist",
		name = "Never send from",
		description = "Monsters and activities that never send a message, even for items on Always send. Same format as Always send, like Barrows or *Tempoross*. Other players aren't affected.",
		section = lootConfig,
		position = 6
	)
	default String lootSourceDenylist() { return ""; }

	@ConfigItem(
		keyName = "includePlayerLoot",
		name = "Include PvP loot",
		description = "Also send loot from killing other players. Safe areas like Last Man Standing never send.",
		section = lootConfig,
		position = 7
	)
	default boolean includePlayerLoot() { return true; }

	@ConfigItem(
		keyName = "sendLootScreenshot",
		name = "Include loot screenshots",
		description = "Include a screenshot with the Discord notification when you get a valuable drop.",
		section = lootConfig,
		position = 100
	)
	default boolean sendLootScreenshot() { return true; }

	@ConfigSection(
		name = "Achievement Diaries",
		description = "The config for achievement diaries",
		position = 9,
		closedByDefault = true
	)
	String diaryConfig = "diaryConfig";

	@ConfigItem(
		keyName = "includeDiaries",
		name = "Send Achievement Diary Notifications",
		description = "Send messages when you finish an achievement diary.",
		section = diaryConfig,
		position = 1
	)
	default boolean includeDiaries() { return false; }

	@ConfigItem(
		keyName = "minDiaryDifficulty",
		name = "Minimum difficulty",
		description = "Only diaries of this difficulty or higher will send a message.",
		section = diaryConfig,
		position = 2
	)
	default DiaryDifficulty minDiaryDifficulty() { return DiaryDifficulty.EASY; }

	@ConfigItem(
		keyName = "sendDiaryScreenshot",
		name = "Include achievement diary screenshots",
		description = "Include a screenshot with the Discord notification when you finish an achievement diary",
		section = diaryConfig,
		position = 100
	)
	default boolean sendDiaryScreenshot() { return true; }

	@ConfigSection(
		name = "Boss Kills",
		description = "The config for boss kill count milestones and personal bests",
		position = 10,
		closedByDefault = true
	)
	String killCountConfig = "killCountConfig";

	@ConfigItem(
		keyName = "includeKillCounts",
		name = "Send Boss Kill Notifications",
		description = "Send messages when you hit a boss kill count milestone or get a new personal best. Filter out boss kill-count with spam-filter has to be off in your game settings.",
		section = killCountConfig,
		position = 1
	)
	default boolean includeKillCounts() { return false; }

	@ConfigItem(
		keyName = "killCountInterval",
		name = "Send every X kills",
		description = "Only kill counts that are a multiple of this value are sent. 0 turns this off if you only want first kills and personal bests.",
		section = killCountConfig,
		position = 2
	)
	default int killCountInterval() { return 50; }

	@ConfigItem(
		keyName = "notifyFirstKill",
		name = "First kill",
		description = "Also send your first kill of a boss.",
		section = killCountConfig,
		position = 3
	)
	default boolean notifyFirstKill() { return true; }

	@ConfigItem(
		keyName = "notifyPersonalBests",
		name = "Personal bests",
		description = "Also send when you beat your best time, whatever your kill count is.",
		section = killCountConfig,
		position = 4
	)
	default boolean notifyPersonalBests() { return true; }

	@ConfigItem(
		keyName = "sendKillCountScreenshot",
		name = "Include boss kill screenshots",
		description = "Include a screenshot with the Discord notification for a boss kill milestone or personal best.",
		section = killCountConfig,
		position = 100
	)
	default boolean sendKillCountScreenshot() { return true; }

	@ConfigSection(
		name = "Leagues (League worlds only)",
		description = "Area, relic, task and combat mastery messages, only sent on a Leagues world",
		position = 998,
		closedByDefault = true
	)
	String leaguesConfig = "leaguesConfig";

	@ConfigItem(
		keyName = "includeLeagues",
		name = "Send Leagues Notifications",
		description = "Send messages for Leagues events. Does nothing outside of a Leagues world, and Leagues and Deadman worlds in Advanced: other has to be on Notify.",
		section = leaguesConfig,
		position = 1
	)
	default boolean includeLeagues() { return false; }

	@ConfigItem(
		keyName = "leaguesAreas",
		name = "Area unlocks",
		description = "Send when you unlock a new area.",
		section = leaguesConfig,
		position = 2
	)
	default boolean leaguesAreas() { return true; }

	@ConfigItem(
		keyName = "leaguesRelics",
		name = "Relic unlocks",
		description = "Send when you unlock a new relic.",
		section = leaguesConfig,
		position = 3
	)
	default boolean leaguesRelics() { return true; }

	@ConfigItem(
		keyName = "leaguesMasteries",
		name = "Combat mastery unlocks",
		description = "Send when you unlock a new combat mastery.",
		section = leaguesConfig,
		position = 4
	)
	default boolean leaguesMasteries() { return true; }

	@ConfigItem(
		keyName = "leaguesTasks",
		name = "Task completions",
		description = "Send when you finish a Leagues task.",
		section = leaguesConfig,
		position = 5
	)
	default boolean leaguesTasks() { return true; }

	@ConfigItem(
		keyName = "minLeaguesTaskTier",
		name = "Minimum task tier",
		description = "Only tasks of this tier or higher will send a message.",
		section = leaguesConfig,
		position = 6
	)
	default LeagueTaskTier minLeaguesTaskTier() { return LeagueTaskTier.EASY; }

	@ConfigItem(
		keyName = "sendLeaguesScreenshot",
		name = "Include Leagues screenshots",
		description = "Include a screenshot with the Discord notification for Leagues events.",
		section = leaguesConfig,
		position = 100
	)
	default boolean sendLeaguesScreenshot() { return true; }

	@ConfigSection(
		name = "Webhook overrides",
		description = "Give an event its own channel, leave it blank to use the main webhook",
		position = 999,
		closedByDefault = true
	)
	String webhookOverridesConfig = "webhookOverridesConfig";

	@ConfigItem(
		keyName = "levelWebhook",
		name = "Levels",
		description = "Webhook URL(s) for level ups.",
		section = webhookOverridesConfig,
		secret = true,
		position = 1
	)
	default String levelWebhook() { return ""; }

	@ConfigItem(
		keyName = "questWebhook",
		name = "Quests",
		description = "Webhook URL(s) for quests.",
		section = webhookOverridesConfig,
		secret = true,
		position = 2
	)
	default String questWebhook() { return ""; }

	@ConfigItem(
		keyName = "deathWebhook",
		name = "Deaths",
		description = "Webhook URL(s) for deaths.",
		section = webhookOverridesConfig,
		secret = true,
		position = 3
	)
	default String deathWebhook() { return ""; }

	@ConfigItem(
		keyName = "clueWebhook",
		name = "Clue scrolls",
		description = "Webhook URL(s) for clue scrolls.",
		section = webhookOverridesConfig,
		secret = true,
		position = 4
	)
	default String clueWebhook() { return ""; }

	@ConfigItem(
		keyName = "petWebhook",
		name = "Pets",
		description = "Webhook URL(s) for pets.",
		section = webhookOverridesConfig,
		secret = true,
		position = 5
	)
	default String petWebhook() { return ""; }

	@ConfigItem(
		keyName = "collectionLogWebhook",
		name = "Collection log",
		description = "Webhook URL(s) for collection log slots.",
		section = webhookOverridesConfig,
		secret = true,
		position = 6
	)
	default String collectionLogWebhook() { return ""; }

	@ConfigItem(
		keyName = "combatAchievementWebhook",
		name = "Combat achievements",
		description = "Webhook URL(s) for combat achievements.",
		section = webhookOverridesConfig,
		secret = true,
		position = 7
	)
	default String combatAchievementWebhook() { return ""; }

	@ConfigItem(
		keyName = "lootWebhook",
		name = "Loot",
		description = "Webhook URL(s) for loot.",
		section = webhookOverridesConfig,
		secret = true,
		position = 8
	)
	default String lootWebhook() { return ""; }

	@ConfigItem(
		keyName = "diaryWebhook",
		name = "Achievement diaries",
		description = "Webhook URL(s) for achievement diaries.",
		section = webhookOverridesConfig,
		secret = true,
		position = 9
	)
	default String diaryWebhook() { return ""; }

	@ConfigItem(
		keyName = "killCountWebhook",
		name = "Boss kills",
		description = "Webhook URL(s) for boss kill milestones and personal bests.",
		section = webhookOverridesConfig,
		secret = true,
		position = 10
	)
	default String killCountWebhook() { return ""; }

	@ConfigItem(
		keyName = "leaguesWebhook",
		name = "Leagues",
		description = "Webhook URL(s) for Leagues events.",
		section = webhookOverridesConfig,
		secret = true,
		position = 11
	)
	default String leaguesWebhook() { return ""; }

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
		description = "Sent when you level up. Use $name, $skills (like Attack to 70 and Strength to 72), $total, $totalXp and $combat. $skill and $level only give the first skill.",
		section = messageTemplatesConfig,
		position = 1
	)
	default String levelMessage() { return "$name leveled $skills - Total Level: $total"; }

	@ConfigItem(
		keyName = "questMessage",
		name = "Quest Message",
		description = "Sent when you finish a quest. Use $name, $quest, $qp, $qpMax, $completed, $totalQuests and $progress (like: (44/293 QP)). The numbers are blank if the game hasn't sent them.",
		section = messageTemplatesConfig,
		position = 2
	)
	default String questMessage() { return "$name has just completed: $quest$progress"; }

	@ConfigItem(
		keyName = "deathMessage",
		name = "Death Message",
		description = "Sent when you die and we can't tell what killed you. Use $name.",
		section = messageTemplatesConfig,
		position = 3
	)
	default String deathMessage() { return "$name has just died!"; }

	@ConfigItem(
		keyName = "deathNpcMessage",
		name = "Death Message (NPC)",
		description = "Sent when an NPC kills you. Use $name and $killer.",
		section = messageTemplatesConfig,
		position = 4
	)
	default String deathNpcMessage() { return "$name has just been killed by $killer!"; }

	@ConfigItem(
		keyName = "deathPvpMessage",
		name = "Death Message (PvP)",
		description = "Sent when another player kills you. Use $name and $killer.",
		section = messageTemplatesConfig,
		position = 5
	)
	default String deathPvpMessage() { return "$name has just been PKed by $killer!"; }

	@ConfigItem(
		keyName = "clueMessage",
		name = "Clue Message",
		description = "Sent when you finish a clue scroll. Use $name, $tier, $count (how many of that tier you've done), $items and $value.",
		section = messageTemplatesConfig,
		position = 6
	)
	default String clueMessage() { return "$name has just completed a clue scroll ($tier #$count) worth $value gp: $items"; }

	@ConfigItem(
		keyName = "petTemplate",
		name = "Pet Message",
		description = "Sent when you get a pet. Use $name, $pet, $source, $from (like: from Zulrah), $kc (like: (KC 1,234)), $milestone (like: at 5,000,000 XP, only used when there's no $kc) and $duplicate. Put $from, $kc, $milestone and $duplicate straight after another word, they add their own space and are blank when unknown.",
		section = messageTemplatesConfig,
		position = 7
	)
	default String petMessage() { return "$name just got a pet: $pet$from$kc$milestone$duplicate"; }

	@ConfigItem(
		keyName = "collectionLogMessage",
		name = "Collection log Message",
		description = "Sent when you fill a collection log slot. Use $name, $entry, $completed, $total, $progress (like: (123/1,692)), $source, $from (like: from Zulrah) and $kc (like: (KC 1,234)). Put $progress, $from and $kc straight after another word, they add their own space. The counts are blank if the game hasn't sent them yet, opening the Character Summary tab fixes that. $source, $from and $kc are blank if the item didn't come from a drop in the last 30 seconds.",
		section = messageTemplatesConfig,
		position = 8
	)
	default String collectionLogMessage() { return "$name added $entry to their collection log$progress$from$kc"; }

	@ConfigItem(
		keyName = "combatAchievementsMessage",
		name = "Combat Achievement Message",
		description = "Sent when you finish a combat task. Use $name, $task, $tier, $points (what the task was worth), $totalPoints and $tierUnlocked (like: and unlocked the Hard tier). Put $tierUnlocked straight after another word, it adds its own space and is blank unless the task got you to a new tier.",
		section = messageTemplatesConfig,
		position = 9
	)
	default String combatAchievementsMessage() { return "$name has just completed a combat achievement: $task ($tier)$tierUnlocked"; }

	@ConfigItem(
		keyName = "lootMessage",
		name = "Loot Message",
		description = "Sent when you get a valuable drop. Use $name, $items (only the stacks that passed your loot filters), $value (the whole drop), $source and $kc (like: (KC 1,234)). Put $kc straight after another word, it adds its own space and is blank if the kill count isn't known.",
		section = messageTemplatesConfig,
		position = 10
	)
	default String lootMessage() { return "$name has just received $items from $source$kc, whole drop worth $value gp"; }

	@ConfigItem(
		keyName = "diaryMessage",
		name = "Achievement Diary Message",
		description = "Sent when you finish an achievement diary. Use $name, $area, $difficulty, $totalDiaries (how many you've finished across every area and difficulty), $tasksComplete and $tasksTotal (tasks across every area), $areaTasksComplete and $areaTasksTotal (tasks in this area).",
		section = messageTemplatesConfig,
		position = 11
	)
	default String diaryMessage() { return "$name has just completed the $difficulty $area diary - Total diaries: $totalDiaries"; }

	@ConfigItem(
		keyName = "killCountMessage",
		name = "Boss Kill Message",
		description = "Sent on a kill count milestone or first kill. Use $name, $boss, $kc (like: (KC 1,234)), $time (like: in 1:23.40) and $pb (like: (PB 1:02.40)). Put $kc, $time and $pb straight after another word, they add their own space. $time is blank if the boss doesn't give one, $pb is blank if your best isn't known.",
		section = messageTemplatesConfig,
		position = 12
	)
	default String killCountMessage() { return "$name has just beaten $boss$kc$time$pb"; }

	@ConfigItem(
		keyName = "personalBestMessage",
		name = "Personal Best Message",
		description = "Sent when you beat your best time. Uses the same placeholders as Boss Kill Message, except $pb is always blank because this kill is the best.",
		section = messageTemplatesConfig,
		position = 13
	)
	default String personalBestMessage() { return "$name has just set a new personal best at $boss$kc$time"; }

	@ConfigItem(
		keyName = "leaguesAreaMessage",
		name = "Leagues Area Message",
		description = "Sent when you unlock a Leagues area. Use $name and $area.",
		section = messageTemplatesConfig,
		position = 14
	)
	default String leaguesAreaMessage() { return "$name has just unlocked a new area: $area"; }

	@ConfigItem(
		keyName = "leaguesRelicMessage",
		name = "Leagues Relic Message",
		description = "Sent when you unlock a Leagues relic. Use $name and $relic.",
		section = messageTemplatesConfig,
		position = 15
	)
	default String leaguesRelicMessage() { return "$name has just unlocked a new relic: $relic"; }

	@ConfigItem(
		keyName = "leaguesMasteryMessage",
		name = "Leagues Mastery Message",
		description = "Sent when you unlock a Leagues combat mastery. Use $name and $mastery.",
		section = messageTemplatesConfig,
		position = 16
	)
	default String leaguesMasteryMessage() { return "$name has just unlocked a new combat mastery: $mastery"; }

	@ConfigItem(
		keyName = "leaguesTaskMessage",
		name = "Leagues Task Message",
		description = "Sent when you finish a Leagues task. Use $name, $task and $tier.",
		section = messageTemplatesConfig,
		position = 17
	)
	default String leaguesTaskMessage() { return "$name has just completed a Leagues task: $task ($tier)"; }

	@ConfigSection(
		name = "Advanced: other",
		description = "Settings that don't fit anywhere else",
		position = 1001,
		closedByDefault = true
	)
	String advancedOtherConfig = "advancedOtherConfig";

	@ConfigItem(
		keyName = "seasonalWorlds",
		name = "Leagues and Deadman worlds",
		description = "Notify puts [Leagues], [DMM] or [Grid Master] at the start of each message. Ignore doesn't send anything from those worlds.",
		section = advancedOtherConfig,
		position = 1
	)
	default SeasonalWorlds seasonalWorlds() { return SeasonalWorlds.NOTIFY; }
}
