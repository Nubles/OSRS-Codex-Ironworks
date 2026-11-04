package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * The rocks Ironworks knows, as the OSRS Wiki gives them: the Mining level, the experience for each
 * ore, and how long a mined rock takes to come back. The game never says when a rock will be back,
 * so a countdown from these figures is only ever an estimate; where the wiki names places a rock
 * comes back at another speed, the rock says so.
 */
public final class Rocks
{
	/** One kind of rock. */
	public static final class Kind
	{
		/** The rock's name, as the game names the object: "Iron rocks". */
		public final String name;
		/** What it gives, by item name: "Iron ore"; "Uncut gems" for gem rocks. */
		public final String ore;
		/** The item it gives, to picture it by. */
		public final int oreItem;
		public final int level;
		public final double xp;
		/** Ticks it takes to come back once mined, by the wiki's figure. */
		public final int respawnTicks;
		/** Where the wiki says it comes back at another speed, in words; null when it names nowhere. */
		public final String varies;

		Kind(String name, String ore, int oreItem, int level, double xp, int respawnTicks, String varies)
		{
			this.name = name;
			this.ore = ore;
			this.oreItem = oreItem;
			this.level = level;
			this.xp = xp;
			this.respawnTicks = respawnTicks;
			this.varies = varies;
		}

		/** The rock's name without "rocks": "Iron", "Amethyst". */
		public String shortName()
		{
			return name.replace(" rocks", "").replace(" crystals", "");
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** A game tick, in milliseconds. */
	public static final long TICK_MS = 600L;

	private static final List<Kind> ALL = new ArrayList<>();
	private static final Map<String, Kind> BY_NAME = new HashMap<>();
	private static final Map<Integer, Kind> BY_ORE = new HashMap<>();

	/** The uncut gems mining can turn up, by item id, with their names. */
	private static final Map<Integer, String> GEMS = new LinkedHashMap<>();
	/** The clues mining can turn up instead of an ore: clue geodes, and scroll boxes after X Marks the Spot. */
	private static final Map<Integer, String> CLUES = new LinkedHashMap<>();

	/** Gem rocks, which give a gem as their ore. */
	public static final Kind GEM_ROCKS;

	static
	{
		// Levels: OSRS Wiki, Rocks. Experience and respawn: each rock's own wiki page ("After being
		// mined, an iron rock takes 5.4 seconds to respawn"), turned into ticks of 0.6 seconds. The
		// Mining Guild's members' area: "the respawn time for rocks mined in this area is halved"
		// (OSRS Wiki, Mining Guild), for iron, coal, mithril, adamantite, runite and amethyst.
		add("Clay rocks", "Clay", ItemID.CLAY, 1, 5, 2, null);
		add("Copper rocks", "Copper ore", ItemID.COPPER_ORE, 1, 17.5, 4, null);
		add("Tin rocks", "Tin ore", ItemID.TIN_ORE, 1, 17.5, 4, null);
		add("Iron rocks", "Iron ore", ItemID.IRON_ORE, 15, 35, 9, "2.4s in the Mining Guild");
		add("Silver rocks", "Silver ore", ItemID.SILVER_ORE, 20, 40, 100, null);
		add("Coal rocks", "Coal", ItemID.COAL, 30, 50, 50, "halved in the Mining Guild");
		add("Gold rocks", "Gold ore", ItemID.GOLD_ORE, 40, 65, 100, null);
		GEM_ROCKS = add("Gem rocks", "Uncut gems", ItemID.UNCUT_RED_TOPAZ, 40, 65, 99, null);
		add("Mithril rocks", "Mithril ore", ItemID.MITHRIL_ORE, 55, 80, 200, "halved in the Mining Guild");
		add("Adamantite rocks", "Adamantite ore", ItemID.ADAMANTITE_ORE, 70, 95, 400, "2:00 in the Mining Guild");
		add("Runite rocks", "Runite ore", ItemID.RUNITE_ORE, 85, 125, 1200, "6:00 in the Mining Guild and Wilderness");
		add("Amethyst crystals", "Amethyst", ItemID.AMETHYST, 92, 240, 125, "halved in the Mining Guild");

		// Gem rocks give opal, jade and red topaz too (OSRS Wiki, Gem rocks); other rocks the mining
		// gem table (OSRS Wiki, Mining).
		GEMS.put(ItemID.UNCUT_OPAL, "Uncut opal");
		GEMS.put(ItemID.UNCUT_JADE, "Uncut jade");
		GEMS.put(ItemID.UNCUT_RED_TOPAZ, "Uncut red topaz");
		GEMS.put(ItemID.UNCUT_SAPPHIRE, "Uncut sapphire");
		GEMS.put(ItemID.UNCUT_EMERALD, "Uncut emerald");
		GEMS.put(ItemID.UNCUT_RUBY, "Uncut ruby");
		GEMS.put(ItemID.UNCUT_DIAMOND, "Uncut diamond");

		CLUES.put(ItemID.MINING_CLUE_GEODE_BEGINNER, "Clue geode (beginner)");
		CLUES.put(ItemID.MINING_CLUE_GEODE_EASY, "Clue geode (easy)");
		CLUES.put(ItemID.MINING_CLUE_GEODE_MEDIUM, "Clue geode (medium)");
		CLUES.put(ItemID.MINING_CLUE_GEODE_HARD, "Clue geode (hard)");
		CLUES.put(ItemID.MINING_CLUE_GEODE_ELITE, "Clue geode (elite)");
		CLUES.put(ItemID.LEAGUE_CLUE_BOX_BEGINNER, "Scroll box (beginner)");
		CLUES.put(ItemID.LEAGUE_CLUE_BOX_EASY, "Scroll box (easy)");
		CLUES.put(ItemID.LEAGUE_CLUE_BOX_MEDIUM, "Scroll box (medium)");
		CLUES.put(ItemID.LEAGUE_CLUE_BOX_HARD, "Scroll box (hard)");
		CLUES.put(ItemID.LEAGUE_CLUE_BOX_ELITE, "Scroll box (elite)");
	}

	private Rocks()
	{
	}

	private static Kind add(String name, String ore, int oreItem, int level, double xp, int respawnTicks, String varies)
	{
		Kind k = new Kind(name, ore, oreItem, level, xp, respawnTicks, varies);
		ALL.add(k);
		BY_NAME.put(name.toLowerCase(Locale.ENGLISH), k);
		if (k != GEM_ROCKS && !"Uncut gems".equals(ore))
		{
			BY_ORE.put(oreItem, k);
		}
		return k;
	}

	/** Every rock, lowest level first. */
	public static List<Kind> all()
	{
		return Collections.unmodifiableList(ALL);
	}

	/** The rock a game object's name is, or null. */
	public static Kind byName(String objectName)
	{
		return objectName == null ? null : BY_NAME.get(objectName.toLowerCase(Locale.ENGLISH));
	}

	/** The rock an ore item comes from, or null; gems are not an ore here, as most rocks give them. */
	public static Kind byOre(int itemId)
	{
		return BY_ORE.get(itemId);
	}

	/** The rock whose ore is a store, or null. */
	public static Kind byStore(String store)
	{
		for (Kind k : ALL)
		{
			if (k != GEM_ROCKS && k.ore.equalsIgnoreCase(store))
			{
				return k;
			}
		}
		return null;
	}

	/** An uncut gem's name, or null when the item is not one mining gives. */
	public static String gem(int itemId)
	{
		return GEMS.get(itemId);
	}

	/** A clue geode's or scroll box's name, or null. */
	public static String clue(int itemId)
	{
		return CLUES.get(itemId);
	}

	/** The item id of a gem or clue by its name, for its picture; -1 when not known. */
	public static int itemOf(String name)
	{
		for (Map.Entry<Integer, String> e : GEMS.entrySet())
		{
			if (e.getValue().equals(name))
			{
				return e.getKey();
			}
		}
		for (Map.Entry<Integer, String> e : CLUES.entrySet())
		{
			if (e.getValue().equals(name))
			{
				return e.getKey();
			}
		}
		return -1;
	}

	/** Whether a name is a clue geode or scroll box. */
	public static boolean isClue(String name)
	{
		return CLUES.containsValue(name);
	}

	/** The rock by its name in the log, or null. */
	public static Kind of(String name)
	{
		return byName(name);
	}
}
