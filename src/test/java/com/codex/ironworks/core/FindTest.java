package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class FindTest
{
	private static Map<Integer, Long> items(long... pairs)
	{
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((int) pairs[i], pairs[i + 1]);
		}
		return m;
	}

	@Test
	public void holdingsAndNamesSurviveASave()
	{
		Works w = new Works(0);
		Map<Integer, String> names = new HashMap<>();
		names.put(4151, "Abyssal whip");
		names.put(385, "Shark");
		w.hold(Spot.BANK, items(4151, 1, 385, 200), names, 10);
		Works back = Works.read(new Gson(), w.write(new Gson()), 20).works;
		assertEquals(200L, (long) back.holding(Spot.BANK).getItems().get(385));
		assertEquals("Abyssal whip", back.itemName(4151));
	}

	@Test
	public void namesOfItemsNoLongerHeldAreDropped()
	{
		Works w = new Works(0);
		w.hold(Spot.INVENTORY, items(385, 5), Collections.singletonMap(385, "Shark"), 1);
		w.hold(Spot.INVENTORY, items(), Collections.emptyMap(), 2);
		Works back = Works.read(new Gson(), w.write(new Gson()), 3).works;
		assertNull(back.itemName(385));
	}

	@Test
	public void deathWaitsAtTheGraveThenDeathsOfficeUntilTakenBack()
	{
		Works w = new Works(0);
		w.played(java.time.LocalDate.of(2026, 10, 1), 60_000);
		w.died(items(4151, 1, 385, 20), Collections.singletonMap(4151, "Abyssal whip"), 330, 100);
		Death d = w.getDeath();
		assertNotNull(d);
		assertEquals(Death.GRAVE_MS, d.graveLeft(w.getPlayed()));
		w.played(java.time.LocalDate.of(2026, 10, 1), Death.GRAVE_MS + 1);
		assertEquals(0, d.graveLeft(w.getPlayed()));
		assertTrue(w.regained(items(4151, 1)));
		assertNotNull(w.getDeath());
		w.regained(items(385, 19));
		assertNull("A tenth left is settled", w.getDeath());
	}

	@Test
	public void ratesCountOnlyTimeAtIt()
	{
		Works w = new Works(0);
		List<Flow> one = Collections.singletonList(Flow.in("Snape grass", 10, Source.GATHERED, "Farming"));
		long t = 1_700_000_000_000L;
		for (int i = 0; i < 40; i++)
		{
			w.rate(one, t);
			t += 15_000;
		}
		t += 3_600_000;
		w.rate(one, t);
		Rate r = w.rate("Snape grass", Source.GATHERED);
		assertEquals(410, r.getUnits());
		assertEquals(39 * 15_000L, r.getMs());
		assertEquals(410 / (39 * 15_000 / 3_600_000.0), r.perHour(), 0.01);
	}

	@Test
	public void everyWayIsForAStore()
	{
		Catalogue.Lookup lookup = Catalogue.standard();
		for (String store : Catalogue.all())
		{
			for (Ways.Way way : Ways.of(store))
			{
				assertTrue(store, way.text.length() > 5 && !way.text.endsWith("."));
			}
		}
		assertTrue(!Ways.of("Snape grass").isEmpty());
		assertTrue("Herbs fall back to their shelf", !Ways.of("Kwuarm").isEmpty());
		assertNotNull(lookup.match("Snape grass"));
	}
}
