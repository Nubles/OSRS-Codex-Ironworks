package com.codex.ironworks;

import com.codex.ironworks.core.Mines;
import com.codex.ironworks.core.Pickaxes;
import com.codex.ironworks.core.RockTimers;
import com.codex.ironworks.core.Rocks;
import com.codex.ironworks.overlay.MineCoach;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Watches mining, on the client thread: the pickaxe the player swings (by animation), the rock they
 * face, the ore, gems and clues that arrive while they mine, and the rocks near them that are mined
 * out and come back. It only reads the game; it never clicks.
 */
final class RockWatch
{
	/** Ticks after the last swing that what arrives still counts as mined. */
	static final int MINED_TICKS = 2;
	/** Ticks after the last swing the coach stays up. */
	static final int COACH_TICKS = 500;
	/** Tiles away a mined rock gets a plaque within. */
	static final int NEAR = 15;
	/** Tiles away a mined rock is let go at. */
	static final int AWAY = 40;

	private final Client client;
	private final MineCoach coach;
	private final RockTimers timers = new RockTimers();

	private int tick;
	private int lastSwing = Integer.MIN_VALUE / 2;
	private String pickaxe;
	private Rocks.Kind faced;

	RockWatch(Client client, MineCoach coach)
	{
		this.client = client;
		this.coach = coach;
	}

	void reset()
	{
		timers.clear();
		faced = null;
		coach.show(null);
	}

	/** The scene was loaded afresh. */
	void loaded()
	{
		timers.loaded();
	}

	void despawned(TileObject o)
	{
		Rocks.Kind k = kind(o.getId());
		WorldPoint w = o.getWorldLocation();
		if (k != null && w != null)
		{
			timers.gone(k, w.getX(), w.getY(), w.getPlane(), w.getRegionID());
		}
	}

	void spawned(TileObject o)
	{
		WorldPoint w = o.getWorldLocation();
		if (w != null)
		{
			timers.spawned(kind(o.getId()), w.getX(), w.getY(), w.getPlane());
		}
	}

	/**
	 * One tick.
	 *
	 * @param before what the player carried last tick, by item id; null on the first
	 * @param now what they carry now
	 * @param bank whether a bank or other storage is open, so what arrives was withdrawn
	 * @return whether the mining log changed
	 */
	boolean tick(Map<Integer, Long> before, Map<Integer, Long> now, boolean bank, long nowMs, Mines log, boolean on)
	{
		tick++;
		WorldView wv = client.getTopLevelWorldView();
		Player me = client.getLocalPlayer();
		if (wv == null || me == null)
		{
			coach.show(null);
			return false;
		}
		String swung = Pickaxes.of(me.getAnimation());
		if (swung != null)
		{
			lastSwing = tick;
			pickaxe = swung;
			Rocks.Kind k = facedRock(wv, me);
			if (k != null)
			{
				faced = k;
			}
		}
		boolean changed = false;
		if (before != null && !bank && tick - lastSwing <= MINED_TICKS && faced != null)
		{
			changed = gains(before, now, faced, pickaxe, nowMs, log);
		}
		long comebacks = log.comebacksSeen();
		timers.settle(tick, log);
		changed |= log.comebacksSeen() != comebacks;
		WorldPoint at = me.getWorldLocation();
		if (at != null)
		{
			timers.expire(tick, log, at.getX(), at.getY(), at.getPlane(), AWAY);
		}
		if (!on || at == null || tick - lastSwing > COACH_TICKS)
		{
			coach.show(null);
			return changed;
		}
		List<MineCoach.Shown> shown = new ArrayList<>();
		for (RockTimers.Depleted d : timers.depleted())
		{
			if (d.plane != at.getPlane() || Math.max(Math.abs(d.x - at.getX()), Math.abs(d.y - at.getY())) > NEAR)
			{
				continue;
			}
			LocalPoint lp = LocalPoint.fromWorld(wv, d.x, d.y);
			if (lp != null)
			{
				shown.add(MineCoach.describe(d, tick, log, lp));
			}
		}
		coach.show(shown);
		return changed;
	}

	/**
	 * What arrived while mining, into the log: ore by the rock it comes from, gems as the ore of gem
	 * rocks or as finds from any other, and clue geodes and scroll boxes as finds.
	 */
	static boolean gains(Map<Integer, Long> before, Map<Integer, Long> now, Rocks.Kind faced, String pickaxe, long nowMs, Mines log)
	{
		boolean changed = false;
		for (Map.Entry<Integer, Long> e : now.entrySet())
		{
			long got = e.getValue() - before.getOrDefault(e.getKey(), 0L);
			if (got <= 0)
			{
				continue;
			}
			int id = e.getKey();
			Rocks.Kind ore = Rocks.byOre(id);
			String gem = Rocks.gem(id);
			String clue = Rocks.clue(id);
			if (ore != null)
			{
				log.mined(ore, pickaxe, got, nowMs);
				changed = true;
			}
			else if (gem != null && faced == Rocks.GEM_ROCKS)
			{
				log.mined(Rocks.GEM_ROCKS, pickaxe, got, nowMs);
				log.found(gem, got);
				changed = true;
			}
			else if (gem != null || clue != null)
			{
				log.found(gem != null ? gem : clue, got);
				changed = true;
			}
		}
		return changed;
	}

	/** The rock the player faces, or stands by for a rock set in a wall. */
	private Rocks.Kind facedRock(WorldView wv, Player me)
	{
		WorldPoint at = me.getWorldLocation();
		if (at == null)
		{
			return null;
		}
		int[] s = TreeWatch.step(me.getOrientation());
		Rocks.Kind k = rockOn(wv, at.dx(s[0]).dy(s[1]));
		return k != null ? k : rockOn(wv, at);
	}

	private Rocks.Kind rockOn(WorldView wv, WorldPoint w)
	{
		LocalPoint lp = LocalPoint.fromWorld(wv, w);
		Scene scene = wv.getScene();
		if (lp == null || scene == null)
		{
			return null;
		}
		Tile[][][] tiles = scene.getTiles();
		int plane = w.getPlane();
		int x = lp.getSceneX();
		int y = lp.getSceneY();
		if (plane < 0 || plane >= tiles.length || x < 0 || x >= tiles[plane].length || y < 0 || y >= tiles[plane][x].length)
		{
			return null;
		}
		Tile t = tiles[plane][x][y];
		if (t == null)
		{
			return null;
		}
		if (t.getGameObjects() != null)
		{
			for (GameObject o : t.getGameObjects())
			{
				Rocks.Kind k = o == null ? null : kind(o.getId());
				if (k != null)
				{
					return k;
				}
			}
		}
		WallObject wall = t.getWallObject();
		return wall == null ? null : kind(wall.getId());
	}

	private Rocks.Kind kind(int id)
	{
		ObjectComposition c = client.getObjectDefinition(id);
		if (c != null && c.getImpostorIds() != null)
		{
			ObjectComposition i = c.getImpostor();
			if (i != null)
			{
				c = i;
			}
		}
		return c == null ? null : Rocks.byName(c.getName());
	}
}
