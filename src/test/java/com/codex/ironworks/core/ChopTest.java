package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The tree coach's count of a Forestry timer it cannot see. */
public class ChopTest
{
	private static final Trees.Kind OAK = Trees.byName("Oak tree");
	private static final Trees.Kind YEW = Trees.byName("Yew tree");

	@Test
	public void theTimerStartsAtTheFirstLogAndRunsWhileChopped()
	{
		Chop c = new Chop(OAK, false);
		c.tick(1, 0, false);
		c.tick(1, 0, false);
		assertFalse(c.isStarted());
		assertEquals(45, c.mostLeft());
		c.tick(1, 0, true);
		assertTrue(c.isStarted());
		assertTrue(c.whole());
		assertEquals(44, c.mostLeft());
		for (int i = 0; i < 10; i++)
		{
			c.tick(1, 0, false);
		}
		assertEquals(34, c.mostLeft());
		assertEquals(34 * 600, c.mostLeftMs());
		assertEquals(c.mostLeft(), c.leastLeft());
	}

	@Test
	public void moreChoppersDoNotRunItFaster()
	{
		Chop c = new Chop(YEW, false);
		c.tick(1, 0, true);
		c.tick(5, 4, false);
		c.tick(10, 9, false);
		assertEquals(190 - 3, c.mostLeft());
		assertEquals(10, c.getChoppers());
	}

	@Test
	public void itGrowsBackWhileNobodyChopsAndStartsAfresh()
	{
		Chop c = new Chop(OAK, false);
		c.tick(1, 0, true);
		for (int i = 0; i < 4; i++)
		{
			c.tick(1, 0, false);
		}
		assertEquals(40, c.mostLeft());
		c.tick(0, 0, false);
		c.tick(0, 0, false);
		assertEquals(42, c.mostLeft());
		c.tick(0, 0, false);
		c.tick(0, 0, false);
		c.tick(0, 0, false);
		assertFalse(c.isStarted());
		assertEquals(45, c.mostLeft());
	}

	@Test
	public void othersAtTheTreeFirstMakeARangeThatRegrowthCloses()
	{
		Chop c = new Chop(OAK, false);
		c.tick(3, 3, false);
		assertTrue(c.isStarted());
		assertFalse(c.whole());
		assertEquals(44, c.mostLeft());
		assertEquals(0, c.leastLeft());
		// Nobody chops for 46 ticks: the one seen, then all that could have gone unseen, grow back.
		for (int i = 0; i < 46; i++)
		{
			c.tick(0, 0, false);
		}
		assertFalse(c.isStarted());
		c.tick(1, 0, true);
		assertTrue(c.whole());
	}

	@Test
	public void aRangeNarrowsAsItRegrows()
	{
		Chop c = new Chop(YEW, false);
		c.tick(2, 2, false);
		c.tick(2, 2, false);
		for (int i = 0; i < 52; i++)
		{
			c.tick(0, 0, false);
		}
		assertEquals(190, c.mostLeft());
		assertEquals(50, c.leastLeft());
	}

	@Test
	public void farmedAndRegularTreesKeepNoTimerAndAFallIsTimedToRegrowth()
	{
		Chop farmed = new Chop(YEW, true);
		farmed.tick(1, 0, true);
		assertFalse(farmed.isStarted());
		Chop tree = new Chop(Trees.byName("Tree"), false);
		tree.tick(1, 0, true);
		assertFalse(tree.isStarted());
		Chop c = new Chop(OAK, false);
		c.tick(1, 0, true);
		assertEquals(-1, c.regrowLeft());
		c.fell();
		assertTrue(c.isFelled());
		assertEquals(14, c.regrowLeft());
		c.tick(0, 0, false);
		c.tick(0, 0, false);
		assertEquals(12, c.regrowLeft());
	}
}
