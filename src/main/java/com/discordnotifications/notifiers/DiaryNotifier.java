package com.discordnotifications.notifiers;

import com.discordnotifications.AchievementDiary;
import com.discordnotifications.DiaryDifficulty;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;

import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class DiaryNotifier extends BaseNotifier
{
	private static final Pattern COMPLETION_REGEX = Pattern.compile("Congratulations! You have completed all of the (?<difficulty>.+) tasks in the (?<area>.+?) area");
	private static final int SNAPSHOT_DELAY_TICKS = 4;
	private static final int COOLDOWN_TICKS = 2;

	private final Map<AchievementDiary, Integer> snapshot = new EnumMap<>(AchievementDiary.class);
	private final Map<AchievementDiary, Integer> lastPostedTick = new EnumMap<>(AchievementDiary.class);
	private int ticksUntilSnapshot = SNAPSHOT_DELAY_TICKS;

	@Override
	public void reset()
	{
		snapshot.clear();
		lastPostedTick.clear();
		ticksUntilSnapshot = SNAPSHOT_DELAY_TICKS;
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			reset();
		}
	}

	public void onGameTick()
	{
		if (!snapshot.isEmpty() || --ticksUntilSnapshot > 0)
		{
			return;
		}

		for (AchievementDiary diary : AchievementDiary.values())
		{
			snapshot.put(diary, client.getVarbitValue(diary.getVarbit()));
		}
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		AchievementDiary diary = AchievementDiary.fromVarbit(event.getVarbitId());
		if (diary == null || snapshot.isEmpty())
		{
			return;
		}

		int value = event.getValue();
		int previous = snapshot.get(diary);
		if (value < previous)
		{
			// Shouldn't happen, but it would post again when the value came back
			reset();
			return;
		}

		snapshot.put(diary, value);
		if (!diary.isComplete(previous) && diary.isComplete(value))
		{
			notifyCompleted(diary);
		}
	}

	public void onMessageBox(String message)
	{
		AchievementDiary diary = parse(message);
		if (diary == null)
		{
			return;
		}

		if (!snapshot.isEmpty())
		{
			// The varbit can land after this message
			snapshot.put(diary, diary.getCompleteValue());
		}
		notifyCompleted(diary);
	}

	private void notifyCompleted(AchievementDiary diary)
	{
		if (!config.includeDiaries() || diary.getDifficulty().ordinal() < config.minDiaryDifficulty().ordinal())
		{
			return;
		}

		int tick = client.getTickCount();
		Integer lastPosted = lastPostedTick.put(diary, tick);
		if (lastPosted != null && tick - lastPosted < COOLDOWN_TICKS)
		{
			return;
		}

		send(Notification.builder()
			.type(NotificationType.DIARY)
			.text(config.diaryMessage())
			.placeholders(placeholders(diary, totalCompleted(diary, client::getVarbitValue)))
			.screenshot(config.sendDiaryScreenshot())
			.build());
	}

	static AchievementDiary parse(String message)
	{
		Matcher matcher = COMPLETION_REGEX.matcher(message);
		if (!matcher.find())
		{
			return null;
		}

		DiaryDifficulty difficulty = DiaryDifficulty.fromName(matcher.group("difficulty"));
		return difficulty == null ? null : AchievementDiary.find(difficulty, matcher.group("area").trim());
	}

	// The varbit for the one just finished may not have landed yet
	static int totalCompleted(AchievementDiary justCompleted, IntUnaryOperator varbits)
	{
		int total = 0;
		for (AchievementDiary diary : AchievementDiary.values())
		{
			if (diary == justCompleted || diary.isComplete(varbits.applyAsInt(diary.getVarbit())))
			{
				total++;
			}
		}
		return total;
	}

	static Map<String, String> placeholders(AchievementDiary diary, int totalDiaries)
	{
		return ImmutableMap.of(
			"area", diary.getArea(),
			"difficulty", diary.getDifficulty().toString(),
			"totalDiaries", String.valueOf(totalDiaries));
	}
}
