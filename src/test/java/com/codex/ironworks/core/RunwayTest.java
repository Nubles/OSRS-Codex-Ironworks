package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.Test;

public class RunwayTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

	private static Works played(long hours, String store, long used, long gained)
	{
		Works w = new Works(0);
		w.played(TODAY, hours * 3_600_000L);
		if (used > 0)
		{
			w.record(Collections.singletonList(Flow.out(store, used, Use.CONSUMED, null)), TODAY);
		}
		if (gained > 0)
		{
			w.record(Collections.singletonList(Flow.in(store, gained, Source.FARMED, "Farming")), TODAY);
		}
		return w;
	}

	@Test
	public void lastsAsManyHoursAsTheStockAtTheBurn()
	{
		Works w = played(4, "Prayer potion", 80, 0);
		Runway r = Runway.of("Prayer potion", 100, w.window(TODAY, 7), 10, 2);
		assertEquals(20, r.burn, 1e-9);
		assertEquals(5, r.hours, 1e-9);
		assertEquals(Runway.Status.SHORT, r.status);
		assertEquals(100, r.need);
		assertTrue(r.wanting());
	}

	@Test
	public void lowOutAndSteady()
	{
		Works w = played(4, "Shark", 400, 0);
		assertEquals(Runway.Status.LOW, Runway.of("Shark", 50, w.window(TODAY, 7), 10, 2).status);
		assertEquals(Runway.Status.OUT, Runway.of("Shark", 0, w.window(TODAY, 7), 10, 2).status);
		Works farm = played(4, "Ranarr weed", 40, 60);
		assertEquals(Runway.Status.STEADY, Runway.of("Ranarr weed", 10, farm.window(TODAY, 7), 10, 2).status);
	}

	@Test
	public void needsAnHourOfPlayAndUseToTell()
	{
		Works shortPlay = played(0, "Shark", 10, 0);
		shortPlay.played(TODAY, 30 * 60_000L);
		assertEquals(Runway.Status.LEARNING, Runway.of("Shark", 10, shortPlay.window(TODAY, 7), 10, 2).status);
		Works unused = played(3, "Shark", 0, 0);
		Runway idle = Runway.of("Shark", 10, unused.window(TODAY, 7), 10, 2);
		assertEquals(Runway.Status.IDLE, idle.status);
		assertTrue(Double.isInfinite(idle.hours));
	}

	@Test
	public void sellingAndDyingDoNotSetThePace()
	{
		Works w = played(4, "Shark", 0, 0);
		w.record(Collections.singletonList(Flow.out("Shark", 500, Use.DIED, null)), TODAY);
		w.record(Collections.singletonList(Flow.out("Shark", 500, Use.SOLD, null)), TODAY);
		assertEquals(Runway.Status.IDLE, Runway.of("Shark", 10, w.window(TODAY, 7), 10, 2).status);
	}

	@Test
	public void theWindowCountsOnlyItsDays()
	{
		Works w = played(2, "Shark", 10, 0);
		w.played(TODAY.minusDays(10), 5 * 3_600_000L);
		w.record(Collections.singletonList(Flow.out("Shark", 1000, Use.CONSUMED, null)), TODAY.minusDays(10));
		Window week = w.window(TODAY, 7);
		assertEquals(2 * 3_600_000L, week.getPlayed());
		assertEquals(10, week.tally("Shark").burned());
		Window all = w.window(TODAY, 0);
		assertEquals(7 * 3_600_000L, all.getPlayed());
		assertEquals(1010, all.tally("Shark").burned());
	}
}
