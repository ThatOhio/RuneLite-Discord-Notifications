package com.discordnotifications.notifiers;

import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class QuestNotifier extends BaseNotifier
{
	private static final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: quest)?[!.]?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: quest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$", Pattern.CASE_INSENSITIVE);
	private static final ImmutableList<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
	private static final ImmutableList<String> WORD_QUEST_IN_NAME_TAGS =
		ImmutableList.of(
			"Another Cook",
			"Doric",
			"Heroes",
			"Legends",
			"Observatory",
			"Olaf",
			"Waterfall"
		);

	private static final ImmutableMap<String, String> QUEST_REPLACEMENTS =
		ImmutableMap.of(
			"Lumbridge Cook... again", "Another Cook's",
			"Skrach 'Bone Crusher' Uglogwee", "Skrach Uglogwee"
		);

	@Inject
	private ClientThread clientThread;

	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.QUESTSCROLL || !config.includeQuestComplete())
		{
			return;
		}

		Widget title = client.getWidget(InterfaceID.Questscroll.QUEST_TITLE);
		if (title != null)
		{
			String text = sanitize(title.getText());
			// The quest varbits haven't updated yet when the scroll opens
			clientThread.invokeLater(() -> sendQuestMessage(text));
		}
	}

	private void sendQuestMessage(String text)
	{
		String questName = parseQuestCompletedWidget(text);
		if (questName == null)
		{
			return;
		}

		send(NotificationType.QUEST, config.questMessage(),
			placeholders(
				questName,
				client.getVarbitValue(VarbitID.QUESTS_COMPLETED_COUNT),
				client.getVarbitValue(VarbitID.QUESTS_TOTAL_COUNT),
				client.getVarpValue(VarPlayerID.QP),
				client.getVarbitValue(VarbitID.QP_MAX)),
			config.sendQuestingScreenshot());
	}

	static Map<String, String> placeholders(String quest, int completed, int totalQuests, int qp, int qpMax)
	{
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("quest", quest);

		if (completed > 0 && totalQuests > 0)
		{
			placeholders.put("completed", Integer.toString(completed));
			placeholders.put("totalQuests", Integer.toString(totalQuests));
		}

		if (qp > 0 && qpMax > 0)
		{
			placeholders.put("qp", Integer.toString(qp));
			placeholders.put("qpMax", Integer.toString(qpMax));
			placeholders.put("progress", " (" + qp + "/" + qpMax + " QP)");
		}

		return placeholders;
	}

	static String parseQuestCompletedWidget(final String text)
	{
		// "You have completed The Corsair Curse!"
		final Matcher questMatch1 = QUEST_PATTERN_1.matcher(text);
		// "'One Small Favour' completed!"
		final Matcher questMatch2 = QUEST_PATTERN_2.matcher(text);
		final Matcher questMatchFinal = questMatch1.matches() ? questMatch1 : questMatch2;

		if (!questMatchFinal.matches())
		{
			return text.isEmpty() ? null : text;
		}

		String quest = questMatchFinal.group("quest");
		quest = QUEST_REPLACEMENTS.getOrDefault(quest, quest);
		String verb = questMatchFinal.group("verb") != null ? questMatchFinal.group("verb") : "";

		if (verb.contains("kind of"))
		{
			return null;
		}
		else if (verb.contains("completely"))
		{
			quest += " II";
		}

		if (RFD_TAGS.stream().anyMatch((quest + verb)::contains))
		{
			quest = "Recipe for Disaster - " + quest;
		}

		if (WORD_QUEST_IN_NAME_TAGS.stream().anyMatch(quest::contains))
		{
			quest += " Quest";
		}

		return quest;
	}
}
