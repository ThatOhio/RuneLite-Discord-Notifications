package com.discordnotifications.notifiers;

import com.discordnotifications.ClueTier;
import com.discordnotifications.CompletedClue;
import com.discordnotifications.ItemStackFormatter.Stack;
import com.discordnotifications.Template;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClueNotifierTest
{
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
