package com.discordnotifications;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class WorldUtils
{
	private static final Set<WorldType> IGNORED = EnumSet.of(
		WorldType.PVP_ARENA,
		WorldType.QUEST_SPEEDRUNNING,
		WorldType.BETA_WORLD,
		WorldType.NOSAVE_MODE,
		WorldType.TOURNAMENT_WORLD);

	// Seasonal Deadman and Grid Master worlds carry the tournament flag too
	private static final Set<WorldType> IGNORED_ON_SEASONAL = Sets.difference(IGNORED, EnumSet.of(WorldType.TOURNAMENT_WORLD));

	private static final Set<WorldType> SEPARATE_STATS = Sets.union(IGNORED, EnumSet.of(WorldType.DEADMAN, WorldType.SEASONAL));

	private static final Set<Integer> BA_REGIONS = ImmutableSet.of(7508, 7509, 10322);
	private static final Set<Integer> CASTLE_WARS_REGIONS = ImmutableSet.of(9520, 9620);
	private static final Set<Integer> CLAN_WARS_REGIONS = ImmutableSet.of(12621, 12622, 12623, 13130, 13131, 13133, 13134, 13135, 13386, 13387, 13390, 13641, 13642, 13643, 13644, 13645, 13646, 13647, 13899, 13900, 14155, 14156);
	private static final Set<Integer> COX_REGIONS = ImmutableSet.of(12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395, 13396, 13397, 13401);
	private static final Set<Integer> GALVEK_REGIONS = ImmutableSet.of(6486, 6487, 6488, 6489, 6742, 6743, 6744, 6745);
	private static final Set<Integer> GAUNTLET_REGIONS = ImmutableSet.of(7512, 7768, 12127);
	private static final Set<Integer> LMS_REGIONS = ImmutableSet.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);
	private static final Set<Integer> POH_REGIONS = ImmutableSet.of(7257, 7534, 7535, 7790, 7791, 8046, 8047, 8302, 8303);
	private static final Set<Integer> SOUL_WARS_REGIONS = ImmutableSet.of(8493, 8748, 8749, 9005);
	private static final Set<Integer> TOA_REGIONS = ImmutableSet.of(14160, 14162, 14164, 14674, 14676, 15184, 15186, 15188, 15696, 15698, 15700);
	private static final Set<Integer> TOB_REGIONS = ImmutableSet.of(12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125, 13379);
	private static final Set<Integer> ZULRAH_REGIONS = ImmutableSet.of(9007, 9008);
	private static final int INFERNO_REGION = 9043;
	private static final int MAGE_TRAINING_GRAVEYARD_REGION = 13462;
	private static final int NMZ_REGION = 9033;
	private static final int FIGHT_CAVE_REGION = 9551;
	private static final int FIGHT_PIT_REGION = 9552;
	private static final int CLAN_HALL_REGION = 6997;
	public static final int FORTIS_REGION = 7216;

	private static final int DS2_COMPLETED = 215;
	private static final int ACCOUNT_HARDCORE = 3;
	private static final int ACCOUNT_HARDCORE_GROUP = 5;

	private WorldUtils()
	{
	}

	public static boolean isGridMaster(Client client)
	{
		return client.getWorldType().contains(WorldType.TOURNAMENT_WORLD)
			&& client.getVarbitValue(VarbitID.BINGO_INITIAL_SETUP_DONE) > 0;
	}

	public static boolean isIgnoredWorld(Set<WorldType> worldTypes, boolean gridMaster)
	{
		boolean seasonal = gridMaster || worldTypes.contains(WorldType.DEADMAN);
		return !Collections.disjoint(seasonal ? IGNORED_ON_SEASONAL : IGNORED, worldTypes);
	}

	public static String gameModePrefix(Set<WorldType> worldTypes, boolean gridMaster)
	{
		if (worldTypes.contains(WorldType.DEADMAN))
		{
			return "[DMM] ";
		}

		if (gridMaster)
		{
			return "[Grid Master] ";
		}

		return worldTypes.contains(WorldType.SEASONAL) ? "[Leagues] " : "";
	}

	public static Set<WorldType> separateStatsTypes(Set<WorldType> worldTypes)
	{
		return Sets.intersection(worldTypes, SEPARATE_STATS).immutableCopy();
	}

	// Instances report made up coordinates from getWorldLocation
	public static WorldPoint getLocation(Client client, Actor actor)
	{
		WorldView view = actor.getWorldView();
		return view.isInstance()
			? WorldPoint.fromLocalInstance(client, actor.getLocalLocation(), view.getPlane())
			: actor.getWorldLocation();
	}

	public static boolean isHardcore(Client client)
	{
		int account = client.getVarbitValue(VarbitID.IRONMAN);
		return account == ACCOUNT_HARDCORE || account == ACCOUNT_HARDCORE_GROUP;
	}

	public static boolean canBePked(Client client)
	{
		Set<WorldType> worldTypes = client.getWorldType();
		boolean pvpArea = client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) > 0
			|| worldTypes.contains(WorldType.PVP)
			|| worldTypes.contains(WorldType.DEADMAN);
		return pvpArea && !isVisible(client, InterfaceID.PvpIcons.PVPW_SAFE);
	}

	public static DangerLevel getDangerLevel(Client client, int regionId)
	{
		boolean hardcoreGroup = client.getVarbitValue(VarbitID.IRONMAN) == ACCOUNT_HARDCORE_GROUP;

		// Plain hardcores lose their status in these three but not in the other PvM activities
		if (GAUNTLET_REGIONS.contains(regionId) || TOA_REGIONS.contains(regionId) || TOB_REGIONS.contains(regionId))
		{
			return isHardcore(client) ? DangerLevel.DANGEROUS : DangerLevel.SAFE;
		}

		if (regionId == FIGHT_CAVE_REGION)
		{
			return hardcoreGroup ? DangerLevel.DANGEROUS : DangerLevel.EXCEPTIONAL;
		}

		if (regionId == INFERNO_REGION)
		{
			if (hardcoreGroup)
			{
				return DangerLevel.DANGEROUS;
			}

			// Jad challenges share the Inferno's region
			return client.getVarpValue(VarPlayerID.SA_ENERGY_STORED) == 0 ? DangerLevel.EXCEPTIONAL : DangerLevel.SAFE;
		}

		// Western Provinces elite gives one free resurrection a day
		if (ZULRAH_REGIONS.contains(regionId) && client.getVarbitValue(VarbitID.ZULRAH_REVIVE) == 0
			&& client.getVarbitValue(VarbitID.WESTERN_DIARY_ELITE_COMPLETE) > 0)
		{
			return DangerLevel.SAFE;
		}

		if (COX_REGIONS.contains(regionId) || BA_REGIONS.contains(regionId) || regionId == NMZ_REGION
			|| regionId == MAGE_TRAINING_GRAVEYARD_REGION
			|| isVisible(client, InterfaceID.PestStatusOverlay.PEST_STATUS_PORT2)
			|| (GALVEK_REGIONS.contains(regionId) && client.getVarbitValue(VarbitID.DS2) >= DS2_COMPLETED))
		{
			return hardcoreGroup ? DangerLevel.DANGEROUS : DangerLevel.SAFE;
		}

		if (CASTLE_WARS_REGIONS.contains(regionId) || CLAN_WARS_REGIONS.contains(regionId)
			|| SOUL_WARS_REGIONS.contains(regionId) || POH_REGIONS.contains(regionId)
			|| regionId == CLAN_HALL_REGION || regionId == FIGHT_PIT_REGION
			|| LMS_REGIONS.contains(regionId) || isVisible(client, InterfaceID.BrOverlay.CONTENT))
		{
			return DangerLevel.SAFE;
		}

		return DangerLevel.DANGEROUS;
	}

	static boolean isVisible(Client client, int componentId)
	{
		Widget widget = client.getWidget(componentId);
		return widget != null && !widget.isHidden();
	}
}
