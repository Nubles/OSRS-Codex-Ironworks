package com.codex.ironworks.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * One item a shop sells: its usual stock, what the player has bought of it, and how fast the shop
 * puts it back, learned by watching the stock climb. Shops keep stock per world, so each world is
 * remembered on its own.
 */
public final class Ware
{
	/** Worlds remembered per item; enough for a hopping run, without the save growing for ever. */
	static final int WORLDS = 40;
	/** Readings further apart than this say nothing reliable about the pace of restocking. */
	static final long LONGEST = 3 * 60 * 60_000L;
	/** Readings closer than this are too coarse to time a restock by. */
	static final long SHORTEST = 3_000L;

	int item;
	String name;
	/** The usual stock: the most ever seen. */
	int base;
	long bought;
	/** Coins and tokens paid for it, by the currency's store. */
	Map<String, Long> spent = new HashMap<>();
	/** Milliseconds for one more to come back into stock, or 0 while not yet known. */
	double restockMs;
	int samples;
	/** The last stock seen on each world. */
	Map<String, Seen> worlds = new HashMap<>();

	/** A stock reading. */
	public static final class Seen
	{
		int stock;
		long at;

		Seen(int stock, long at)
		{
			this.stock = stock;
			this.at = at;
		}

		public int getStock()
		{
			return stock;
		}

		public long getAt()
		{
			return at;
		}
	}

	Ware()
	{
	}

	Ware(int item, String name)
	{
		this.item = item;
		this.name = name;
	}

	/** The shop shows this much in stock on this world now. */
	void observe(int world, int stock, long now)
	{
		if (stock < 0)
		{
			return;
		}
		base = Math.max(base, stock);
		String w = String.valueOf(world);
		Seen prev = worlds.get(w);
		if (prev != null && stock > prev.stock && prev.stock < base)
		{
			long gap = now - prev.at;
			if (gap >= SHORTEST && gap <= LONGEST)
			{
				double perUnit = gap / (double) (stock - prev.stock);
				if (stock < base)
				{
					// Still climbing, so the whole gap was restocking: a true measure.
					restockMs = samples == 0 ? perUnit : restockMs * 0.7 + perUnit * 0.3;
					samples++;
				}
				else if (restockMs == 0 || restockMs > perUnit)
				{
					// Back to full: it restocked at least this fast, perhaps faster.
					restockMs = perUnit;
				}
			}
		}
		if (prev == null || prev.stock != stock || now - prev.at > SHORTEST)
		{
			worlds.put(w, new Seen(stock, now));
		}
		while (worlds.size() > WORLDS)
		{
			Iterator<Map.Entry<String, Seen>> it = worlds.entrySet().iterator();
			Map.Entry<String, Seen> oldest = it.next();
			for (Map.Entry<String, Seen> e : worlds.entrySet())
			{
				if (e.getValue().at < oldest.getValue().at)
				{
					oldest = e;
				}
			}
			worlds.remove(oldest.getKey());
		}
	}

	void bought(long qty, Map<String, Long> paid)
	{
		bought += qty;
		paid.forEach((k, v) -> spent.merge(k, v, Long::sum));
	}

	/** The stock expected on a world now, or -1 when that world was never seen. */
	public int expected(int world, long now)
	{
		Seen s = worlds.get(String.valueOf(world));
		if (s == null)
		{
			return -1;
		}
		if (restockMs <= 0 || s.stock >= base)
		{
			return s.stock;
		}
		long back = (long) ((now - s.at) / restockMs);
		return (int) Math.min(base, s.stock + back);
	}

	/** When a world will be back to its usual stock, or 0 when it already is or the pace is unknown. */
	public long fullAt(int world)
	{
		Seen s = worlds.get(String.valueOf(world));
		if (s == null || s.stock >= base || restockMs <= 0)
		{
			return 0;
		}
		return s.at + (long) Math.ceil((base - s.stock) * restockMs);
	}

	/** How long an emptied shop takes to fill again, or 0 while unknown. */
	public long refillMs()
	{
		return restockMs <= 0 ? 0 : (long) (restockMs * base);
	}

	public int getItem()
	{
		return item;
	}

	public String getName()
	{
		return name;
	}

	public int getBase()
	{
		return base;
	}

	public long getBought()
	{
		return bought;
	}

	public Map<String, Long> getSpent()
	{
		return spent;
	}

	public double getRestockMs()
	{
		return restockMs;
	}

	public Map<String, Seen> getWorlds()
	{
		return worlds;
	}

	Ware repair()
	{
		base = Math.max(0, base);
		bought = Math.max(0, bought);
		if (Double.isNaN(restockMs) || Double.isInfinite(restockMs) || restockMs < 0)
		{
			restockMs = 0;
		}
		spent = spent == null ? new HashMap<>() : spent;
		spent.values().removeIf(v -> v == null || v <= 0);
		spent.keySet().removeIf(k -> k == null);
		worlds = worlds == null ? new HashMap<>() : worlds;
		worlds.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue().stock < 0);
		return this;
	}
}
