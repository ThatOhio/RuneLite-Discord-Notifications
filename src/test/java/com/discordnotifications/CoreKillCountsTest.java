package com.discordnotifications;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoreKillCountsTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void readsKillsFromSavedConfigLoot()
	{
		String json = "{\"type\":\"NPC\",\"name\":\"Zulrah\",\"kills\":12,\"first\":1762600000000,\"last\":1762610000000,\"drops\":[12934,250,526,1]}";
		assertEquals(12, CoreKillCounts.parseKills(GSON, json));
	}

	@Test
	public void missingKillsIsZero()
	{
		assertEquals(0, CoreKillCounts.parseKills(GSON, "{\"name\":\"Zulrah\"}"));
		assertEquals(0, CoreKillCounts.parseKills(GSON, "null"));
	}
}
