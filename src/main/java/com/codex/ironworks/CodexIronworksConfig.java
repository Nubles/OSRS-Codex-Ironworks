package com.codex.ironworks;

import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Fish;
import com.codex.ironworks.core.Stars;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CodexIronworksConfig.GROUP)
public interface CodexIronworksConfig extends Config
{
	String GROUP = "codexironworks";

	@ConfigSection(
		name = "Stores",
		description = "Which items count as stores, beyond the ironman staples Ironworks knows",
		position = 10
	)
	String STORES = "stores";

	@ConfigSection(
		name = "Guide",
		description = "How the Guide tab points you to the step in hand",
		position = 20
	)
	String GUIDE = "guide";

	@ConfigItem(
		keyName = "target",
		name = "Runway target",
		description = "Hours of play you want each store to last; stores short of it go on the shopping list",
		position = 0
	)
	@Range(min = 1, max = 500)
	@Units(" hours")
	default int target()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "warn",
		name = "Running low",
		description = "A store that lasts fewer hours of play than this is marked as running low",
		position = 1
	)
	@Range(min = 1, max = 100)
	@Units(" hours")
	default int warn()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "chatWarnings",
		name = "Low stores in chat",
		description = "When you open your bank, say in the chat box which stores are running low, once a session each",
		position = 2
	)
	default boolean chatWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shopPlaque",
		name = "Shop plaque",
		description = "At a shop you have used before, show what your stores need from it and when each world fills again",
		position = 3
	)
	default boolean shopPlaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "itemTips",
		name = "Item tooltips",
		description = "Hover an item in your inventory or bank to see how long its store lasts and what it makes",
		position = 4
	)
	default boolean itemTips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "othersLoot",
		name = "Others' loot last",
		description = "On an ironman, put Take below Walk here for ground items that belong to another player",
		position = 5
	)
	default boolean othersLoot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "altar",
		name = "Bones go to",
		description = "Where you offer bones, which sets the Prayer experience the Workshop counts for them",
		position = 6
	)
	default Altar altar()
	{
		return Altar.GILDED;
	}

	@ConfigItem(
		keyName = "wayfinder",
		name = "Travel with Wayfinder",
		description = "With Codex Wayfinder on, cost each way to get more by real travel time with your teleports and show a Route button",
		position = 7
	)
	default boolean wayfinder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pouchWarnings",
		name = "Pouch wear in chat",
		description = "Say in the chat box when an essence pouch you carry could shrink on its next fill, and how to have it repaired",
		position = 8
	)
	default boolean pouchWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "treeCoach",
		name = "Tree coach",
		description = "Over the tree you are chopping, show how many are chopping it and an estimate of the time left on its hidden Forestry timer",
		position = 9
	)
	default boolean treeCoach()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mineCoach",
		name = "Rock respawn estimates",
		description = "While you mine, over each rock near you that has been mined out, show an estimate of when it comes back: the wiki's respawn time, or your own once you have seen enough rocks there come back",
		position = 10
	)
	default boolean mineCoach()
	{
		return true;
	}

	@ConfigItem(
		keyName = "starCoach",
		name = "Star layer estimate",
		description = "While you mine or prospect a shooting star, show its size, the level it needs and an estimate of the time left on its layer",
		position = 13
	)
	default boolean starCoach()
	{
		return true;
	}

	@ConfigItem(
		keyName = "starGoal",
		name = "Stardust goal",
		description = "A Dusuri's Star Shop item to save stardust toward: the stars card shows your stardust against its price and the time at your pace",
		position = 14
	)
	default Stars.Reward starGoal()
	{
		return Stars.Reward.NONE;
	}

	@ConfigItem(
		keyName = "fishCoach",
		name = "Fishing spot move estimate",
		description = "While you fish, over the spot you fish at, show an estimate of when it may move, from the wiki's figures for how long spots stay; the game never says, so it is only ever an estimate",
		position = 15
	)
	default boolean fishCoach()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pearlGoal",
		name = "Molch pearl goal",
		description = "An item from Alry the Angler's shop to save Molch pearls toward: the fishing card shows your pearls against its price and the time at your aerial fishing pace",
		position = 16
	)
	default Fish.PearlGoal pearlGoal()
	{
		return Fish.PearlGoal.NONE;
	}

	@ConfigItem(
		keyName = "picked",
		name = "Also track",
		description = "Item names to keep as stores too, separated by commas, such as Abyssal whip, Games necklace(8)",
		section = STORES,
		position = 11
	)
	default String picked()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hidden",
		name = "Never track",
		description = "Store names to leave out entirely, separated by commas, such as Coins, Feather",
		section = STORES,
		position = 12
	)
	default String hidden()
	{
		return "";
	}

	@ConfigItem(
		keyName = "guideArrow",
		name = "Hint arrow",
		description = "Point the game's hint arrow at the person or place the step in hand needs, when it is near",
		section = GUIDE,
		position = 21
	)
	default boolean guideArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guidePeople",
		name = "Highlight clicks",
		description = "Outline in gold what the step in hand asks you to click: people, objects, items to pick up, buy or use, and the chat option to choose",
		section = GUIDE,
		position = 22
	)
	default boolean guidePeople()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guidePath",
		name = "Shortest Path",
		description = "Ask the Shortest Path plugin, if you have it, to draw the way to the step in hand",
		section = GUIDE,
		position = 23
	)
	default boolean guidePath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guidePlaque",
		name = "Step plaque",
		description = "While the guide points the way, show the step in hand and what it still needs in the game view",
		section = GUIDE,
		position = 24
	)
	default boolean guidePlaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideMap",
		name = "Map pin and minimap arrow",
		description = "Pin the step in hand on the world map and point to it from the minimap, however far it is",
		section = GUIDE,
		position = 25
	)
	default boolean guideMap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guideChat",
		name = "Step done in chat",
		description = "When the game shows the step in hand done, say so in chat and name the next step",
		section = GUIDE,
		position = 26
	)
	default boolean guideChat()
	{
		return true;
	}

	@ConfigSection(
		name = "Field notes",
		description = "A local record for checking the plugin's guesses",
		position = 99,
		closedByDefault = true
	)
	String FIELD_NOTES = "fieldNotesSection";

	@ConfigItem(
		keyName = "fieldNotes",
		name = "Keep field notes",
		description = "Write what Ironworks reads and decides to its folder in .runelite/plugin-data on your computer, for checking its guesses; nothing is sent anywhere",
		section = FIELD_NOTES,
		position = 0
	)
	default boolean fieldNotes()
	{
		return false;
	}
}
