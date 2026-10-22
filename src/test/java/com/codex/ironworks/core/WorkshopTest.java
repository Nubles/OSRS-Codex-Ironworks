package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class WorkshopTest
{
	private static final Catalogue.Lookup LOOKUP = Catalogue.standard();

	private static Map<String, Long> stocks(Object... pairs)
	{
		Map<String, Long> m = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
		}
		return m;
	}

	@Test
	public void everyRecipeIsMadeOfStoresAndMakesWhatItSays()
	{
		for (Recipe r : Recipes.all())
		{
			assertTrue(r.product, r.productItem > 0);
			assertTrue(r.product, r.level >= 1 && r.level <= 99);
			assertTrue(r.product, r.xp >= 0);
			for (String in : r.inputs.keySet())
			{
				Catalogue.Match m = LOOKUP.match(in);
				assertNotNull(r.product + " needs " + in + ", which is no store", m);
				assertEquals(in, m.store);
			}
			if (r.count > 0 && r.tier < 9)
			{
				// Intermediate things must be stores, or nothing could be made from them.
				boolean usedLater = false;
				for (Recipe o : Recipes.all())
				{
					usedLater |= o.tier > r.tier && LOOKUP.match(r.product) != null && o.inputs.containsKey(LOOKUP.match(r.product).store);
				}
				if (r.xp == 0)
				{
					assertTrue(r.product + " gives nothing and feeds nothing", usedLater);
				}
			}
		}
	}

	@Test
	public void herbsAndSecondariesBecomePotionsCountedInDoses()
	{
		Workshop w = Workshop.plan(stocks("Ranarr weed", 100, "Vial of water", 100, "Snape grass", 60), LOOKUP,
			Collections.emptyMap(), Altar.GILDED);
		Workshop.Plan herblore = w.plan(Skill.HERBLORE);
		assertNotNull(herblore);
		assertEquals(60 * 87.5, herblore.xp, 0.01);
		Workshop.Short s = herblore.shorts.get(0);
		assertEquals("Snape grass", s.input);
		assertEquals(40, s.need);
		assertEquals(40, s.more);
	}

	@Test
	public void unfinishedPotionsShortOfVialsSayWhatTheyWouldFinish()
	{
		Workshop w = Workshop.plan(stocks("Ranarr weed", 50, "Vial of water", 20, "Snape grass", 50), LOOKUP,
			Collections.emptyMap(), Altar.GILDED);
		Workshop.Plan herblore = w.plan(Skill.HERBLORE);
		assertEquals(20 * 87.5, herblore.xp, 0.01);
		boolean vials = false;
		for (Workshop.Short s : herblore.shorts)
		{
			if (s.input.equals("Vial of water"))
			{
				vials = true;
				assertEquals(30, s.need);
				assertEquals(30 * 87.5, s.xp, 0.01);
			}
		}
		assertTrue(vials);
	}

	@Test
	public void bonesCountAtTheChosenAltarAndAshesDoNot()
	{
		Map<String, Long> st = stocks("Dragon bones", 10, "Infernal ashes", 10);
		Workshop bury = Workshop.plan(st, LOOKUP, Collections.emptyMap(), Altar.BURY);
		Workshop gilded = Workshop.plan(st, LOOKUP, Collections.emptyMap(), Altar.GILDED);
		assertEquals(10 * 72 + 10 * 110, bury.plan(Skill.PRAYER).xp, 0.01);
		assertEquals(10 * 72 * 3.5 + 10 * 110, gilded.plan(Skill.PRAYER).xp, 0.01);
	}

	@Test
	public void logsAreFletchedBeforeAnyAreBurned()
	{
		Workshop w = Workshop.plan(stocks("Yew logs", 100, "Bow string", 40), LOOKUP, Collections.emptyMap(), Altar.GILDED);
		assertEquals(40 * 150 + 60 * 75, w.plan(Skill.FLETCHING).xp, 0.01);
		assertNull(w.plan(Skill.FIREMAKING));
	}

	@Test
	public void recipesAboveTheLevelAreKeptApart()
	{
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		levels.put(Skill.COOKING, 70);
		Workshop w = Workshop.plan(stocks("Raw shark", 10, "Raw lobster", 10), LOOKUP, levels, Altar.GILDED);
		Workshop.Plan cooking = w.plan(Skill.COOKING);
		assertEquals(10 * 120, cooking.xp, 0.01);
		assertEquals(10 * 210, cooking.lockedXp, 0.01);
		assertEquals(10 * 120, w.total(), 0.01);
		assertEquals(10 * 210, w.lockedTotal(), 0.01);
	}

	@Test
	public void chainsRunThroughSeveralSteps()
	{
		// Sand and soda ash become glass, glass an orb, the orb is charged, and a staff takes it.
		Workshop w = Workshop.plan(stocks("Bucket of sand", 5, "Soda ash", 5, "Cosmic rune", 15, "Air rune", 150, "Battlestaff", 5),
			LOOKUP, Collections.emptyMap(), Altar.GILDED);
		assertEquals(5 * 20 + 5 * 52.5 + 5 * 137.5, w.plan(Skill.CRAFTING).xp, 0.01);
		assertEquals(5 * 76, w.plan(Skill.MAGIC).xp, 0.01);
	}

	@Test
	public void nothingToMakeIsEmpty()
	{
		Workshop w = Workshop.plan(stocks("Coins", 1000), LOOKUP, Collections.emptyMap(), Altar.GILDED);
		assertTrue(w.plans().isEmpty());
		assertFalse(w.total() > 0);
	}
}
