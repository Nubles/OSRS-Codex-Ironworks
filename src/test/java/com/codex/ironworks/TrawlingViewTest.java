package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Shoals;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Trawls;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The trawling card, fish paces fed into the ways to get more, and what the watch takes as a catch. */
public class TrawlingViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;
	private static final Shoals.Kind HADDOCK = Shoals.byStore("Raw haddock");
	private static final Shoals.Kind YELLOWFIN = Shoals.byStore("Raw yellowfin");

	private static Map<Skill, Integer> levels(int fishing)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.FISHING, fishing);
		return m;
	}

	private static View view(Works w, Map<Skill, Integer> levels)
	{
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		levels.forEach((s, l) -> xp.put(s, Experience.getXpForLevel(l)));
		return Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1, levels, xp, Altar.GILDED, id -> 0);
	}

	/** A catch every few seconds for ten minutes. */
	private static void trawl(Works w, Shoals.Kind k, long fish, int every, double fishing, double sailing, long from)
	{
		for (int i = 0; i <= 600 / every; i++)
		{
			w.trawls().caught(Collections.singletonMap(k, fish), fishing, sailing, from + i * every * 1000L);
		}
	}

	@Test
	public void noCardUntilSomethingIsTrawled()
	{
		assertNull(view(new Works(), levels(80)).trawling);
	}

	@Test
	public void theCardShowsFishAndExperienceAnHourAndTheBestShoal()
	{
		Works w = new Works();
		trawl(w, HADDOCK, 2, 6, 257, 30, NOW - 3_000_000L);
		trawl(w, YELLOWFIN, 2, 6, 311, 30, NOW - 1_000_000L);
		View.TrawlView v = view(w, levels(80)).trawling;
		assertNotNull(v);
		assertEquals(404, v.fish);
		assertEquals(202, v.catches);
		assertEquals("Yellowfin shoal", v.shoal);
		assertEquals("Raw yellowfin", v.fishName);
		assertEquals(ItemID.RAW_YELLOWFIN, v.icon);
		assertEquals(1200, v.perHour, 1e-6);
		assertEquals(600_000L, v.timedMs);
		assertEquals(311 * 600, v.fishingPerHour, 1e-6);
		assertEquals(30 * 600, v.sailingPerHour, 1e-6);
		assertEquals(2, v.hauls.size());
		assertEquals("Yellowfin shoal", v.best.shoal);
		// Nothing better is open at 80 that has not been trawled.
		assertNull(v.untried);
		// At 84, halibut shoals open and have not been tried.
		View.TrawlView v84 = view(w, levels(84)).trawling;
		assertEquals("Halibut shoal", v84.untried);
		assertEquals(83, v84.untriedLevel);
		assertEquals(195.5, v84.untriedXp, 1e-9);
	}

	@Test
	public void aShoalAboveTheLevelIsNeverTheBest()
	{
		Works w = new Works();
		trawl(w, HADDOCK, 2, 6, 257, 30, NOW - 3_000_000L);
		trawl(w, YELLOWFIN, 2, 6, 311, 30, NOW - 1_000_000L);
		assertEquals("Haddock shoal", view(w, levels(75)).trawling.best.shoal);
	}

	@Test
	public void fishThePlansAreShortOfAreTimedAtThePlayersPace()
	{
		Works w = new Works();
		trawl(w, HADDOCK, 2, 6, 257, 30, NOW - 1_000_000L);
		View.Want haddock = new View.Want();
		haddock.store = "Raw haddock";
		haddock.need = 2400;
		View.Want marlin = new View.Want();
		marlin.store = "Raw marlin";
		marlin.need = 50;
		View.Want coal = new View.Want();
		coal.store = "Coal";
		coal.need = 100;
		View.TrawlView v = Views.trawling(w, levels(80), Arrays.asList(haddock, marlin, coal));
		assertEquals(2, v.plans.size());
		View.FishPlan h = v.plans.get(0);
		assertEquals("Haddock shoal", h.shoal);
		assertEquals(1200, h.perHour, 1e-6);
		assertEquals(2, h.hours, 1e-6);
		assertEquals(0, h.needs);
		View.FishPlan m = v.plans.get(1);
		assertEquals(91, m.needs);
		assertEquals(0, m.perHour, 1e-9);
		// The same pace times trawling wherever Ironworks ranks ways to get raw haddock.
		assertEquals(1200, Views.pace(w, "Raw haddock", Source.GATHERED), 1e-6);
		assertEquals(0, Views.pace(w, "Raw marlin", Source.GATHERED), 1e-9);
	}

	@Test
	public void aCatchCountsOnlyWhileTheNetIsRolling()
	{
		TrawlWatch watch = new TrawlWatch();
		Trawls log = new Trawls();
		// A haddock caught with a rod: Fishing experience and a message, but no Sailing experience.
		watch.xp(Skill.FISHING, 26);
		watch.heard("You catch a haddock!");
		assertFalse(watch.tick(NOW, log));
		assertTrue(log.isEmpty());
		// Trawling: a Sailing drop for the roll, the catch and its Fishing experience.
		watch.xp(Skill.SAILING, 15);
		watch.xp(Skill.FISHING, 257);
		watch.heard("You catch two haddock!");
		assertTrue(watch.tick(NOW + 600, log));
		assertEquals(2, log.fish());
		watch.xp(Skill.SAILING, 15);
		watch.tick(NOW + 1200, log);
		watch.xp(Skill.SAILING, 15);
		watch.xp(Skill.FISHING, 128);
		watch.heard("You catch a haddock!");
		assertTrue(watch.tick(NOW + 1800, log));
		assertEquals(3, log.fish());
		Trawls.Haul h = log.haul(HADDOCK);
		assertEquals(1200, h.getMs());
		// A long way from the last roll the net is up: a message alone is not a catch.
		for (int i = 0; i < TrawlWatch.ROLLING_TICKS + 1; i++)
		{
			watch.tick(NOW + 2400 + i * 600L, log);
		}
		watch.heard("You catch a haddock!");
		assertFalse(watch.tick(NOW + 20_000, log));
		assertEquals(3, log.fish());
	}

	@Test
	public void theNetItselfIsReadOverMessagesOnceItIsSent()
	{
		TrawlWatch watch = new TrawlWatch();
		Trawls log = new Trawls();
		Map<Integer, Long> net = new HashMap<>();
		net.put(ItemID.RAW_HADDOCK, 40L);
		// The first sight of the net is what it held, not a catch.
		watch.net(net);
		watch.xp(Skill.SAILING, 15);
		assertFalse(watch.tick(NOW, log));
		net.put(ItemID.RAW_HADDOCK, 43L);
		net.put(ItemID.RAW_YELLOWFIN, 1L);
		watch.net(net);
		watch.xp(Skill.SAILING, 15);
		watch.heard("You catch three haddock!");
		assertTrue(watch.tick(NOW + 600, log));
		assertEquals(4, log.fish());
		// From now on messages are not counted on top of the net.
		watch.xp(Skill.SAILING, 15);
		watch.heard("You catch two haddock!");
		assertFalse(watch.tick(NOW + 1200, log));
		// Emptying the net is not a catch, and nor is the whole net arriving at once.
		watch.net(Collections.emptyMap());
		Map<Integer, Long> full = new HashMap<>();
		full.put(ItemID.RAW_HADDOCK, 120L);
		watch.net(full);
		watch.xp(Skill.SAILING, 15);
		assertFalse(watch.tick(NOW + 1800, log));
		assertEquals(4, log.fish());
		// A hop starts afresh: messages count again until the net is sent.
		watch.reset();
		watch.xp(Skill.SAILING, 15);
		watch.heard("You catch two haddock!");
		assertTrue(watch.tick(NOW + 2400, log));
		assertEquals(6, log.fish());
	}
}
