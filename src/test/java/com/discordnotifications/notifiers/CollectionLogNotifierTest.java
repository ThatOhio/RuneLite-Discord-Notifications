package com.discordnotifications.notifiers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
}
