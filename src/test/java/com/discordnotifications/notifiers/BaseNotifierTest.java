package com.discordnotifications.notifiers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaseNotifierTest
{
	@Test
	public void sanitizeStripsTagsColourCodesAndNbsp()
	{
		assertEquals("You have completed The Corsair Curse!",
			BaseNotifier.sanitize("<col=ff0000>You have completed<br>The Corsair Curse!</col>@whi@ "));
		assertEquals("", BaseNotifier.sanitize(null));
	}

	@Test
	public void popupSubjectKeepsTextAfterLabel()
	{
		assertEquals("Abyssal whip",
			BaseNotifier.popupSubject("New item: <col=ffffff>Abyssal whip</col>"));
		assertEquals("Perfect Zulrah: No damage",
			BaseNotifier.popupSubject("Task Completed: <col=ffffff>Perfect Zulrah: No damage</col>"));
		assertEquals("No label here", BaseNotifier.popupSubject("No label here"));
	}
}
