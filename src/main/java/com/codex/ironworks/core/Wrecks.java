package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * The shipwrecks Sailing salvages, as the OSRS Wiki gives them (Shipwreck salvaging, and each
 * salvage's own page): the Sailing level each needs, the salvage it gives, the experience a salvage
 * gives on the hook and again when it is sorted at a salvaging station, and the wiki's average time
 * a wreck lasts once someone starts salvaging it. A wreck's yield depends on the hook, the crew and
 * the level, so nothing here predicts a pace: the pace is the player's own, from their own trips.
 */
public final class Wrecks
{
	/** One kind of shipwreck and its salvage. */
	public static final class Kind
	{
		/** The wreck, as the wiki names it: "Fisherman's shipwreck". */
		public final String wreck;
		/** The salvage, by its store and item name: "Fishy salvage". */
		public final String salvage;
		public final int item;
		public final int level;
		/** Sailing experience for a salvage on the hook, by the wiki's figure. */
		public final double xp;
		/** Sailing experience for sorting one at a salvaging station, by the wiki's figure. */
		public final double sortXp;
		/** The wiki's average time the wreck lasts once salvaging starts, in seconds. */
		public final int lastsSeconds;

		Kind(String wreck, String salvage, int item, int level, double xp, double sortXp, int lastsSeconds)
		{
			this.wreck = wreck;
			this.salvage = salvage;
			this.item = item;
			this.level = level;
			this.xp = xp;
			this.sortXp = sortXp;
			this.lastsSeconds = lastsSeconds;
		}

		/** The wreck's name without "shipwreck": "Fisherman's", "Merchant". */
		public String shortName()
		{
			return wreck.replace(" shipwreck", "");
		}

		@Override
		public String toString()
		{
			return wreck;
		}
	}

	private static final List<Kind> ALL = new ArrayList<>();
	private static final Map<String, Kind> BY_SALVAGE = new HashMap<>();
	private static final Map<Integer, Kind> BY_ITEM = new HashMap<>();

	static
	{
		// OSRS Wiki, Shipwreck salvaging: level, wreck, salvaging xp, salvage, sorting xp and average
		// duration, row by row ("15 | Small | 10 | Small | 5.5 | 1 minute"). The sorting figures are the
		// ones each salvage's page gives after the 5 December 2025 hotfix ("The amount of experience
		// given when sorting was increased back to 95"). Item ids: RuneLite gameval, as each page's infobox.
		add("Small shipwreck", "Small salvage", ItemID.SAILING_SMALL_SHIPWRECK_SALVAGE, 15, 10, 5.5, 60);
		add("Fisherman's shipwreck", "Fishy salvage", ItemID.SAILING_FISHERMAN_SHIPWRECK_SALVAGE, 26, 17, 9, 180);
		add("Barracuda shipwreck", "Barracuda salvage", ItemID.SAILING_BARRACUDA_SHIPWRECK_SALVAGE, 35, 31, 15.5, 180);
		add("Large shipwreck", "Large salvage", ItemID.SAILING_LARGE_SHIPWRECK_SALVAGE, 53, 48, 24, 180);
		add("Pirate shipwreck", "Plundered salvage", ItemID.SAILING_PIRATE_SHIPWRECK_SALVAGE, 64, 76, 31.5, 180);
		add("Mercenary shipwreck", "Martial salvage", ItemID.SAILING_MERCENARY_SHIPWRECK_SALVAGE, 73, 138, 63.5, 195);
		add("Fremennik shipwreck", "Fremennik salvage", ItemID.SAILING_FREMENNIK_SHIPWRECK_SALVAGE, 80, 162, 75, 220);
		add("Merchant shipwreck", "Opulent salvage", ItemID.SAILING_MERCHANT_SHIPWRECK_SALVAGE, 87, 200, 95, 240);
	}

	/**
	 * The game's line when a wreck's time is up (OSRS Wiki, Shipwreck salvaging): "You salvage all you
	 * can from the shipwreck before it is reclaimed by the sea."
	 */
	public static final String SUNK = "You salvage all you can from the shipwreck before it is reclaimed by the sea.";

	/**
	 * Slots a cargo hold can have, by tier and boat (OSRS Wiki, Cargo hold, "Capacity limits"): a
	 * capacity read off the hold's window is only taken when it is one of these.
	 */
	private static final int[] HOLD_SIZES = {20, 30, 40, 45, 60, 80, 90, 105, 120, 150, 160, 180, 210, 240};

	private Wrecks()
	{
	}

	private static void add(String wreck, String salvage, int item, int level, double xp, double sortXp, int lasts)
	{
		Kind k = new Kind(wreck, salvage, item, level, xp, sortXp, lasts);
		ALL.add(k);
		BY_SALVAGE.put(salvage, k);
		BY_ITEM.put(item, k);
	}

	/** Every shipwreck, lowest level first. */
	public static List<Kind> all()
	{
		return Collections.unmodifiableList(ALL);
	}

	/** The wreck of a salvage store: "Large salvage"; null for anything else. */
	public static Kind bySalvage(String store)
	{
		return store == null ? null : BY_SALVAGE.get(store);
	}

	/** The wreck of a salvage item id; null for anything else. */
	public static Kind byItem(int item)
	{
		return BY_ITEM.get(item);
	}

	/** Whether a number of slots is one a cargo hold can have, by the wiki's table. */
	public static boolean holdSize(int slots)
	{
		for (int s : HOLD_SIZES)
		{
			if (s == slots)
			{
				return true;
			}
		}
		return false;
	}
}
