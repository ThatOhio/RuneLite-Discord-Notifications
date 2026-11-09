package com.discordnotifications.notifiers;

import com.discordnotifications.AchievementDiary;
import com.discordnotifications.Template;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DiaryNotifierTest
{
	@Test
	public void parsesDifficultyAndArea()
	{
		assertEquals(AchievementDiary.KARAMJA_HARD,
			DiaryNotifier.parse("Congratulations! You have completed all of the hard tasks in the Karamja area. Speak to Pirate Jackie the Fruit to claim your reward."));
	}

	@Test
	public void matchesAreaNameThatDiffersFromTheDiary()
	{
		assertEquals(AchievementDiary.WESTERN_HARD,
			DiaryNotifier.parse("Congratulations! You have completed all of the hard tasks in the Western Province area. Speak to the Elder Gnome child at the Gnome Stronghold to claim your reward."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(DiaryNotifier.parse("Congratulations, you've completed a hard combat task: Whack-a-Mole."));
		assertNull(DiaryNotifier.parse("Congratulations! You have completed all of the silly tasks in the Karamja area."));
		assertNull(DiaryNotifier.parse("Congratulations! You have completed all of the easy tasks in the Narnia area."));
	}

	@Test
	public void karamjaBelowEliteNeedsTwo()
	{
		assertFalse(AchievementDiary.KARAMJA_EASY.isComplete(1));
		assertTrue(AchievementDiary.KARAMJA_EASY.isComplete(2));
		assertTrue(AchievementDiary.KARAMJA_ELITE.isComplete(1));
		assertTrue(AchievementDiary.VARROCK_EASY.isComplete(1));
		assertFalse(AchievementDiary.VARROCK_EASY.isComplete(0));
	}

	@Test
	public void looksUpByVarbit()
	{
		assertEquals(AchievementDiary.WILDERNESS_ELITE, AchievementDiary.fromVarbit(AchievementDiary.WILDERNESS_ELITE.getVarbit()));
		assertNull(AchievementDiary.fromVarbit(-1));
	}

	@Test
	public void countsTheOneJustFinishedEvenIfTheVarbitIsBehind()
	{
		int ardougne = AchievementDiary.ARDOUGNE_EASY.getVarbit();
		assertEquals(2, DiaryNotifier.totalCompleted(AchievementDiary.VARROCK_ELITE, varbit -> varbit == ardougne ? 1 : 0));
	}

	@Test
	public void startedKaramjaDoesNotCount()
	{
		int karamja = AchievementDiary.KARAMJA_EASY.getVarbit();
		assertEquals(1, DiaryNotifier.totalCompleted(AchievementDiary.VARROCK_ELITE, varbit -> varbit == karamja ? 1 : 0));
	}

	@Test
	public void rendersPlaceholders()
	{
		assertEquals("Hard Lumbridge & Draynor 12 400/492 30/40",
			Template.render("$difficulty $area $totalDiaries $tasksComplete/$tasksTotal $areaTasksComplete/$areaTasksTotal",
				DiaryNotifier.placeholders(AchievementDiary.LUMBRIDGE_HARD, 12, 400, 492, 30, 40)));
	}

	@Test
	public void everyAreaHasAScriptId()
	{
		for (AchievementDiary diary : AchievementDiary.values())
		{
			assertTrue(diary.getArea(), DiaryNotifier.SCRIPT_AREA_IDS.containsKey(diary.getArea()));
		}
	}
}
