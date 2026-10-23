package com.codex.ironworks;

import com.codex.ironworks.core.Route;
import com.codex.ironworks.core.Workshop;
import com.codex.ironworks.ui.Format;
import com.codex.ironworks.ui.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Works out the Guide tab: the step in hand with its checks, the steps after it, and the supplies ahead. */
final class Guides
{
	/** Steps after the one in hand listed under it. */
	static final int NEXT = 4;
	/** Steps ahead whose supplies are counted. */
	static final int AHEAD = 12;

	private Guides()
	{
	}

	/**
	 * @param found everything the account owns, by name, from the Find tab
	 * @param workshop what the stores could become, for banked experience toward a level
	 * @param xp each skill's experience now
	 */
	static View.GuideView build(Route route, Set<String> ticked, Route.Account a, boolean following, List<View.Found> found,
		Workshop workshop, Map<Skill, Integer> xp)
	{
		View.GuideView g = new View.GuideView();
		g.following = following;
		g.total = route.size();
		g.done = route.doneCount(ticked, a);
		Map<String, View.Found> owned = new HashMap<>();
		for (View.Found f : found)
		{
			owned.put(f.name.toLowerCase(Locale.ENGLISH), f);
		}

		Route.Step current = route.current(ticked, a);
		if (current != null)
		{
			g.current = step(route, current, ticked, a, owned, workshop, xp, true);
			List<View.StepView> next = new ArrayList<>();
			Map<String, View.NeedView> needs = new LinkedHashMap<>();
			int ahead = 0;
			for (Route.Step s : route.steps().subList(current.index, route.size()))
			{
				if (route.done(s, ticked, a))
				{
					continue;
				}
				if (s != current && next.size() < NEXT)
				{
					next.add(step(route, s, ticked, a, owned, workshop, xp, false));
				}
				for (Map.Entry<String, Long> e : s.needs.entrySet())
				{
					View.NeedView n = needs.computeIfAbsent(e.getKey().toLowerCase(Locale.ENGLISH), k ->
					{
						View.NeedView v = new View.NeedView();
						View.Found f = find(owned, k);
						v.item = e.getKey();
						v.have = f == null ? 0 : f.total;
						v.icon = f == null ? -1 : f.icon;
						return v;
					});
					if (n.need == 0)
					{
						n.stepsAway = ahead;
					}
					n.need += e.getValue();
				}
				if (++ahead >= AHEAD)
				{
					break;
				}
			}
			g.next = next;
			g.needs = new ArrayList<>(needs.values());
		}

		List<View.SectionView> sections = new ArrayList<>();
		for (String name : route.sections())
		{
			int[] d = route.section(name, ticked, a);
			View.SectionView sv = new View.SectionView();
			sv.name = name;
			sv.done = d[0];
			sv.all = d[1];
			sections.add(sv);
		}
		g.sections = sections;
		return g;
	}

	private static View.StepView step(Route route, Route.Step s, Set<String> ticked, Route.Account a, Map<String, View.Found> owned,
		Workshop workshop, Map<Skill, Integer> xp, boolean full)
	{
		View.StepView v = new View.StepView();
		v.key = s.key();
		v.section = s.section;
		v.text = s.text;
		v.optional = s.optional;
		v.met = Route.met(s, a) || route.passed(s, a);
		v.done = route.done(s, ticked, a);
		if (!full)
		{
			return v;
		}
		v.tips = new ArrayList<>(s.tips);
		List<View.CheckView> checks = new ArrayList<>();
		List<String> hints = new ArrayList<>();
		for (String q : s.started)
		{
			checks.add(new View.CheckView(q, a.questStarted(q) ? "Started" : "Not started", a.questStarted(q)));
		}
		for (String q : s.quests)
		{
			boolean doneQ = a.questDone(q);
			checks.add(new View.CheckView(q, doneQ ? "Done" : a.questStarted(q) ? "Started" : "To do", doneQ));
		}
		for (Map.Entry<String, Integer> e : s.skills.entrySet())
		{
			int level = a.level(e.getKey());
			boolean ok = level >= e.getValue();
			checks.add(new View.CheckView(e.getKey(), level + " / " + e.getValue(), ok));
			Skill skill = skill(e.getKey());
			Integer now = skill == null ? null : xp.get(skill);
			if (!ok && now != null && e.getValue() <= Experience.MAX_REAL_LEVEL)
			{
				long short_ = Experience.getXpForLevel(e.getValue()) - now;
				Workshop.Plan plan = workshop.plan(skill);
				double banked = plan == null ? 0 : plan.xp;
				if (banked >= short_ && short_ > 0)
				{
					hints.add("Your stores hold " + Format.amount(Math.round(banked)) + " " + e.getKey() + " xp: enough to get there. See the Make tab");
				}
				else if (banked > 0)
				{
					hints.add(Format.amount(short_) + " xp to go; your stores hold " + Format.amount(Math.round(banked)) + " of it");
				}
				else if (short_ > 0)
				{
					hints.add(Format.amount(short_) + " xp to go");
				}
			}
		}
		for (Route.Var var : s.varbits)
		{
			boolean ok = var.exact ? a.varbit(var.id) == var.value : a.varbit(var.id) >= var.value;
			checks.add(new View.CheckView("Done in game", ok ? "Yes" : "Not yet", ok));
		}
		if (s.questPoints > 0)
		{
			checks.add(new View.CheckView("Quest points", a.questPoints() + " / " + s.questPoints, a.questPoints() >= s.questPoints));
		}
		if (s.combat > 0)
		{
			checks.add(new View.CheckView("Combat level", a.combat() + " / " + s.combat, a.combat() >= s.combat));
		}
		List<String> items = new ArrayList<>(s.owned);
		items.addAll(s.items);
		for (String i : items)
		{
			View.Found f = find(owned, i);
			checks.add(new View.CheckView(i, f != null ? "Owned" : "Not yet", f != null));
		}
		for (List<String> names : s.anyOf)
		{
			String have = null;
			for (String i : names)
			{
				View.Found f = find(owned, i);
				if (f != null)
				{
					have = f.name;
					break;
				}
			}
			checks.add(new View.CheckView(have != null ? have : String.join(" or ", names), have != null ? "Owned" : "Not yet", have != null));
		}
		v.checks = checks;
		v.hints = hints;
		return v;
	}

	/** The owned entry for a name, or for a charged or dosed form of it. */
	static View.Found find(Map<String, View.Found> owned, String name)
	{
		String key = name.toLowerCase(Locale.ENGLISH);
		View.Found f = owned.get(key);
		if (f != null)
		{
			return f;
		}
		String prefix = key + " (";
		for (Map.Entry<String, View.Found> e : owned.entrySet())
		{
			if (e.getKey().startsWith(prefix))
			{
				return e.getValue();
			}
		}
		return null;
	}

	private static Skill skill(String name)
	{
		for (Skill s : Skill.values())
		{
			if (s.getName().equalsIgnoreCase(name))
			{
				return s;
			}
		}
		return null;
	}
}
