package com.discordnotifications.notifiers;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CollectionLogNotifierTest
{
	@Test
	public void parsesEntryFromChat()
	{
		assertEquals("Twisted bow", CollectionLogNotifier.parseEntry("New item added to your collection log: Twisted bow"));
		assertEquals("Vet'ion jr.", CollectionLogNotifier.parseEntry("New item added to your collection log: Vet'ion jr."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(CollectionLogNotifier.parseEntry("Untradeable drop: Twisted bow"));
		assertNull(CollectionLogNotifier.parseEntry("You have a funny feeling like you're being followed."));
	}

	@Test
	public void entryHasToBeInTheDrop()
	{
		assertTrue(CollectionLogNotifier.cameFrom("Tanzanite fang", Arrays.asList("Zulrah's scales", "tanzanite fang")));
		assertFalse(CollectionLogNotifier.cameFrom("Tanzanite fang", Arrays.asList("Zulrah's scales", "Coins")));
	}

	@Test
	public void dropSetFromChatTakesAnyEntry()
	{
		assertTrue(CollectionLogNotifier.cameFrom("Araxyte fang", Collections.emptyList()));
	}

	@Test
	public void petsAreNeverInTheDrop()
	{
		assertTrue(CollectionLogNotifier.cameFrom("Pet snakeling", Collections.singletonList("Zulrah's scales")));
	}
}
