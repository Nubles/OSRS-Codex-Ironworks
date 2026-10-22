package com.codex.ironworks.core;

/** Where bones are offered, which sets the experience each one gives. Ashes are always scattered. */
public enum Altar
{
	BURY("Buried", 1),
	GILDED("Gilded altar", 3.5),
	ECTOFUNTUS("Ectofuntus", 4),
	/** Three and a half times, and half the bones are saved, so each bone goes twice as far. */
	CHAOS("Chaos altar", 7);

	private final String label;
	private final double multiplier;

	Altar(String label, double multiplier)
	{
		this.label = label;
		this.multiplier = multiplier;
	}

	public double getMultiplier()
	{
		return multiplier;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
