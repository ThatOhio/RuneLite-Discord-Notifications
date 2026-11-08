package com.discordnotifications.notifiers;

import com.discordnotifications.ClueTier;
import com.discordnotifications.ItemStackFormatter.Stack;
import com.discordnotifications.Template;
import com.discordnotifications.notifiers.ClueNotifier.CompletedClue;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClueNotifierTest
{
	@Test
	public void parsesTierAndCount()
	{
		assertEquals(new CompletedClue(ClueTier.MEDIUM, 1312), ClueNotifier.parse("You have completed 1312 medium Treasure Trails."));
		assertEquals(new CompletedClue(ClueTier.BEGINNER, 42), ClueNotifier.parse("You have completed 42 beginner Treasure Trails."));
		assertEquals(new CompletedClue(ClueTier.MASTER, 2), ClueNotifier.parse("You have completed 2 master Treasure Trails."));
	}

	@Test
	public void parsesSingleClue()
	{
		assertEquals(new CompletedClue(ClueTier.EASY, 1), ClueNotifier.parse("You have completed 1 easy Treasure Trail."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(ClueNotifier.parse("You have completed 5 quests."));
		assertNull(ClueNotifier.parse("Well done, you've completed the Treasure Trail!"));
		assertNull(ClueNotifier.parse("You have completed 3 impossible Treasure Trails."));
	}

	@Test
	public void rendersPlaceholders()
	{
		CompletedClue completed = new CompletedClue(ClueTier.HARD, 1234);
		assertEquals("Hard 1,234 2 x Rune scimitar (30K), 1 x Law rune (200) 30.2K",
			Template.render("$tier $count $items $value", ClueNotifier.placeholders(completed, Arrays.asList(
				new Stack("Rune scimitar", 2, 15_000),
				new Stack("Law rune", 1, 200)))));
	}

	@Test
	public void rendersWithNoRewards()
	{
		CompletedClue completed = new CompletedClue(ClueTier.EASY, 1);
		assertEquals("[]0", Template.render("[$items]$value", ClueNotifier.placeholders(completed, Collections.emptyList())));
	}
}
