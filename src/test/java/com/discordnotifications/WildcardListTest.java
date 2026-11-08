package com.discordnotifications;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WildcardListTest
{
	@Test
	public void plainNameHasToMatchWhole()
	{
		WildcardList list = WildcardList.parse("Bones");
		assertTrue(list.matches("Bones"));
		assertTrue(list.matches("bones"));
		assertFalse(list.matches("Big bones"));
		assertFalse(list.matches("Bones to peaches"));
	}

	@Test
	public void wildcards()
	{
		WildcardList list = WildcardList.parse("Clue scroll*\n*ahrim's*\nDragon*helm");
		assertTrue(list.matches("Clue scroll (hard)"));
		assertTrue(list.matches("Clue scroll"));
		assertTrue(list.matches("Ahrim's hood"));
		assertTrue(list.matches("Dragon full helm"));
		assertFalse(list.matches("Dragon boots"));
	}

	@Test
	public void separatorsAndBlanks()
	{
		WildcardList list = WildcardList.parse(" Bones , Ashes;\n\n Coal ");
		assertTrue(list.matches("Bones"));
		assertTrue(list.matches("Ashes"));
		assertTrue(list.matches("Coal"));
		assertFalse(list.matches(""));
	}

	@Test
	public void emptyListMatchesNothing()
	{
		assertFalse(WildcardList.parse("").matches("Bones"));
		assertFalse(WildcardList.parse(" \n , ").matches(""));
	}

	@Test
	public void regexCharactersAreLiteral()
	{
		WildcardList list = WildcardList.parse("Ring of wealth (i5)");
		assertTrue(list.matches("Ring of wealth (i5)"));
		assertFalse(list.matches("Ring of wealth i5"));
	}
}
