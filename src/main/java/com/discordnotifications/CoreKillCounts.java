package com.discordnotifications;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.plugins.chatcommands.ChatCommandsPlugin;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

// Reads the config ChatCommandsPlugin and LootTrackerPlugin save, none of it is core API
@Slf4j
@Singleton
class CoreKillCounts
{
	private static final String CHAT_COMMANDS_PLUGIN = ChatCommandsPlugin.class.getSimpleName().toLowerCase(Locale.ROOT);
	private static final String LOOT_TRACKER_PLUGIN = LootTrackerPlugin.class.getSimpleName().toLowerCase(Locale.ROOT);

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	int chatCommandsKc(String boss)
	{
		if (pluginDisabled(CHAT_COMMANDS_PLUGIN))
		{
			return 0;
		}
		Integer kc = configManager.getRSProfileConfiguration("killcount", boss, int.class);
		return kc == null ? 0 : kc;
	}

	Double chatCommandsPb(String boss)
	{
		if (pluginDisabled(CHAT_COMMANDS_PLUGIN))
		{
			return null;
		}
		return configManager.getRSProfileConfiguration("personalbest", boss, double.class);
	}

	// The tracker skips kills with no loot and only knows kills made with it on, so this is a lower bound
	int lootTrackerKills(LootRecordType type, String source)
	{
		if (pluginDisabled(LOOT_TRACKER_PLUGIN))
		{
			return 0;
		}

		String json = configManager.getConfiguration(LootTrackerConfig.GROUP, configManager.getRSProfileKey(), "drops_" + type + "_" + source);
		if (json == null)
		{
			return 0;
		}

		try
		{
			return parseKills(gson, json);
		}
		catch (JsonSyntaxException e)
		{
			log.debug("Couldn't read loot tracker record for {}", source, e);
			return 0;
		}
	}

	static int parseKills(Gson gson, String json)
	{
		LootTrackerRecord record = gson.fromJson(json, LootTrackerRecord.class);
		return record == null ? 0 : record.kills;
	}

	boolean lootTrackerDisabled()
	{
		return pluginDisabled(LOOT_TRACKER_PLUGIN);
	}

	private boolean pluginDisabled(String simpleLowerClassName)
	{
		return "false".equals(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, simpleLowerClassName));
	}

	// Same shape as the loot tracker's own ConfigLoot, which is package-private
	private static class LootTrackerRecord
	{
		int kills;
	}
}
