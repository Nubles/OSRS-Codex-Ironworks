package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class TrackerTest
{
	private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

	private Works works;
	private Tracker tracker;
	private long time;

	@Before
	public void setUp()
	{
		works = new Works(0);
		tracker = new Tracker(works);
	}

	private static Map<String, Long> units(Object... pairs)
	{
		Map<String, Long> m = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
		}
		return m;
	}

	private List<Flow> tick(Map<String, Long> carried, Classify.Context ctx)
	{
		return tracker.tick(carried, ctx, DAY, time += 600);
	}

	private List<Flow> tick(Map<String, Long> carried)
	{
		return tick(carried, new Classify.Context().xp(EnumSet.noneOf(Skill.class)));
	}

	private static Classify.Context click(String option, String target, String store)
	{
		return new Classify.Context().xp(EnumSet.noneOf(Skill.class))
			.actions(Collections.singletonList(new Classify.Action(option, target, store, false)));
	}

	/** Opens the bank, lets it settle, and returns what settling recorded. */
	private List<Flow> openBank(Map<String, Long> bank, Map<String, Long> carried, Classify.Context ctx)
	{
		tracker.bankOpened();
		works.place(Place.BANK, bank, time);
		List<Flow> all = new java.util.ArrayList<>();
		for (int i = 0; i < Tracker.OPEN_TICKS; i++)
		{
			all.addAll(tick(carried, ctx));
		}
		return all;
	}

	private List<Flow> closeBank(Map<String, Long> carried, Classify.Context ctx)
	{
		tracker.bankClosed();
		List<Flow> all = new java.util.ArrayList<>();
		for (int i = 0; i < Tracker.CLOSE_TICKS; i++)
		{
			all.addAll(tick(carried, ctx));
		}
		return all;
	}

	@Test
	public void theFirstReadingIsNotAGain()
	{
		assertTrue(tick(units("Shark", 20)).isEmpty());
		assertEquals(20, works.stock("Shark"));
		List<Flow> eaten = tick(units("Shark", 19), click("Eat", "Shark", "Shark"));
		assertEquals(1, eaten.size());
		assertEquals(Use.CONSUMED, eaten.get(0).use);
		assertEquals(1, works.window(DAY, 1).tally("Shark").burned());
	}

	@Test
	public void bankingIsAMoveNotAGainOrLoss()
	{
		tick(units("Shark", 0));
		openBank(units("Shark", 100), units(), new Classify.Context());
		works.place(Place.BANK, units("Shark", 72), time);
		tick(units("Shark", 28));
		assertTrue(closeBank(units("Shark", 28), new Classify.Context()).isEmpty());
		assertEquals(100, works.stock("Shark"));
		assertTrue(works.window(DAY, 0).getStores().isEmpty());
	}

	@Test
	public void aHerbSackEmptiedAtTheBankIsFarmedHerbs()
	{
		Map<Skill, Long> recent = new HashMap<>();
		recent.put(Skill.FARMING, 3_000L);
		Classify.Context ctx = new Classify.Context().recent(recent);
		tick(units());
		openBank(units("Ranarr weed", 10), units(), ctx);
		works.place(Place.BANK, units("Ranarr weed", 40), time);
		tick(units(), ctx);
		List<Flow> flows = closeBank(units(), ctx);
		assertEquals(1, flows.size());
		assertEquals(Source.FARMED, flows.get(0).source);
		assertEquals(30, flows.get(0).units);
	}

	@Test
	public void whatTheBankGainedUnseenSinceTheLastVisitIsCounted()
	{
		Map<Skill, Long> recent = new HashMap<>();
		recent.put(Skill.FISHING, 9_000L);
		Classify.Context ctx = new Classify.Context().recent(recent);
		tick(units());
		openBank(units("Raw shark", 10), units(), ctx);
		closeBank(units(), ctx);
		// Away from the bank, a full barrel is sent to it: the next visit finds 28 more.
		tick(units(), ctx);
		List<Flow> flows = openBank(units("Raw shark", 38), units(), ctx);
		assertEquals(1, flows.size());
		assertEquals(Source.GATHERED, flows.get(0).source);
		assertEquals(28, flows.get(0).units);
	}

	@Test
	public void aDepositBoxMovesToTheBank()
	{
		tick(units());
		openBank(units("Coal", 0), units(), new Classify.Context());
		closeBank(units(), new Classify.Context());
		tick(units("Coal", 27), new Classify.Context().xp(EnumSet.of(Skill.MINING)));
		tracker.storageOpened(true);
		tick(units());
		tracker.storageClosed();
		tick(units());
		tick(units());
		assertTrue(openBank(units("Coal", 27), units(), new Classify.Context()).isEmpty());
		assertEquals(27, works.window(DAY, 0).tally("Coal").in(Source.GATHERED));
		assertEquals(0, works.window(DAY, 0).tally("Coal").burned());
	}

	@Test
	public void afterALoginTheBankIsNotComparedWithHowItWasLeft()
	{
		tick(units());
		openBank(units("Shark", 10), units(), new Classify.Context());
		closeBank(units(), new Classify.Context());
		tracker.restart(DAY);
		tick(units());
		// Played on another client in between: the bank is simply taken as it is.
		assertTrue(openBank(units("Shark", 500), units(), new Classify.Context()).isEmpty());
	}

	@Test
	public void aStaminaDrunkAtTheBankIsConsumed()
	{
		tick(units());
		openBank(units("Stamina potion", 40), units(), new Classify.Context());
		works.place(Place.BANK, units("Stamina potion", 36), time);
		tick(units("Stamina potion", 4));
		tick(units("Stamina potion", 3));
		List<Flow> flows = closeBank(units("Stamina potion", 3), new Classify.Context());
		assertEquals(1, flows.size());
		assertEquals(Use.CONSUMED, flows.get(0).use);
		assertEquals(1, flows.get(0).units);
	}

	@Test
	public void deathHoldsItemsInAGraveUntilTakenBack()
	{
		tick(units("Shark", 10));
		assertTrue(tick(units(), new Classify.Context().died(true)).isEmpty());
		assertTrue(tick(units("Shark", 10), click("Take", "Shark", "Shark")).isEmpty());
		assertEquals(0, works.window(DAY, 0).tally("Shark").burned());

		tick(units("Shark", 10), new Classify.Context().died(true));
		tick(units(), new Classify.Context().died(true));
		for (int i = 0; i < Tracker.GRAVE_TICKS; i++)
		{
			tick(units());
		}
		assertEquals(10, works.window(DAY, 0).tally("Shark").out(Use.DIED));
	}

	@Test
	public void hoppingStartsTheReadingAfresh()
	{
		tick(units("Shark", 10));
		tracker.hop();
		assertTrue(tick(units("Shark", 12)).isEmpty());
	}
}
