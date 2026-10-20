package com.codex.ironworks.core;

import java.util.HashMap;
import java.util.Map;

/** What happened over a stretch of days, summed: time played and each store's ins and outs. */
public final class Window
{
	final long played;
	final Map<String, Tally> stores;
	/** Calendar days the stretch covers, played or not; 0 for all time. */
	final int days;

	Window(long played, Map<String, Tally> stores, int days)
	{
		this.played = played;
		this.stores = stores;
		this.days = days;
	}

	public long getPlayed()
	{
		return played;
	}

	public int getDays()
	{
		return days;
	}

	public Map<String, Tally> getStores()
	{
		return stores;
	}

	public Tally tally(String store)
	{
		Tally t = stores.get(store);
		return t == null ? new Tally() : t;
	}

	/** Every store's tally summed into one, money left out. */
	public Tally supplies()
	{
		Tally sum = new Tally();
		stores.forEach((k, t) ->
		{
			if (!Catalogue.money(k))
			{
				sum.add(t);
			}
		});
		return sum;
	}

	static Window empty()
	{
		return new Window(0, new HashMap<>(), 0);
	}
}
