package com.discordnotifications;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemplateTest
{
	@Test
	public void replacesKnownPlaceholders()
	{
		assertEquals("Zezima's Attack is 99",
			Template.render("$name's $skill is $level", ImmutableMap.of("name", "Zezima", "skill", "Attack", "level", "99")));
	}

	@Test
	public void similarNamesDoNotCollide()
	{
		assertEquals("70 / a, b",
			Template.render("$level / $levels", ImmutableMap.of("level", "70", "levels", "a, b")));
		assertEquals("a, b / 70",
			Template.render("$levels / $level", ImmutableMap.of("level", "70", "levels", "a, b")));
	}

	@Test
	public void unknownPlaceholdersAreBlanked()
	{
		assertEquals("Got  at  kc", Template.render("Got $pet at $kc kc", Collections.emptyMap()));
	}

	@Test
	public void selfSpacingPlaceholdersLeaveNoGapWhenUnknown()
	{
		assertEquals("Zezima added Twisted bow to their collection log",
			Template.render("$name added $entry to their collection log$progress$from$kc", ImmutableMap.of("name", "Zezima", "entry", "Twisted bow")));
	}

	@Test
	public void dollarWithoutANameIsLeftAlone()
	{
		assertEquals("$5 and $", Template.render("$5 and $", Collections.emptyMap()));
	}

	@Test
	public void valuesAreLiteralAndNotScannedAgain()
	{
		assertEquals("$name C:\\\\x $1",
			Template.render("$a $b", ImmutableMap.of("a", "$name", "b", "C:\\x $1", "name", "Zezima")));
	}

	@Test
	public void markdownInValuesIsEscaped()
	{
		assertEquals("\\* \\_ \\~ \\` \\| \\> \\\\",
			Template.render("$a", ImmutableMap.of("a", "* _ ~ ` | > \\")));
	}

	@Test
	public void markdownInTheTemplateIsKept()
	{
		assertEquals("**Dragon \\_ sword** > _99_",
			Template.render("**$item** > _99_", ImmutableMap.of("item", "Dragon _ sword")));
	}
}
