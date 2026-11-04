package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Fish;
import com.codex.ironworks.core.Fishing;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.overlay.MineCoach;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;

/** The fishing card, fish paces fed into the ways to get more, what arrives while fishing, and the spot plaque. */
public class FishingViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;
	private static final Fish.Kind LOBSTER = Fish.byStore("Raw lobster");
	private static final Fish.Kind TUNA = Fish.byStore("Raw tuna");
	private static final Fish.Kind SWORD = Fish.byStore("Raw swordfish");

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

	/** A fish every few seconds for ten minutes. */
	private static void fish(Works w, Fish.Kind k, String tool, int every, long from)
	{
		for (int i = 0; i <= 600 / every; i++)
		{
			w.fishing().caught(k, tool, 1, from + i * every * 1000L);
		}
	}

	@Test
	public void noCardUntilSomethingIsCaught()
	{
		assertNull(view(new Works(), levels(60)).fishing);
	}

	@Test
	public void theCardShowsFishAndExperienceAnHourAndTheBestWay()
	{
		Works w = new Works();
		fish(w, LOBSTER, "Lobster pot", 10, NOW - 2_000_000L);
		fish(w, TUNA, "Harpoon", 6, NOW - 1_000_000L);
		View.FishView v = view(w, levels(45)).fishing;
		assertNotNull(v);
		assertEquals(61 + 101, v.fish);
		assertEquals("Harpoon", v.method);
		assertEquals("Harpoon", v.tool);
		assertEquals("Raw tuna", v.fishName);
		assertEquals(ItemID.RAW_TUNA, v.icon);
		assertEquals(606, v.perHour, 1e-6);
		assertEquals(606 * 80, v.xpPerHour, 1e-6);
		assertEquals(2, v.casts.size());
		// Tuna at 606 an hour is 48k xp; lobster at 366 is 33k.
		assertEquals("Harpoon", v.best.method);
		// Lobster's 90 xp a fish is the most a way open at 45 gives (bass needs 46), and it is tried.
		assertNull(v.untried);
		v = view(w, levels(50)).fishing;
		// At 50 a big net's bass gives 100 xp a fish, as much as swordfish, and the big net is untried.
		assertEquals("Big net", v.untried);
		assertEquals("Raw bass", v.untriedFish);
		v = view(w, levels(80)).fishing;
		assertEquals("Shark harpoon", v.untried);
		assertEquals("Raw shark", v.untriedFish);
		assertEquals(76, v.untriedLevel);
		assertEquals(110, v.untriedXp, 1e-9);
	}

	@Test
	public void aWayAboveTheLevelIsNeverTheBest()
	{
		Works w = new Works();
		fish(w, LOBSTER, "Lobster pot", 10, NOW - 2_000_000L);
		fish(w, SWORD, "Harpoon", 5, NOW - 1_000_000L);
		// The harpoon opens at 35 for tuna, so it counts at 45 even with swordfish caught boosted.
		assertEquals("Harpoon", view(w, levels(45)).fishing.best.method);
		// Below 35, only what the level opens.
		assertNull(view(w, levels(30)).fishing.best);
	}

	@Test
	public void aerialFishingIsOnlySuggestedWithTheHunterLevel()
	{
		Works w = new Works();
		fish(w, Fish.byStore("Raw shark"), "Dragon harpoon", 20, NOW - 1_000_000L);
		Map<Skill, Integer> l = levels(95);
		l.put(Skill.HUNTER, 80);
		// Shark gives more a fish than any aerial fish, and is tried: nothing untried to suggest.
		assertNull(view(w, l).fishing.untried);
	}

	@Test
	public void fishThePlansAreShortOfAreTimedAtThePlayersPace()
	{
		Works w = new Works();
		fish(w, LOBSTER, "Lobster pot", 10, NOW - 1_000_000L);
		View.Want lobster = new View.Want();
		lobster.store = "Raw lobster";
		lobster.need = 732;
		View.Want shark = new View.Want();
		shark.store = "Raw shark";
		shark.need = 50;
		View.Want haddock = new View.Want();
		haddock.store = "Raw haddock";
		haddock.need = 50;
		View.FishView v = Views.fishing(w, levels(60), Arrays.asList(lobster, shark, haddock));
		assertEquals(2, v.plans.size());
		View.CatchPlan c = v.plans.get(0);
		assertEquals("Cage", c.method);
		assertEquals(366, c.perHour, 1e-6);
		assertEquals(2, c.hours, 1e-6);
		assertEquals(0, c.needs);
		View.CatchPlan s = v.plans.get(1);
		assertEquals(76, s.needs);
		assertEquals(0, s.perHour, 1e-9);
		// The same pace times fishing wherever Ironworks ranks ways to get lobsters.
		assertEquals(366, Views.pace(w, "Raw lobster", Source.GATHERED), 1e-6);
		// Trawled fish keep the trawling card's pace, not this one.
		assertEquals(0, Views.pace(w, "Raw haddock", Source.GATHERED), 1e-9);
	}

	@Test
	public void whatArrivesWhileFishingIsLogged()
	{
		Fishing log = new Fishing();
		Map<Integer, Long> before = new HashMap<>();
		before.put(ItemID.RAW_SWORDFISH, 3L);
		Map<Integer, Long> after = new HashMap<>(before);
		after.put(ItemID.RAW_SWORDFISH, 4L);
		after.put(ItemID.FISHING_CLUE_BOTTLE_HARD, 1L);
		after.put(ItemID.POH_TROPHYDROP_SWORDFISH, 1L);
		after.put(ItemID.IRON_ORE, 1L);
		Fish.Tool harpoon = Fish.tool(AnimationID.HUMAN_HARPOON_DRAGON);
		FishWatch.Result r = FishWatch.gains(before, after, true, false, false, harpoon, Collections.emptyList(), NOW, log);
		assertTrue(r.changed);
		assertEquals(1, r.fish);
		assertEquals(1, log.spots().get("Harpoon|Dragon harpoon").count());
		assertEquals(1L, (long) log.found().get("Clue bottle (hard)"));
		assertEquals(1L, (long) log.found().get("Big swordfish"));
		assertEquals(2, log.found().size());
		// Not fishing: a lobster taken from a stall or a casket bought is not a catch.
		Fishing idle = new Fishing();
		Map<Integer, Long> got = new HashMap<>();
		got.put(ItemID.RAW_LOBSTER, 1L);
		got.put(ItemID.CASKET, 1L);
		assertFalse(FishWatch.gains(Collections.emptyMap(), got, false, false, false, null, Collections.emptyList(), NOW, idle).changed);
		assertTrue(idle.isEmpty());
		// Aerial fish count without a fishing animation, and the pearl that came with them is theirs.
		Map<Integer, Long> aerial = new HashMap<>();
		aerial.put(ItemID.AERIAL_FISHING_MOTTLED_EEL, 1L);
		aerial.put(ItemID.AERIAL_FISHING_PEARL, 1L);
		r = FishWatch.gains(Collections.emptyMap(), aerial, false, false, false, null, Collections.emptyList(), NOW, idle);
		assertTrue(r.aerial);
		assertEquals(1, idle.spots().get("Aerial|Cormorant").count());
		assertEquals(1L, (long) idle.found().get("Molch pearl"));
		// Into an open fish barrel: nothing reaches the pack, so the catch message names the fish.
		Fishing barrel = new Fishing();
		Map<Integer, Long> carry = new HashMap<>();
		carry.put(ItemID.FISH_BARREL_OPEN, 1L);
		r = FishWatch.gains(carry, carry, true, false, true, harpoon, Collections.singletonList(TUNA), NOW, barrel);
		assertEquals(1, r.fish);
		assertEquals(1L, (long) barrel.spots().get("Harpoon|Dragon harpoon").getFish().get("Raw tuna"));
		// Without Fishing experience on the tick, a message alone is not a catch.
		Fishing quiet = new Fishing();
		FishWatch.gains(carry, carry, true, false, false, harpoon, Collections.singletonList(TUNA), NOW, quiet);
		assertTrue(quiet.isEmpty());
	}

	@Test
	public void theCardShowsWhatTurnedUpAndPearlLuck()
	{
		Works w = new Works();
		fish(w, Fish.byStore("Raw bass"), "Big fishing net", 3, NOW - 1_000_000L);
		for (int i = 0; i <= 600; i++)
		{
			w.fishing().caught(Fish.byStore("Common tench"), "Cormorant", 1, NOW - 600_000L + i * 1_000L);
		}
		w.fishing().found("Casket", 4);
		w.fishing().found("Clue bottle (medium)", 1);
		w.fishing().found(Fish.PEARL_NAME, 10);
		View.FishView v = view(w, levels(70)).fishing;
		assertEquals(1, v.clues);
		assertEquals("Molch pearl", v.found.get(0).name);
		assertTrue(v.found.get(v.found.size() - 1).clue);
		assertEquals(1, v.luck.size());
		assertEquals("Big bass", v.luck.get(0).name);
		assertEquals(0, v.luck.get(0).seen);
		assertEquals(201 / 1000.0, v.luck.get(0).expected, 1e-9);
		assertEquals(601, v.aerialCatches);
		assertEquals(10, v.pearls);
		assertEquals(60, v.pearlsPerHour, 1e-9);
		Views.pearlGoal(v, Fish.PearlGoal.PEARL_FISHING_ROD, w);
		assertEquals("Pearl fishing rod", v.pearlGoal);
		assertEquals(100, v.pearlGoalPrice);
		// None held yet: 100 pearls at 60 an hour.
		assertEquals(100 / 60.0, v.pearlGoalHours, 1e-9);
		Views.pearlGoal(v, Fish.PearlGoal.NONE, w);
	}

	@Test
	public void spotsAreKnownByRuneLitesListAndBoatsNeverMove()
	{
		assertEquals(Fish.Stay.MOST, FishWatch.stay(NpcID._0_44_53_RAREFISH));
		assertEquals("Cage/Harpoon", FishWatch.label(NpcID._0_44_53_RAREFISH));
		assertEquals(Fish.Stay.ANGLERFISH, FishWatch.stay(NpcID.PISCARILIUSFISH));
		assertEquals(Fish.Stay.EELS, FishWatch.stay(NpcID.SNAKEBOSS_FISHINGSPOT));
		assertEquals(Fish.Stay.NEVER, FishWatch.stay(NpcID.FISHING_BOAT_PISCARILIUSFISH));
		assertEquals("Boat spot", FishWatch.label(NpcID.FISHING_BOAT_PISCARILIUSFISH));
		// Aerial spots: the wiki gives no stay for them here.
		assertNull(FishWatch.stay(NpcID.FISHING_SPOT_AERIAL));
	}

	@Test
	public void thePlaqueSaysWhenTheSpotMayMove()
	{
		FishWatch.Seen s = new FishWatch.Seen("Cage/Harpoon", Fish.Stay.MOST, 0, 0, 0, 100, true);
		MineCoach.Shown sh = FishWatch.describe(s, 150, null);
		assertEquals("Cage/Harpoon", sh.title);
		assertEquals("~2:00-4:48", sh.figure);
		assertEquals("until it moves, est., wiki", sh.note);
		sh = FishWatch.describe(s, 400, null);
		assertEquals("by ~2:18", sh.figure);
		assertEquals("may move any moment, est.", sh.note);
		sh = FishWatch.describe(s, 630, null);
		assertEquals("Due", sh.figure);
		// Arrival unseen: only the longest stay bounds it.
		FishWatch.Seen u = new FishWatch.Seen("Net/Bait", Fish.Stay.MOST, 0, 0, 0, 100, false);
		sh = FishWatch.describe(u, 200, null);
		assertEquals("by ~4:18", sh.figure);
		assertEquals("here 1:00+, est., wiki", sh.note);
		FishWatch.Seen b = new FishWatch.Seen("Boat spot", Fish.Stay.NEVER, 0, 0, 0, 100, true);
		assertEquals("Stays", FishWatch.describe(b, 9000, null).figure);
	}

	@Test
	public void theCardShowsSpotsSeenMove()
	{
		Works w = new Works();
		w.fishing().caught(LOBSTER, null, 1, NOW);
		w.fishing().stayed("Cage/Harpoon", 300);
		w.fishing().stayed("Cage/Harpoon", 400);
		w.fishing().stayed("Anglerfish", 100);
		View.FishView v = view(w, levels(60)).fishing;
		assertEquals(2, v.stays.size());
		assertEquals("Cage/Harpoon", v.stays.get(0).spot);
		assertEquals(210, v.stays.get(0).seconds, 1e-9);
		assertEquals(150, v.stays.get(0).wikiMinSeconds, 1e-9);
		assertEquals(318, v.stays.get(0).wikiMaxSeconds, 1e-9);
		assertEquals(498, v.stays.get(1).wikiMaxSeconds, 1e-9);
		assertNull(v.tool);
	}
}
