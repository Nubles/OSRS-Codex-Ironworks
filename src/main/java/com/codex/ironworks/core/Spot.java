package com.codex.ironworks.core;

/** Every place items are kept, item by item, for finding anything an account owns. */
public enum Spot
{
	BANK("Bank"),
	INVENTORY("Inventory"),
	WORN("Worn"),
	RUNE_POUCH("Rune pouch"),
	LOOTING_BAG("Looting bag"),
	SEED_VAULT("Seed vault"),
	POTION_STORAGE("Potion storage"),
	GROUP_STORAGE("Group storage"),
	DEATH("Death");

	private final String label;

	Spot(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
