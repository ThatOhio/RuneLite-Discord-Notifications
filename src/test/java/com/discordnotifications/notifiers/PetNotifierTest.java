package com.discordnotifications.notifiers;

import com.discordnotifications.DiscordNotificationsConfig;
import com.discordnotifications.DiscordWebhookService;
import com.discordnotifications.KillCountService;
import com.discordnotifications.Notification;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PetNotifierTest
{
	private static final String FOLLOWED = "You have a funny feeling like you're being followed.";
	private static final String WOULD_HAVE_BEEN = "You have a funny feeling like you would have been followed...";
	private static final String TEMPLATE = "$name got $pet$from$kc";
	private static final int WAIT_TICKS = 6;

	@Mock
	private Client client;

	@Mock
	private DiscordNotificationsConfig config;

	@Mock
	private DiscordWebhookService webhookService;

	@Mock
	private HeldNotifications heldNotifications;

	@Mock
	private KillCountService killCountService;

	@Mock
	private Player player;

	@InjectMocks
	private PetNotifier notifier;

	@Before
	public void setUp()
	{
		lenient().when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
		lenient().when(client.getLocalPlayer()).thenReturn(player);
		lenient().when(player.getName()).thenReturn("Bob");
		lenient().when(config.includePets()).thenReturn(true);
		lenient().when(config.petMessage()).thenReturn(TEMPLATE);
	}

	private void play(String... messages)
	{
		for (String message : messages)
		{
			notifier.onGameMessage(message);
		}
		for (int i = 0; i < WAIT_TICKS; i++)
		{
			notifier.onGameTick();
		}
	}

	private Notification posted()
	{
		ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
		verify(webhookService).send(sent.capture());
		return sent.getValue();
	}

	@Test
	public void knownNameUsesTheTemplate()
	{
		play(FOLLOWED, "Untradeable drop: Olmlet");
		assertEquals("Bob got Olmlet", posted().getText());
		assertNull(posted().getNamedText());
	}

	@Test
	public void noNameFallsBackAndLeavesTheTemplateForTheQueue()
	{
		play(FOLLOWED);
		assertEquals("Bob just got a pet!", posted().getText());
		assertEquals(TEMPLATE, posted().getNamedText());
	}

	@Test
	public void fallbackKeepsSourceAndKc()
	{
		when(killCountService.getRecentPetDrop()).thenReturn(new KillCountService.Drop("Zulrah", LootRecordType.NPC, Collections.emptyList(), Instant.now()));
		when(killCountService.getKillCount(LootRecordType.NPC, "Zulrah")).thenReturn(50);
		play(FOLLOWED);
		assertEquals("Bob just got a pet from Zulrah (KC 50)!", posted().getText());
	}

	@Test
	public void templateThatDoesntNeedTheNameIsKept()
	{
		when(config.petMessage()).thenReturn("$name got a pet!");
		play(FOLLOWED);
		assertEquals("Bob got a pet!", posted().getText());
		assertEquals("$name got a pet!", posted().getNamedText());
	}

	@Test
	public void duplicateNeverWaitsOnACollectionLogName()
	{
		when(config.notifyDuplicatePets()).thenReturn(true);
		play(WOULD_HAVE_BEEN);
		assertEquals("Bob just got a pet! (would have been followed)", posted().getText());
		assertNull(posted().getNamedText());
	}

	@Test
	public void duplicatesAreSkippedWhenTurnedOff()
	{
		play(WOULD_HAVE_BEEN, "Untradeable drop: Olmlet");
		verify(webhookService, never()).send(any());
	}

	private static Matcher clan(String message)
	{
		Matcher matcher = PetNotifier.CLAN_REGEX.matcher(message);
		assertTrue(message, matcher.find());
		return matcher;
	}

	@Test
	public void petMessages()
	{
		assertTrue(PetNotifier.PET_REGEX.matcher("You have a funny feeling like you're being followed.").matches());
		assertTrue(PetNotifier.PET_REGEX.matcher("You feel something weird sneaking into your backpack.").matches());
		assertTrue(PetNotifier.PET_REGEX.matcher("You have a funny feeling like you would have been followed...").matches());
		assertFalse(PetNotifier.PET_REGEX.matcher("You feel Forsen's warmth behind you.").matches());
	}

	@Test
	public void clanBroadcastWithMilestone()
	{
		Matcher matcher = clan("[Some Clan] Zezima has a funny feeling like he would have been followed: Olmlet at 1,234 kills.");
		assertEquals("Zezima", matcher.group("user").trim());
		assertEquals("Olmlet", matcher.group("pet"));
		assertEquals("1,234 kills", matcher.group("milestone"));
	}

	@Test
	public void clanBroadcastWithoutMilestone()
	{
		Matcher matcher = clan("Zezima feels like she's acquired something special: Yami");
		assertEquals("Yami", matcher.group("pet"));
		assertNull(matcher.group("milestone"));
	}

	@Test
	public void clanBroadcastKeepsSpacesAndHyphensInName()
	{
		assertEquals("Iron-Man Bob", clan("Iron-Man Bob feels something weird sneaking into his backpack: Heron at 5,000,000 XP.").group("user").trim());
	}

	@Test
	public void clanBroadcastKeepsPetNameFullStopBeforeMilestone()
	{
		assertEquals("Vet'ion jr.", clan("Bob has a funny feeling like she's being followed: Vet'ion jr. at 50 kills.").group("pet"));
	}

	@Test
	public void namesPetFromUntradeableAndCollectionLogChat()
	{
		assertEquals("Olmlet", PetNotifier.parsePetName("Untradeable drop: Olmlet"));
		assertEquals("Pet snakeling", PetNotifier.parsePetName("New item added to your collection log: Pet snakeling"));
	}

	@Test
	public void ignoresItemsThatArentPets()
	{
		assertNull(PetNotifier.parsePetName("Untradeable drop: Ensouled dragon head"));
		assertNull(PetNotifier.parsePetName("New item added to your collection log: Abyssal whip"));
		assertNull(PetNotifier.parsePetName("random unrelated chat message"));
	}
}
