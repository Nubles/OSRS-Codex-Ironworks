package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The rocks as the wiki gives them, the player's mining log, and the rocks seen mined and come back. */
public class MinesTest
{
	private static final long T = 1_800_000_000_000L;
	private static final Rocks.Kind IRON = Rocks.byName("Iron rocks");
	private static final Rocks.Kind COAL = Rocks.byName("Coal rocks");
	private static final Rocks.Kind RUNITE = Rocks.byName("Runite rocks");

	@Test
	public void theRocksAreTheWikis()
	{
		// Level, experience an ore and respawn time in ticks, from each rock's wiki page.
		Object[][] facts = {
			{"Clay rocks", 1, 5.0, 2}, {"Copper rocks", 1, 17.5, 4}, {"Tin rocks", 1, 17.5, 4}, {"Iron rocks", 15, 35.0, 9},
			{"Silver rocks", 20, 40.0, 100}, {"Coal rocks", 30, 50.0, 50}, {"Gold rocks", 40, 65.0, 100}, {"Gem rocks", 40, 65.0, 99},
			{"Mithril rocks", 55, 80.0, 200}, {"Adamantite rocks", 70, 95.0, 400}, {"Runite rocks", 85, 125.0, 1200},
			{"Amethyst crystals", 92, 240.0, 125}};
		assertEquals(facts.length, Rocks.all().size());
		for (Object[] f : facts)
		{
			Rocks.Kind k = Rocks.byName((String) f[0]);
			assertEquals((String) f[0], (int) f[1], k.level);
			assertEquals((String) f[0], (double) f[2], k.xp, 1e-9);
			assertEquals((String) f[0], (int) f[3], k.respawnTicks);
		}
		assertSame(IRON, Rocks.byName("iron ROCKS"));
		assertSame(IRON, Rocks.byOre(ItemID.IRON_ORE));
		assertSame(COAL, Rocks.byStore("Coal"));
		assertNull(Rocks.byOre(ItemID.UNCUT_SAPPHIRE));
		assertNull(Rocks.byStore("Uncut gems"));
		assertEquals("Uncut sapphire", Rocks.gem(ItemID.UNCUT_SAPPHIRE));
		assertEquals("Clue geode (hard)", Rocks.clue(ItemID.MINING_CLUE_GEODE_HARD));
		assertEquals("Scroll box (easy)", Rocks.clue(ItemID.LEAGUE_CLUE_BOX_EASY));
		assertTrue(Rocks.isClue("Clue geode (beginner)"));
		assertEquals(ItemID.UNCUT_RUBY, Rocks.itemOf("Uncut ruby"));
		assertEquals("Amethyst", Rocks.byName("Amethyst crystals").shortName());
		// Where the wiki names a place it comes back at another speed, the rock says so.
		assertTrue(RUNITE.varies.contains("Wilderness"));
		assertNull(Rocks.byName("Gold rocks").varies);
	}

	@Test
	public void pickaxesAreKnownByTheirSwing()
	{
		assertEquals("Dragon pickaxe", Pickaxes.of(AnimationID.HUMAN_MINING_DRAGON_PICKAXE));
		assertEquals("Dragon pickaxe", Pickaxes.of(AnimationID.HUMAN_MINING_ZALCANO_PICKAXE));
		assertEquals("Rune pickaxe", Pickaxes.of(AnimationID.HUMAN_MINING_RUNE_PICKAXE_WALL));
		assertEquals("Infernal pickaxe", Pickaxes.of(AnimationID.PICKAXE_POWER_SWING_INFERNAL));
		assertEquals("Iron pickaxe", Pickaxes.of(AnimationID.HUMAN_MINING_IRON_PICKAXE));
		assertEquals("Crystal pickaxe", Pickaxes.of(AnimationID.HUMAN_MINING_CRYSTAL_PICKAXE_NOREACHFORWARD));
		assertNull(Pickaxes.of(-1));
		assertNull(Pickaxes.of(AnimationID.HUMAN_WOODCUTTING_RUNE_AXE));
		assertEquals(74, Pickaxes.all().size());
	}

	@Test
	public void oreIsTimedOreToOreByRockAndPickaxe()
	{
		Mines m = new Mines();
		for (int i = 0; i <= 100; i++)
		{
			m.mined(IRON, "Rune pickaxe", 1, T + i * 3_000L);
		}
		// A bank trip of five minutes is not timed.
		m.mined(IRON, "Rune pickaxe", 1, T + 300_000L + 300_000L);
		Mines.Seam s = m.seams().get("Iron rocks|Rune pickaxe");
		assertEquals(102, s.getOre());
		assertEquals(300_000L, s.getMs());
		assertEquals(102 * 12, s.perHour(), 1e-6);
		assertEquals(1224, m.perHour("Iron ore"), 1e-6);
		// Another pickaxe on the same rock is its own seam; the best pace is the rock's.
		m.mined(IRON, "Dragon pickaxe", 1, T + 1_000_000L);
		assertEquals(0, m.seams().get("Iron rocks|Dragon pickaxe").perHour(), 1e-9);
		assertEquals("Iron rocks|Dragon pickaxe", m.latest());
		assertSame(IRON, Mines.rockOf(m.latest()));
		assertEquals("Dragon pickaxe", Mines.pickaxeOf(m.latest()));
		assertEquals(103, m.ore());
	}

	@Test
	public void aRocksWaitCountsUpToACap()
	{
		Mines m = new Mines();
		// Runite: twelve minutes to come back, but only four minutes and two count.
		m.mined(RUNITE, "Dragon pickaxe", 1, T);
		m.mined(RUNITE, "Dragon pickaxe", 1, T + 5 * 60_000L);
		assertEquals(5 * 60_000L, m.seams().get("Runite rocks|Dragon pickaxe").getMs());
		m.mined(RUNITE, "Dragon pickaxe", 1, T + 12 * 60_000L);
		assertEquals(5 * 60_000L, m.seams().get("Runite rocks|Dragon pickaxe").getMs());
	}

	@Test
	public void rocksSeenComeBackTeachTheEstimate()
	{
		Mines m = new Mines();
		assertEquals(50, m.respawnTicks(COAL, 12850), 1e-9);
		assertFalse(m.learned(COAL, 12850));
		m.cameBack(COAL, 12850, 25);
		m.cameBack(COAL, 12850, 26);
		assertEquals(50, m.respawnTicks(COAL, 12850), 1e-9);
		m.cameBack(COAL, 12850, 24);
		assertTrue(m.learned(COAL, 12850));
		assertEquals(25, m.respawnTicks(COAL, 12850), 1e-9);
		// Another place keeps the wiki's figure.
		assertEquals(50, m.respawnTicks(COAL, 11828), 1e-9);
		// A rock back long past the wiki's time was out of sight in between.
		m.cameBack(COAL, 11828, 400);
		assertNull(m.comeback(COAL, 11828));
		assertEquals(3, m.comebacks(COAL).getSeen());
		assertEquals(3, m.comebacksSeen());
	}

	@Test
	public void depletedRocksAreTimedTickByTick()
	{
		Mines log = new Mines();
		RockTimers t = new RockTimers();
		// Tick 10: the iron rock at 3285,3363 is mined out; its depleted rock takes the tile.
		t.gone(IRON, 3285, 3363, 0, 13107);
		t.spawned(null, 3285, 3363, 0);
		t.settle(10, log);
		assertEquals(1, t.depleted().size());
		RockTimers.Depleted d = t.depleted().get(0);
		assertSame(IRON, d.kind);
		assertEquals(9, RockTimers.left(d, 10, log), 1e-9);
		assertEquals(4, RockTimers.left(d, 15, log), 1e-9);
		// Tick 19: it is back, after nine ticks.
		t.spawned(IRON, 3285, 3363, 0);
		t.settle(19, log);
		assertTrue(t.depleted().isEmpty());
		assertEquals(1, log.comebacks(IRON).getSeen());
		assertEquals(9, log.comebacks(IRON).average(), 1e-9);
	}

	@Test
	public void aSceneLoadIsNotRocksMinedOrBack()
	{
		Mines log = new Mines();
		RockTimers t = new RockTimers();
		t.gone(COAL, 3000, 3000, 0, 1);
		t.settle(5, log);
		assertEquals(1, t.depleted().size());
		// The scene loads: every rock leaves and comes again, the mined one with ore.
		t.loaded();
		t.gone(IRON, 3010, 3000, 0, 1);
		t.spawned(IRON, 3010, 3000, 0);
		t.gone(Rocks.byName("Tin rocks"), 3020, 3000, 0, 1);
		t.spawned(COAL, 3000, 3000, 0);
		t.settle(30, log);
		// Back, but not timed; and the rocks unloaded are not taken as mined.
		assertTrue(t.depleted().isEmpty());
		assertEquals(0, log.comebacksSeen());
		// A rock swapping for one of its own kind was not mined.
		t.gone(IRON, 3010, 3000, 0, 1);
		t.spawned(IRON, 3010, 3000, 0);
		t.settle(31, log);
		assertTrue(t.depleted().isEmpty());
	}

	@Test
	public void rocksFarAwayOrLongOverdueAreLetGo()
	{
		Mines log = new Mines();
		RockTimers t = new RockTimers();
		t.gone(IRON, 3000, 3000, 0, 1);
		t.gone(RUNITE, 3005, 3000, 0, 1);
		t.settle(0, log);
		t.expire(50, log, 3000, 3000, 0, 40);
		assertEquals(2, t.depleted().size());
		t.expire(101, log, 3000, 3000, 0, 40);
		assertEquals(1, t.depleted().size());
		assertSame(RUNITE, t.depleted().get(0).kind);
		t.expire(102, log, 3100, 3000, 0, 40);
		assertTrue(t.depleted().isEmpty());
	}

	@Test
	public void theLogSavesWithTheAccountAndOldSavesReadWithout()
	{
		Gson gson = new Gson();
		Works works = new Works();
		for (int i = 0; i <= 120; i++)
		{
			works.mines().mined(COAL, "Dragon pickaxe", 1, T + i * 4_000L);
		}
		works.mines().found("Uncut ruby", 1);
		works.mines().cameBack(COAL, 12850, 50);
		Works back = Works.read(gson, works.write(gson), 99).works;
		assertEquals(121, back.mines().mined());
		assertEquals(works.mines().perHour("Coal"), back.mines().perHour("Coal"), 1e-9);
		assertEquals(1L, (long) back.mines().found().get("Uncut ruby"));
		assertEquals(1, back.mines().comebacksSeen());
		assertTrue(Works.read(gson, "{\"account\":2}", 99).works.mines().isEmpty());
		Works damaged = Works.read(gson,
			"{\"mines\":{\"seams\":{\"Elder rocks|x\":{\"ore\":3},\"Coal rocks|\":{\"ore\":-4}},\"comebacks\":{\"Nope@1\":{\"seen\":2},\"Coal rocks@5\":{\"seen\":-1}}}}",
			99).works;
		assertTrue(damaged.mines().isEmpty());
		assertEquals(1, damaged.mines().seams().size());
		assertEquals(0, damaged.mines().comebacksSeen());
	}
}
