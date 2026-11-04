package com.codex.ironworks;

import com.codex.ironworks.core.Salvages;
import com.codex.ironworks.core.Wrecks;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Skill;

/**
 * Watches shipwreck salvaging, on the client thread: salvage coming onto the player's own hook,
 * salvage sorted at a station and what it sorted into, wrecks sinking, and the cargo hold when it is
 * open, into the player's own salvaging log. It only reads the game; it never clicks, and it gives no
 * cue to act on.
 *
 * <p>Salvaging and sorting both give Sailing experience (OSRS Wiki, Shipwreck salvaging), so salvage
 * that comes into the pack counts as salvaged only with Sailing experience on the same tick: salvage
 * taken out of a bank or the hold comes with none. Salvage that leaves the pack with Sailing
 * experience was sorted, and what came into the pack with it is what it sorted into; salvage that
 * leaves with none was put in the hold, banked or dropped, and is not counted.
 */
final class SalvageWatch
{
	/** Experience with no salvage in the pack is the hooks only up to this many salvages' worth. */
	static final int MOST_HOOKS = 5;
	private static final Pattern NUMBER = Pattern.compile("\\d+");
	private static final Pattern OF = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

	private double sailingXp;
	private boolean sunk;

	/** A world hop, login or new account: nothing carries over. */
	void reset()
	{
		sailingXp = 0;
		sunk = false;
	}

	/** Experience gained in a skill. */
	void xp(Skill skill, long gained)
	{
		if (skill == Skill.SAILING && gained > 0)
		{
			sailingXp += gained;
		}
	}

	/** A game message, tags taken off. */
	void heard(String message)
	{
		if (message != null && Wrecks.SUNK.equals(message.trim()))
		{
			sunk = true;
		}
	}

	/**
	 * One tick: what the carried items did since the last, with the Sailing experience that came.
	 *
	 * @param before what was carried on the last tick, by item id; null on the first
	 * @param now what is carried now
	 * @param bank whether a bank or other storage is open, when items move for other reasons
	 * @param names the game's name for an item id
	 * @return whether the log changed
	 */
	boolean tick(Map<Integer, Long> before, Map<Integer, Long> now, boolean bank, IntFunction<String> names, long nowMs,
		Salvages log)
	{
		double xp = sailingXp;
		sailingXp = 0;
		boolean changed = false;
		if (sunk)
		{
			sunk = false;
			Salvages.Wreck w = log.working(nowMs) == null ? null : log.wreck(log.working(nowMs).salvage);
			long was = w == null ? 0 : w.getSunk();
			log.sunk(nowMs);
			changed = w != null && w.getSunk() != was;
		}
		if (before == null || now == null || bank || xp <= 0)
		{
			return changed;
		}
		Map<Wrecks.Kind, Long> gained = new HashMap<>();
		Map<Wrecks.Kind, Long> lost = new HashMap<>();
		long gainedAll = 0;
		for (Wrecks.Kind k : Wrecks.all())
		{
			long d = now.getOrDefault(k.item, 0L) - before.getOrDefault(k.item, 0L);
			if (d > 0)
			{
				gained.put(k, d);
				gainedAll += d;
			}
			else if (d < 0)
			{
				lost.put(k, -d);
			}
		}
		// Sorting: salvage gone with experience, and what came into the pack with it.
		double sortXp = 0;
		if (!lost.isEmpty())
		{
			Map<Integer, Long> got = new HashMap<>();
			Map<Integer, String> gotNames = new HashMap<>();
			now.forEach((id, q) ->
			{
				long d = q - before.getOrDefault(id, 0L);
				if (d > 0 && Wrecks.byItem(id) == null)
				{
					got.put(id, d);
					String n = names == null ? null : names.apply(id);
					if (n != null)
					{
						gotNames.put(id, n);
					}
				}
			});
			for (Map.Entry<Wrecks.Kind, Long> e : lost.entrySet())
			{
				sortXp += e.getKey().sortXp * e.getValue();
			}
			double share = Math.min(xp, sortXp);
			for (Map.Entry<Wrecks.Kind, Long> e : lost.entrySet())
			{
				Wrecks.Kind k = e.getKey();
				double part = sortXp <= 0 ? 0 : share * (k.sortXp * e.getValue() / sortXp);
				// Loot is the one wreck's when one kind was sorted; mixed sorts on one tick share none.
				log.sorted(k, e.getValue(), part, lost.size() == 1 ? got : null, gotNames);
			}
			changed = true;
		}
		double hookXp = Math.max(0, xp - sortXp);
		if (gainedAll > 0)
		{
			for (Map.Entry<Wrecks.Kind, Long> e : gained.entrySet())
			{
				log.salvaged(e.getKey(), e.getValue(), hookXp * e.getValue() / gainedAll, nowMs);
			}
			return true;
		}
		Wrecks.Kind at = log.working(nowMs);
		if (lost.isEmpty() && at != null && hookXp > 0 && hookXp <= at.xp * MOST_HOOKS)
		{
			// Experience from the hooks with nothing in the pack: a crewmate's salvage into the hold.
			log.salvaged(at, 0, hookXp, nowMs);
			changed = true;
		}
		return changed;
	}

	/**
	 * The hold's size from the text its window shows: "39/160" or "160". Zero when it does not read
	 * as a size a cargo hold can have.
	 */
	static int capacity(String text)
	{
		if (text == null)
		{
			return 0;
		}
		Matcher of = OF.matcher(text);
		if (of.find())
		{
			int n = Integer.parseInt(of.group(2));
			return Wrecks.holdSize(n) ? n : 0;
		}
		Matcher m = NUMBER.matcher(text);
		int found = 0;
		int count = 0;
		while (m.find())
		{
			count++;
			if (m.group().length() <= 4)
			{
				found = Integer.parseInt(m.group());
			}
		}
		return count == 1 && Wrecks.holdSize(found) ? found : 0;
	}
}
