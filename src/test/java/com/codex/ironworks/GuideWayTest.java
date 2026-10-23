package com.codex.ironworks;

import com.codex.ironworks.core.Route;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GuideWayTest
{
	@Test
	public void theMinimapArrowSitsOnTheWayOrOnTheSpot()
	{
		assertNull(Compass.toward(1, 0));
		assertArrayEquals(new int[]{5, -3, 1}, Compass.toward(5, -3));
		int[] far = Compass.toward(300, 0);
		assertArrayEquals(new int[]{Compass.REACH, 0, 0}, far);
		int[] diagonal = Compass.toward(-400, -400);
		assertTrue(diagonal[0] < 0 && diagonal[0] == diagonal[1] && diagonal[2] == 0);
		assertTrue(Math.hypot(diagonal[0], diagonal[1]) <= Compass.REACH + 1);
	}

	@Test
	public void theStepsItemsGlowWhateverTheirCharges()
	{
		Route r = Route.parse(Arrays.asList(
			"A | Get a black mask | owned:Black mask; need:Prayer potion 4; any:Games necklace,Ring of dueling"));
		Set<String> items = RouteGuide.items(r.steps().get(0));
		assertTrue(RouteGuide.asked(items, "Black mask (10)"));
		assertTrue(RouteGuide.asked(items, "Black mask"));
		assertTrue(RouteGuide.asked(items, "Prayer potion(4)"));
		assertTrue(RouteGuide.asked(items, "Ring of dueling(8)"));
		assertTrue(!RouteGuide.asked(items, "Black masked thing"));
		assertTrue(!RouteGuide.asked(Collections.emptySet(), "Black mask"));
	}

	@Test
	public void aStepDoneNamesTheNextInChat()
	{
		Route r = Route.parse(Arrays.asList("A | Complete Cook's Assistant | quest:Cook's Assistant",
			"A | Complete Sheep Shearer | quest:Sheep Shearer"));
		assertEquals("Done: Complete Cook's Assistant. Next: Complete Sheep Shearer.",
			Guides.chat(r.steps().get(0), r.steps().get(1)));
		assertTrue(Guides.chat(r.steps().get(1), null).endsWith("last step of the route."));
	}

	@Test
	public void questStepsHaveAPlaceToPointAt()
	{
		Route r = Route.load();
		// Training can happen anywhere; every quest, start and named place should be pointed at.
		java.util.List<Route.Step> placeable = r.steps().stream()
			.filter(s -> !s.quests.isEmpty() || !s.started.isEmpty() || s.go != null)
			.collect(java.util.stream.Collectors.toList());
		long placed = placeable.stream().filter(s -> r.target(s, null) != null).count();
		assertTrue(placed + " of " + placeable.size(), placed * 10 >= placeable.size() * 9);
	}

	@Test
	public void theRouteCarriesEveryQuestAndDiary()
	{
		Route r = Route.load();
		Set<String> quests = new java.util.HashSet<>();
		Set<String> diaries = new java.util.HashSet<>();
		r.steps().forEach(s ->
		{
			quests.addAll(s.quests);
			diaries.addAll(s.diaries);
		});
		assertTrue(quests.size() + " quests", quests.size() >= 200);
		assertEquals(48, diaries.size());
		assertTrue(r.size() + " steps", r.size() >= 300);
	}

	@Test
	public void theWikisNotesSitUnderTheirQuestAndTickOff()
	{
		Route r = Route.load();
		Route.Step cook = r.steps().stream().filter(s -> s.quests.contains("Cook's Assistant")).findFirst().get();
		assertTrue(cook.tips.size() >= 5);
		assertTrue(r.steps().stream().anyMatch(s -> s.text.equals("Train Thieving to 21")));
		Set<String> ticked = new java.util.HashSet<>();
		Route.Account none = new Route.Account()
		{
			@Override
			public boolean questDone(String quest)
			{
				return false;
			}

			@Override
			public int level(String skill)
			{
				return 1;
			}
		};
		com.codex.ironworks.ui.View.GuideView g = Guides.build(r, ticked, none, false, Collections.emptyList(),
			com.codex.ironworks.core.Workshop.empty(), Collections.emptyMap());
		ticked.add(com.codex.ironworks.ui.View.tipKey(g.current.key, 0));
		g = Guides.build(r, ticked, none, false, Collections.emptyList(), com.codex.ironworks.core.Workshop.empty(), Collections.emptyMap());
		assertTrue(g.current.tipsDone.get(0));
		assertTrue(!g.current.tipsDone.get(1));
		assertTrue(com.codex.ironworks.ui.View.heading("While in Port Sarim:"));
	}
}
