package com.discordnotifications;

import org.junit.Test;

import java.util.function.ToIntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CollectionLogRankTest
{
	private static final int[] THRESHOLDS = {100, 300, 500, 700, 900, 1000, 1100, 1200, 1500};
	private static final ToIntFunction<CollectionLogRank> KNOWN = rank -> THRESHOLDS[rank.ordinal()];

	@Test
	public void noRankBelowBronze()
	{
		assertNull(CollectionLogRank.at(99, KNOWN));
	}

	@Test
	public void reachesARankExactlyOnItsThreshold()
	{
		assertEquals(CollectionLogRank.BRONZE, CollectionLogRank.at(100, KNOWN));
		assertEquals(CollectionLogRank.BRONZE, CollectionLogRank.at(299, KNOWN));
		assertEquals(CollectionLogRank.IRON, CollectionLogRank.at(300, KNOWN));
		assertEquals(CollectionLogRank.GILDED, CollectionLogRank.at(1500, KNOWN));
	}

	@Test
	public void skipsAThresholdItCouldNotRead()
	{
		ToIntFunction<CollectionLogRank> missingIron = rank -> rank == CollectionLogRank.IRON ? 0 : KNOWN.applyAsInt(rank);
		assertEquals(CollectionLogRank.BRONZE, CollectionLogRank.at(300, missingIron));
	}

	@Test
	public void gildedIsNinetyPercentRoundedDownToTwentyFive()
	{
		assertEquals(1500, CollectionLogRank.gildedThreshold(1692));
		assertEquals(1125, CollectionLogRank.gildedThreshold(1250));
		assertEquals(1100, CollectionLogRank.gildedThreshold(1249));
	}
}
