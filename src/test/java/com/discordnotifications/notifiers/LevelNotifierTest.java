package com.discordnotifications.notifiers;

import com.discordnotifications.DiscordNotificationsConfig;
import com.discordnotifications.DiscordWebhookService;
import com.discordnotifications.Notification;
import com.discordnotifications.Template;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.StatChanged;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LevelNotifierTest
{
	private static final int SETTLE_TICKS = 16;
	private static final int TICKS_TO_POST = 3;

	@Mock
	private Client client;

	@Mock
	private DiscordNotificationsConfig config;

	@Mock
	private DiscordWebhookService webhookService;

	@Mock
	private HeldNotifications heldNotifications;

	@Mock
	private Player player;

	@InjectMocks
	private LevelNotifier notifier;

	@Before
	public void setUp()
	{
		lenient().when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
		lenient().when(client.getLocalPlayer()).thenReturn(player);
		lenient().when(player.getName()).thenReturn("Bob");
		lenient().when(client.getRealSkillLevel(any())).thenReturn(50);
		lenient().when(config.includeLevelling()).thenReturn(true);
		lenient().when(config.levelMessage()).thenReturn("$name: $skills");
	}

	private void ticks(int count)
	{
		for (int i = 0; i < count; i++)
		{
			notifier.onGameTick();
		}
	}

	private void stat(Skill skill, int level)
	{
		notifier.onStatChanged(new StatChanged(skill, Experience.getXpForLevel(level), level, level));
	}

	private void assertNothingPosted()
	{
		verify(webhookService, never()).send(any());
	}

	private void assertPosted(String text)
	{
		ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
		verify(webhookService).send(sent.capture());
		assertEquals(text, sent.getValue().getText());
	}

	@Test
	public void statsAreIgnoredUntilTheyHaveSettled()
	{
		ticks(SETTLE_TICKS - 1);
		stat(Skill.ATTACK, 51);
		ticks(1 + TICKS_TO_POST);
		assertNothingPosted();

		stat(Skill.ATTACK, 51);
		ticks(TICKS_TO_POST);
		assertPosted("Bob: Attack to 51");
	}

	@Test
	public void seededLevelIsNotALevelUp()
	{
		ticks(SETTLE_TICKS);
		stat(Skill.ATTACK, 50);
		ticks(TICKS_TO_POST);
		assertNothingPosted();
	}

	@Test
	public void seedsVirtualLevelsFromXp()
	{
		int xp = Experience.getXpForLevel(100);
		when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(99);
		when(client.getSkillExperience(Skill.ATTACK)).thenReturn(xp);
		lenient().when(config.notifyVirtualLevels()).thenReturn(true);

		ticks(SETTLE_TICKS);
		notifier.onStatChanged(new StatChanged(Skill.ATTACK, xp, 99, 99));
		ticks(TICKS_TO_POST);
		assertNothingPosted();
	}

	@Test
	public void waitsSoSkillsLevelledTogetherShareALine()
	{
		ticks(SETTLE_TICKS);
		stat(Skill.ATTACK, 51);
		ticks(1);
		stat(Skill.STRENGTH, 51);
		ticks(1);
		assertNothingPosted();

		ticks(1);
		assertPosted("Bob: Attack to 51 and Strength to 51");
	}

	@Test
	public void levelBelowTheSeedStartsOver()
	{
		ticks(SETTLE_TICKS);
		stat(Skill.ATTACK, 40);

		stat(Skill.ATTACK, 51);
		ticks(SETTLE_TICKS - 1);
		assertNothingPosted();

		ticks(1);
		stat(Skill.ATTACK, 51);
		ticks(TICKS_TO_POST);
		assertPosted("Bob: Attack to 51");
	}

	@Test
	public void resetDropsThePendingPostAndTheSeed()
	{
		ticks(SETTLE_TICKS);
		stat(Skill.ATTACK, 51);
		notifier.reset();

		ticks(TICKS_TO_POST);
		stat(Skill.ATTACK, 52);
		ticks(TICKS_TO_POST);
		assertNothingPosted();
	}

	@Test
	public void hoppingToAWorldWithItsOwnStatsReseeds()
	{
		ticks(SETTLE_TICKS);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));
		stat(Skill.ATTACK, 51);
		ticks(TICKS_TO_POST);
		assertNothingPosted();
	}

	@Test
	public void hoppingBetweenNormalWorldsKeepsTheSeed()
	{
		ticks(SETTLE_TICKS);
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
		stat(Skill.ATTACK, 51);
		ticks(TICKS_TO_POST);
		assertPosted("Bob: Attack to 51");
	}

	private static Map<String, Integer> levels(Object... skillsAndLevels)
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (int i = 0; i < skillsAndLevels.length; i += 2)
		{
			levels.put((String) skillsAndLevels[i], (Integer) skillsAndLevels[i + 1]);
		}
		return levels;
	}

	private static String render(String template, Map<String, Integer> levels)
	{
		return Template.render(template, LevelNotifier.levelPlaceholders(levels, 1500, 88, 4_600_000_000L));
	}

	@Test
	public void oneSkill()
	{
		assertEquals("Attack to 70 / 1500", render("$skills / $total", levels("Attack", 70)));
	}

	@Test
	public void twoSkills()
	{
		assertEquals("Attack to 70 and Hitpoints to 71",
			render("$skills", levels("Attack", 70, "Hitpoints", 71)));
	}

	@Test
	public void threeSkillsKeepOrder()
	{
		assertEquals("Attack to 70, Strength to 72 and Hitpoints to 71",
			render("$skills", levels("Attack", 70, "Strength", 72, "Hitpoints", 71)));
	}

	@Test
	public void oldPlaceholdersGiveTheFirstSkill()
	{
		assertEquals("Attack to 70", render("$skill to $level", levels("Attack", 70, "Hitpoints", 71)));
	}

	@Test
	public void combatPlaceholder()
	{
		assertEquals("Combat to 88 / 88", render("$skills / $combat", levels("Combat", 88)));
	}

	@Test
	public void totalXpPlaceholder()
	{
		assertEquals("1500 / 4,600,000,000", render("$total / $totalXp", levels("Attack", 70)));
	}

	@Test
	public void maxXpReadsAsWords()
	{
		assertEquals("Attack to Max XP", render("$skills", levels("Attack", LevelNotifier.MAX_XP_LEVEL)));
		assertEquals("Attack Max XP", render("$skill $level", levels("Attack", LevelNotifier.MAX_XP_LEVEL)));
	}

	@Test
	public void virtualLevels()
	{
		assertEquals(98, LevelNotifier.virtualLevel(98, 12_000_000));
		assertEquals(99, LevelNotifier.virtualLevel(99, 13_034_431));
		assertEquals(100, LevelNotifier.virtualLevel(99, 14_391_160));
		assertEquals(126, LevelNotifier.virtualLevel(99, 199_999_999));
		assertEquals(LevelNotifier.MAX_XP_LEVEL, LevelNotifier.virtualLevel(99, 200_000_000));
	}

	@Test
	public void intervalAndOverride()
	{
		assertTrue(LevelNotifier.meetsInterval(73, 1, 0));
		assertTrue(LevelNotifier.meetsInterval(70, 10, 0));
		assertFalse(LevelNotifier.meetsInterval(73, 10, 0));
		assertTrue(LevelNotifier.meetsInterval(99, 10, 0));
		assertTrue(LevelNotifier.meetsInterval(LevelNotifier.MAX_XP_LEVEL, 10, 0));

		assertFalse(LevelNotifier.meetsInterval(73, 10, 80));
		assertTrue(LevelNotifier.meetsInterval(80, 10, 80));
		assertTrue(LevelNotifier.meetsInterval(83, 10, 80));
	}
}
