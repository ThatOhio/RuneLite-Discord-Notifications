package com.discordnotifications.notifiers;

import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.WorldUtils;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.WorldType;
import net.runelite.api.events.StatChanged;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class LevelNotifier extends BaseNotifier
{
	private final Map<String, Integer> currentLevels = new HashMap<>();
	private final Set<String> leveledSkills = new LinkedHashSet<>();
	private boolean shouldSendLevelMessage = false;
	private int ticksWaited = 0;
	private Set<WorldType> seededWorldTypes = null;

	@Override
	public void reset()
	{
		currentLevels.clear();
		leveledSkills.clear();
		shouldSendLevelMessage = false;
		ticksWaited = 0;
		seededWorldTypes = null;
	}

	public void onStatChanged(StatChanged statChanged)
	{
		// Hopping to a world with its own stats (Leagues, speedrun) would look like a level up
		Set<WorldType> worldTypes = WorldUtils.separateStatsTypes(client.getWorldType());
		if (!worldTypes.equals(seededWorldTypes))
		{
			reset();
			seededWorldTypes = worldTypes;
		}

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

	public void onGameTick()
	{
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

	private void sendLevelMessage()
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (String skill : leveledSkills)
		{
			levels.put(skill, currentLevels.get(skill));
		}
		leveledSkills.clear();

		send(Notification.builder()
			.type(NotificationType.LEVEL)
			.text(config.levelMessage())
			.placeholders(levelPlaceholders(levels, client.getTotalLevel()))
			.screenshot(config.sendLevellingScreenshot())
			.build());
	}

	static Map<String, String> levelPlaceholders(Map<String, Integer> levels, int totalLevel)
	{
		List<String> skills = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : levels.entrySet())
		{
			skills.add(entry.getKey() + " to " + entry.getValue());
		}

		int last = skills.size() - 1;
		String joined = last == 0
			? skills.get(0)
			: String.join(", ", skills.subList(0, last)) + " and " + skills.get(last);

		Map.Entry<String, Integer> first = levels.entrySet().iterator().next();
		return ImmutableMap.of(
			"skills", joined,
			"skill", first.getKey(),
			"level", first.getValue().toString(),
			"total", Integer.toString(totalLevel));
	}
}
