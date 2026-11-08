package com.discordnotifications.notifiers;

import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;

import javax.inject.Singleton;
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

	private boolean shouldSendQuestMessage = false;

	@Override
	public void reset()
	{
		shouldSendQuestMessage = false;
	}

	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.QUESTSCROLL) {
			shouldSendQuestMessage = true;
		}
	}

	public void onGameTick()
	{
		if (shouldSendQuestMessage
				&& config.includeQuestComplete()
				&& client.getWidget(InterfaceID.Questscroll.QUEST_TITLE) != null
		) {
			shouldSendQuestMessage = false;
			String text = sanitize(client.getWidget(InterfaceID.Questscroll.QUEST_TITLE).getText());
			String questName = parseQuestCompletedWidget(text);
			sendQuestMessage(questName);
		}
	}

	private void sendQuestMessage(String questName)
	{
		send(Notification.builder()
			.type(NotificationType.QUEST)
			.text(config.questMessage())
			.placeholders(ImmutableMap.of("quest", questName))
			.screenshot(config.sendQuestingScreenshot())
			.build());
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
			return "Unable to find quest name!";
		}

		String quest = questMatchFinal.group("quest");
		String verb = questMatchFinal.group("verb") != null ? questMatchFinal.group("verb") : "";

		if (verb.contains("kind of"))
		{
			quest += " partial completion";
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
