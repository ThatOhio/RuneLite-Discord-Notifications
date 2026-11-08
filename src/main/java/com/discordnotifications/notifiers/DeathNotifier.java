package com.discordnotifications.notifiers;

import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;

import javax.inject.Singleton;

@Singleton
public class DeathNotifier extends BaseNotifier
{
	public void onActorDeath(ActorDeath actorDeath)
	{
		if (!config.includeDeaths()) {
			return;
		}

		Actor actor = actorDeath.getActor();
		if (actor instanceof Player) {
			Player player = (Player) actor;
			if (player == client.getLocalPlayer())
			{
				send(config.deathMessage(), config.sendDeathScreenshot());
			}
		}
	}
}
