package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * The recipes an ironman turns stores into experience with. Levels and experience come from
 * RuneLite's own skill calculator; the inputs are the game's. Potions are counted in doses, as
 * their stores are, so a three-dose potion is one making that adds three doses.
 */
public final class Recipes
{
	private static final List<Recipe> ALL = new ArrayList<>();

	static
	{
		r(Skill.HERBLORE, 1, 0f, "Guam potion (unf)", 91, 1, 0, "Guam leaf", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Harralander potion (unf)", 97, 1, 0, "Harralander", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Ranarr potion (unf)", 99, 1, 0, "Ranarr weed", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Toadflax potion (unf)", 3002, 1, 0, "Toadflax", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Irit potion (unf)", 101, 1, 0, "Irit leaf", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Avantoe potion (unf)", 103, 1, 0, "Avantoe", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Kwuarm potion (unf)", 105, 1, 0, "Kwuarm", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Snapdragon potion (unf)", 3004, 1, 0, "Snapdragon", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Cadantine potion (unf)", 107, 1, 0, "Cadantine", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Lantadyme potion (unf)", 2483, 1, 0, "Lantadyme", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Dwarf weed potion (unf)", 109, 1, 0, "Dwarf weed", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 1, 0f, "Torstol potion (unf)", 111, 1, 0, "Torstol", 1, "Vial of water", 1);
		r(Skill.HERBLORE, 3, 25f, "Attack potion(3)", 121, 1, 1, "Guam potion (unf)", 1, "Eye of newt", 1);
		r(Skill.HERBLORE, 22, 62.5f, "Restore potion(3)", 127, 1, 1, "Harralander potion (unf)", 1, "Red spiders' eggs", 1);
		r(Skill.HERBLORE, 26, 67.5f, "Energy potion(3)", 3010, 1, 1, "Harralander potion (unf)", 1, "Chocolate dust", 1);
		r(Skill.HERBLORE, 36, 84f, "Combat potion(3)", 9741, 1, 1, "Harralander potion (unf)", 1, "Goat horn dust", 1);
		r(Skill.HERBLORE, 38, 87.5f, "Prayer potion(3)", 139, 1, 1, "Ranarr potion (unf)", 1, "Snape grass", 1);
		r(Skill.HERBLORE, 45, 100f, "Super attack(3)", 145, 1, 1, "Irit potion (unf)", 1, "Eye of newt", 1);
		r(Skill.HERBLORE, 48, 106.3f, "Superantipoison(3)", 181, 1, 1, "Irit potion (unf)", 1, "Unicorn horn dust", 1);
		r(Skill.HERBLORE, 52, 117.5f, "Super energy(3)", 3018, 1, 1, "Avantoe potion (unf)", 1, "Mort myre fungus", 1);
		r(Skill.HERBLORE, 55, 125f, "Super strength(3)", 157, 1, 1, "Kwuarm potion (unf)", 1, "Limpwurt root", 1);
		r(Skill.HERBLORE, 63, 142.5f, "Super restore(3)", 3026, 1, 1, "Snapdragon potion (unf)", 1, "Red spiders' eggs", 1);
		r(Skill.HERBLORE, 66, 150f, "Super defence(3)", 163, 1, 1, "Cadantine potion (unf)", 1, "White berries", 1);
		r(Skill.HERBLORE, 69, 157.5f, "Antifire potion(3)", 2454, 1, 1, "Lantadyme potion (unf)", 1, "Dragon scale dust", 1);
		r(Skill.HERBLORE, 72, 162.5f, "Ranging potion(3)", 169, 1, 1, "Dwarf weed potion (unf)", 1, "Wine of Zamorak", 1);
		r(Skill.HERBLORE, 76, 172.5f, "Magic potion(3)", 3042, 1, 1, "Lantadyme potion (unf)", 1, "Potato cactus", 1);
		r(Skill.HERBLORE, 78, 175f, "Zamorak brew(3)", 189, 1, 1, "Torstol potion (unf)", 1, "Jangerberries", 1);
		r(Skill.HERBLORE, 81, 180f, "Saradomin brew(3)", 6687, 1, 1, "Toadflax potion (unf)", 1, "Crushed nest", 1);
		r(Skill.HERBLORE, 77, 102f, "Stamina potion(4)", 12625, 1, 2, "Super energy", 4, "Amylase crystal", 4);
		r(Skill.HERBLORE, 84, 110f, "Extended antifire(4)", 11951, 1, 2, "Antifire potion", 4, "Lava scale shard", 4);
		r(Skill.HERBLORE, 92, 130f, "Super antifire potion(4)", 21978, 1, 2, "Antifire potion", 4, "Crushed superior dragon bones", 1);
		r(Skill.HERBLORE, 98, 160f, "Extended super antifire(4)", 22209, 1, 3, "Super antifire potion", 4, "Lava scale shard", 4);
		r(Skill.HERBLORE, 87, 120f, "Anti-venom(4)", 12905, 1, 2, "Antidote++", 4, "Zulrah's scales", 20);
		r(Skill.HERBLORE, 94, 125f, "Anti-venom+(4)", 12913, 1, 3, "Anti-venom", 4, "Torstol", 1);
		r(Skill.HERBLORE, 90, 150f, "Super combat potion(4)", 12695, 1, 2, "Torstol", 1, "Super attack", 4, "Super strength", 4, "Super defence", 4);
		r(Skill.PRAYER, 1, 4.5f, "Bones", 526, 0, 9, "Bones", 1);
		r(Skill.PRAYER, 1, 15f, "Big bones", 532, 0, 9, "Big bones", 1);
		r(Skill.PRAYER, 1, 30f, "Babydragon bones", 534, 0, 9, "Babydragon bones", 1);
		r(Skill.PRAYER, 1, 72f, "Dragon bones", 536, 0, 9, "Dragon bones", 1);
		r(Skill.PRAYER, 1, 72f, "Wyvern bones", 6812, 0, 9, "Wyvern bones", 1);
		r(Skill.PRAYER, 1, 85f, "Lava dragon bones", 11943, 0, 9, "Lava dragon bones", 1);
		r(Skill.PRAYER, 1, 125f, "Dagannoth bones", 6729, 0, 9, "Dagannoth bones", 1);
		r(Skill.PRAYER, 70, 150f, "Superior dragon bones", 22124, 0, 9, "Superior dragon bones", 1);
		r(Skill.PRAYER, 1, 110f, "Hydra bones", 22786, 0, 9, "Hydra bones", 1);
		r(Skill.PRAYER, 1, 80f, "Drake bones", 22783, 0, 9, "Drake bones", 1);
		r(Skill.PRAYER, 1, 50f, "Wyrm bones", 22780, 0, 9, "Wyrm bones", 1);
		r(Skill.PRAYER, 1, 21f, "Wyrmling bones", 28899, 0, 9, "Wyrmling bones", 1);
		r(Skill.PRAYER, 1, 100f, "Frost dragon bones", 31729, 0, 9, "Frost dragon bones", 1);
		r(Skill.PRAYER, 1, 140f, "Ourg bones", 4834, 0, 9, "Ourg bones", 1);
		r(Skill.PRAYER, 1, 10f, "Fiendish ashes", 25766, 0, 9, "Fiendish ashes", 1);
		r(Skill.PRAYER, 1, 25f, "Vile ashes", 25769, 0, 9, "Vile ashes", 1);
		r(Skill.PRAYER, 1, 65f, "Malicious ashes", 25772, 0, 9, "Malicious ashes", 1);
		r(Skill.PRAYER, 1, 85f, "Abyssal ashes", 25775, 0, 9, "Abyssal ashes", 1);
		r(Skill.PRAYER, 1, 110f, "Infernal ashes", 25778, 0, 9, "Infernal ashes", 1);
		r(Skill.COOKING, 80, 210f, "Shark", 385, 1, 0, "Raw shark", 1);
		r(Skill.COOKING, 91, 216.3f, "Manta ray", 391, 1, 0, "Raw manta ray", 1);
		r(Skill.COOKING, 84, 230f, "Anglerfish", 13441, 1, 0, "Raw anglerfish", 1);
		r(Skill.COOKING, 90, 215f, "Dark crab", 11936, 1, 0, "Raw dark crab", 1);
		r(Skill.COOKING, 30, 190f, "Cooked karambwan", 3144, 1, 0, "Raw karambwan", 1);
		r(Skill.COOKING, 62, 150f, "Monkfish", 7946, 1, 0, "Raw monkfish", 1);
		r(Skill.COOKING, 40, 120f, "Lobster", 379, 1, 0, "Raw lobster", 1);
		r(Skill.COOKING, 45, 140f, "Swordfish", 373, 1, 0, "Raw swordfish", 1);
		r(Skill.COOKING, 82, 211.3f, "Sea turtle", 397, 1, 0, "Raw sea turtle", 1);
		r(Skill.COOKING, 30, 100f, "Tuna", 361, 1, 0, "Raw tuna", 1);
		r(Skill.COOKING, 25, 90f, "Salmon", 329, 1, 0, "Raw salmon", 1);
		r(Skill.COOKING, 15, 70f, "Trout", 333, 1, 0, "Raw trout", 1);
		r(Skill.CRAFTING, 1, 0f, "Green dragon leather", 1745, 1, 0, "Green dragonhide", 1);
		r(Skill.CRAFTING, 1, 0f, "Blue dragon leather", 2505, 1, 0, "Blue dragonhide", 1);
		r(Skill.CRAFTING, 1, 0f, "Red dragon leather", 2507, 1, 0, "Red dragonhide", 1);
		r(Skill.CRAFTING, 1, 0f, "Black dragon leather", 2509, 1, 0, "Black dragonhide", 1);
		r(Skill.CRAFTING, 63, 186f, "Green d'hide body", 1135, 1, 1, "Green dragon leather", 3);
		r(Skill.CRAFTING, 71, 210f, "Blue d'hide body", 2499, 1, 1, "Blue dragon leather", 3);
		r(Skill.CRAFTING, 77, 234f, "Red d'hide body", 2501, 1, 1, "Red dragon leather", 3);
		r(Skill.CRAFTING, 84, 258f, "Black d'hide body", 2503, 1, 1, "Black dragon leather", 3);
		r(Skill.CRAFTING, 20, 50f, "Sapphire", 1607, 1, 0, "Uncut sapphire", 1);
		r(Skill.CRAFTING, 27, 67.5f, "Emerald", 1605, 1, 0, "Uncut emerald", 1);
		r(Skill.CRAFTING, 34, 85f, "Ruby", 1603, 1, 0, "Uncut ruby", 1);
		r(Skill.CRAFTING, 43, 107.5f, "Diamond", 1601, 1, 0, "Uncut diamond", 1);
		r(Skill.CRAFTING, 55, 137.5f, "Dragonstone", 1615, 1, 0, "Uncut dragonstone", 1);
		r(Skill.CRAFTING, 1, 20f, "Molten glass", 1775, 1, 0, "Bucket of sand", 1, "Soda ash", 1);
		r(Skill.CRAFTING, 46, 52.5f, "Unpowered orb", 567, 1, 1, "Molten glass", 1);
		r(Skill.CRAFTING, 10, 15f, "Bow string", 1777, 1, 0, "Flax", 1);
		r(Skill.CRAFTING, 66, 137.5f, "Air battlestaff", 1397, 1, 3, "Battlestaff", 1, "Air orb", 1);
		r(Skill.MAGIC, 66, 76f, "Air orb", 573, 1, 2, "Unpowered orb", 1, "Cosmic rune", 3, "Air rune", 30);
		r(Skill.FLETCHING, 70, 150f, "Yew longbow", 855, 1, 1, "Yew logs", 1, "Bow string", 1);
		r(Skill.FLETCHING, 85, 183f, "Magic longbow", 859, 1, 1, "Magic logs", 1, "Bow string", 1);
		r(Skill.FLETCHING, 55, 116.5f, "Maple longbow", 851, 1, 1, "Maple logs", 1, "Bow string", 1);
		r(Skill.FLETCHING, 70, 75f, "Yew longbow (u)", 66, 1, 1, "Yew logs", 1);
		r(Skill.FLETCHING, 85, 91.5f, "Magic longbow (u)", 70, 1, 1, "Magic logs", 1);
		r(Skill.FLETCHING, 55, 58.3f, "Maple longbow (u)", 62, 1, 1, "Maple logs", 1);
		r(Skill.FLETCHING, 40, 41.5f, "Willow longbow (u)", 58, 1, 1, "Willow logs", 1);
		r(Skill.FLETCHING, 25, 25f, "Oak longbow (u)", 56, 1, 1, "Oak logs", 1);
		r(Skill.SMITHING, 15, 12.5f, "Iron bar", 2351, 1, 0, "Iron ore", 1);
		r(Skill.SMITHING, 30, 17.5f, "Steel bar", 2353, 1, 0, "Iron ore", 1, "Coal", 2);
		r(Skill.SMITHING, 50, 30f, "Mithril bar", 2359, 1, 0, "Mithril ore", 1, "Coal", 4);
		r(Skill.SMITHING, 70, 37.5f, "Adamantite bar", 2361, 1, 0, "Adamantite ore", 1, "Coal", 6);
		r(Skill.SMITHING, 85, 50f, "Runite bar", 2363, 1, 0, "Runite ore", 1, "Coal", 8);
		r(Skill.SMITHING, 40, 22.5f, "Gold bar", 2357, 1, 0, "Gold ore", 1);
		r(Skill.SMITHING, 20, 13.7f, "Silver bar", 2355, 1, 0, "Silver ore", 1);
		r(Skill.SMITHING, 35, 25.6f, "Steel cannonball", 2, 4, 1, "Steel bar", 1);
		r(Skill.SMITHING, 99, 375f, "Rune platebody", 1127, 1, 1, "Runite bar", 5);
		r(Skill.SMITHING, 88, 312.5f, "Adamant platebody", 1123, 1, 1, "Adamantite bar", 5);
		r(Skill.SMITHING, 68, 250f, "Mithril platebody", 1121, 1, 1, "Mithril bar", 5);
		r(Skill.CONSTRUCTION, 1, 29f, "Plank", 960, 0, 9, "Plank", 1);
		r(Skill.CONSTRUCTION, 1, 60f, "Oak plank", 8778, 0, 9, "Oak plank", 1);
		r(Skill.CONSTRUCTION, 1, 90f, "Teak plank", 8780, 0, 9, "Teak plank", 1);
		r(Skill.CONSTRUCTION, 1, 140f, "Mahogany plank", 8782, 0, 9, "Mahogany plank", 1);
		r(Skill.FIREMAKING, 1, 40f, "Logs", 1511, 0, 9, "Logs", 1);
		r(Skill.FIREMAKING, 15, 60f, "Oak logs", 1521, 0, 9, "Oak logs", 1);
		r(Skill.FIREMAKING, 30, 90f, "Willow logs", 1519, 0, 9, "Willow logs", 1);
		r(Skill.FIREMAKING, 35, 105f, "Teak logs", 6333, 0, 9, "Teak logs", 1);
		r(Skill.FIREMAKING, 45, 135f, "Maple logs", 1517, 0, 9, "Maple logs", 1);
		r(Skill.FIREMAKING, 50, 157.5f, "Mahogany logs", 6332, 0, 9, "Mahogany logs", 1);
		r(Skill.FIREMAKING, 60, 202.5f, "Yew logs", 1515, 0, 9, "Yew logs", 1);
		r(Skill.FIREMAKING, 75, 303.8f, "Magic logs", 1513, 0, 9, "Magic logs", 1);
		r(Skill.FIREMAKING, 90, 350f, "Redwood logs", 19669, 0, 9, "Redwood logs", 1);
		r(Skill.FARMING, 15, 481.3f, "Acorn", 5312, 0, 9, "Acorn", 1);
		r(Skill.FARMING, 30, 1481.5f, "Willow seed", 5313, 0, 9, "Willow seed", 1);
		r(Skill.FARMING, 35, 7325f, "Teak seed", 21486, 0, 9, "Teak seed", 1);
		r(Skill.FARMING, 45, 3448.4f, "Maple seed", 5314, 0, 9, "Maple seed", 1);
		r(Skill.FARMING, 55, 15783f, "Mahogany seed", 21488, 0, 9, "Mahogany seed", 1);
		r(Skill.FARMING, 57, 6218.4f, "Papaya tree seed", 5288, 0, 9, "Papaya tree seed", 1);
		r(Skill.FARMING, 60, 7150.9f, "Yew seed", 5315, 0, 9, "Yew seed", 1);
		r(Skill.FARMING, 68, 10260.6f, "Palm tree seed", 5289, 0, 9, "Palm tree seed", 1);
		r(Skill.FARMING, 72, 12225.5f, "Calquat tree seed", 5290, 0, 9, "Calquat tree seed", 1);
		r(Skill.FARMING, 75, 13913.8f, "Magic seed", 5316, 0, 9, "Magic seed", 1);
		r(Skill.FARMING, 81, 17475f, "Dragonfruit tree seed", 22877, 0, 9, "Dragonfruit tree seed", 1);
		r(Skill.FARMING, 83, 19501.3f, "Spirit seed", 5317, 0, 9, "Spirit seed", 1);
		r(Skill.FARMING, 85, 14334f, "Celastrus seed", 22869, 0, 9, "Celastrus seed", 1);
		r(Skill.FARMING, 90, 22680f, "Redwood tree seed", 22871, 0, 9, "Redwood tree seed", 1);
	}

	private Recipes()
	{
	}

	private static void r(Skill skill, int level, float xp, String product, int item, int count, int tier, Object... inputs)
	{
		Map<String, Integer> in = new LinkedHashMap<>();
		for (int i = 0; i + 1 < inputs.length; i += 2)
		{
			in.put((String) inputs[i], (Integer) inputs[i + 1]);
		}
		ALL.add(new Recipe(skill, level, xp, product, item, count, tier, in));
	}

	public static List<Recipe> all()
	{
		return Collections.unmodifiableList(ALL);
	}

	/** Recipes that take the store, as their main input or a second one. */
	public static List<Recipe> using(String store)
	{
		List<Recipe> out = new ArrayList<>();
		for (Recipe r : ALL)
		{
			if (r.inputs.containsKey(store))
			{
				out.add(r);
			}
		}
		return out;
	}
}
