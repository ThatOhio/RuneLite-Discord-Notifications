package com.discordnotifications;

import org.junit.Test;

import java.util.List;

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
}
