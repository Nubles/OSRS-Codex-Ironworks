package com.codex.ironworks.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * One thing a player can make from their stores: what goes in, in store units, what comes out,
 * and the experience it gives. A recipe that makes nothing, such as offering bones, uses its
 * inputs up for the experience alone.
 */
public final class Recipe
{
	public final Skill skill;
	public final int level;
	/** Experience for making one. */
	public final double xp;
	/** The game name of what is made: "Prayer potion(3)". */
	public final String product;
	public final int productItem;
	/** How many of the product one making gives; 0 when the inputs are used up for experience alone. */
	public final int count;
	/**
	 * When it is made: unfinished potions and bars before what is made from them, and recipes that
	 * only use things up last, so logs are fletched before any are burned.
	 */
	public final int tier;
	/** Store to units per making, the first being the one the recipe is made for. */
	public final Map<String, Integer> inputs;

	Recipe(Skill skill, int level, double xp, String product, int productItem, int count, int tier, Map<String, Integer> inputs)
	{
		this.skill = skill;
		this.level = level;
		this.xp = xp;
		this.product = product;
		this.productItem = productItem;
		this.count = count;
		this.tier = tier;
		this.inputs = Collections.unmodifiableMap(new LinkedHashMap<>(inputs));
	}

	/** The store the recipe is made for: the herb, the bones, the logs. */
	public String primary()
	{
		return inputs.keySet().iterator().next();
	}

	/** Whether it uses its inputs up for experience alone. */
	public boolean usesUp()
	{
		return count == 0;
	}

	/** Bones offered at an altar, as opposed to ashes, which are only ever scattered. */
	public boolean bones()
	{
		return skill == Skill.PRAYER && !primary().toLowerCase(java.util.Locale.ENGLISH).endsWith("ashes");
	}

	@Override
	public String toString()
	{
		return product;
	}
}
