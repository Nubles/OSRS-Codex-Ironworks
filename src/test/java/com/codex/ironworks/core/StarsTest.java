package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/** Shooting star facts as the wiki gives them, the game's lines, and the player's own star log. */
public class StarsTest
{
	private static final long NOW = 2_000_000_000L;

	@Test
	public void eachCrashedStarObjectIsItsSize()
	{
		// OSRS Wiki, Crashed star: size 1 is 41229 ... size 7 is 41223, size 8 41021, size 9 41020.
		assertEquals(1, Stars.size(41229));
		assertEquals(7, Stars.size(41223));
		assertEquals(8, Stars.size(41021));
		assertEquals(9, Stars.size(41020));
		assertEquals(9, Stars.size(ObjectID.STAR_SIZE_NINE_STAR));
		assertEquals(0, Stars.size(ObjectID.STAR_SIZE_NINE_STAR + 1000));
		for (int size = 1; size <= Stars.MAX_SIZE; size++)
		{
			assertEquals(size, Stars.size(Stars.object(size)));
		}
		assertEquals(-1, Stars.object(10));
	}

	@Test
	public void eachLayerNeedsTenLevelsASize()
	{
		// OSRS Wiki, Shooting Stars: size 9 at 90 down to size 1 at 10.
		assertEquals(90, Stars.level(9));
		assertEquals(10, Stars.level(1));
		assertEquals(0, Stars.largestFor(9));
		assertEquals(1, Stars.largestFor(10));
		assertEquals(7, Stars.largestFor(79));
		assertEquals(9, Stars.largestFor(99));
		assertEquals(700, Stars.LAYER_TICKS);
		assertEquals(32, Stars.XP_PER_DUST, 0);
		assertEquals(ItemID.STAR_DUST, Stars.STARDUST);
	}

	@Test
	public void dusurisShopPricesAreTheWikis()
	{
		assertEquals(150, Stars.Reward.SOFT_CLAY_PACK.price);
		assertEquals(300, Stars.Reward.BAG_FULL_OF_GEMS.price);
		assertEquals(2000, Stars.Reward.CELESTIAL_RING.price);
		assertEquals(3000, Stars.Reward.STAR_FRAGMENT.price);
		assertEquals("Celestial ring (uncharged)", Stars.Reward.CELESTIAL_RING.item);
	}

	@Test
	public void prospectingIsReadWhetherItComesInOneLineOrTwo()
	{
		Stars.Prospect p = Stars.prospect("This is a size-9 star. A Mining level of at least 90 is required to mine this layer. "
			+ "It has been mined 13% of the way to the next layer.");
		assertNotNull(p);
		assertEquals(9, p.size);
		assertEquals(13, p.mined);
		Stars.Prospect size = Stars.prospect("This is a size-6 star. A Mining level of at least 60 is required to mine this layer.");
		assertEquals(6, size.size);
		assertEquals(-1, size.mined);
		Stars.Prospect share = Stars.prospect("It has been mined 0% of the way to the next layer.");
		assertEquals(0, share.size);
		assertEquals(0, share.mined);
		assertNull(Stars.prospect("You manage to mine some copper."));
		assertNull(Stars.prospect("This is a size-12 star."));
		assertNull(Stars.prospect(null));
	}

	@Test
	public void dustIsTimedDustToDustAndBonusDustIsLeftOutOfThePace()
	{
		Starlog log = new Starlog();
		Starlog.Find star = log.found(420, 3030, 3348, 0, 8, NOW);
		// One dust every 15 seconds for ten minutes, each with 32 xp; the first two ticks with bonus.
		for (int i = 0; i <= 40; i++)
		{
			log.mined(star, 8, i < 2 ? 2 : 1, 32, NOW + i * 15_000L);
		}
		Starlog.Tier t = log.tier(8);
		assertEquals(43, t.getDust());
		assertEquals(2, t.getBonus());
		assertEquals(600_000L, t.getMs());
		// 40 timed successes in ten minutes; bonus dust is not pace.
		assertEquals(240, t.perHour(), 1e-9);
		assertEquals(240 * 32, t.xpPerHour(), 1e-9);
		assertEquals(43, star.getDust());
		// A long break is not timed.
		log.mined(star, 8, 1, 32, NOW + 40 * 15_000L + 30 * 60_000L);
		assertEquals(600_000L, log.tier(8).getMs());
		assertEquals(8, log.latest());
	}

	@Test
	public void aStarFoundAgainOnTheSameWorldAndSpotIsTheSameStar()
	{
		Starlog log = new Starlog();
		Starlog.Find a = log.found(420, 3030, 3348, 0, 8, NOW);
		assertSame(a, log.found(420, 3031, 3349, 0, 7, NOW + 60_000));
		assertEquals(8, a.getSize());
		assertEquals(7, a.getLowest());
		// Another world is another star; so is the same spot once this one has gone.
		Starlog.Find b = log.found(301, 3030, 3348, 0, 8, NOW);
		assertFalse(a == b);
		log.gone(a);
		assertTrue(a.isGone());
		Starlog.Find c = log.found(420, 3030, 3348, 0, 9, NOW + 120_000);
		assertFalse(a == c);
		assertEquals(3, log.finds().size());
		assertSame(c, log.finds().get(0));
		for (int i = 0; i < 60; i++)
		{
			log.found(300 + i, 1000, 1000, 0, 6, NOW);
		}
		assertEquals(Starlog.FINDS, log.finds().size());
	}

	@Test
	public void layersWatchedCountOverTheWikisOnceThereAreEnough()
	{
		Starlog log = new Starlog();
		assertEquals(Stars.LAYER_TICKS, log.layerTicks(), 0);
		log.layer(690);
		log.layer(710);
		assertFalse(log.learnedLayer());
		log.layer(5000);
		assertEquals(2, log.getLayers());
		log.layer(700);
		assertTrue(log.learnedLayer());
		assertEquals(700, log.layerTicks(), 1e-9);
	}

	@Test
	public void theStarLogIsSavedAndADamagedOneIsSetRight()
	{
		Works w = new Works(1);
		Starlog.Find f = w.stars().found(420, 3030, 3348, 0, 8, NOW);
		w.stars().mined(f, 8, 2, 32, NOW);
		w.stars().discovered(f);
		w.stars().layer(700);
		Gson gson = new Gson();
		String json = w.write(gson);
		Works back = Works.read(gson, json, 9).works;
		assertEquals(2, back.stars().dust());
		assertEquals(1, back.stars().bonus());
		assertTrue(back.stars().finds().get(0).isDiscovered());
		assertEquals(420, back.stars().finds().get(0).getWorld());
		assertEquals(1, back.stars().getLayers());
		assertEquals(json, back.write(gson));
		// An older save has no stars at all.
		Works old = Works.read(gson, json.replaceAll(",\"stars\":\\{.*\\}\\}\\s*$", "}"), 9).works;
		assertTrue(old.stars().isEmpty());
		Works bad = Works.read(gson, json.replace("\"8\":{", "\"12\":{").replace("\"size\":8", "\"size\":40"), 9).works;
		assertEquals(0, bad.stars().dust());
		assertEquals(Stars.MAX_SIZE, bad.stars().finds().get(0).getSize());
	}
}
