package com.codex.ironworks.core;

/**
 * How fast the player gets a store one way, such as gathering: units over the time spent at it.
 * Only the time between gains close together counts, so a break, a bank trip or a day off is not
 * held against the pace.
 */
public final class Rate
{
	/** Gains further apart than this are separate stretches of work. */
	static final long GAP = 2 * 60_000L;
	/** Time at it before the pace is worth showing. */
	public static final long LEARN_MS = 5 * 60_000L;

	long units;
	long ms;
	/** The last gain, while this session is still at it; not saved. */
	transient long last;

	Rate()
	{
	}

	void add(long n, long now)
	{
		if (n <= 0)
		{
			return;
		}
		if (last > 0 && now - last > 0 && now - last <= GAP)
		{
			ms += now - last;
		}
		units += n;
		last = now;
	}

	/** Units an hour while at it, or 0 until there has been enough time to tell. */
	public double perHour()
	{
		return ms < LEARN_MS ? 0 : units / (ms / 3_600_000.0);
	}

	public long getUnits()
	{
		return units;
	}

	public long getMs()
	{
		return ms;
	}

	Rate repair()
	{
		units = Math.max(0, units);
		ms = Math.max(0, ms);
		return this;
	}
}
