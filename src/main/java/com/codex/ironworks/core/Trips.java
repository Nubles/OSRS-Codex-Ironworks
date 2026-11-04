package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own runecrafting trips, measured from what they craft. Runes come in bursts at the
 * altar (a craft, the pouches emptied, another craft) with the run to the bank and back between,
 * so a pace timed gain to gain, as {@link Rate} does, would see only the moments at the altar. Here
 * a trip is one burst of crafting, the essence it used and the runes it made, and a trip's time is
 * from its first craft to the next trip's first craft: the whole round, bank and running included.
 * A round longer than {@link #ROUND_MS} is a break, not a trip, and is not timed; nor is a round
 * that ends at another altar, since the run there is not the run the pace is for.
 */
public final class Trips
{
	/** Crafts closer together than this are one trip at the altar. */
	static final long BURST_MS = 20_000L;
	/** A round longer than this was a break. */
	static final long ROUND_MS = 10 * 60_000L;
	/** Timed rounds before a pace is worth showing. */
	public static final int LEARN_TRIPS = 3;
	/** Recent trips kept, for the essence carried each trip. */
	static final int RECENT = 12;

	/** One rune's trips, in all. */
	public static final class Leg
	{
		long trips;
		long essence;
		long runes;
		long timed;
		long timedRunes;
		long ms;
		long last;

		Leg repair()
		{
			trips = Math.max(0, trips);
			essence = Math.max(0, essence);
			runes = Math.max(0, runes);
			timed = Math.max(0, timed);
			timedRunes = Math.max(0, timedRunes);
			ms = Math.max(0, ms);
			return this;
		}

		public long getTrips()
		{
			return trips;
		}

		public long getRunes()
		{
			return runes;
		}

		public long getTimed()
		{
			return timed;
		}

		/** Essence used a trip, on average. */
		public double essencePerTrip()
		{
			return trips == 0 ? 0 : essence / (double) trips;
		}

		/** Runes made from each essence, multiples included. */
		public double runesPerEssence()
		{
			return essence == 0 ? 0 : runes / (double) essence;
		}

		/** Runes an hour over whole rounds, or 0 until enough rounds are timed. */
		public double perHour()
		{
			return timed < LEARN_TRIPS || ms <= 0 ? 0 : timedRunes / (ms / 3_600_000.0);
		}

		/** The average round, bank to altar to bank, or 0 until timed. */
		public long roundMs()
		{
			return timed < LEARN_TRIPS ? 0 : ms / timed;
		}
	}

	/** One trip as it was. */
	public static final class Trip
	{
		public String rune;
		public long essence;
		public long runes;
		public long at;
	}

	Map<String, Leg> legs = new HashMap<>();
	List<Trip> recent = new ArrayList<>();

	/** The trip at the altar now: its first and latest craft, essence used and runes made. */
	private transient long start;
	private transient long latest;
	private transient long essence;
	private transient Map<String, Long> runes = new HashMap<>();
	/** The last trip closed, kept until the next starts so its round can be timed. */
	private transient long prevStart;
	private transient Map<String, Long> prevRunes;

	public Trips()
	{
	}

	/** Whether a flow is part of crafting runes: runes made, or essence used, with Runecraft. */
	static boolean crafted(Flow f)
	{
		if (!"Runecraft".equals(f.skill))
		{
			return false;
		}
		if (f.isGain())
		{
			return f.source == Source.MADE && Catalogue.family(f.store) == Family.RUNES;
		}
		return f.use == Use.MADE_INTO && essence(f.store);
	}

	/** Whether a store is essence runes are crafted from. */
	public static boolean essence(String store)
	{
		return store != null && store.toLowerCase(java.util.Locale.ENGLISH).endsWith("essence");
	}

	/** Adds one tick's flows; call every tick, so a trip at the altar closes once the crafting stops. */
	public void record(List<Flow> flows, long now)
	{
		boolean any = false;
		for (Flow f : flows)
		{
			any |= crafted(f);
		}
		if (start > 0 && now - latest > BURST_MS)
		{
			close();
		}
		if (!any)
		{
			return;
		}
		if (start == 0)
		{
			start = now;
		}
		latest = now;
		for (Flow f : flows)
		{
			if (!crafted(f))
			{
				continue;
			}
			if (f.isGain())
			{
				runes.merge(f.store, f.units, Long::sum);
			}
			else
			{
				essence += f.units;
			}
		}
	}

	/** Closes the trip at the altar: it counts, and the round before it is timed. */
	private void close()
	{
		if (!runes.isEmpty())
		{
			if (prevRunes != null && start - prevStart > 0 && start - prevStart <= ROUND_MS
				&& !Collections.disjoint(prevRunes.keySet(), runes.keySet()))
			{
				long round = start - prevStart;
				prevRunes.forEach((rune, n) ->
				{
					Leg l = legs.computeIfAbsent(rune, k -> new Leg());
					l.timed++;
					l.timedRunes += n;
					l.ms += round;
				});
			}
			String main = null;
			long most = 0;
			for (Map.Entry<String, Long> e : runes.entrySet())
			{
				Leg l = legs.computeIfAbsent(e.getKey(), k -> new Leg());
				l.trips++;
				l.essence += essence;
				l.runes += e.getValue();
				l.last = start;
				if (e.getValue() > most)
				{
					most = e.getValue();
					main = e.getKey();
				}
			}
			Trip t = new Trip();
			t.rune = main;
			t.essence = essence;
			t.runes = most;
			t.at = start;
			recent.add(t);
			while (recent.size() > RECENT)
			{
				recent.remove(0);
			}
			prevStart = start;
			prevRunes = new HashMap<>(runes);
		}
		start = 0;
		latest = 0;
		essence = 0;
		runes = new HashMap<>();
	}

	/** One rune's trips, or null when never crafted. */
	public Leg leg(String rune)
	{
		return legs.get(rune);
	}

	public Map<String, Leg> legs()
	{
		return Collections.unmodifiableMap(legs);
	}

	public List<Trip> recent()
	{
		return Collections.unmodifiableList(recent);
	}

	/** Runes an hour at the player's own rounds, or 0 when not yet timed. */
	public double perHour(String rune)
	{
		Leg l = legs.get(rune);
		return l == null ? 0 : l.perHour();
	}

	/** The rune crafted on the latest trip, or null. */
	public String current()
	{
		return recent.isEmpty() ? null : recent.get(recent.size() - 1).rune;
	}

	/** Essence used a trip over the recent trips, or 0. */
	public double essencePerTrip()
	{
		if (recent.isEmpty())
		{
			return 0;
		}
		long sum = 0;
		for (Trip t : recent)
		{
			sum += t.essence;
		}
		return sum / (double) recent.size();
	}

	/** The most essence used on one recent trip: what a full load carries. */
	public long bestTrip()
	{
		long best = 0;
		for (Trip t : recent)
		{
			best = Math.max(best, t.essence);
		}
		return best;
	}

	public boolean isEmpty()
	{
		return legs.isEmpty();
	}

	Trips repair()
	{
		Map<String, Leg> l = new HashMap<>();
		if (legs != null)
		{
			legs.forEach((k, v) ->
			{
				if (k != null && v != null)
				{
					l.put(k, v.repair());
				}
			});
		}
		legs = l;
		List<Trip> r = new ArrayList<>();
		if (recent != null)
		{
			for (Trip t : recent)
			{
				if (t != null && t.rune != null && t.essence >= 0 && t.runes >= 0)
				{
					r.add(t);
				}
			}
		}
		while (r.size() > RECENT)
		{
			r.remove(0);
		}
		recent = r;
		if (runes == null)
		{
			runes = new HashMap<>();
		}
		return this;
	}
}
