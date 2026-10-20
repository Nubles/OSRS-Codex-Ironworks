package com.codex.ironworks.core;

/** Where something in the stores came from. */
public enum Source
{
	GATHERED("Gathered", true),
	MADE("Made", true),
	FARMED("Farmed", true),
	LOOTED("Looted", false),
	REWARD("Rewards", false),
	BOUGHT("Bought", false),
	TRADED("Traded in", false),
	SOLD("Sold to shops", false),
	ALCHED("Alched", false),
	OTHER("Other", false);

	private final String label;
	private final boolean self;

	Source(String label, boolean self)
	{
		this.label = label;
		this.self = self;
	}

	public String getLabel()
	{
		return label;
	}

	/** Whether the player made or gathered it with their own hands. */
	public boolean isSelf()
	{
		return self;
	}

	/** Reads a saved name; null for one this version does not know. */
	public static Source of(String name)
	{
		for (Source s : values())
		{
			if (s.name().equals(name))
			{
				return s;
			}
		}
		return null;
	}
}
