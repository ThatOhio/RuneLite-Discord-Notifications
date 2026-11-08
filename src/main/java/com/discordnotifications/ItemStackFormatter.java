package com.discordnotifications;

import lombok.Value;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.stream.Collectors;

@Singleton
public class ItemStackFormatter
{
	@Inject
	private ItemManager itemManager;

	public Stack stack(int itemId, int quantity)
	{
		return new Stack(itemManager.getItemComposition(itemId).getMembersName(), quantity, itemManager.getItemPrice(itemId));
	}

	public static String format(Collection<Stack> stacks)
	{
		return stacks.stream().map(ItemStackFormatter::format).collect(Collectors.joining(", "));
	}

	public static String format(Stack stack)
	{
		String text = QuantityFormatter.formatNumber(stack.getQuantity()) + " x " + stack.getName();
		return stack.getTotalPrice() > 0 ? text + " (" + QuantityFormatter.quantityToStackSize(stack.getTotalPrice()) + ")" : text;
	}

	public static long totalValue(Collection<Stack> stacks)
	{
		return stacks.stream().mapToLong(Stack::getTotalPrice).sum();
	}

	@Value
	public static class Stack
	{
		String name;
		int quantity;
		long unitPrice;

		public long getTotalPrice()
		{
			return unitPrice * quantity;
		}
	}
}
