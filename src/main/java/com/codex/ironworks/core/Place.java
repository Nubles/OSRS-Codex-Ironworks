package com.codex.ironworks.core;

/** Where stores are kept. Each is counted as it was last seen. */
public enum Place
{
	BANK("Bank"),
	CARRIED("Carried"),
	POTION_STORAGE("Potion storage"),
	SEED_VAULT("Seed vault"),
	GROUP_STORAGE("Group storage"),
	LOOTING_BAG("Looting bag");

	private final String label;

	Place(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
