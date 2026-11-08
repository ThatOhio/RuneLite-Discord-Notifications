package com.discordnotifications.notifiers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LevelNotifierTest
{
	private static final String LEVEL = "$name leveled $skill to $level";
	private static final String AND = ", and $skill to $level";
	private static final String TOTAL = " - Total Level: $total";

	private static Map<String, Integer> levels(Object... skillsAndLevels)
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (int i = 0; i < skillsAndLevels.length; i += 2)
		{
			levels.put((String) skillsAndLevels[i], (Integer) skillsAndLevels[i + 1]);
		}
		return levels;
	}

	@Test
	public void oneSkill()
	{
		assertEquals("$name leveled Attack to 70",
			LevelNotifier.buildLevelMessage(levels("Attack", 70), 1500, LEVEL, AND, ""));
	}

	@Test
	public void oneSkillWithTotal()
	{
		assertEquals("$name leveled Attack to 70 - Total Level: 1500",
			LevelNotifier.buildLevelMessage(levels("Attack", 70), 1500, LEVEL, AND, TOTAL));
	}

	@Test
	public void twoSkillsAppendTotalOnce()
	{
		assertEquals("$name leveled Attack to 70, and Hitpoints to 71 - Total Level: 1500",
			LevelNotifier.buildLevelMessage(levels("Attack", 70, "Hitpoints", 71), 1500, LEVEL, AND, TOTAL));
	}

	@Test
	public void threeSkillsKeepOrder()
	{
		assertEquals("$name leveled Attack to 70, and Strength to 72, and Hitpoints to 71",
			LevelNotifier.buildLevelMessage(levels("Attack", 70, "Strength", 72, "Hitpoints", 71), 1500, LEVEL, AND, ""));
	}
}
