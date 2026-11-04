package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The player's own deep sea trawling log, fish by fish: what came up in the net, the time at it,
 * and the Fishing and Sailing experience that came with it.
 *
 * <p>A net brings fish up catch by catch while the boat keeps with a shoal, and the shoal moves on
 * between its stops, so time is counted catch to catch: a gap up to {@link #GAP_MS} is time at the
 * shoal (following it to its next stop among it), and anything longer was a trip to the bank or a
 * break and is not timed. The experience that came in a gap is counted only when the gap is. A
 * mixed shoal gives more than one fish; a catch of several kinds shares its gap among them by
 * number. The first catch after a gap starts the clock, so its fish are counted but not timed.
 */
public final class Trawls
{
	/** A gap between catches longer than this was not trawling. */
	static final long GAP_MS = 3 * 60_000L;
	/** Time at a shoal before its pace is worth showing. */
	public static final long LEARN_MS = 5 * 60_000L;

	/** One kind of fish, as this account trawls it. */
	public static final class Haul
	{
		long fish;
		/** Fish in catches that closed a timed gap: the ones the time is counted for. */
		long timedFish;
		long catches;
		long ms;
		double fishingXp;
		double sailingXp;
		long last;

		public long getFish()
		{
			return fish;
		}

		public long getCatches()
		{
			return catches;
		}

		public long getMs()
		{
			return ms;
		}

		public long getLast()
		{
			return last;
		}

		/** Fish an hour while at it, or 0 until there has been enough time to tell. */
		public double perHour()
		{
			return ms < LEARN_MS ? 0 : timedFish / (ms / 3_600_000.0);
		}

		/** Fishing experience an hour while at it, as it came in; 0 until timed. */
		public double fishingPerHour()
		{
			return ms < LEARN_MS ? 0 : fishingXp / (ms / 3_600_000.0);
		}

		/** Sailing experience an hour while at it, as it came in; 0 until timed or when none came. */
		public double sailingPerHour()
		{
			return ms < LEARN_MS ? 0 : sailingXp / (ms / 3_600_000.0);
		}

		Haul repair()
		{
			fish = Math.max(0, fish);
			timedFish = Math.max(0, Math.min(timedFish, fish));
			catches = Math.max(0, Math.min(catches, fish));
			ms = Math.max(0, ms);
			fishingXp = Double.isFinite(fishingXp) ? Math.max(0, fishingXp) : 0;
			sailingXp = Double.isFinite(sailingXp) ? Math.max(0, sailingXp) : 0;
			last = Math.max(0, last);
			return this;
		}
	}

	/** By the fish's store: "Raw haddock". */
	Map<String, Haul> hauls = new HashMap<>();
	/** When the last catch came, of any fish. */
	long last;

	public Trawls()
	{
	}

	/** Words the game may write a number of fish as. */
	private static final String[] WORDS = {"a", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
	private static final Pattern CATCH = Pattern.compile("^(?:You catch|.+ catches) (an?|one|two|three|four|five|six|seven|eight|nine|ten|\\d{1,3}) (.+?)[.!]*$",
		Pattern.CASE_INSENSITIVE);

	/** A catch as a game message gives it: so many of one fish. */
	public static final class Catch
	{
		public final Shoals.Kind kind;
		public final long units;

		Catch(Shoals.Kind kind, long units)
		{
			this.kind = kind;
			this.units = units;
		}
	}

	/**
	 * Reads a catch message, tags already taken off: "You catch two haddock!", or a crewmate's "Jobless
	 * Jim catches a giant krill!". Null for anything else, a rare fish or a fish not trawled among it.
	 */
	public static Catch parse(String message)
	{
		if (message == null)
		{
			return null;
		}
		Matcher m = CATCH.matcher(message.trim());
		if (!m.matches())
		{
			return null;
		}
		Shoals.Kind k = Shoals.byCaught(m.group(2));
		if (k == null)
		{
			return null;
		}
		String n = m.group(1).toLowerCase(Locale.ENGLISH);
		long units = 0;
		for (int i = 0; i < WORDS.length; i++)
		{
			if (WORDS[i].equals(n))
			{
				units = Math.max(1, i);
			}
		}
		if (n.equals("an"))
		{
			units = 1;
		}
		if (units == 0)
		{
			units = Long.parseLong(n);
		}
		return units > 0 ? new Catch(k, units) : null;
	}

	/**
	 * One catch: the fish that came up on one tick, by kind, with the Fishing and Sailing experience
	 * that came in since the last catch.
	 */
	public void caught(Map<Shoals.Kind, Long> fish, double fishingXp, double sailingXp, long now)
	{
		long total = 0;
		for (Long u : fish.values())
		{
			total += u == null ? 0 : Math.max(0, u);
		}
		if (total <= 0)
		{
			return;
		}
		long gap = now - last;
		boolean timed = last > 0 && gap > 0 && gap <= GAP_MS;
		for (Map.Entry<Shoals.Kind, Long> e : fish.entrySet())
		{
			long u = e.getValue() == null ? 0 : e.getValue();
			if (e.getKey() == null || u <= 0)
			{
				continue;
			}
			Haul h = hauls.computeIfAbsent(e.getKey().fish, n -> new Haul());
			h.fish += u;
			h.catches++;
			h.last = now;
			if (timed)
			{
				double share = u / (double) total;
				h.timedFish += u;
				h.ms += Math.round(gap * share);
				h.fishingXp += Math.max(0, fishingXp) * share;
				h.sailingXp += Math.max(0, sailingXp) * share;
			}
		}
		last = now;
	}

	public Map<String, Haul> hauls()
	{
		return Collections.unmodifiableMap(hauls);
	}

	/** Every fish trawled. */
	public long fish()
	{
		long n = 0;
		for (Haul h : hauls.values())
		{
			n += h.fish;
		}
		return n;
	}

	public boolean isEmpty()
	{
		return fish() == 0;
	}

	/** Whether the player has trawled a fish at all. */
	public boolean tried(Shoals.Kind kind)
	{
		Haul h = kind == null ? null : hauls.get(kind.fish);
		return h != null && h.fish > 0;
	}

	/** One fish as trawled, or null. */
	public Haul haul(Shoals.Kind kind)
	{
		return kind == null ? null : hauls.get(kind.fish);
	}

	/** Fish an hour of one store at the player's own timed pace; 0 until timed. */
	public double perHour(String store)
	{
		Haul h = hauls.get(store);
		return h == null ? 0 : h.perHour();
	}

	/** The fish, the latest first. */
	public List<Map.Entry<String, Haul>> byLatest()
	{
		List<Map.Entry<String, Haul>> l = new ArrayList<>(hauls.entrySet());
		l.sort((a, b) -> Long.compare(b.getValue().last, a.getValue().last));
		return l;
	}

	/** All fish timed together: fish, time and experience. */
	public Haul all()
	{
		Haul all = new Haul();
		for (Haul h : hauls.values())
		{
			all.fish += h.fish;
			all.timedFish += h.timedFish;
			all.catches += h.catches;
			all.ms += h.ms;
			all.fishingXp += h.fishingXp;
			all.sailingXp += h.sailingXp;
			all.last = Math.max(all.last, h.last);
		}
		return all;
	}

	Trawls repair()
	{
		Map<String, Haul> h = new HashMap<>();
		if (hauls != null)
		{
			hauls.forEach((k, v) ->
			{
				if (v != null && Shoals.byStore(k) != null)
				{
					h.put(k, v.repair());
				}
			});
		}
		hauls = h;
		last = Math.max(0, last);
		return this;
	}
}
