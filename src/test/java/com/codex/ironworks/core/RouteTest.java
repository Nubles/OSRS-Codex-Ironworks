package com.codex.ironworks.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RouteTest
{
	private static final Set<String> TAGS = new HashSet<>(Arrays.asList("quest", "started", "skill", "qp", "combat", "varbit", "varp",
		"ca", "item", "owned", "gone", "at", "area", "empty", "coins", "chat", "deathbank", "nodeathbank", "built", "house", "go", "npc",
		"optional", "need"));

	private final Route route = Route.load();

	@Test
	public void theRouteLoads()
	{
		assertTrue(route.size() > 80);
		assertEquals(route.size(), route.steps().stream().map(Route.Step::key).distinct().count());
	}

	@Test
	public void everyQuestAndSkillIsOneTheGameKnows()
	{
		Set<String> quests = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
		Set<String> skills = Arrays.stream(Skill.values()).map(Skill::getName).collect(Collectors.toSet());
		for (Route.Step s : route.steps())
		{
			for (String q : s.quests)
			{
				assertTrue(q, quests.contains(q));
			}
			for (String q : s.started)
			{
				assertTrue(q, quests.contains(q));
			}
			for (String k : s.skills.keySet())
			{
				assertTrue(k, skills.contains(k));
			}
		}
	}

	@Test
	public void everyTagIsKnownAndEveryStepCanBeTold() throws Exception
	{
		try (BufferedReader r = new BufferedReader(new InputStreamReader(Route.class.getResourceAsStream("/com/codex/ironworks/route.txt"), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = r.readLine()) != null)
			{
				String[] parts = line.split("\\|", -1);
				if (line.startsWith("#") || line.startsWith("~") || parts.length < 3)
				{
					continue;
				}
				for (String tag : parts[2].split(";"))
				{
					String kind = tag.trim().split(":")[0].trim();
					assertTrue(line, kind.isEmpty() || TAGS.contains(kind));
				}
			}
		}
		for (Route.Step s : route.steps())
		{
			assertTrue("Nothing ticks: " + s.text, s.index > 0 || s.watched());
		}
	}

	@Test
	public void lastingStepsTickThemselvesAndPassTheStepsBefore()
	{
		Set<String> done = new HashSet<>(Arrays.asList("Cook's Assistant", "Sheep Shearer"));
		Route.Account a = account(done, 1);
		Route.Step first = route.steps().get(0);
		assertFalse(first.checkable());
		assertTrue(route.passed(first, a));
		Route.Step current = route.current(new HashSet<>(), a);
		assertEquals("Complete X Marks the Spot", current.text);
	}

	@Test
	public void suppliesAreRead()
	{
		Route.Step waterfall = route.steps().stream().filter(s -> s.quests.contains("Waterfall Quest")).findFirst().get();
		assertEquals(Long.valueOf(6), waterfall.needs.get("Air rune"));
		assertEquals(Long.valueOf(1), waterfall.needs.get("Rope"));
	}

	@Test
	public void aFinishedAccountHasNothingLeft()
	{
		Set<String> all = Arrays.stream(Quest.values()).map(Quest::getName).collect(Collectors.toSet());
		Route.Account a = account(all, 99);
		Set<String> ticked = new HashSet<>();
		for (Route.Step s : route.steps())
		{
			if (!route.done(s, ticked, a))
			{
				ticked.add(s.key());
			}
		}
		assertNull(route.current(ticked, a));
	}

	static Route.Account account(Set<String> quests, int level)
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

			@Override
			public int questPoints()
			{
				return level >= 99 ? 300 : 0;
			}
		};
	}
}
