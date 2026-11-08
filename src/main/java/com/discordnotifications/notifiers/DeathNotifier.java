package com.discordnotifications.notifiers;

import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
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
				send(Notification.builder()
					.type(NotificationType.DEATH)
					.text(config.deathMessage())
					.screenshot(config.sendDeathScreenshot())
					.build());
			}
		}
	}
}
