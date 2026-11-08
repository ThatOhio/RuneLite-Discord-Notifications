package com.discordnotifications.notifiers;

import com.discordnotifications.Template;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LevelNotifierTest
{
	private static Map<String, Integer> levels(Object... skillsAndLevels)
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (int i = 0; i < skillsAndLevels.length; i += 2)
		{
			levels.put((String) skillsAndLevels[i], (Integer) skillsAndLevels[i + 1]);
		}
		return levels;
	}

	private static String render(String template, Map<String, Integer> levels)
	{
		return Template.render(template, LevelNotifier.levelPlaceholders(levels, 1500));
	}

	@Test
	public void oneSkill()
	{
		assertEquals("Attack to 70 / 1500", render("$skills / $total", levels("Attack", 70)));
	}

	@Test
	public void twoSkills()
	{
		assertEquals("Attack to 70 and Hitpoints to 71",
			render("$skills", levels("Attack", 70, "Hitpoints", 71)));
	}

	@Test
	public void threeSkillsKeepOrder()
	{
		assertEquals("Attack to 70, Strength to 72 and Hitpoints to 71",
			render("$skills", levels("Attack", 70, "Strength", 72, "Hitpoints", 71)));
	}

	@Test
	public void oldPlaceholdersGiveTheFirstSkill()
	{
		assertEquals("Attack to 70", render("$skill to $level", levels("Attack", 70, "Hitpoints", 71)));
	}
}
