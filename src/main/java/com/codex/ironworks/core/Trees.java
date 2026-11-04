package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * The trees Ironworks knows, as the OSRS Wiki gives them: the Woodcutting level, the experience for
 * each log, and, for the trees Forestry put on a timer, how many ticks of chopping fell one and how
 * long it takes to grow back. The game never shows the timer itself, so the coach can only count
 * ticks of chopping it sees against these figures.
 */
public final class Trees
{
	/** One kind of tree. */
	public static final class Kind
	{
		/** The tree's name, as the game names the object: "Yew tree". */
		public final String name;
		/** The logs it gives, by store name. */
		public final String logs;
		public final int logItem;
		public final int level;
		public final double xp;
		/** Ticks of chopping that fell it on the Forestry timer; 0 when it is not on one. */
		public final int fallTicks;
		/** Ticks it takes to grow back once felled; 0 when not given. */
		public final int respawnTicks;
		/**
		 * Whether RuneLite's gameval names an object so; false for the Sailing trees, whose object
		 * name is taken from their wiki page and is unconfirmed.
		 */
		public final boolean named;

		Kind(String name, String logs, int logItem, int level, double xp, int fallTicks, int respawnTicks, boolean named)
		{
			this.name = name;
			this.logs = logs;
			this.logItem = logItem;
			this.level = level;
			this.xp = xp;
			this.fallTicks = fallTicks;
			this.respawnTicks = respawnTicks;
			this.named = named;
		}

		/** Whether Forestry's timer fells it, rather than its first log. */
		public boolean timed()
		{
			return fallTicks > 0;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/** A game tick, in milliseconds. */
	public static final long TICK_MS = 600L;

	private static final List<Kind> ALL = new ArrayList<>();
	private static final Map<String, Kind> BY_NAME = new HashMap<>();
	private static final Map<String, Kind> BY_LOGS = new HashMap<>();

	/**
	 * Trees grown in a Farming patch, which "are not affected by Forestry timers" (OSRS Wiki:
	 * Forestry): the fully grown patch trees, by RuneLite gameval ObjectID.
	 */
	private static final Set<Integer> FARMED = new HashSet<>();

	static
	{
		// Level and experience: OSRS Wiki, Woodcutting. Fall and respawn ticks: each tree's own wiki
		// page ("will fall after 45 ticks (27 seconds) of chopping"), and Forestry's table, which gives
		// the same times in seconds. Regular trees "are always felled after one log has been cut" (Tree).
		add("Tree", "Logs", ItemID.LOGS, 1, 25, 0, 0, true);
		add("Oak tree", "Oak logs", ItemID.OAK_LOGS, 15, 37.5, 45, 14, true);
		add("Willow tree", "Willow logs", ItemID.WILLOW_LOGS, 30, 67.5, 50, 14, true);
		add("Teak tree", "Teak logs", ItemID.TEAK_LOGS, 35, 85, 50, 15, true);
		add("Maple tree", "Maple logs", ItemID.MAPLE_LOGS, 45, 100, 100, 59, true);
		add("Mahogany tree", "Mahogany logs", ItemID.MAHOGANY_LOGS, 50, 125, 100, 14, true);
		add("Arctic pine tree", "Arctic pine logs", ItemID.ARCTIC_PINE_LOG, 54, 40, 140, 14, true);
		add("Yew tree", "Yew logs", ItemID.YEW_LOGS, 60, 175, 190, 99, true);
		add("Camphor tree", "Camphor logs", ItemID.CAMPHOR_LOGS, 66, 143.5, 200, 99, false);
		add("Magic tree", "Magic logs", ItemID.MAGIC_LOGS, 75, 250, 390, 199, true);
		add("Ironwood tree", "Ironwood logs", ItemID.IRONWOOD_LOGS, 80, 175, 400, 199, false);
		add("Redwood tree", "Redwood logs", ItemID.REDWOOD_LOGS, 90, 380, 440, 199, true);
		add("Rosewood tree", "Rosewood logs", ItemID.ROSEWOOD_LOGS, 92, 212.5, 458, 205, false);

		int[] farmed = {
			ObjectID.OAK_TREE_FULLYGROWN_1, ObjectID.OAK_TREE_FULLYGROWN_2,
			ObjectID.WILLOW_TREE_FULLYGROWN_1, ObjectID.WILLOW_TREE_FULLYGROWN_2,
			ObjectID.MAPLE_TREE_FULLYGROWN_1, ObjectID.MAPLE_TREE_FULLYGROWN_2,
			ObjectID.YEW_TREE_FULLYGROWN_1, ObjectID.YEW_TREE_FULLYGROWN_2,
			ObjectID.MAGIC_TREE_FULLYGROWN_1, ObjectID.MAGIC_TREE_FULLYGROWN_2,
			ObjectID.TEAK_TREE_FULLYGROWN, ObjectID.TEAK_TREE_FULLYGROWN_CLAIMXP,
			ObjectID.MAHOGANY_TREE_FULLYGROWN, ObjectID.MAHOGANY_TREE_FULLYGROWN_CLAIMXP,
			ObjectID.FARMING_REDWOOD_TREE_PATCH_1_2, ObjectID.FARMING_REDWOOD_TREE_PATCH_1_4,
			ObjectID.FARMING_REDWOOD_TREE_PATCH_1_6, ObjectID.FARMING_REDWOOD_TREE_PATCH_1_8,
			ObjectID.REDWOOD_TREE_FULLYGROWN_1_2, ObjectID.REDWOOD_TREE_FULLYGROWN_1_4,
			ObjectID.REDWOOD_TREE_FULLYGROWN_1_6, ObjectID.REDWOOD_TREE_FULLYGROWN_1_8,
		};
		for (int id : farmed)
		{
			FARMED.add(id);
		}
	}

	private Trees()
	{
	}

	private static void add(String name, String logs, int logItem, int level, double xp, int fall, int respawn, boolean named)
	{
		Kind k = new Kind(name, logs, logItem, level, xp, fall, respawn, named);
		ALL.add(k);
		BY_NAME.put(name.toLowerCase(Locale.ENGLISH), k);
		BY_LOGS.put(logs.toLowerCase(Locale.ENGLISH), k);
	}

	/** Every tree, lowest level first. */
	public static List<Kind> all()
	{
		return Collections.unmodifiableList(ALL);
	}

	/** The tree a game object's name is, or null. */
	public static Kind byName(String objectName)
	{
		return objectName == null ? null : BY_NAME.get(objectName.toLowerCase(Locale.ENGLISH));
	}

	/** The tree a store of logs comes from, or null when the store is not logs. */
	public static Kind byLogs(String store)
	{
		return store == null ? null : BY_LOGS.get(store.toLowerCase(Locale.ENGLISH));
	}

	/** The tree by its name in the log, or null. */
	public static Kind of(String name)
	{
		if (name == null)
		{
			return null;
		}
		for (Kind k : ALL)
		{
			if (k.name.equalsIgnoreCase(name))
			{
				return k;
			}
		}
		return null;
	}

	/** Whether the object is a tree grown in a Farming patch, which keeps no Forestry timer. */
	public static boolean farmed(int objectId)
	{
		return FARMED.contains(objectId);
	}
}
