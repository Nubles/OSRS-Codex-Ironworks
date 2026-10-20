package com.codex.ironworks.core;

/** The shelves an ironman's stores sit on, in the order the panel shows them. */
public enum Family
{
	FOOD("Food", "items"),
	POTIONS("Potions", "doses"),
	UNFINISHED("Unfinished potions", "items"),
	HERBS("Herbs", "herbs"),
	SECONDARIES("Secondaries", "items"),
	SEEDS("Seeds", "seeds"),
	RUNES("Runes", "runes"),
	AMMO("Ammunition", "items"),
	PRAYER("Bones and ashes", "items"),
	MATERIALS("Materials", "items"),
	CURRENCY("Coins and tokens", "coins"),
	PICKED("Your picks", "items");

	private final String label;
	private final String unit;

	Family(String label, String unit)
	{
		this.label = label;
		this.unit = unit;
	}

	public String getLabel()
	{
		return label;
	}

	/** What one unit of a store on this shelf is: potions are counted in doses, not bottles. */
	public String getUnit()
	{
		return unit;
	}
}
