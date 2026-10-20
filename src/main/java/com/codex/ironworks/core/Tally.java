package com.codex.ironworks.core;

import java.util.HashMap;
import java.util.Map;

/** Units of one store in and out, by where they came from and went to. */
public final class Tally
{
	/** Units gained, by {@link Source} name. */
	Map<String, Long> in = new HashMap<>();
	/** Units lost, by {@link Use} name. */
	Map<String, Long> out = new HashMap<>();
	/** Units gathered, made or farmed, by the skill's name. */
	Map<String, Long> skills = new HashMap<>();

	void add(Flow f)
	{
		if (f.isGain())
		{
			in.merge(f.source.name(), f.units, Long::sum);
			if (f.skill != null)
			{
				skills.merge(f.skill, f.units, Long::sum);
			}
		}
		else
		{
			out.merge(f.use.name(), f.units, Long::sum);
		}
	}

	void add(Tally t)
	{
		t.in.forEach((k, v) -> in.merge(k, v, Long::sum));
		t.out.forEach((k, v) -> out.merge(k, v, Long::sum));
		t.skills.forEach((k, v) -> skills.merge(k, v, Long::sum));
	}

	public long in(Source source)
	{
		return in.getOrDefault(source.name(), 0L);
	}

	public long out(Use use)
	{
		return out.getOrDefault(use.name(), 0L);
	}

	/** Everything gained, from any source this version knows. */
	public long gained()
	{
		long sum = 0;
		for (Source s : Source.values())
		{
			sum += in(s);
		}
		return sum;
	}

	/** What was worked through: eaten, cast, fired, made into something else and so on. */
	public long burned()
	{
		long sum = 0;
		for (Use u : Use.values())
		{
			if (u.isBurn())
			{
				sum += out(u);
			}
		}
		return sum;
	}

	/** What the player gathered, made or farmed themselves. */
	public long self()
	{
		long sum = 0;
		for (Source s : Source.values())
		{
			if (s.isSelf())
			{
				sum += in(s);
			}
		}
		return sum;
	}

	public Map<String, Long> getSkills()
	{
		return skills;
	}

	boolean isEmpty()
	{
		return in.isEmpty() && out.isEmpty();
	}

	/** Drops figures that are not counts, from a damaged save. */
	Tally repair()
	{
		in = clean(in);
		out = clean(out);
		skills = clean(skills);
		return this;
	}

	private static Map<String, Long> clean(Map<String, Long> m)
	{
		Map<String, Long> c = new HashMap<>();
		if (m != null)
		{
			m.forEach((k, v) ->
			{
				if (k != null && v != null && v > 0)
				{
					c.put(k, v);
				}
			});
		}
		return c;
	}
}
