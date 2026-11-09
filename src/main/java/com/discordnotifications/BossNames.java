package com.discordnotifications;

import com.google.common.collect.ImmutableMap;
import lombok.Value;
import net.runelite.api.gameval.VarPlayerID;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BossNames
{
	private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion|harvest|success|opened|lap|Total Ticket)\\s?count is: ?(?<value>[\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?:completed|subdued) (?<key>.+) count is: (?<value>[\\d,]+)\\b");

	static final String GAUNTLET_BOSS = "Crystalline Hunllef";
	static final String CG_BOSS = "Corrupted Hunllef";
	static final String THE_GAUNTLET = "The Gauntlet";
	static final String CG_NAME = "Corrupted Gauntlet";
	private static final String HERBIBOAR = "Herbiboar";
	private static final String TOA = "Tombs of Amascut";
	private static final String TOB = "Theatre of Blood";
	static final String COX = "Chambers of Xeric";
	private static final String TFA = "Fractured Archive";
	static final List<String> RAIDS = List.of(TOA, TOB, COX, TFA);

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

	private BossNames()
	{
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

	@Value
	public static class BossKc
	{
		String boss;
		int count;
	}
}
