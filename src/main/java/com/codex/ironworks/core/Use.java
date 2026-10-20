package com.codex.ironworks.core;

/** Where something left the stores to. */
public enum Use
{
	CONSUMED("Eaten and drunk", true),
	CAST("Cast", true),
	FIRED("Fired", true),
	MADE_INTO("Made into", true),
	PLANTED("Planted", true),
	OFFERED("Offered", true),
	SPENT("Spent at shops", true),
	USED("Used", true),
	SOLD("Sold", false),
	TRADED("Traded away", false),
	DROPPED("Dropped", false),
	DIED("Lost on death", false);

	private final String label;
	private final boolean burn;

	Use(String label, boolean burn)
	{
		this.label = label;
		this.burn = burn;
	}

	public String getLabel()
	{
		return label;
	}

	/**
	 * Whether this is the stores being worked through, which sets how long they last. Selling,
	 * trading, dropping and dying are one-off choices or accidents, not the pace of play.
	 */
	public boolean isBurn()
	{
		return burn;
	}

	/** Reads a saved name; null for one this version does not know. */
	public static Use of(String name)
	{
		for (Use u : values())
		{
			if (u.name().equals(name))
			{
				return u;
			}
		}
		return null;
	}
}
