package com.codex.ironworks;

import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Route;
import com.codex.ironworks.core.Workshop;
import com.codex.ironworks.ui.View;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GuidesTest
{
	private final Route route = Route.load();

	private static Route.Account account(Set<String> quests, int level)
	{
		return new Route.Account()
		{
			@Override
			public boolean questDone(String quest)
			{
				return quests.contains(quest);
			}

			@Override
			public int level(String skill)
			{
				return level;
			}
		};
	}

	private static View.Found found(String name, long total)
	{
		View.Found f = new View.Found();
		f.name = name;
		f.total = total;
		return f;
	}

	@Test
	public void suppliesAheadAreCountedAgainstWhatIsOwned()
	{
		Set<String> quests = new HashSet<>();
		for (Quest q : Quest.values())
		{
			quests.add(q.getName());
		}
		quests.remove("Waterfall Quest");
		quests.remove("Druidic Ritual");
		View.GuideView g = Guides.build(route, new HashSet<>(), account(quests, 25), false,
			Arrays.asList(found("Rope", 2), found("Air rune", 3)), Workshop.empty(), Collections.emptyMap());
		assertNotNull(g.current);
		assertEquals("Early members' quests", g.current.section);
		View.NeedView air = g.needs.stream().filter(n -> n.item.equals("Air rune")).findFirst().get();
		assertEquals(6, air.need);
		assertEquals(3, air.have);
		View.NeedView rope = g.needs.stream().filter(n -> n.item.equals("Rope")).findFirst().get();
		assertTrue(rope.need >= 1 && rope.have == 2);
	}

	@Test
	public void everyPartOfTheRouteListsItsStepsAndTheOneInHand()
	{
		View.GuideView g = Guides.build(route, new HashSet<>(), account(new HashSet<>(), 1), false,
			Arrays.asList(found("Black mask (10)", 1)), Workshop.empty(), Collections.emptyMap());
		int steps = 0;
		int current = 0;
		for (View.SectionView sv : g.sections)
		{
			assertTrue(sv.all <= sv.steps.size());
			assertEquals(sv.steps.stream().filter(st -> !st.optional).count(), sv.all);
			steps += sv.steps.size();
			current += sv.current ? 1 : 0;
		}
		assertEquals(route.size(), steps);
		assertEquals(1, current);
		assertTrue(g.sections.get(0).current);
		assertEquals(route.steps().stream().filter(st -> !st.optional).count(), g.total);
	}

	@Test
	public void aQuestStepNamesWhatItsQuestStillNeeds()
	{
		Route r = Route.parse(java.util.Collections.singletonList("A | Complete Regicide | quest:Regicide"));
		java.util.List<View.CheckView> checks = new java.util.ArrayList<>();
		Guides.requirements(r.steps().get(0), account(new HashSet<>(), 40), checks);
		java.util.List<String> labels = new java.util.ArrayList<>();
		checks.forEach(c -> labels.add(c.label));
		assertTrue(labels.toString(), labels.contains("Underground Pass"));
		assertTrue(labels.toString(), labels.contains("Agility for the quest"));
		assertTrue(labels.indexOf("Biohazard") < labels.indexOf("Underground Pass"));

		checks.clear();
		Set<String> all = new HashSet<>();
		for (Quest q : Quest.values())
		{
			all.add(q.getName());
		}
		all.remove("Regicide");
		Guides.requirements(r.steps().get(0), account(all, 99), checks);
		assertTrue(checks.isEmpty());

		checks.clear();
		Route d = Route.parse(java.util.Collections.singletonList("A | Finish the Ardougne easy diary | varbit:4499; diary:Ardougne Easy"));
		Guides.requirements(d.steps().get(0), account(new HashSet<>(), 1), checks);
		labels.clear();
		checks.forEach(c -> labels.add(c.label));
		assertTrue(labels.toString(), labels.contains("Plague City"));
		assertTrue(labels.toString(), labels.contains("Thieving for the diary"));
	}

	@Test
	public void bankedExperienceIsNamedTowardALevel()
	{
		Set<String> quests = new HashSet<>();
		for (Quest q : Quest.values())
		{
			quests.add(q.getName());
		}
		Route.Account a = account(quests, 99);
		// Everything done but the Herblore step: level 30 Herblore, with herbs in the bank.
		Route.Account partial = new Route.Account()
		{
			@Override
			public boolean questDone(String quest)
			{
				return a.questDone(quest);
			}

			@Override
			public int level(String skill)
			{
				return skill.equals("Herblore") ? 30 : 99;
			}

			@Override
			public int questPoints()
			{
				return 300;
			}
		};
		Set<String> ticked = new HashSet<>();
		for (Route.Step s : route.steps())
		{
			if (!s.skills.containsKey("Herblore") && !route.done(s, ticked, partial))
			{
				ticked.add(s.key());
			}
		}
		Map<String, Long> stocks = new HashMap<>();
		stocks.put("Ranarr weed", 500L);
		stocks.put("Vial of water", 500L);
		stocks.put("Snape grass", 500L);
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		levels.put(Skill.HERBLORE, 38);
		Workshop w = Workshop.plan(stocks, Catalogue.standard(), levels, Altar.GILDED);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		xp.put(Skill.HERBLORE, Experience.getXpForLevel(30));
		View.GuideView g = Guides.build(route, ticked, partial, false, Collections.emptyList(), w, xp);
		assertTrue(g.current.text, g.current.text.contains("Herblore"));
		List<String> hints = g.current.hints;
		assertEquals(1, hints.size());
		assertTrue(hints.get(0), hints.get(0).startsWith("Your stores hold"));
	}
}
