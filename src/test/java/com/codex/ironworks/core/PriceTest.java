package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PriceTest
{
	/** Coins paid for n bought from a stock of s, at a first price of 5 climbing by 0.1 a unit below 100. */
	private static long[] buy(int stock, int n)
	{
		double coins = 0;
		for (int i = 0; i < n; i++)
		{
			coins += 5 + 0.1 * (100 - (stock - i));
		}
		return new long[]{stock, n, Math.round(coins)};
	}

	@Test
	public void learnsTheFirstPriceAndTheClimb()
	{
		List<long[]> p = new ArrayList<>();
		p.add(buy(100, 10));
		p.add(buy(90, 30));
		p.add(buy(100, 50));
		Price price = Price.fit(p, 100);
		assertEquals(5, price.first, 0.1);
		assertEquals(0.1, price.climb, 0.01);
		// The price is a tenth up after five more, so a sixth is the last one bought on a world.
		assertEquals(6, price.perWorld(0.1));
		assertEquals(Math.round(5 * 100 + 0.1 * 100 * 99 / 2.0), price.cost(100), 10);
		assertEquals(16 * price.cost(6) + price.cost(4), price.costHopping(100, 0.1), 20.0);
	}

	@Test
	public void oneFlatPurchaseIsAnAverage()
	{
		List<long[]> p = new ArrayList<>();
		p.add(new long[]{1000, 100, 300});
		Price price = Price.fit(p, 1000);
		assertEquals(3, price.first, 0.001);
		assertEquals(0, price.climb, 0.001);
		assertEquals(1000, price.perWorld(0.1));
	}

	@Test
	public void noPurchasesNoPrice()
	{
		assertNull(Price.fit(new ArrayList<>(), 10));
	}

	@Test
	public void purchasesAtAShopTeachTheWare()
	{
		Works w = new Works(0);
		Shop s = w.visit("Feather shop", 1, 301, 0);
		w.stock(s, 314, "Feather", 301, 990, 1);
		w.stock(s, 314, "Feather", 301, 1000, 0);
		w.stock(s, 314, "Feather", 301, 900, 2);
		java.util.Map<String, Long> paid = new java.util.HashMap<>();
		paid.put("Coins", 300L);
		w.purchase(s, 314, "Feather", 100, paid, 301, 2);
		Price price = s.getWares().get(0).price();
		assertEquals(3, price.first, 0.001);
	}
}
