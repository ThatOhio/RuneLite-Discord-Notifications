package com.discordnotifications;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiscordNotificationsPluginTest
{
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
