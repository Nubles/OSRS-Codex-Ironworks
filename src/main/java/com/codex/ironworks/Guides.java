package com.codex.ironworks;

import com.codex.ironworks.core.QuestReqs;
import com.codex.ironworks.core.Route;
import com.codex.ironworks.core.Workshop;
import com.codex.ironworks.ui.Format;
import com.codex.ironworks.ui.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
	/** Unfinished quests named as needed first, at most. */
	static final int FIRST = 5;
	/** Route quests listed as ready now, at most. */
	static final int READY = 6;

	private static final QuestReqs REQS = QuestReqs.load();
	private static final QuestReqs DIARIES = QuestReqs.diaries();

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
		for (Route.Step s : route.steps())
		{
			boolean done = route.done(s, ticked, a);
			// An optional step left undone does not hold the route back from being finished.
			if (done || !s.optional)
			{
				g.total++;
				g.done += done ? 1 : 0;
			}
		}
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

		ready(g, route, ticked, a, current, owned, workshop, xp);

		List<View.UseView> uses = new ArrayList<>();
		for (Route.Step s : route.steps())
		{
			if (s.needs.isEmpty() || route.done(s, ticked, a))
			{
				continue;
			}
			for (Map.Entry<String, Long> e : s.needs.entrySet())
			{
				View.UseView u = new View.UseView();
				u.item = e.getKey();
				u.qty = e.getValue();
				u.step = s.text;
				uses.add(u);
			}
		}
		g.uses = uses;

		List<View.SectionView> sections = new ArrayList<>();
		for (String name : route.sections())
		{
			View.SectionView sv = new View.SectionView();
			sv.name = name;
			List<View.StepView> steps = new ArrayList<>();
			for (Route.Step s : route.steps())
			{
				if (s.section.equals(name))
				{
					View.StepView step = step(route, s, ticked, a, owned, workshop, xp, false);
					steps.add(step);
					sv.current |= s == current;
					if (step.done || !s.optional)
					{
						sv.all++;
						sv.done += step.done ? 1 : 0;
					}
				}
			}
			sv.steps = steps;
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
		v.tipsDone = new ArrayList<>();
		for (int i = 0; i < s.tips.size(); i++)
		{
			v.tipsDone.add(ticked.contains(View.tipKey(s.key(), i)));
		}
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
		requirements(s, a, checks);
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

	/** Quests the account could do now, past the steps coming up: those further on the route, then the rest. */
	private static void ready(View.GuideView g, Route route, Set<String> ticked, Route.Account a, Route.Step current,
		Map<String, View.Found> owned, Workshop workshop, Map<Skill, Integer> xp)
	{
		List<View.StepView> ready = new ArrayList<>();
		List<View.StepView> unlock = new ArrayList<>();
		Set<String> listed = new java.util.HashSet<>();
		for (View.StepView n : g.next)
		{
			listed.add(n.key);
		}
		Set<String> onRoute = new java.util.HashSet<>();
		for (Route.Step s : route.steps())
		{
			onRoute.addAll(s.quests);
			if (s == current || s.quests.isEmpty() || listed.contains(s.key()) || route.done(s, ticked, a)
				|| current != null && s.index < current.index)
			{
				continue;
			}
			if (questReady(s, a))
			{
				if (ready.size() < READY)
				{
					ready.add(step(route, s, ticked, a, owned, workshop, xp, false));
				}
			}
			else if (unlock.size() < READY)
			{
				List<String> banked = banked(s, a, workshop, xp);
				if (banked != null)
				{
					View.StepView v = step(route, s, ticked, a, owned, workshop, xp, false);
					v.hints = banked;
					unlock.add(v);
				}
			}
		}
		List<String> others = new ArrayList<>();
		for (net.runelite.api.Quest q : net.runelite.api.Quest.values())
		{
			String name = q.getName();
			if (!onRoute.contains(name) && !a.questDone(name) && met(name, a))
			{
				others.add(name);
			}
		}
		g.ready = ready;
		g.unlock = unlock;
		g.readyElse = others;
	}

	/**
	 * The levels a quest step lacks when the stores hold the experience for every one of them, such as
	 * "Herblore 52"; null when anything else stands in the way.
	 */
	static List<String> banked(Route.Step s, Route.Account a, Workshop workshop, Map<Skill, Integer> xp)
	{
		Map<String, Integer> levels = new LinkedHashMap<>(s.skills);
		int qp = s.questPoints;
		for (String q : s.quests)
		{
			if (a.questDone(q))
			{
				continue;
			}
			QuestReqs.Req req = REQS.of(q);
			for (String first : req.quests)
			{
				if (!a.questDone(first))
				{
					return null;
				}
			}
			req.skills.forEach((k, v) -> levels.merge(k, v, Math::max));
			qp = Math.max(qp, req.questPoints);
		}
		if (a.questPoints() < qp)
		{
			return null;
		}
		List<String> out = new ArrayList<>();
		for (Map.Entry<String, Integer> e : levels.entrySet())
		{
			if (a.level(e.getKey()) >= e.getValue())
			{
				continue;
			}
			Skill skill = skill(e.getKey());
			Integer now = skill == null ? null : xp.get(skill);
			Workshop.Plan plan = skill == null ? null : workshop.plan(skill);
			if (now == null || plan == null || e.getValue() > Experience.MAX_REAL_LEVEL
				|| plan.xp < Experience.getXpForLevel(e.getValue()) - now)
			{
				return null;
			}
			out.add(e.getKey() + " " + e.getValue());
		}
		return out.isEmpty() ? null : out;
	}

	/** True when nothing stands between the account and every quest the step asks for. */
	static boolean questReady(Route.Step s, Route.Account a)
	{
		for (Map.Entry<String, Integer> e : s.skills.entrySet())
		{
			if (a.level(e.getKey()) < e.getValue())
			{
				return false;
			}
		}
		for (String q : s.quests)
		{
			if (!a.questDone(q) && !met(q, a))
			{
				return false;
			}
		}
		return true;
	}

	/** True when a quest's own levels, quest points and quests first are all in hand. */
	static boolean met(String quest, Route.Account a)
	{
		QuestReqs.Req req = REQS.of(quest);
		for (String q : req.quests)
		{
			if (!a.questDone(q))
			{
				return false;
			}
		}
		for (Map.Entry<String, Integer> e : req.skills.entrySet())
		{
			if (a.level(e.getKey()) < e.getValue())
			{
				return false;
			}
		}
		return a.questPoints() >= req.questPoints;
	}

	/**
	 * What the step's quests and diaries ask that the account lacks: quests to finish first, down the whole chain,
	 * and the highest level any quest on the way asks.
	 */
	static void requirements(Route.Step s, Route.Account a, List<View.CheckView> checks)
	{
		Set<String> first = new LinkedHashSet<>();
		Map<String, Integer> levels = new LinkedHashMap<>();
		int[] qp = {0};
		Set<String> seen = new java.util.HashSet<>();
		List<String> quests = new ArrayList<>(s.quests);
		quests.addAll(s.started);
		for (String q : quests)
		{
			if (!a.questDone(q))
			{
				chain(q, a, seen, first, levels, qp);
			}
		}
		for (String d : s.diaries)
		{
			QuestReqs.Req req = DIARIES.of(d);
			for (String q : req.quests)
			{
				if (!a.questDone(q))
				{
					chain(q, a, seen, first, levels, qp);
				}
			}
			for (Map.Entry<String, Integer> e : req.skills.entrySet())
			{
				levels.merge(e.getKey(), e.getValue(), Math::max);
			}
			qp[0] = Math.max(qp[0], req.questPoints);
		}
		first.removeAll(quests);
		String what = quests.isEmpty() ? "the diary" : "the quest";
		List<String> named = new ArrayList<>(first);
		for (String q : named.subList(0, Math.min(FIRST, named.size())))
		{
			checks.add(new View.CheckView(q, a.questStarted(q) ? "Started" : "Needed first", false));
		}
		if (named.size() > FIRST)
		{
			checks.add(new View.CheckView("and " + (named.size() - FIRST) + " more quests first", "", false));
		}
		for (Map.Entry<String, Integer> e : levels.entrySet())
		{
			int level = a.level(e.getKey());
			Integer asked = s.skills.get(e.getKey());
			if (level < e.getValue() && (asked == null || asked < e.getValue()))
			{
				checks.add(new View.CheckView(e.getKey() + " for " + what, level + " / " + e.getValue(), false));
			}
		}
		if (qp[0] > Math.max(s.questPoints, a.questPoints()))
		{
			checks.add(new View.CheckView("Quest points for " + what, a.questPoints() + " / " + qp[0], false));
		}
	}

	/** Walks a quest's unfinished prerequisites, the deepest first. */
	private static void chain(String quest, Route.Account a, Set<String> seen, Set<String> first, Map<String, Integer> levels, int[] qp)
	{
		if (!seen.add(quest))
		{
			return;
		}
		QuestReqs.Req req = REQS.of(quest);
		for (String q : req.quests)
		{
			if (!a.questDone(q))
			{
				chain(q, a, seen, first, levels, qp);
			}
		}
		for (Map.Entry<String, Integer> e : req.skills.entrySet())
		{
			levels.merge(e.getKey(), e.getValue(), Math::max);
		}
		qp[0] = Math.max(qp[0], req.questPoints);
		first.add(quest);
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

	/** The chat line for a step done in play: what was done and what comes next. */
	static String chat(Route.Step done, Route.Step next)
	{
		String line = "Done: " + done.text + ".";
		return next == null ? line + " That was the last step of the route." : line + " Next: " + next.text + ".";
	}
}
