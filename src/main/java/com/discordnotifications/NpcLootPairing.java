package com.discordnotifications;

import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Singleton
public class NpcLootPairing
{
	enum Signal
	{
		TILE_SCAN,
		SERVER
	}

	// Core can hold the tile scan for 10 ticks (6 s) after the kill
	private static final Duration WINDOW = Duration.ofSeconds(8);

	@Inject
	private ItemManager itemManager;

	private final Deque<Kill> unpaired = new ArrayDeque<>();

	void reset()
	{
		unpaired.clear();
	}

	boolean isNewKill(String source, Collection<ItemStack> items, Signal signal, Instant now)
	{
		while (!unpaired.isEmpty() && Duration.between(unpaired.peek().time, now).compareTo(WINDOW) > 0)
		{
			unpaired.poll();
		}

		Map<Integer, Integer> totals = totals(items);
		Kill pair = findPair(source, totals, signal);
		if (pair != null)
		{
			unpaired.remove(pair);
			return false;
		}

		unpaired.add(new Kill(source, totals, signal, now));
		return true;
	}

	// Tightest match first, so a loose one can't take the pair of the kill queued behind it
	private Kill findPair(String source, Map<Integer, Integer> totals, Signal signal)
	{
		Kill subset = null;
		Kill oneSided = null;
		for (Kill kill : unpaired)
		{
			if (kill.signal == signal || !kill.source.equals(source))
			{
				continue;
			}

			if (kill.totals.equals(totals))
			{
				return kill;
			}
			if (subset == null && (containedIn(kill.totals, totals) || containedIn(totals, kill.totals)))
			{
				subset = kill;
			}
			if (oneSided == null && differsOnlyInOneSidedItems(kill.totals, totals))
			{
				oneSided = kill;
			}
		}
		return subset != null ? subset : oneSided;
	}

	private boolean differsOnlyInOneSidedItems(Map<Integer, Integer> a, Map<Integer, Integer> b)
	{
		Set<Integer> ids = new HashSet<>(a.keySet());
		ids.addAll(b.keySet());

		boolean shared = false;
		for (int id : ids)
		{
			if (a.getOrDefault(id, 0).equals(b.getOrDefault(id, 0)))
			{
				shared = true;
			}
			else if (!isOneSidedItem(id))
			{
				return false;
			}
		}
		return shared;
	}

	// Bonecrusher and ash sanctifier keep these off the ground, clue drops can be missing from either list
	private boolean isOneSidedItem(int id)
	{
		ItemComposition item = itemManager.getItemComposition(id);
		String name = item == null || item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
		return name.endsWith("bones") || name.endsWith("ashes") || name.startsWith("scroll box (") || name.startsWith("clue scroll (");
	}

	private static boolean containedIn(Map<Integer, Integer> part, Map<Integer, Integer> whole)
	{
		return part.entrySet().stream().allMatch(item -> whole.getOrDefault(item.getKey(), 0) >= item.getValue());
	}

	// Summed because the events stack non-stackables differently
	private static Map<Integer, Integer> totals(Collection<ItemStack> items)
	{
		Map<Integer, Integer> totals = new HashMap<>();
		for (ItemStack item : items)
		{
			totals.merge(item.getId(), item.getQuantity(), Integer::sum);
		}
		return totals;
	}

	private static class Kill
	{
		private final String source;
		private final Map<Integer, Integer> totals;
		private final Signal signal;
		private final Instant time;

		Kill(String source, Map<Integer, Integer> totals, Signal signal, Instant time)
		{
			this.source = source;
			this.totals = totals;
			this.signal = signal;
			this.time = time;
		}
	}
}
