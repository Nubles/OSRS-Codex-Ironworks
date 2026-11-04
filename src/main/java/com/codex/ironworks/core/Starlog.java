package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own shooting star log: stardust and Mining experience by the star's size (its
 * layer), timed dust to dust at the star; the stars they found themselves, world by world; and how
 * long the layers they watched from start to end took. Nothing in it comes from other players.
 *
 * <p>Each success at a star gives one stardust, and a finder's discovery bonus a second (OSRS
 * Wiki, Crashed star), so dust beyond one on a tick is bonus dust: counted, but left out of the
 * player's pace, since the bonus runs out. A gap between dust up to {@link #GAP_MS} is time at the
 * star; anything longer was a trip or a break and is not timed.
 */
public final class Starlog
{
	/** A gap between dust longer than this was not mining. */
	static final long GAP_MS = 2 * 60_000L;
	/** Time at a size before its pace is worth showing. */
	public static final long LEARN_MS = 5 * 60_000L;
	/** Layers watched from start to end before the player's own time counts over the wiki's. */
	public static final int LEARN_LAYERS = 3;
	/** Stars kept in the found log, the latest first. */
	static final int FINDS = 40;
	/** Seen again within this long, on the same world and spot, a star is the same star. */
	static final long SAME_STAR_MS = 60 * 60_000L;

	/** One size of star, as this account mines it. */
	public static final class Tier
	{
		/** Every stardust, bonus among it. */
		long dust;
		/** Discovery bonus dust. */
		long bonus;
		/** Dust from successes (bonus left out) on ticks that closed a timed gap. */
		long timedDust;
		long ms;
		/** Mining experience that came with timed dust. */
		double timedXp;
		long last;

		public long getDust()
		{
			return dust;
		}

		public long getBonus()
		{
			return bonus;
		}

		public long getMs()
		{
			return ms;
		}

		public long getLast()
		{
			return last;
		}

		/** Stardust an hour at the player's own pace, bonus left out; 0 until timed. */
		public double perHour()
		{
			return ms < LEARN_MS ? 0 : timedDust / (ms / 3_600_000.0);
		}

		/** Mining experience an hour as it came in; 0 until timed. */
		public double xpPerHour()
		{
			return ms < LEARN_MS ? 0 : timedXp / (ms / 3_600_000.0);
		}

		Tier repair()
		{
			dust = Math.max(0, dust);
			bonus = Math.max(0, Math.min(bonus, dust));
			timedDust = Math.max(0, Math.min(timedDust, dust));
			ms = Math.max(0, ms);
			timedXp = Double.isFinite(timedXp) ? Math.max(0, timedXp) : 0;
			last = Math.max(0, last);
			return this;
		}
	}

	/** One star the player found: prospected or mined. */
	public static final class Find
	{
		int world;
		int x;
		int y;
		int plane;
		/** Its size when the player first came to it, and the smallest they saw it at. */
		int size;
		int lowest;
		long first;
		long last;
		/** Stardust the player mined from it, bonus among it. */
		long dust;
		/** Whether the game said the player was the first to find it. */
		boolean discovered;
		/** Whether the player saw it disintegrate. */
		boolean gone;

		public int getWorld()
		{
			return world;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getPlane()
		{
			return plane;
		}

		public int getSize()
		{
			return size;
		}

		public int getLowest()
		{
			return lowest;
		}

		public long getFirst()
		{
			return first;
		}

		public long getLast()
		{
			return last;
		}

		public long getDust()
		{
			return dust;
		}

		public boolean isDiscovered()
		{
			return discovered;
		}

		public boolean isGone()
		{
			return gone;
		}

		boolean at(int world, int x, int y, int plane)
		{
			return this.world == world && this.plane == plane && Math.max(Math.abs(this.x - x), Math.abs(this.y - y)) <= 2;
		}

		Find repair()
		{
			size = Math.max(1, Math.min(Stars.MAX_SIZE, size));
			lowest = lowest < 1 ? size : Math.max(1, Math.min(size, lowest));
			dust = Math.max(0, dust);
			first = Math.max(0, first);
			last = Math.max(first, last);
			return this;
		}
	}

	/** By size, as text: "7". */
	Map<String, Tier> tiers = new HashMap<>();
	/** Stars found, the latest first. */
	List<Find> finds = new ArrayList<>();
	/** Layers watched from start to end, and their ticks in all. */
	long layers;
	long layerTicks;

	public Starlog()
	{
	}

	/** The star found on this world at this spot lately, or a new one. */
	public Find found(int world, int x, int y, int plane, int size, long now)
	{
		for (Find f : finds)
		{
			if (f.at(world, x, y, plane) && !f.gone && now - f.last <= SAME_STAR_MS)
			{
				f.last = Math.max(f.last, now);
				if (size > 0)
				{
					f.lowest = Math.min(f.lowest, size);
				}
				return f;
			}
		}
		Find f = new Find();
		f.world = world;
		f.x = x;
		f.y = y;
		f.plane = plane;
		f.size = Math.max(1, size);
		f.lowest = f.size;
		f.first = now;
		f.last = now;
		finds.add(0, f);
		while (finds.size() > FINDS)
		{
			finds.remove(finds.size() - 1);
		}
		return f;
	}

	/**
	 * Stardust mined on one tick from a star of this size: one from the success, any more from the
	 * discovery bonus, with the Mining experience that came with it.
	 */
	public void mined(Find star, int size, long units, double xp, long now)
	{
		if (size < 1 || size > Stars.MAX_SIZE || units <= 0)
		{
			return;
		}
		Tier t = tiers.computeIfAbsent(Integer.toString(size), k -> new Tier());
		long gap = now - t.last;
		long own = Math.min(1, units);
		t.dust += units;
		t.bonus += units - own;
		if (t.last > 0 && gap > 0 && gap <= GAP_MS)
		{
			t.ms += gap;
			t.timedDust += own;
			t.timedXp += Math.max(0, xp);
		}
		t.last = now;
		if (star != null)
		{
			star.dust += units;
			star.last = Math.max(star.last, now);
			star.lowest = Math.min(star.lowest, size);
		}
	}

	/** The game said the player was the first to find this star. */
	public void discovered(Find star)
	{
		if (star != null)
		{
			star.discovered = true;
		}
	}

	/** The player saw this star disintegrate. */
	public void gone(Find star)
	{
		if (star != null)
		{
			star.gone = true;
			star.lowest = 1;
		}
	}

	/** A layer the player watched from its start to its end took so many ticks. */
	public void layer(int ticks)
	{
		// Far past the wiki's 7 minutes nobody was mining it for a while; leave it out.
		if (ticks <= 0 || ticks > Stars.LAYER_TICKS * 3)
		{
			return;
		}
		layers++;
		layerTicks += ticks;
	}

	/** Layers watched from start to end. */
	public long getLayers()
	{
		return layers;
	}

	/** Their average ticks; 0 when none. */
	public double averageLayer()
	{
		return layers == 0 ? 0 : layerTicks / (double) layers;
	}

	/** The ticks a layer lasts as best known: the player's own once {@link #LEARN_LAYERS} were watched, else the wiki's. */
	public double layerTicks()
	{
		return layers >= LEARN_LAYERS ? averageLayer() : Stars.LAYER_TICKS;
	}

	/** Whether {@link #layerTicks} is the player's own figure. */
	public boolean learnedLayer()
	{
		return layers >= LEARN_LAYERS;
	}

	public Map<String, Tier> tiers()
	{
		return Collections.unmodifiableMap(tiers);
	}

	/** One size as mined, or null. */
	public Tier tier(int size)
	{
		return tiers.get(Integer.toString(size));
	}

	public List<Find> finds()
	{
		return Collections.unmodifiableList(finds);
	}

	/** Every stardust mined, bonus among it. */
	public long dust()
	{
		long n = 0;
		for (Tier t : tiers.values())
		{
			n += t.dust;
		}
		return n;
	}

	/** Every bonus dust. */
	public long bonus()
	{
		long n = 0;
		for (Tier t : tiers.values())
		{
			n += t.bonus;
		}
		return n;
	}

	public boolean isEmpty()
	{
		return dust() == 0 && finds.isEmpty();
	}

	/** All sizes timed together. */
	public Tier all()
	{
		Tier all = new Tier();
		for (Tier t : tiers.values())
		{
			all.dust += t.dust;
			all.bonus += t.bonus;
			all.timedDust += t.timedDust;
			all.ms += t.ms;
			all.timedXp += t.timedXp;
			all.last = Math.max(all.last, t.last);
		}
		return all;
	}

	/** The size mined last, or 0. */
	public int latest()
	{
		int best = 0;
		long at = -1;
		for (Map.Entry<String, Tier> e : tiers.entrySet())
		{
			if (e.getValue().last > at)
			{
				at = e.getValue().last;
				best = Integer.parseInt(e.getKey());
			}
		}
		return best;
	}

	Starlog repair()
	{
		Map<String, Tier> t = new HashMap<>();
		if (tiers != null)
		{
			tiers.forEach((k, v) ->
			{
				int size;
				try
				{
					size = k == null ? 0 : Integer.parseInt(k);
				}
				catch (NumberFormatException e)
				{
					size = 0;
				}
				if (size >= 1 && size <= Stars.MAX_SIZE && v != null)
				{
					t.put(Integer.toString(size), v.repair());
				}
			});
		}
		tiers = t;
		List<Find> f = new ArrayList<>();
		if (finds != null)
		{
			for (Find x : finds)
			{
				if (x != null && f.size() < FINDS)
				{
					f.add(x.repair());
				}
			}
		}
		finds = f;
		layers = Math.max(0, layers);
		layerTicks = layers == 0 ? 0 : Math.max(0, layerTicks);
		return this;
	}
}
