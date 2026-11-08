package com.discordnotifications.notifiers;

import com.discordnotifications.LeagueTaskTier;
import com.discordnotifications.Template;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LeaguesNotifierTest
{
	@Test
	public void parsesAreaUnlock()
	{
		assertEquals(new LeaguesNotifier.Event(LeaguesNotifier.Kind.AREA, "Kandarin", null),
			LeaguesNotifier.parse("Congratulations, you've unlocked a new area: Kandarin."));
	}

	@Test
	public void parsesRelicUnlock()
	{
		assertEquals(new LeaguesNotifier.Event(LeaguesNotifier.Kind.RELIC, "Endless Harvest", null),
			LeaguesNotifier.parse("Congratulations, you've unlocked a new Relic: Endless Harvest."));
	}

	@Test
	public void parsesMasteryUnlock()
	{
		assertEquals(new LeaguesNotifier.Event(LeaguesNotifier.Kind.MASTERY, "Melee I", null),
			LeaguesNotifier.parse("Congratulations, you've unlocked a new Melee Combat Mastery: Melee I."));
	}

	@Test
	public void parsesTaskWithTier()
	{
		assertEquals(new LeaguesNotifier.Event(LeaguesNotifier.Kind.TASK, "The Frozen Door", LeagueTaskTier.HARD),
			LeaguesNotifier.parse("Congratulations, you've completed a hard task: The Frozen Door."));
		assertEquals(new LeaguesNotifier.Event(LeaguesNotifier.Kind.TASK, "Equip Amy's Saw", LeagueTaskTier.ELITE),
			LeaguesNotifier.parse("Congratulations, you've completed an elite task: Equip Amy's Saw."));
	}

	@Test
	public void ignoresCombatTasksAndUnknownTiers()
	{
		assertNull(LeaguesNotifier.parse("Congratulations, you've completed a hard combat task: Ready to Pounce."));
		assertNull(LeaguesNotifier.parse("Congratulations, you've completed a silly task: Nope."));
		assertNull(LeaguesNotifier.parse("You have completed 5 easy Treasure Trails."));
	}

	@Test
	public void rendersPlaceholders()
	{
		assertEquals("Hard The Frozen Door",
			Template.render("$tier $task", LeaguesNotifier.placeholders(LeaguesNotifier.parse("Congratulations, you've completed a hard task: The Frozen Door."))));
		assertEquals("Kandarin",
			Template.render("$area", LeaguesNotifier.placeholders(LeaguesNotifier.parse("Congratulations, you've unlocked a new area: Kandarin."))));
	}
}
