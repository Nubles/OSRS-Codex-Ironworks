package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A shop run: shop visits close enough together to be one trip, whether going round several shops
 * or hopping worlds at one.
 */
public final class Run
{
	/** Visits further apart than this start a new run. */
	public static final long GAP = 10 * 60_000L;

	long start;
	long end;
	/** The shops visited, by key, in the order first visited. */
	List<String> shops = new ArrayList<>();
	/** Items bought, by item name. */
	Map<String, Long> bought = new HashMap<>();
	/** Coins and tokens paid, by the currency's store. */
	Map<String, Long> spent = new HashMap<>();
	Set<Integer> worlds = new HashSet<>();
	int visits;

	Run()
	{
	}

	Run(long now)
	{
		start = now;
		end = now;
	}

	public long getStart()
	{
		return start;
	}

	public long getEnd()
	{
		return end;
	}

	public long duration()
	{
		return Math.max(0, end - start);
	}

	public List<String> getShops()
	{
		return shops;
	}

	public Map<String, Long> getBought()
	{
		return bought;
	}

	public Map<String, Long> getSpent()
	{
		return spent;
	}

	public int worlds()
	{
		return worlds.size();
	}

	public int getVisits()
	{
		return visits;
	}

	public long items()
	{
		long sum = 0;
		for (long v : bought.values())
		{
			sum += v;
		}
		return sum;
	}

	Run repair()
	{
		shops = shops == null ? new ArrayList<>() : shops;
		shops.removeIf(s -> s == null);
		bought = bought == null ? new HashMap<>() : bought;
		bought.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() <= 0);
		spent = spent == null ? new HashMap<>() : spent;
		spent.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() <= 0);
		worlds = worlds == null ? new HashSet<>() : worlds;
		worlds.removeIf(w -> w == null);
		end = Math.max(start, end);
		return this;
	}
}
