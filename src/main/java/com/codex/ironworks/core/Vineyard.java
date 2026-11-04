package com.codex.ironworks.core;

import net.runelite.api.Experience;
import net.runelite.api.gameval.ItemID;

/**
 * The Aldarin vineyard, as the OSRS Wiki gives it (Vineyard foreman, Grape barrel, Grapevine,
 * Grapes, Jug of wine): the foreman at the Moonrise Brewery and Winery hands out a grape barrel,
 * the player fills it from four ripe grapevines across the vineyards, and a full barrel handed back
 * pays 5 noted grapes. It gives no experience and no coins ("Can I have some gold instead?" "No."):
 * its worth is the grapes, which an ironman ferments into jugs of wine for Cooking experience.
 *
 * <p>Ironworks only reads the pack: grapes that come in while the grape barrel is carried are a
 * barrel handed in. It marks no vine and gives no cue to act on.
 */
public final class Vineyard
{
	/** The grape barrel the foreman hands out (gameval ALDARIN_GRAPE_COLLECTION_BARREL). */
	public static final int BARREL = ItemID.ALDARIN_GRAPE_COLLECTION_BARREL;
	/** Grapes; noted grapes are folded into it before they reach here. */
	public static final int GRAPES = ItemID.GRAPES;
	public static final int WINE = ItemID.JUG_WINE;
	/** The store grapes are kept under. */
	public static final String STORE = "Grapes";
	/** Noted grapes a full barrel pays (OSRS Wiki: Vineyard foreman, "5 (noted)", "Always"). */
	public static final int GRAPES_PER_BARREL = 5;
	/** Ripe grapevines a barrel takes (OSRS Wiki: "fill this barrel with four sets of ripe grapes"). */
	public static final int VINES_PER_BARREL = 4;
	/** The quest the foreman asks for (OSRS Wiki: Vineyard foreman). */
	public static final String QUEST = "Children of the Sun";
	/** Cooking to make a jug of wine, and the experience a jug that ferments gives (OSRS Wiki: Jug of wine). */
	public static final int WINE_LEVEL = 35;
	public static final double WINE_XP = 200;
	/** Cooking at which wine stops failing to ferment (OSRS Wiki: Jug of bad wine). */
	public static final int NO_FAIL_LEVEL = 68;

	/** A Cooking level to fill the vineyard's grapes toward, as jugs of wine. */
	public enum Goal
	{
		NONE("None", 0),
		NEXT_LEVEL("Next Cooking level", -1),
		NO_FAIL("68 Cooking (wine stops failing)", NO_FAIL_LEVEL),
		MAX("99 Cooking", 99);

		private final String label;
		/** The Cooking level aimed at; -1 for the one after the player's own. */
		public final int level;

		Goal(String label, int level)
		{
			this.label = label;
			this.level = level;
		}

		/** The level this goal aims at for a player at {@code cooking}; 0 when there is none left. */
		public int target(int cooking)
		{
			int t = level < 0 ? cooking + 1 : level;
			return this == NONE || t <= cooking || t > 99 ? 0 : t;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private Vineyard()
	{
	}

	/**
	 * Jugs of wine, and so grapes, that take Cooking experience from {@code xp} to the level: every jug
	 * at the wiki's 200. Below 68 Cooking some jugs fail and give none, so it is the least it takes.
	 */
	public static long grapesTo(long xp, int level)
	{
		if (level < 2 || level > 99)
		{
			return 0;
		}
		long left = Experience.getXpForLevel(level) - xp;
		return left <= 0 ? 0 : (long) Math.ceil(left / WINE_XP);
	}
}
