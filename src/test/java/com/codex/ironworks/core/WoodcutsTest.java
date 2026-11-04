package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

/** The account's own woodcutting log. */
public class WoodcutsTest
{
	private static final long T = 1_700_000_000_000L;

	private static List<Flow> logs(String store, long n)
	{
		return Collections.singletonList(Flow.in(store, n, Source.GATHERED, "Woodcutting"));
	}

	@Test
	public void logsCutWithWoodcuttingAreFiledByTree()
	{
		Map<String, Long> delta = new HashMap<>();
		delta.put("Yew logs", 1L);
		List<Flow> flows = Classify.tick(delta, new Classify.Context().xp(EnumSet.of(Skill.WOODCUTTING)), new HashMap<>());
		assertEquals(1, flows.size());
		assertSame(Trees.byName("Yew tree"), Woodcuts.cut(flows.get(0)));
		assertNull(Woodcuts.cut(Flow.in("Yew logs", 1, Source.BOUGHT, null)));
		assertNull(Woodcuts.cut(Flow.in("Yew logs", 1, Source.LOOTED, null)));
	}

	@Test
	public void logsAnHourCountTheWaitForRegrowthButNotABankTrip()
	{
		Woodcuts w = new Woodcuts();
		// 31 yew logs 15 seconds apart: 7m30s.
		for (int i = 0; i <= 30; i++)
		{
			w.record(logs("Yew logs", 1), T + i * 15_000L);
		}
		// A yew grows back in 99 ticks: a 2m50s wait still counts (2m + 59.4s).
		long at = T + 30 * 15_000L + 170_000L;
		w.record(logs("Yew logs", 1), at);
		// Then a bank trip of five minutes does not.
		w.record(logs("Yew logs", 1), at + 300_000L);
		Woodcuts.Wood yew = w.wood(Trees.byName("Yew tree"));
		assertEquals(33, yew.getLogs());
		assertEquals(450_000 + 170_000, yew.getMs());
		assertEquals(33 / (620_000 / 3_600_000.0), w.perHour("Yew logs"), 1e-6);
		assertSame(Trees.byName("Yew tree"), w.latest());
		// An oak's regrowth is short: the same wait is a break there.
		w.record(logs("Oak logs", 1), T);
		w.record(logs("Oak logs", 1), T + 170_000L);
		assertEquals(0, w.wood(Trees.byName("Oak tree")).getMs());
	}

	@Test
	public void aPaceWaitsForFiveMinutes()
	{
		Woodcuts w = new Woodcuts();
		for (int i = 0; i < 10; i++)
		{
			w.record(logs("Willow logs", 1), T + i * 20_000L);
		}
		assertEquals(0, w.perHour("Willow logs"), 1e-9);
	}

	@Test
	public void fallsSeenWholeAreTimed()
	{
		Woodcuts w = new Woodcuts();
		Trees.Kind oak = Trees.byName("Oak tree");
		w.felled(oak, 44);
		w.felled(oak, 46);
		w.felled(oak, -1);
		Woodcuts.Wood o = w.wood(oak);
		assertEquals(3, o.getFelled());
		assertEquals(2, o.getTimedFalls());
		assertEquals(45, o.ticksToFall(), 1e-9);
		assertEquals(3, w.felled());
	}

	@Test
	public void theLogSavesWithTheAccountAndOldSavesReadWithout()
	{
		Gson gson = new Gson();
		Works works = new Works();
		for (int i = 0; i <= 30; i++)
		{
			works.rate(logs("Maple logs", 1), T + i * 12_000L);
		}
		works.woodcuts().felled(Trees.byName("Maple tree"), 99);
		Works back = Works.read(gson, works.write(gson), 99).works;
		assertEquals(31, back.woodcuts().wood(Trees.byName("Maple tree")).getLogs());
		assertEquals(works.woodcuts().perHour("Maple logs"), back.woodcuts().perHour("Maple logs"), 1e-9);
		assertEquals(1, back.woodcuts().felled());
		assertTrue(Works.read(gson, "{\"account\":2}", 99).works.woodcuts().isEmpty());
		Works damaged = Works.read(gson, "{\"woodcuts\":{\"woods\":{\"Elder tree\":{\"logs\":3},\"Oak tree\":{\"logs\":-4,\"timedFalls\":9}}}}", 99).works;
		assertTrue(damaged.woodcuts().isEmpty());
		assertEquals(0, damaged.woodcuts().wood(Trees.byName("Oak tree")).getTimedFalls());
	}
}
