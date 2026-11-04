package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's own shipwreck salvaging log, wreck type by wreck type: the salvage that came up on
 * their own hook, the time at it and the Sailing experience that came with it, the wrecks they saw
 * sink, what each salvage sorted into at a salvaging station, and their cargo hold as last seen.
 *
 * <p>Time is counted salvage to salvage on one wreck type: a gap up to {@link #GAP_MS} is time at the
 * wrecks (waiting on a roll, or moving to the next wreck as one sinks and another rises), anything
 * longer was a trip or a break and is not timed. Sailing experience from the hooks that came with no
 * salvage in the pack, such as a crewmate's into the hold, is counted to the wreck type being worked
 * as time, never as salvage: salvage an hour is the player's own hook, experience an hour is all hooks.
 */
public final class Salvages
{
	/** A gap between salvages longer than this was not salvaging. */
	static final long GAP_MS = 3 * 60_000L;
	/** Time at a wreck type before its pace is worth showing. */
	public static final long LEARN_MS = 5 * 60_000L;
	/** Different loot kept for a wreck type; the rarest beyond this are let go. */
	static final int MOST_LOOT = 80;

	/** One wreck type, as this account salvages and sorts it. */
	public static final class Wreck
	{
		/** Salvage onto the player's own hook, into the pack. */
		long salvage;
		/** Salvage in timed gaps: the ones the time is counted for. */
		long timedSalvage;
		long ms;
		/** Sailing experience from the hooks in timed gaps, the player's and the crew's. */
		double xp;
		/** Wrecks seen sink while at this type. */
		long sunk;
		long last;
		/** Salvage sorted at a station, the experience it gave, and what it sorted into, by item id. */
		long sorted;
		double sortXp;
		/** Sorts that kept nothing in the pack: loot dropped, or sorted at a full pack. */
		long sortedBare;
		Map<Integer, Long> loot = new HashMap<>();

		public long getSalvage()
		{
			return salvage;
		}

		public long getMs()
		{
			return ms;
		}

		public long getSunk()
		{
			return sunk;
		}

		public long getLast()
		{
			return last;
		}

		public long getSorted()
		{
			return sorted;
		}

		public double getSortXp()
		{
			return sortXp;
		}

		public long getSortedBare()
		{
			return sortedBare;
		}

		public Map<Integer, Long> getLoot()
		{
			return Collections.unmodifiableMap(loot);
		}

		/** Salvage an hour on the player's own hook while at it, or 0 until timed. */
		public double perHour()
		{
			return ms < LEARN_MS ? 0 : timedSalvage / (ms / 3_600_000.0);
		}

		/** Sailing experience an hour from the hooks while at it, or 0 until timed. */
		public double xpPerHour()
		{
			return ms < LEARN_MS ? 0 : xp / (ms / 3_600_000.0);
		}

		/** Seconds between the player's own salvages while at it, or 0 until timed. */
		public double secondsEach()
		{
			return ms < LEARN_MS || timedSalvage == 0 ? 0 : ms / 1000.0 / timedSalvage;
		}

		Wreck repair()
		{
			salvage = Math.max(0, salvage);
			timedSalvage = Math.max(0, Math.min(timedSalvage, salvage));
			ms = Math.max(0, ms);
			xp = Double.isFinite(xp) ? Math.max(0, xp) : 0;
			sunk = Math.max(0, sunk);
			last = Math.max(0, last);
			sorted = Math.max(0, sorted);
			sortXp = Double.isFinite(sortXp) ? Math.max(0, sortXp) : 0;
			sortedBare = Math.max(0, Math.min(sortedBare, sorted));
			Map<Integer, Long> l = new HashMap<>();
			if (loot != null)
			{
				loot.forEach((k, v) ->
				{
					if (k != null && k > 0 && v != null && v > 0)
					{
						l.put(k, v);
					}
				});
			}
			loot = l;
			return this;
		}
	}

	/** The cargo hold as last seen open. */
	public static final class Hold
	{
		/** The boat's slot, 1 to 5, as the game numbers its holds. */
		int boat;
		int used;
		/** Slots in the hold as its window showed them; 0 when not read. */
		int capacity;
		/** Salvage in it, by store. */
		Map<String, Long> salvage = new HashMap<>();
		long seen;

		public int getBoat()
		{
			return boat;
		}

		public int getUsed()
		{
			return used;
		}

		public int getCapacity()
		{
			return capacity;
		}

		public Map<String, Long> getSalvage()
		{
			return Collections.unmodifiableMap(salvage);
		}

		public long getSeen()
		{
			return seen;
		}

		Hold repair()
		{
			boat = Math.max(0, boat);
			used = Math.max(0, used);
			capacity = Wrecks.holdSize(capacity) ? capacity : 0;
			Map<String, Long> s = new HashMap<>();
			if (salvage != null)
			{
				salvage.forEach((k, v) ->
				{
					if (Wrecks.bySalvage(k) != null && v != null && v > 0)
					{
						s.put(k, v);
					}
				});
			}
			salvage = s;
			seen = Math.max(0, seen);
			return this;
		}
	}

	/** By the salvage's store: "Large salvage". */
	Map<String, Wreck> wrecks = new HashMap<>();
	/** Names of loot items, by id, as the game gave them. */
	Map<Integer, String> names = new HashMap<>();
	/** The wreck type worked last, by its salvage, and when. */
	String working;
	long last;
	/** The cargo hold as last seen, or null. */
	Hold hold;

	public Salvages()
	{
	}

	private Wreck wreck(Wrecks.Kind k)
	{
		return wrecks.computeIfAbsent(k.salvage, n -> new Wreck());
	}

	/**
	 * Salvage onto the player's own hook, with the Sailing experience from the hooks since the last.
	 * Salvage of 0 is experience alone: a crewmate's salvage, or a roll that went to the hold.
	 */
	public void salvaged(Wrecks.Kind k, long salvage, double xp, long now)
	{
		if (k == null || salvage < 0 || salvage == 0 && xp <= 0)
		{
			return;
		}
		Wreck w = wreck(k);
		long gap = now - w.last;
		boolean timed = w.last > 0 && gap > 0 && gap <= GAP_MS;
		w.salvage += salvage;
		if (timed)
		{
			w.ms += gap;
			w.timedSalvage += salvage;
			w.xp += Math.max(0, xp);
		}
		w.last = now;
		working = k.salvage;
		last = now;
	}

	/** The wreck type being worked: salvaged within the gap; null when none. */
	public Wrecks.Kind working(long now)
	{
		return working != null && last > 0 && now - last <= GAP_MS ? Wrecks.bySalvage(working) : null;
	}

	/** A wreck sank while at it. */
	public void sunk(long now)
	{
		Wrecks.Kind k = working(now);
		if (k != null)
		{
			wreck(k).sunk++;
		}
	}

	/**
	 * Salvage sorted at a station: how many, the Sailing experience it gave, and what came of it in the
	 * pack, by item id, with the items' names.
	 */
	public void sorted(Wrecks.Kind k, long n, double xp, Map<Integer, Long> got, Map<Integer, String> itemNames)
	{
		if (k == null || n <= 0)
		{
			return;
		}
		Wreck w = wreck(k);
		w.sorted += n;
		w.sortXp += Math.max(0, xp);
		boolean any = false;
		if (got != null)
		{
			for (Map.Entry<Integer, Long> e : got.entrySet())
			{
				Integer id = e.getKey();
				long q = e.getValue() == null ? 0 : e.getValue();
				if (id == null || id <= 0 || q <= 0 || Wrecks.byItem(id) != null)
				{
					continue;
				}
				if (!w.loot.containsKey(id) && w.loot.size() >= MOST_LOOT)
				{
					continue;
				}
				w.loot.merge(id, q, Long::sum);
				any = true;
				String name = itemNames == null ? null : itemNames.get(id);
				if (name != null && !name.isEmpty())
				{
					names.put(id, name);
				}
			}
		}
		if (!any)
		{
			w.sortedBare += n;
		}
	}

	/** The cargo hold seen open: its boat, slots used and its size, and the salvage in it. */
	public void hold(int boat, int used, int capacity, Map<Wrecks.Kind, Long> salvage, long now)
	{
		Hold h = new Hold();
		h.boat = boat;
		h.used = Math.max(0, used);
		h.capacity = Wrecks.holdSize(capacity) ? capacity : hold != null && hold.boat == boat ? hold.capacity : 0;
		if (salvage != null)
		{
			salvage.forEach((k, v) ->
			{
				if (k != null && v != null && v > 0)
				{
					h.salvage.merge(k.salvage, v, Long::sum);
				}
			});
		}
		h.seen = now;
		hold = h;
	}

	/** The size read off the hold's window, kept with the last hold seen when it is that boat's. */
	public void holdSize(int boat, int capacity)
	{
		if (hold != null && hold.boat == boat && Wrecks.holdSize(capacity))
		{
			hold.capacity = capacity;
		}
	}

	public Hold hold()
	{
		return hold;
	}

	public Map<String, Wreck> wrecks()
	{
		return Collections.unmodifiableMap(wrecks);
	}

	/** One wreck type as salvaged, or null. */
	public Wreck wreck(String salvage)
	{
		return wrecks.get(salvage);
	}

	/** A loot item's name as the game gave it, or null. */
	public String name(int id)
	{
		return names.get(id);
	}

	/** Every salvage on the player's own hook. */
	public long salvage()
	{
		long n = 0;
		for (Wreck w : wrecks.values())
		{
			n += w.salvage;
		}
		return n;
	}

	/** Every salvage sorted. */
	public long sorted()
	{
		long n = 0;
		for (Wreck w : wrecks.values())
		{
			n += w.sorted;
		}
		return n;
	}

	public boolean isEmpty()
	{
		return salvage() == 0 && sorted() == 0 && hold == null;
	}

	/** Whether the player has salvaged a wreck type on their own hook. */
	public boolean tried(Wrecks.Kind k)
	{
		Wreck w = k == null ? null : wrecks.get(k.salvage);
		return w != null && w.salvage > 0;
	}

	/** Salvage an hour of one store at the player's own timed pace; 0 until timed. */
	public double perHour(String store)
	{
		Wreck w = wrecks.get(store);
		return w == null ? 0 : w.perHour();
	}

	/** The wreck types, the latest worked first. */
	public List<Map.Entry<String, Wreck>> byLatest()
	{
		List<Map.Entry<String, Wreck>> l = new ArrayList<>(wrecks.entrySet());
		l.sort((a, b) -> Long.compare(b.getValue().last, a.getValue().last));
		return l;
	}

	Salvages repair()
	{
		Map<String, Wreck> w = new HashMap<>();
		if (wrecks != null)
		{
			wrecks.forEach((k, v) ->
			{
				if (v != null && Wrecks.bySalvage(k) != null)
				{
					w.put(k, v.repair());
				}
			});
		}
		wrecks = w;
		Map<Integer, String> n = new HashMap<>();
		if (names != null)
		{
			names.forEach((k, v) ->
			{
				if (k != null && v != null && !v.isEmpty())
				{
					n.put(k, v);
				}
			});
		}
		names = n;
		working = Wrecks.bySalvage(working) == null ? null : working;
		last = Math.max(0, last);
		hold = hold == null ? null : hold.repair();
		return this;
	}
}
