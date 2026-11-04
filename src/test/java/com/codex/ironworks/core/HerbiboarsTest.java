package com.codex.ironworks.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;

/** The herbiboar's facts as the wiki gives them, and the player's own log of harvests and trails. */
public class HerbiboarsTest
{
	private static final long T = 2_000_000_000L;
	private static final long TRAIL = 60_000L;

	private static void harvest(Herbiboars log, long at, int herblore, Object... herbs)
	{
		Flow[] flows = new Flow[herbs.length / 2];
		for (int i = 0; i < herbs.length; i += 2)
		{
			flows[i / 2] = Flow.in((String) herbs[i], ((Number) herbs[i + 1]).longValue(), Source.GATHERED, "Hunter");
		}
		log.record(Arrays.asList(flows), true, herblore, at);
		log.record(Collections.emptyList(), true, herblore, at + 10_000);
	}

	@Test
	public void herbsDropAtTheWikisHerbloreRanges()
	{
		assertEquals(80, Herbiboar.HUNTER);
		assertEquals(31, Herbiboar.HERBLORE);
		assertEquals(25, Herbiboar.XP_PER_EXTRA_HERB);
		assertTrue(Herbiboar.drops("Marrentill", 80));
		assertFalse(Herbiboar.drops("Marrentill", 81));
		assertTrue(Herbiboar.drops("Tarromin", 77));
		assertFalse(Herbiboar.drops("Tarromin", 78));
		assertTrue(Herbiboar.drops("Harralander", 76));
		assertFalse(Herbiboar.drops("Harralander", 77));
		assertFalse(Herbiboar.drops("Lantadyme", 40));
		assertTrue(Herbiboar.drops("Lantadyme", 41));
		assertFalse(Herbiboar.drops("Dwarf weed", 61));
		assertTrue(Herbiboar.drops("Dwarf weed", 62));
		assertFalse(Herbiboar.drops("Snapdragon", 73));
		assertTrue(Herbiboar.drops("Snapdragon", 74));
		assertFalse(Herbiboar.drops("Torstol", 76));
		assertTrue(Herbiboar.drops("Torstol", 77));
		assertTrue(Herbiboar.drops("Ranarr weed", 99));
		assertTrue(Herbiboar.drops("Guam leaf", 99));
		// Nothing below the level to harvest at all.
		assertFalse(Herbiboar.drops("Ranarr weed", 30));
		// "Grimy toadflax and Grimy huasca are the only herbs that are not dropped at all."
		assertNull(Herbiboar.range("Toadflax"));
		assertNull(Herbiboar.range("Huasca"));
		assertEquals(13, Herbiboar.herbs().size());
	}

	@Test
	public void everyHerbIsInTheCatalogueWithItsGrimyItem()
	{
		Set<String> all = Catalogue.all();
		for (String herb : Herbiboar.herbs())
		{
			assertTrue(herb, all.contains(herb));
			assertEquals(herb, Family.HERBS, Catalogue.family(herb));
		}
		assertEquals(ItemID.UNIDENTIFIED_RANARR, Herbiboar.range("Ranarr weed").grimy);
		assertEquals(207, Herbiboar.range("Ranarr weed").grimy);
		assertEquals(219, Herbiboar.range("Torstol").grimy);
		assertEquals(3051, Herbiboar.range("Snapdragon").grimy);
		assertArrayEquals(new int[]{7785, 7786}, new int[]{NpcID.FOSSIL_HERBIBOAR_VISIBLE, NpcID.FOSSIL_HERBIBOAR_ANIM_VISIBLE});
	}

	@Test
	public void bandsBreakWhereAHerbStartsOrStops()
	{
		assertArrayEquals(new int[]{31, 40}, Herbiboar.band(31));
		assertArrayEquals(new int[]{41, 61}, Herbiboar.band(55));
		assertArrayEquals(new int[]{62, 73}, Herbiboar.band(73));
		assertArrayEquals(new int[]{74, 76}, Herbiboar.band(74));
		assertArrayEquals(new int[]{77, 77}, Herbiboar.band(77));
		assertArrayEquals(new int[]{78, 80}, Herbiboar.band(80));
		assertArrayEquals(new int[]{81, 99}, Herbiboar.band(99));
		assertEquals("41-61", Herbiboar.bandKey(50));
	}

	@Test
	public void groundIsTheHerbiboarsMapSquares()
	{
		assertTrue(Herbiboar.ground(new int[]{14396, 14652}));
		assertFalse(Herbiboar.ground(new int[]{12850}));
		assertFalse(Herbiboar.ground(null));
	}

	@Test
	public void harvestsAreLoggedByKindAndTimedHarvestToHarvest()
	{
		Herbiboars log = new Herbiboars();
		harvest(log, T, 70, "Ranarr weed", 2, "Irit leaf", 1);
		harvest(log, T + TRAIL, 70, "Ranarr weed", 1);
		assertEquals(2, log.getHarvests());
		assertEquals(1, log.getTimed());
		// Not enough timed yet.
		assertEquals(0, log.trailsPerHour(), 1e-9);
		assertEquals(0, log.perHour("Ranarr weed"), 1e-9);
		harvest(log, T + 2 * TRAIL, 70, "Avantoe", 2);
		harvest(log, T + 3 * TRAIL, 70, "Ranarr weed", 1, "Dwarf weed", 2);
		assertEquals(4, log.getHarvests());
		assertEquals(9, log.getHerbs());
		assertEquals(3, log.getTimed());
		assertEquals(60, log.trailsPerHour(), 1e-6);
		assertEquals(60_000, log.trailMs());
		Herbiboars.Band b = log.band("62-73");
		assertEquals(4, b.getHarvests());
		assertEquals(4 / 4.0, b.perHarvest("Ranarr weed"), 1e-9);
		// Ranarr: 4 in 4 harvests, at 60 trails an hour.
		assertEquals(60, log.perHour("Ranarr weed"), 1e-6);
		assertEquals(30, log.perHour("Avantoe"), 1e-6);
		assertEquals(9 / 4.0, log.perHarvest(), 1e-9);
	}

	@Test
	public void aBreakIsNotTimedAndHerbsOverTwoTicksAreOneHarvest()
	{
		Herbiboars log = new Herbiboars();
		log.record(Collections.singletonList(Flow.in("Ranarr weed", 1, Source.GATHERED, "Hunter")), true, 50, T);
		log.record(Collections.singletonList(Flow.in("Irit leaf", 1, Source.OTHER, null)), true, 50, T + 600);
		log.record(Collections.emptyList(), true, 50, T + 10_000);
		assertEquals(1, log.getHarvests());
		assertEquals(2, log.getHerbs());
		harvest(log, T + Herbiboars.ROUND_MS + 1, 50, "Guam leaf", 1);
		assertEquals(2, log.getHarvests());
		assertEquals(0, log.getTimed());
	}

	@Test
	public void onlyHerbsGivenOnTheHuntingGroundCount()
	{
		Herbiboars log = new Herbiboars();
		// Looted herbs (a zygomite's drop), herbs farmed or carried in elsewhere, and seeds are not harvests.
		log.record(Collections.singletonList(Flow.in("Ranarr weed", 1, Source.LOOTED, null)), true, 50, T);
		log.record(Collections.singletonList(Flow.in("Ranarr weed", 6, Source.FARMED, "Farming")), true, 50, T + 20_000);
		log.record(Collections.singletonList(Flow.in("Ranarr seed", 1, Source.GATHERED, "Hunter")), true, 50, T + 40_000);
		log.record(Collections.singletonList(Flow.in("Ranarr weed", 2, Source.GATHERED, "Hunter")), false, 50, T + 60_000);
		log.record(Collections.emptyList(), true, 50, T + 90_000);
		assertTrue(log.isEmpty());
	}

	@Test
	public void herbloreLevelsKeepTheirOwnMix()
	{
		Herbiboars log = new Herbiboars();
		harvest(log, T, 60, "Guam leaf", 3);
		harvest(log, T + TRAIL, 62, "Dwarf weed", 1);
		assertEquals("62-73", log.currentBand());
		assertEquals(1, log.band("41-61").getHarvests());
		assertEquals(1, log.band("62-73").getHarvests());
		assertTrue(log.gave("Guam leaf"));
		assertFalse(log.gave("Torstol"));
	}

	@Test
	public void theLogSavesWithTheAccountAndOldSavesReadWithout()
	{
		Gson gson = new Gson();
		Works w = new Works();
		for (int i = 0; i < 4; i++)
		{
			w.rate(Collections.singletonList(Flow.in("Ranarr weed", 2, Source.GATHERED, "Hunter")), T + i * TRAIL, true, 80);
			w.rate(Collections.emptyList(), T + i * TRAIL + 10_000, true, 80);
		}
		assertEquals(4, w.herbiboars().getHarvests());
		Works back = Works.read(gson, w.write(gson), 99).works;
		assertEquals(4, back.herbiboars().getHarvests());
		assertEquals(3, back.herbiboars().getTimed());
		assertEquals(120, back.herbiboars().perHour("Ranarr weed"), 1e-6);
		Works old = Works.read(gson, "{\"account\":2}", 99).works;
		assertTrue(old.herbiboars().isEmpty());
		Works damaged = Works.read(gson, "{\"herbiboars\":{\"harvests\":-3,\"bands\":{\"31-40\":null},\"recent\":[null]}}", 99).works;
		assertEquals(0, damaged.herbiboars().getHarvests());
		assertTrue(damaged.herbiboars().bands().isEmpty());
	}

	@Test
	public void aHarvestWithHerbloreExperienceIsStillGatheredByHunter()
	{
		Map<String, Long> delta = new HashMap<>();
		delta.put("Ranarr weed", 3L);
		java.util.List<Flow> flows = Classify.tick(delta,
			new Classify.Context().xp(java.util.EnumSet.of(Skill.HUNTER, Skill.HERBLORE)), new HashMap<>());
		assertEquals(1, flows.size());
		assertEquals(Source.GATHERED, flows.get(0).source);
		assertEquals("Hunter", flows.get(0).skill);
		assertTrue(Herbiboars.harvested(flows.get(0)));
	}

	@Test
	public void waysAndLeadsFollowTheHerbloreRange()
	{
		boolean hunted = false;
		for (Ways.Way w : Ways.of("Marrentill"))
		{
			if (w.herblore != null)
			{
				hunted = true;
				assertEquals(Skill.HUNTER, w.skill);
				assertEquals(80, w.level);
				assertTrue(w.gives(80));
				assertFalse(w.gives(81));
				assertTrue(w.text.contains("until 81 Herblore"));
			}
		}
		assertTrue(hunted);
		for (Ways.Way w : Ways.of("Huasca"))
		{
			assertNull(w.herblore);
		}
		for (Ways.Way w : Ways.of("Toadflax"))
		{
			assertNull(w.herblore);
		}
		// Torstol keeps its herb runs and gains the herbiboar from 77 Herblore.
		int n = 0;
		for (Ways.Way w : Ways.of("Torstol"))
		{
			if (w.herblore != null)
			{
				n++;
				assertFalse(w.herbloreOpen(76));
				assertTrue(w.herbloreOpen(77));
			}
		}
		assertEquals(1, n);
		assertTrue(Ways.of("Torstol").size() >= 2);

		Map<Skill, Integer> levels = new java.util.EnumMap<>(Skill.class);
		levels.put(Skill.HUNTER, 85);
		levels.put(Skill.HERBLORE, 70);
		Acquire.Path torstol = hunt(Acquire.rank("Torstol", 10, levels, null, null, null, null, 0));
		assertEquals("Herblore 77", torstol.locked);
		assertEquals("Bone Voyage", torstol.needs);
		assertEquals(3686, torstol.x);
		Acquire.Path ranarr = hunt(Acquire.rank("Ranarr weed", 10, levels, (s, src) -> 60, null, null, null, 0));
		assertNull(ranarr.locked);
		assertEquals("your pace", ranarr.basis);
		levels.put(Skill.HERBLORE, 85);
		assertNull(hunt(Acquire.rank("Marrentill", 10, levels, null, null, null, null, 0)));
	}

	private static Acquire.Path hunt(java.util.List<Acquire.Path> paths)
	{
		for (Acquire.Path p : paths)
		{
			if (p.text.startsWith("Hunt the herbiboar"))
			{
				return p;
			}
		}
		return null;
	}
}
