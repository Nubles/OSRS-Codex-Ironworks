package com.codex.ironworks;

import com.codex.ironworks.core.Chop;
import com.codex.ironworks.core.Trees;
import com.codex.ironworks.core.Woodcuts;
import com.codex.ironworks.overlay.TreeCoach;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;

/**
 * Watches the tree the player is chopping, once a tick, on the client thread: the tree is the one
 * the player faces while swinging an axe, and the people chopping it are everyone swinging an axe
 * while facing one of its tiles, the player among them. It only reads the scene; it never clicks.
 */
final class TreeWatch
{
	/** Woodcutting animations, as RuneLite's own Woodcutting plugin knows them (gameval AnimationID). */
	static final Set<Integer> CHOPPING = new HashSet<>(Arrays.asList(
		AnimationID.HUMAN_WOODCUTTING_BRONZE_AXE, AnimationID.HUMAN_WOODCUTTING_IRON_AXE, AnimationID.HUMAN_WOODCUTTING_STEEL_AXE,
		AnimationID.HUMAN_WOODCUTTING_BLACK_AXE, AnimationID.HUMAN_WOODCUTTING_MITHRIL_AXE, AnimationID.HUMAN_WOODCUTTING_ADAMANT_AXE,
		AnimationID.HUMAN_WOODCUTTING_RUNE_AXE, AnimationID.HUMAN_WOODCUTTING_GILDED_AXE, AnimationID.HUMAN_WOODCUTTING_DRAGON_AXE,
		AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_AXE_NO_INFERNAL, AnimationID.HUMAN_WOODCUTTING_INFERNAL_AXE,
		AnimationID.HUMAN_WOODCUTTING_3A_AXE, AnimationID.HUMAN_WOODCUTTING_CRYSTAL_AXE,
		AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_RELOADED_AXE_NO_INFERNAL, AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_AXE,
		AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_RELOADED_AXE, AnimationID.FORESTRY_2H_AXE_CHOPPING_BRONZE,
		AnimationID.FORESTRY_2H_AXE_CHOPPING_IRON, AnimationID.FORESTRY_2H_AXE_CHOPPING_STEEL, AnimationID.FORESTRY_2H_AXE_CHOPPING_BLACK,
		AnimationID.FORESTRY_2H_AXE_CHOPPING_MITHRIL, AnimationID.FORESTRY_2H_AXE_CHOPPING_ADAMANT,
		AnimationID.FORESTRY_2H_AXE_CHOPPING_RUNE, AnimationID.FORESTRY_2H_AXE_CHOPPING_DRAGON,
		AnimationID.FORESTRY_2H_AXE_CHOPPING_CRYSTAL, AnimationID.FORESTRY_2H_AXE_CHOPPING_CRYSTAL_INACTIVE,
		AnimationID.FORESTRY_2H_AXE_CHOPPING_3A));

	/** Ticks the player may stand away from a tree before the coach lets it go. */
	static final int IDLE_TICKS = 50;
	/** Tiles away from a tree the coach lets it go at. */
	static final int AWAY = 15;

	private final Client client;
	private final TreeCoach coach;

	private Chop chop;
	private int objectId;
	private WorldPoint min;
	private int sizeX;
	private int sizeY;
	private int idle;

	TreeWatch(Client client, TreeCoach coach)
	{
		this.client = client;
		this.coach = coach;
	}

	void reset()
	{
		chop = null;
		coach.show(null);
	}

	/**
	 * One tick.
	 *
	 * @param cut whether Woodcutting experience came this tick
	 * @param log the account's woodcutting log, told of each tree felled
	 * @param on whether the coach is shown
	 */
	void tick(boolean cut, Woodcuts log, boolean on)
	{
		WorldView wv = client.getTopLevelWorldView();
		Player me = client.getLocalPlayer();
		if (wv == null || me == null)
		{
			reset();
			return;
		}
		boolean swinging = CHOPPING.contains(me.getAnimation());
		if (swinging)
		{
			GameObject faced = tree(wv, front(me));
			if (faced != null && (chop == null || chop.isFelled() || faced.getId() != objectId || !minOf(wv, faced).equals(min)))
			{
				Trees.Kind kind = kind(faced.getId());
				chop = new Chop(kind, Trees.farmed(resolved(faced.getId())));
				objectId = faced.getId();
				min = minOf(wv, faced);
				sizeX = Math.max(1, faced.sizeX());
				sizeY = Math.max(1, faced.sizeY());
			}
		}
		if (chop == null)
		{
			coach.show(null);
			return;
		}
		WorldPoint at = me.getWorldLocation();
		if (at == null || at.getPlane() != min.getPlane() || at.distanceTo2D(min) > AWAY)
		{
			reset();
			return;
		}
		if (!chop.isFelled() && standing(wv) == null)
		{
			if (!wv.contains(min))
			{
				reset();
				return;
			}
			chop.fell();
			log.felled(chop.kind, chop.whole() && chop.kind.timed() && !chop.farmed ? chop.getGone() : -1);
		}
		int choppers = 0;
		boolean self = false;
		for (Player p : wv.players())
		{
			if (p == null || !CHOPPING.contains(p.getAnimation()) || !faces(p))
			{
				continue;
			}
			choppers++;
			if (p == me)
			{
				self = true;
			}
		}
		chop.tick(choppers, self ? choppers - 1 : choppers, cut && self);
		idle = self ? 0 : idle + 1;
		if ((chop.isFelled() && chop.regrowLeft() == 0) || (!chop.isFelled() && idle > IDLE_TICKS))
		{
			reset();
			return;
		}
		if (!on)
		{
			coach.show(null);
			return;
		}
		LocalPoint centre = LocalPoint.fromWorld(wv, min.getX(), min.getY());
		if (centre != null)
		{
			centre = new LocalPoint(centre.getX() + (sizeX - 1) * 64, centre.getY() + (sizeY - 1) * 64, wv);
		}
		coach.show(TreeCoach.describe(chop, self, centre, min.getPlane()));
	}

	/** The tile an actor faces, by its orientation to the nearest of eight directions (0 south, 512 west). */
	static int[] step(int orientation)
	{
		int o = ((orientation % 2048) + 2048) % 2048;
		int dir = ((o + 128) / 256) % 8;
		int[][] steps = {{0, -1}, {-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}};
		return steps[dir];
	}

	private static WorldPoint front(Player p)
	{
		WorldPoint at = p.getWorldLocation();
		if (at == null)
		{
			return null;
		}
		int[] s = step(p.getOrientation());
		return at.dx(s[0]).dy(s[1]);
	}

	/** Whether a tile is in an area of tiles: the tree's, from its south-west corner. */
	static boolean inside(int x, int y, int minX, int minY, int sizeX, int sizeY)
	{
		return x >= minX && x < minX + sizeX && y >= minY && y < minY + sizeY;
	}

	private boolean faces(Player p)
	{
		WorldPoint f = front(p);
		return f != null && f.getPlane() == min.getPlane() && inside(f.getX(), f.getY(), min.getX(), min.getY(), sizeX, sizeY);
	}

	private static WorldPoint minOf(WorldView wv, GameObject o)
	{
		Point m = o.getSceneMinLocation();
		return WorldPoint.fromScene(wv, m.getX(), m.getY(), o.getPlane());
	}

	private static Tile tile(WorldView wv, WorldPoint w)
	{
		if (w == null)
		{
			return null;
		}
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
		return tiles[plane][x][y];
	}

	/** The tree on a tile: a game object whose name is a tree Ironworks knows. */
	private GameObject tree(WorldView wv, WorldPoint w)
	{
		Tile t = tile(wv, w);
		if (t == null || t.getGameObjects() == null)
		{
			return null;
		}
		for (GameObject o : t.getGameObjects())
		{
			if (o != null && kind(o.getId()) != null)
			{
				return o;
			}
		}
		return null;
	}

	/** The tracked tree, while it still stands on its tile. */
	private GameObject standing(WorldView wv)
	{
		Tile t = tile(wv, min);
		if (t == null || t.getGameObjects() == null)
		{
			return null;
		}
		for (GameObject o : t.getGameObjects())
		{
			if (o != null && o.getId() == objectId)
			{
				return o;
			}
		}
		return null;
	}

	private ObjectComposition composition(int id)
	{
		ObjectComposition c = client.getObjectDefinition(id);
		if (c != null && c.getImpostorIds() != null)
		{
			ObjectComposition i = c.getImpostor();
			if (i != null)
			{
				return i;
			}
		}
		return c;
	}

	private int resolved(int id)
	{
		ObjectComposition c = composition(id);
		return c == null ? id : c.getId();
	}

	private Trees.Kind kind(int id)
	{
		ObjectComposition c = composition(id);
		return c == null ? null : Trees.byName(c.getName());
	}
}
