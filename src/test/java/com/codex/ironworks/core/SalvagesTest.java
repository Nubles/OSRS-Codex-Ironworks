package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The salvaging log and the shipwreck facts it stands on. */
public class SalvagesTest
{
	private static final long T = 1_000_000_000L;
	private static final Wrecks.Kind LARGE = Wrecks.bySalvage("Large salvage");
	private static final Wrecks.Kind PIRATE = Wrecks.bySalvage("Plundered salvage");

	@Test
	public void theWrecksAreTheWikis()
	{
		// OSRS Wiki, Shipwreck salvaging table, and each salvage's page for its item id and sorting xp.
		Object[][] facts = {
			{"Small shipwreck", "Small salvage", 32847, 15, 10.0, 5.5, 60},
			{"Fisherman's shipwreck", "Fishy salvage", 32849, 26, 17.0, 9.0, 180},
			{"Barracuda shipwreck", "Barracuda salvage", 32851, 35, 31.0, 15.5, 180},
			{"Large shipwreck", "Large salvage", 32853, 53, 48.0, 24.0, 180},
			{"Pirate shipwreck", "Plundered salvage", 32855, 64, 76.0, 31.5, 180},
			{"Mercenary shipwreck", "Martial salvage", 32857, 73, 138.0, 63.5, 195},
			{"Fremennik shipwreck", "Fremennik salvage", 32859, 80, 162.0, 75.0, 220},
			{"Merchant shipwreck", "Opulent salvage", 32861, 87, 200.0, 95.0, 240},
		};
		assertEquals(facts.length, Wrecks.all().size());
		for (Object[] f : facts)
		{
			Wrecks.Kind k = Wrecks.bySalvage((String) f[1]);
			assertEquals(f[0], k.wreck);
			assertEquals(f[2], k.item);
			assertEquals(f[3], k.level);
			assertEquals((double) f[4], k.xp, 1e-9);
			assertEquals((double) f[5], k.sortXp, 1e-9);
			assertEquals(f[6], k.lastsSeconds);
			assertSame(k, Wrecks.byItem(k.item));
			// Every salvage is a store Ironworks already lists a way to get, at the same level.
			Ways.Way w = Ways.of(k.salvage).get(0);
			assertEquals(k.level, w.level);
			assertEquals(Skill.SAILING, w.skill);
			// And the Make tab sorts it for the same experience at the same level.
			Recipe sort = null;
			for (Recipe r : Recipes.using(k.salvage))
			{
				if (r.skill == Skill.SAILING)
				{
					sort = r;
				}
			}
			assertEquals(k.level, sort.level);
			assertEquals(k.sortXp, sort.xp, 1e-6);
		}
		assertEquals(32847, ItemID.SAILING_SMALL_SHIPWRECK_SALVAGE);
		assertEquals(32861, ItemID.SAILING_MERCHANT_SHIPWRECK_SALVAGE);
		assertNull(Wrecks.byItem(ItemID.COINS));
		// Cargo hold sizes, OSRS Wiki, Cargo hold "Capacity limits".
		for (int s : new int[]{20, 30, 40, 45, 60, 80, 90, 105, 120, 150, 160, 180, 210, 240})
		{
			assertTrue(Wrecks.holdSize(s));
		}
		assertFalse(Wrecks.holdSize(28));
		assertFalse(Wrecks.holdSize(0));
	}

	@Test
	public void salvageIsTimedSalvageToSalvageAndAGapIsNot()
	{
		Salvages log = new Salvages();
		// A large salvage every 12 seconds for ten minutes, with the hook's 48 xp.
		for (int i = 0; i <= 50; i++)
		{
			log.salvaged(LARGE, 1, 48, T + i * 12_000L);
		}
		Salvages.Wreck w = log.wreck("Large salvage");
		assertEquals(51, w.getSalvage());
		assertEquals(600_000L, w.getMs());
		assertEquals(300, w.perHour(), 1e-6);
		assertEquals(300 * 48, w.xpPerHour(), 1e-6);
		assertEquals(12, w.secondsEach(), 1e-9);
		// A crewmate's roll into the hold: experience and time, no salvage.
		log.salvaged(LARGE, 0, 20, T + 612_000L);
		assertEquals(51, w.getSalvage());
		assertEquals(612_000L, w.getMs());
		// After a bank trip the first salvage is counted but not timed.
		log.salvaged(LARGE, 1, 48, T + 2_000_000L);
		assertEquals(52, w.getSalvage());
		assertEquals(612_000L, w.getMs());
		assertSame(LARGE, log.working(T + 2_000_000L));
		assertNull(log.working(T + 2_000_000L + Salvages.GAP_MS + 1));
		// The crewmate's time counts at the wreck: 50 timed salvage over 612 seconds.
		assertEquals(50 / (612 / 3600.0), log.perHour("Large salvage"), 1e-6);
		assertEquals(0, log.perHour("Opulent salvage"), 1e-9);
		// Pace waits for enough time.
		Salvages fresh = new Salvages();
		fresh.salvaged(PIRATE, 1, 76, T);
		fresh.salvaged(PIRATE, 1, 76, T + 10_000L);
		assertEquals(0, fresh.wreck("Plundered salvage").perHour(), 1e-9);
	}

	@Test
	public void sinkingWrecksAreCountedToTheWreckBeingWorked()
	{
		Salvages log = new Salvages();
		log.sunk(T);
		assertTrue(log.isEmpty());
		log.salvaged(PIRATE, 1, 76, T);
		log.sunk(T + 60_000L);
		log.sunk(T + 120_000L);
		assertEquals(2, log.wreck("Plundered salvage").getSunk());
		log.sunk(T + 10 * 60_000L);
		assertEquals(2, log.wreck("Plundered salvage").getSunk());
	}

	@Test
	public void sortingKeepsWhatEachWreckGave()
	{
		Salvages log = new Salvages();
		Map<Integer, Long> got = new HashMap<>();
		got.put(ItemID.COINS, 120L);
		got.put(ItemID.STEEL_BAR, 1L);
		Map<Integer, String> names = new HashMap<>();
		names.put(ItemID.COINS, "Coins");
		names.put(ItemID.STEEL_BAR, "Steel bar");
		log.sorted(LARGE, 1, 24, got, names);
		log.sorted(LARGE, 1, 24, Collections.singletonMap(ItemID.COINS, 80L), names);
		// Salvage itself never counts as loot; a sort that kept nothing is marked so.
		log.sorted(LARGE, 1, 24, Collections.singletonMap(LARGE.item, 1L), names);
		Salvages.Wreck w = log.wreck("Large salvage");
		assertEquals(3, w.getSorted());
		assertEquals(72, w.getSortXp(), 1e-9);
		assertEquals(1, w.getSortedBare());
		assertEquals(200L, (long) w.getLoot().get(ItemID.COINS));
		assertEquals(1L, (long) w.getLoot().get(ItemID.STEEL_BAR));
		assertEquals(2, w.getLoot().size());
		assertEquals("Steel bar", log.name(ItemID.STEEL_BAR));
		assertEquals(3, log.sorted());
		assertEquals(0, log.salvage());
		assertFalse(log.isEmpty());
	}

	@Test
	public void theHoldIsKeptAsLastSeen()
	{
		Salvages log = new Salvages();
		Map<Wrecks.Kind, Long> in = new HashMap<>();
		in.put(LARGE, 31L);
		log.hold(2, 39, 0, in, T);
		assertEquals(0, log.hold().getCapacity());
		log.holdSize(2, 160);
		assertEquals(160, log.hold().getCapacity());
		// A size no hold has is not taken, nor one for another boat.
		log.holdSize(2, 161);
		log.holdSize(1, 40);
		assertEquals(160, log.hold().getCapacity());
		// Seen again: the size stays with the boat.
		log.hold(2, 40, 0, in, T + 1000);
		assertEquals(160, log.hold().getCapacity());
		assertEquals(40, log.hold().getUsed());
		assertEquals(31L, (long) log.hold().getSalvage().get("Large salvage"));
		log.hold(1, 3, 0, Collections.emptyMap(), T + 2000);
		assertEquals(0, log.hold().getCapacity());
	}

	@Test
	public void theLogSavesWithTheAccountAndOldSavesReadWithout()
	{
		Gson gson = new Gson();
		Works works = new Works();
		for (int i = 0; i <= 60; i++)
		{
			works.salvages().salvaged(PIRATE, 1, 76, T + i * 10_000L);
		}
		works.salvages().sorted(PIRATE, 2, 63, Collections.singletonMap(ItemID.COINS, 300L), Collections.singletonMap(ItemID.COINS, "Coins"));
		works.salvages().hold(1, 12, 0, Collections.singletonMap(PIRATE, 9L), T);
		works.salvages().holdSize(1, 45);
		Works back = Works.read(gson, works.write(gson), 99).works;
		assertEquals(61, back.salvages().salvage());
		assertEquals(works.salvages().perHour("Plundered salvage"), back.salvages().perHour("Plundered salvage"), 1e-9);
		assertEquals(300L, (long) back.salvages().wreck("Plundered salvage").getLoot().get(ItemID.COINS));
		assertEquals("Coins", back.salvages().name(ItemID.COINS));
		assertEquals(45, back.salvages().hold().getCapacity());
		assertEquals(9L, (long) back.salvages().hold().getSalvage().get("Plundered salvage"));
		assertTrue(Works.read(gson, "{\"account\":2}", 99).works.salvages().isEmpty());
		Works damaged = Works.read(gson,
			"{\"salvages\":{\"wrecks\":{\"Shark salvage\":{\"salvage\":3},\"Large salvage\":{\"salvage\":-4,\"ms\":-1,\"xp\":-3,"
				+ "\"loot\":{\"995\":-2,\"2353\":4}}},\"working\":\"Nope\",\"hold\":{\"boat\":1,\"used\":-2,\"capacity\":161,"
				+ "\"salvage\":{\"Rocks\":3}}}}", 99).works;
		Salvages d = damaged.salvages();
		assertEquals(1, d.wrecks().size());
		assertEquals(0, d.wreck("Large salvage").getMs());
		assertEquals(0, d.wreck("Large salvage").getSalvage());
		assertEquals(1, d.wreck("Large salvage").getLoot().size());
		assertNull(d.working(T));
		assertEquals(0, d.hold().getCapacity());
		assertEquals(0, d.hold().getUsed());
		assertTrue(d.hold().getSalvage().isEmpty());
	}
}
