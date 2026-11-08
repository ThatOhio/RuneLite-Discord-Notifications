package com.discordnotifications;

import com.discordnotifications.ItemStackFormatter.Stack;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ItemStackFormatterTest
{
	@Test
	public void formatsStackWithValue()
	{
		assertEquals("3 x Law rune (600)", ItemStackFormatter.format(new Stack("Law rune", 3, 200)));
		assertEquals("12,000 x Coins (12K)", ItemStackFormatter.format(new Stack("Coins", 12_000, 1)));
	}

	@Test
	public void leavesOutValueWhenThereIsNone()
	{
		assertEquals("1 x Clue hunter garb", ItemStackFormatter.format(new Stack("Clue hunter garb", 1, 0)));
	}

	@Test
	public void joinsStacks()
	{
		assertEquals("1 x Rune scimitar (15K), 3 x Law rune (600)", ItemStackFormatter.format(Arrays.asList(
			new Stack("Rune scimitar", 1, 15_000),
			new Stack("Law rune", 3, 200))));
	}

	@Test
	public void emptyListIsBlank()
	{
		assertEquals("", ItemStackFormatter.format(Collections.emptyList()));
		assertEquals(0, ItemStackFormatter.totalValue(Collections.emptyList()));
	}

	@Test
	public void totalsDoNotOverflowInt()
	{
		Stack stack = new Stack("Twisted bow", 2_000, 1_500_000_000L);
		assertEquals(3_000_000_000_000L, stack.getTotalPrice());
		assertEquals(3_000_000_000_000L + 600, ItemStackFormatter.totalValue(Arrays.asList(stack, new Stack("Law rune", 3, 200))));
	}
}
