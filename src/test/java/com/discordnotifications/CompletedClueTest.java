package com.discordnotifications;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CompletedClueTest
{
	@Test
	public void parsesTierAndCount()
	{
		assertEquals(new CompletedClue(ClueTier.MEDIUM, 1312), CompletedClue.parse("You have completed 1312 medium Treasure Trails."));
		assertEquals(new CompletedClue(ClueTier.BEGINNER, 42), CompletedClue.parse("You have completed 42 beginner Treasure Trails."));
		assertEquals(new CompletedClue(ClueTier.MASTER, 2), CompletedClue.parse("You have completed 2 master Treasure Trails."));
	}

	@Test
	public void parsesSingleClue()
	{
		assertEquals(new CompletedClue(ClueTier.EASY, 1), CompletedClue.parse("You have completed 1 easy Treasure Trail."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(CompletedClue.parse("You have completed 5 quests."));
		assertNull(CompletedClue.parse("Well done, you've completed the Treasure Trail!"));
		assertNull(CompletedClue.parse("You have completed 3 impossible Treasure Trails."));
	}
}
