package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own herbiboar log: every harvest, the herbs it gave by kind, and the time from one
 * harvest to the next, which is a whole trail (the start, the searching, the tunnel, the harvest).
 * Herbs come in once a trail, so a pace timed gain to gain, as {@link Rate} does, would see only
 * the seconds at the harvest; a trail is timed harvest to harvest instead. A gap longer than
 * {@link #ROUND_MS} was a break, a bank or a dropped trail, and is not timed.
 *
 * <p>The herbs on offer change with Herblore level (OSRS Wiki: Herbiboar), so harvests are also
 * kept by the stretch of levels they were made at ({@link Herbiboar#band}), and the herbs an hour
 * are worked out from the stretch the latest harvest was made at.
 */
public final class Herbiboars
{
	/** Herbs closer together than this are one harvest. */
	static final long BURST_MS = 3_000L;
	/** A trail longer than this was a break. */
	static final long ROUND_MS = 5 * 60_000L;
	/** Timed trails before a pace is worth showing. */
	public static final int LEARN_TRAILS = 3;
	/** Recent harvests kept. */
	static final int RECENT = 20;

	/** Harvests at one stretch of Herblore levels. */
	public static final class Band
	{
		long harvests;
		long herbs;
		Map<String, Long> byHerb = new HashMap<>();

		Band repair()
		{
			harvests = Math.max(0, harvests);
			herbs = Math.max(0, herbs);
			Map<String, Long> b = new HashMap<>();
			if (byHerb != null)
			{
				byHerb.forEach((k, v) ->
				{
					if (k != null && v != null && v > 0)
					{
						b.put(k, v);
					}
				});
			}
			byHerb = b;
			return this;
		}

		public long getHarvests()
		{
			return harvests;
		}

		public long getHerbs()
		{
			return herbs;
		}

		public Map<String, Long> getByHerb()
		{
			return Collections.unmodifiableMap(byHerb);
		}

		/** Herbs a harvest, all kinds. */
		public double perHarvest()
		{
			return harvests == 0 ? 0 : herbs / (double) harvests;
		}

		/** One kind of herb a harvest. */
		public double perHarvest(String herb)
		{
			return harvests == 0 ? 0 : byHerb.getOrDefault(herb, 0L) / (double) harvests;
		}
	}

	/** One harvest as it was. */
	public static final class Harvest
	{
		public long herbs;
		public long at;
		public String band;
	}

	long harvests;
	long herbs;
	long timed;
	long ms;
	Map<String, Band> bands = new HashMap<>();
	List<Harvest> recent = new ArrayList<>();

	/** The harvest coming in now: its first and latest herbs, and what they are. */
	private transient long start;
	private transient long latest;
	private transient int herblore;
	private transient Map<String, Long> now = new HashMap<>();
	/** The last harvest closed, kept so the next one can time the trail between. */
	private transient long prevStart;

	public Herbiboars()
	{
	}

	/** Whether a flow is a herb the harvest gave: gathered with Hunter, or arrived with no skill seen. */
	static boolean harvested(Flow f)
	{
		return f.isGain() && Catalogue.family(f.store) == Family.HERBS
			&& (f.source == Source.GATHERED || f.source == Source.OTHER);
	}

	/**
	 * Adds one tick's flows; call every tick, so a harvest closes once the herbs stop.
	 *
	 * @param ground whether the player is where the herbiboar is hunted
	 * @param herblore the player's Herblore level, unboosted
	 */
	public void record(List<Flow> flows, boolean ground, int herblore, long time)
	{
		if (start > 0 && time - latest > BURST_MS)
		{
			close();
		}
		if (!ground)
		{
			return;
		}
		boolean any = false;
		for (Flow f : flows)
		{
			if (harvested(f))
			{
				any = true;
				now.merge(f.store, f.units, Long::sum);
			}
		}
		if (!any)
		{
			return;
		}
		if (start == 0)
		{
			start = time;
			this.herblore = herblore;
		}
		latest = time;
	}

	/** Closes the harvest coming in: it counts, and the trail before it is timed. */
	private void close()
	{
		if (!now.isEmpty())
		{
			long n = 0;
			for (long v : now.values())
			{
				n += v;
			}
			if (prevStart > 0 && start - prevStart > 0 && start - prevStart <= ROUND_MS)
			{
				timed++;
				ms += start - prevStart;
			}
			String key = Herbiboar.bandKey(herblore);
			Band b = bands.computeIfAbsent(key, k -> new Band());
			b.harvests++;
			b.herbs += n;
			now.forEach((herb, units) -> b.byHerb.merge(herb, units, Long::sum));
			harvests++;
			herbs += n;
			Harvest h = new Harvest();
			h.herbs = n;
			h.at = start;
			h.band = key;
			recent.add(h);
			while (recent.size() > RECENT)
			{
				recent.remove(0);
			}
			prevStart = start;
		}
		start = 0;
		latest = 0;
		now = new HashMap<>();
	}

	public long getHarvests()
	{
		return harvests;
	}

	public long getHerbs()
	{
		return herbs;
	}

	public long getTimed()
	{
		return timed;
	}

	public boolean isEmpty()
	{
		return harvests == 0;
	}

	/** Trails an hour over whole trails, harvest to harvest, or 0 until enough are timed. */
	public double trailsPerHour()
	{
		return timed < LEARN_TRAILS || ms <= 0 ? 0 : timed / (ms / 3_600_000.0);
	}

	/** The average trail, harvest to harvest, or 0 until timed. */
	public long trailMs()
	{
		return timed < LEARN_TRAILS ? 0 : ms / timed;
	}

	/** Herbs a harvest over every harvest logged. */
	public double perHarvest()
	{
		return harvests == 0 ? 0 : herbs / (double) harvests;
	}

	/** The stretch of Herblore levels the latest harvest was made at, or null. */
	public String currentBand()
	{
		return recent.isEmpty() ? null : recent.get(recent.size() - 1).band;
	}

	/** Harvests at one stretch of Herblore levels, or null when none. */
	public Band band(String key)
	{
		return key == null ? null : bands.get(key);
	}

	public Map<String, Band> bands()
	{
		return Collections.unmodifiableMap(bands);
	}

	public List<Harvest> recent()
	{
		return Collections.unmodifiableList(recent);
	}

	/** Whether the log has this herb from a harvest at any level. */
	public boolean gave(String herb)
	{
		for (Band b : bands.values())
		{
			if (b.byHerb.containsKey(herb))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * One herb an hour at the player's own trails and the mix of the stretch of Herblore levels
	 * they last hunted at; 0 until timed, or when that stretch has not given it.
	 */
	public double perHour(String herb)
	{
		Band b = band(currentBand());
		return b == null ? 0 : trailsPerHour() * b.perHarvest(herb);
	}

	Herbiboars repair()
	{
		harvests = Math.max(0, harvests);
		herbs = Math.max(0, herbs);
		timed = Math.max(0, timed);
		ms = Math.max(0, ms);
		Map<String, Band> b = new HashMap<>();
		if (bands != null)
		{
			bands.forEach((k, v) ->
			{
				if (k != null && v != null)
				{
					b.put(k, v.repair());
				}
			});
		}
		bands = b;
		List<Harvest> r = new ArrayList<>();
		if (recent != null)
		{
			for (Harvest h : recent)
			{
				if (h != null && h.herbs > 0 && h.band != null)
				{
					r.add(h);
				}
			}
		}
		while (r.size() > RECENT)
		{
			r.remove(0);
		}
		recent = r;
		if (now == null)
		{
			now = new HashMap<>();
		}
		return this;
	}
}
