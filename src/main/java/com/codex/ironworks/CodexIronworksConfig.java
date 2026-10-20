package com.codex.ironworks;

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
}
