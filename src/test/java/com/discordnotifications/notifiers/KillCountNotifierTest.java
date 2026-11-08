package com.discordnotifications.notifiers;

import com.discordnotifications.Template;
import com.discordnotifications.notifiers.KillCountNotifier.Kill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KillCountNotifierTest
{
	private static Kill parseAll(String... messages)
	{
		Kill kill = null;
		for (String message : messages)
		{
			Kill parsed = KillCountNotifier.parse(message);
			kill = kill == null ? parsed : kill.merge(parsed);
		}
		return kill;
	}

	@Test
	public void parsesKillCount()
	{
		assertEquals(new Kill("Zulrah", 1234, null, false), KillCountNotifier.parse("Your Zulrah kill count is: 1,234."));
	}

	@Test
	public void parsesTimes()
	{
		assertEquals(new Kill(null, null, "0:56.50", true), KillCountNotifier.parse("Fight duration: 0:56.50 (new personal best)."));
		assertEquals(new Kill(null, null, "1:00:56.50", true), KillCountNotifier.parse("Fight duration: 1:00:56.50 (new personal best)."));
		assertEquals(new Kill(null, null, "0:59.30", false), KillCountNotifier.parse("Fight duration: 0:59.30. Personal best: 0:56.50"));
		assertEquals(new Kill(null, null, "6:30", true), KillCountNotifier.parse("Subdued in 6:30 (new personal best)."));
		assertEquals(new Kill(null, null, "46:31.80", false), KillCountNotifier.parse("Congratulations - your raid is complete! Team size: Solo Duration: 46:31.80 Personal best: 40:24.60"));
	}

	@Test
	public void ignoresLapsAndOtherMessages()
	{
		assertNull(KillCountNotifier.parse("Your Ardougne Rooftop lap count is: 200."));
		assertNull(KillCountNotifier.parse("Your Agility Arena Total Ticket count is: 1,000."));
		assertNull(KillCountNotifier.parse("Preparation time: 1:23 (new personal best)"));
		assertNull(KillCountNotifier.parse("You eat the shark."));
	}

	@Test
	public void mergesEitherOrder()
	{
		Kill expected = new Kill("Zulrah", 12, "0:56.50", true);
		assertEquals(expected, parseAll("Your Zulrah kill count is: 12.", "Fight duration: 0:56.50 (new personal best)."));
		assertEquals(expected, parseAll("Fight duration: 0:56.50 (new personal best).", "Your Zulrah kill count is: 12."));
	}

	@Test
	public void lastTimeWinsOutsideTob()
	{
		Kill kill = parseAll("Challenge complete: Path of Het. Duration: 3:00 (new personal best)",
			"Tombs of Amascut total completion time: 29:45.60. Personal best: 25:08.40",
			"Your completed Tombs of Amascut count is: 40.");
		assertEquals(new Kill("Tombs of Amascut", 40, "29:45.60", false), kill);
	}

	@Test
	public void tobKeepsTheChallengeTime()
	{
		Kill kill = parseAll("Wave 'The Final Challenge' (Normal Mode) complete! Duration: 6:37.80 Theatre of Blood completion time: 19:26.40. Personal best: 19:24.00",
			"Theatre of Blood total completion time: 26:34.20 (new personal best)",
			"Your completed Theatre of Blood count is: 5.");
		assertEquals(new Kill("Theatre of Blood", 5, "19:26.40", false), kill);
	}

	@Test
	public void tobIgnoresTheWaveDuration()
	{
		Kill kill = parseAll("Wave 'The Final Challenge' (Normal Mode) complete! Duration: 7:42.60 Theatre of Blood completion time: 21:33.60 (new personal best)",
			"Theatre of Blood total completion time: 25:28.80 (new personal best)",
			"Your completed Theatre of Blood count is: 1.");
		assertEquals(new Kill("Theatre of Blood", 1, "21:33.60", true), kill);
	}

	@Test
	public void milestones()
	{
		assertTrue(KillCountNotifier.isMilestone(100, 50, false));
		assertFalse(KillCountNotifier.isMilestone(101, 50, false));
		assertTrue(KillCountNotifier.isMilestone(1, 50, true));
		assertFalse(KillCountNotifier.isMilestone(1, 50, false));
		assertTrue(KillCountNotifier.isMilestone(7, 1, false));
	}

	@Test
	public void zeroIntervalTurnsMilestonesOff()
	{
		assertFalse(KillCountNotifier.isMilestone(100, 0, false));
		assertTrue(KillCountNotifier.isMilestone(1, 0, true));
	}

	@Test
	public void rendersPlaceholders()
	{
		assertEquals("Zulrah (KC 1,234) in 0:56.50",
			Template.render("$boss$kc$time", KillCountNotifier.placeholders(new Kill("Zulrah", 1234, "0:56.50", true))));
	}

	@Test
	public void missingTimeLeavesNothingBehind()
	{
		assertEquals("Zulrah (KC 50)!",
			Template.render("$boss$kc$time!", KillCountNotifier.placeholders(new Kill("Zulrah", 50, null, false))));
	}
}
