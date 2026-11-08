package com.discordnotifications.notifiers;

import com.discordnotifications.CombatAchievementTier;
import com.discordnotifications.Template;
import com.discordnotifications.notifiers.CombatAchievementNotifier.CompletedTask;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CombatAchievementNotifierTest
{
	private static final Map<CombatAchievementTier, Integer> THRESHOLDS = new EnumMap<>(CombatAchievementTier.class);

	static
	{
		THRESHOLDS.put(CombatAchievementTier.EASY, 33);
		THRESHOLDS.put(CombatAchievementTier.MEDIUM, 115);
		THRESHOLDS.put(CombatAchievementTier.HARD, 304);
		THRESHOLDS.put(CombatAchievementTier.ELITE, 820);
		THRESHOLDS.put(CombatAchievementTier.MASTER, 1465);
		THRESHOLDS.put(CombatAchievementTier.GRANDMASTER, 2005);
	}

	private static CombatAchievementTier unlocked(int totalPoints, int taskPoints)
	{
		return CombatAchievementNotifier.unlockedTier(totalPoints, taskPoints, THRESHOLDS::get);
	}

	private static String render(String template, CompletedTask completed, int totalPoints, CombatAchievementTier unlocked)
	{
		return Template.render(template, CombatAchievementNotifier.placeholders(completed, totalPoints, unlocked));
	}

	@Test
	public void parsesTierAndTask()
	{
		assertEquals(new CompletedTask(CombatAchievementTier.EASY, "Defence? What Defence?"),
			CombatAchievementNotifier.parse("Congratulations, you've completed an easy combat task: Defence? What Defence?."));
		assertEquals(new CompletedTask(CombatAchievementTier.MEDIUM, "Fire in the Hole!"),
			CombatAchievementNotifier.parse("Congratulations, you've completed a medium combat task: Fire in the Hole!."));
		assertEquals(new CompletedTask(CombatAchievementTier.GRANDMASTER, "Inferno Grandmaster"),
			CombatAchievementNotifier.parse("Congratulations, you've completed a grandmaster combat task: Inferno Grandmaster."));
	}

	@Test
	public void stripsPointsSuffix()
	{
		assertEquals(new CompletedTask(CombatAchievementTier.HARD, "I Can't Reach That"),
			CombatAchievementNotifier.parse("Congratulations, you've completed a hard combat task: I Can't Reach That (3 points)."));
		assertEquals(new CompletedTask(CombatAchievementTier.EASY, "A Slow Death"),
			CombatAchievementNotifier.parse("Congratulations, you've completed an easy combat task: A Slow Death (1 point)."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(CombatAchievementNotifier.parse("Congratulations, you just advanced a Strength level."));
		assertNull(CombatAchievementNotifier.parse("Congratulations, you've completed a hard task: The Frozen Door."));
		assertNull(CombatAchievementNotifier.parse("Congratulations, you've completed a gachi combat task: Swordfight with the homies."));
	}

	@Test
	public void unlocksTierWhenThresholdCrossed()
	{
		assertEquals(CombatAchievementTier.MEDIUM, unlocked(115, 2));
		assertEquals(CombatAchievementTier.MEDIUM, unlocked(116, 3));
		assertEquals(CombatAchievementTier.GRANDMASTER, unlocked(2005, 6));
	}

	@Test
	public void noUnlockWhenThresholdNotCrossed()
	{
		assertNull(unlocked(114, 2));
		assertNull(unlocked(118, 3));
		assertNull(unlocked(2100, 6));
	}

	@Test
	public void noUnlockWhenVarbitsNotLoaded()
	{
		assertNull(CombatAchievementNotifier.unlockedTier(120, 6, tier -> 0));
		assertNull(unlocked(0, 6));
	}

	@Test
	public void rendersPlaceholders()
	{
		CompletedTask completed = new CompletedTask(CombatAchievementTier.HARD, "Whack-a-Mole");
		assertEquals("Whack-a-Mole Hard 3 1,234 and unlocked the Medium tier",
			render("$task $tier $points $totalPoints$tierUnlocked", completed, 1234, CombatAchievementTier.MEDIUM));
	}

	@Test
	public void unknownValuesAreBlank()
	{
		CompletedTask completed = new CompletedTask(CombatAchievementTier.HARD, "Whack-a-Mole");
		assertEquals("Whack-a-Mole/", render("$task$tierUnlocked/$totalPoints", completed, 0, null));
	}

	@Test
	public void oldPlaceholderStillWorks()
	{
		CompletedTask completed = new CompletedTask(CombatAchievementTier.HARD, "Whack-a-Mole");
		assertEquals("Whack-a-Mole", render("$achievement", completed, 100, null));
	}
}
