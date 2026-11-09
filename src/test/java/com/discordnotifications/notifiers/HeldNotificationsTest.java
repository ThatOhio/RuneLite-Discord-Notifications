package com.discordnotifications.notifiers;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class HeldNotificationsTest
{
	private final HeldNotifications held = new HeldNotifications();
	private final AtomicInteger attempts = new AtomicInteger();

	private void tick(int times)
	{
		for (int i = 0; i < times; i++)
		{
			held.onGameTick();
		}
	}

	@Test
	public void retriesUntilDeliveredThenStops()
	{
		AtomicBoolean named = new AtomicBoolean();
		held.add(() ->
		{
			attempts.incrementAndGet();
			return named.get();
		});

		tick(3);
		assertEquals(3, attempts.get());

		named.set(true);
		tick(3);
		assertEquals(4, attempts.get());
	}

	@Test
	public void givesUpAfterMaxTicks()
	{
		held.add(() ->
		{
			attempts.incrementAndGet();
			return false;
		});

		tick(HeldNotifications.MAX_TICKS + 5);
		assertEquals(HeldNotifications.MAX_TICKS, attempts.get());
	}

	@Test
	public void resetDiscardsHeldNotifications()
	{
		held.add(() ->
		{
			attempts.incrementAndGet();
			return true;
		});

		held.reset();
		tick(1);
		assertEquals(0, attempts.get());
	}
}
