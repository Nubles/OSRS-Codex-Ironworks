package com.codex.ironworks.core;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AcquireTest
{
	private static Map<Skill, Integer> levels(Object... pairs)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((Skill) pairs[i], (Integer) pairs[i + 1]);
		}
		return m;
	}

	@Test
	public void theWikiRatesAreTheOnesCited()
	{
		Acquire.Lead wine = Acquire.leads("Wine of Zamorak").get(0);
		assertEquals(204, wine.perHour, 0);
		assertEquals(Skill.MAGIC, wine.skill);
		assertEquals(33, wine.level);
		assertEquals(950, Acquire.leads("Snape grass").get(0).perHour, 0);
		assertEquals(390, Acquire.leads("Red spiders' eggs").get(0).perHour, 0);
		assertEquals(400, Acquire.leads("White berries").get(0).perHour, 0);
		assertEquals(56, Acquire.leads("White berries").get(0).level);
		assertEquals(1620, Acquire.leads("Mort myre fungus").get(0).perHour, 0);
		// Chickens: 5 feathers at 64/128 and 15 at 32/128.
		assertEquals(6.25, Acquire.leads("Feather").get(1).perKill, 1e-9);
		assertEquals(10_000, Acquire.leads("Feather").get(0).perWorld);
		assertEquals(11 / 128.0, Acquire.leads("Limpwurt root").get(0).perKill, 1e-9);
	}

	@Test
	public void lockedPathsGoLastAndWikiRatesTimeTheRest()
	{
		List<Acquire.Path> p = Acquire.rank("White berries", 800, levels(Skill.AGILITY, 50, Skill.FARMING, 70), null,
			Collections.emptyList(), null, null, -1);
		// The bush is open but untimed; Isafdar is timed but needs 56 Agility.
		assertEquals(Acquire.Kind.FARM, p.get(0).kind);
		assertNull(p.get(0).locked);
		assertEquals(-1, p.get(0).seconds);
		assertEquals("Agility 56", p.get(1).locked);
		assertEquals(2 * 3600, p.get(1).seconds);
	}

	@Test
	public void ownPaceBeatsTheWikiAndTravelCounts()
	{
		Travel atlas = Travel.read(new Gson(), "{\"version\":1,\"walkSecondsPerTile\":0.3,\"teleports\":["
			+ "{\"name\":\"Falador Teleport\",\"x\":2965,\"y\":3379,\"plane\":0,\"seconds\":5,\"carried\":true}]}");
		int[] here = {3222, 3218, 0};
		List<Acquire.Path> wiki = Acquire.rank("Wine of Zamorak", 204, levels(Skill.MAGIC, 60), null, Collections.emptyList(), atlas, here, -1);
		Acquire.Path p = wiki.get(0);
		assertEquals("wiki rate", p.basis);
		assertNotNull(p.leg);
		assertEquals("Falador Teleport", p.leg.via);
		// 5s teleport + 126 tiles north at 0.3s, then an hour of work.
		assertEquals(5 + Math.round(126 * 0.3) + 3600, p.seconds);
		List<Acquire.Path> mine = Acquire.rank("Wine of Zamorak", 204, levels(Skill.MAGIC, 60),
			(store, src) -> src == Source.GATHERED ? 408 : 0, Collections.emptyList(), atlas, here, -1);
		assertEquals("your pace", mine.get(0).basis);
		assertEquals(5 + Math.round(126 * 0.3) + 1800, mine.get(0).seconds);
	}

	@Test
	public void feathersBoughtBeatChickensUntilYouHaveAPace()
	{
		List<Acquire.Path> p = Acquire.rank("Feather", 5_000, levels(), null, Collections.emptyList(), null, null, -1);
		assertEquals(Acquire.Kind.SHOP, p.get(0).kind);
		assertEquals(Acquire.WORLD_SECONDS, p.get(0).seconds);
		assertEquals(Acquire.Kind.DROP, p.get(1).kind);
		assertEquals(800, p.get(1).kills);
		// Once the player has looted feathers at a pace, the chickens are timed too.
		List<Acquire.Path> paced = Acquire.rank("Feather", 50_000, levels(), (s, src) -> src == Source.LOOTED ? 2_000_000 : 0,
			Collections.emptyList(), null, null, -1);
		assertEquals(Acquire.Kind.DROP, paced.get(0).kind);
		assertEquals(90, paced.get(0).seconds);
		assertEquals(5 * Acquire.WORLD_SECONDS, paced.get(1).seconds);
	}

	@Test
	public void shopsYouUsedAreCostedFromTheStockYouSaw()
	{
		Acquire.Offer jatix = new Acquire.Offer("Jatix's Herblore Shop", 11573, 300);
		List<Acquire.Path> p = Acquire.rank("Vial of water", 900, levels(), null, Collections.singletonList(jatix), null, null, -1);
		assertEquals(1, p.size());
		assertEquals(3 * Acquire.WORLD_SECONDS, p.get(0).seconds);
		int[] c = Acquire.centre(11573);
		assertEquals((45 << 6) + 32, c[0]);
		assertEquals((53 << 6) + 32, c[1]);
		assertTrue(Acquire.knows("snape GRASS"));
	}

	@Test
	public void recipesAreTimedAtYourMakingPace()
	{
		List<Acquire.Path> p = Acquire.rank("Crushed nest", 100, levels(Skill.HERBLORE, 1), (s, src) -> src == Source.MADE ? 600 : 0,
			Collections.emptyList(), Collections.singletonList(new Acquire.Make("Make it from bird nest", Skill.HERBLORE, 1)), null, null, -1);
		assertEquals(Acquire.Kind.MAKE, p.get(0).kind);
		assertEquals(600, p.get(0).seconds);
	}
}
