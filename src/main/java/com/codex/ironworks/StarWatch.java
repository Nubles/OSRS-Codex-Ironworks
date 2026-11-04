package com.codex.ironworks;

import com.codex.ironworks.core.Starlog;
import com.codex.ironworks.core.Stars;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Watches shooting stars, on the client thread: the crashed stars in the scene and their size as
 * it drops layer by layer, the stardust and Mining experience the player gets beside one, and what
 * the game says when they prospect a star or find one first. It only reads the game; it never
 * clicks, and it knows nothing of stars other players found.
 */
final class StarWatch
{
	/** Tiles from a star's spot that the player mines or prospects it from. */
	static final int REACH = 3;
	/** Tiles away a star still gets its plaque. */
	static final int NEAR = 15;
	/** Ticks after the last dust or prospect that the star's plaque stays up. */
	static final int COACH_TICKS = 200;
	/** Ticks a star may be missing before it is taken as gone: a layer swaps one object for the next. */
	static final int SWAP_TICKS = 2;

	/** One crashed star in the scene, as watched. */
	static final class Seen
	{
		final int x;
		final int y;
		final int plane;
		int size;
		/** The tick this size was first seen. */
		int since;
		/** Whether the layer was seen to start, so its age is known. */
		boolean sawStart;
		/** The share of the layer mined when last prospected, 0 to 100, and when; -1 when not. */
		int prospected = -1;
		int prospectTick;
		/** The tick it went missing, or -1: a missing star is kept a moment for the lines about it. */
		int missing = -1;

		Seen(int x, int y, int plane, int size, int tick)
		{
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.size = size;
			this.since = tick;
		}

		int distance(int px, int py)
		{
			return Math.max(Math.abs(x - px), Math.abs(y - py));
		}
	}

	/** What the panel and the plaque say about the star by the player. */
	static final class Now
	{
		int world;
		int x;
		int y;
		int plane;
		int size;
		int level;
		/** The share of this layer mined, 0 to 1, or -1 when not known. */
		double mined = -1;
		/** Estimated ticks left on this layer, or -1 when not known. */
		double leftTicks = -1;
		/** The share as the game last gave it, or -1; and ticks since. */
		int prospected = -1;
		int prospectAgo;
		/** Whether the layer time is the player's own, not the wiki's. */
		boolean own;
	}

	private int tick;
	private int lastActive = Integer.MIN_VALUE / 2;
	private final Map<String, Seen> scene = new HashMap<>();
	private double miningXp;
	private int xpTick = Integer.MIN_VALUE / 2;
	private Stars.Prospect prospect;
	private boolean first;
	private boolean gone;
	private Now now;

	/** A world hop, login or new account: nothing carries over. */
	void reset()
	{
		scene.clear();
		miningXp = 0;
		prospect = null;
		first = false;
		gone = false;
		now = null;
		lastActive = Integer.MIN_VALUE / 2;
	}

	/** The scene was loaded afresh: the stars in it are sent again. */
	void loaded()
	{
		scene.clear();
	}

	private static String key(int x, int y, int plane)
	{
		return x + "," + y + "," + plane;
	}

	private Seen at(int x, int y, int plane)
	{
		Seen s = scene.get(key(x, y, plane));
		if (s != null)
		{
			return s;
		}
		for (Seen o : scene.values())
		{
			if (o.plane == plane && o.distance(x, y) <= 1)
			{
				return o;
			}
		}
		return null;
	}

	/** An object appeared: a crashed star lands, comes into view, or drops to its next layer. */
	void spawned(int objectId, int x, int y, int plane, Starlog log)
	{
		int size = Stars.size(objectId);
		if (size == 0)
		{
			return;
		}
		Seen s = at(x, y, plane);
		if (s == null)
		{
			scene.put(key(x, y, plane), new Seen(x, y, plane, size, tick));
			return;
		}
		if (size < s.size)
		{
			// The layer was mined through. Timed only when its start was seen too.
			if (s.sawStart)
			{
				log.layer(tick - s.since);
			}
			s.sawStart = true;
			s.since = tick;
			s.prospected = -1;
		}
		else if (size > s.size)
		{
			// A new star where an old one was: start over.
			s.sawStart = false;
			s.since = tick;
			s.prospected = -1;
		}
		s.size = size;
		s.missing = -1;
	}

	/** An object went: a star's layer swapped out, it went out of view, or it crumbled. */
	void despawned(int objectId, int x, int y, int plane)
	{
		int size = Stars.size(objectId);
		Seen s = size == 0 ? null : at(x, y, plane);
		if (s != null && s.size == size)
		{
			s.missing = tick;
		}
	}

	/** Experience gained in a skill. */
	void xp(Skill skill, long gained)
	{
		if (skill == Skill.MINING && gained > 0)
		{
			miningXp += gained;
			xpTick = tick + 1;
		}
	}

	/** A game message, tags taken off. */
	void heard(String message)
	{
		if (message == null)
		{
			return;
		}
		Stars.Prospect p = Stars.prospect(message);
		if (p != null)
		{
			if (prospect != null)
			{
				// The size and the share may come as two lines on one tick.
				p = merge(prospect, p);
			}
			prospect = p;
		}
		else if (message.contains(Stars.FIRST))
		{
			first = true;
		}
		else if (message.contains(Stars.GONE))
		{
			gone = true;
		}
	}

	private static Stars.Prospect merge(Stars.Prospect a, Stars.Prospect b)
	{
		Stars.Prospect both = Stars.prospect("This is a size-" + (b.size > 0 ? b.size : a.size) + " star. It has been mined "
			+ (b.mined >= 0 ? b.mined : a.mined) + "% of the way to the next layer.");
		return both == null ? b : both;
	}

	/** The star the player stands by, within so many tiles, or null. */
	private Seen nearest(int px, int py, int plane, int within, boolean missing)
	{
		Seen best = null;
		for (Seen s : scene.values())
		{
			if (s.plane == plane && (missing || s.missing < 0) && s.distance(px, py) <= within && (best == null || s.distance(px, py) < best.distance(px, py)))
			{
				best = s;
			}
		}
		return best;
	}

	/**
	 * One tick.
	 *
	 * @param dust stardust that arrived in the pack this tick, from mining (0 while a bank is open)
	 * @return whether the star log changed
	 */
	boolean tick(long nowMs, int world, int px, int py, int plane, long dust, Starlog log)
	{
		tick++;
		boolean changed = false;
		for (Iterator<Seen> it = scene.values().iterator(); it.hasNext(); )
		{
			Seen s = it.next();
			if (s.missing >= 0 && tick - s.missing > SWAP_TICKS)
			{
				it.remove();
			}
		}
		Seen by = nearest(px, py, plane, REACH, true);
		if (by != null)
		{
			Starlog.Find find = null;
			if (dust > 0)
			{
				find = log.found(world, by.x, by.y, by.plane, by.size, nowMs);
				log.mined(find, by.size, dust, tick - xpTick <= 1 ? miningXp : 0, nowMs);
				lastActive = tick;
				changed = true;
			}
			if (prospect != null || first || gone)
			{
				find = find != null ? find : log.found(world, by.x, by.y, by.plane, by.size, nowMs);
				if (prospect != null && prospect.mined >= 0)
				{
					by.prospected = prospect.mined;
					by.prospectTick = tick;
				}
				if (first)
				{
					log.discovered(find);
				}
				if (gone)
				{
					log.gone(find);
				}
				lastActive = tick;
				changed = true;
			}
		}
		if (dust > 0 || tick - xpTick > 1)
		{
			miningXp = 0;
		}
		prospect = null;
		first = false;
		gone = false;
		Seen shown = tick - lastActive <= COACH_TICKS ? nearest(px, py, plane, NEAR, false) : null;
		now = shown == null ? null : describe(shown, world, log);
		return changed;
	}

	/** What to say about a star at this tick. */
	Now describe(Seen s, int world, Starlog log)
	{
		Now n = new Now();
		n.world = world;
		n.x = s.x;
		n.y = s.y;
		n.plane = s.plane;
		n.size = s.size;
		n.level = Stars.level(s.size);
		n.own = log.learnedLayer();
		double whole = log.layerTicks();
		double mined = -1;
		if (s.prospected >= 0)
		{
			n.prospected = s.prospected;
			n.prospectAgo = tick - s.prospectTick;
			mined = s.prospected / 100.0 + n.prospectAgo / whole;
		}
		else if (s.sawStart)
		{
			mined = (tick - s.since) / whole;
		}
		if (mined >= 0)
		{
			n.mined = Math.min(1, mined);
			n.leftTicks = Math.max(0, (1 - mined) * whole);
		}
		return n;
	}

	/** The star by the player while they mine or prospect it, or null. */
	Now now()
	{
		return now;
	}
}
