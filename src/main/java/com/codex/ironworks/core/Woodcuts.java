package com.codex.ironworks.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The player's own woodcutting log, tree by tree: logs cut, the time at it, and the trees felled.
 *
 * <p>Logs come one at a time, so time is counted log to log; but a tree on the Forestry timer falls
 * and takes its while to grow back, and that wait is part of chopping it. So a gap counts as time
 * at it up to {@link #GAP_MS} plus the tree's own regrowth (OSRS Wiki), and anything longer was a
 * bank trip or a break and is not timed.
 *
 * <p>Falls the coach saw from the first log are timed in ticks of chopping, so the account's own
 * trees can be held against the wiki's figure.
 */
public final class Woodcuts
{
	/** A gap between logs longer than this, past the tree's regrowth, was not woodcutting. */
	static final long GAP_MS = 2 * 60_000L;
	/** Time at a tree before its pace is worth showing. */
	public static final long LEARN_MS = 5 * 60_000L;

	/** One kind of tree, as this account cuts it. */
	public static final class Wood
	{
		long logs;
		long ms;
		/** Trees felled while the coach watched. */
		long felled;
		/** Falls seen from the first log, and the ticks of chopping they took in all. */
		long timedFalls;
		long fallTicks;
		/** When the last log came, for the latest tree chopped. */
		long last;

		public long getLogs()
		{
			return logs;
		}

		public long getMs()
		{
			return ms;
		}

		public long getFelled()
		{
			return felled;
		}

		public long getTimedFalls()
		{
			return timedFalls;
		}

		public long getLast()
		{
			return last;
		}

		/** Logs an hour while at it, or 0 until there has been enough time to tell. */
		public double perHour()
		{
			return ms < LEARN_MS ? 0 : logs / (ms / 3_600_000.0);
		}

		/** The ticks of chopping a tree took on average, from falls seen whole; 0 when none. */
		public double ticksToFall()
		{
			return timedFalls == 0 ? 0 : fallTicks / (double) timedFalls;
		}

		Wood repair()
		{
			logs = Math.max(0, logs);
			ms = Math.max(0, ms);
			felled = Math.max(0, felled);
			timedFalls = Math.max(0, Math.min(timedFalls, felled));
			fallTicks = timedFalls == 0 ? 0 : Math.max(0, fallTicks);
			last = Math.max(0, last);
			return this;
		}
	}

	/** By the tree's name: "Yew tree". */
	Map<String, Wood> woods = new HashMap<>();

	public Woodcuts()
	{
	}

	/** Whether a flow is logs cut with Woodcutting, and so of which tree; null when it is not. */
	static Trees.Kind cut(Flow f)
	{
		if (!f.isGain() || f.source != Source.GATHERED || !"Woodcutting".equals(f.skill))
		{
			return null;
		}
		return Trees.byLogs(f.store);
	}

	/** Adds one tick's flows: the logs cut, timed log to log. */
	public void record(Iterable<Flow> flows, long now)
	{
		for (Flow f : flows)
		{
			Trees.Kind k = cut(f);
			if (k == null || f.units <= 0)
			{
				continue;
			}
			Wood w = woods.computeIfAbsent(k.name, n -> new Wood());
			long gap = now - w.last;
			if (w.last > 0 && gap > 0 && gap <= GAP_MS + k.respawnTicks * Trees.TICK_MS)
			{
				w.ms += gap;
			}
			w.logs += f.units;
			w.last = now;
		}
	}

	/**
	 * A tree fell while the coach watched it.
	 *
	 * @param ticks the ticks of chopping it took from its first log, or -1 when it was not seen whole
	 */
	public void felled(Trees.Kind kind, int ticks)
	{
		if (kind == null)
		{
			return;
		}
		Wood w = woods.computeIfAbsent(kind.name, n -> new Wood());
		w.felled++;
		if (ticks > 0)
		{
			w.timedFalls++;
			w.fallTicks += ticks;
		}
	}

	/** Trees felled in all while the coach watched. */
	public long felled()
	{
		long n = 0;
		for (Wood w : woods.values())
		{
			n += w.felled;
		}
		return n;
	}

	public boolean isEmpty()
	{
		for (Wood w : woods.values())
		{
			if (w.logs > 0 || w.felled > 0)
			{
				return false;
			}
		}
		return true;
	}

	/** One tree's log, or null when never cut. */
	public Wood wood(Trees.Kind kind)
	{
		return kind == null ? null : woods.get(kind.name);
	}

	public Map<String, Wood> woods()
	{
		return Collections.unmodifiableMap(woods);
	}

	/** Logs an hour of one store at the player's own pace on its tree; 0 until timed. */
	public double perHour(String logs)
	{
		Wood w = wood(Trees.byLogs(logs));
		return w == null ? 0 : w.perHour();
	}

	/** The tree whose logs came last, or null. */
	public Trees.Kind latest()
	{
		Trees.Kind best = null;
		long at = -1;
		for (Map.Entry<String, Wood> e : woods.entrySet())
		{
			Trees.Kind k = Trees.of(e.getKey());
			if (k != null && e.getValue().last > at)
			{
				at = e.getValue().last;
				best = k;
			}
		}
		return best;
	}

	Woodcuts repair()
	{
		Map<String, Wood> m = new HashMap<>();
		if (woods != null)
		{
			woods.forEach((k, v) ->
			{
				if (k != null && v != null && Trees.of(k) != null)
				{
					m.put(k, v.repair());
				}
			});
		}
		woods = m;
		return this;
	}
}
