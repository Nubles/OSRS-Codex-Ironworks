package com.codex.ironworks.core;

import net.runelite.api.gameval.ItemID;

/**
 * The essence pouches a runecrafter carries, and how each one wears. Capacities, levels and the
 * points at which a pouch shrinks are the OSRS Wiki's (Essence pouch: "Times Degraded" table), and
 * match the game's own scripts as RuneLite's runecraft plugin reads them. Each essence put in adds a
 * random 0 to 3 to a hidden counter (the colossal pouch counts each essence once, unconfirmed: source
 * RuneLite's runecraft plugin, borne out by the wiki's "Degrades after 320 essence stored"); when the
 * counter passes a step the pouch holds less, and past the last step it is gone. The small pouch never
 * wears. The Dark Mage in the Abyss repairs every pouch for free, and Astral Contact reaches him from
 * anywhere. Pure, so every rule can be tested without a client.
 */
public enum Pouch
{
	SMALL("Small pouch", ItemID.RCU_POUCH_SMALL, -1, 1, 3, 3, new int[0], new int[0]),
	MEDIUM("Medium pouch", ItemID.RCU_POUCH_MEDIUM, ItemID.RCU_POUCH_MEDIUM_DEGRADE, 25, 6, 3,
		new int[]{400, 800}, new int[]{3, 0}),
	LARGE("Large pouch", ItemID.RCU_POUCH_LARGE, ItemID.RCU_POUCH_LARGE_DEGRADE, 50, 9, 3,
		new int[]{400, 600, 800, 1000}, new int[]{7, 5, 3, 0}),
	GIANT("Giant pouch", ItemID.RCU_POUCH_GIANT, ItemID.RCU_POUCH_GIANT_DEGRADE, 75, 12, 3,
		new int[]{200, 300, 400, 600, 800, 1000, 1200}, new int[]{9, 8, 7, 6, 5, 3, 0}),
	COLOSSAL("Colossal pouch", ItemID.RCU_POUCH_COLOSSAL, ItemID.RCU_POUCH_COLOSSAL_DEGRADE, 25, 40, 1,
		new int[]{320, 565, 745, 870, 950, 995, 1015, 1020}, new int[]{35, 30, 25, 20, 15, 10, 5, 0});

	private final String label;
	private final int item;
	private final int degradedItem;
	private final int level;
	private final int full;
	/** The most one essence can add to the wear counter. */
	private final int wearPerEssence;
	/** The counter values at which the pouch shrinks, lowest first. */
	private final int[] steps;
	/** What it holds once the counter reaches each step; 0 is gone. */
	private final int[] holds;

	Pouch(String label, int item, int degradedItem, int level, int full, int wearPerEssence, int[] steps, int[] holds)
	{
		this.label = label;
		this.item = item;
		this.degradedItem = degradedItem;
		this.level = level;
		this.full = full;
		this.wearPerEssence = wearPerEssence;
		this.steps = steps;
		this.holds = holds;
	}

	public String getLabel()
	{
		return label;
	}

	/** The pouch's item as new. */
	public int getItem()
	{
		return item;
	}

	/** The Runecraft level it needs; the colossal pouch's 25 is where it starts, holding 8. */
	public int getLevel()
	{
		return level;
	}

	/** Whether it wears at all: the small pouch does not. */
	public boolean wears()
	{
		return steps.length > 0;
	}

	/** The pouch an item is, as new or degraded, or null. */
	public static Pouch of(int itemId)
	{
		if (itemId <= 0)
		{
			return null;
		}
		for (Pouch p : values())
		{
			if (p.item == itemId || p.degradedItem == itemId)
			{
				return p;
			}
		}
		return null;
	}

	/** Whether the item is the pouch's degraded form. */
	public static boolean degraded(int itemId)
	{
		Pouch p = of(itemId);
		return p != null && p.degradedItem == itemId;
	}

	/**
	 * The most the colossal pouch holds as new at a Runecraft level: 8 from 25, 16 from 50, 27 from
	 * 75 and 40 from 85 (OSRS Wiki). Other pouches hold the same at every level.
	 */
	int fullAt(int runecraft)
	{
		if (this != COLOSSAL)
		{
			return full;
		}
		return runecraft >= 85 ? 40 : runecraft >= 75 ? 27 : runecraft >= 50 ? 16 : 8;
	}

	/** What it holds at a wear counter, before the colossal pouch's level scaling. */
	private int holdsAt(int wear)
	{
		int h = full;
		for (int i = 0; i < steps.length; i++)
		{
			if (wear >= steps[i])
			{
				h = holds[i];
			}
		}
		return h;
	}

	/** Scales what the colossal pouch holds down linearly from its full 40, as the game does below level 85. */
	private int scale(int holds, int runecraft)
	{
		if (this != COLOSSAL || holds <= 0)
		{
			return holds;
		}
		return Math.max(1, holds * fullAt(runecraft) / full);
	}

	/** What the pouch holds now, at its wear counter and the player's Runecraft level; 0 once it is gone. */
	public int capacity(int wear, int runecraft)
	{
		return scale(holdsAt(wear), runecraft);
	}

	/** What it holds as new at the player's Runecraft level. */
	public int capacityNew(int runecraft)
	{
		return fullAt(runecraft);
	}

	/** The wear counter at which it next shrinks, or -1 when it does not wear or is already gone. */
	public int nextStep(int wear)
	{
		for (int s : steps)
		{
			if (wear < s)
			{
				return s;
			}
		}
		return -1;
	}

	/** What it will hold once it next shrinks; 0 when that step destroys it. */
	public int capacityAfterNext(int wear, int runecraft)
	{
		int next = nextStep(wear);
		return next < 0 ? capacity(wear, runecraft) : capacity(next, runecraft);
	}

	/**
	 * Essence the pouch is sure to take before it can shrink, at the most wear each essence can add;
	 * {@link Integer#MAX_VALUE} for a pouch that never wears.
	 */
	public int safeEssence(int wear)
	{
		int next = nextStep(wear);
		if (next < 0)
		{
			return wears() ? 0 : Integer.MAX_VALUE;
		}
		return Math.max(0, (next - 1 - wear) / wearPerEssence);
	}

	/** Essence it takes on average before it shrinks, at the mean wear an essence adds. */
	public int expectedEssence(int wear)
	{
		int next = nextStep(wear);
		if (next < 0)
		{
			return wears() ? 0 : Integer.MAX_VALUE;
		}
		double mean = wearPerEssence == 1 ? 1 : wearPerEssence / 2.0;
		return (int) Math.round((next - wear) / mean);
	}

	/** Whole fills the pouch is sure to take before it can shrink. */
	public int safeFills(int wear, int runecraft)
	{
		int cap = capacity(wear, runecraft);
		if (cap <= 0)
		{
			return 0;
		}
		int safe = safeEssence(wear);
		return safe == Integer.MAX_VALUE ? Integer.MAX_VALUE : safe / cap;
	}

	/** Whether the next fill could shrink it: fewer than one full pouch of essence is safe. */
	public boolean atRisk(int wear, int runecraft)
	{
		return wears() && nextStep(wear) >= 0 && safeFills(wear, runecraft) < 1;
	}
}
