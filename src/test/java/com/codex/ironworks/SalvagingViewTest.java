package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Salvages;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.core.Wrecks;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The salvaging card, salvage paces fed into the ways to get more, and what the watch takes as salvaging and sorting. */
public class SalvagingViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;
	private static final Wrecks.Kind LARGE = Wrecks.bySalvage("Large salvage");
	private static final Wrecks.Kind PIRATE = Wrecks.bySalvage("Plundered salvage");

	private static Map<Skill, Integer> levels(int sailing)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.SAILING, sailing);
		return m;
	}

	private static View view(Works w, Map<Skill, Integer> levels)
	{
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		levels.forEach((s, l) -> xp.put(s, Experience.getXpForLevel(l)));
		return Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1, levels, xp, Altar.GILDED, id -> 0);
	}

	/** A salvage every few seconds for ten minutes. */
	private static void salvage(Works w, Wrecks.Kind k, int every, double xp, long from)
	{
		for (int i = 0; i <= 600 / every; i++)
		{
			w.salvages().salvaged(k, 1, xp, from + i * every * 1000L);
		}
	}

	private static Map<Integer, Long> items(Object... pairs)
	{
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((Integer) pairs[i], ((Number) pairs[i + 1]).longValue());
		}
		return m;
	}

	@Test
	public void noCardUntilSomethingIsSalvaged()
	{
		assertNull(view(new Works(), levels(70)).salvaging);
	}

	@Test
	public void theCardShowsSalvageAndExperienceAnHourAndTheBestWreck()
	{
		Works w = new Works();
		salvage(w, LARGE, 12, 48, NOW - 3_000_000L);
		salvage(w, PIRATE, 15, 76, NOW - 1_000_000L);
		View.SalvageView v = view(w, levels(70)).salvaging;
		assertNotNull(v);
		assertEquals(51 + 41, v.salvage);
		assertEquals("Pirate shipwreck", v.last.wreck);
		assertEquals("Plundered salvage", v.last.salvage);
		assertEquals(ItemID.SAILING_PIRATE_SHIPWRECK_SALVAGE, v.last.icon);
		assertEquals(240, v.last.perHour, 1e-6);
		assertEquals(240 * 76, v.last.xpPerHour, 1e-6);
		// Sorted at the wiki's 31.5 a salvage, on top.
		assertEquals(240 * 76 + 240 * 31.5, v.last.withSorting, 1e-6);
		assertEquals(15, v.last.secondsEach, 1e-9);
		assertEquals(180, v.last.lastsSeconds);
		assertEquals(2, v.wrecks.size());
		assertEquals("Pirate shipwreck", v.best.wreck);
		assertNull(v.untried);
		// A wreck above the level is never the best, and the best open one not tried is named.
		View.SalvageView v60 = view(w, levels(60)).salvaging;
		assertEquals("Large shipwreck", v60.best.wreck);
		View.SalvageView v75 = view(w, levels(75)).salvaging;
		assertEquals("Mercenary shipwreck", v75.untried);
		assertEquals(73, v75.untriedLevel);
		assertEquals(138, v75.untriedXp, 1e-9);
		assertEquals(63.5, v75.untriedSortXp, 1e-9);
		// The same pace times salvaging wherever Ironworks ranks ways to get salvage.
		assertEquals(300, Views.pace(w, "Large salvage", Source.GATHERED), 1e-6);
		assertEquals(0, Views.pace(w, "Opulent salvage", Source.GATHERED), 1e-9);
	}

	@Test
	public void theCardShowsLootAndTheHold()
	{
		Works w = new Works();
		Map<Integer, String> names = new HashMap<>();
		names.put(ItemID.COINS, "Coins");
		names.put(ItemID.STEEL_BAR, "Steel bar");
		for (int i = 0; i < 10; i++)
		{
			w.salvages().sorted(LARGE, 1, 24, i < 8 ? Collections.singletonMap(ItemID.COINS, 50L)
				: Collections.singletonMap(ItemID.STEEL_BAR, 1L), names);
		}
		w.salvages().sorted(LARGE, 2, 48, null, names);
		Map<Wrecks.Kind, Long> hold = new HashMap<>();
		hold.put(LARGE, 30L);
		hold.put(PIRATE, 4L);
		w.salvages().hold(1, 40, 0, hold, NOW - 120_000L);
		w.salvages().holdSize(1, 60);
		View.SalvageView v = view(w, levels(70)).salvaging;
		assertNull(v.last);
		assertEquals(12, v.sorted);
		View.WreckView large = v.wrecks.get(0);
		assertEquals(12, large.sorted);
		assertEquals(2, large.bare);
		assertEquals(24, large.sortXpEach, 1e-9);
		assertEquals("Coins", large.loot.get(0).name);
		assertEquals(400, large.loot.get(0).count);
		assertEquals(40, large.loot.get(0).perSort, 1e-9);
		assertEquals("Steel bar", large.loot.get(1).name);
		assertEquals(40, v.hold.used);
		assertEquals(60, v.hold.capacity);
		assertEquals(34, v.hold.salvage);
		assertEquals(30 * 24 + 4 * 31.5, v.hold.sortXp, 1e-9);
		assertEquals("Large salvage", v.hold.kinds.get(0).name);
		assertEquals(120_000L, v.hold.agoMs);
	}

	@Test
	public void salvageCountsOnlyWithSailingExperience()
	{
		SalvageWatch watch = new SalvageWatch();
		Salvages log = new Salvages();
		Map<Integer, Long> before = items(ItemID.COINS, 10);
		// Salvage taken out of the hold or a bank: no experience, not salvaged.
		Map<Integer, Long> took = items(ItemID.COINS, 10, LARGE.item, 5);
		assertFalse(watch.tick(before, took, false, id -> null, NOW, log));
		assertTrue(log.isEmpty());
		// On the hook: one salvage with the hook's experience.
		watch.xp(Skill.SAILING, 48);
		Map<Integer, Long> hooked = items(ItemID.COINS, 10, LARGE.item, 6);
		assertTrue(watch.tick(took, hooked, false, id -> null, NOW + 600, log));
		assertEquals(1, log.salvage());
		// A crewmate's roll into the hold: experience alone, counted as time at the large wreck.
		watch.xp(Skill.SAILING, 30);
		assertTrue(watch.tick(hooked, hooked, false, id -> null, NOW + 4200, log));
		assertEquals(1, log.salvage());
		assertEquals(3600, log.wreck("Large salvage").getMs());
		// Far more experience than hooks give, such as a delivery, is not salvaging.
		watch.xp(Skill.SAILING, 5000);
		assertFalse(watch.tick(hooked, hooked, false, id -> null, NOW + 4800, log));
		// Anything while a bank is open is left alone.
		watch.xp(Skill.SAILING, 48);
		assertFalse(watch.tick(hooked, items(ItemID.COINS, 10, LARGE.item, 7), true, id -> null, NOW + 5400, log));
		assertEquals(1, log.salvage());
	}

	@Test
	public void sortingIsReadFromThePack()
	{
		SalvageWatch watch = new SalvageWatch();
		Salvages log = new Salvages();
		Map<Integer, Long> before = items(LARGE.item, 5);
		// One sorted: the salvage gone, the wiki's 24 xp, and coins and a steel bar in its place.
		watch.xp(Skill.SAILING, 24);
		Map<Integer, Long> after = items(LARGE.item, 4, ItemID.COINS, 75, ItemID.STEEL_BAR, 1);
		Map<Integer, String> names = new HashMap<>();
		names.put(ItemID.COINS, "Coins");
		names.put(ItemID.STEEL_BAR, "Steel bar");
		assertTrue(watch.tick(before, after, false, names::get, NOW, log));
		Salvages.Wreck w = log.wreck("Large salvage");
		assertEquals(1, w.getSorted());
		assertEquals(24, w.getSortXp(), 1e-9);
		assertEquals(75L, (long) w.getLoot().get(ItemID.COINS));
		assertEquals("Steel bar", log.name(ItemID.STEEL_BAR));
		assertEquals(0, log.salvage());
		// Salvage put in the hold or dropped: gone with no experience, not sorted.
		assertFalse(watch.tick(after, items(ItemID.COINS, 75, ItemID.STEEL_BAR, 1), false, names::get, NOW + 600, log));
		assertEquals(1, w.getSorted());
		// Sorting with the loot dropped: kept nothing.
		watch.xp(Skill.SAILING, 24);
		assertTrue(watch.tick(after, items(LARGE.item, 3, ItemID.COINS, 75, ItemID.STEEL_BAR, 1), false, names::get, NOW + 1200, log));
		assertEquals(2, w.getSorted());
		assertEquals(1, w.getSortedBare());
	}

	@Test
	public void aSinkingWreckIsHeard()
	{
		SalvageWatch watch = new SalvageWatch();
		Salvages log = new Salvages();
		watch.xp(Skill.SAILING, 76);
		watch.tick(items(), items(PIRATE.item, 1), false, id -> null, NOW, log);
		watch.heard("You salvage all you can from the shipwreck before it is reclaimed by the sea.");
		assertTrue(watch.tick(items(PIRATE.item, 1), items(PIRATE.item, 1), false, id -> null, NOW + 600, log));
		assertEquals(1, log.wreck("Plundered salvage").getSunk());
		watch.heard("You salvage some junk.");
		assertFalse(watch.tick(items(PIRATE.item, 1), items(PIRATE.item, 1), false, id -> null, NOW + 1200, log));
	}

	@Test
	public void theHoldSizeIsReadOnlyAsASizeAHoldCanHave()
	{
		assertEquals(160, SalvageWatch.capacity("39/160"));
		assertEquals(160, SalvageWatch.capacity("Capacity: 39 / 160"));
		assertEquals(45, SalvageWatch.capacity("45"));
		assertEquals(0, SalvageWatch.capacity("39/161"));
		assertEquals(0, SalvageWatch.capacity("Slots 12 of 3"));
		assertEquals(0, SalvageWatch.capacity(""));
		assertEquals(0, SalvageWatch.capacity(null));
	}
}
