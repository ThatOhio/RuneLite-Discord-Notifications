package com.discordnotifications.notifiers;

import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import net.runelite.api.events.StatChanged;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class LevelNotifier extends BaseNotifier
{
	private final Map<String, Integer> currentLevels = new HashMap<>();
	private final Set<String> leveledSkills = new LinkedHashSet<>();
	private boolean shouldSendLevelMessage = false;
	private int ticksWaited = 0;

	@Override
	public void reset()
	{
		currentLevels.clear();
		leveledSkills.clear();
		shouldSendLevelMessage = false;
		ticksWaited = 0;
	}

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

		String message = buildLevelMessage(
			levels,
			client.getTotalLevel(),
			config.levelMessage(),
			config.andLevelMessage(),
			config.includeTotalLevel() ? config.totalLevelMessage() : "");

		send(Notification.builder()
			.type(NotificationType.LEVEL)
			.text(message)
			.screenshot(config.sendLevellingScreenshot())
			.build());
	}

	static String buildLevelMessage(Map<String, Integer> levels, int totalLevel,
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

		return message.toString().replace("$total", Integer.toString(totalLevel));
	}
}
