package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Mines;
import com.codex.ironworks.core.RockTimers;
import com.codex.ironworks.core.Rocks;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.overlay.MineCoach;
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

/** The mining card, ore paces fed into the ways to get more, what arrives while mining, and the respawn plaques. */
public class MiningViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;
	private static final Rocks.Kind IRON = Rocks.byName("Iron rocks");
	private static final Rocks.Kind COAL = Rocks.byName("Coal rocks");

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

	/** Ore every few seconds for ten minutes. */
	private static void mine(Works w, Rocks.Kind k, String pickaxe, int every, long from)
	{
		for (int i = 0; i <= 600 / every; i++)
		{
			w.mines().mined(k, pickaxe, 1, from + i * every * 1000L);
		}
	}

	@Test
	public void noCardUntilSomethingIsMined()
	{
		assertNull(view(new Works(), levels(60)).mining);
	}

	@Test
	public void theCardShowsOreAndExperienceAnHourAndTheBestRock()
	{
		Works w = new Works();
		mine(w, COAL, "Rune pickaxe", 6, NOW - 2_000_000L);
		mine(w, IRON, "Rune pickaxe", 3, NOW - 1_000_000L);
		View.MineView v = view(w, levels(45)).mining;
		assertNotNull(v);
		assertEquals(101 + 201, v.ore);
		assertEquals("Iron rocks", v.rock);
		assertEquals("Rune pickaxe", v.pickaxe);
		assertEquals("Iron ore", v.oreName);
		assertEquals(ItemID.IRON_ORE, v.icon);
		assertEquals(1206, v.perHour, 1e-6);
		assertEquals(1206 * 35, v.xpPerHour, 1e-6);
		assertEquals(2, v.seams.size());
		// Iron at 1,206 an hour is 42k xp; coal at 606 is 30k.
		assertEquals("Iron rocks", v.best.rock);
		// Gold and gem rocks give more for each ore at 45, and are untried.
		assertEquals("Gold rocks", v.untried);
		assertEquals(40, v.untriedLevel);
		assertEquals(65, v.untriedXp, 1e-9);
	}

	@Test
	public void aRockAboveTheLevelIsNeverTheBest()
	{
		Works w = new Works();
		mine(w, IRON, "Rune pickaxe", 3, NOW - 2_000_000L);
		mine(w, Rocks.byName("Mithril rocks"), "Rune pickaxe", 2, NOW - 1_000_000L);
		// Boosted to mine mithril at 52: the best open at 52 is iron, even though mithril paid more.
		View.MineView v = view(w, levels(52)).mining;
		assertEquals("Iron rocks", v.best.rock);
	}

	@Test
	public void oreThePlansAreShortOfIsTimedAtThePlayersPace()
	{
		Works w = new Works();
		mine(w, COAL, "Dragon pickaxe", 6, NOW - 1_000_000L);
		View.Want coal = new View.Want();
		coal.store = "Coal";
		coal.need = 1212;
		View.Want rune = new View.Want();
		rune.store = "Runite ore";
		rune.need = 20;
		View.Want logs = new View.Want();
		logs.store = "Yew logs";
		logs.need = 100;
		View.MineView v = Views.mining(w, levels(60), java.util.Arrays.asList(coal, rune, logs));
		assertEquals(2, v.plans.size());
		View.OrePlan c = v.plans.get(0);
		assertEquals("Coal rocks", c.rock);
		assertEquals(606, c.perHour, 1e-6);
		assertEquals(2, c.hours, 1e-6);
		assertEquals(0, c.needs);
		View.OrePlan r = v.plans.get(1);
		assertEquals(85, r.needs);
		assertEquals(0, r.perHour, 1e-9);
		// The same pace times mining wherever Ironworks ranks ways to get coal.
		assertEquals(606, Views.pace(w, "Coal", Source.GATHERED), 1e-6);
	}

	@Test
	public void whatArrivesWhileMiningIsLogged()
	{
		Mines log = new Mines();
		Map<Integer, Long> before = new HashMap<>();
		before.put(ItemID.IRON_ORE, 3L);
		Map<Integer, Long> after = new HashMap<>(before);
		after.put(ItemID.IRON_ORE, 4L);
		after.put(ItemID.UNCUT_EMERALD, 1L);
		after.put(ItemID.MINING_CLUE_GEODE_MEDIUM, 1L);
		after.put(ItemID.LOBSTER, 1L);
		assertTrue(RockWatch.gains(before, after, IRON, "Rune pickaxe", NOW, log));
		assertEquals(1, log.seams().get("Iron rocks|Rune pickaxe").getOre());
		assertEquals(1L, (long) log.found().get("Uncut emerald"));
		assertEquals(1L, (long) log.found().get("Clue geode (medium)"));
		assertEquals(2, log.found().size());
		// At gem rocks a gem is the ore, and still counted as found.
		Map<Integer, Long> gem = new HashMap<>();
		gem.put(ItemID.UNCUT_OPAL, 1L);
		assertTrue(RockWatch.gains(Collections.emptyMap(), gem, Rocks.GEM_ROCKS, "Rune pickaxe", NOW, log));
		assertEquals(1, log.seams().get("Gem rocks|Rune pickaxe").getOre());
		assertEquals(1, log.ore());
		// Nothing new: nothing logged.
		assertFalse(RockWatch.gains(after, after, IRON, "Rune pickaxe", NOW, log));
		Works w = new Works();
		w.mines().mined(IRON, "Rune pickaxe", 300, NOW);
		w.mines().found("Uncut sapphire", 2);
		w.mines().found("Uncut ruby", 1);
		w.mines().found("Scroll box (hard)", 1);
		View.MineView v = view(w, levels(60)).mining;
		assertEquals(3, v.gems);
		assertEquals(1, v.clues);
		assertEquals("Uncut sapphire", v.found.get(0).name);
		assertTrue(v.found.get(2).clue);
		assertEquals(100, v.orePerGem, 1e-9);
	}

	@Test
	public void thePlaqueSaysWhenARockShouldBeBackAndWhose()
	{
		Mines log = new Mines();
		RockTimers t = new RockTimers();
		t.gone(COAL, 3000, 3000, 0, 12850);
		t.settle(100, log);
		RockTimers.Depleted d = t.depleted().get(0);
		MineCoach.Shown s = MineCoach.describe(d, 100, log, null);
		assertEquals("Coal", s.title);
		assertEquals("~0:30", s.figure);
		assertEquals("est., wiki; varies", s.note);
		s = MineCoach.describe(d, 125, log, null);
		assertEquals("~0:15", s.figure);
		assertEquals(0.5, s.left, 1e-9);
		s = MineCoach.describe(d, 151, log, null);
		assertEquals("Due", s.figure);
		assertEquals("past the wiki's time", s.note);
		for (int i = 0; i < 3; i++)
		{
			log.cameBack(COAL, 12850, 40);
		}
		s = MineCoach.describe(d, 100, log, null);
		assertEquals("~0:24", s.figure);
		assertEquals("est., yours here", s.note);
		t.gone(Rocks.byName("Gold rocks"), 3001, 3000, 0, 12850);
		t.settle(100, log);
		assertEquals("est., wiki", MineCoach.describe(t.depleted().get(1), 100, log, null).note);
	}

	@Test
	public void theCardShowsRocksSeenComeBack()
	{
		Works w = new Works();
		w.mines().mined(COAL, null, 1, NOW);
		w.mines().cameBack(COAL, 12850, 48);
		w.mines().cameBack(COAL, 12850, 52);
		View.MineView v = view(w, levels(60)).mining;
		assertEquals(1, v.comebacks.size());
		assertEquals(30, v.comebacks.get(0).seconds, 1e-9);
		assertEquals(30, v.comebacks.get(0).wikiSeconds, 1e-9);
		assertNull(v.pickaxe);
	}
}
