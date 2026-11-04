package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The player's own Aldarin vineyard log: every grape barrel handed in to the foreman, the grapes it
 * paid, and the time from one barrel to the next, which is a whole round (the four ripe vines, the
 * walk between the vineyards, the hand-in). Grapes come in once a barrel, so the pace is timed
 * barrel to barrel, as {@link Herbiboars} times trails; the first barrel of a sitting is timed from
 * the moment the barrel came into the pack. A gap longer than {@link #ROUND_MS} was a break or a bank
 * and is not timed, and starts a new sitting.
 */
public final class Vineyards
{
	/** A round longer than this was a break: a planning assumption, not a game fact. */
	static final long ROUND_MS = 10 * 60_000L;
	/** Timed barrels before a pace is worth showing. */
	public static final int LEARN_BARRELS = 3;
	/** Sittings kept. */
	static final int SITTINGS = 12;

	/** One sitting at the vineyard: barrels handed in with no gap over {@link #ROUND_MS} between. */
	public static final class Sitting
	{
		long start;
		long end;
		long barrels;
		long grapes;
		/** Barrels timed in it, and the time over them. */
		long timed;
		long ms;

		public long getStart()
		{
			return start;
		}

		public long getEnd()
		{
			return end;
		}

		public long getBarrels()
		{
			return barrels;
		}

		public long getGrapes()
		{
			return grapes;
		}

		/** Time at it, the timed barrels only. */
		public long getMs()
		{
			return ms;
		}

		/** Grapes an hour in this sitting, or 0 when none of it was timed. */
		public double grapesPerHour()
		{
			return timed == 0 || ms <= 0 || barrels == 0 ? 0 : timed * (grapes / (double) barrels) / (ms / 3_600_000.0);
		}
	}

	long barrels;
	long grapes;
	long timed;
	long ms;
	/** The latest barrel handed in, or the barrel coming into the pack; 0 when neither. */
	long last;
	/** Whether {@link #last} is a barrel taken from the foreman rather than one handed in. */
	boolean fresh;
	List<Sitting> sittings = new ArrayList<>();

	public Vineyards()
	{
	}

	/** The foreman's barrel came into the pack: the first round of a sitting is timed from here. */
	public void given(long at)
	{
		if (last == 0 || at - last > ROUND_MS)
		{
			last = at;
			fresh = true;
		}
	}

	/** A full barrel handed in for {@code grapes}. */
	public void handedIn(long grapes, long at)
	{
		if (grapes <= 0)
		{
			return;
		}
		long gap = last > 0 ? at - last : -1;
		boolean round = gap > 0 && gap <= ROUND_MS;
		Sitting s = sittings.isEmpty() ? null : sittings.get(sittings.size() - 1);
		// A barrel taken after a break starts a sitting, timed from when it was taken.
		if (s == null || !round || fresh)
		{
			s = new Sitting();
			s.start = round ? last : at;
			sittings.add(s);
			while (sittings.size() > SITTINGS)
			{
				sittings.remove(0);
			}
		}
		if (round)
		{
			timed++;
			ms += gap;
			s.timed++;
			s.ms += gap;
		}
		s.end = at;
		s.barrels++;
		s.grapes += grapes;
		barrels++;
		this.grapes += grapes;
		last = at;
		fresh = false;
	}

	public long getBarrels()
	{
		return barrels;
	}

	public long getGrapes()
	{
		return grapes;
	}

	public long getTimed()
	{
		return timed;
	}

	public long getLast()
	{
		return last;
	}

	/** Whether nothing is logged: no barrel handed in and none taken. */
	public boolean isEmpty()
	{
		return barrels == 0 && last == 0;
	}

	/** Grapes a barrel over every barrel logged; 0 until one is. */
	public double perBarrel()
	{
		return barrels == 0 ? 0 : grapes / (double) barrels;
	}

	/** Barrels an hour over whole rounds, or 0 until enough are timed. */
	public double barrelsPerHour()
	{
		return timed < LEARN_BARRELS || ms <= 0 ? 0 : timed / (ms / 3_600_000.0);
	}

	/** The average round, barrel to barrel, or 0 until timed. */
	public long roundMs()
	{
		return timed < LEARN_BARRELS ? 0 : ms / timed;
	}

	/** Grapes an hour at the player's own rounds and their own grapes a barrel; 0 until timed. */
	public double grapesPerHour()
	{
		return barrelsPerHour() * perBarrel();
	}

	/** The sittings, the latest last. */
	public List<Sitting> sittings()
	{
		return Collections.unmodifiableList(sittings);
	}

	Vineyards repair()
	{
		barrels = Math.max(0, barrels);
		grapes = Math.max(0, grapes);
		timed = Math.max(0, Math.min(timed, barrels));
		ms = Math.max(0, ms);
		last = Math.max(0, last);
		List<Sitting> s = new ArrayList<>();
		if (sittings != null)
		{
			for (Sitting t : sittings)
			{
				if (t != null && t.barrels > 0 && t.grapes > 0)
				{
					t.timed = Math.max(0, Math.min(t.timed, t.barrels));
					t.ms = Math.max(0, t.ms);
					s.add(t);
				}
			}
		}
		while (s.size() > SITTINGS)
		{
			s.remove(0);
		}
		sittings = s;
		return this;
	}
}
