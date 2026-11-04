package com.codex.ironworks;

import com.codex.ironworks.core.Fish;
import com.codex.ironworks.core.Fishing;
import com.codex.ironworks.overlay.MineCoach;
import com.codex.ironworks.ui.Theme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.game.FishingSpot;

/**
 * Watches fishing, on the client thread: the tool the player fishes with (by animation), the fish
 * and extras that arrive while they fish, and the fishing spots near them, to estimate when the one
 * they fish at will move. It only reads the game; it never clicks.
 *
 * <p>A fish counts when it arrives while the player is seen fishing (a fishing animation within
 * {@link #CONTEXT_TICKS}) or with Fishing experience; aerial fish, which only aerial fishing gives
 * and which show no fishing animation, count whenever they arrive outside the bank. A catch that went
 * into an open fish barrel never reaches the pack, so it is taken from the catch message instead.
 */
final class FishWatch
{
	/** Ticks after the last fishing animation that what arrives still counts as caught. */
	static final int CONTEXT_TICKS = 3;
	/** Ticks after an aerial catch that pearls and the golden tench still count as from it. */
	static final int AERIAL_TICKS = 2;
	/** Ticks after the last catch the plaque stays up. */
	static final int COACH_TICKS = 100;
	/** A spot that appears this close to the player has just arrived there; farther, it may have come into view. */
	static final int ARRIVED_NEAR = 8;
	/** A spot that leaves this close to the player moved; farther, it may only have gone out of view. */
	static final int LEFT_NEAR = 12;

	/** A fishing spot near the player, as first seen. */
	static final class Seen
	{
		final String label;
		final Fish.Stay stay;
		int x;
		int y;
		int plane;
		int since;
		/** Whether its arrival here was seen, so its time here is known rather than at least so long. */
		boolean known;

		Seen(String label, Fish.Stay stay, int x, int y, int plane, int since, boolean known)
		{
			this.label = label;
			this.stay = stay;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.since = since;
			this.known = known;
		}
	}

	private final Client client;
	private final MineCoach coach;
	private final Map<Integer, Seen> spots = new HashMap<>();
	private final List<Fish.Kind> heard = new ArrayList<>();

	private int tick;
	private int loadedAt = Integer.MIN_VALUE / 2;
	private int lastAnim = Integer.MIN_VALUE / 2;
	private int lastXp = Integer.MIN_VALUE / 2;
	private int lastAerial = Integer.MIN_VALUE / 2;
	private int lastCatch = Integer.MIN_VALUE / 2;
	private Fish.Tool tool;

	FishWatch(Client client, MineCoach coach)
	{
		this.client = client;
		this.coach = coach;
	}

	void reset()
	{
		spots.clear();
		heard.clear();
		tool = null;
		coach.show(null);
	}

	/** The scene was loaded afresh: spots seen now were already there. */
	void loaded()
	{
		loadedAt = tick;
		spots.clear();
	}

	/** Experience gained in a skill. */
	void xp(Skill skill, long gained)
	{
		if (skill == Skill.FISHING && gained > 0)
		{
			lastXp = tick + 1;
		}
	}

	/** A game message, tags taken off. */
	void heard(String message)
	{
		Fish.Kind k = Fish.fromMessage(message);
		if (k != null)
		{
			heard.add(k);
		}
	}

	/**
	 * One tick.
	 *
	 * @param before what the player carried last tick, by item id; null on the first
	 * @param now what they carry now
	 * @param bank whether a bank or other storage is open, so what arrives was withdrawn
	 * @return whether the fishing log changed
	 */
	boolean tick(Map<Integer, Long> before, Map<Integer, Long> now, boolean bank, Fishing log, long nowMs, boolean on)
	{
		tick++;
		WorldView wv = client.getTopLevelWorldView();
		Player me = client.getLocalPlayer();
		if (wv == null || me == null)
		{
			heard.clear();
			coach.show(null);
			return false;
		}
		Fish.Tool seen = Fish.tool(me.getAnimation());
		if (seen != null)
		{
			lastAnim = tick;
			tool = seen;
		}
		boolean fishing = tick - lastAnim <= CONTEXT_TICKS || tick - lastXp <= 1;
		boolean aerial = tick - lastAerial <= AERIAL_TICKS;
		Result r = null;
		if (before != null && !bank)
		{
			r = gains(before, now, fishing, aerial, tick == lastXp, tool, heard, nowMs, log);
			if (r.aerial)
			{
				lastAerial = tick;
			}
			if (r.fish > 0)
			{
				lastCatch = tick;
			}
		}
		heard.clear();
		WorldPoint at = me.getWorldLocation();
		if (at != null)
		{
			follow(wv, at, log);
		}
		Seen fished = fishedSpot(me);
		if (!on || at == null || fished == null || tick - Math.max(lastCatch, lastAnim) > COACH_TICKS)
		{
			coach.show(null);
		}
		else
		{
			LocalPoint lp = LocalPoint.fromWorld(wv, fished.x, fished.y);
			coach.show(lp == null ? null : Collections.singletonList(describe(fished, tick, lp)));
		}
		return r != null && r.changed;
	}

	/** What one tick brought. */
	static final class Result
	{
		boolean changed;
		long fish;
		/** Whether an aerial fish came, so the pearls after it are its. */
		boolean aerial;
	}

	/**
	 * What arrived while fishing, into the log: fish by the method they come from and the tool seen,
	 * and clue bottles, caskets, big fish, pearls and golden tench as finds. With no fish in the pack
	 * but Fishing experience and an open fish barrel, the fish the catch messages named are the catch.
	 */
	static Result gains(Map<Integer, Long> before, Map<Integer, Long> now, boolean fishing, boolean aerialRecent, boolean xpNow,
		Fish.Tool tool, List<Fish.Kind> heard, long nowMs, Fishing log)
	{
		Result r = new Result();
		List<String> extras = new ArrayList<>();
		List<Long> extraUnits = new ArrayList<>();
		for (Map.Entry<Integer, Long> e : now.entrySet())
		{
			long got = e.getValue() - before.getOrDefault(e.getKey(), 0L);
			if (got <= 0)
			{
				continue;
			}
			Fish.Kind k = Fish.byItem(e.getKey());
			if (k != null && (fishing || k.method == Fish.AERIAL))
			{
				log.caught(k, Fish.toolFor(k.method, tool), got, nowMs);
				r.fish += got;
				r.aerial |= k.method == Fish.AERIAL;
				r.changed = true;
				continue;
			}
			String extra = Fish.extra(e.getKey());
			if (extra != null)
			{
				extras.add(extra);
				extraUnits.add(got);
			}
		}
		if (r.fish == 0 && xpNow && now.getOrDefault(Fish.BARREL_OPEN, 0L) > 0)
		{
			for (Fish.Kind k : heard)
			{
				if (k.method != Fish.AERIAL)
				{
					log.caught(k, Fish.toolFor(k.method, tool), 1, nowMs);
					r.fish++;
					r.changed = true;
				}
			}
		}
		if (fishing || r.aerial || aerialRecent)
		{
			for (int i = 0; i < extras.size(); i++)
			{
				log.found(extras.get(i), extraUnits.get(i));
				r.changed = true;
			}
		}
		return r;
	}

	/** Keeps up with the fishing spots near the player: where each stands and since when. */
	private void follow(WorldView wv, WorldPoint me, Fishing log)
	{
		boolean fresh = tick - loadedAt <= 2;
		Map<Integer, Boolean> present = new HashMap<>();
		for (NPC n : wv.npcs())
		{
			if (n == null)
			{
				continue;
			}
			Fish.Stay stay = stay(n.getId());
			WorldPoint w = n.getWorldLocation();
			if (stay == null || w == null)
			{
				continue;
			}
			present.put(n.getIndex(), true);
			Seen s = spots.get(n.getIndex());
			boolean near = w.getPlane() == me.getPlane() && Math.max(Math.abs(w.getX() - me.getX()), Math.abs(w.getY() - me.getY())) <= ARRIVED_NEAR;
			if (s == null)
			{
				spots.put(n.getIndex(), new Seen(label(n.getId()), stay, w.getX(), w.getY(), w.getPlane(), tick, !fresh && near));
			}
			else if (s.x != w.getX() || s.y != w.getY() || s.plane != w.getPlane())
			{
				// The spot moved without leaving the scene: its stay here is over, and its new one starts.
				if (s.known)
				{
					log.stayed(s.label, tick - s.since);
				}
				s.x = w.getX();
				s.y = w.getY();
				s.plane = w.getPlane();
				s.since = tick;
				s.known = true;
			}
		}
		for (Iterator<Map.Entry<Integer, Seen>> it = spots.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<Integer, Seen> e = it.next();
			if (present.containsKey(e.getKey()))
			{
				continue;
			}
			Seen s = e.getValue();
			boolean near = s.plane == me.getPlane() && Math.max(Math.abs(s.x - me.getX()), Math.abs(s.y - me.getY())) <= LEFT_NEAR;
			if (s.known && near && !fresh)
			{
				log.stayed(s.label, tick - s.since);
			}
			it.remove();
		}
	}

	/** The spot the player is fishing at, or null. */
	private Seen fishedSpot(Player me)
	{
		Actor a = me.getInteracting();
		if (!(a instanceof NPC))
		{
			return null;
		}
		return spots.get(((NPC) a).getIndex());
	}

	/** Boat spots, which the wiki says never move. */
	private static boolean boat(int npcId)
	{
		return npcId == NpcID.FISHING_BOAT_SALTFISH || npcId == NpcID.FISHING_BOAT_MEMBERFISH || npcId == NpcID.FISHING_BOAT_RAREFISH
			|| npcId == NpcID.FISHING_BOAT_KARAMBWANFISH || npcId == NpcID.FISHING_BOAT_PISCARILIUSFISH || npcId == NpcID.FISHING_BOAT_MONKFISH;
	}

	/**
	 * How long a spot of this NPC stays put, by RuneLite's own list of fishing spot NPCs; null for the
	 * spots the wiki gives no stay for here (aerial, minnows, Tempoross, quest spots and the like).
	 */
	static Fish.Stay stay(int npcId)
	{
		if (boat(npcId))
		{
			return Fish.Stay.NEVER;
		}
		FishingSpot s = FishingSpot.findSpot(npcId);
		if (s == null)
		{
			return null;
		}
		switch (s)
		{
			case SACRED_EEL:
			case INFERNAL_EEL:
				return Fish.Stay.EELS;
			case ANGLERFISH:
				return Fish.Stay.ANGLERFISH;
			case SHRIMP:
			case LOBSTER:
			case SHARK:
			case SALMON:
			case MONKFISH:
			case BARB_FISH:
			case KARAMBWAN:
			case DARK_CRAB:
			case LAVA_EEL:
			case CAVE_EEL:
			case SLIMY_EEL:
			case KARAMBWANJI:
				return Fish.Stay.MOST;
			default:
				return null;
		}
	}

	/** A spot's kind as the wiki names spots: "Cage/Harpoon". */
	static String label(int npcId)
	{
		if (boat(npcId))
		{
			return "Boat spot";
		}
		FishingSpot s = FishingSpot.findSpot(npcId);
		if (s == null)
		{
			return "Fishing spot";
		}
		switch (s)
		{
			case SHRIMP:
				return "Net/Bait";
			case LOBSTER:
				return "Cage/Harpoon";
			case SHARK:
				return "Big Net/Harpoon";
			case SALMON:
				return "Lure/Bait";
			case BARB_FISH:
				return "Use-rod";
			case MONKFISH:
				return "Monkfish";
			case KARAMBWAN:
				return "Karambwan";
			case ANGLERFISH:
				return "Anglerfish";
			case DARK_CRAB:
				return "Dark crab";
			case SACRED_EEL:
				return "Sacred eel";
			case INFERNAL_EEL:
				return "Infernal eel";
			default:
				return "Fishing spot";
		}
	}

	/** What the plaque over the spot being fished says at this tick. */
	static MineCoach.Shown describe(Seen s, int tick, LocalPoint at)
	{
		if (s.stay == Fish.Stay.NEVER)
		{
			return new MineCoach.Shown(at, s.plane, s.label, "Stays", "boat spots never move (wiki)", Theme.GREEN, -1);
		}
		int here = Math.max(0, tick - s.since);
		int min = s.stay.minTicks;
		int max = s.stay.maxTicks;
		double share = Math.min(1, here / (double) max);
		if (here >= max)
		{
			return new MineCoach.Shown(at, s.plane, s.label, "Due", "past the wiki's longest stay", Theme.TEAL, 0);
		}
		if (!s.known)
		{
			return new MineCoach.Shown(at, s.plane, s.label, "by ~" + MineCoach.clock(max - here),
				"here " + MineCoach.clock(here) + "+, est., wiki", Theme.GOLD_BRIGHT, 1 - share);
		}
		if (here < min)
		{
			return new MineCoach.Shown(at, s.plane, s.label, "~" + MineCoach.clock(min - here) + "-" + MineCoach.clock(max - here),
				"until it moves, est., wiki", Theme.GREEN, 1 - share);
		}
		return new MineCoach.Shown(at, s.plane, s.label, "by ~" + MineCoach.clock(max - here), "may move any moment, est.",
			Theme.GOLD_BRIGHT, 1 - share);
	}
}
