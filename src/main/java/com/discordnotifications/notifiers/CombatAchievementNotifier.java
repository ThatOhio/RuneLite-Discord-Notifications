package com.discordnotifications.notifiers;

import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.events.ScriptPreFired;

import javax.inject.Singleton;

@Singleton
public class CombatAchievementNotifier extends BaseNotifier
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
				if (topText.equalsIgnoreCase("Combat Task Completed!")
						&& config.includeCombatAchievements()
						&& client.getVarbitValue(Varbits.COMBAT_ACHIEVEMENTS_POPUP) == 0)
				{
					String task = popupSubject(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT));
					send(config.combatAchievementsMessage().replace("$achievement", task), config.sendCombatAchievementsScreenshot());
				}
				break;
		}
	}
}
