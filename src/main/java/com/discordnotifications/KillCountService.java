package com.discordnotifications;

import com.discordnotifications.notifiers.BaseNotifier;
import com.discordnotifications.notifiers.ClueNotifier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.chatcommands.ChatCommandsPlugin;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class KillCountService
{
	private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion|harvest|success|opened|lap|Total Ticket)\\s?count is: ?(?<value>[\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?:completed|subdued) (?<key>.+) count is: (?<value>[\\d,]+)\\b");

	private static final String CONFIG_GROUP = "discordnotifications";
	private static final String CHAT_COMMANDS_PLUGIN = ChatCommandsPlugin.class.getSimpleName().toLowerCase(Locale.ROOT);
	private static final String LOOT_TRACKER_PLUGIN = LootTrackerPlugin.class.getSimpleName().toLowerCase(Locale.ROOT);
	private static final int SLAYER_LOG_CAP = 65535;
	private static final Duration RECENT_DROP = Duration.ofSeconds(30);
	// PetNotifier can wait 3.6 s for the pet name before it asks for the drop
	private static final Duration RECENT_PET_DROP = Duration.ofSeconds(6);

	private static final String GAUNTLET_BOSS = "Crystalline Hunllef";
	private static final String CG_BOSS = "Corrupted Hunllef";
	private static final String THE_GAUNTLET = "The Gauntlet";
	private static final String CG_NAME = "Corrupted Gauntlet";
	private static final String HERBIBOAR = "Herbiboar";
	private static final String TOA = "Tombs of Amascut";
	private static final String TOB = "Theatre of Blood";
	private static final String COX = "Chambers of Xeric";
	private static final String TFA = "Fractured Archive";
	private static final List<String> RAIDS = List.of(TOA, TOB, COX, TFA);

	// Core posts LootReceived for these instead of NpcLootReceived
	public static final Set<String> LOOT_RECEIVED_NPCS = Set.of("The Whisperer", "Araxxor", "Branda the Fire Queen", "Eldric the Ice King", GAUNTLET_BOSS, CG_BOSS);

	// Their loot never lands where NpcLootReceived looks for it
	private static final Set<Integer> SERVER_LOOT_NPC_IDS = ImmutableSet.of(
		NpcID.YAMA,
		NpcID.HESPORI,
		NpcID.MAGGOT_KING,
		NpcID.MAGGOT_KING_CORPSE,
		NpcID.SAILING_BULL_SHARK_DEAD,
		NpcID.SAILING_HAMMERHEAD_SHARK_DEAD,
		NpcID.SAILING_TIGER_SHARK_DEAD,
		NpcID.SAILING_GREAT_WHITE_SHARK_DEAD,
		NpcID.SAILING_NARWHAL_DEAD,
		NpcID.SAILING_ORCA_DEAD,
		NpcID.SAILING_PYGMY_KRAKEN_DEAD,
		NpcID.SAILING_SPINED_KRAKEN_DEAD,
		NpcID.SAILING_ARMOURED_KRAKEN_DEAD,
		NpcID.SAILING_VAMPYRE_KRAKEN_DEAD,
		NpcID.SAILING_EAGLE_RAY_DEAD,
		NpcID.SAILING_BUTTERFLY_RAY_DEAD,
		NpcID.SAILING_STINGRAY_DEAD,
		NpcID.SAILING_MANTA_RAY_DEAD,
		NpcID.SAILING_OSPREY_DEAD,
		NpcID.SAILING_ALBATROSS_DEAD,
		NpcID.SAILING_FRIGATEBIRD_DEAD,
		NpcID.SAILING_TERN_DEAD,
		NpcID.SAILING_SEA_MOGRE_DEAD,
		NpcID.SAILING_DOLPHIN_DEAD,
		NpcID.SAILING_VEILED_KRAKEN_DEAD,
		16305, 16306, 16307, 16308, 16309, 16310, 16311, 16312, 16313, 16314, 16315); // Mad Angel

	// Keys are cleanBossName output
	static final Map<String, Integer> BOSS_VARPS = ImmutableMap.<String, Integer>builder()
		.put("abyssal sire", VarPlayerID.TOTAL_ABYSSALSIRE_KILLS)
		.put("alchemical hydra", VarPlayerID.TOTAL_HYDRABOSS_KILLS)
		.put("amoxliatl", VarPlayerID.TOTAL_AMOXLIATL_KILLS)
		.put("araxxor", VarPlayerID.TOTAL_ARAXXOR_KILLS)
		.put("artio", VarPlayerID.TOTAL_ARTIO_KILLS)
		.put("barrows chests", VarPlayerID.TOTAL_BARROWS_CHESTS)
		.put("brutus", VarPlayerID.TOTAL_COWBOSS_KILLS)
		.put("bryophyta", VarPlayerID.TOTAL_BRYOPHYTA_KILLS)
		.put("callisto", VarPlayerID.TOTAL_CALLISTO_KILLS)
		.put("calvar'ion", VarPlayerID.TOTAL_CALVARION_KILLS)
		.put("cerberus", VarPlayerID.TOTAL_CERBERUS_KILLS)
		.put("chambers of xeric", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS)
		.put("chambers of xeric challenge mode", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS_CHALLENGE)
		.put("chaos elemental", VarPlayerID.TOTAL_CHAOSELE_KILLS)
		.put("chaos fanatic", VarPlayerID.TOTAL_CHAOSFANATIC_KILLS)
		.put("commander zilyana", VarPlayerID.TOTAL_SARADOMIN_KILLS)
		.put("corporeal beast", VarPlayerID.TOTAL_CORP_KILLS)
		.put("corrupted gauntlet", VarPlayerID.TOTAL_COMPLETED_GAUNTLET_HM)
		.put("crazy archaeologist", VarPlayerID.TOTAL_CRAZYARCHAEOLOGIST_KILLS)
		.put("dagannoth prime", VarPlayerID.TOTAL_PRIME_KILLS)
		.put("dagannoth rex", VarPlayerID.TOTAL_REX_KILLS)
		.put("dagannoth supreme", VarPlayerID.TOTAL_SUPREME_KILLS)
		.put("demonic brutus", VarPlayerID.TOTAL_COWBOSS_HARDMODE_KILLS)
		.put("deranged archaeologist", VarPlayerID.TOTAL_DERANGEDARCHAEOLOGIST_KILLS)
		.put("duke sucellus", VarPlayerID.TOTAL_DUKE_SUCELLUS_KILLS)
		.put("fractured archive", VarPlayerID.TOTAL_COMPLETED_THEFRACTUREDARCHIVE)
		.put("gauntlet", VarPlayerID.TOTAL_COMPLETED_GAUNTLET)
		.put("gemstone crab", VarPlayerID.TOTAL_GEMSTONE_CRAB_KILLS)
		.put("general graardor", VarPlayerID.TOTAL_BANDOS_KILLS)
		.put("giant mole", VarPlayerID.TOTAL_MOLE_KILLS)
		.put("grotesque guardians", VarPlayerID.TOTAL_GARGBOSS_KILLS)
		.put("guardians of the rift", VarPlayerID.TOTAL_GOTR_KILLS)
		.put("hespori", VarPlayerID.TOTAL_HESPORI_KILLS)
		.put("hueycoatl", VarPlayerID.TOTAL_HUEY_KILLS)
		.put("k'ril tsutsaroth", VarPlayerID.TOTAL_ZAMORAK_KILLS)
		.put("kalphite queen", VarPlayerID.TOTAL_KALPHITE_KILLS)
		.put("king black dragon", VarPlayerID.TOTAL_KBD_KILLS)
		.put("kraken", VarPlayerID.TOTAL_KRAKEN_BOSS_KILLS)
		.put("kree'arra", VarPlayerID.TOTAL_ARMADYL_KILLS)
		.put("leviathan", VarPlayerID.TOTAL_LEVIATHAN_KILLS)
		.put("lunar chest", VarPlayerID.TOTAL_PMOON_CHESTS)
		.put("mad angel", VarPlayerID.TOTAL_MAD_ANGEL_KILLS)
		.put("maggot king", VarPlayerID.TOTAL_MAGGOT_KING_KILLS)
		.put("mimic", VarPlayerID.TOTAL_MIMIC_KILLS)
		.put("nex", VarPlayerID.TOTAL_NEX_KILLS)
		.put("nightmare", VarPlayerID.TOTAL_NIGHTMARE_KILLS)
		.put("obor", VarPlayerID.TOTAL_HILLGIANT_BOSS_KILLS)
		.put("phantom muspah", VarPlayerID.TOTAL_MUSPAH_KILLS)
		.put("phosani's nightmare", VarPlayerID.TOTAL_NIGHTMARE_CHALLENGE_KILLS)
		.put("royal titans", VarPlayerID.TOTAL_ROYAL_TITAN_KILLS)
		.put("sarachnis", VarPlayerID.TOTAL_SARACHNIS_KILLS)
		.put("scorpia", VarPlayerID.TOTAL_SCORPIA_KILLS)
		.put("scurrius", VarPlayerID.TOTAL_RAT_BOSS_KILLS)
		.put("shellbane gryphon", VarPlayerID.TOTAL_GRYPHON_BOSS_KILLS)
		.put("skotizo", VarPlayerID.TOTAL_CATA_BOSS_KILLS)
		.put("sol heredit", VarPlayerID.TOTAL_SOL_KILLS)
		.put("spindel", VarPlayerID.TOTAL_SPINDEL_KILLS)
		.put("tempoross", VarPlayerID.TOTAL_TEMPOROSS_KILLS)
		.put("theatre of blood", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD)
		.put("theatre of blood entry mode", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_STORY)
		.put("theatre of blood hard mode", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_HARD)
		.put("thermonuclear smoke devil", VarPlayerID.TOTAL_THERMY_KILLS)
		.put("tombs of amascut", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT)
		.put("tombs of amascut entry mode", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_ENTRY)
		.put("tombs of amascut expert mode", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_EXPERT)
		.put("tzkal-zuk", VarPlayerID.TOTAL_ZUK_KILLS)
		.put("tztok-jad", VarPlayerID.TOTAL_JAD_KILLS)
		.put("vardorvis", VarPlayerID.TOTAL_VARDORVIS_KILLS)
		.put("venenatis", VarPlayerID.TOTAL_VENENATIS_KILLS)
		.put("vet'ion", VarPlayerID.TOTAL_VETION_KILLS)
		.put("vorkath", VarPlayerID.TOTAL_VORKATH_KILLS)
		.put("whisperer", VarPlayerID.TOTAL_WHISPERER_KILLS)
		.put("wintertodt", VarPlayerID.TOTAL_WINTERTODT_KILLS)
		.put("yama", VarPlayerID.TOTAL_YAMA_KILLS)
		.put("zalcano", VarPlayerID.TOTAL_ZALCANO_KILLS)
		.put("zulrah", VarPlayerID.TOTAL_SNAKEBOSS_KILLS)
		.build();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	private final Map<String, Integer> counts = new HashMap<>();

	@Getter
	private Drop lastDrop;

	public void reset()
	{
		counts.clear();
		lastDrop = null;
	}

	public void onNpcLootReceived(NpcLootReceived event)
	{
		String name = event.getNpc().getName();
		if (usesNpcLootEvent(event.getNpc().getId(), name))
		{
			recordLoot(LootRecordType.NPC, name, event.getItems());
		}
	}

	public void onServerNpcLoot(ServerNpcLoot event)
	{
		NPCComposition npc = event.getComposition();
		if (npc != null && usesServerLoot(npc.getId(), npc.getName()))
		{
			recordLoot(LootRecordType.NPC, npc.getName(), event.getItems());
		}
	}

	public void onPlayerLootReceived(PlayerLootReceived event)
	{
		String name = event.getPlayer().getName();
		if (name != null)
		{
			recordLoot(LootRecordType.PLAYER, name, event.getItems());
		}
	}

	public void onLootReceived(LootReceived event)
	{
		LootRecordType type = event.getType();
		// Core's empty Maggot King posts, popping an egg after the kill isn't another kill
		if (type == LootRecordType.UNKNOWN || event.getName() == null)
		{
			return;
		}

		// The loot tracker reposts every NPC and PvP kill, those are already counted above
		if (type == LootRecordType.PLAYER || (type == LootRecordType.NPC && !LOOT_RECEIVED_NPCS.contains(event.getName())))
		{
			return;
		}

		String source = getStandardizedSource(event);
		recordLoot(standardizedType(source, type), source, event.getItems());
	}

	public void onGameMessage(String message)
	{
		ClueNotifier.CompletedClue clue = ClueNotifier.parse(message);
		if (clue != null)
		{
			recordChatCount("Clue Scroll (" + clue.getTier() + ")", clue.getCount());
			return;
		}

		BossKc parsed = parseBoss(message);
		if (parsed == null)
		{
			return;
		}

		String boss = parsed.getBoss();
		recordChatCount(boss, parsed.getCount());
		if (boss.equals("Araxxor") || boss.equals("Maggot King") || boss.endsWith("Gauntlet") || RAIDS.stream().anyMatch(boss::startsWith))
		{
			// These give their loot late or not through the NPC event, so pets and clog need the name now
			lastDrop = new Drop(boss, LootRecordType.EVENT, Collections.emptyList(), Instant.now());
		}
	}

	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.KILL_LOG)
		{
			clientThread.invokeAtTickEnd(this::readSlayerLog);
		}
	}

	public String getStandardizedSource(LootReceived event)
	{
		String name = event.getName();
		if (GAUNTLET_BOSS.equals(name))
		{
			return THE_GAUNTLET;
		}
		if (CG_BOSS.equals(name))
		{
			return CG_NAME;
		}

		// Raid loot is just "Chambers of Xeric", the chat line has the mode
		boolean sameRaid = lastDrop != null && event.getType() == LootRecordType.EVENT
			&& RAIDS.stream().anyMatch(raid -> raid.equals(name) && lastDrop.getSource().startsWith(raid));
		return sameRaid ? lastDrop.getSource() : name;
	}

	public static boolean usesServerLoot(int npcId, String name)
	{
		return SERVER_LOOT_NPC_IDS.contains(npcId) || (name != null && name.startsWith("Hallowed Sepulchre"));
	}

	public static boolean usesNpcLootEvent(int npcId, String name)
	{
		return name != null && !LOOT_RECEIVED_NPCS.contains(name) && !usesServerLoot(npcId, name);
	}

	public static LootRecordType standardizedType(String source, LootRecordType type)
	{
		return THE_GAUNTLET.equals(source) || CG_NAME.equals(source) ? LootRecordType.EVENT : type;
	}

	public boolean lootTrackerDisabled()
	{
		return pluginDisabled(LOOT_TRACKER_PLUGIN);
	}

	public Integer getKillCount(LootRecordType type, String source)
	{
		if (source == null)
		{
			return null;
		}

		int kc = Math.max(storedKc(type, source, 0), counts.getOrDefault(cacheKey(type, source), 0));
		return kc > 0 ? kc : null;
	}

	public Drop getRecentDrop()
	{
		return isRecent(lastDrop, Instant.now()) || heldForCoxChest() ? lastDrop : null;
	}

	static boolean isRecent(Drop drop, Instant now)
	{
		return drop != null && Duration.between(drop.getTime(), now).compareTo(RECENT_DROP) <= 0;
	}

	public Drop getRecentPetDrop()
	{
		return isRecentForPet(lastDrop, Instant.now()) || heldForCoxChest() ? lastDrop : null;
	}

	// EVENT keeps the long window, raid chests pay out well after the drop is recorded
	static boolean isRecentForPet(Drop drop, Instant now)
	{
		return isRecent(drop, now)
			&& (drop.getType() == LootRecordType.EVENT || Duration.between(drop.getTime(), now).compareTo(RECENT_PET_DROP) <= 0);
	}

	private boolean heldForCoxChest()
	{
		return isAwaitingCoxChest(lastDrop) && client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
	}

	// The chest can sit unopened for minutes after the KC line, and only that line records no items
	static boolean isAwaitingCoxChest(Drop drop)
	{
		return drop != null && drop.getType() == LootRecordType.EVENT && drop.getItems().isEmpty() && drop.getSource().startsWith(COX);
	}

	public Map<String, String> sourcePlaceholders(Drop drop)
	{
		return sourcePlaceholders(BaseNotifier.sanitize(drop.getSource()), getKillCount(drop.getType(), drop.getSource()));
	}

	static Map<String, String> sourcePlaceholders(String source, Integer kc)
	{
		return ImmutableMap.of("source", source, "from", " from " + source, "kc", kcText(kc));
	}

	public static String kcText(Integer kc)
	{
		return kc == null ? "" : " (KC " + QuantityFormatter.formatNumber(kc) + ")";
	}

	private void recordLoot(LootRecordType type, String source, Collection<ItemStack> items)
	{
		lastDrop = new Drop(source, type, items, Instant.now());

		// Don't start counting from nothing, a made up "KC 1" is worse than no KC
		Integer kc = counts.compute(cacheKey(type, source), (key, cached) ->
		{
			if (cached != null)
			{
				return cached + 1;
			}
			int stored = storedKc(type, source, 1);
			return stored > 0 ? stored + 1 : null;
		});

		if (kc != null && type == LootRecordType.NPC && savedKc(source) > 0)
		{
			saveKc(source, kc);
		}
	}

	// Chat can land either side of the loot event: cached is kc - 1 until the bump, saved is exact
	private void recordChatCount(String source, int kc)
	{
		counts.merge(cacheKey(LootRecordType.UNKNOWN, source), kc - 1, Math::max);
		saveKc(source, kc);
	}

	// lootOffset is 1 while handling a loot event, the game's own counts may already include that kill
	private int storedKc(LootRecordType type, String source, int lootOffset)
	{
		int kc = lootTrackerKills(type, source);
		if (type == LootRecordType.PLAYER || type == LootRecordType.PICKPOCKET)
		{
			return kc;
		}

		String boss = cleanBossName(source);
		Integer varp = BOSS_VARPS.get(boss);
		kc = Math.max(kc, savedKc(source));
		kc = Math.max(kc, chatCommandsKc(boss) - lootOffset);
		return varp == null ? kc : Math.max(kc, client.getVarpValue(varp) - lootOffset);
	}

	private int chatCommandsKc(String boss)
	{
		if (pluginDisabled(CHAT_COMMANDS_PLUGIN))
		{
			return 0;
		}
		Integer kc = configManager.getRSProfileConfiguration("killcount", boss, int.class);
		return kc == null ? 0 : kc;
	}

	public Double getPb(String boss)
	{
		if (pluginDisabled(CHAT_COMMANDS_PLUGIN))
		{
			return null;
		}
		return configManager.getRSProfileConfiguration("personalbest", cleanBossName(boss), double.class);
	}

	// The tracker skips kills with no loot and only knows kills made with it on, so this is a lower bound
	private int lootTrackerKills(LootRecordType type, String source)
	{
		if (pluginDisabled(LOOT_TRACKER_PLUGIN))
		{
			return 0;
		}

		String json = configManager.getConfiguration(LootTrackerConfig.GROUP, configManager.getRSProfileKey(), "drops_" + type + "_" + source);
		if (json == null)
		{
			return 0;
		}

		try
		{
			LootTrackerRecord record = gson.fromJson(json, LootTrackerRecord.class);
			return record == null ? 0 : record.kills;
		}
		catch (JsonSyntaxException e)
		{
			log.debug("Couldn't read loot tracker record for {}", source, e);
			return 0;
		}
	}

	private int savedKc(String source)
	{
		Integer kc = configManager.getRSProfileConfiguration(CONFIG_GROUP, savedKey(source), int.class);
		return kc == null ? 0 : kc;
	}

	private void saveKc(String source, int kc)
	{
		if (kc > savedKc(source))
		{
			configManager.setRSProfileConfiguration(CONFIG_GROUP, savedKey(source), kc);
		}
	}

	private boolean pluginDisabled(String simpleLowerClassName)
	{
		return "false".equals(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, simpleLowerClassName));
	}

	private void readSlayerLog()
	{
		Widget title = client.getWidget(InterfaceID.KillLog.INTERFACE_TITLE);
		Widget names = client.getWidget(InterfaceID.KillLog.NAME);
		Widget kills = client.getWidget(InterfaceID.KillLog.KILL);
		if (title == null || names == null || kills == null || !"Slayer Kill Log".equals(title.getText()))
		{
			return;
		}

		Widget[] mobs = names.getChildren();
		Widget[] values = kills.getChildren();
		if (mobs == null || values == null)
		{
			return;
		}

		for (int i = 0; i < Math.min(mobs.length, values.length); i++)
		{
			saveKc(mobs[i].getText().replace(":", ""), parseSlayerCount(values[i].getText()));
		}
	}

	static int parseSlayerCount(String text)
	{
		String count = text.replace(",", "").trim();
		if (count.startsWith("Lots"))
		{
			return SLAYER_LOG_CAP;
		}

		try
		{
			return Integer.parseInt(count);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	public static BossKc parseBoss(String message)
	{
		Matcher primary = PRIMARY_REGEX.matcher(message);
		if (primary.find())
		{
			return result(primaryBoss(primary.group("key"), primary.group("type")), primary.group("value"));
		}

		Matcher secondary = SECONDARY_REGEX.matcher(message);
		return secondary.find() ? result(secondaryBoss(secondary.group("key")), secondary.group("value")) : null;
	}

	private static BossKc result(String boss, String count)
	{
		try
		{
			return boss == null ? null : new BossKc(boss, Integer.parseInt(count.replace(",", "")));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private static String primaryBoss(String boss, String type)
	{
		switch (type.toLowerCase(Locale.ROOT))
		{
			case "chest":
				if (boss.equalsIgnoreCase("Barrows"))
				{
					return boss;
				}
				return boss.equalsIgnoreCase("Lunar") ? boss + " " + type : null;
			case "completion":
				return boss.equalsIgnoreCase("Gauntlet") || boss.equalsIgnoreCase(CG_NAME) ? boss : null;
			case "harvest":
				return boss.equalsIgnoreCase(HERBIBOAR) ? HERBIBOAR : null;
			case "lap":
			case "total ticket":
				return null;
			default:
				return boss;
		}
	}

	private static String secondaryBoss(String boss)
	{
		return boss.equalsIgnoreCase("Wintertodt") || RAIDS.stream().anyMatch(boss::startsWith) ? boss : null;
	}

	// Lowercase name the Chat Commands plugin saves under
	static String cleanBossName(String source)
	{
		if (THE_GAUNTLET.equalsIgnoreCase(source) || GAUNTLET_BOSS.equals(source))
		{
			return "gauntlet";
		}
		if (CG_BOSS.equals(source))
		{
			return "corrupted gauntlet";
		}
		if (source.equals("Branda the Fire Queen") || source.equals("Eldric the Ice King"))
		{
			return "royal titans";
		}
		if (source.startsWith("Barrows"))
		{
			return "barrows chests";
		}
		if (source.endsWith("Hallowed Sepulchre)"))
		{
			return "hallowed sepulchre";
		}
		if (source.endsWith("Tempoross)"))
		{
			return "tempoross";
		}
		if (source.endsWith("Wintertodt)"))
		{
			return "wintertodt";
		}

		String name = source.toLowerCase(Locale.ROOT).replace(":", "");
		for (String prefixed : List.of("the leviathan", "the whisperer", "the hueycoatl", "the nightmare"))
		{
			if (name.equals(prefixed))
			{
				return name.substring("the ".length());
			}
		}
		return name;
	}

	private static String cacheKey(LootRecordType type, String source)
	{
		if (type == LootRecordType.PLAYER || type == LootRecordType.PICKPOCKET)
		{
			return type + "_" + source;
		}
		return cleanBossName(source);
	}

	private static String savedKey(String source)
	{
		return "kc_" + cleanBossName(source);
	}

	@Value
	public static class BossKc
	{
		String boss;
		int count;
	}

	@Value
	public static class Drop
	{
		String source;
		LootRecordType type;
		Collection<ItemStack> items;
		Instant time;
	}

	// Same shape as the loot tracker's own ConfigLoot, which is package-private
	private static class LootTrackerRecord
	{
		int kills;
	}
}
