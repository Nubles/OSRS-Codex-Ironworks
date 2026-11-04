package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Spot;
import com.codex.ironworks.core.Starlog;
import com.codex.ironworks.core.Stars;
import com.codex.ironworks.core.Travel;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.ui.View;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/** The stars card, the star by the player and its layer, and what the watch takes as mined dust. */
public class StarsViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;
	private static final int SX = 3030;
	private static final int SY = 3348;

	private static Map<Skill, Integer> levels(int mining)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.MINING, mining);
		return m;
	}

	private static View view(Works w, Map<Skill, Integer> levels)
	{
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		levels.forEach((s, l) -> xp.put(s, Experience.getXpForLevel(l)));
		return Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1, levels, xp, Altar.GILDED, id -> 0);
	}

	/** One dust every so many seconds for ten minutes on a star of one size. */
	private static void mine(Works w, int size, int every, double xp, long from)
	{
		Starlog.Find f = w.stars().found(420, SX, SY, 0, size, from);
		for (int i = 0; i <= 600 / every; i++)
		{
			w.stars().mined(f, size, 1, xp, from + i * every * 1000L);
		}
	}

	@Test
	public void noCardUntilAStarIsFound()
	{
		assertNull(view(new Works(), levels(80)).stars);
	}

	@Test
	public void theCardShowsDustAndExperienceAnHourBySizeAndTheBestOpenAtTheLevel()
	{
		Works w = new Works();
		mine(w, 8, 15, 32, NOW - 3_000_000L);
		mine(w, 6, 12, 32, NOW - 1_000_000L);
		View.StarView v = view(w, levels(85)).stars;
		assertNotNull(v);
		assertEquals(41 + 51, v.dust);
		assertEquals(1, v.found);
		assertEquals(6, v.size);
		assertEquals(300, v.perHour, 1e-6);
		assertEquals(300 * 32, v.xpPerHour, 1e-6);
		assertEquals(2, v.tiers.size());
		assertEquals(8, v.tiers.get(0).size);
		assertEquals(6, v.best.size);
		assertEquals(8, v.largest);
		// At 79 a size-8 layer is closed, so it is never the best, whatever its pace.
		mine(w, 8, 5, 32, NOW - 5_000_000L);
		assertEquals(8, view(w, levels(85)).stars.best.size);
		assertEquals(6, view(w, levels(79)).stars.best.size);
	}

	@Test
	public void stardustTowardTheChosenShopItemIsTimedAtThePlayersPace()
	{
		Works w = new Works();
		mine(w, 7, 15, 32, NOW - 1_000_000L);
		w.hold(Spot.BANK, Collections.singletonMap(Stars.STARDUST, 1500L), Collections.singletonMap(Stars.STARDUST, "Stardust"), NOW);
		w.hold(Spot.INVENTORY, Collections.singletonMap(Stars.STARDUST, 20L), Collections.singletonMap(Stars.STARDUST, "Stardust"), NOW);
		View.StarView v = view(w, levels(80)).stars;
		Views.starGoal(v, Stars.Reward.NONE, w);
		assertNull(v.goal);
		Views.starGoal(v, Stars.Reward.CELESTIAL_RING, w);
		assertEquals("Celestial ring (uncharged)", v.goal);
		assertEquals(2000, v.goalPrice);
		assertEquals(1520, v.held);
		// 480 short at 240 an hour.
		assertEquals(2, v.goalHours, 1e-9);
	}

	@Test
	public void aStarIsPlacedByTheNearestTeleportTheAccountOwnsElseByItsTile()
	{
		assertEquals("tile 3030, 3348", Views.where(SX, SY, 0, null));
		String atlas = "{\"version\":1,\"teleports\":[{\"name\":\"Falador teleport\",\"x\":2965,\"y\":3379,\"plane\":0,\"seconds\":4},"
			+ "{\"name\":\"Varrock teleport\",\"x\":3213,\"y\":3424,\"plane\":0,\"seconds\":4}]}";
		Travel t = Travel.read(new Gson(), atlas);
		assertNotNull(t);
		assertEquals("near Falador teleport", Views.where(SX, SY, 0, t));
		assertEquals("tile 1000, 1000", Views.where(1000, 1000, 0, t));
	}

	@Test
	public void dustBesideAStarIsMinedFromItAndALayerSeenWholeIsTimed()
	{
		StarWatch watch = new StarWatch();
		Starlog log = new Starlog();
		watch.spawned(Stars.object(8), SX, SY, 0, log);
		// Dust with nobody's star nearby is not counted.
		watch.xp(Skill.MINING, 32);
		assertFalse(watch.tick(NOW, 420, SX + 10, SY, 0, 1, log));
		assertTrue(log.isEmpty());
		watch.xp(Skill.MINING, 32);
		assertTrue(watch.tick(NOW + 600, 420, SX + 1, SY, 0, 2, log));
		assertEquals(2, log.dust());
		assertEquals(1, log.bonus());
		assertEquals(8, log.finds().get(0).getSize());
		assertEquals(420, log.finds().get(0).getWorld());
		// Its first layer was not seen from the start: no time left without a prospect.
		StarWatch.Now n = watch.now();
		assertNotNull(n);
		assertEquals(8, n.size);
		assertEquals(80, n.level);
		assertEquals(-1, n.leftTicks, 0);
		// The layer goes: the size-8 star is swapped for a size-7 one.
		watch.despawned(Stars.object(8), SX, SY, 0);
		watch.spawned(Stars.object(7), SX, SY, 0, log);
		assertEquals(0, log.getLayers());
		for (int i = 0; i < 100; i++)
		{
			watch.tick(NOW + 1200 + i * 600L, 420, SX + 1, SY, 0, i % 10 == 0 ? 1 : 0, log);
		}
		n = watch.now();
		assertEquals(7, n.size);
		assertEquals(600, n.leftTicks, 1e-9);
		assertEquals(100 / 700.0, n.mined, 1e-9);
		// The next swap closes a layer seen whole.
		watch.spawned(Stars.object(6), SX, SY, 0, log);
		watch.despawned(Stars.object(7), SX, SY, 0);
		assertEquals(1, log.getLayers());
		assertEquals(100, log.averageLayer(), 1e-9);
		assertEquals(7, log.finds().get(0).getLowest());
		assertEquals(1, log.finds().size());
	}

	@Test
	public void prospectingGivesTheTimeLeftAndTheGamesLinesAreKept()
	{
		StarWatch watch = new StarWatch();
		Starlog log = new Starlog();
		watch.spawned(Stars.object(9), SX, SY, 0, log);
		watch.heard("Congratulations! You were the first person to find this star!");
		watch.heard("This is a size-9 star. A Mining level of at least 90 is required to mine this layer.");
		watch.heard("It has been mined 50% of the way to the next layer.");
		assertTrue(watch.tick(NOW, 301, SX + 1, SY + 1, 0, 0, log));
		Starlog.Find f = log.finds().get(0);
		assertTrue(f.isDiscovered());
		assertEquals(9, f.getSize());
		assertEquals(0, f.getDust());
		StarWatch.Now n = watch.now();
		assertEquals(350, n.leftTicks, 1e-9);
		assertEquals(50, n.prospected);
		watch.tick(NOW + 600, 301, SX + 1, SY + 1, 0, 0, log);
		assertEquals(349, watch.now().leftTicks, 1e-9);
		assertEquals(1, watch.now().prospectAgo);
		// The star crumbles: the object goes as the game says so.
		watch.despawned(Stars.object(9), SX, SY, 0);
		watch.heard("The star disintegrates into dust.");
		watch.tick(NOW + 1200, 301, SX + 1, SY + 1, 0, 0, log);
		assertTrue(f.isGone());
		assertNull(watch.now());
		for (int i = 0; i < 5; i++)
		{
			watch.tick(NOW + 1800 + i * 600L, 301, SX + 1, SY + 1, 0, 0, log);
		}
		// Gone from the scene; dust now is not from it.
		assertFalse(watch.tick(NOW + 9000, 301, SX + 1, SY + 1, 0, 1, log));
	}

	@Test
	public void theStarPlaqueSaysItsAnEstimate()
	{
		StarWatch watch = new StarWatch();
		Starlog log = new Starlog();
		watch.spawned(Stars.object(7), SX, SY, 0, log);
		watch.heard("This is a size-7 star. A Mining level of at least 70 is required to mine this layer. It has been mined 80% of the way to the next layer.");
		watch.tick(NOW, 420, SX, SY + 1, 0, 0, log);
		com.codex.ironworks.overlay.MineCoach.Shown s = StarCoach.describe(watch.now(), null);
		assertEquals("Size 7 · 70", s.title);
		assertEquals("~1:24", s.figure);
		assertEquals("layer left, est., wiki 7 min", s.note);
		StarWatch fresh = new StarWatch();
		fresh.spawned(Stars.object(7), SX, SY, 0, log);
		fresh.tick(NOW, 420, SX, SY + 1, 0, 1, log);
		assertEquals("Prospect it for time left", StarCoach.describe(fresh.now(), null).note);
	}
}
