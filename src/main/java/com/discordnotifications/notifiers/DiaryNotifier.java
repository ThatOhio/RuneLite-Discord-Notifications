package com.discordnotifications.notifiers;

import com.discordnotifications.AchievementDiary;
import com.discordnotifications.DiaryDifficulty;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
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
	// CS2 procs, RuneLite has no ScriptID constants for these
	private static final int COMPLETED_TASKS_SCRIPT = 3971;
	private static final int TOTAL_TASKS_SCRIPT = 3980;
	private static final int COMPLETED_AREA_TASKS_SCRIPT = 4072;
	private static final int TOTAL_AREA_TASKS_SCRIPT = 4073;
	static final Map<String, Integer> SCRIPT_AREA_IDS = ImmutableMap.<String, Integer>builder()
		.put("Karamja", 0)
		.put("Ardougne", 1)
		.put("Falador", 2)
		.put("Fremennik", 3)
		.put("Kandarin", 4)
		.put("Desert", 5)
		.put("Lumbridge & Draynor", 6)
		.put("Morytania", 7)
		.put("Varrock", 8)
		.put("Wilderness", 9)
		.put("Western Provinces", 10)
		.put("Kourend & Kebos", 11)
		.build();

	@Inject
	private ClientThread clientThread;

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

		// Scripts can't safely run from inside these events
		clientThread.invokeLater(() -> sendCompleted(diary));
	}

	private void sendCompleted(AchievementDiary diary)
	{
		int areaId = SCRIPT_AREA_IDS.get(diary.getArea());
		send(NotificationType.DIARY, config.diaryMessage(),
			placeholders(diary, totalCompleted(diary, client::getVarbitValue),
				runIntScript(COMPLETED_TASKS_SCRIPT), runIntScript(TOTAL_TASKS_SCRIPT),
				runIntScript(COMPLETED_AREA_TASKS_SCRIPT, areaId), runIntScript(TOTAL_AREA_TASKS_SCRIPT, areaId)),
			config.sendDiaryScreenshot());
	}

	private int runIntScript(Object... args)
	{
		client.runScript(args);
		return client.getIntStack()[0];
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

	static Map<String, String> placeholders(AchievementDiary diary, int totalDiaries, int tasksComplete, int tasksTotal, int areaTasksComplete, int areaTasksTotal)
	{
		return ImmutableMap.<String, String>builder()
			.put("area", diary.getArea())
			.put("difficulty", diary.getDifficulty().toString())
			.put("totalDiaries", String.valueOf(totalDiaries))
			.put("tasksComplete", String.valueOf(tasksComplete))
			.put("tasksTotal", String.valueOf(tasksTotal))
			.put("areaTasksComplete", String.valueOf(areaTasksComplete))
			.put("areaTasksTotal", String.valueOf(areaTasksTotal))
			.build();
	}
}
