package com.discordnotifications;

import lombok.Value;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.ItemID;
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

	public Stack stackWithStorePrice(int itemId, int quantity)
	{
		ItemComposition item = itemManager.getItemComposition(itemId);
		long price = inferredPrice(itemId);
		if (price <= 0)
		{
			price = itemManager.getItemPrice(itemId);
		}
		return new Stack(item.getMembersName(), quantity, price > 0 ? price : item.getPrice());
	}

	// Untradeable drops whose store price says nothing about what they're worth
	private long inferredPrice(int itemId)
	{
		switch (itemId)
		{
			case ItemID.ARAXYTE_FANG:
				return gePrice(ItemID.ETCHED_ARAXYTE_FANG);
			case ItemID.ELDER_VENATOR_FANG:
				return gePrice(ItemID.ETCHED_ELDER_VENATOR_FANG);
			case ItemID.NOXIOUS_HALBERD_PART_1:
			case ItemID.NOXIOUS_HALBERD_PART_2:
			case ItemID.NOXIOUS_HALBERD_PART_3:
				return gePrice(ItemID.NOXIOUS_HALBERD) / 3;
			case ItemID.MOKHAIOTL_CLOTH:
				return gePrice(ItemID.CONFLICTION_GAUNTLETS) - gePrice(ItemID.ZENYTE_BRACELET_ENCHANTED) - gePrice(ItemID.DEMON_TEAR) * 10_000;
			default:
				return 0;
		}
	}

	private long gePrice(int itemId)
	{
		return itemManager.getItemPriceWithSource(itemId, true);
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
