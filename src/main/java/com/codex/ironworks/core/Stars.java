package com.codex.ironworks.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * Shooting stars as the OSRS Wiki gives them: the crashed star's size, which is its layer, the
 * Mining level each layer needs, the experience for each stardust, how long a layer lasts, the
 * game's own lines about a star, and what Dusuri's Star Shop sells for stardust.
 *
 * <p>Sources: OSRS Wiki, Shooting Stars ("Each layer has an associated Mining level requirement,
 * decreasing with each layer that is mined through"; the size table, size 9 at level 90 down to
 * size 1 at level 10; "Each layer will always deplete at a fixed rate of 7 minutes, provided there
 * is at least one player mining it"; "receive 32 (32.8 with the full prospectors kit) experience
 * each time a piece of stardust is mined ... (halved to 16 on Free-to-play worlds)"). Object ids:
 * OSRS Wiki, Crashed star, matching RuneLite's gameval {@code STAR_SIZE_ONE_STAR} to
 * {@code STAR_SIZE_NINE_STAR}. Lines: OSRS Wiki, Transcript:Crashed Star. Shop: OSRS Wiki,
 * Dusuri's Star Shop.
 */
public final class Stars
{
	/** The largest size a star lands at. */
	public static final int MAX_SIZE = 9;
	/** Mining experience for each stardust on a members' world, before the prospector kit. */
	public static final double XP_PER_DUST = 32;
	/** A layer lasts 7 minutes while anyone mines it: 700 ticks of 0.6 seconds. */
	public static final int LAYER_TICKS = 700;
	/** The stardust item. */
	public static final int STARDUST = ItemID.STAR_DUST;

	/** The crashed star objects, size 1 first. */
	private static final int[] OBJECTS = {
		ObjectID.STAR_SIZE_ONE_STAR, ObjectID.STAR_SIZE_TWO_STAR, ObjectID.STAR_SIZE_THREE_STAR,
		ObjectID.STAR_SIZE_FOUR_STAR, ObjectID.STAR_SIZE_FIVE_STAR, ObjectID.STAR_SIZE_SIX_STAR,
		ObjectID.STAR_SIZE_SEVEN_STAR, ObjectID.STAR_SIZE_EIGHT_STAR, ObjectID.STAR_SIZE_NINE_STAR,
	};

	/** "This is a size-9 star. A Mining level of at least 90 is required to mine this layer." */
	private static final Pattern SIZE = Pattern.compile("This is a size-(\\d) star\\.");
	/** "It has been mined [#]% of the way to the next layer." */
	private static final Pattern MINED = Pattern.compile("It has been mined (\\d{1,3})% of the way to the next layer\\.");
	/** "Congratulations! You were the first person to find this star!" */
	public static final String FIRST = "Congratulations! You were the first person to find this star!";
	/** "The star disintegrates into dust." */
	public static final String GONE = "The star disintegrates into dust.";

	/** What Dusuri's Star Shop sells for stardust, as the wiki prices it. */
	public enum Reward
	{
		NONE("None", 0, -1),
		SOFT_CLAY_PACK("Soft clay pack", 150, ItemID.STAR_PACK_SOFTCLAY),
		BAG_FULL_OF_GEMS("Bag full of gems", 300, ItemID.STAR_REWARD_GEM_BAG),
		CELESTIAL_RING("Celestial ring (uncharged)", 2000, ItemID.CELESTIAL_RING),
		STAR_FRAGMENT("Star fragment", 3000, ItemID.STAR_FRAGMENT);

		public final String item;
		/** Its price in stardust. */
		public final int price;
		public final int itemId;

		Reward(String item, int price, int itemId)
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

	private Stars()
	{
	}

	/** The size of a crashed star object, or 0 when the object is not one. */
	public static int size(int objectId)
	{
		for (int i = 0; i < OBJECTS.length; i++)
		{
			if (OBJECTS[i] == objectId)
			{
				return i + 1;
			}
		}
		return 0;
	}

	/** The crashed star object of a size. */
	public static int object(int size)
	{
		return size < 1 || size > MAX_SIZE ? -1 : OBJECTS[size - 1];
	}

	/** The Mining level a layer of this size needs. */
	public static int level(int size)
	{
		return size * 10;
	}

	/** The largest layer a Mining level can mine, or 0 below the smallest. */
	public static int largestFor(int level)
	{
		return Math.max(0, Math.min(MAX_SIZE, level / 10));
	}

	/** What prospecting a star said. */
	public static final class Prospect
	{
		/** Its size, or 0 when the line did not say. */
		public final int size;
		/** How far its layer has been mined, 0 to 100, or -1 when the line did not say. */
		public final int mined;

		Prospect(int size, int mined)
		{
			this.size = size;
			this.mined = mined;
		}
	}

	/**
	 * Reads what prospecting a star says, tags already taken off. The size and the share of the
	 * layer mined may come in one line or two; either part is read where it is. Null for any other
	 * line.
	 */
	public static Prospect prospect(String message)
	{
		if (message == null)
		{
			return null;
		}
		Matcher s = SIZE.matcher(message);
		Matcher m = MINED.matcher(message);
		int size = s.find() ? Integer.parseInt(s.group(1)) : 0;
		int mined = m.find() ? Integer.parseInt(m.group(1)) : -1;
		if (size < 1 || size > MAX_SIZE)
		{
			size = 0;
		}
		if (mined > 100)
		{
			mined = -1;
		}
		return size == 0 && mined < 0 ? null : new Prospect(size, mined);
	}

	/** "Size 7". */
	public static String name(int size)
	{
		return String.format(Locale.ENGLISH, "Size %d", size);
	}
}
