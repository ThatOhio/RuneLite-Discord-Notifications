package com.discordnotifications.notifiers;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PetNotifierTest
{
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
