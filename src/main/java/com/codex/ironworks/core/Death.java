package com.codex.ironworks.core;

import java.util.HashMap;
import java.util.Map;

/**
 * What the player lost on their last death and has not taken back. It lies at the grave for
 * fifteen minutes of play, then waits at Death's Office until reclaimed. Dying again before then
 * loses it for good.
 */
public final class Death
{
	/** Play time a grave lasts before its items move to Death's Office. */
	public static final long GRAVE_MS = 15 * 60_000L;

	long at;
	/** Play time on the account's clock when the player died. */
	long playedAt;
	int world;
	/** Items lost and not yet seen come back, by item id. */
	Map<Integer, Long> items = new HashMap<>();
	/** How many items were lost in all, to tell how much has come back. */
	long lost;

	Death()
	{
	}

	Death(Map<Integer, Long> items, long at, long playedAt, int world)
	{
		this.at = at;
		this.playedAt = playedAt;
		this.world = world;
		items.forEach((k, v) ->
		{
			if (k != null && v != null && v > 0)
			{
				this.items.put(k, v);
				lost += v;
			}
		});
	}

	/** Play time left at the grave, or 0 once the items have gone to Death's Office. */
	public long graveLeft(long played)
	{
		return Math.max(0, GRAVE_MS - (played - playedAt));
	}

	public long getAt()
	{
		return at;
	}

	public int getWorld()
	{
		return world;
	}

	public Map<Integer, Long> getItems()
	{
		return items;
	}

	public long getLost()
	{
		return lost;
	}

	/** Items seen come back; true when so little is left that the death is settled. */
	boolean back(int item, long qty)
	{
		Long held = items.get(item);
		if (held != null && qty > 0)
		{
			if (qty >= held)
			{
				items.remove(item);
			}
			else
			{
				items.put(item, held - qty);
			}
		}
		long left = 0;
		for (long v : items.values())
		{
			left += v;
		}
		return items.isEmpty() || left * 10 < lost;
	}

	Death repair()
	{
		Map<Integer, Long> clean = new HashMap<>();
		if (items != null)
		{
			items.forEach((k, v) ->
			{
				if (k != null && v != null && v > 0)
				{
					clean.put(k, v);
				}
			});
		}
		items = clean;
		lost = Math.max(lost, 0);
		return this;
	}
}
