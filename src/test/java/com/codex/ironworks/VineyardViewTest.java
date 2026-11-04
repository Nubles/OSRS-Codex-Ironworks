package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Acquire;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Spot;
import com.codex.ironworks.core.Vineyard;
import com.codex.ironworks.core.Vineyards;
import com.codex.ironworks.core.Ways;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The vineyard card, the vineyard pace in the ways to get grapes, and what the watch takes as a barrel handed in. */
public class VineyardViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;

	private static Map<Skill, Integer> levels(int cooking)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.COOKING, cooking);
		m.put(Skill.FARMING, 30);
		return m;
	}

	private static Map<Skill, Integer> xp(Map<Skill, Integer> levels)
	{
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		levels.forEach((s, l) -> xp.put(s, Experience.getXpForLevel(l)));
		return xp;
	}

	private static View view(Works w, Map<Skill, Integer> levels)
	{
		return Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1, levels, xp(levels), Altar.GILDED, id -> 0);
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

	/** A barrel taken, then six handed in two minutes apart. */
	private static void sitting(Works w, long from)
	{
		w.vineyard().given(from);
		for (int i = 1; i <= 6; i++)
		{
			w.vineyard().handedIn(5, from + i * 120_000L);
		}
	}

	@Test
	public void noCardUntilABarrelIsTaken()
	{
		assertNull(view(new Works(), levels(50)).vineyard);
		Works w = new Works();
		w.vineyard().given(NOW);
		View.VineyardView v = view(w, levels(50)).vineyard;
		assertNotNull(v);
		assertEquals(0, v.barrels);
		assertEquals(0, v.grapesPerHour, 0);
	}

	@Test
	public void theCardShowsGrapesAnHourWineAndSittings()
	{
		Works w = new Works();
		sitting(w, NOW - 5 * 3_600_000L);
		sitting(w, NOW - 3_600_000L);
		w.hold(Spot.INVENTORY, items(Vineyard.GRAPES, 40L, Vineyard.BARREL, 1L), Collections.emptyMap(), NOW);
		w.hold(Spot.BANK, items(Vineyard.GRAPES, 100L), Collections.emptyMap(), NOW);
		Map<Skill, Integer> levels = levels(50);
		View.VineyardView v = view(w, levels).vineyard;
		assertEquals(12, v.barrels);
		assertEquals(60, v.grapes);
		assertEquals(5, v.perBarrel, 0);
		assertEquals(12, v.timed);
		assertEquals(30, v.barrelsPerHour, 1e-9);
		assertEquals(150, v.grapesPerHour, 1e-9);
		assertEquals(120_000L, v.roundMs);
		assertEquals(150 * 200, v.wineXpPerHour, 1e-9);
		assertEquals(140, v.held);
		assertEquals(140 * 200, v.heldXp, 0);
		assertEquals(2, v.sittings.size());
		// The latest sitting first.
		assertEquals(NOW - 3_600_000L, v.sittings.get(0).start);
		assertEquals(6, v.sittings.get(0).barrels);
		assertEquals(30, v.sittings.get(0).grapes);
		assertEquals(150, v.sittings.get(0).grapesPerHour, 1e-9);
		// The goal: grapes to the next Cooking level as wine, less those held, at the player's pace.
		Views.vineyardGoal(v, Vineyard.Goal.NEXT_LEVEL, xp(levels));
		assertEquals(51, v.goalLevel);
		long need = Vineyard.grapesTo(Experience.getXpForLevel(50), 51);
		assertEquals(need, v.goalGrapes);
		assertEquals(Math.max(0, need - 140) / 150.0, v.goalHours, 1e-9);
		View.VineyardView none = view(w, levels).vineyard;
		Views.vineyardGoal(none, Vineyard.Goal.NONE, xp(levels));
		assertNull(none.goal);
		// Below 35, where wine is first made, the goal counts from 35.
		View.VineyardView low = view(w, levels(30)).vineyard;
		Views.vineyardGoal(low, Vineyard.Goal.NEXT_LEVEL, xp(levels(30)));
		assertEquals(36, low.goalLevel);
		assertEquals(Vineyard.grapesTo(Experience.getXpForLevel(35), 36), low.goalGrapes);
		View.VineyardView no = view(w, levels(70)).vineyard;
		Views.vineyardGoal(no, Vineyard.Goal.NO_FAIL, xp(levels(70)));
		assertNull(no.goal);
	}

	@Test
	public void theVineyardPaceRanksAmongTheWaysToGetGrapes()
	{
		Works w = new Works();
		assertEquals(0, Views.pace(w, "Grapes", Source.GATHERED), 0);
		sitting(w, NOW - 3_600_000L);
		assertEquals(150, Views.pace(w, "Grapes", Source.GATHERED), 1e-9);
		assertEquals(0, Views.pace(w, "Grapes", Source.FARMED), 0);
		// The ways to get grapes: the vineyard first at the player's own pace; the Vinery locked at 30 Farming.
		View.Getting g = Views.getting("Grapes", Vineyard.GRAPES, w, Catalogue.standard(), levels(50));
		assertEquals(Ways.Kind.GATHER, g.ways.get(0).kind);
		assertTrue(g.ways.get(0).text.contains("Aldarin"));
		assertEquals(150, g.ways.get(0).perHour, 1e-9);
		boolean vinery = false;
		for (View.WayView wv : g.ways)
		{
			if (wv.kind == Ways.Kind.FARM)
			{
				vinery = true;
				assertEquals(36, wv.level);
				assertFalse(wv.open);
			}
		}
		assertTrue(vinery);
		// Costed for 300 grapes: the vineyard is timed at the player's pace, the Vinery locked.
		List<Acquire.Path> paths = Acquire.rank("Grapes", 300, levels(50), (store, src) -> Views.pace(w, store, src),
			Collections.emptyList(), null, null, 60);
		Acquire.Path vineyard = null;
		for (Acquire.Path p : paths)
		{
			if (p.kind == Acquire.Kind.GATHER)
			{
				vineyard = p;
			}
			if (p.kind == Acquire.Kind.FARM)
			{
				assertEquals("Farming 36", p.locked);
			}
		}
		assertNotNull(vineyard);
		assertEquals("your pace", vineyard.basis);
		assertEquals(2 * 3600, vineyard.seconds);
		assertEquals(Vineyard.QUEST, vineyard.needs);
		// Wine is a way to use grapes in the Make tab's plan.
		assertTrue(Catalogue.standard().match("Grapes") != null);
	}

	@Test
	public void grapesWithTheBarrelCarriedAreABarrelHandedIn()
	{
		VineyardWatch watch = new VineyardWatch();
		Vineyards log = new Vineyards();
		Map<Integer, Long> empty = items(ItemID.COINS, 10);
		// The foreman's barrel comes into the pack.
		Map<Integer, Long> barrel = items(ItemID.COINS, 10, Vineyard.BARREL, 1);
		assertTrue(watch.tick(empty, barrel, false, NOW, log));
		assertEquals(NOW, log.getLast());
		// Picking the vines changes nothing in the pack the watch counts.
		assertFalse(watch.tick(barrel, barrel, false, NOW + 600, log));
		// Handed in: five noted grapes, folded into grapes, with the barrel carried.
		Map<Integer, Long> paid = items(ItemID.COINS, 10, Vineyard.BARREL, 1, Vineyard.GRAPES, 5);
		assertTrue(watch.tick(barrel, paid, false, NOW + 120_000L, log));
		assertEquals(1, log.getBarrels());
		assertEquals(5, log.getGrapes());
		// The foreman taking the barrel back on the same tick still counts.
		Map<Integer, Long> taken = items(ItemID.COINS, 10, Vineyard.GRAPES, 10);
		assertTrue(watch.tick(paid, taken, false, NOW + 240_000L, log));
		assertEquals(2, log.getBarrels());
		// Grapes with no barrel carried, or with a bank or shop open, are something else.
		assertFalse(watch.tick(taken, items(ItemID.COINS, 10, Vineyard.GRAPES, 15), false, NOW + 300_000L, log));
		assertFalse(watch.tick(paid, items(ItemID.COINS, 10, Vineyard.BARREL, 1, Vineyard.GRAPES, 10), true, NOW + 360_000L, log));
		// A great many grapes at once is not a barrel.
		assertFalse(watch.tick(barrel, items(Vineyard.BARREL, 1, Vineyard.GRAPES, 500), false, NOW + 420_000L, log));
		assertFalse(watch.tick(null, barrel, false, NOW, log));
		assertEquals(2, log.getBarrels());
	}
}
