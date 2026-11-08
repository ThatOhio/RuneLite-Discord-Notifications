package com.discordnotifications.notifiers;

import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;

import javax.inject.Singleton;

@Singleton
public class ClueNotifier extends BaseNotifier
{
	private boolean shouldSendClueMessage = false;

	@Override
	public void reset()
	{
		shouldSendClueMessage = false;
	}

	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.TRAIL_REWARDSCREEN) {
			shouldSendClueMessage = true;
		}
	}

	public void onGameTick()
	{
		boolean didCompleteClue = client.getWidget(InterfaceID.TrailRewardscreen.ITEMS) != null;

		if (shouldSendClueMessage && didCompleteClue && config.includeClues()) {
			shouldSendClueMessage = false;
			send(config.clueMessage(), config.sendClueScreenshot());
		}
	}
}
