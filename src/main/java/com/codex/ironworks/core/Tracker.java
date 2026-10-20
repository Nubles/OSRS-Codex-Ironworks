package com.codex.ironworks.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns what the player carries, tick by tick, into gains and losses, and keeps moves out of them.
 * Anything moved at a bank, a deposit box or another storage is a move, not a gain or a loss. A
 * bank visit is checked whole instead: whatever the bank side holds beyond what went in and came
 * out arrived unseen, such as herbs emptied from a herb sack or fish from a barrel, and is put down
 * to what the player had been doing. Items lost on death wait in a grave until picked up again.
 */
public final class Tracker
{
	/** Ticks after the bank opens before its items and potion storage are all in. */
	static final int OPEN_TICKS = 3;
	/** Ticks after a storage closes in which a last move may still arrive. */
	static final int CLOSE_TICKS = 2;
	/** Ticks a death holds what was lost; uncollected after this, it is gone. */
	static final int GRAVE_TICKS = 6000;

	private final Works works;

	private Map<String, Long> carried;
	private boolean bank;
	private int bankWait;
	private int bankClose;
	private Map<String, Long> carriedAtOpen;
	private Map<String, Long> openTotal;
	/** The bank side as the player left it, with what a deposit box added since; null when not watched this session. */
	private Map<String, Long> expectedBank;
	private int storage;
	private int storageGrace;
	private boolean deposit;
	private final Map<String, Long> grave = new HashMap<>();
	private int graveTicks;

	public Tracker(Works works)
	{
		this.works = works;
	}

	/**
	 * Logging in, or the plugin starting: what is carried is taken as it is, and the bank is not
	 * compared with how it was left, since the account may have been played elsewhere.
	 */
	public void restart(LocalDate day)
	{
		carried = null;
		bank = false;
		bankWait = 0;
		bankClose = 0;
		carriedAtOpen = null;
		openTotal = null;
		expectedBank = null;
		storage = 0;
		storageGrace = 0;
		deposit = false;
		buryGrave(day);
	}

	/** A world hop: nothing changes hands, but every window shuts and the next carried reading starts afresh. */
	public void hop()
	{
		carried = null;
		bank = false;
		bankWait = 0;
		bankClose = 0;
		openTotal = null;
		storage = 0;
		storageGrace = 0;
		deposit = false;
	}

	public void bankOpened()
	{
		if (bank)
		{
			return;
		}
		bank = true;
		bankWait = OPEN_TICKS;
		bankClose = 0;
		carriedAtOpen = carried == null ? null : new HashMap<>(carried);
	}

	public void bankClosed()
	{
		if (!bank)
		{
			return;
		}
		bank = false;
		bankClose = CLOSE_TICKS;
	}

	/** A deposit box, seed vault, group storage or other store opened. */
	public void storageOpened(boolean depositBox)
	{
		storage++;
		deposit |= depositBox;
	}

	public void storageClosed()
	{
		storage = Math.max(0, storage - 1);
		storageGrace = CLOSE_TICKS;
		if (storage == 0)
		{
			deposit = false;
		}
	}

	public boolean isBankOpen()
	{
		return bank;
	}

	/** Whether moves are being made now, so changes to what is carried are not gains or losses. */
	public boolean moving()
	{
		return bank || bankWait > 0 || bankClose > 0 || storage > 0 || storageGrace > 0;
	}

	/**
	 * One game tick.
	 *
	 * @param now what is carried now, in store units
	 * @return the gains and losses recorded on this tick
	 */
	public List<Flow> tick(Map<String, Long> now, Classify.Context ctx, LocalDate day, long time)
	{
		List<Flow> flows = new ArrayList<>();
		works.place(Place.CARRIED, now, time);
		if (carried != null)
		{
			Map<String, Long> delta = delta(carried, now);
			if (!delta.isEmpty())
			{
				if (moving())
				{
					if (deposit && expectedBank != null)
					{
						delta.forEach((k, v) ->
						{
							if (v < 0)
							{
								expectedBank.merge(k, -v, Long::sum);
							}
						});
					}
				}
				else
				{
					flows.addAll(settle(Classify.tick(delta, ctx, works.sacks), ctx));
				}
			}
		}
		carried = new HashMap<>(now);

		if (bank && bankWait > 0 && --bankWait == 0)
		{
			flows.addAll(opened(ctx));
		}
		if (bankClose > 0 && --bankClose == 0)
		{
			flows.addAll(closed(ctx));
		}
		if (storageGrace > 0 && storage == 0)
		{
			storageGrace--;
		}
		if (graveTicks > 0 && --graveTicks == 0)
		{
			buryGrave(day);
		}
		works.record(flows, day);
		return flows;
	}

	/** The bank's items are all in: what arrived there unseen since the last visit is counted. */
	private List<Flow> opened(Classify.Context ctx)
	{
		openTotal = works.stocks();
		List<Flow> flows = new ArrayList<>();
		if (expectedBank == null || carriedAtOpen == null)
		{
			return flows;
		}
		Map<String, Long> was = new HashMap<>(expectedBank);
		carriedAtOpen.forEach((k, v) -> was.merge(k, v, Long::sum));
		Map<String, Long> is = bankSide();
		carried.forEach((k, v) -> is.merge(k, v, Long::sum));
		delta(was, is).forEach((store, units) ->
		{
			if (units > 0)
			{
				flows.addAll(arrive(store, units, ctx));
			}
		});
		return flows;
	}

	/**
	 * The visit is over: anything that came into the bank side and the pack together, beyond what
	 * was there at the start, came out of a carried container; anything gone went into one, or was
	 * eaten or drunk at the bank.
	 */
	private List<Flow> closed(Classify.Context ctx)
	{
		List<Flow> flows = new ArrayList<>();
		if (openTotal != null)
		{
			delta(openTotal, works.stocks()).forEach((store, units) ->
			{
				if (units > 0)
				{
					flows.addAll(arrive(store, units, ctx));
				}
				else
				{
					Family f = Catalogue.family(store);
					if (f == Family.FOOD && !store.startsWith("Raw ") || f == Family.POTIONS)
					{
						flows.add(Flow.out(store, -units, Use.CONSUMED, null));
					}
					else if (Catalogue.gathered(store) || f == Family.MATERIALS || f == Family.SEEDS)
					{
						works.sacks.merge(store, -units, Long::sum);
					}
				}
			});
		}
		openTotal = null;
		expectedBank = bankSide();
		return flows;
	}

	/** Units that turned up unseen: first what went into a container, then new arrivals. */
	private List<Flow> arrive(String store, long units, Classify.Context ctx)
	{
		long back = Math.min(units, works.sacks.getOrDefault(store, 0L));
		if (back > 0)
		{
			long left = works.sacks.get(store) - back;
			if (left > 0)
			{
				works.sacks.put(store, left);
			}
			else
			{
				works.sacks.remove(store);
			}
		}
		if (units > back)
		{
			return settle(Collections.singletonList(Classify.arrival(store, units - back, ctx.recent)), ctx);
		}
		return Collections.emptyList();
	}

	/** Losses on death go to the grave; gains of what is in it are the player taking it back. */
	private List<Flow> settle(List<Flow> flows, Classify.Context ctx)
	{
		List<Flow> out = new ArrayList<>();
		for (Flow f : flows)
		{
			if (!f.isGain() && f.use == Use.DIED)
			{
				grave.merge(f.store, f.units, Long::sum);
				graveTicks = GRAVE_TICKS;
				continue;
			}
			long held = f.isGain() ? grave.getOrDefault(f.store, 0L) : 0;
			if (held > 0 && f.source != Source.GATHERED && f.source != Source.MADE && f.source != Source.FARMED
				&& f.source != Source.BOUGHT)
			{
				long back = Math.min(held, f.units);
				if (held - back > 0)
				{
					grave.put(f.store, held - back);
				}
				else
				{
					grave.remove(f.store);
				}
				if (f.units > back)
				{
					out.add(Flow.in(f.store, f.units - back, f.source, f.skill));
				}
				continue;
			}
			out.add(f);
		}
		return out;
	}

	/** What was never taken back from a grave is gone. */
	private void buryGrave(LocalDate day)
	{
		if (grave.isEmpty())
		{
			return;
		}
		List<Flow> lost = new ArrayList<>();
		grave.forEach((k, v) -> lost.add(Flow.out(k, v, Use.DIED, null)));
		grave.clear();
		graveTicks = 0;
		works.record(lost, day);
	}

	private Map<String, Long> bankSide()
	{
		Map<String, Long> side = new HashMap<>(works.place(Place.BANK));
		works.place(Place.POTION_STORAGE).forEach((k, v) -> side.merge(k, v, Long::sum));
		return side;
	}

	static Map<String, Long> delta(Map<String, Long> was, Map<String, Long> is)
	{
		Map<String, Long> d = new HashMap<>();
		is.forEach((k, v) -> d.merge(k, v, Long::sum));
		was.forEach((k, v) -> d.merge(k, -v, Long::sum));
		d.values().removeIf(v -> v == 0);
		return d;
	}
}
