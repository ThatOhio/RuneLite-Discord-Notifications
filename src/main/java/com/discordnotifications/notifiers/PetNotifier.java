package com.discordnotifications.notifiers;

import com.discordnotifications.KillCountService;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.ScriptPreFired;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class PetNotifier extends BaseNotifier
{
	static final Pattern PET_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");
	static final Pattern CLAN_REGEX = Pattern.compile("\\b(?<user>[\\w\\s-]+) (?:has a funny feeling like .+ followed|feels something weird sneaking into .+ backpack|feels like .+ acquired something special): (?<pet>.+?)(?: at (?<milestone>.+?)\\.?)?$");

	private static final List<Pattern> NAME_SOURCES = ImmutableList.of(
		Pattern.compile("Untradeable drop: (.+)"),
		Pattern.compile("New item added to your collection log: (.+)"));

	private static final String NO_NAME_MESSAGE = "$name just got a pet$from$kc!$duplicate";
	private static final int MAX_WAIT_TICKS = 5;

	// Other untradeables can drop during the wait, "Pet ..." names are matched by prefix
	private static final Set<String> PET_NAMES = ImmutableSet.of(
		"abyssal orphan", "abyssal protector", "baby chinchompa", "baby mole", "baron", "beef",
		"bran", "butch", "beaver", "bloodhound", "callisto cub", "chompy chick", "dom",
		"giant squirrel", "hellpuppy", "herbi", "heron", "huberte", "ikkle hydra", "jal-nib-rek",
		"kalphite princess", "lil' creator", "lil' zik", "lil'viathan", "little nightmare",
		"maggot marquess", "moxi", "muphin", "nexling", "nid", "noon", "olmlet", "phoenix",
		"prince black dragon", "quetzin", "rift guardian", "rock golem", "rocky",
		"scorpia's offspring", "scurry", "skotos", "smolcano", "smol heredit", "soup", "sraracha",
		"tangleroot", "tiny tempor", "tumeken's guardian", "tzrek-jad", "venenatis spiderling",
		"vet'ion jr.", "vorki", "wisp", "yami", "youngllef");

	@Inject
	private KillCountService killCountService;

	private boolean waiting;
	private boolean duplicate;
	private String pet;
	private String milestone;
	private int ticksWaited;

	@Override
	public void reset()
	{
		waiting = false;
		duplicate = false;
		pet = null;
		milestone = null;
		ticksWaited = 0;
	}

	public void onGameMessage(String chatMessage)
	{
		if (waiting)
		{
			if (pet == null)
			{
				pet = parsePetName(chatMessage);
			}
			return;
		}

		if (!config.includePets() || !PET_REGEX.matcher(chatMessage).matches())
		{
			return;
		}

		boolean wouldHaveBeen = chatMessage.contains("would have been");
		if (!wouldHaveBeen || config.notifyDuplicatePets())
		{
			waiting = true;
			duplicate = wouldHaveBeen;
		}
	}

	public void onClanMessage(String message)
	{
		if (!waiting)
		{
			return;
		}

		Matcher matcher = CLAN_REGEX.matcher(message);
		if (matcher.find() && matcher.group("user").trim().equals(playerName()))
		{
			pet = matcher.group("pet");
			milestone = matcher.group("milestone");
		}
	}

	public void onScriptPreFired(ScriptPreFired event)
	{
		if (!waiting || pet != null || event.getScriptId() != ScriptID.NOTIFICATION_DELAY)
		{
			return;
		}

		String topText = sanitize(client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT));
		String entry = popupSubject(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT));
		if (topText.equalsIgnoreCase("Collection log") && isPet(entry))
		{
			pet = entry;
		}
	}

	public void onGameTick()
	{
		if (!waiting)
		{
			return;
		}

		if ((pet != null && milestone != null) || ++ticksWaited > MAX_WAIT_TICKS)
		{
			if (config.includePets())
			{
				sendPet();
			}
			reset();
		}
	}

	private void sendPet()
	{
		Map<String, String> placeholders = new HashMap<>();
		if (pet != null)
		{
			placeholders.put("pet", pet);
		}
		KillCountService.Drop drop = killCountService.getRecentDrop();
		if (drop != null)
		{
			placeholders.putAll(killCountService.sourcePlaceholders(drop));
		}
		if (milestone != null && placeholders.getOrDefault("kc", "").isEmpty())
		{
			placeholders.put("milestone", " at " + milestone);
		}
		if (duplicate)
		{
			placeholders.put("duplicate", " (would have been followed)");
		}

		String template = config.petMessage();
		boolean noName = pet == null && template.contains("$pet");
		send(Notification.builder()
			.type(NotificationType.PET)
			.text(noName ? NO_NAME_MESSAGE : template)
			// A duplicate never fills a collection log slot, so any clog line beside it is another item
			.namedText(pet == null && !duplicate ? template : null)
			.placeholders(placeholders)
			.screenshot(config.sendPetScreenshot())
			.build());
	}

	static String parsePetName(String message)
	{
		for (Pattern source : NAME_SOURCES)
		{
			Matcher matcher = source.matcher(message);
			if (matcher.find() && isPet(matcher.group(1)))
			{
				return matcher.group(1);
			}
		}
		return null;
	}

	static boolean isPet(String itemName)
	{
		String name = itemName.toLowerCase();
		return name.startsWith("pet ") || PET_NAMES.contains(name);
	}
}
