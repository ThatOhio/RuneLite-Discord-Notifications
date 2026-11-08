package com.discordnotifications;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

import java.util.Arrays;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum AchievementDiary
{
	ARDOUGNE_EASY(VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, "Ardougne", DiaryDifficulty.EASY),
	ARDOUGNE_MEDIUM(VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, "Ardougne", DiaryDifficulty.MEDIUM),
	ARDOUGNE_HARD(VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, "Ardougne", DiaryDifficulty.HARD),
	ARDOUGNE_ELITE(VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE, "Ardougne", DiaryDifficulty.ELITE),
	DESERT_EASY(VarbitID.DESERT_DIARY_EASY_COMPLETE, "Desert", DiaryDifficulty.EASY),
	DESERT_MEDIUM(VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, "Desert", DiaryDifficulty.MEDIUM),
	DESERT_HARD(VarbitID.DESERT_DIARY_HARD_COMPLETE, "Desert", DiaryDifficulty.HARD),
	DESERT_ELITE(VarbitID.DESERT_DIARY_ELITE_COMPLETE, "Desert", DiaryDifficulty.ELITE),
	FALADOR_EASY(VarbitID.FALADOR_DIARY_EASY_COMPLETE, "Falador", DiaryDifficulty.EASY),
	FALADOR_MEDIUM(VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, "Falador", DiaryDifficulty.MEDIUM),
	FALADOR_HARD(VarbitID.FALADOR_DIARY_HARD_COMPLETE, "Falador", DiaryDifficulty.HARD),
	FALADOR_ELITE(VarbitID.FALADOR_DIARY_ELITE_COMPLETE, "Falador", DiaryDifficulty.ELITE),
	FREMENNIK_EASY(VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, "Fremennik", DiaryDifficulty.EASY),
	FREMENNIK_MEDIUM(VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, "Fremennik", DiaryDifficulty.MEDIUM),
	FREMENNIK_HARD(VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, "Fremennik", DiaryDifficulty.HARD),
	FREMENNIK_ELITE(VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE, "Fremennik", DiaryDifficulty.ELITE),
	KANDARIN_EASY(VarbitID.KANDARIN_DIARY_EASY_COMPLETE, "Kandarin", DiaryDifficulty.EASY),
	KANDARIN_MEDIUM(VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, "Kandarin", DiaryDifficulty.MEDIUM),
	KANDARIN_HARD(VarbitID.KANDARIN_DIARY_HARD_COMPLETE, "Kandarin", DiaryDifficulty.HARD),
	KANDARIN_ELITE(VarbitID.KANDARIN_DIARY_ELITE_COMPLETE, "Kandarin", DiaryDifficulty.ELITE),
	// Karamja below elite goes 0 not started, 1 started, 2 finished
	KARAMJA_EASY(VarbitID.ATJUN_EASY_DONE, "Karamja", DiaryDifficulty.EASY, 2),
	KARAMJA_MEDIUM(VarbitID.ATJUN_MED_DONE, "Karamja", DiaryDifficulty.MEDIUM, 2),
	KARAMJA_HARD(VarbitID.ATJUN_HARD_DONE, "Karamja", DiaryDifficulty.HARD, 2),
	KARAMJA_ELITE(VarbitID.KARAMJA_DIARY_ELITE_COMPLETE, "Karamja", DiaryDifficulty.ELITE),
	KOUREND_EASY(VarbitID.KOUREND_DIARY_EASY_COMPLETE, "Kourend & Kebos", DiaryDifficulty.EASY),
	KOUREND_MEDIUM(VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, "Kourend & Kebos", DiaryDifficulty.MEDIUM),
	KOUREND_HARD(VarbitID.KOUREND_DIARY_HARD_COMPLETE, "Kourend & Kebos", DiaryDifficulty.HARD),
	KOUREND_ELITE(VarbitID.KOUREND_DIARY_ELITE_COMPLETE, "Kourend & Kebos", DiaryDifficulty.ELITE),
	LUMBRIDGE_EASY(VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, "Lumbridge & Draynor", DiaryDifficulty.EASY),
	LUMBRIDGE_MEDIUM(VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, "Lumbridge & Draynor", DiaryDifficulty.MEDIUM),
	LUMBRIDGE_HARD(VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, "Lumbridge & Draynor", DiaryDifficulty.HARD),
	LUMBRIDGE_ELITE(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE, "Lumbridge & Draynor", DiaryDifficulty.ELITE),
	MORYTANIA_EASY(VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, "Morytania", DiaryDifficulty.EASY),
	MORYTANIA_MEDIUM(VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, "Morytania", DiaryDifficulty.MEDIUM),
	MORYTANIA_HARD(VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, "Morytania", DiaryDifficulty.HARD),
	MORYTANIA_ELITE(VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE, "Morytania", DiaryDifficulty.ELITE),
	VARROCK_EASY(VarbitID.VARROCK_DIARY_EASY_COMPLETE, "Varrock", DiaryDifficulty.EASY),
	VARROCK_MEDIUM(VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, "Varrock", DiaryDifficulty.MEDIUM),
	VARROCK_HARD(VarbitID.VARROCK_DIARY_HARD_COMPLETE, "Varrock", DiaryDifficulty.HARD),
	VARROCK_ELITE(VarbitID.VARROCK_DIARY_ELITE_COMPLETE, "Varrock", DiaryDifficulty.ELITE),
	WESTERN_EASY(VarbitID.WESTERN_DIARY_EASY_COMPLETE, "Western Provinces", DiaryDifficulty.EASY),
	WESTERN_MEDIUM(VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, "Western Provinces", DiaryDifficulty.MEDIUM),
	WESTERN_HARD(VarbitID.WESTERN_DIARY_HARD_COMPLETE, "Western Provinces", DiaryDifficulty.HARD),
	WESTERN_ELITE(VarbitID.WESTERN_DIARY_ELITE_COMPLETE, "Western Provinces", DiaryDifficulty.ELITE),
	WILDERNESS_EASY(VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, "Wilderness", DiaryDifficulty.EASY),
	WILDERNESS_MEDIUM(VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, "Wilderness", DiaryDifficulty.MEDIUM),
	WILDERNESS_HARD(VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, "Wilderness", DiaryDifficulty.HARD),
	WILDERNESS_ELITE(VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE, "Wilderness", DiaryDifficulty.ELITE);

	private static final Map<Integer, AchievementDiary> BY_VARBIT = Maps.uniqueIndex(Arrays.asList(values()), AchievementDiary::getVarbit);

	private final int varbit;
	private final String area;
	private final DiaryDifficulty difficulty;
	private final int completeValue;

	AchievementDiary(int varbit, String area, DiaryDifficulty difficulty)
	{
		this(varbit, area, difficulty, 1);
	}

	public boolean isComplete(int value)
	{
		return value >= completeValue;
	}

	public static AchievementDiary fromVarbit(int varbit)
	{
		return BY_VARBIT.get(varbit);
	}

	// The game text doesn't always match the area's name, "Western Province" for one
	public static AchievementDiary find(DiaryDifficulty difficulty, String area)
	{
		for (AchievementDiary diary : values())
		{
			if (diary.difficulty == difficulty && (diary.area.contains(area) || area.contains(diary.area)))
			{
				return diary;
			}
		}
		return null;
	}
}
