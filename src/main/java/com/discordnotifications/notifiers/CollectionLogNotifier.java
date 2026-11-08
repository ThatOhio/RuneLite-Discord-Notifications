package com.discordnotifications.notifiers;

import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.ScriptPreFired;

import javax.inject.Singleton;

@Singleton
public class CollectionLogNotifier extends BaseNotifier
{
	private boolean popupStarted = false;

	@Override
	public void reset()
	{
		popupStarted = false;
	}

	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		switch (scriptPreFired.getScriptId())
		{
			case ScriptID.NOTIFICATION_START:
				popupStarted = true;
				break;
			case ScriptID.NOTIFICATION_DELAY:
				if (!popupStarted)
				{
					return;
				}
				popupStarted = false;

				String topText = sanitize(client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT));
				if (topText.equalsIgnoreCase("Collection log") && config.includeCollectionLogs())
				{
					String entry = popupSubject(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT));
					send(Notification.builder()
						.type(NotificationType.COLLECTION_LOG)
						.text(config.collectionLogMessage())
						.placeholders(ImmutableMap.of("entry", entry))
						.screenshot(config.sendCollectionLogScreenshot())
						.build());
				}
				break;
		}
	}
}
