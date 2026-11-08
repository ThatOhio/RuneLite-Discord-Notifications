package com.discordnotifications;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class NotificationQueueTest
{
	private static Notification of(NotificationType type, String text)
	{
		return Notification.builder().type(type).text(text).build();
	}

	@Test
	public void ordersByTypePriority()
	{
		assertEquals("pet\nclog\nlevel\ndeath", NotificationQueue.join(List.of(
			of(NotificationType.DEATH, "death"),
			of(NotificationType.LEVEL, "level"),
			of(NotificationType.COLLECTION_LOG, "clog"),
			of(NotificationType.PET, "pet"))));
	}

	@Test
	public void keepsArrivalOrderWithinAType()
	{
		assertEquals("first\nsecond", NotificationQueue.join(List.of(
			of(NotificationType.LEVEL, "first"),
			of(NotificationType.LEVEL, "second"))));
	}

	private static Notification clog(String entry)
	{
		return Notification.builder()
			.type(NotificationType.COLLECTION_LOG)
			.text("clog " + entry)
			.placeholders(Map.of("entry", entry))
			.build();
	}

	private static Notification namedPet(String pet)
	{
		return Notification.builder()
			.type(NotificationType.PET)
			.text("pet " + pet)
			.placeholders(Map.of("name", "Bob", "pet", pet))
			.build();
	}

	private static Notification unnamedPet(String namedText)
	{
		return Notification.builder()
			.type(NotificationType.PET)
			.text("Bob just got a pet!")
			.namedText(namedText)
			.placeholders(Map.of("name", "Bob"))
			.build();
	}

	@Test
	public void petDropsItsOwnCollectionLogLine()
	{
		assertEquals("pet Olmlet", NotificationQueue.join(List.of(clog("olmlet"), namedPet("Olmlet"))));
	}

	@Test
	public void namedPetKeepsUnrelatedCollectionLogLine()
	{
		assertEquals("pet Olmlet\nclog Twisted bow",
			NotificationQueue.join(List.of(clog("Twisted bow"), namedPet("Olmlet"))));
	}

	@Test
	public void unnamedPetTakesItsNameFromCollectionLog()
	{
		assertEquals("Bob just got a pet: Olmlet",
			NotificationQueue.join(List.of(unnamedPet("$name just got a pet: $pet$kc"), clog("Olmlet"))));
	}

	@Test
	public void mergedPetNameIsEscapedOnce()
	{
		assertEquals("Bob got Pet\\_snake",
			NotificationQueue.join(List.of(unnamedPet("$name got $pet"), clog("Pet_snake"))));
	}

	@Test
	public void unnamedPetLeavesSeveralCollectionLogLinesAlone()
	{
		assertEquals("Bob just got a pet!\nclog Olmlet\nclog Twisted bow",
			NotificationQueue.join(List.of(unnamedPet("$pet"), clog("Olmlet"), clog("Twisted bow"))));
	}

	@Test
	public void duplicatePetDoesNotTakeACollectionLogName()
	{
		assertEquals("Bob just got a pet!\nclog Twisted bow",
			NotificationQueue.join(List.of(unnamedPet(null), clog("Twisted bow"))));
	}
}
