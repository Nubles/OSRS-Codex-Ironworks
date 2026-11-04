package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

/**
 * The herbiboar of Fossil Island, as the OSRS Wiki gives it: what it asks for and which herbs it
 * can give at which Herblore level. How many of each a harvest gives at a level is not given in
 * words, so Ironworks learns the mix from the player's own harvests ({@link Herbiboars}) instead
 * of assuming one.
 */
public final class Herbiboar
{
	/** "80 (boostable)" Hunter to begin tracking (OSRS Wiki: Herbiboar). */
	public static final int HUNTER = 80;
	/** "can be harvested, requiring at least level 31 Herblore" (OSRS Wiki: Herbiboar). */
	public static final int HERBLORE = 31;
	/** "In order to reach the island, players must have completed the Bone Voyage quest." (OSRS Wiki: Fossil Island). */
	public static final String QUEST = "Bone Voyage";
	/** "For each herb picked except for the first, 25 Herblore experience is granted." (OSRS Wiki: Herbiboar). */
	public static final int XP_PER_EXTRA_HERB = 25;
	/** "An average of 59–66 herbiboars can be caught per hour while using stamina potions." (OSRS Wiki: Herbiboar). */
	public static final int WIKI_LOW = 59;
	public static final int WIKI_HIGH = 66;
	/** The herbiboar as it shows when tracked down (RuneLite gameval NpcID). */
	public static final int[] NPCS = {NpcID.FOSSIL_HERBIBOAR_VISIBLE, NpcID.FOSSIL_HERBIBOAR_ANIM_VISIBLE};
	/** The map squares the herbiboar is hunted in, as RuneLite's own Herbiboar plugin knows them. */
	public static final int[] REGIONS = {14652, 14651, 14908, 14907};
	/** Where the middle trail starts, from RuneLite's Herbiboar plugin, for a route there. */
	public static final int START_X = 3686;
	public static final int START_Y = 3870;

	/** The Herblore levels a herb drops at, with its grimy item. */
	public static final class Range
	{
		public final int min;
		public final int max;
		public final int grimy;

		Range(int min, int max, int grimy)
		{
			this.min = min;
			this.max = max;
			this.grimy = grimy;
		}

		public boolean at(int herblore)
		{
			return herblore >= min && herblore <= max;
		}
	}

	private static final Map<String, Range> HERBS = new LinkedHashMap<>();
	private static final List<String> NAMES = new ArrayList<>();

	static
	{
		// The wiki's drop table: a "Herblore level range" for these, and none for guam, irit, avantoe,
		// kwuarm and cadantine, which are taken to drop at every level the herbiboar can be harvested at.
		// "Grimy toadflax and Grimy huasca are the only herbs that are not dropped at all."
		herb("Guam leaf", HERBLORE, 99, ItemID.UNIDENTIFIED_GUAM);
		herb("Marrentill", 31, 80, ItemID.UNIDENTIFIED_MARENTILL);
		herb("Tarromin", 31, 77, ItemID.UNIDENTIFIED_TARROMIN);
		herb("Harralander", 31, 76, ItemID.UNIDENTIFIED_HARRALANDER);
		herb("Ranarr weed", 31, 99, ItemID.UNIDENTIFIED_RANARR);
		herb("Irit leaf", HERBLORE, 99, ItemID.UNIDENTIFIED_IRIT);
		herb("Avantoe", HERBLORE, 99, ItemID.UNIDENTIFIED_AVANTOE);
		herb("Kwuarm", HERBLORE, 99, ItemID.UNIDENTIFIED_KWUARM);
		herb("Cadantine", HERBLORE, 99, ItemID.UNIDENTIFIED_CADANTINE);
		herb("Lantadyme", 41, 99, ItemID.UNIDENTIFIED_LANTADYME);
		herb("Dwarf weed", 62, 99, ItemID.UNIDENTIFIED_DWARF_WEED);
		herb("Snapdragon", 74, 99, ItemID.UNIDENTIFIED_SNAPDRAGON);
		herb("Torstol", 77, 99, ItemID.UNIDENTIFIED_TORSTOL);
	}

	private Herbiboar()
	{
	}

	private static void herb(String store, int min, int max, int grimy)
	{
		HERBS.put(store.toLowerCase(Locale.ENGLISH), new Range(min, max, grimy));
		NAMES.add(store);
	}

	/** The Herblore levels a herb drops at, or null when the herbiboar never gives it. */
	public static Range range(String store)
	{
		return store == null ? null : HERBS.get(store.toLowerCase(Locale.ENGLISH));
	}

	/** Whether a harvest can give this herb at this Herblore level. */
	public static boolean drops(String store, int herblore)
	{
		Range r = range(store);
		return r != null && herblore >= HERBLORE && r.at(herblore);
	}

	/** Every herb the herbiboar can give, by store name, in the wiki's order. */
	public static List<String> herbs()
	{
		return Collections.unmodifiableList(NAMES);
	}

	/**
	 * The stretch of Herblore levels a level sits in over which the herbs on offer stay the same:
	 * between the levels where a herb starts or stops dropping. Harvests are logged by stretch,
	 * since the mix changes at each edge.
	 *
	 * @return {first level, last level}
	 */
	public static int[] band(int herblore)
	{
		TreeSet<Integer> edges = new TreeSet<>();
		edges.add(1);
		edges.add(HERBLORE);
		for (Range r : HERBS.values())
		{
			edges.add(r.min);
			if (r.max < 99)
			{
				edges.add(r.max + 1);
			}
		}
		int level = Math.max(1, Math.min(99, herblore));
		int lo = edges.floor(level);
		Integer next = edges.higher(lo);
		return new int[]{lo, next == null ? 99 : next - 1};
	}

	/** A band's key in the log: "41-61". */
	public static String bandKey(int herblore)
	{
		int[] b = band(herblore);
		return b[0] + "-" + b[1];
	}

	/** Whether the player stands where the herbiboar is hunted, from the map squares loaded. */
	public static boolean ground(int[] mapRegions)
	{
		if (mapRegions == null)
		{
			return false;
		}
		for (int r : mapRegions)
		{
			for (int h : REGIONS)
			{
				if (r == h)
				{
					return true;
				}
			}
		}
		return false;
	}
}
