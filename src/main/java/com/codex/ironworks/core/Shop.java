package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A shop the player has used, told apart from a namesake elsewhere by the map square it stands in. */
public final class Shop
{
	String name;
	int region;
	long first;
	long last;
	int visits;
	/** What it sells, by item id. */
	Map<String, Ware> wares = new HashMap<>();

	Shop()
	{
	}

	Shop(String name, int region, long now)
	{
		this.name = name;
		this.region = region;
		this.first = now;
		this.last = now;
	}

	public String key()
	{
		return key(name, region);
	}

	static String key(String name, int region)
	{
		return name + "@" + region;
	}

	Ware ware(int item, String itemName)
	{
		Ware w = wares.computeIfAbsent(String.valueOf(item), k -> new Ware(item, itemName));
		if (itemName != null)
		{
			w.name = itemName;
		}
		return w;
	}

	public Ware getWare(int item)
	{
		return wares.get(String.valueOf(item));
	}

	/** What the shop sells, most bought first, then by name. */
	public List<Ware> getWares()
	{
		List<Ware> list = new ArrayList<>(wares.values());
		list.sort((a, b) -> a.bought != b.bought ? Long.compare(b.bought, a.bought)
			: String.valueOf(a.name).compareToIgnoreCase(String.valueOf(b.name)));
		return list;
	}

	public String getName()
	{
		return name;
	}

	public int getRegion()
	{
		return region;
	}

	public long getFirst()
	{
		return first;
	}

	public long getLast()
	{
		return last;
	}

	public int getVisits()
	{
		return visits;
	}

	/** Everything paid here, by currency. */
	public Map<String, Long> spent()
	{
		Map<String, Long> sum = new HashMap<>();
		wares.values().forEach(w -> w.spent.forEach((k, v) -> sum.merge(k, v, Long::sum)));
		return sum;
	}

	public long bought()
	{
		long sum = 0;
		for (Ware w : wares.values())
		{
			sum += w.bought;
		}
		return sum;
	}

	Shop repair()
	{
		name = name == null || name.trim().isEmpty() ? "Shop" : name;
		visits = Math.max(0, visits);
		Map<String, Ware> clean = new HashMap<>();
		if (wares != null)
		{
			wares.forEach((k, w) ->
			{
				if (k != null && w != null)
				{
					clean.put(k, w.repair());
				}
			});
		}
		wares = clean;
		return this;
	}
}
