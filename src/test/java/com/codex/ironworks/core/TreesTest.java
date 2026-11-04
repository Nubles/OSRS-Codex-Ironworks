package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/** Every tree fact against the OSRS Wiki, as quoted in the pull request. */
public class TreesTest
{
	private static void fact(String name, String logs, int item, int level, double xp, int fall, int respawn)
	{
		Trees.Kind k = Trees.byName(name);
		assertTrue(name, k != null);
		assertSame(k, Trees.byLogs(logs));
		assertSame(k, Trees.of(name));
		assertEquals(name, item, k.logItem);
		assertEquals(name, level, k.level);
		assertEquals(name, xp, k.xp, 1e-9);
		assertEquals(name, fall, k.fallTicks);
		assertEquals(name, respawn, k.respawnTicks);
	}

	@Test
	public void everyTreeIsAsTheWikiGivesIt()
	{
		// Woodcutting: level and experience. Each tree's page: "will fall after N ticks ... of chopping".
		fact("Tree", "Logs", 1511, 1, 25, 0, 0);
		fact("Oak tree", "Oak logs", 1521, 15, 37.5, 45, 14);
		fact("Willow tree", "Willow logs", 1519, 30, 67.5, 50, 14);
		fact("Teak tree", "Teak logs", 6333, 35, 85, 50, 15);
		fact("Maple tree", "Maple logs", 1517, 45, 100, 100, 59);
		fact("Mahogany tree", "Mahogany logs", 6332, 50, 125, 100, 14);
		fact("Arctic pine tree", "Arctic pine logs", 10810, 54, 40, 140, 14);
		fact("Yew tree", "Yew logs", 1515, 60, 175, 190, 99);
		fact("Camphor tree", "Camphor logs", 32904, 66, 143.5, 200, 99);
		fact("Magic tree", "Magic logs", 1513, 75, 250, 390, 199);
		fact("Ironwood tree", "Ironwood logs", 32907, 80, 175, 400, 199);
		fact("Redwood tree", "Redwood logs", 19669, 90, 380, 440, 199);
		fact("Rosewood tree", "Rosewood logs", 32910, 92, 212.5, 458, 205);
		assertEquals(13, Trees.all().size());
	}

	@Test
	public void forestryTableSecondsMatchTheTicks()
	{
		// Forestry's table in seconds: oak 27, magic 3:54, rosewood 4:34.8; respawn magic 1:59.4, rosewood 2:03.
		assertEquals(27_000, Trees.byName("Oak tree").fallTicks * Trees.TICK_MS);
		assertEquals(234_000, Trees.byName("Magic tree").fallTicks * Trees.TICK_MS);
		assertEquals(274_800, Trees.byName("Rosewood tree").fallTicks * Trees.TICK_MS);
		assertEquals(119_400, Trees.byName("Magic tree").respawnTicks * Trees.TICK_MS);
		assertEquals(123_000, Trees.byName("Rosewood tree").respawnTicks * Trees.TICK_MS);
		assertEquals(120_000, Trees.byName("Camphor tree").fallTicks * Trees.TICK_MS);
	}

	@Test
	public void regularTreesHaveNoTimerAndOthersAreNotTrees()
	{
		assertFalse(Trees.byName("Tree").timed());
		assertTrue(Trees.byName("yew TREE").timed());
		assertNull(Trees.byName("Dead tree"));
		assertNull(Trees.byName("Oak"));
		assertNull(Trees.byLogs("Bark"));
		assertNull(Trees.byName(null));
	}

	@Test
	public void patchTreesAreKnownAsFarmed()
	{
		assertTrue(Trees.farmed(ObjectID.YEW_TREE_FULLYGROWN_1));
		assertTrue(Trees.farmed(ObjectID.MAGIC_TREE_FULLYGROWN_2));
		assertFalse(Trees.farmed(ObjectID.YEWTREE));
		assertFalse(Trees.farmed(ObjectID.OAKTREE));
	}
}
