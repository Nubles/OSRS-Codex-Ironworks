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
	public void mostStepsHaveAPlaceToPointAt()
	{
		Route r = Route.load();
		long placed = r.steps().stream().filter(s -> r.target(s, null) != null).count();
		assertTrue(placed + " of " + r.size(), placed * 10 >= r.size() * 7);
	}
}
