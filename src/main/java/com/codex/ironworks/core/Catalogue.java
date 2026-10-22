package com.codex.ironworks.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The stores an ironman lives on, by the game's own item names. Every item is filed under one store:
 * potions by their name without the dose, counted in doses, so a four-dose and a one-dose prayer
 * potion are five doses of one store; grimy and clean herbs are one store too. Noted items are
 * already turned into their own item before they get here. Matching is by name, so a store the
 * player never holds is simply never shown.
 */
public final class Catalogue
{
	private static final Pattern DOSE = Pattern.compile("^(.+?)\\s*\\((\\d)\\)$");
	private static final String GRIMY = "grimy ";
	/** Items a player buys in packs: opening one is a purchase, not a reward. */
	private static final String PACK = " pack";

	private static final Map<String, String> NAMES = new HashMap<>();
	private static final Map<String, Family> SHELF = new HashMap<>();
	/** Stores that are gathered rather than made, so a tick with both kinds of experience files them right. */
	private static final Set<String> GATHERED = new HashSet<>();

	static
	{
		shelf(Family.FOOD,
			"Shark", "Manta ray", "Anglerfish", "Dark crab", "Cooked karambwan", "Monkfish", "Lobster", "Swordfish",
			"Sea turtle", "Tuna potato", "Pineapple pizza", "Cooked moonlight antelope", "Cooked dashing kebbit",
			"Cooked sunlight antelope", "Cooked pyre fox", "Raw shark", "Raw manta ray", "Raw anglerfish", "Raw dark crab",
			"Raw karambwan", "Raw monkfish", "Raw lobster", "Raw swordfish", "Raw sea turtle", "Tuna", "Salmon", "Trout",
			"Raw tuna", "Raw salmon", "Raw trout");
		shelf(Family.POTIONS,
			"Prayer potion", "Super restore", "Saradomin brew", "Super combat potion", "Divine super combat potion",
			"Ranging potion", "Divine ranging potion", "Bastion potion", "Divine bastion potion", "Magic potion",
			"Divine magic potion", "Stamina potion", "Super energy", "Antidote++", "Anti-venom+", "Anti-venom",
			"Extended antifire", "Super antifire potion", "Extended super antifire", "Antifire potion", "Superantipoison",
			"Sanfew serum", "Super attack", "Super strength", "Super defence", "Zamorak brew", "Menaphite remedy",
			"Prayer regeneration potion", "Forgotten brew", "Ancient brew", "Attack potion", "Restore potion", "Energy potion",
			"Combat potion");
		shelf(Family.UNFINISHED,
			"Ranarr potion (unf)", "Snapdragon potion (unf)", "Toadflax potion (unf)", "Torstol potion (unf)",
			"Irit potion (unf)", "Kwuarm potion (unf)", "Cadantine potion (unf)", "Lantadyme potion (unf)",
			"Dwarf weed potion (unf)", "Avantoe potion (unf)", "Guam potion (unf)", "Harralander potion (unf)");
		shelf(Family.HERBS,
			"Guam leaf", "Marrentill", "Tarromin", "Harralander", "Ranarr weed", "Toadflax", "Irit leaf", "Avantoe", "Kwuarm",
			"Huasca", "Snapdragon", "Cadantine", "Lantadyme", "Dwarf weed", "Torstol");
		shelf(Family.SECONDARIES,
			"Vial of water", "Eye of newt", "Snape grass", "Red spiders' eggs", "Limpwurt root", "White berries",
			"Wine of Zamorak", "Potato cactus", "Mort myre fungus", "Crushed nest", "Bird nest", "Zulrah's scales",
			"Dragon scale dust", "Jangerberries", "Unicorn horn dust", "Chocolate dust", "Crushed superior dragon bones",
			"Amylase crystal", "Lava scale shard", "Goat horn dust", "Kebbit teeth dust", "Nail beast nails", "Yew roots",
			"Magic roots", "Coconut milk", "Toad's legs", "Cave nightshade", "Ground mud runes", "Ashes", "Volcanic ash",
			"Silver dust", "Poison ivy berries", "Cactus spine", "Blue dragon scale");
		shelf(Family.SEEDS,
			"Guam seed", "Marrentill seed", "Tarromin seed", "Harralander seed", "Ranarr seed", "Toadflax seed", "Irit seed",
			"Avantoe seed", "Kwuarm seed", "Huasca seed", "Snapdragon seed", "Cadantine seed", "Lantadyme seed",
			"Dwarf weed seed", "Torstol seed", "Snape grass seed", "Potato cactus seed", "Limpwurt seed", "Watermelon seed",
			"White lily seed", "Belladonna seed", "Mushroom spore", "Poison ivy seed", "Whiteberry seed", "Acorn",
			"Willow seed", "Maple seed", "Yew seed", "Magic seed", "Teak seed", "Mahogany seed", "Palm tree seed",
			"Papaya tree seed", "Dragonfruit tree seed", "Celastrus seed", "Redwood tree seed", "Spirit seed",
			"Calquat tree seed", "Crystal acorn", "Hespori seed");
		shelf(Family.RUNES,
			"Air rune", "Water rune", "Earth rune", "Fire rune", "Mind rune", "Body rune", "Chaos rune", "Cosmic rune",
			"Nature rune", "Law rune", "Death rune", "Blood rune", "Soul rune", "Astral rune", "Wrath rune", "Sunfire rune",
			"Aether rune", "Dust rune", "Mist rune", "Mud rune", "Smoke rune", "Steam rune", "Lava rune");
		shelf(Family.AMMO,
			"Ruby dragon bolts (e)", "Diamond dragon bolts (e)", "Onyx dragon bolts (e)", "Dragonstone dragon bolts (e)",
			"Ruby bolts (e)", "Diamond bolts (e)", "Onyx bolts (e)", "Dragonstone bolts (e)", "Broad bolts",
			"Amethyst broad bolts", "Adamant bolts", "Runite bolts", "Dragon bolts", "Mithril bolts", "Bronze arrow",
			"Iron arrow", "Steel arrow", "Mithril arrow", "Adamant arrow", "Rune arrow", "Amethyst arrow", "Dragon arrow",
			"Broad arrows", "Mithril dart", "Adamant dart", "Rune dart", "Amethyst dart", "Dragon dart", "Steel cannonball",
			"Granite cannonball", "Chinchompa", "Red chinchompa", "Black chinchompa", "Rune javelin", "Dragon javelin",
			"Amethyst javelin");
		shelf(Family.PRAYER,
			"Bones", "Big bones", "Babydragon bones", "Dragon bones", "Wyvern bones", "Lava dragon bones", "Dagannoth bones",
			"Superior dragon bones", "Hydra bones", "Drake bones", "Wyrm bones", "Wyrmling bones", "Frost dragon bones",
			"Ourg bones", "Fiendish ashes", "Vile ashes", "Malicious ashes", "Abyssal ashes", "Infernal ashes",
			"Ensouled dragon head", "Ensouled giant head");
		shelf(Family.MATERIALS,
			"Coal", "Iron ore", "Mithril ore", "Adamantite ore", "Runite ore", "Gold ore", "Silver ore", "Iron bar",
			"Steel bar", "Mithril bar", "Adamantite bar", "Runite bar", "Gold bar", "Silver bar", "Logs", "Oak logs",
			"Willow logs", "Teak logs", "Maple logs", "Mahogany logs", "Yew logs", "Magic logs", "Redwood logs", "Plank",
			"Oak plank", "Teak plank", "Mahogany plank", "Pure essence", "Daeyalt essence", "Feather", "Bucket of sand",
			"Soda ash", "Seaweed", "Giant seaweed", "Molten glass", "Flax", "Bow string", "Green dragonhide",
			"Blue dragonhide", "Red dragonhide", "Black dragonhide", "Green dragon leather", "Blue dragon leather",
			"Red dragon leather", "Black dragon leather", "Uncut sapphire", "Uncut emerald", "Uncut ruby", "Uncut diamond",
			"Uncut dragonstone", "Amethyst", "Steel nails", "Bolt of cloth", "Limestone brick", "Marble block", "Soft clay",
			"Air orb", "Unpowered orb", "Battlestaff");
		shelf(Family.CURRENCY,
			"Coins", "Tokkul", "Trading sticks", "Numulite");
		for (String name : new String[]{
			"Coal", "Iron ore", "Mithril ore", "Adamantite ore", "Runite ore", "Gold ore", "Silver ore", "Logs", "Oak logs",
			"Willow logs", "Teak logs", "Maple logs", "Mahogany logs", "Yew logs", "Magic logs", "Redwood logs", "Pure essence",
			"Daeyalt essence", "Feather", "Seaweed", "Giant seaweed", "Flax", "Green dragonhide", "Blue dragonhide",
			"Red dragonhide", "Black dragonhide", "Uncut sapphire", "Uncut emerald", "Uncut ruby", "Uncut diamond",
			"Uncut dragonstone", "Amethyst", "Chinchompa", "Red chinchompa", "Black chinchompa", "Snape grass",
			"Red spiders' eggs", "Limpwurt root", "White berries", "Potato cactus", "Mort myre fungus", "Bird nest",
			"Zulrah's scales", "Jangerberries", "Cave nightshade", "Poison ivy berries", "Cactus spine", "Blue dragon scale",
			"Yew roots", "Magic roots", "Amylase crystal", "Lava scale shard", "Nail beast nails", "Toad's legs",
			"Volcanic ash", "Ashes"})
		{
			GATHERED.add(key(name));
		}
	}

	private Catalogue()
	{
	}

	private static void shelf(Family family, String... names)
	{
		for (String name : names)
		{
			NAMES.put(key(name), name);
			SHELF.put(key(name), family);
		}
	}

	private static String key(String name)
	{
		return name.toLowerCase(Locale.ENGLISH).trim();
	}

	/** One item as a store sees it: which store, and how many of the store's units the item is. */
	public static final class Match
	{
		public final String store;
		public final Family family;
		public final int units;
		/** Whether this item is the one to picture the store by: a full potion, a clean herb. */
		public final boolean model;

		Match(String store, Family family, int units, boolean model)
		{
			this.store = store;
			this.family = family;
			this.units = units;
			this.model = model;
		}
	}

	/** The catalogue alone, with no picks of the player's own. */
	public static Catalogue.Lookup standard()
	{
		return new Lookup(Collections.emptySet(), Collections.emptySet());
	}

	/**
	 * The catalogue as one player has it: with items they asked to track added, and stores they
	 * never want to see taken out.
	 */
	public static Lookup with(String picked, String hidden)
	{
		return new Lookup(names(picked), names(hidden));
	}

	private static Set<String> names(String list)
	{
		Set<String> out = new HashSet<>();
		if (list == null)
		{
			return out;
		}
		for (String part : list.split("[,\\n]"))
		{
			String k = key(part);
			if (!k.isEmpty())
			{
				out.add(k);
			}
		}
		return out;
	}

	/** Finds the store an item belongs to. Immutable, so it can be shared between threads. */
	public static final class Lookup
	{
		private final Set<String> picked;
		private final Set<String> hidden;

		private Lookup(Set<String> picked, Set<String> hidden)
		{
			this.picked = picked;
			this.hidden = hidden;
		}

		/** @return the store the named item is filed under, or null when it is not one */
		public Match match(String itemName)
		{
			if (itemName == null)
			{
				return null;
			}
			String name = itemName.trim();
			if (name.isEmpty() || "null".equalsIgnoreCase(name))
			{
				return null;
			}
			Matcher dose = DOSE.matcher(name);
			if (dose.matches())
			{
				Match m = find(dose.group(1), Integer.parseInt(dose.group(2)), false);
				if (m != null && m.family == Family.POTIONS)
				{
					return new Match(m.store, m.family, m.units, m.units == 4);
				}
			}
			String k = key(name);
			if (k.startsWith(GRIMY))
			{
				Match m = find(k.substring(GRIMY.length()), 1, false);
				if (m != null && m.family == Family.HERBS)
				{
					return m;
				}
			}
			return find(name, 1, true);
		}

		private Match find(String name, int units, boolean model)
		{
			String k = key(name);
			if (hidden.contains(k))
			{
				return null;
			}
			Family family = SHELF.get(k);
			if (family != null)
			{
				return new Match(NAMES.get(k), family, units, model);
			}
			if (picked.contains(k))
			{
				// The player's own spelling may differ in case from the game's; the store takes the game's.
				return new Match(name.trim(), Family.PICKED, units, model);
			}
			return null;
		}

		/** The store a pack opens into, such as feathers from a feather pack, or null. */
		public String packOf(String itemName)
		{
			if (itemName == null)
			{
				return null;
			}
			String k = key(itemName);
			if (!k.endsWith(PACK))
			{
				return null;
			}
			Match m = match(itemName.trim().substring(0, itemName.trim().length() - PACK.length()));
			if (m == null)
			{
				// "Eye of newt pack" opens into eye of newt, but some packs are named for the plural.
				String singular = k.substring(0, k.length() - PACK.length());
				m = singular.endsWith("s") ? match(singular.substring(0, singular.length() - 1)) : null;
			}
			return m == null ? null : m.store;
		}
	}

	/** Whether a store is gathered (fished, mined, picked, chopped) rather than made from something else. */
	public static boolean gathered(String store)
	{
		return store != null && (GATHERED.contains(key(store)) || key(store).startsWith("raw ")
			|| SHELF.get(key(store)) == Family.HERBS || SHELF.get(key(store)) == Family.SEEDS
			|| SHELF.get(key(store)) == Family.PRAYER);
	}

	/** The shelf a store sits on, or the player's own shelf for one they picked. */
	public static Family family(String store)
	{
		Family f = store == null ? null : SHELF.get(key(store));
		return f == null ? Family.PICKED : f;
	}

	/** Every store in the catalogue, by its game name. */
	public static Set<String> all()
	{
		return Collections.unmodifiableSet(new HashSet<>(NAMES.values()));
	}

	/** Whether the store is money rather than supplies: it is never counted as self-made or run short. */
	public static boolean money(String store)
	{
		return family(store) == Family.CURRENCY;
	}
}
