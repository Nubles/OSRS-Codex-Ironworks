package com.codex.ironworks;

import com.codex.ironworks.core.Shoals;
import com.codex.ironworks.core.Trawls;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Watches deep sea trawling, on the client thread: the fish that come up in the net, the Fishing
 * and Sailing experience that comes with them, into the player's own trawling log. It only reads
 * the game; it never clicks, and it gives no cue to act on.
 *
 * <p>Fish land in the boat's trawling net, not the pack (OSRS Wiki, Deep sea trawling), so they are
 * read from the net's own item container when the game sends it, or else from the catch message.
 * A catch only counts while the net is rolling: "Each time a small amount of Sailing XP is awarded
 * indicates a skilling roll" (OSRS Wiki), so a fish named in a message with no Sailing experience
 * near it, such as a haddock caught with a rod, is not a trawled one.
 */
final class TrawlWatch
{
	/** Ticks after a Sailing experience drop that the net still counts as rolling. */
	static final int ROLLING_TICKS = 10;
	/**
	 * More fish than this arriving in the net on one tick is the net being sent whole (on login, or on
	 * opening it), not a catch: the wiki's best net brings up 5 a catch.
	 */
	static final long MOST_A_TICK = 30;

	private int tick;
	private int lastSail = Integer.MIN_VALUE / 2;
	private double fishingXp;
	private double sailingXp;
	/** What the net held when last sent, by item id; null until it has been sent once. */
	private Map<Integer, Long> net;
	/** Whether the net's container has shown a catch, so messages are not counted on top of it. */
	private boolean netSeen;
	private final Map<Shoals.Kind, Long> fromNet = new HashMap<>();
	private final Map<Shoals.Kind, Long> fromChat = new HashMap<>();

	/** A world hop, login or new account: the net is read afresh and nothing carries over. */
	void reset()
	{
		net = null;
		netSeen = false;
		fromNet.clear();
		fromChat.clear();
		fishingXp = 0;
		sailingXp = 0;
		lastSail = Integer.MIN_VALUE / 2;
	}

	/** Experience gained in a skill. */
	void xp(Skill skill, long gained)
	{
		if (gained <= 0)
		{
			return;
		}
		if (skill == Skill.SAILING)
		{
			sailingXp += gained;
			lastSail = tick + 1;
		}
		else if (skill == Skill.FISHING)
		{
			fishingXp += gained;
		}
	}

	/** A game message, tags taken off. */
	void heard(String message)
	{
		Trawls.Catch c = Trawls.parse(message);
		if (c != null)
		{
			fromChat.merge(c.kind, c.units, Long::sum);
		}
	}

	/** The trawling net's contents, by item id, as the game sent them. */
	void net(Map<Integer, Long> contents)
	{
		Map<Integer, Long> was = net;
		net = new HashMap<>(contents);
		if (was == null)
		{
			return;
		}
		Map<Shoals.Kind, Long> got = new HashMap<>();
		long total = 0;
		for (Map.Entry<Integer, Long> e : contents.entrySet())
		{
			Shoals.Kind k = Shoals.byItem(e.getKey());
			long more = e.getValue() - was.getOrDefault(e.getKey(), 0L);
			if (k != null && more > 0)
			{
				got.merge(k, more, Long::sum);
				total += more;
			}
		}
		if (total == 0 || total > MOST_A_TICK)
		{
			return;
		}
		got.forEach((k, u) -> fromNet.merge(k, u, Long::sum));
	}

	/**
	 * One tick: the fish that came up since the last, into the log with the experience since the last
	 * catch.
	 *
	 * @return whether the log changed
	 */
	boolean tick(long nowMs, Trawls log)
	{
		tick++;
		boolean rolling = tick - lastSail <= ROLLING_TICKS;
		Map<Shoals.Kind, Long> fish;
		if (!fromNet.isEmpty())
		{
			netSeen = true;
			fish = new HashMap<>(fromNet);
		}
		else
		{
			fish = netSeen ? null : new HashMap<>(fromChat);
		}
		fromNet.clear();
		fromChat.clear();
		if (fish == null || fish.isEmpty())
		{
			return false;
		}
		if (!rolling)
		{
			fishingXp = 0;
			sailingXp = 0;
			return false;
		}
		log.caught(fish, fishingXp, sailingXp, nowMs);
		fishingXp = 0;
		sailingXp = 0;
		return true;
	}
}
