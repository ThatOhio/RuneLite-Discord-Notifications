package com.discordnotifications.notifiers;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Slf4j
@Singleton
public class HeldNotifications
{
	static final int MAX_TICKS = 10;

	private final List<Held> held = new ArrayList<>();

	void add(BooleanSupplier delivery)
	{
		held.add(new Held(delivery));
	}

	public void onGameTick()
	{
		held.removeIf(Held::finished);
	}

	public void reset()
	{
		held.clear();
	}

	private static class Held
	{
		final BooleanSupplier delivery;
		int ticks;

		Held(BooleanSupplier delivery)
		{
			this.delivery = delivery;
		}

		boolean finished()
		{
			if (delivery.getAsBoolean())
			{
				return true;
			}

			if (++ticks < MAX_TICKS)
			{
				return false;
			}

			log.debug("Dropping notification, local player name not available after {} ticks", MAX_TICKS);
			return true;
		}
	}
}
