package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The trawling log, the fish facts it stands on, and reading catch messages. */
public class TrawlsTest
{
	private static final long T = 1_000_000_000L;
	private static final Shoals.Kind HADDOCK = Shoals.byStore("Raw haddock");
	private static final Shoals.Kind HALIBUT = Shoals.byStore("Raw halibut");

	private static Map<Shoals.Kind, Long> of(Shoals.Kind k, long n)
	{
		return Collections.singletonMap(k, n);
	}

	@Test
	public void theFishAreTheWikis()
	{
		// OSRS Wiki, each raw fish's page: level and experience a catch with a trawling net.
		Object[][] facts = {
			{"Raw giant krill", ItemID.RAW_GIANT_KRILL, 69, 112.5, "Giant krill shoal"},
			{"Raw haddock", ItemID.RAW_HADDOCK, 73, 128.5, "Haddock shoal"},
			{"Raw yellowfin", ItemID.RAW_YELLOWFIN, 79, 155.5, "Yellowfin shoal"},
			{"Raw halibut", ItemID.RAW_HALIBUT, 83, 195.5, "Halibut shoal"},
			{"Raw bluefin", ItemID.RAW_BLUEFIN, 87, 220.5, "Bluefin shoal"},
			{"Raw marlin", ItemID.RAW_MARLIN, 91, 265.5, "Marlin shoal"},
		};
		assertEquals(facts.length, Shoals.all().size());
		for (Object[] f : facts)
		{
			Shoals.Kind k = Shoals.byStore((String) f[0]);
			assertEquals(f[1], k.item);
			assertEquals(f[2], k.level);
			assertEquals((double) f[3], k.xp, 1e-9);
			assertEquals(f[4], k.shoal);
			assertSame(k, Shoals.byItem(k.item));
			// Every trawled fish is a store Ironworks already lists a way to get, at the same level.
			Ways.Way w = Ways.of(k.fish).get(0);
			assertEquals(k.level, w.level);
			assertTrue(w.text.startsWith("Trawl "));
		}
		// The item ids are the ones the wiki gives.
		assertEquals(32309, ItemID.RAW_GIANT_KRILL);
		assertEquals(32317, ItemID.RAW_HADDOCK);
		assertEquals(32325, ItemID.RAW_YELLOWFIN);
		assertEquals(32333, ItemID.RAW_HALIBUT);
		assertEquals(32341, ItemID.RAW_BLUEFIN);
		assertEquals(32349, ItemID.RAW_MARLIN);
	}

	@Test
	public void catchMessagesAreRead()
	{
		Trawls.Catch c = Trawls.parse("You catch two haddock!");
		assertSame(HADDOCK, c.kind);
		assertEquals(2, c.units);
		c = Trawls.parse("You catch a giant krill!");
		assertSame(Shoals.byStore("Raw giant krill"), c.kind);
		assertEquals(1, c.units);
		c = Trawls.parse("Jobless Jim catches five halibut!");
		assertSame(HALIBUT, c.kind);
		assertEquals(5, c.units);
		assertEquals(3, Trawls.parse("You catch 3 raw marlin.").units);
		// Rare fish, other fish and anything else are not trawled catches of these.
		assertNull(Trawls.parse("You catch a golden haddock!"));
		assertNull(Trawls.parse("You catch a giant blue krill!"));
		assertNull(Trawls.parse("You catch a swift marlin!"));
		assertNull(Trawls.parse("You catch a lobster."));
		assertNull(Trawls.parse("You catch some shrimps."));
		assertNull(Trawls.parse("Your nets are too full to hold any more fish"));
		assertNull(Trawls.parse(null));
	}

	@Test
	public void catchesAreTimedCatchToCatch()
	{
		Trawls log = new Trawls();
		// A catch every 6 seconds for ten minutes: 2 haddock, 257 Fishing and 30 Sailing experience each.
		for (int i = 0; i <= 100; i++)
		{
			log.caught(of(HADDOCK, 2), 257, 30, T + i * 6_000L);
		}
		Trawls.Haul h = log.haul(HADDOCK);
		assertEquals(202, h.getFish());
		assertEquals(101, h.getCatches());
		assertEquals(600_000L, h.getMs());
		// The first catch starts the clock: its fish are counted, not timed; 200 fish in ten minutes.
		assertEquals(1200, h.perHour(), 1e-6);
		assertEquals(100 * 257 * 6, h.fishingPerHour(), 1e-6);
		assertEquals(100 * 30 * 6, h.sailingPerHour(), 1e-6);
		assertEquals(1200, log.perHour("Raw haddock"), 1e-6);
		// A long gap is a bank trip: counted, not timed.
		log.caught(of(HADDOCK, 3), 400, 50, T + 600_000L + 10 * 60_000L);
		assertEquals(205, log.haul(HADDOCK).getFish());
		assertEquals(600_000L, log.haul(HADDOCK).getMs());
		// A gap within three minutes, following the shoal to its next stop, is time at it.
		log.caught(of(HADDOCK, 1), 128.5, 15, T + 600_000L + 12 * 60_000L);
		assertEquals(720_000L, log.haul(HADDOCK).getMs());
	}

	@Test
	public void aMixedCatchSharesItsGap()
	{
		Trawls log = new Trawls();
		log.caught(of(HADDOCK, 1), 0, 0, T);
		Map<Shoals.Kind, Long> mixed = new HashMap<>();
		mixed.put(HADDOCK, 1L);
		mixed.put(HALIBUT, 3L);
		log.caught(mixed, 800, 40, T + 8_000L);
		assertEquals(2_000L, log.haul(HADDOCK).getMs());
		assertEquals(6_000L, log.haul(HALIBUT).getMs());
		assertEquals(5, log.fish());
		assertTrue(log.tried(HALIBUT));
		assertEquals(6_000L + 2_000L, log.all().getMs());
		// Nothing caught: nothing logged.
		log.caught(Collections.emptyMap(), 100, 100, T + 9_000L);
		assertEquals(5, log.fish());
	}

	@Test
	public void theLogSavesWithTheAccountAndOldSavesReadWithout()
	{
		Gson gson = new Gson();
		Works works = new Works();
		for (int i = 0; i <= 120; i++)
		{
			works.trawls().caught(of(HALIBUT, 3), 586.5, 45, T + i * 4_000L);
		}
		Works back = Works.read(gson, works.write(gson), 99).works;
		assertEquals(363, back.trawls().fish());
		assertEquals(works.trawls().perHour("Raw halibut"), back.trawls().perHour("Raw halibut"), 1e-9);
		assertEquals(works.trawls().haul(HALIBUT).fishingPerHour(), back.trawls().haul(HALIBUT).fishingPerHour(), 1e-9);
		assertTrue(Works.read(gson, "{\"account\":2}", 99).works.trawls().isEmpty());
		Works damaged = Works.read(gson,
			"{\"trawls\":{\"hauls\":{\"Raw shark\":{\"fish\":3},\"Raw haddock\":{\"fish\":-4,\"ms\":-1,\"fishingXp\":-3}},\"last\":-9}}",
			99).works;
		assertTrue(damaged.trawls().isEmpty());
		assertEquals(1, damaged.trawls().hauls().size());
		assertEquals(0, damaged.trawls().haul(HADDOCK).getMs());
	}
}
