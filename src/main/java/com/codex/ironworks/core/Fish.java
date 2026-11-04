package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;

/**
 * The fish Ironworks knows, as the OSRS Wiki gives them: the Fishing level and experience for each
 * fish, the way it is caught (which is what a fishing spot is used for), the tool the player is
 * seen fishing with, what else fishing can turn up, and how long a fishing spot stays put.
 *
 * <p>Levels, experience and tools: each fish's own wiki page, "Fishing info" ("Level required",
 * "Fishing XP", "Required tool"); the aerial fish from OSRS Wiki, Aerial fishing. Tools by
 * animation: RuneLite's gameval AnimationID, the fishing animations RuneLite's own Idle Notifier
 * counts as fishing. Deep sea trawling is logged apart (see {@link Trawls}); its fish are not here.
 */
public final class Fish
{
	/** How a fish is caught: a fishing spot's option and the tool it takes. */
	public enum Gear
	{
		SMALL_NET, BIG_NET, CAGE, HARPOON, ROD, FLY_ROD, BARBARIAN_ROD, VESSEL, CORMORANT
	}

	/** One way of fishing: the spot's option, and the fish it gives. */
	public static final class Method
	{
		/** Its name in the log and on the card: "Cage", "Big net". */
		public final String name;
		/** The spot it is used at, as the wiki names spots: "Cage/Harpoon". */
		public final String spot;
		public final Gear gear;
		/** The tool the wiki names for it, when no other is seen. */
		public final String tool;
		/**
		 * Whether any account may take it up with the level alone: no quest, miniquest, other skill or
		 * Wilderness trip first. Only these are suggested as worth a try.
		 */
		public final boolean plain;
		final List<Kind> fish = new ArrayList<>();

		Method(String name, String spot, Gear gear, String tool, boolean plain)
		{
			this.name = name;
			this.spot = spot;
			this.gear = gear;
			this.tool = tool;
			this.plain = plain;
		}

		public List<Kind> fish()
		{
			return Collections.unmodifiableList(fish);
		}

		/** The lowest level that catches anything this way. */
		public int level()
		{
			int l = Integer.MAX_VALUE;
			for (Kind k : fish)
			{
				l = Math.min(l, k.level);
			}
			return l;
		}

		/** The fish this way gives the most experience for that a level catches; null when none. */
		public Kind best(int level)
		{
			Kind b = null;
			for (Kind k : fish)
			{
				if (k.level <= level && (b == null || k.xp > b.xp))
				{
					b = k;
				}
			}
			return b;
		}

		/** "Cage (lobster)", "Big net (mackerel, cod, bass)". */
		public String label()
		{
			StringBuilder s = new StringBuilder(name).append(" (");
			for (int i = 0; i < fish.size(); i++)
			{
				s.append(i == 0 ? "" : ", ").append(fish.get(i).shortName());
			}
			return s.append(')').toString();
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** One fish. */
	public static final class Kind
	{
		/** Its store, as the item is named: "Raw lobster", "Leaping trout". */
		public final String store;
		public final int item;
		public final int level;
		/** Fishing experience for one, before the angler outfit's bonus. */
		public final double xp;
		public final Method method;
		/** The Hunter level aerial fish also need; 0 for the rest. */
		public final int hunter;

		Kind(String store, int item, int level, double xp, Method method, int hunter)
		{
			this.store = store;
			this.item = item;
			this.level = level;
			this.xp = xp;
			this.method = method;
			this.hunter = hunter;
		}

		/** "lobster", "leaping trout". */
		public String shortName()
		{
			return (store.startsWith("Raw ") ? store.substring(4) : store).toLowerCase(Locale.ENGLISH);
		}

		@Override
		public String toString()
		{
			return store;
		}
	}

	/** A tool as the player is seen fishing with it. */
	public static final class Tool
	{
		public final String name;
		public final Gear gear;

		Tool(String name, Gear gear)
		{
			this.name = name;
			this.gear = gear;
		}
	}

	private static final List<Method> METHODS = new ArrayList<>();
	private static final Map<String, Method> METHOD_BY_NAME = new HashMap<>();
	private static final List<Kind> ALL = new ArrayList<>();
	private static final Map<Integer, Kind> BY_ITEM = new HashMap<>();
	private static final Map<String, Kind> BY_STORE = new HashMap<>();
	private static final Map<Integer, Tool> TOOLS = new HashMap<>();
	/** What else fishing turns up, by item id. */
	private static final Map<Integer, String> EXTRAS = new LinkedHashMap<>();

	/** Molch pearls, from aerial fishing. */
	public static final int PEARL = ItemID.AERIAL_FISHING_PEARL;
	public static final String PEARL_NAME = "Molch pearl";
	/** The open fish barrel, which takes the fish caught instead of the pack (OSRS Wiki, Fish barrel). */
	public static final int BARREL_OPEN = ItemID.FISH_BARREL_OPEN;

	public static final Method SMALL_NET;
	public static final Method SEA_BAIT;
	public static final Method FLY;
	public static final Method RIVER_BAIT;
	public static final Method BIG_NET;
	public static final Method CAGE;
	public static final Method HARPOON;
	public static final Method SHARK_HARPOON;
	public static final Method MONKFISH;
	public static final Method KARAMBWAN;
	public static final Method ANGLERFISH;
	public static final Method DARK_CRAB;
	public static final Method BARBARIAN;
	public static final Method AERIAL;

	static
	{
		SMALL_NET = method("Net", "Net/Bait", Gear.SMALL_NET, "Small fishing net", true);
		SEA_BAIT = method("Sea bait", "Net/Bait", Gear.ROD, "Fishing rod", true);
		FLY = method("Lure", "Lure/Bait", Gear.FLY_ROD, "Fly fishing rod", true);
		RIVER_BAIT = method("River bait", "Lure/Bait", Gear.ROD, "Fishing rod", true);
		BIG_NET = method("Big net", "Big Net/Harpoon", Gear.BIG_NET, "Big fishing net", true);
		CAGE = method("Cage", "Cage/Harpoon", Gear.CAGE, "Lobster pot", true);
		HARPOON = method("Harpoon", "Cage/Harpoon", Gear.HARPOON, "Harpoon", true);
		SHARK_HARPOON = method("Shark harpoon", "Big Net/Harpoon", Gear.HARPOON, "Harpoon", true);
		MONKFISH = method("Monkfish net", "Piscatoris", Gear.SMALL_NET, "Small fishing net", false);
		KARAMBWAN = method("Karambwan vessel", "Karambwan spot", Gear.VESSEL, "Karambwan vessel", false);
		ANGLERFISH = method("Sandworm bait", "Anglerfish spot", Gear.ROD, "Fishing rod", false);
		DARK_CRAB = method("Dark crab cage", "Wilderness", Gear.CAGE, "Lobster pot", false);
		BARBARIAN = method("Barbarian rod", "Use-rod", Gear.BARBARIAN_ROD, "Barbarian rod", false);
		AERIAL = method("Aerial", "Lake Molch", Gear.CORMORANT, "Cormorant", true);

		// Each fish's wiki page: "Level required" and "Fishing XP".
		fish("Raw shrimps", ItemID.RAW_SHRIMP, 1, 10, SMALL_NET);
		fish("Raw anchovies", ItemID.RAW_ANCHOVIES, 15, 40, SMALL_NET);
		fish("Raw sardine", ItemID.RAW_SARDINE, 5, 20, SEA_BAIT);
		fish("Raw herring", ItemID.RAW_HERRING, 10, 30, SEA_BAIT);
		fish("Raw trout", ItemID.RAW_TROUT, 20, 50, FLY);
		fish("Raw salmon", ItemID.RAW_SALMON, 30, 70, FLY);
		fish("Raw pike", ItemID.RAW_PIKE, 25, 60, RIVER_BAIT);
		fish("Raw mackerel", ItemID.RAW_MACKEREL, 16, 20, BIG_NET);
		fish("Raw cod", ItemID.RAW_COD, 23, 45, BIG_NET);
		fish("Raw bass", ItemID.RAW_BASS, 46, 100, BIG_NET);
		fish("Raw lobster", ItemID.RAW_LOBSTER, 40, 90, CAGE);
		fish("Raw tuna", ItemID.RAW_TUNA, 35, 80, HARPOON);
		fish("Raw swordfish", ItemID.RAW_SWORDFISH, 50, 100, HARPOON);
		fish("Raw shark", ItemID.RAW_SHARK, 76, 110, SHARK_HARPOON);
		fish("Raw monkfish", ItemID.RAW_MONKFISH, 62, 120, MONKFISH);
		fish("Raw karambwan", ItemID.TBWT_RAW_KARAMBWAN, 65, 50, KARAMBWAN);
		fish("Raw anglerfish", ItemID.RAW_ANGLERFISH, 82, 120, ANGLERFISH);
		fish("Raw dark crab", ItemID.RAW_DARK_CRAB, 85, 130, DARK_CRAB);
		fish("Leaping trout", ItemID.BRUT_SPAWNING_TROUT, 48, 50, BARBARIAN);
		fish("Leaping salmon", ItemID.BRUT_SPAWNING_SALMON, 58, 70, BARBARIAN);
		fish("Leaping sturgeon", ItemID.BRUT_STURGEON, 70, 80, BARBARIAN);
		// OSRS Wiki, Aerial fishing: Fishing level, Fishing XP and Hunter level for each.
		aerial("Bluegill", ItemID.AERIAL_FISHING_BLUEGILL, 43, 11.5, 35);
		aerial("Common tench", ItemID.AERIAL_FISHING_COMMON_TENCH, 56, 40, 51);
		aerial("Mottled eel", ItemID.AERIAL_FISHING_MOTTLED_EEL, 73, 65, 68);
		aerial("Greater siren", ItemID.AERIAL_FISHING_GREATER_SIREN, 91, 100, 87);

		tool("Small fishing net", Gear.SMALL_NET, AnimationID.HUMAN_SMALLNET);
		tool("Big fishing net", Gear.BIG_NET, AnimationID.HUMAN_LARGENET);
		tool("Lobster pot", Gear.CAGE, AnimationID.HUMAN_LOBSTER);
		tool("Harpoon", Gear.HARPOON, AnimationID.HUMAN_HARPOON);
		tool("Barb-tail harpoon", Gear.HARPOON, AnimationID.HUMAN_HARPOON_BARBED);
		tool("Dragon harpoon", Gear.HARPOON, AnimationID.HUMAN_HARPOON_DRAGON,
			AnimationID.HUMAN_HARPOON_TRAILBLAZER_NO_INFERNAL, AnimationID.HUMAN_HARPOON_TRAILBLAZER_RELOADED_NO_INFERNAL);
		tool("Infernal harpoon", Gear.HARPOON, AnimationID.HUMAN_HARPOON_INFERNAL,
			AnimationID.HUMAN_HARPOON_TRAILBLAZER, AnimationID.HUMAN_HARPOON_TRAILBLAZER_RELOADED);
		tool("Crystal harpoon", Gear.HARPOON, AnimationID.HUMAN_HARPOON_CRYSTAL);
		tool("Trailblazer harpoon", Gear.HARPOON, AnimationID.HUMAN_HARPOON_LEAGUE_TRAILBLAZER);
		// Barbarian fishing for tuna, swordfish and sharks without a harpoon.
		tool("Bare hands", Gear.HARPOON, AnimationID.BRUT_PLAYER_HAND_FISHING_START, AnimationID.BRUT_PLAYER_HAND_FISHING_READY,
			AnimationID.BRUT_PLAYER_HAND_FISHING_END_SHARK_1, AnimationID.BRUT_PLAYER_HAND_FISHING_END_SHARK_2,
			AnimationID.BRUT_PLAYER_HAND_FISHING_END_SWORDFISH_1, AnimationID.BRUT_PLAYER_HAND_FISHING_END_SWORDFISH_2,
			AnimationID.BRUT_PLAYER_HAND_FISHING_END_TUNA_1, AnimationID.BRUT_PLAYER_HAND_FISHING_END_TUNA_2,
			AnimationID.BRUT_PLAYER_HAND_FISHING_END_BLANK);
		// A rod is cast the same way for bait and for flies; the method tells which rod it is.
		tool("Fishing rod", Gear.ROD, AnimationID.HUMAN_FISHING_CASTING, AnimationID.HUMAN_FISH_ONSPOT);
		tool("Pearl fishing rod", Gear.ROD, AnimationID.HUMAN_FISHING_CASTING_PEARL, AnimationID.HUMAN_FISH_ONSPOT_PEARL);
		tool("Pearl fly fishing rod", Gear.FLY_ROD, AnimationID.HUMAN_FISHING_CASTING_PEARL_FLY, AnimationID.HUMAN_FISH_ONSPOT_PEARL_FLY);
		tool("Barbarian rod", Gear.BARBARIAN_ROD, AnimationID.HUMAN_FISHING_ONSPOT_BRUT);
		tool("Pearl barbarian rod", Gear.BARBARIAN_ROD, AnimationID.HUMAN_FISHING_CASTING_PEARL_BRUT,
			AnimationID.HUMAN_FISH_ONSPOT_PEARL_BRUT);

		// OSRS Wiki, Clue bottle: "Clue bottles are bottles that can be found while training Fishing."
		EXTRAS.put(ItemID.FISHING_CLUE_BOTTLE_BEGINNER, "Clue bottle (beginner)");
		EXTRAS.put(ItemID.FISHING_CLUE_BOTTLE_EASY, "Clue bottle (easy)");
		EXTRAS.put(ItemID.FISHING_CLUE_BOTTLE_MEDIUM, "Clue bottle (medium)");
		EXTRAS.put(ItemID.FISHING_CLUE_BOTTLE_HARD, "Clue bottle (hard)");
		EXTRAS.put(ItemID.FISHING_CLUE_BOTTLE_ELITE, "Clue bottle (elite)");
		// OSRS Wiki, Big fishing net: its rolls include "Oyster Casket".
		EXTRAS.put(ItemID.CASKET, "Casket");
		EXTRAS.put(ItemID.OYSTERSHELL, "Oyster");
		// OSRS Wiki, Big fish: "Big bass (1/1,000 per raw bass caught)", big swordfish 1/2,500, big shark 1/5,000.
		EXTRAS.put(ItemID.POH_TROPHYDROP_BASS, "Big bass");
		EXTRAS.put(ItemID.POH_TROPHYDROP_SWORDFISH, "Big swordfish");
		EXTRAS.put(ItemID.POH_TROPHYDROP_SHARK, "Big shark");
		// OSRS Wiki, Aerial fishing: Molch pearls, and the golden tench "in place of a regular catch".
		EXTRAS.put(PEARL, PEARL_NAME);
		EXTRAS.put(ItemID.GOLDEN_TENCH, "Golden tench");
	}

	/** The chance of a big fish for each of its fish, as the wiki gives it (OSRS Wiki, Big fish). */
	public static final Map<String, Integer> BIG_FISH_ONE_IN;

	static
	{
		Map<String, Integer> big = new LinkedHashMap<>();
		big.put("Big bass", 1_000);
		big.put("Big swordfish", 2_500);
		big.put("Big shark", 5_000);
		BIG_FISH_ONE_IN = Collections.unmodifiableMap(big);
	}

	/** The fish each big fish comes from. */
	public static String bigFrom(String big)
	{
		switch (big)
		{
			case "Big bass":
				return "Raw bass";
			case "Big swordfish":
				return "Raw swordfish";
			case "Big shark":
				return "Raw shark";
			default:
				return null;
		}
	}

	/**
	 * Molch pearls for each aerial catch, as the wiki states the range: "each catch has a 1/66.9 to
	 * 1/42 chance of giving Molch pearls ... which scales based on the player's Fishing and Hunter
	 * levels" (OSRS Wiki, Aerial fishing). The page notes the rate changed lately, so it is only
	 * shown as the wiki's range, never as a firm figure.
	 */
	public static final double PEARL_ONE_IN_LOW_LEVELS = 66.9;
	public static final double PEARL_ONE_IN_HIGH_LEVELS = 42;

	/** What Alry the Angler's Angling Accessories sells for Molch pearls, as the wiki prices it. */
	public enum PearlGoal
	{
		NONE("None", 0, -1),
		PEARL_FISHING_ROD("Pearl fishing rod", 100, ItemID.FISHINGROD_PEARL),
		PEARL_FLY_FISHING_ROD("Pearl fly fishing rod", 120, ItemID.FISHINGROD_PEARL_FLY),
		PEARL_BARBARIAN_ROD("Pearl barbarian rod", 150, ItemID.FISHINGROD_PEARL_BRUT),
		ANGLER_OUTFIT("Angler outfit, all four pieces", 200, ItemID.TRAWLER_REWARD_TOP),
		FISH_SACK("Fish sack", 500, ItemID.FISH_SACK);

		public final String item;
		/** Its price in Molch pearls. */
		public final int price;
		public final int itemId;

		PearlGoal(String item, int price, int itemId)
		{
			this.item = item;
			this.price = price;
			this.itemId = itemId;
		}

		@Override
		public String toString()
		{
			return this == NONE ? "None" : item + " (" + price + ")";
		}
	}

	/**
	 * How long a fishing spot stays put, in ticks (OSRS Wiki, Fishing spot): "Most fishing spots take
	 * a minimum of 250 ticks (2.5 minutes) to move, and a maximum of 530 ticks (around 5 minutes)";
	 * "boat fishing spots never move"; "fishing spots for Sacred eel and Infernal eel move between 100
	 * ticks (1 minute) and 300 ticks (3 minutes)"; "fishing spots for Anglerfish move between 8 ticks
	 * (5 seconds) and 830 ticks (8.3 minutes)". The game never says when, so any time from these is
	 * an estimate.
	 */
	public enum Stay
	{
		MOST(250, 530),
		EELS(100, 300),
		ANGLERFISH(8, 830),
		NEVER(0, 0);

		public final int minTicks;
		public final int maxTicks;

		Stay(int minTicks, int maxTicks)
		{
			this.minTicks = minTicks;
			this.maxTicks = maxTicks;
		}
	}

	private Fish()
	{
	}

	private static Method method(String name, String spot, Gear gear, String tool, boolean plain)
	{
		Method m = new Method(name, spot, gear, tool, plain);
		METHODS.add(m);
		METHOD_BY_NAME.put(name.toLowerCase(Locale.ENGLISH), m);
		return m;
	}

	private static void fish(String store, int item, int level, double xp, Method m)
	{
		add(new Kind(store, item, level, xp, m, 0));
	}

	private static void aerial(String store, int item, int level, double xp, int hunter)
	{
		add(new Kind(store, item, level, xp, AERIAL, hunter));
	}

	private static void add(Kind k)
	{
		ALL.add(k);
		k.method.fish.add(k);
		BY_ITEM.put(k.item, k);
		BY_STORE.put(k.store.toLowerCase(Locale.ENGLISH), k);
	}

	private static void tool(String name, Gear gear, int... animations)
	{
		Tool t = new Tool(name, gear);
		for (int a : animations)
		{
			TOOLS.put(a, t);
		}
	}

	/** Every way of fishing, as listed. */
	public static List<Method> methods()
	{
		return Collections.unmodifiableList(METHODS);
	}

	/** Every fish, by method. */
	public static List<Kind> all()
	{
		return Collections.unmodifiableList(ALL);
	}

	/** A method by its name in the log, or null. */
	public static Method method(String name)
	{
		return name == null ? null : METHOD_BY_NAME.get(name.toLowerCase(Locale.ENGLISH));
	}

	/** The fish an item is, or null. */
	public static Kind byItem(int itemId)
	{
		return BY_ITEM.get(itemId);
	}

	/** The fish a store is, or null. */
	public static Kind byStore(String store)
	{
		return store == null ? null : BY_STORE.get(store.toLowerCase(Locale.ENGLISH));
	}

	/** The tool a player fishing with this animation holds, or null when it is not a fishing animation known here. */
	public static Tool tool(int animation)
	{
		return TOOLS.get(animation);
	}

	/**
	 * The tool to file a catch under: the one seen in hand when it suits the method (a dragon harpoon
	 * for tuna), else the method's own (a cormorant for aerial fish, which shows no tool).
	 */
	public static String toolFor(Method m, Tool seen)
	{
		if (seen == null)
		{
			return m.tool;
		}
		if (seen.gear == m.gear)
		{
			return seen.name;
		}
		// A plain rod is cast the same way with bait or with flies: on a fly method it is the fly rod.
		return m.tool;
	}

	/** What else fishing turned up, by item id, or null. */
	public static String extra(int itemId)
	{
		return EXTRAS.get(itemId);
	}

	/** The item of an extra by its name, for its picture; -1 when not known. */
	public static int extraItem(String name)
	{
		for (Map.Entry<Integer, String> e : EXTRAS.entrySet())
		{
			if (e.getValue().equals(name))
			{
				return e.getKey();
			}
		}
		return -1;
	}

	/** Whether an extra is a clue bottle. */
	public static boolean isClue(String name)
	{
		return name != null && name.startsWith("Clue bottle");
	}

	/**
	 * The fish a catch message names, for a catch that went into a fish barrel and so never reached
	 * the pack: "You catch a lobster." gives raw lobster. RuneLite's own Fishing plugin takes "You
	 * catch a", "You catch an" and "You catch some" as a catch; the fish's name after that is matched
	 * against the fish here, the longest first ("leaping trout" before "trout"). Null when none fits.
	 */
	public static Kind fromMessage(String message)
	{
		if (message == null)
		{
			return null;
		}
		String m = message.trim().toLowerCase(Locale.ENGLISH);
		String rest = null;
		for (String p : Arrays.asList("you catch a ", "you catch an ", "you catch some "))
		{
			if (m.startsWith(p))
			{
				rest = m.substring(p.length());
				break;
			}
		}
		if (rest == null)
		{
			return null;
		}
		Kind best = null;
		for (Kind k : ALL)
		{
			String n = k.shortName();
			if ((rest.startsWith(n) || rest.startsWith("raw " + n)) && (best == null || n.length() > best.shortName().length()))
			{
				best = k;
			}
		}
		return best;
	}
}
