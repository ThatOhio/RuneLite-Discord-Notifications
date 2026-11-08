package com.discordnotifications.notifiers;

import com.discordnotifications.LeagueTaskTier;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class LeaguesNotifier extends BaseNotifier
{
	public void onGameMessage(String message)
	{
		Event event = parse(message);
		if (event == null || !config.includeLeagues() || !onLeagueWorld())
		{
			return;
		}

		String template = templateFor(event);
		if (template == null)
		{
			return;
		}

		send(Notification.builder()
			.type(NotificationType.LEAGUES)
			.text(template)
			.placeholders(placeholders(event))
			.screenshot(config.sendLeaguesScreenshot())
			.build());
	}

	private boolean onLeagueWorld()
	{
		return client.getWorldType().contains(WorldType.SEASONAL) && client.getVarbitValue(VarbitID.LEAGUE_TYPE) > 0;
	}

	private String templateFor(Event event)
	{
		switch (event.getKind())
		{
			case AREA:
				return config.leaguesAreas() ? config.leaguesAreaMessage() : null;
			case RELIC:
				return config.leaguesRelics() ? config.leaguesRelicMessage() : null;
			case MASTERY:
				return config.leaguesMasteries() ? config.leaguesMasteryMessage() : null;
			default:
				return config.leaguesTasks() && event.getTier().ordinal() >= config.minLeaguesTaskTier().ordinal() ? config.leaguesTaskMessage() : null;
		}
	}

	static Event parse(String message)
	{
		for (Kind kind : Kind.values())
		{
			Matcher matcher = kind.regex.matcher(message);
			if (!matcher.find())
			{
				continue;
			}

			if (kind != Kind.TASK)
			{
				return new Event(kind, matcher.group("name"), null);
			}

			LeagueTaskTier tier = LeagueTaskTier.fromName(matcher.group("tier"));
			return tier == null ? null : new Event(kind, matcher.group("name"), tier);
		}
		return null;
	}

	static Map<String, String> placeholders(Event event)
	{
		String key = event.getKind().placeholder;
		return event.getTier() == null
			? ImmutableMap.of(key, event.getName())
			: ImmutableMap.of(key, event.getName(), "tier", event.getTier().toString());
	}

	@RequiredArgsConstructor
	enum Kind
	{
		AREA("area", Pattern.compile("Congratulations, you've unlocked a new area: (?<name>.+)\\.")),
		RELIC("relic", Pattern.compile("Congratulations, you've unlocked a new Relic: (?<name>.+)\\.")),
		TASK("task", Pattern.compile("Congratulations, you've completed an? (?<tier>\\w+) task: (?<name>.+)\\.")),
		MASTERY("mastery", Pattern.compile("Congratulations, you've unlocked a new .+ Combat Mastery: (?<name>.+)\\."));

		private final String placeholder;
		private final Pattern regex;
	}

	@Value
	static class Event
	{
		Kind kind;
		String name;
		LeagueTaskTier tier;
	}
}
