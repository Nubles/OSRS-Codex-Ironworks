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
import com.codex.ironworks.core.Recipe;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/** The herbiboar card on the Sources tab, and its herbs fed into the ways to get more. */
public class HerbiboarViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;
	private static final long TRAIL = 72_000L;

	private static Map<Skill, Integer> levels(int hunter, int herblore)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.HUNTER, hunter);
		m.put(Skill.HERBLORE, herblore);
		return m;
	}

	private static Map<Skill, Integer> xp(Map<Skill, Integer> levels)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		levels.forEach((s, l) -> m.put(s, Experience.getXpForLevel(l)));
		return m;
	}

	private static View view(Works w, Map<Skill, Integer> levels)
	{
		return Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1, levels, xp(levels), Altar.GILDED, id -> 0);
	}

	/** Four harvests 72 seconds apart: three timed trails at 50 an hour. */
	private static Works hunted(int herblore)
	{
		Works w = new Works();
		w.played(TODAY, 2 * 3_600_000L);
		Map<String, Long> bank = new HashMap<>();
		bank.put("Ranarr weed", 40L);
		bank.put("Vial of water", 500L);
		w.place(Place.BANK, bank, NOW);
		Object[][] harvests = {
			{"Ranarr weed", 2, "Irit leaf", 1},
			{"Ranarr weed", 1, "Irit leaf", 1},
			{"Avantoe", 2},
			{"Ranarr weed", 1, "Avantoe", 1, "Irit leaf", 1},
		};
		for (int i = 0; i < harvests.length; i++)
		{
			List<Flow> flows = new ArrayList<>();
			for (int k = 0; k < harvests[i].length; k += 2)
			{
				flows.add(Flow.in((String) harvests[i][k], ((Number) harvests[i][k + 1]).longValue(), Source.GATHERED, "Hunter"));
			}
			long at = NOW - 3_600_000L + i * TRAIL;
			w.rate(flows, at, true, herblore);
			w.rate(Collections.emptyList(), at + 10_000, true, herblore);
		}
		return w;
	}

	@Test
	public void theCardShowsYourTrailsHerbsAndHerbloreAnHour()
	{
		Map<Skill, Integer> lv = levels(85, 60);
		View v = view(hunted(60), lv);
		View.HerbiView h = v.herbiboar;
		assertNotNull(h);
		assertEquals(4, h.harvests);
		assertEquals(3, h.timed);
		assertEquals(50, h.trailsPerHour, 1e-6);
		assertEquals(72_000, h.trailMs);
		assertEquals(41, h.bandLo);
		assertEquals(61, h.bandHi);
		assertFalse(h.bandStale);
		// 10 herbs over 4 harvests.
		assertEquals(10 / 4.0, h.herbsPerHarvest, 1e-9);
		assertEquals(50 * 10 / 4.0, h.herbsPerHour, 1e-6);
		// 25 Herblore experience for each herb but the first.
		assertEquals(50 * (10 / 4.0 - 1) * 25, h.harvestXpPerHour, 1e-6);
		View.HerbView top = h.herbs.get(0);
		assertEquals("Ranarr weed", top.herb);
		assertEquals(207, top.icon);
		assertEquals(1.0, top.perHarvest, 1e-9);
		assertEquals(50, top.perHour, 1e-6);
		assertEquals("Prayer potion(3)", top.potion);
		assertEquals(87.5, top.potionXp, 1e-9);
		// Irit at 60 Herblore: super antipoison (48) beats super attack (45).
		double banked = 0;
		for (View.HerbView hv : h.herbs)
		{
			banked += hv.perHour * hv.potionXp;
		}
		assertEquals(banked, h.bankedXpPerHour, 1e-6);
		assertTrue(h.unseen.contains("Lantadyme"));
		assertFalse(h.unseen.contains("Ranarr weed"));
		assertFalse(h.unseen.contains("Torstol"));
	}

	@Test
	public void herbsFromTheHerbiboarAreTimedByTrailsInTheWaysToGetMore()
	{
		Works w = hunted(60);
		assertEquals(50, Views.pace(w, "Ranarr weed", Source.GATHERED), 1e-6);
		View.Getting g = Views.getting("Ranarr weed", 207, w, Catalogue.standard(), levels(85, 60));
		View.WayView hunt = null;
		for (View.WayView wv : g.ways)
		{
			if (wv.text.startsWith("Hunt the herbiboar"))
			{
				hunt = wv;
			}
		}
		assertNotNull(hunt);
		assertTrue(hunt.open);
		assertEquals(50, hunt.perHour, 1e-6);
		// With a pace it ranks first.
		assertTrue(g.ways.get(0) == hunt);
	}

	@Test
	public void waysFollowTheHerbloreLevel()
	{
		Works w = new Works();
		View.Getting marrentill = Views.getting("Marrentill", -1, w, Catalogue.standard(), levels(85, 81));
		for (View.WayView wv : marrentill.ways)
		{
			assertFalse(wv.text.startsWith("Hunt the herbiboar"));
		}
		View.Getting torstol = Views.getting("Torstol", -1, w, Catalogue.standard(), levels(85, 70));
		boolean found = false;
		for (View.WayView wv : torstol.ways)
		{
			if (wv.text.startsWith("Hunt the herbiboar"))
			{
				found = true;
				assertFalse(wv.open);
				assertEquals(Skill.HERBLORE, wv.skill);
				assertEquals(77, wv.level);
			}
		}
		assertTrue(found);
		View.Getting huasca = Views.getting("Huasca", -1, w, Catalogue.standard(), levels(85, 70));
		for (View.WayView wv : huasca.ways)
		{
			assertFalse(wv.text.startsWith("Hunt the herbiboar"));
		}
	}

	@Test
	public void anAccountThatCannotReachItSeesNoCard()
	{
		assertNull(view(new Works(), levels(60, 60)).herbiboar);
		assertNull(view(new Works(), Collections.emptyMap()).herbiboar);
		View.HerbiView invite = view(new Works(), levels(76, 50)).herbiboar;
		assertNotNull(invite);
		assertEquals(0, invite.harvests);
		assertEquals(1, invite.missing.size());
		assertTrue(invite.missing.get(0).contains("80 Hunter"));
		assertTrue(view(new Works(), levels(80, 50)).herbiboar.missing.isEmpty());
	}

	@Test
	public void aNewHerbloreStretchIsCalledOut()
	{
		View.HerbiView h = view(hunted(60), levels(85, 62)).herbiboar;
		assertTrue(h.bandStale);
	}

	@Test
	public void theBestPotionIsTheMostExperienceAtYourLevel()
	{
		Recipe r = Views.bestPotion("Ranarr weed", 37);
		assertNull(r);
		assertEquals("Prayer potion(3)", Views.bestPotion("Ranarr weed", 38).product);
		assertEquals("Superantipoison(3)", Views.bestPotion("Irit leaf", 60).product);
		assertEquals("Zamorak brew(3)", Views.bestPotion("Torstol", 90).product);
		assertNull(Views.bestPotion("Torstol", 77));
		assertEquals("Zamorak brew(3)", Views.bestPotion("Torstol", 80).product);
	}
}
