package com.codex.ironworks.ui;

import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Summary;
import com.codex.ironworks.core.Tally;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
	}
}
