package com.codex.ironworks.core;

import java.util.List;

/**
 * A shop's price for one item, as learned from what the player paid: shops charge more for each
 * one bought as the stock falls, so the price is a first price and a climb for every one taken
 * from the usual stock.
 */
public final class Price
{
	/** Coins for one at the usual stock. */
	public final double first;
	/** Coins more for each one the stock is below its usual. */
	public final double climb;
	/** The usual stock. */
	public final int base;

	public Price(double first, double climb, int base)
	{
		this.first = first;
		this.climb = climb;
		this.base = base;
	}

	/** Coins for one when the stock stands at this. */
	public double at(int stock)
	{
		return first + climb * Math.max(0, base - stock);
	}

	/** Coins to buy this many from a full shop, as one stop. */
	public long cost(long count)
	{
		if (count <= 0)
		{
			return 0;
		}
		return Math.round(count * first + climb * count * (count - 1) / 2.0);
	}

	/**
	 * Coins to buy this many, hopping worlds whenever the price has climbed by the share given,
	 * such as a tenth, so every stop starts at a full shop.
	 */
	public long costHopping(long count, double share)
	{
		long each = perWorld(share);
		long full = count / each;
		return full * cost(each) + cost(count - full * each);
	}

	/** How many to buy on one world before the price has climbed by the share given. */
	public long perWorld(double share)
	{
		long cap = Math.max(1, base);
		if (climb <= 0)
		{
			return cap;
		}
		return Math.max(1, Math.min(cap, (long) Math.floor(first * share / climb) + 1));
	}

	/**
	 * Fits the first price and the climb to purchases by least squares. Each purchase of n from a
	 * stock of s paid n first prices plus the climb for each one below the usual stock.
	 */
	static Price fit(List<long[]> purchases, int base)
	{
		if (purchases == null || purchases.isEmpty())
		{
			return null;
		}
		double sxx = 0;
		double sxy = 0;
		double syy = 0;
		double sxc = 0;
		double syc = 0;
		long units = 0;
		long coins = 0;
		for (long[] p : purchases)
		{
			double n = p[1];
			double below = Math.max(0, base - p[0]);
			double x = n;
			double y = n * below + n * (n - 1) / 2.0;
			double c = p[2];
			sxx += x * x;
			sxy += x * y;
			syy += y * y;
			sxc += x * c;
			syc += y * c;
			units += p[1];
			coins += p[2];
		}
		double det = sxx * syy - sxy * sxy;
		if (purchases.size() >= 2 && Math.abs(det) > 1e-9 * Math.max(1, sxx * syy))
		{
			double first = (sxc * syy - syc * sxy) / det;
			double climb = (sxx * syc - sxy * sxc) / det;
			if (first > 0 && climb >= 0)
			{
				return new Price(first, climb, base);
			}
		}
		return new Price(coins / (double) Math.max(1, units), 0, base);
	}
}
