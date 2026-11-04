package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Flow;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Pouch;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Use;
import com.codex.ironworks.core.Workshop;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** Pouches and trips as the panel shows them, and the trips folded into banked experience and the ways to get runes. */
public class RunecraftTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;
	private static final long ROUND = 240_000L;

	private static void trip(Works w, String rune, long at, long essence, long runes)
	{
		w.rate(Arrays.asList(Flow.out("Pure essence", essence, Use.MADE_INTO, "Runecraft"),
			Flow.in(rune, runes, Source.MADE, "Runecraft")), at);
		w.rate(Collections.emptyList(), at + 30_000);
	}

	private static Works natures()
	{
		Works w = new Works();
		w.played(TODAY, 3 * 3_600_000L);
		Map<String, Long> bank = new HashMap<>();
		bank.put("Pure essence", 1000L);
		bank.put("Nature rune", 50L);
		w.place(Place.BANK, bank, NOW);
		for (int i = 0; i < 6; i++)
		{
			trip(w, "Nature rune", NOW - 3_600_000L + i * ROUND, 100, 200);
		}
		return w;
	}

	private static Map<Skill, Integer> levels(int rc)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.RUNECRAFT, rc);
		return m;
	}

	private static Map<Skill, Integer> xp(int rc)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.RUNECRAFT, Experience.getXpForLevel(rc));
		return m;
	}

	private static View view(Works w, int rc)
	{
		return Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1, levels(rc), xp(rc), Altar.GILDED, id -> 0);
	}

	@Test
	public void bankedEssenceGoesToTheAltarYourTripsCraftAt()
	{
		View v = view(natures(), 80);
		Workshop.Plan rc = null;
		for (Workshop.Plan p : v.workshop.plans())
		{
			if (p.skill == Skill.RUNECRAFT)
			{
				rc = p;
			}
		}
		assertNotNull(rc);
		assertEquals("Nature rune", rc.makes.get(0).recipe.product);
		// 1,000 essence at 9 experience each (OSRS Wiki: Runecraft).
		assertEquals(9000, rc.xp, 0.5);
		// Without trips the best experience a level allows is counted: the True Blood Altar at 77.
		Works fresh = new Works();
		fresh.place(Place.BANK, Collections.singletonMap("Pure essence", 1000L), NOW);
		View best = view(fresh, 80);
		assertEquals(10_500, best.workshop.plans().get(0).xp, 0.5);
		// A pick is the player's own and beats the trips.
		Works picked = natures();
		picked.method("Pure essence", "Law rune");
		View p = view(picked, 80);
		Views.runecraft(p, picked, Collections.emptyList(), 0, false);
		assertEquals(9500, p.workshop.plans().get(0).xp, 0.5);
		assertNull(p.rc.essenceTo);
	}

	@Test
	public void runesAreTimedByWholeTripsNotGainToGain()
	{
		Works w = natures();
		// 200 natures every 4 minutes is 3,000 an hour; gain to gain would see only the moment at the altar.
		assertEquals(3000, Views.pace(w, "Nature rune", Source.MADE), 0.5);
		assertEquals(3000, Views.pace(w, "Nature rune", Source.GATHERED), 0.5);
		assertEquals(0, Views.pace(w, "Law rune", Source.MADE), 0);
		View.Getting g = Views.getting("Nature rune", -1, w, Catalogue.standard(), levels(80));
		Views.plan(g, 6000, w, levels(80), Collections.emptyList(), null, null);
		assertFalse(g.plan.isEmpty());
		boolean timed = false;
		for (View.PathView pv : g.plan)
		{
			if (pv.text.equals("Craft them at the Nature Altar"))
			{
				timed = pv.timed && pv.basis.startsWith("3,000/h");
			}
		}
		assertTrue(timed);
	}

	@Test
	public void theCardAddsUpEssenceTripsAndTime()
	{
		Works w = natures();
		View v = view(w, 80);
		List<View.PouchView> pouches = Arrays.asList(
			Views.pouch(Pouch.SMALL, ItemID.RCU_POUCH_SMALL, 3, 0, 80),
			Views.pouch(Pouch.GIANT, ItemID.RCU_POUCH_GIANT, 9, 175, 80),
			Views.pouch(Pouch.COLOSSAL, ItemID.RCU_POUCH_COLOSSAL_DEGRADE, 20, 330, 80));
		Views.runecraft(v, w, pouches, 20, true);
		View.RcView rc = v.rc;
		assertEquals(20, rc.pack);
		assertEquals(32, rc.pouched);
		// 3 + 12 + 35 of 40 scaled to level 80's 27: 23.
		assertEquals(3 + 12 + 23, rc.pouchRoom);
		assertEquals(100, rc.essencePerTrip, 0.001);
		assertEquals(10, rc.tripsInStock);
		assertEquals(10 * ROUND, rc.stockMs);
		assertEquals(2000, rc.runesInStock);
		assertEquals("Nature rune", rc.essenceTo);
		assertTrue(pouches.get(1).atRisk);
		assertTrue(pouches.get(2).degraded);
		assertFalse(pouches.get(0).wears);
		assertEquals("Your giant pouch could shrink on its next fill: it holds 12, then 9. The Dark Mage in the Abyss "
			+ "repairs pouches for free; Astral Contact reaches him from anywhere.", Views.pouchWarning(pouches.get(1)));
		assertEquals("Your colossal pouch has worn and holds 23 of 27. The Dark Mage in the Abyss repairs pouches for free; "
			+ "Astral Contact reaches him from anywhere.", Views.pouchWarning(pouches.get(2)));
		// Nothing runecrafting known: no card.
		Works none = new Works();
		View n = view(none, 1);
		Views.runecraft(n, none, Collections.emptyList(), 0, true);
		assertNull(n.rc);
	}
}
