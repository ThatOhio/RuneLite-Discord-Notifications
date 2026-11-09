package com.discordnotifications.notifiers;

import com.discordnotifications.Template;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		return Template.render(template, LevelNotifier.levelPlaceholders(levels, 1500, 88, 4_600_000_000L));
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

	@Test
	public void combatPlaceholder()
	{
		assertEquals("Combat to 88 / 88", render("$skills / $combat", levels("Combat", 88)));
	}

	@Test
	public void totalXpPlaceholder()
	{
		assertEquals("1500 / 4,600,000,000", render("$total / $totalXp", levels("Attack", 70)));
	}

	@Test
	public void maxXpReadsAsWords()
	{
		assertEquals("Attack to Max XP", render("$skills", levels("Attack", LevelNotifier.MAX_XP_LEVEL)));
		assertEquals("Attack Max XP", render("$skill $level", levels("Attack", LevelNotifier.MAX_XP_LEVEL)));
	}

	@Test
	public void virtualLevels()
	{
		assertEquals(98, LevelNotifier.virtualLevel(98, 12_000_000));
		assertEquals(99, LevelNotifier.virtualLevel(99, 13_034_431));
		assertEquals(100, LevelNotifier.virtualLevel(99, 14_391_160));
		assertEquals(126, LevelNotifier.virtualLevel(99, 199_999_999));
		assertEquals(LevelNotifier.MAX_XP_LEVEL, LevelNotifier.virtualLevel(99, 200_000_000));
	}

	@Test
	public void intervalAndOverride()
	{
		assertTrue(LevelNotifier.meetsInterval(73, 1, 0));
		assertTrue(LevelNotifier.meetsInterval(70, 10, 0));
		assertFalse(LevelNotifier.meetsInterval(73, 10, 0));
		assertTrue(LevelNotifier.meetsInterval(99, 10, 0));
		assertTrue(LevelNotifier.meetsInterval(LevelNotifier.MAX_XP_LEVEL, 10, 0));

		assertFalse(LevelNotifier.meetsInterval(73, 10, 80));
		assertTrue(LevelNotifier.meetsInterval(80, 10, 80));
		assertTrue(LevelNotifier.meetsInterval(83, 10, 80));
	}
}
