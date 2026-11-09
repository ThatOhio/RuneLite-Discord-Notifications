package com.discordnotifications.notifiers;

import com.discordnotifications.CombatAchievementTier;
import com.discordnotifications.NotificationType;
import lombok.Value;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class CombatAchievementNotifier extends BaseNotifier
{
	private static final Pattern TASK_REGEX = Pattern.compile("Congratulations, you've completed an? (?<tier>\\w+) combat task: (?<task>.+)\\.");
	private static final Pattern POINTS_SUFFIX = Pattern.compile("\\s+\\(\\d+ points?\\)$");

	@Inject
	private ClientThread clientThread;

	public void onGameMessage(String chatMessage)
	{
		if (!config.includeCombatAchievements())
		{
			return;
		}

		CompletedTask completed = parse(chatMessage);
		if (completed != null && completed.getTier().ordinal() >= config.minCombatAchievementTier().ordinal())
		{
			// CA_POINTS hasn't updated yet when the message arrives
			clientThread.invokeAtTickEnd(() -> sendTask(completed));
		}
	}

	private void sendTask(CompletedTask completed)
	{
		int totalPoints = client.getVarbitValue(VarbitID.CA_POINTS);
		CombatAchievementTier unlocked = unlockedTier(totalPoints, completed.getTier().getPoints(),
			tier -> client.getVarbitValue(tier.getThresholdVarbit()));

		send(NotificationType.COMBAT_ACHIEVEMENT, config.combatAchievementsMessage(),
			placeholders(completed, totalPoints, unlocked),
			config.sendCombatAchievementsScreenshot());
	}

	static CompletedTask parse(String message)
	{
		Matcher matcher = TASK_REGEX.matcher(message);
		if (!matcher.find())
		{
			return null;
		}

		CombatAchievementTier tier = CombatAchievementTier.fromName(matcher.group("tier"));
		if (tier == null)
		{
			return null;
		}

		return new CompletedTask(tier, POINTS_SUFFIX.matcher(matcher.group("task")).replaceFirst(""));
	}

	static CombatAchievementTier unlockedTier(int totalPoints, int taskPoints, ToIntFunction<CombatAchievementTier> thresholds)
	{
		for (CombatAchievementTier tier : CombatAchievementTier.values())
		{
			int threshold = thresholds.applyAsInt(tier);
			if (threshold > 0 && totalPoints >= threshold && totalPoints - taskPoints < threshold)
			{
				return tier;
			}
		}
		return null;
	}

	static Map<String, String> placeholders(CompletedTask completed, int totalPoints, CombatAchievementTier unlocked)
	{
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("task", completed.getTask());
		placeholders.put("achievement", completed.getTask());
		placeholders.put("tier", completed.getTier().toString());
		placeholders.put("points", String.valueOf(completed.getTier().getPoints()));
		if (totalPoints > 0)
		{
			placeholders.put("totalPoints", QuantityFormatter.formatNumber(totalPoints));
		}
		if (unlocked != null)
		{
			placeholders.put("tierUnlocked", " and unlocked the " + unlocked + " tier");
		}
		return placeholders;
	}

	@Value
	static class CompletedTask
	{
		CombatAchievementTier tier;
		String task;
	}
}
