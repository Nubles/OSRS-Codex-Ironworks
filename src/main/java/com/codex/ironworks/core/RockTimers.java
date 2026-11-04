package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The rocks seen mined near the player, and when each should be back. A rock is seen mined when a
 * rock with ore leaves its tile outside a scene load and no rock of its kind takes the tile on the
 * same tick (the mined-out rock that does take it is something else); it is seen back when a rock
 * of its kind takes the tile again. The game never says when a rock comes back, so the
 * time left is the wiki's figure counted down, or the player's own once enough comebacks have been
 * seen at that place: always an estimate.
 *
 * <p>Pure, so it can be driven tick by tick in a test; the plugin feeds it the object events.
 */
public final class RockTimers
{
	/** One mined rock waiting to come back. */
	public static final class Depleted
	{
		public final Rocks.Kind kind;
		public final int x;
		public final int y;
		public final int plane;
		public final int region;
		/** The tick it was seen mined. */
		public final int tick;

		Depleted(Rocks.Kind kind, int x, int y, int plane, int region, int tick)
		{
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.region = region;
			this.tick = tick;
		}
	}

	private final Map<Long, Rocks.Kind> gone = new HashMap<>();
	private final Map<Long, Integer> goneRegion = new HashMap<>();
	private final Map<Long, Rocks.Kind> back = new HashMap<>();
	private final Map<Long, Depleted> depleted = new LinkedHashMap<>();
	/** Whether the scene was loaded since the last settle, so what spawned was the load, not the game. */
	private boolean loaded;

	static long key(int x, int y, int plane)
	{
		return ((long) plane << 40) | ((long) (x & 0xFFFFF) << 20) | (y & 0xFFFFF);
	}

	/** A rock with ore left its tile. */
	public void gone(Rocks.Kind kind, int x, int y, int plane, int region)
	{
		if (kind != null)
		{
			gone.put(key(x, y, plane), kind);
			goneRegion.put(key(x, y, plane), region);
		}
	}

	/** Something took a tile: a rock with ore when kind is known; anything else (null) is left be. */
	public void spawned(Rocks.Kind kind, int x, int y, int plane)
	{
		if (kind != null)
		{
			back.put(key(x, y, plane), kind);
		}
	}

	/** The scene was loaded afresh: the next tick's spawns are the load, not rocks mined or back. */
	public void loaded()
	{
		loaded = true;
	}

	/**
	 * Settles one tick's events.
	 *
	 * @param log told of each comeback seen whole
	 */
	public void settle(int tick, Mines log)
	{
		if (!loaded)
		{
			for (Map.Entry<Long, Rocks.Kind> e : gone.entrySet())
			{
				long k = e.getKey();
				if (!back.containsKey(k))
				{
					int plane = (int) (k >>> 40);
					int x = (int) ((k >>> 20) & 0xFFFFF);
					int y = (int) (k & 0xFFFFF);
					depleted.put(k, new Depleted(e.getValue(), x, y, plane, goneRegion.getOrDefault(k, -1), tick));
				}
			}
		}
		for (Map.Entry<Long, Rocks.Kind> e : back.entrySet())
		{
			Depleted d = depleted.get(e.getKey());
			if (d != null && d.kind == e.getValue() && !gone.containsKey(e.getKey()))
			{
				depleted.remove(e.getKey());
				if (!loaded && log != null)
				{
					log.cameBack(d.kind, d.region, tick - d.tick);
				}
			}
		}
		gone.clear();
		goneRegion.clear();
		back.clear();
		loaded = false;
	}

	/** Lets go of rocks long overdue (likely back out of sight) and far from where the player stands. */
	public void expire(int tick, Mines log, int px, int py, int plane, int away)
	{
		depleted.values().removeIf(d ->
			tick - d.tick > Math.max(100, (int) (estimate(d, log) * 2))
				|| d.plane != plane || Math.max(Math.abs(d.x - px), Math.abs(d.y - py)) > away);
	}

	/** The ticks a rock takes to come back as best known. */
	public static double estimate(Depleted d, Mines log)
	{
		return log == null ? d.kind.respawnTicks : log.respawnTicks(d.kind, d.region);
	}

	/** Ticks left until it should be back; below zero when past the estimate. */
	public static double left(Depleted d, int tick, Mines log)
	{
		return estimate(d, log) - (tick - d.tick);
	}

	public List<Depleted> depleted()
	{
		return Collections.unmodifiableList(new ArrayList<>(depleted.values()));
	}

	public void clear()
	{
		depleted.clear();
		gone.clear();
		goneRegion.clear();
		back.clear();
	}
}
