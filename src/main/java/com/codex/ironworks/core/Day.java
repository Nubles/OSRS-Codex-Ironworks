package com.codex.ironworks.core;

import java.util.HashMap;
import java.util.Map;

/** One calendar day: how long was played and what each store did. */
public final class Day
{
	/** Milliseconds logged in. */
	long played;
	Map<String, Tally> stores = new HashMap<>();

	public long getPlayed()
	{
		return played;
	}

	Tally tally(String store)
	{
		return stores.computeIfAbsent(store, k -> new Tally());
	}

	Day repair()
	{
		played = Math.max(0, played);
		Map<String, Tally> clean = new HashMap<>();
		if (stores != null)
		{
			stores.forEach((k, t) ->
			{
				if (k != null && t != null && !t.repair().isEmpty())
				{
					clean.put(k, t);
				}
			});
		}
		stores = clean;
		return this;
	}
}
