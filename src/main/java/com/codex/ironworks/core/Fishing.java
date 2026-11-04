package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own fishing log: fish by way of fishing and tool, timed catch to catch; what else
 * fishing turned up (clue bottles, caskets, big fish, Molch pearls); and how long the fishing spots
 * the player watched stayed put.
 *
 * <p>A gap between catches up to {@link #GAP_MS} is time at the spot, walking to it after it moved
 * among it; anything longer was a bank trip or a break and is not timed. Deep sea trawling is logged
 * apart in {@link Trawls}.
 */
public final class Fishing
{
	/** A gap between catches longer than this was not fishing. */
	static final long GAP_MS = 2 * 60_000L;
	/** Time at a way of fishing before its pace is worth showing. */
	public static final long LEARN_MS = 5 * 60_000L;

	/** One way of fishing with one tool, as this account fishes it. */
	public static final class Spot
	{
		/** Fish caught by store. */
		Map<String, Long> fish = new HashMap<>();
		long ms;
		long last;

		public Map<String, Long> getFish()
		{
			return Collections.unmodifiableMap(fish);
		}

		public long getMs()
		{
			return ms;
		}

		public long getLast()
		{
			return last;
		}

		public long count()
		{
			long n = 0;
			for (long v : fish.values())
			{
				n += v;
			}
			return n;
		}

		/** Fish an hour while at it, every kind together; 0 until there has been enough time to tell. */
		public double perHour()
		{
			return ms < LEARN_MS ? 0 : count() / (ms / 3_600_000.0);
		}

		/** Fish of one kind an hour while at it; 0 until timed. */
		public double perHour(String store)
		{
			return ms < LEARN_MS ? 0 : fish.getOrDefault(store, 0L) / (ms / 3_600_000.0);
		}

		/** Fishing experience an hour at the wiki's experience for each fish caught; 0 until timed. */
		public double xpPerHour()
		{
			if (ms < LEARN_MS)
			{
				return 0;
			}
			double xp = 0;
			for (Map.Entry<String, Long> e : fish.entrySet())
			{
				Fish.Kind k = Fish.byStore(e.getKey());
				xp += k == null ? 0 : k.xp * e.getValue();
			}
			return xp / (ms / 3_600_000.0);
		}

		Spot repair()
		{
			Map<String, Long> f = new HashMap<>();
			if (fish != null)
			{
				fish.forEach((k, v) ->
				{
					if (Fish.byStore(k) != null && v != null && v > 0)
					{
						f.put(k, v);
					}
				});
			}
			fish = f;
			ms = Math.max(0, ms);
			last = Math.max(0, last);
			return this;
		}
	}

	/** The stays of one kind of fishing spot the player watched from its arrival to its move. */
	public static final class Stays
	{
		long seen;
		long ticks;

		public long getSeen()
		{
			return seen;
		}

		/** Ticks a spot stayed on average; 0 when none seen. */
		public double average()
		{
			return seen == 0 ? 0 : ticks / (double) seen;
		}

		Stays repair()
		{
			seen = Math.max(0, seen);
			ticks = seen == 0 ? 0 : Math.max(0, ticks);
			return this;
		}
	}

	/** By "method|tool": "Harpoon|Dragon harpoon". */
	Map<String, Spot> spots = new HashMap<>();
	/** Clue bottles, caskets, big fish, pearls and the like, by item name. */
	Map<String, Long> found = new HashMap<>();
	/** By the kind of spot: "Cage/Harpoon". */
	Map<String, Stays> stays = new HashMap<>();

	public Fishing()
	{
	}

	static String key(Fish.Method m, String tool)
	{
		return m.name + "|" + (tool == null ? "" : tool);
	}

	/** The method of a log key. */
	public static Fish.Method methodOf(String key)
	{
		int bar = key.indexOf('|');
		return Fish.method(bar < 0 ? key : key.substring(0, bar));
	}

	/** The tool of a log key, or null when not known. */
	public static String toolOf(String key)
	{
		int bar = key.indexOf('|');
		String t = bar < 0 ? "" : key.substring(bar + 1);
		return t.isEmpty() ? null : t;
	}

	/** Fish caught, timed catch to catch with the tool it was caught with. */
	public void caught(Fish.Kind kind, String tool, long units, long now)
	{
		if (kind == null || units <= 0)
		{
			return;
		}
		Spot s = spots.computeIfAbsent(key(kind.method, tool), n -> new Spot());
		long gap = now - s.last;
		if (s.last > 0 && gap > 0 && gap <= GAP_MS)
		{
			s.ms += gap;
		}
		s.fish.merge(kind.store, units, Long::sum);
		s.last = Math.max(s.last, now);
	}

	/** Something else fishing turned up. */
	public void found(String item, long units)
	{
		if (item != null && units > 0)
		{
			found.merge(item, units, Long::sum);
		}
	}

	/** A fishing spot of a kind was watched from its arrival to its move: so many ticks. */
	public void stayed(String spot, int ticks)
	{
		if (spot == null || ticks <= 0 || ticks > 2_000)
		{
			return;
		}
		Stays s = stays.computeIfAbsent(spot, n -> new Stays());
		s.seen++;
		s.ticks += ticks;
	}

	public Map<String, Spot> spots()
	{
		return Collections.unmodifiableMap(spots);
	}

	public Map<String, Long> found()
	{
		return Collections.unmodifiableMap(found);
	}

	public Map<String, Stays> stays()
	{
		return Collections.unmodifiableMap(stays);
	}

	/** Every fish caught. */
	public long caught()
	{
		long n = 0;
		for (Spot s : spots.values())
		{
			n += s.count();
		}
		return n;
	}

	/** Fish of one kind caught, every way and tool together. */
	public long caught(String store)
	{
		long n = 0;
		for (Spot s : spots.values())
		{
			n += s.fish.getOrDefault(store, 0L);
		}
		return n;
	}

	public boolean isEmpty()
	{
		return caught() == 0 && found.isEmpty();
	}

	/** The key fished last, or null. */
	public String latest()
	{
		String best = null;
		long at = -1;
		for (Map.Entry<String, Spot> e : spots.entrySet())
		{
			if (e.getValue().last > at)
			{
				at = e.getValue().last;
				best = e.getKey();
			}
		}
		return best;
	}

	/** Whether the player has caught anything this way. */
	public boolean tried(Fish.Method m)
	{
		for (Map.Entry<String, Spot> e : spots.entrySet())
		{
			if (methodOf(e.getKey()) == m && e.getValue().count() > 0)
			{
				return true;
			}
		}
		return false;
	}

	/** Fish of one store an hour at the player's best timed pace at it; 0 until timed. */
	public double perHour(String store)
	{
		Fish.Kind k = Fish.byStore(store);
		if (k == null)
		{
			return 0;
		}
		double best = 0;
		for (Map.Entry<String, Spot> e : spots.entrySet())
		{
			if (methodOf(e.getKey()) == k.method)
			{
				best = Math.max(best, e.getValue().perHour(k.store));
			}
		}
		return best;
	}

	/** Every way and tool, the latest first. */
	public List<Map.Entry<String, Spot>> byLatest()
	{
		List<Map.Entry<String, Spot>> l = new ArrayList<>(spots.entrySet());
		l.sort((a, b) -> Long.compare(b.getValue().last, a.getValue().last));
		return l;
	}

	/** Aerial fish caught, which Molch pearls are counted against. */
	public long aerialCatches()
	{
		long n = 0;
		for (Map.Entry<String, Spot> e : spots.entrySet())
		{
			if (methodOf(e.getKey()) == Fish.AERIAL)
			{
				n += e.getValue().count();
			}
		}
		return n;
	}

	/** Time timed at aerial fishing. */
	public long aerialMs()
	{
		long n = 0;
		for (Map.Entry<String, Spot> e : spots.entrySet())
		{
			if (methodOf(e.getKey()) == Fish.AERIAL)
			{
				n += e.getValue().ms;
			}
		}
		return n;
	}

	/** Molch pearls an hour of aerial fishing at the player's pace; 0 until timed. */
	public double pearlsPerHour()
	{
		long ms = aerialMs();
		return ms < LEARN_MS ? 0 : found.getOrDefault(Fish.PEARL_NAME, 0L) / (ms / 3_600_000.0);
	}

	Fishing repair()
	{
		Map<String, Spot> s = new HashMap<>();
		if (spots != null)
		{
			spots.forEach((k, v) ->
			{
				if (k != null && v != null && methodOf(k) != null)
				{
					s.put(k, v.repair());
				}
			});
		}
		spots = s;
		Map<String, Long> f = new HashMap<>();
		if (found != null)
		{
			found.forEach((k, v) ->
			{
				if (k != null && v != null && v > 0)
				{
					f.put(k, v);
				}
			});
		}
		found = f;
		Map<String, Stays> st = new HashMap<>();
		if (stays != null)
		{
			stays.forEach((k, v) ->
			{
				if (k != null && v != null)
				{
					st.put(k, v.repair());
				}
			});
		}
		stays = st;
		return this;
	}
}
