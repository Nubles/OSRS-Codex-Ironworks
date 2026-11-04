package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own mining log: ore by rock and pickaxe, timed ore to ore; the gems and clues mining
 * turned up; and how long the rocks the player saw mined took to come back, place by place.
 *
 * <p>A gap between ores counts as time at the rock up to {@link #GAP_MS} plus the rock's wait to
 * come back (capped at {@link #WAIT_CAP_MS}, as nobody stands by a runite rock for twelve minutes),
 * and anything longer was a bank trip or a break and is not timed.
 */
public final class Mines
{
	/** A gap between ores longer than this, past the rock's wait to come back, was not mining. */
	static final long GAP_MS = 2 * 60_000L;
	/** The most of a rock's wait to come back that still counts as time at it. */
	static final long WAIT_CAP_MS = 4 * 60_000L;
	/** Time at a rock before its pace is worth showing. */
	public static final long LEARN_MS = 5 * 60_000L;
	/** Comebacks seen at a place before the coach counts on them over the wiki's figure. */
	public static final int LEARN_RESPAWNS = 3;

	/** One rock with one pickaxe, as this account mines it. */
	public static final class Seam
	{
		long ore;
		long ms;
		long last;

		public long getOre()
		{
			return ore;
		}

		public long getMs()
		{
			return ms;
		}

		public long getLast()
		{
			return last;
		}

		/** Ore an hour while at it, or 0 until there has been enough time to tell. */
		public double perHour()
		{
			return ms < LEARN_MS ? 0 : ore / (ms / 3_600_000.0);
		}

		Seam repair()
		{
			ore = Math.max(0, ore);
			ms = Math.max(0, ms);
			last = Math.max(0, last);
			return this;
		}
	}

	/** The comebacks of one kind of rock seen at one place. */
	public static final class Comeback
	{
		long seen;
		long ticks;

		public long getSeen()
		{
			return seen;
		}

		/** The ticks a rock took to come back on average; 0 when none seen. */
		public double average()
		{
			return seen == 0 ? 0 : ticks / (double) seen;
		}

		Comeback repair()
		{
			seen = Math.max(0, seen);
			ticks = seen == 0 ? 0 : Math.max(0, ticks);
			return this;
		}
	}

	/** By "rock|pickaxe": "Iron rocks|Dragon pickaxe". */
	Map<String, Seam> seams = new HashMap<>();
	/** Gems and clues turned up while mining, by item name. */
	Map<String, Long> found = new HashMap<>();
	/** Ore mined from rocks other than gem rocks, the gems and clues counted against. */
	long ore;
	/** By "rock@region": "Iron rocks@12850". */
	Map<String, Comeback> comebacks = new HashMap<>();

	public Mines()
	{
	}

	static String key(Rocks.Kind kind, String pickaxe)
	{
		return kind.name + "|" + (pickaxe == null ? "" : pickaxe);
	}

	/** The rock of a seam key. */
	public static Rocks.Kind rockOf(String key)
	{
		int bar = key.indexOf('|');
		return Rocks.of(bar < 0 ? key : key.substring(0, bar));
	}

	/** The pickaxe of a seam key, or null when it was not known. */
	public static String pickaxeOf(String key)
	{
		int bar = key.indexOf('|');
		String p = bar < 0 ? "" : key.substring(bar + 1);
		return p.isEmpty() ? null : p;
	}

	/** Ore from a rock, timed ore to ore with the pickaxe it was swung with. */
	public void mined(Rocks.Kind kind, String pickaxe, long units, long now)
	{
		if (kind == null || units <= 0)
		{
			return;
		}
		Seam s = seams.computeIfAbsent(key(kind, pickaxe), n -> new Seam());
		long gap = now - s.last;
		long wait = Math.min(WAIT_CAP_MS, kind.respawnTicks * Rocks.TICK_MS);
		if (s.last > 0 && gap > 0 && gap <= GAP_MS + wait)
		{
			s.ms += gap;
		}
		s.ore += units;
		s.last = now;
		if (kind != Rocks.GEM_ROCKS)
		{
			ore += units;
		}
	}

	/** A gem or clue turned up while mining. */
	public void found(String item, long units)
	{
		if (item != null && units > 0)
		{
			found.merge(item, units, Long::sum);
		}
	}

	/** A rock seen mined came back after so many ticks, at a place (its map region). */
	public void cameBack(Rocks.Kind kind, int region, int ticks)
	{
		// Far past the wiki's time it was most likely out of sight in between; leave it out.
		if (kind == null || ticks <= 0 || ticks > kind.respawnTicks * 3 + 10)
		{
			return;
		}
		Comeback c = comebacks.computeIfAbsent(kind.name + "@" + region, n -> new Comeback());
		c.seen++;
		c.ticks += ticks;
	}

	/** The comebacks seen for a rock at a place, or null. */
	public Comeback comeback(Rocks.Kind kind, int region)
	{
		return kind == null ? null : comebacks.get(kind.name + "@" + region);
	}

	/**
	 * The ticks a rock at this place takes to come back, as best known: the player's own average once
	 * {@link #LEARN_RESPAWNS} have been seen here, else the wiki's.
	 */
	public double respawnTicks(Rocks.Kind kind, int region)
	{
		Comeback c = comeback(kind, region);
		return c != null && c.seen >= LEARN_RESPAWNS ? c.average() : kind.respawnTicks;
	}

	/** Whether {@link #respawnTicks} is the player's own figure here rather than the wiki's. */
	public boolean learned(Rocks.Kind kind, int region)
	{
		Comeback c = comeback(kind, region);
		return c != null && c.seen >= LEARN_RESPAWNS;
	}

	/** Comebacks seen for one rock everywhere, as one: seen and average ticks. */
	public Comeback comebacks(Rocks.Kind kind)
	{
		Comeback all = new Comeback();
		for (Map.Entry<String, Comeback> e : comebacks.entrySet())
		{
			if (e.getKey().startsWith(kind.name + "@"))
			{
				all.seen += e.getValue().seen;
				all.ticks += e.getValue().ticks;
			}
		}
		return all;
	}

	/** Comebacks seen in all, of every rock everywhere. */
	public long comebacksSeen()
	{
		long n = 0;
		for (Comeback c : comebacks.values())
		{
			n += c.seen;
		}
		return n;
	}

	public Map<String, Seam> seams()
	{
		return Collections.unmodifiableMap(seams);
	}

	public Map<String, Long> found()
	{
		return Collections.unmodifiableMap(found);
	}

	/** Ore from rocks other than gem rocks. */
	public long ore()
	{
		return ore;
	}

	/** Every ore mined, gems from gem rocks among them. */
	public long mined()
	{
		long n = 0;
		for (Seam s : seams.values())
		{
			n += s.ore;
		}
		return n;
	}

	public boolean isEmpty()
	{
		return mined() == 0 && found.isEmpty();
	}

	/** The seam mined last, or null. */
	public String latest()
	{
		String best = null;
		long at = -1;
		for (Map.Entry<String, Seam> e : seams.entrySet())
		{
			if (e.getValue().last > at)
			{
				at = e.getValue().last;
				best = e.getKey();
			}
		}
		return best;
	}

	/** Whether the player has mined a rock at all. */
	public boolean tried(Rocks.Kind kind)
	{
		for (Map.Entry<String, Seam> e : seams.entrySet())
		{
			if (rockOf(e.getKey()) == kind && e.getValue().ore > 0)
			{
				return true;
			}
		}
		return false;
	}

	/** The best timed pace on a rock, with any pickaxe; 0 until one is timed. */
	public double perHour(Rocks.Kind kind)
	{
		double best = 0;
		for (Map.Entry<String, Seam> e : seams.entrySet())
		{
			if (rockOf(e.getKey()) == kind)
			{
				best = Math.max(best, e.getValue().perHour());
			}
		}
		return best;
	}

	/** Ore an hour of one store at the player's own best timed pace on its rock; 0 until timed. */
	public double perHour(String store)
	{
		Rocks.Kind k = Rocks.byStore(store);
		return k == null ? 0 : perHour(k);
	}

	/** The seams, the latest first. */
	public List<Map.Entry<String, Seam>> byLatest()
	{
		List<Map.Entry<String, Seam>> l = new ArrayList<>(seams.entrySet());
		l.sort((a, b) -> Long.compare(b.getValue().last, a.getValue().last));
		return l;
	}

	Mines repair()
	{
		Map<String, Seam> s = new HashMap<>();
		if (seams != null)
		{
			seams.forEach((k, v) ->
			{
				if (k != null && v != null && rockOf(k) != null)
				{
					s.put(k, v.repair());
				}
			});
		}
		seams = s;
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
		Map<String, Comeback> c = new HashMap<>();
		if (comebacks != null)
		{
			comebacks.forEach((k, v) ->
			{
				int at = k == null ? -1 : k.indexOf('@');
				if (at > 0 && v != null && Rocks.of(k.substring(0, at)) != null)
				{
					c.put(k, v.repair());
				}
			});
		}
		comebacks = c;
		ore = Math.max(0, ore);
		return this;
	}
}
