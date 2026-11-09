package com.discordnotifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.util.function.ToIntFunction;

@Slf4j
@RequiredArgsConstructor
public enum CollectionLogRank
{
	BRONZE("Bronze", 1714),
	IRON("Iron", 1715),
	STEEL("Steel", 1716),
	BLACK("Black", 1717),
	MITHRIL("Mithril", 1718),
	ADAMANT("Adamant", 1719),
	RUNE("Rune", 1740),
	DRAGON("Dragon", 1741),
	GILDED("Gilded", -1);

	private static final int THRESHOLD_PARAM = 2232;

	private final String displayName;
	private final int structId;

	public int threshold(Client client, int totalEntries)
	{
		if (this == GILDED)
		{
			return gildedThreshold(totalEntries);
		}

		// A missing struct shouldn't cost the player their collection log post
		try
		{
			return client.getStructComposition(structId).getIntValue(THRESHOLD_PARAM);
		}
		catch (RuntimeException e)
		{
			log.debug("Could not read the threshold for {}", this, e);
			return 0;
		}
	}

	// Scales with the log size: 90% of it, rounded down to a multiple of 25
	static int gildedThreshold(int totalEntries)
	{
		return 25 * (9 * totalEntries / 250);
	}

	public static CollectionLogRank at(int completed, ToIntFunction<CollectionLogRank> thresholds)
	{
		CollectionLogRank reached = null;
		for (CollectionLogRank rank : values())
		{
			int threshold = thresholds.applyAsInt(rank);
			if (threshold > 0 && completed >= threshold)
			{
				reached = rank;
			}
		}
		return reached;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
