package com.discordnotifications.notifiers;

import com.discordnotifications.LeagueTaskTier;
import com.discordnotifications.Template;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LeaguesNotifierTest
{
	private static final int[] THRESHOLDS = {2_000, 4_000, 10_000, 22_000, 32_000, 47_500, 65_000};

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
	public void earnsTrophyWhenTaskCrossesThreshold()
	{
		assertEquals("Iron", LeaguesNotifier.earnedTrophy(THRESHOLDS, 4_010, 80));
		assertEquals("Iron", LeaguesNotifier.earnedTrophy(THRESHOLDS, 4_000, 10));
		assertEquals("Bronze", LeaguesNotifier.earnedTrophy(THRESHOLDS, 2_000, 400));
	}

	@Test
	public void noTrophyWhenThresholdAlreadyPassedOrNotReached()
	{
		assertNull(LeaguesNotifier.earnedTrophy(THRESHOLDS, 4_090, 80));
		assertNull(LeaguesNotifier.earnedTrophy(THRESHOLDS, 3_990, 80));
		assertNull(LeaguesNotifier.earnedTrophy(THRESHOLDS, 1_000, 10));
		assertNull(LeaguesNotifier.earnedTrophy(new int[0], 4_010, 80));
	}

	@Test
	public void ignoresThresholdsPastTheKnownTrophies()
	{
		assertNull(LeaguesNotifier.earnedTrophy(new int[]{1, 2, 3, 4, 5, 6, 7, 80_000}, 80_010, 80));
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
