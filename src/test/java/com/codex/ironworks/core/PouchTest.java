package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * The OSRS Wiki's Essence pouch page: levels and capacities ("Small pouch 1 ... 3", medium 25 and 6,
 * large 50 and 9, giant 75 and 12, colossal "ranging from 8 at level 25 to 40 at level 85"), and its
 * "Times Degraded" table of what each pouch holds as its hidden counter climbs.
 */
public class PouchTest
{
	@Test
	public void capacitiesAndLevelsAreTheWikis()
	{
		assertEquals(3, Pouch.SMALL.capacity(0, 99));
		assertEquals(6, Pouch.MEDIUM.capacity(0, 99));
		assertEquals(9, Pouch.LARGE.capacity(0, 99));
		assertEquals(12, Pouch.GIANT.capacity(0, 99));
		assertEquals(40, Pouch.COLOSSAL.capacity(0, 99));
		assertEquals(1, Pouch.SMALL.getLevel());
		assertEquals(25, Pouch.MEDIUM.getLevel());
		assertEquals(50, Pouch.LARGE.getLevel());
		assertEquals(75, Pouch.GIANT.getLevel());
		assertEquals(25, Pouch.COLOSSAL.getLevel());
	}

	@Test
	public void theColossalPouchGrowsWithRunecraft()
	{
		// "starting from 27 for levels 75-84 ... 16 for levels 50-74, and ... 8 for levels 25-49".
		assertEquals(8, Pouch.COLOSSAL.capacity(0, 25));
		assertEquals(8, Pouch.COLOSSAL.capacity(0, 49));
		assertEquals(16, Pouch.COLOSSAL.capacity(0, 50));
		assertEquals(16, Pouch.COLOSSAL.capacity(0, 74));
		assertEquals(27, Pouch.COLOSSAL.capacity(0, 75));
		assertEquals(27, Pouch.COLOSSAL.capacity(0, 84));
		assertEquals(40, Pouch.COLOSSAL.capacity(0, 85));
		// Worn, it scales down from its 40 the same way: 35 of 40 at 85 is 23 of 27 at 75.
		assertEquals(35, Pouch.COLOSSAL.capacity(320, 85));
		assertEquals(23, Pouch.COLOSSAL.capacity(320, 75));
		assertEquals(0, Pouch.COLOSSAL.capacity(1020, 75));
	}

	@Test
	public void wornPouchesHoldWhatTheWikisTableSays()
	{
		int[][] medium = {{399, 6}, {400, 3}, {799, 3}, {800, 0}};
		int[][] large = {{399, 9}, {400, 7}, {599, 7}, {600, 5}, {800, 3}, {999, 3}, {1000, 0}};
		int[][] giant = {{199, 12}, {200, 9}, {300, 8}, {400, 7}, {600, 6}, {800, 5}, {1000, 3}, {1199, 3}, {1200, 0}};
		int[][] colossal = {{319, 40}, {320, 35}, {565, 30}, {745, 25}, {870, 20}, {950, 15}, {995, 10}, {1015, 5}, {1019, 5}, {1020, 0}};
		check(Pouch.MEDIUM, medium);
		check(Pouch.LARGE, large);
		check(Pouch.GIANT, giant);
		check(Pouch.COLOSSAL, colossal);
		// "While the small pouch does not degrade".
		assertFalse(Pouch.SMALL.wears());
		assertEquals(3, Pouch.SMALL.capacity(5000, 1));
		assertEquals(Integer.MAX_VALUE, Pouch.SMALL.safeEssence(0));
		assertFalse(Pouch.SMALL.atRisk(0, 99));
	}

	private static void check(Pouch p, int[][] rows)
	{
		for (int[] r : rows)
		{
			assertEquals(p + " at " + r[0], r[1], p.capacity(r[0], 99));
		}
	}

	@Test
	public void safeEssenceAssumesTheMostWearEachEssenceCanAdd()
	{
		// "a random value between 0 and 3" per essence: from 0, a giant pouch is sure of 66 essence before 200.
		assertEquals(66, Pouch.GIANT.safeEssence(0));
		assertEquals(5, Pouch.GIANT.safeFills(0, 99));
		assertEquals(133, Pouch.GIANT.expectedEssence(0));
		assertEquals(300, Pouch.GIANT.nextStep(200));
		assertEquals(8, Pouch.GIANT.capacityAfterNext(200, 99));
		// 12 essence could add 36: from 170 the next fill could pass 200.
		assertFalse(Pouch.GIANT.atRisk(163, 99));
		assertTrue(Pouch.GIANT.atRisk(170, 99));
		// The colossal pouch counts each essence once (unconfirmed: RuneLite's runecraft plugin).
		assertEquals(319, Pouch.COLOSSAL.safeEssence(0));
		assertTrue(Pouch.COLOSSAL.atRisk(290, 99));
		assertFalse(Pouch.COLOSSAL.atRisk(279, 99));
		// Past the last step there is nothing left to wear.
		assertEquals(-1, Pouch.MEDIUM.nextStep(800));
		assertEquals(0, Pouch.MEDIUM.safeFills(800, 99));
		assertFalse(Pouch.MEDIUM.atRisk(800, 99));
	}

	@Test
	public void itemsAreTheGamesPouchesNewAndDegraded()
	{
		assertEquals(Pouch.SMALL, Pouch.of(ItemID.RCU_POUCH_SMALL));
		assertEquals(Pouch.MEDIUM, Pouch.of(ItemID.RCU_POUCH_MEDIUM_DEGRADE));
		assertEquals(Pouch.LARGE, Pouch.of(ItemID.RCU_POUCH_LARGE));
		assertEquals(Pouch.GIANT, Pouch.of(ItemID.RCU_POUCH_GIANT_DEGRADE));
		assertEquals(Pouch.COLOSSAL, Pouch.of(ItemID.RCU_POUCH_COLOSSAL));
		assertTrue(Pouch.degraded(ItemID.RCU_POUCH_COLOSSAL_DEGRADE));
		assertFalse(Pouch.degraded(ItemID.RCU_POUCH_GIANT));
		assertNull(Pouch.of(ItemID.BH_RUNE_POUCH));
		assertNull(Pouch.of(-1));
	}
}
