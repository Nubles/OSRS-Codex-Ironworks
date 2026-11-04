package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The vineyard log's barrels, rounds and sittings, the wiki's facts it rests on, and grapes to a Cooking level. */
public class VineyardsTest
{
	private static final long T = 1_000_000_000L;

	@Test
	public void theFactsAreTheGamesAndTheWikis()
	{
		// RuneLite gameval: the foreman's barrel, grapes and a jug of wine.
		assertEquals(30037, Vineyard.BARREL);
		assertEquals(ItemID.ALDARIN_GRAPE_COLLECTION_BARREL, Vineyard.BARREL);
		assertEquals(1987, Vineyard.GRAPES);
		assertEquals(1993, Vineyard.WINE);
		// OSRS Wiki: Vineyard foreman, Jug of wine, Jug of bad wine.
		assertEquals(5, Vineyard.GRAPES_PER_BARREL);
		assertEquals(4, Vineyard.VINES_PER_BARREL);
		assertEquals(35, Vineyard.WINE_LEVEL);
		assertEquals(200, Vineyard.WINE_XP, 0);
		assertEquals(68, Vineyard.NO_FAIL_LEVEL);
		// The wine recipe the Make tab plans with agrees.
		Recipe wine = null;
		for (Recipe r : Recipes.all())
		{
			if ("Jug of wine".equals(r.product))
			{
				wine = r;
			}
		}
		assertNotNull(wine);
		assertEquals(Skill.COOKING, wine.skill);
		assertEquals(Vineyard.WINE_LEVEL, wine.level);
		assertEquals(Vineyard.WINE_XP, wine.xp, 0);
		assertEquals(Vineyard.STORE, wine.primary());
		assertEquals(Family.MATERIALS, Catalogue.family("Grapes"));
		assertEquals(Family.MATERIALS, Catalogue.family("Jug of water"));
	}

	@Test
	public void barrelsAreTimedBarrelToBarrelFromTheBarrelTaken()
	{
		Vineyards v = new Vineyards();
		assertTrue(v.isEmpty());
		v.given(T);
		assertFalse(v.isEmpty());
		// Two minutes to the first hand-in, then two and a half, three, and two.
		v.handedIn(5, T + 120_000L);
		v.handedIn(5, T + 270_000L);
		assertEquals(0, v.grapesPerHour(), 0);
		v.handedIn(5, T + 450_000L);
		assertEquals(3, v.getTimed());
		assertEquals(150_000L, v.roundMs());
		assertEquals(24, v.barrelsPerHour(), 1e-9);
		assertEquals(120, v.grapesPerHour(), 1e-9);
		v.handedIn(5, T + 570_000L);
		assertEquals(4, v.getBarrels());
		assertEquals(20, v.getGrapes());
		assertEquals(5, v.perBarrel(), 0);
		assertEquals(1, v.sittings().size());
		Vineyards.Sitting s = v.sittings().get(0);
		assertEquals(T, s.getStart());
		assertEquals(T + 570_000L, s.getEnd());
		assertEquals(4, s.getBarrels());
		assertEquals(570_000L, s.getMs());
		assertEquals(4 * 5 / (570_000 / 3_600_000.0), s.grapesPerHour(), 1e-6);
	}

	@Test
	public void aBreakIsNotTimedAndStartsANewSitting()
	{
		Vineyards v = new Vineyards();
		v.handedIn(5, T);
		v.handedIn(5, T + 100_000L);
		// Eleven minutes away: not a round.
		v.handedIn(5, T + 100_000L + 11 * 60_000L);
		assertEquals(1, v.getTimed());
		assertEquals(2, v.sittings().size());
		// A barrel taken after the break times the next round from when it was taken.
		v.given(T + 3_600_000L);
		v.handedIn(5, T + 3_600_000L + 90_000L);
		assertEquals(2, v.getTimed());
		assertEquals(3, v.sittings().size());
		assertEquals(T + 3_600_000L, v.sittings().get(2).getStart());
		// A barrel taken mid-sitting changes nothing.
		v.given(T + 3_600_000L + 100_000L);
		v.handedIn(5, T + 3_600_000L + 200_000L);
		assertEquals(3, v.getTimed());
		assertEquals(3, v.sittings().size());
		assertEquals(200_000L, v.sittings().get(2).getMs());
	}

	@Test
	public void sittingsAreCappedAndTheLogSurvivesASave()
	{
		Vineyards v = new Vineyards();
		for (int i = 0; i < Vineyards.SITTINGS + 5; i++)
		{
			v.handedIn(5, T + i * 3_600_000L);
		}
		assertEquals(Vineyards.SITTINGS, v.sittings().size());
		Gson gson = new Gson();
		Vineyards back = gson.fromJson(gson.toJson(v), Vineyards.class).repair();
		assertEquals(v.getBarrels(), back.getBarrels());
		assertEquals(v.getGrapes(), back.getGrapes());
		assertEquals(Vineyards.SITTINGS, back.sittings().size());
		// A damaged save is put right rather than trusted.
		Vineyards bad = gson.fromJson("{\"barrels\":-3,\"grapes\":10,\"timed\":9,\"sittings\":[null,{\"barrels\":0}]}", Vineyards.class).repair();
		assertEquals(0, bad.getBarrels());
		assertEquals(0, bad.getTimed());
		assertTrue(bad.sittings().isEmpty());
		// Nothing handed in counts nothing.
		Vineyards none = new Vineyards();
		none.handedIn(0, T);
		assertTrue(none.isEmpty());
	}

	@Test
	public void grapesToALevelAreJugsOfWineAtTwoHundred()
	{
		int xp35 = Experience.getXpForLevel(35);
		int xp36 = Experience.getXpForLevel(36);
		assertEquals((long) Math.ceil((xp36 - xp35) / 200.0), Vineyard.grapesTo(xp35, 36));
		assertEquals(0, Vineyard.grapesTo(xp36, 36));
		assertEquals(0, Vineyard.grapesTo(xp35, 100));
		assertEquals(36, Vineyard.Goal.NEXT_LEVEL.target(35));
		assertEquals(68, Vineyard.Goal.NO_FAIL.target(50));
		assertEquals(0, Vineyard.Goal.NO_FAIL.target(70));
		assertEquals(0, Vineyard.Goal.NEXT_LEVEL.target(99));
		assertEquals(0, Vineyard.Goal.NONE.target(40));
		assertEquals(99, Vineyard.Goal.MAX.target(40));
	}
}
