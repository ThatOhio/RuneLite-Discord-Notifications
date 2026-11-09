package com.discordnotifications.notifiers;

import com.discordnotifications.LeagueTaskTier;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.StructComposition;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class LeaguesNotifier extends BaseNotifier
{
	private static final int LEAGUES_ENUM = 2670;
	private static final int TROPHY_THRESHOLDS_PARAM = 1857;
	private static final int RELIC_TIERS_PARAM = 870;
	private static final int TIER_RELICS_PARAM = 878;
	private static final int RELIC_NAME_PARAM = 879;
	private static final String[] TROPHIES = {"Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune", "Dragon"};

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

		send(NotificationType.LEAGUES, template, withGameDetails(event), config.sendLeaguesScreenshot());
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

	private Map<String, String> withGameDetails(Event event)
	{
		Map<String, String> values = new HashMap<>(placeholders(event));
		// The struct ids can move between leagues, that shouldn't cost the player their post
		try
		{
			if (event.getKind() == Kind.RELIC)
			{
				int tier = relicTier(event.getName());
				if (tier > 0)
				{
					values.put("relicTier", " (Tier " + tier + ")");
				}
			}
			else if (event.getKind() == Kind.TASK)
			{
				int[] thresholds = client.getEnum(leagueStruct().getIntValue(TROPHY_THRESHOLDS_PARAM)).getIntVals();
				String trophy = earnedTrophy(thresholds, client.getVarpValue(VarPlayerID.LEAGUE_POINTS_COMPLETED), event.getTier().getPoints());
				if (trophy != null)
				{
					values.put("trophy", ", unlocking the " + trophy + " trophy");
				}
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Could not read league details for {}", event, e);
		}
		return values;
	}

	private StructComposition leagueStruct()
	{
		int[] leagues = client.getEnum(LEAGUES_ENUM).getIntVals();
		return client.getStructComposition(leagues[client.getVarbitValue(VarbitID.LEAGUE_TYPE) - 1]);
	}

	private int relicTier(String relic)
	{
		int[] tiers = client.getEnum(leagueStruct().getIntValue(RELIC_TIERS_PARAM)).getIntVals();
		for (int i = 0; i < tiers.length; i++)
		{
			int relics = client.getStructComposition(tiers[i]).getIntValue(TIER_RELICS_PARAM);
			for (int id : client.getEnum(relics).getIntVals())
			{
				if (relic.equals(client.getStructComposition(id).getStringValue(RELIC_NAME_PARAM)))
				{
					return i + 1;
				}
			}
		}
		return 0;
	}

	static String earnedTrophy(int[] thresholds, int points, int taskPoints)
	{
		String earned = null;
		for (int i = 0; i < Math.min(thresholds.length, TROPHIES.length); i++)
		{
			if (points >= thresholds[i] && points - taskPoints < thresholds[i])
			{
				earned = TROPHIES[i];
			}
		}
		return earned;
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
