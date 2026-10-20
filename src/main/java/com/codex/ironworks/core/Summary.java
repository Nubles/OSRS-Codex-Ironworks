package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Where a stretch of play's supplies came from and went, weighed by worth so a hundred feathers
 * do not outweigh one shark-sized stack of runes. Money itself is left out: coins are what supplies
 * are bought with, not a supply.
 */
public final class Summary
{
	/** One store's part in a source or use. */
	public static final class Line
	{
		public final String store;
		public final long units;
		public final double worth;

		Line(String store, long units, double worth)
		{
			this.store = store;
			this.units = units;
			this.worth = worth;
		}
	}

	public final Map<Source, Double> sources = new EnumMap<>(Source.class);
	public final Map<Use, Double> uses = new EnumMap<>(Use.class);
	public final Map<Source, List<Line>> bySource = new EnumMap<>(Source.class);
	public final Map<Use, List<Line>> byUse = new EnumMap<>(Use.class);
	/** Worth gathered, made or farmed with each skill. */
	public final Map<String, Double> skills = new HashMap<>();
	public final double gained;
	public final double used;
	/** Share of what came in that the player made or gathered themselves, by worth; -1 when nothing came in. */
	public final double self;
	public final long played;

	private Summary(Window w, ToDoubleFunction<String> unitWorth)
	{
		double in = 0;
		double out = 0;
		double own = 0;
		for (Map.Entry<String, Tally> e : w.stores.entrySet())
		{
			String store = e.getKey();
			if (Catalogue.money(store))
			{
				continue;
			}
			// Untradeable things are worth nothing on the exchange but are still supplies; count them a coin each.
			double each = Math.max(1, unitWorth.applyAsDouble(store));
			Tally t = e.getValue();
			for (Source s : Source.values())
			{
				long units = t.in(s);
				if (units > 0)
				{
					double worth = units * each;
					sources.merge(s, worth, Double::sum);
					bySource.computeIfAbsent(s, k -> new ArrayList<>()).add(new Line(store, units, worth));
					in += worth;
					own += s.isSelf() ? worth : 0;
				}
			}
			for (Use u : Use.values())
			{
				long units = t.out(u);
				if (units > 0)
				{
					double worth = units * each;
					uses.merge(u, worth, Double::sum);
					byUse.computeIfAbsent(u, k -> new ArrayList<>()).add(new Line(store, units, worth));
					out += worth;
				}
			}
			long selfUnits = t.self();
			if (selfUnits > 0)
			{
				t.skills.forEach((skill, units) -> skills.merge(skill, units * each, Double::sum));
			}
		}
		bySource.values().forEach(l -> l.sort((a, b) -> Double.compare(b.worth, a.worth)));
		byUse.values().forEach(l -> l.sort((a, b) -> Double.compare(b.worth, a.worth)));
		gained = in;
		used = out;
		self = in > 0 ? own / in : -1;
		played = w.played;
	}

	/**
	 * @param unitWorth what one unit of a store is worth, such as one dose of a potion
	 */
	public static Summary of(Window window, ToDoubleFunction<String> unitWorth)
	{
		return new Summary(window, unitWorth);
	}
}
