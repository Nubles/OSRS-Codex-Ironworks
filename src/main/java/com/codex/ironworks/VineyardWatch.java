package com.codex.ironworks;

import com.codex.ironworks.core.Vineyard;
import com.codex.ironworks.core.Vineyards;
import java.util.Map;

/**
 * Watches the Aldarin vineyard, on the client thread, by the pack alone: the foreman's grape barrel
 * coming into it, and grapes coming in while the barrel was carried, which is a full barrel handed
 * in (OSRS Wiki: Vineyard foreman, "Great work! Here, keep some grapes as payment."). It only reads
 * the game; it marks no vine, never clicks, and gives no cue to act on.
 */
final class VineyardWatch
{
	/** More grapes than this at once is not a barrel handed in, whatever else it was. */
	static final int MOST_AT_ONCE = 4 * Vineyard.GRAPES_PER_BARREL;

	/**
	 * One tick: what the carried items did since the last.
	 *
	 * @param before what was carried on the last tick, by item id, noted folded in; null on the first
	 * @param now what is carried now
	 * @param busy whether a bank, storage, shop or trade is open, when grapes move for other reasons
	 * @return whether the log changed
	 */
	boolean tick(Map<Integer, Long> before, Map<Integer, Long> now, boolean busy, long nowMs, Vineyards log)
	{
		if (before == null || now == null || busy)
		{
			return false;
		}
		boolean had = before.getOrDefault(Vineyard.BARREL, 0L) > 0;
		boolean has = now.getOrDefault(Vineyard.BARREL, 0L) > 0;
		long grapes = now.getOrDefault(Vineyard.GRAPES, 0L) - before.getOrDefault(Vineyard.GRAPES, 0L);
		if (had && grapes > 0 && grapes <= MOST_AT_ONCE)
		{
			log.handedIn(grapes, nowMs);
			return true;
		}
		if (!had && has)
		{
			long was = log.getLast();
			log.given(nowMs);
			return log.getLast() != was;
		}
		return false;
	}
}
