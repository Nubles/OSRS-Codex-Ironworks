package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * What the stores could become: every recipe made as many times as the stores allow, the best use
 * of each store first, and the experience it all adds up to, by skill. Intermediate things, such as
 * unfinished potions and bars, are made first and counted into the stores the next recipes draw on.
 * Recipes above the player's level are worked out after the rest, from what is left, and kept apart.
 */
public final class Workshop
{
	/** One recipe as the stores allow it. */
	public static final class Make
	{
		public final Recipe recipe;
		public final long times;
		public final double xp;
		/** Above the player's level for now. */
		public final boolean locked;

		Make(Recipe recipe, long times, double xp, boolean locked)
		{
			this.recipe = recipe;
			this.times = times;
			this.xp = xp;
			this.locked = locked;
		}
	}

	/** A second input that runs out before the main one: buy or gather this many more to use it all. */
	public static final class Short
	{
		public final Recipe recipe;
		public final String input;
		/** Units of the input still wanted. */
		public final long need;
		/** Makings the main input has left in it. */
		public final long more;
		/** Experience those makings would give, counting what the result is made into next. */
		public final double xp;

		Short(Recipe recipe, String input, long need, long more, double xp)
		{
			this.recipe = recipe;
			this.input = input;
			this.need = need;
			this.more = more;
			this.xp = xp;
		}
	}

	/** One skill's share. */
	public static final class Plan
	{
		public final Skill skill;
		public double xp;
		public double lockedXp;
		public final List<Make> makes = new ArrayList<>();
		public final List<Short> shorts = new ArrayList<>();

		Plan(Skill skill)
		{
			this.skill = skill;
		}
	}

	private final Map<Skill, Plan> plans;
	private final double total;
	private final double lockedTotal;

	private Workshop(Map<Skill, Plan> plans)
	{
		this.plans = plans;
		double t = 0;
		double l = 0;
		for (Plan p : plans.values())
		{
			t += p.xp;
			l += p.lockedXp;
		}
		total = t;
		lockedTotal = l;
	}

	/** Every skill with something to make, the most experience first. */
	public List<Plan> plans()
	{
		List<Plan> out = new ArrayList<>(plans.values());
		out.sort((a, b) -> Double.compare(b.xp + b.lockedXp, a.xp + a.lockedXp));
		return out;
	}

	public Plan plan(Skill skill)
	{
		return plans.get(skill);
	}

	/** Experience the stores hold at the player's levels. */
	public double total()
	{
		return total;
	}

	/** Experience in recipes above the player's level, from what the rest leave. */
	public double lockedTotal()
	{
		return lockedTotal;
	}

	/**
	 * @param stocks units of each store held, everywhere
	 * @param levels the player's levels; a skill missing from it counts as high enough for anything
	 */
	public static Workshop plan(Map<String, Long> stocks, Catalogue.Lookup lookup, Map<Skill, Integer> levels, Altar altar)
	{
		List<Recipe> order = new ArrayList<>(Recipes.all());
		order.sort((a, b) -> a.tier != b.tier ? Integer.compare(a.tier, b.tier)
			: Double.compare(perPrimary(b, altar), perPrimary(a, altar)));
		Map<String, Long> pool = new HashMap<>();
		stocks.forEach((k, v) ->
		{
			if (v > 0)
			{
				pool.put(k, v);
			}
		});
		Map<Skill, Plan> plans = new EnumMap<>(Skill.class);
		List<Recipe> locked = new ArrayList<>();
		for (Recipe r : order)
		{
			if (levels.getOrDefault(r.skill, 99) < r.level)
			{
				locked.add(r);
				continue;
			}
			make(r, pool, lookup, altar, plans, false);
		}
		for (Recipe r : order)
		{
			if (r.xp > 0 && r.inputs.size() > 1 && !locked.contains(r))
			{
				Short s = shortOf(r, pool, order, altar);
				if (s != null)
				{
					plans.computeIfAbsent(r.skill, Plan::new).shorts.add(s);
				}
			}
			else if (r.xp == 0 && !locked.contains(r))
			{
				Short s = shortOf(r, pool, order, altar);
				if (s != null && s.xp > 0)
				{
					// An unfinished potion short of vials is counted under the skill that finishes it.
					plans.computeIfAbsent(r.skill, Plan::new).shorts.add(s);
				}
			}
		}
		for (Recipe r : locked)
		{
			make(r, pool, lookup, altar, plans, true);
		}
		for (Plan p : plans.values())
		{
			p.makes.sort((a, b) -> Double.compare(b.xp, a.xp));
			p.shorts.sort((a, b) -> Double.compare(b.xp, a.xp));
		}
		plans.values().removeIf(p -> p.makes.isEmpty() && p.shorts.isEmpty());
		return new Workshop(plans);
	}

	private static double xpOf(Recipe r, Altar altar)
	{
		return r.bones() ? r.xp * altar.getMultiplier() : r.xp;
	}

	private static double perPrimary(Recipe r, Altar altar)
	{
		return xpOf(r, altar) / r.inputs.get(r.primary());
	}

	private static void make(Recipe r, Map<String, Long> pool, Catalogue.Lookup lookup, Altar altar, Map<Skill, Plan> plans, boolean locked)
	{
		long times = Long.MAX_VALUE;
		for (Map.Entry<String, Integer> in : r.inputs.entrySet())
		{
			times = Math.min(times, pool.getOrDefault(in.getKey(), 0L) / in.getValue());
		}
		if (times <= 0)
		{
			return;
		}
		for (Map.Entry<String, Integer> in : r.inputs.entrySet())
		{
			pool.merge(in.getKey(), -times * in.getValue(), Long::sum);
		}
		if (r.count > 0)
		{
			Catalogue.Match m = lookup.match(r.product);
			if (m != null)
			{
				pool.merge(m.store, times * r.count * m.units, Long::sum);
			}
		}
		double xp = times * xpOf(r, altar);
		if (xp <= 0)
		{
			return;
		}
		Plan p = plans.computeIfAbsent(r.skill, Plan::new);
		p.makes.add(new Make(r, times, xp, locked));
		if (locked)
		{
			p.lockedXp += xp;
		}
		else
		{
			p.xp += xp;
		}
	}

	/** The input that stops the rest of the main one being used, if one does. */
	private static Short shortOf(Recipe r, Map<String, Long> pool, List<Recipe> order, Altar altar)
	{
		String primary = r.primary();
		long more = pool.getOrDefault(primary, 0L) / r.inputs.get(primary);
		if (more <= 0)
		{
			return null;
		}
		String input = null;
		long need = 0;
		for (Map.Entry<String, Integer> in : r.inputs.entrySet())
		{
			long want = more * in.getValue() - pool.getOrDefault(in.getKey(), 0L);
			if (!in.getKey().equals(primary) && want > need)
			{
				need = want;
				input = in.getKey();
			}
		}
		if (input == null)
		{
			return null;
		}
		double each = xpOf(r, altar);
		if (r.count > 0)
		{
			// What the result is made into next, at its best.
			double next = 0;
			for (Recipe o : order)
			{
				if (o.primary().equalsIgnoreCase(stripDose(r.product)) || o.primary().equalsIgnoreCase(r.product))
				{
					next = Math.max(next, xpOf(o, altar) * r.count / o.inputs.get(o.primary()));
				}
			}
			each += next;
		}
		return new Short(r, input, need, more, more * each);
	}

	private static String stripDose(String product)
	{
		return product.replaceAll("\\(\\d\\)$", "").trim();
	}

	/** Nothing to make. */
	public static Workshop empty()
	{
		return new Workshop(Collections.emptyMap());
	}
}
