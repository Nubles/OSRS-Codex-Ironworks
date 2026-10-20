package com.codex.ironworks.core;

/** Units of one store coming in from a source or going out to a use. */
public final class Flow
{
	public final String store;
	public final long units;
	public final Source source;
	public final Use use;
	/** The skill the units were gathered, made or farmed with, or used up in; null when none. */
	public final String skill;

	private Flow(String store, long units, Source source, Use use, String skill)
	{
		this.store = store;
		this.units = units;
		this.source = source;
		this.use = use;
		this.skill = skill;
	}

	public static Flow in(String store, long units, Source source, String skill)
	{
		return new Flow(store, units, source, null, skill);
	}

	public static Flow out(String store, long units, Use use, String skill)
	{
		return new Flow(store, units, null, use, skill);
	}

	public boolean isGain()
	{
		return source != null;
	}

	@Override
	public String toString()
	{
		return (isGain() ? "+" : "-") + units + " " + store + " " + (isGain() ? source : use) + (skill == null ? "" : " (" + skill + ")");
	}
}
