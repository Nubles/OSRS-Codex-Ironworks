package com.codex.ironworks.core;

import java.util.HashMap;
import java.util.Map;

/** Everything in one place as last seen, by item id. Potion storage is held in doses of the one-dose potion. */
public final class Holding
{
	long at;
	Map<Integer, Long> items = new HashMap<>();

	Holding()
	{
	}

	Holding(Map<Integer, Long> items, long at)
	{
		this.at = at;
		items.forEach((k, v) ->
		{
			if (k != null && k >= 0 && v != null && v > 0)
			{
				this.items.put(k, v);
			}
		});
	}

	public long getAt()
	{
		return at;
	}

	public Map<Integer, Long> getItems()
	{
		return items;
	}

	Holding repair()
	{
		Map<Integer, Long> clean = new HashMap<>();
		if (items != null)
		{
			items.forEach((k, v) ->
			{
				if (k != null && k >= 0 && v != null && v > 0)
				{
					clean.put(k, v);
				}
			});
		}
		items = clean;
		at = Math.max(0, at);
		return this;
	}
}
