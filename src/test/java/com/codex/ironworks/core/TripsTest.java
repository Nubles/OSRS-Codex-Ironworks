package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TripsTest
{
	private static final long T0 = 1_000_000_000L;
	private static final long ROUND = 200_000L;

	private static List<Flow> craft(String rune, long essence, long runes)
	{
		return Arrays.asList(Flow.out("Pure essence", essence, Use.MADE_INTO, "Runecraft"),
			Flow.in(rune, runes, Source.MADE, "Runecraft"));
	}

	/** One trip: the pack crafted, then two pouch empties crafted, then the walk back. */
	private static void trip(Works w, String rune, long at)
	{
		w.rate(craft(rune, 60, 120), at);
		w.rate(Collections.emptyList(), at + 600);
		w.rate(craft(rune, 30, 60), at + 3_000);
		w.rate(craft(rune, 15, 30), at + 6_000);
		w.rate(Collections.emptyList(), at + 60_000);
	}

	@Test
	public void aTripIsOneBurstAtTheAltar()
	{
		Works w = new Works(1);
		trip(w, "Nature rune", T0);
		Trips.Leg l = w.trips().leg("Nature rune");
		assertEquals(1, l.getTrips());
		assertEquals(105, l.essencePerTrip(), 0.001);
		assertEquals(2.0, l.runesPerEssence(), 0.001);
		assertEquals("Nature rune", w.trips().current());
		assertEquals(105, w.trips().bestTrip());
		// One trip is no round yet.
		assertEquals(0, l.perHour(), 0);
	}

	@Test
	public void runesAnHourCountTheWholeRound()
	{
		Works w = new Works(1);
		for (int i = 0; i < 5; i++)
		{
			trip(w, "Nature rune", T0 + i * ROUND);
		}
		Trips.Leg l = w.trips().leg("Nature rune");
		assertEquals(5, l.getTrips());
		assertEquals(4, l.getTimed());
		// 210 runes every 200 seconds, bank and run included: 3,780 an hour.
		assertEquals(3780, l.perHour(), 0.5);
		assertEquals(ROUND, l.roundMs());
		// The gain-to-gain pace sees only the seconds at the altar.
		assertTrue(w.rate("Nature rune", Source.MADE).getUnits() > 0);
	}

	@Test
	public void breaksAndOtherAltarsAreNotTimed()
	{
		Works w = new Works(1);
		trip(w, "Law rune", T0);
		trip(w, "Law rune", T0 + Trips.ROUND_MS + 1);
		trip(w, "Nature rune", T0 + Trips.ROUND_MS + 1 + ROUND);
		trip(w, "Law rune", T0 + Trips.ROUND_MS + 1 + 2 * ROUND);
		assertEquals(3, w.trips().leg("Law rune").getTrips());
		assertEquals(0, w.trips().leg("Law rune").getTimed());
		assertEquals(0, w.trips().leg("Nature rune").getTimed());
		assertEquals("Law rune", w.trips().current());
	}

	@Test
	public void onlyRunecraftCounts()
	{
		Works w = new Works(1);
		w.rate(Arrays.asList(Flow.out("Pure essence", 20, Use.DROPPED, null), Flow.in("Nature rune", 100, Source.BOUGHT, null),
			Flow.in("Pure essence", 27, Source.MADE, "Runecraft")), T0);
		w.rate(Collections.emptyList(), T0 + 60_000);
		assertTrue(w.trips().isEmpty());
		assertNull(w.trips().current());
	}

	@Test
	public void tripsAreSavedAndOldSavesReadWithout()
	{
		Gson gson = new Gson();
		Works w = new Works(1);
		for (int i = 0; i < 4; i++)
		{
			trip(w, "Blood rune", T0 + i * ROUND);
		}
		Works.Loaded back = Works.read(gson, w.write(gson), 99);
		assertFalse(back.damaged);
		Trips.Leg l = back.works.trips().leg("Blood rune");
		assertEquals(4, l.getTrips());
		assertEquals(3, l.getTimed());
		assertEquals(w.trips().leg("Blood rune").perHour(), l.perHour(), 0.001);
		assertEquals("Blood rune", back.works.trips().current());
		Works old = Works.read(gson, "{\"version\":1,\"since\":5}", 99).works;
		assertTrue(old.trips().isEmpty());
		Works damaged = Works.read(gson, "{\"version\":1,\"since\":5,\"trips\":{\"legs\":{\"Law rune\":{\"trips\":-4}},\"recent\":[null]}}", 99).works;
		assertEquals(0, damaged.trips().leg("Law rune").getTrips());
		assertTrue(damaged.trips().recent().isEmpty());
	}
}
