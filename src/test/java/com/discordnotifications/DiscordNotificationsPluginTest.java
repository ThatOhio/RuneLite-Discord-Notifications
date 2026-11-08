package com.discordnotifications;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiscordNotificationsPluginTest
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
			DiscordNotificationsPlugin.buildLevelMessage(levels("Attack", 70), 1500, LEVEL, AND, ""));
	}

	@Test
	public void oneSkillWithTotal()
	{
		assertEquals("$name leveled Attack to 70 - Total Level: 1500",
			DiscordNotificationsPlugin.buildLevelMessage(levels("Attack", 70), 1500, LEVEL, AND, TOTAL));
	}

	@Test
	public void twoSkillsAppendTotalOnce()
	{
		assertEquals("$name leveled Attack to 70, and Hitpoints to 71 - Total Level: 1500",
			DiscordNotificationsPlugin.buildLevelMessage(levels("Attack", 70, "Hitpoints", 71), 1500, LEVEL, AND, TOTAL));
	}

	@Test
	public void threeSkillsKeepOrder()
	{
		assertEquals("$name leveled Attack to 70, and Strength to 72, and Hitpoints to 71",
			DiscordNotificationsPlugin.buildLevelMessage(levels("Attack", 70, "Strength", 72, "Hitpoints", 71), 1500, LEVEL, AND, ""));
	}

	@Test
	public void sanitizeStripsTagsColourCodesAndNbsp()
	{
		assertEquals("You have completed The Corsair Curse!",
			DiscordNotificationsPlugin.sanitize("<col=ff0000>You have completed<br>The\u00A0Corsair Curse!</col>@whi@ "));
		assertEquals("", DiscordNotificationsPlugin.sanitize(null));
	}

	@Test
	public void popupSubjectKeepsTextAfterLabel()
	{
		assertEquals("Abyssal whip",
			DiscordNotificationsPlugin.popupSubject("New item: <col=ffffff>Abyssal whip</col>"));
		assertEquals("Perfect Zulrah: No damage",
			DiscordNotificationsPlugin.popupSubject("Task Completed: <col=ffffff>Perfect Zulrah: No damage</col>"));
		assertEquals("No label here", DiscordNotificationsPlugin.popupSubject("No label here"));
	}
}
