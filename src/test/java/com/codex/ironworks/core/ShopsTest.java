package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import org.junit.Test;

public class ShopsTest
{
	private static final long MIN = 60_000L;

	@Test
	public void restockPaceIsLearnedFromStockClimbing()
	{
		Works w = new Works(0);
		Shop shop = w.visit("Gerrant's Fishy Business", 12082, 302, 0);
		w.stock(shop, 11881, "Feather pack", 302, 10, 0);
		w.stock(shop, 11881, "Feather pack", 302, 0, 5_000);
		w.purchase(shop, 11881, "Feather pack", 10, Collections.singletonMap("Coins", 20_000L), 5_000);
		// Five back a minute later, still short of ten: one every twelve seconds.
		w.stock(shop, 11881, "Feather pack", 302, 5, 5_000 + MIN);
		Ware ware = shop.getWare(11881);
		assertEquals(10, ware.getBase());
		assertEquals(12_000, ware.getRestockMs(), 1);
		assertEquals(10, ware.getBought());
		assertEquals(20_000L, (long) ware.getSpent().get("Coins"));
		assertEquals(5_000 + MIN + 60_000, ware.fullAt(302));
		assertEquals(10, ware.expected(302, 5_000 + 3 * MIN));
		assertEquals(-1, ware.expected(303, 0));
		assertEquals(120_000, ware.refillMs());
	}

	@Test
	public void aFullShopOnlyBoundsThePace()
	{
		Works w = new Works(0);
		Shop shop = w.visit("Ordan", 7757, 330, 0);
		w.stock(shop, 453, "Coal", 330, 100, 0);
		w.stock(shop, 453, "Coal", 330, 20, 1_000);
		w.stock(shop, 453, "Coal", 330, 100, 1_000 + 8 * MIN);
		// Eighty back in eight minutes at most, so no slower than six seconds each.
		assertEquals(6_000, shop.getWare(453).getRestockMs(), 1);
	}

	@Test
	public void visitsCloseTogetherAreOneRun()
	{
		Works w = new Works(0);
		Shop a = w.visit("Gerrant's Fishy Business", 12082, 302, 0);
		w.purchase(a, 11881, "Feather pack", 10, Collections.singletonMap("Coins", 20_000L), 1_000);
		w.visit("Gerrant's Fishy Business", 12082, 303, 2 * MIN);
		w.visit("Betty's Magic Emporium", 12082, 303, 4 * MIN);
		w.visit("Gerrant's Fishy Business", 12082, 304, 30 * MIN);
		assertEquals(2, w.getRuns().size());
		Run first = w.getRuns().get(0);
		assertEquals(2, first.getShops().size());
		assertEquals(2, first.worlds());
		assertEquals(3, first.getVisits());
		assertEquals(10, first.items());
		assertEquals(4 * MIN, first.duration());
		assertEquals(3, a.getVisits());
		assertTrue(w.shop(a.key()) == a);
	}

	@Test
	public void namesakesInDifferentPlacesAreDifferentShops()
	{
		Works w = new Works(0);
		w.visit("General Store", 12850, 301, 0);
		w.visit("General Store", 12853, 301, MIN);
		assertEquals(2, w.getShops().size());
	}
}
