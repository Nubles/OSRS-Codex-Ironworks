package com.codex.ironworks.ui;

import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Summary;
import com.codex.ironworks.core.Spot;
import com.codex.ironworks.core.Tally;
import com.codex.ironworks.core.Ways;
import com.codex.ironworks.core.Workshop;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Everything the panel shows, worked out on the client thread and handed over whole, so the panel
 * never reads the plugin's live state while the game changes it.
 */
public final class View
{
	public boolean loggedIn;
	/** The ironman varbit: 0 none, 1 ironman, 2 ultimate, 3 hardcore, 4 group, 5 hardcore group, 6 unranked group; -1 unknown. */
	public int account = -1;
	public List<StoreView> stores = Collections.emptyList();
	public Summary today;
	public Summary week;
	public Summary all;
	public List<ShopView> shops = Collections.emptyList();
	public List<RunView> runs = Collections.emptyList();
	public List<Want> wants = Collections.emptyList();
	public double target = 10;
	public double warn = 2;
	public double hoursPerDay;
	/** Play over the last week, which the pace is measured from. */
	public long playedWeek;
	public boolean bankSeen;
	public long now;
	/** The picture of each store, by store. */
	public Map<String, Integer> icons = Collections.emptyMap();
	/** Each store's view, by store, for the tooltips. */
	public Map<String, StoreView> byStore = Collections.emptyMap();

	/** What the stores could become, at the player's levels. */
	public Workshop workshop = Workshop.empty();
	public Map<Skill, Integer> levels = Collections.emptyMap();
	public Map<Skill, Integer> xp = Collections.emptyMap();
	public Altar altar = Altar.GILDED;

	/** Every item the account owns, wherever it is kept, by name. */
	public List<Found> found = Collections.emptyList();
	public List<SpotView> spots = Collections.emptyList();
	/** The last death not yet taken back, or null. */
	public DeathView death;

	/** The ways to get more of each store, by store. */
	public Map<String, Getting> getting = Collections.emptyMap();
	/** Coins the shopping list costs, as far as prices are known. */
	public long listCoins;
	/** Coins held, everywhere. */
	public long coins;

	/** One thing owned, with where it is. */
	public static final class Found
	{
		public String name;
		public int icon = -1;
		public long total;
		/** Counted in doses, for potions, across every dose size. */
		public boolean doses;
		public Map<Spot, Long> where = Collections.emptyMap();
		/** The store it belongs to, or null. */
		public String store;
	}

	/** One place things are kept. */
	public static final class SpotView
	{
		public Spot spot;
		public long at;
		public int kinds;
		public long worth;
		/** The most valuable items in it, by item id, with quantities. */
		public List<long[]> top = Collections.emptyList();
	}

	/** The last death's items, still to take back. */
	public static final class DeathView
	{
		public long at;
		public int world;
		/** Play time left at the grave, or 0 when they wait at Death's Office. */
		public long graveLeft;
		public long worth;
		/** Item id and quantity, most valuable first. */
		public List<long[]> items = Collections.emptyList();
		public List<String> names = Collections.emptyList();
	}

	/** How to get more of one store. */
	public static final class Getting
	{
		public String store;
		public int icon = -1;
		public List<WayView> ways = Collections.emptyList();
	}

	/** One way to get more. */
	public static final class WayView
	{
		public Ways.Kind kind;
		public String text;
		/** The player's own pace this way, units an hour, or 0 when not yet known. */
		public double perHour;
		public Skill skill;
		public int level;
		/** Whether the player has the level for it. */
		public boolean open = true;
	}

	/** One store as the panel shows it. */
	public static final class StoreView
	{
		public String store;
		public Family family;
		public int icon = -1;
		public long stock;
		public Runway runway;
		public Tally week;
		public Tally all;
		public Map<Place, Long> where = Collections.emptyMap();
		/** Shops the player has bought it at, by name. */
		public List<String> shops = Collections.emptyList();
	}

	/** One shop and what it sells. */
	public static final class ShopView
	{
		public String key;
		public String name;
		public int visits;
		public long first;
		public long last;
		public long bought;
		public Map<String, Long> spent = Collections.emptyMap();
		public List<WareView> wares = Collections.emptyList();
	}

	/** One item a shop sells. */
	public static final class WareView
	{
		public int item;
		public String name;
		/** The store it fills, directly or as a pack, or null. */
		public String store;
		/** Units of the store in one item: a hundred feathers in a pack. */
		public int units = 1;
		public int base;
		public long bought;
		public long coins;
		/** Milliseconds for one to come back, or 0 while unknown. */
		public double restockMs;
		public long refillMs;
		/** Coins for one at the usual stock, and more for each one below it; 0 while unknown. */
		public double price;
		public double climb;
		/** Worlds seen short of full, soonest full first. */
		public List<WorldView> worlds = Collections.emptyList();
	}

	/** A world a shop was last seen short on. */
	public static final class WorldView
	{
		public int world;
		public int stock;
		public long fullAt;
	}

	/** One shop run. */
	public static final class RunView
	{
		public long start;
		public long end;
		public List<String> shops = Collections.emptyList();
		public Map<String, Long> bought = Collections.emptyMap();
		public Map<String, Long> spent = Collections.emptyMap();
		public int worlds;
		public int visits;
	}

	/** A store short of the runway wanted, and where it was bought before. */
	public static final class Want
	{
		public String store;
		public int icon = -1;
		public long need;
		public double hours;
		public Runway.Status status;
		/** The shop to buy it at, or null when it was never bought. */
		public String shop;
		public String ware;
		public int wareItem = -1;
		public int units = 1;
		public int base;
		public long refillMs;
		/** The skill it is mostly made or gathered with, when it is not bought. */
		public String skill;
		/** Coins for what is needed, hopping as the price climbs, or 0 when the price is not yet known. */
		public long coins;
		/** How many to buy a world before the price has climbed by a tenth, or 0 when unknown. */
		public long perWorld;
		/** The player's own pace of getting it, units an hour, and how, when there is one. */
		public double perHour;
		public String how;
	}
}
