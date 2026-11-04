package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * The fish deep sea trawling brings up, as the OSRS Wiki gives them: the shoal each comes from, the
 * Fishing level it needs and the experience the wiki gives for a catch with a trawling net. Shoals
 * move and stop on their own routes and the net's catch depends on the net, the depth and bait, so
 * nothing here predicts a pace: the pace is the player's own, from their trips.
 */
public final class Shoals
{
	/** One kind of trawled fish and the shoal it comes from. */
	public static final class Kind
	{
		/** The shoal, as the game names it: "Haddock shoal". */
		public final String shoal;
		/** The fish, by its store and item name: "Raw haddock". */
		public final String fish;
		/** The fish as a catch message names it: "haddock". */
		final String caught;
		public final int item;
		public final int level;
		/** Fishing experience for a catch with a trawling net, by the wiki's figure. */
		public final double xp;

		Kind(String shoal, String fish, int item, int level, double xp)
		{
			this.shoal = shoal;
			this.fish = fish;
			this.caught = fish.substring("Raw ".length()).toLowerCase(Locale.ENGLISH);
			this.item = item;
			this.level = level;
			this.xp = xp;
		}

		/** The shoal's name without "shoal": "Haddock", "Giant krill". */
		public String shortName()
		{
			return shoal.replace(" shoal", "");
		}

		@Override
		public String toString()
		{
			return shoal;
		}
	}

	private static final List<Kind> ALL = new ArrayList<>();
	private static final Map<String, Kind> BY_FISH = new HashMap<>();
	private static final Map<Integer, Kind> BY_ITEM = new HashMap<>();

	static
	{
		// OSRS Wiki, each raw fish's page: "requiring level 73 Fishing and granting 128.5 experience
		// per successful catch with a trawling net" (Raw haddock), and so on. Item ids: RuneLite gameval.
		add("Giant krill shoal", "Raw giant krill", ItemID.RAW_GIANT_KRILL, 69, 112.5);
		add("Haddock shoal", "Raw haddock", ItemID.RAW_HADDOCK, 73, 128.5);
		add("Yellowfin shoal", "Raw yellowfin", ItemID.RAW_YELLOWFIN, 79, 155.5);
		add("Halibut shoal", "Raw halibut", ItemID.RAW_HALIBUT, 83, 195.5);
		add("Bluefin shoal", "Raw bluefin", ItemID.RAW_BLUEFIN, 87, 220.5);
		add("Marlin shoal", "Raw marlin", ItemID.RAW_MARLIN, 91, 265.5);
	}

	private Shoals()
	{
	}

	private static void add(String shoal, String fish, int item, int level, double xp)
	{
		Kind k = new Kind(shoal, fish, item, level, xp);
		ALL.add(k);
		BY_FISH.put(fish, k);
		BY_ITEM.put(item, k);
	}

	/** Every trawled fish, lowest level first. */
	public static List<Kind> all()
	{
		return Collections.unmodifiableList(ALL);
	}

	/** The fish of a store: "Raw haddock"; null for anything else. */
	public static Kind byStore(String store)
	{
		return store == null ? null : BY_FISH.get(store);
	}

	/** The fish of an item id, as it lands in the net; null for anything else, rare fish among them. */
	public static Kind byItem(int item)
	{
		return BY_ITEM.get(item);
	}

	/**
	 * The fish a catch message names: "giant krill", "Haddock", "raw marlin". Exact, so the rare fish
	 * that share a word with one (golden haddock, giant blue krill, swift marlin) are not taken for it.
	 */
	static Kind byCaught(String name)
	{
		if (name == null)
		{
			return null;
		}
		String n = name.toLowerCase(Locale.ENGLISH).trim();
		if (n.startsWith("raw "))
		{
			n = n.substring(4);
		}
		for (Kind k : ALL)
		{
			if (n.equals(k.caught) || n.equals(k.caught + "s"))
			{
				return k;
			}
		}
		return null;
	}
}
