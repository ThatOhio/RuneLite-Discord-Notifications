package com.discordnotifications.notifiers;

import com.discordnotifications.DangerLevel;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.WildcardList;
import com.discordnotifications.WorldUtils;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPreFired;

import javax.inject.Singleton;
import java.lang.ref.WeakReference;
import java.util.Collections;

@Singleton
public class DeathNotifier extends BaseNotifier
{
	private static final String TOA_WIPE_MESSAGE = "You failed to survive the Tombs of Amascut";
	private static final String TOB_WIPE_MESSAGE = "Your party has failed";
	private static final String DOOM_MESSAGE = "You have been doomed!";
	private static final int TOB_HUD_PORTAL_SCRIPT = 2307;
	private static final int KILLER_WINDOW_TICKS = 17;

	private WeakReference<Actor> lastTarget = new WeakReference<>(null);
	private int lastTargetTick;

	@Override
	public void reset()
	{
		lastTarget.clear();
	}

	public void onActorDeath(ActorDeath actorDeath)
	{
		Actor actor = actorDeath.getActor();
		boolean self = actor == client.getLocalPlayer();
		if (self && config.includeDeaths())
		{
			notifyDeath(false);
		}

		if (self || actor == lastTarget.get())
		{
			lastTarget.clear();
		}
	}

	public void onGameMessage(String message)
	{
		if (message.equals(DOOM_MESSAGE))
		{
			// Doom kills you without reaching 0 HP, so there's no ActorDeath
			Player player = client.getLocalPlayer();
			if (config.includeDeaths() && player != null && player.getHealthRatio() > 0
				&& WorldUtils.getLocation(client, player).getRegionID() == WorldUtils.FORTIS_REGION)
			{
				notifyDeath(true);
			}
		}
		else if (message.contains(TOA_WIPE_MESSAGE) && wipeNotAlreadySent())
		{
			notifyDeath(true);
		}
	}

	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != TOB_HUD_PORTAL_SCRIPT || event.getScriptEvent() == null)
		{
			return;
		}

		Object[] args = event.getScriptEvent().getArguments();
		if (args != null && args.length > 1 && args[1] instanceof String
			&& ((String) args[1]).contains(TOB_WIPE_MESSAGE) && wipeNotAlreadySent())
		{
			notifyDeath(true);
		}
	}

	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		Actor target = event.getTarget();
		if (target != null && target.getCombatLevel() > 0)
		{
			lastTarget = new WeakReference<>(target);
		}
		lastTargetTick = client.getTickCount();
	}

	// With safe deaths on, or on a hardcore, the ActorDeath in the raid was already posted
	private boolean wipeNotAlreadySent()
	{
		return config.includeDeaths() && config.ignoreSafeDeaths() && !WorldUtils.isHardcore(client);
	}

	private void notifyDeath(boolean alwaysDangerous)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		int regionId = WorldUtils.getLocation(client, player).getRegionID();
		if (WildcardList.parse(config.deathIgnoredRegions()).matches(String.valueOf(regionId)))
		{
			return;
		}

		if (!alwaysDangerous && config.ignoreSafeDeaths() && WorldUtils.getDangerLevel(client, regionId) == DangerLevel.SAFE)
		{
			return;
		}

		Actor killer = findKiller(player);
		String killerName = killer == null ? "" : sanitize(killer.getName());

		String template = config.deathMessage();
		if (!killerName.isEmpty())
		{
			template = killer instanceof Player ? config.deathPvpMessage() : config.deathNpcMessage();
		}

		send(Notification.builder()
			.type(NotificationType.DEATH)
			.text(template)
			.placeholders(Collections.singletonMap("killer", killerName))
			.screenshot(config.sendDeathScreenshot())
			.build());
	}

	private Actor findKiller(Player player)
	{
		Actor target = lastTarget.get();
		if (target == null || target.isDead() || target.getInteracting() != player)
		{
			return null;
		}

		// InteractingChanged doesn't fire during a long fight, so still targeting it counts as recent
		if (player.getInteracting() != target && client.getTickCount() - lastTargetTick > KILLER_WINDOW_TICKS)
		{
			return null;
		}

		if (target instanceof Player)
		{
			// Friends following you also show up as interacting
			Player other = (Player) target;
			if (!WorldUtils.canBePked(client) || other.isFriend() || other.isClanMember() || other.isFriendsChatMember())
			{
				return null;
			}
		}

		return target;
	}
}
