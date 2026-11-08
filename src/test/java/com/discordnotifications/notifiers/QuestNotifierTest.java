package com.discordnotifications.notifiers;

import com.discordnotifications.Template;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class QuestNotifierTest
{
	private static String parse(String text)
	{
		return QuestNotifier.parseQuestCompletedWidget(text);
	}

	@Test
	public void parsesRecipeForDisaster()
	{
		assertEquals("Recipe for Disaster - Another Cook's Quest", parse("You have completed Another Cook's Quest!"));
		assertEquals("Recipe for Disaster - Another Cook's Quest", parse("You have assisted the Lumbridge Cook... again!"));
		assertEquals("Recipe for Disaster - Goblin Generals", parse("You have freed the Goblin Generals!"));
		assertEquals("Recipe for Disaster - Sir Amik Varze", parse("You have freed Sir Amik Varze!"));
		assertEquals("Recipe for Disaster - Skrach Uglogwee", parse("You have freed Skrach 'Bone Crusher' Uglogwee!"));
		assertEquals("Recipe for Disaster", parse("You have completed Recipe for Disaster!"));
	}

	@Test
	public void keepsQuestInNamesThatHaveIt()
	{
		assertEquals("Doric's Quest", parse("You have completed Doric's Quest!"));
		assertEquals("Heroes Quest", parse("You have completed the Heroes' Quest!"));
	}

	@Test
	public void parsesNumberedQuests()
	{
		assertEquals("Dragon Slayer I", parse("You have completed the Dragon Slayer I quest!"));
		assertEquals("Dragon Slayer II", parse("You have completed Dragon Slayer II!"));
		assertEquals("Rag and Bone Man II", parse("You have completed Rag and Bone Man II!"));
		assertEquals("Fairytale II - Cure a Queen", parse("You have completed Fairytale II - Cure a Queen!"));
	}

	@Test
	public void skipsPartialCompletion()
	{
		assertNull(parse("You have... kind of... completed Hazeel Cult!"));
		assertEquals("Hazeel Cult", parse("You have completed Hazeel Cult!"));
	}

	@Test
	public void fallsBackToTitleWhenNothingMatches()
	{
		assertEquals("Congratulations on finishing Some New Quest!", parse("Congratulations on finishing Some New Quest!"));
		assertNull(parse(""));
	}

	@Test
	public void rendersPlaceholders()
	{
		assertEquals("Cook's Assistant 22/156 44/293",
			Template.render("$quest $completed/$totalQuests $qp/$qpMax", QuestNotifier.placeholders("Cook's Assistant", 22, 156, 44, 293)));
	}

	@Test
	public void progressAddsItsOwnSpace()
	{
		assertEquals("Cook's Assistant (44/293 QP)",
			Template.render("$quest$progress", QuestNotifier.placeholders("Cook's Assistant", 22, 156, 44, 293)));
	}

	@Test
	public void progressIsBlankWhenUnknown()
	{
		assertEquals("Cook's Assistant",
			Template.render("$quest$progress", QuestNotifier.placeholders("Cook's Assistant", 22, 156, 0, 293)));
	}

	@Test
	public void leavesNumbersBlankWhenUnknown()
	{
		assertEquals("Cook's Assistant / 44/293",
			Template.render("$quest $completed/$totalQuests $qp/$qpMax", QuestNotifier.placeholders("Cook's Assistant", 0, 156, 44, 293)));
		assertEquals("Cook's Assistant 22/156 /",
			Template.render("$quest $completed/$totalQuests $qp/$qpMax", QuestNotifier.placeholders("Cook's Assistant", 22, 156, 44, 0)));
	}
}
