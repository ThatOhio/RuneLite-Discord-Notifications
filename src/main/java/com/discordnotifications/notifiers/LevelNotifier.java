package com.discordnotifications.notifiers;

import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.WorldUtils;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.StatChanged;
import net.runelite.client.util.QuantityFormatter;

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
	// Stats aren't reliable for the first few seconds after login
	private static final int SETTLE_TICKS = 16;
	// Virtual levels stop at 126, so 200M XP gets treated as one more
	static final int MAX_XP_LEVEL = Experience.MAX_VIRT_LEVEL + 1;
	private static final String COMBAT = "Combat";

	private final Map<String, Integer> currentLevels = new HashMap<>();
	private final Set<String> leveledSkills = new LinkedHashSet<>();
	private boolean shouldSendLevelMessage = false;
	private int ticksWaited = 0;
	private int settleTicks = 0;
	private Set<WorldType> seededWorldTypes = null;

	@Override
	public void reset()
	{
		currentLevels.clear();
		leveledSkills.clear();
		shouldSendLevelMessage = false;
		ticksWaited = 0;
		settleTicks = 0;
		seededWorldTypes = null;
	}

	public void onStatChanged(StatChanged statChanged)
	{
		resetIfWorldChanged();

		int xp = statChanged.getXp();
		if (currentLevels.isEmpty() || xp <= 0 || statChanged.getLevel() <= 1)
		{
			return;
		}

		String skillName = statChanged.getSkill().getName();
		int newLevel = virtualLevel(statChanged.getLevel(), xp);
		int previousLevel = currentLevels.put(skillName, newLevel);

		if (newLevel < previousLevel)
		{
			// Levels can't go down, so what we seeded was wrong. Start over.
			reset();
			return;
		}

		if (newLevel == previousLevel)
		{
			return;
		}

		int previousCombat = currentLevels.get(COMBAT);
		int combat = combatLevel();
		currentLevels.put(COMBAT, combat);

		if (!config.includeLevelling())
		{
			return;
		}

		queueLevelUp(skillName, previousLevel, newLevel, config.notifyVirtualLevels());
		if (config.notifyCombatLevel())
		{
			queueLevelUp(COMBAT, previousCombat, combat, true);
		}
	}

	public void onGameTick()
	{
		resetIfWorldChanged();

		if (currentLevels.isEmpty())
		{
			if (++settleTicks >= SETTLE_TICKS)
			{
				seedLevels();
			}
			return;
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

	// Hopping to a world with its own stats (Leagues, speedrun) would look like a level up
	private void resetIfWorldChanged()
	{
		Set<WorldType> worldTypes = WorldUtils.separateStatsTypes(client.getWorldType());
		if (!worldTypes.equals(seededWorldTypes))
		{
			reset();
			seededWorldTypes = worldTypes;
		}
	}

	private void seedLevels()
	{
		for (Skill skill : Skill.values())
		{
			currentLevels.put(skill.getName(), virtualLevel(client.getRealSkillLevel(skill), client.getSkillExperience(skill)));
		}
		currentLevels.put(COMBAT, combatLevel());
	}

	private void queueLevelUp(String name, int previousLevel, int newLevel, boolean allowVirtual)
	{
		// Certain activities can multilevel, check if any of the levels are valid for the message.
		for (int level = previousLevel + 1; level <= newLevel; level++)
		{
			if (shouldSendForThisLevel(level, allowVirtual))
			{
				leveledSkills.add(name);
				shouldSendLevelMessage = true;
				break;
			}
		}
	}

	private boolean shouldSendForThisLevel(int level, boolean allowVirtual)
	{
		return (allowVirtual || level <= Experience.MAX_REAL_LEVEL)
			&& level >= config.minLevel()
			&& meetsInterval(level, config.levelInterval(), config.levelIntervalOverride());
	}

	static boolean meetsInterval(int level, int interval, int override)
	{
		return interval <= 1
			|| level == Experience.MAX_REAL_LEVEL
			|| level == MAX_XP_LEVEL
			|| (override > 0 && level >= override)
			|| level % interval == 0;
	}

	static int virtualLevel(int level, int xp)
	{
		if (level < Experience.MAX_REAL_LEVEL)
		{
			return level;
		}
		return xp >= Experience.MAX_SKILL_XP ? MAX_XP_LEVEL : Experience.getLevelForXp(xp);
	}

	private int combatLevel()
	{
		return Experience.getCombatLevel(
			realLevel(Skill.ATTACK),
			realLevel(Skill.STRENGTH),
			realLevel(Skill.DEFENCE),
			realLevel(Skill.HITPOINTS),
			realLevel(Skill.MAGIC),
			realLevel(Skill.RANGED),
			realLevel(Skill.PRAYER));
	}

	private int realLevel(Skill skill)
	{
		return Math.min(currentLevels.get(skill.getName()), Experience.MAX_REAL_LEVEL);
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
			.placeholders(levelPlaceholders(levels, client.getTotalLevel(), currentLevels.get(COMBAT), client.getOverallExperience()))
			.screenshot(config.sendLevellingScreenshot())
			.build());
	}

	static Map<String, String> levelPlaceholders(Map<String, Integer> levels, int totalLevel, int combatLevel, long totalXp)
	{
		List<String> skills = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : levels.entrySet())
		{
			skills.add(entry.getKey() + " to " + displayLevel(entry.getValue()));
		}

		int last = skills.size() - 1;
		String joined = last == 0
			? skills.get(0)
			: String.join(", ", skills.subList(0, last)) + " and " + skills.get(last);

		Map.Entry<String, Integer> first = levels.entrySet().iterator().next();
		return ImmutableMap.<String, String>builder()
			.put("skills", joined)
			.put("skill", first.getKey())
			.put("level", displayLevel(first.getValue()))
			.put("total", Integer.toString(totalLevel))
			.put("totalXp", QuantityFormatter.formatNumber(totalXp))
			.put("combat", Integer.toString(combatLevel))
			.build();
	}

	private static String displayLevel(int level)
	{
		return level >= MAX_XP_LEVEL ? "Max XP" : Integer.toString(level);
	}
}
