package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * The well-trodden ways an ironman gets more of a store: where it is gathered, farmed, looted,
 * bought or handed out each day. Stores without their own entry fall back to their shelf's.
 * Recipes that make a store are found from {@link Recipes}, not listed here.
 */
public final class Ways
{
	/** How a way gets the store, which matches the sources the player's own gains are filed under. */
	public enum Kind
	{
		GATHER(Source.GATHERED),
		MAKE(Source.MADE),
		FARM(Source.FARMED),
		LOOT(Source.LOOTED),
		BUY(Source.BOUGHT),
		DAILY(Source.REWARD),
		REWARD(Source.REWARD);

		private final Source source;

		Kind(Source source)
		{
			this.source = source;
		}

		public Source getSource()
		{
			return source;
		}
	}

	/** One way to get more. */
	public static final class Way
	{
		public final Kind kind;
		public final String text;
		/** The skill it trains or needs, or null. */
		public final Skill skill;
		public final int level;

		Way(Kind kind, String text, Skill skill, int level)
		{
			this.kind = kind;
			this.text = text;
			this.skill = skill;
			this.level = level;
		}
	}

	private static final Map<String, List<Way>> BY_STORE = new HashMap<>();
	private static final Map<Family, List<Way>> BY_SHELF = new HashMap<>();

	static
	{
		// Secondaries
		add("Snape grass", Kind.GATHER, "Pick it on Waterbirth Island", null, 0);
		add("Snape grass", Kind.FARM, "Grow it in allotments", Skill.FARMING, 61);
		add("Red spiders' eggs", Kind.GATHER, "Pick them up in Edgeville Dungeon or the Varrock Sewers", null, 0);
		add("Red spiders' eggs", Kind.LOOT, "Sarachnis and spiders drop them", null, 0);
		add("Limpwurt root", Kind.FARM, "Grow it in flower patches", Skill.FARMING, 26);
		add("Limpwurt root", Kind.LOOT, "Hill giants drop them", null, 0);
		add("White berries", Kind.FARM, "Grow a whiteberry bush", Skill.FARMING, 59);
		add("Wine of Zamorak", Kind.GATHER, "Telegrab it at the Chaos Temple", Skill.MAGIC, 33);
		add("Potato cactus", Kind.FARM, "Grow a potato cactus", Skill.FARMING, 64);
		add("Potato cactus", Kind.LOOT, "Kalphites drop it", null, 0);
		add("Mort myre fungus", Kind.GATHER, "Cast Bloom in Mort Myre Swamp with a blessed silver sickle", null, 0);
		add("Crushed nest", Kind.MAKE, "Crush bird nests from woodcutting and birdhouses", null, 0);
		add("Bird nest", Kind.GATHER, "Birdhouse runs and woodcutting", Skill.HUNTER, 5);
		add("Zulrah's scales", Kind.LOOT, "Zulrah drops them", null, 0);
		add("Dragon scale dust", Kind.LOOT, "Grind blue dragon scales from Taverley Dungeon", null, 0);
		add("Blue dragon scale", Kind.GATHER, "Pick them up in Taverley Dungeon", null, 0);
		add("Jangerberries", Kind.FARM, "Grow a jangerberry bush", Skill.FARMING, 48);
		add("Unicorn horn dust", Kind.LOOT, "Grind unicorn horns from unicorns", null, 0);
		add("Chocolate dust", Kind.BUY, "Grind chocolate bars bought from the Culinaromancer's Chest", null, 0);
		add("Amylase crystal", Kind.REWARD, "Trade marks of grace to Grace in Rogues' Den", Skill.AGILITY, 0);
		add("Lava scale shard", Kind.LOOT, "Grind lava scales from lava dragons", null, 0);
		add("Crushed superior dragon bones", Kind.LOOT, "Crush superior dragon bones from Vorkath", null, 0);
		add("Vial of water", Kind.BUY, "Buy vial packs and fill them at a fountain", null, 0);
		add("Eye of newt", Kind.BUY, "Buy packs from Betty in Port Sarim or Jatix in Taverley", null, 0);
		add("Goat horn dust", Kind.LOOT, "Grind desert goat horns", null, 0);
		add("Volcanic ash", Kind.GATHER, "Mine ash piles on Fossil Island", Skill.MINING, 22);
		add("Nail beast nails", Kind.LOOT, "Nail beasts in the Temple Trekking event", null, 0);
		// Herbs, seeds and runes
		add("Ranarr weed", Kind.FARM, "Herb runs with ranarr seeds", Skill.FARMING, 32);
		add("Snapdragon", Kind.FARM, "Herb runs with snapdragon seeds", Skill.FARMING, 62);
		add("Toadflax", Kind.FARM, "Herb runs with toadflax seeds", Skill.FARMING, 38);
		add("Torstol", Kind.FARM, "Herb runs with torstol seeds", Skill.FARMING, 85);
		add("Ranarr seed", Kind.GATHER, "Pickpocket master farmers", Skill.THIEVING, 38);
		add("Ranarr seed", Kind.GATHER, "Seed nests from birdhouses", Skill.HUNTER, 5);
		add("Snapdragon seed", Kind.GATHER, "Pickpocket master farmers", Skill.THIEVING, 38);
		add("Torstol seed", Kind.GATHER, "Pickpocket master farmers", Skill.THIEVING, 38);
		add("Nature rune", Kind.GATHER, "Craft them at the Nature Altar", Skill.RUNECRAFT, 44);
		add("Nature rune", Kind.BUY, "Magic shops, such as Ali Morrisane's", null, 0);
		add("Law rune", Kind.GATHER, "Craft them at the Law Altar", Skill.RUNECRAFT, 54);
		add("Death rune", Kind.GATHER, "Craft them at the Death Altar", Skill.RUNECRAFT, 65);
		add("Death rune", Kind.BUY, "Lundail's shop at the Mage Arena", null, 0);
		add("Blood rune", Kind.GATHER, "Craft them at the True Blood Altar", Skill.RUNECRAFT, 77);
		add("Blood rune", Kind.LOOT, "Bosses and Slayer monsters drop them", null, 0);
		add("Soul rune", Kind.GATHER, "Craft them at the Soul Altar", Skill.RUNECRAFT, 90);
		add("Astral rune", Kind.GATHER, "Craft them at the Astral Altar", Skill.RUNECRAFT, 40);
		add("Cosmic rune", Kind.GATHER, "Craft them at the Cosmic Altar", Skill.RUNECRAFT, 27);
		add("Chaos rune", Kind.GATHER, "Craft them at the Chaos Altar", Skill.RUNECRAFT, 35);
		add("Chaos rune", Kind.BUY, "Magic shops sell them by the hundred", null, 0);
		add("Wrath rune", Kind.GATHER, "Craft them at the Wrath Altar", Skill.RUNECRAFT, 95);
		add("Fire rune", Kind.BUY, "Magic shops sell them in packs", null, 0);
		add("Air rune", Kind.BUY, "Magic shops sell them in packs", null, 0);
		add("Water rune", Kind.BUY, "Magic shops sell them in packs", null, 0);
		add("Earth rune", Kind.BUY, "Magic shops sell them in packs", null, 0);
		add("Pure essence", Kind.GATHER, "Mine it after Rune Mysteries", Skill.MINING, 30);
		// Food
		add("Raw shark", Kind.GATHER, "Harpoon them at the Fishing Guild or Catherby", Skill.FISHING, 76);
		add("Raw karambwan", Kind.GATHER, "Karambwan vessel at Karamja's fairy ring DKP", Skill.FISHING, 65);
		add("Raw monkfish", Kind.GATHER, "Net them at Piscatoris after Swan Song", Skill.FISHING, 62);
		add("Raw anglerfish", Kind.GATHER, "Fish them in Port Piscarilius with sandworms", Skill.FISHING, 82);
		add("Raw dark crab", Kind.GATHER, "Cage them in the Wilderness Resource Area", Skill.FISHING, 85);
		add("Raw lobster", Kind.GATHER, "Cage them at Catherby or Karamja", Skill.FISHING, 40);
		add("Raw swordfish", Kind.GATHER, "Harpoon them at Catherby or Karamja", Skill.FISHING, 50);
		add("Raw manta ray", Kind.REWARD, "Fishing Trawler rewards", Skill.FISHING, 15);
		add("Raw sea turtle", Kind.REWARD, "Fishing Trawler rewards", Skill.FISHING, 15);
		add("Raw tuna", Kind.GATHER, "Harpoon them at Catherby or Karamja", Skill.FISHING, 35);
		add("Raw salmon", Kind.GATHER, "Fly fish at Barbarian Village or Shilo", Skill.FISHING, 30);
		add("Raw trout", Kind.GATHER, "Fly fish at Barbarian Village or Lumbridge", Skill.FISHING, 20);
		// Prayer
		add("Dragon bones", Kind.LOOT, "Every dragon drops them", null, 0);
		add("Superior dragon bones", Kind.LOOT, "Vorkath drops two a kill", null, 0);
		add("Wyrm bones", Kind.LOOT, "Wyrms in the Karuulm Slayer Dungeon", Skill.SLAYER, 62);
		add("Drake bones", Kind.LOOT, "Drakes in the Karuulm Slayer Dungeon", Skill.SLAYER, 84);
		add("Hydra bones", Kind.LOOT, "Hydras and Alchemical Hydra", Skill.SLAYER, 95);
		add("Dagannoth bones", Kind.LOOT, "The Dagannoth Kings", null, 0);
		add("Big bones", Kind.LOOT, "Hill giants and ogres drop them", null, 0);
		// Materials
		add("Coal", Kind.GATHER, "Mine it, or take it from the Motherlode Mine", Skill.MINING, 30);
		add("Iron ore", Kind.GATHER, "Mine it", Skill.MINING, 15);
		add("Mithril ore", Kind.GATHER, "Mine it", Skill.MINING, 55);
		add("Adamantite ore", Kind.GATHER, "Mine it", Skill.MINING, 70);
		add("Runite ore", Kind.GATHER, "Mine it", Skill.MINING, 85);
		add("Gold ore", Kind.GATHER, "Mine it, or take it from the Motherlode Mine", Skill.MINING, 40);
		add("Logs", Kind.GATHER, "Chop trees", Skill.WOODCUTTING, 1);
		add("Oak logs", Kind.GATHER, "Chop oak trees", Skill.WOODCUTTING, 15);
		add("Willow logs", Kind.GATHER, "Chop willow trees", Skill.WOODCUTTING, 30);
		add("Teak logs", Kind.GATHER, "Chop teak trees", Skill.WOODCUTTING, 35);
		add("Maple logs", Kind.GATHER, "Chop maple trees", Skill.WOODCUTTING, 45);
		add("Mahogany logs", Kind.GATHER, "Chop mahogany trees", Skill.WOODCUTTING, 50);
		add("Yew logs", Kind.GATHER, "Chop yew trees", Skill.WOODCUTTING, 60);
		add("Magic logs", Kind.GATHER, "Chop magic trees", Skill.WOODCUTTING, 75);
		add("Redwood logs", Kind.GATHER, "Chop redwood trees", Skill.WOODCUTTING, 90);
		add("Plank", Kind.BUY, "Have logs made into planks at a sawmill", null, 0);
		add("Oak plank", Kind.BUY, "Have oak logs made into planks at a sawmill", null, 0);
		add("Teak plank", Kind.BUY, "Have teak logs made into planks at a sawmill", null, 0);
		add("Mahogany plank", Kind.BUY, "Have mahogany logs made into planks at a sawmill", null, 0);
		add("Mahogany plank", Kind.REWARD, "Mahogany Homes rewards", Skill.CONSTRUCTION, 1);
		add("Feather", Kind.BUY, "Buy feather packs at fishing shops", null, 0);
		add("Bucket of sand", Kind.DAILY, "Bert gives 84 a day after The Hand in the Sand", null, 0);
		add("Soda ash", Kind.GATHER, "Burn seaweed or giant seaweed", null, 0);
		add("Giant seaweed", Kind.FARM, "Grow it in seaweed patches on Fossil Island", Skill.FARMING, 23);
		add("Seaweed", Kind.GATHER, "Pick it on beaches, or net it at Catherby", null, 0);
		add("Flax", Kind.GATHER, "Pick it at Seers' Village", null, 0);
		add("Flax", Kind.DAILY, "Your Kingdom of Miscellania's herb workers", null, 0);
		add("Bow string", Kind.DAILY, "The Flax keeper in Seers' Village swaps flax for them daily with the Kandarin diary", null, 0);
		add("Battlestaff", Kind.DAILY, "Zaff sells battlestaves daily with the Varrock diary", null, 0);
		add("Battlestaff", Kind.BUY, "Zaff's shop in Varrock", null, 0);
		add("Green dragonhide", Kind.LOOT, "Green dragons", null, 0);
		add("Blue dragonhide", Kind.LOOT, "Blue dragons in Taverley Dungeon", null, 0);
		add("Red dragonhide", Kind.LOOT, "Red dragons in the Brimhaven Dungeon", null, 0);
		add("Black dragonhide", Kind.LOOT, "Black dragons", null, 0);
		add("Uncut sapphire", Kind.GATHER, "Gem rocks at Shilo Village", Skill.MINING, 40);
		add("Uncut diamond", Kind.GATHER, "Gem rocks at Shilo Village", Skill.MINING, 40);
		add("Amethyst", Kind.GATHER, "Mine it in the Mining Guild", Skill.MINING, 92);
		add("Molten glass", Kind.MAKE, "Cast Superglass Make on sand and seaweed", Skill.MAGIC, 77);
		add("Air orb", Kind.MAKE, "Charge unpowered orbs at the Air Obelisk", Skill.MAGIC, 66);
		// Ammunition
		add("Broad bolts", Kind.BUY, "Slayer masters sell unfinished broad bolts once Broader Fletching is unlocked", Skill.FLETCHING, 55);
		add("Broad arrows", Kind.BUY, "Slayer masters sell broad arrowheads once Broader Fletching is unlocked", Skill.FLETCHING, 52);
		add("Chinchompa", Kind.GATHER, "Box trap them in Piscatoris", Skill.HUNTER, 53);
		add("Red chinchompa", Kind.GATHER, "Box trap them in the Feldip Hills", Skill.HUNTER, 63);
		add("Black chinchompa", Kind.GATHER, "Box trap them in the Wilderness", Skill.HUNTER, 73);

		shelf(Family.HERBS, Kind.FARM, "Herb runs", Skill.FARMING, 9);
		shelf(Family.HERBS, Kind.GATHER, "Hunt the herbiboar on Fossil Island", Skill.HUNTER, 80);
		shelf(Family.SEEDS, Kind.GATHER, "Pickpocket master farmers", Skill.THIEVING, 38);
		shelf(Family.SEEDS, Kind.GATHER, "Seed nests from birdhouse runs", Skill.HUNTER, 5);
		shelf(Family.RUNES, Kind.GATHER, "Craft them at their altar", Skill.RUNECRAFT, 1);
		shelf(Family.RUNES, Kind.BUY, "Magic shops", null, 0);
		shelf(Family.PRAYER, Kind.LOOT, "Monsters drop them", null, 0);
		shelf(Family.AMMO, Kind.LOOT, "Monsters and bosses drop them", null, 0);
		shelf(Family.FOOD, Kind.GATHER, "Fish and cook it", Skill.FISHING, 1);
	}

	private Ways()
	{
	}

	private static void add(String store, Kind kind, String text, Skill skill, int level)
	{
		BY_STORE.computeIfAbsent(key(store), k -> new ArrayList<>()).add(new Way(kind, text, skill, level));
	}

	private static void shelf(Family family, Kind kind, String text, Skill skill, int level)
	{
		BY_SHELF.computeIfAbsent(family, k -> new ArrayList<>()).add(new Way(kind, text, skill, level));
	}

	private static String key(String store)
	{
		return store.toLowerCase(Locale.ENGLISH);
	}

	/** The store's own ways, or its shelf's when it has none. */
	public static List<Way> of(String store)
	{
		List<Way> own = BY_STORE.get(key(store));
		if (own != null)
		{
			return Collections.unmodifiableList(own);
		}
		List<Way> shelf = BY_SHELF.get(Catalogue.family(store));
		return shelf == null ? Collections.emptyList() : Collections.unmodifiableList(shelf);
	}
}
