package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The fish table against the wiki, the tools read from animations, and the fishing log's timing and save. */
public class FishingTest
{
	private static final long NOW = 2_000_000_000L;

	private static void check(String store, int item, int level, double xp, Fish.Method m)
	{
		Fish.Kind k = Fish.byStore(store);
		assertEquals(store, item, k.item);
		assertEquals(store, level, k.level);
		assertEquals(store, xp, k.xp, 1e-9);
		assertSame(store, m, k.method);
		assertSame(k, Fish.byItem(item));
	}

	@Test
	public void everyFishMatchesItsWikiPage()
	{
		// Each fish page's "Level required" and "Fishing XP"; the aerial fish from Aerial fishing.
		check("Raw shrimps", ItemID.RAW_SHRIMP, 1, 10, Fish.SMALL_NET);
		check("Raw anchovies", ItemID.RAW_ANCHOVIES, 15, 40, Fish.SMALL_NET);
		check("Raw sardine", ItemID.RAW_SARDINE, 5, 20, Fish.SEA_BAIT);
		check("Raw herring", ItemID.RAW_HERRING, 10, 30, Fish.SEA_BAIT);
		check("Raw trout", ItemID.RAW_TROUT, 20, 50, Fish.FLY);
		check("Raw salmon", ItemID.RAW_SALMON, 30, 70, Fish.FLY);
		check("Raw pike", ItemID.RAW_PIKE, 25, 60, Fish.RIVER_BAIT);
		check("Raw mackerel", ItemID.RAW_MACKEREL, 16, 20, Fish.BIG_NET);
		check("Raw cod", ItemID.RAW_COD, 23, 45, Fish.BIG_NET);
		check("Raw bass", ItemID.RAW_BASS, 46, 100, Fish.BIG_NET);
		check("Raw lobster", ItemID.RAW_LOBSTER, 40, 90, Fish.CAGE);
		check("Raw tuna", ItemID.RAW_TUNA, 35, 80, Fish.HARPOON);
		check("Raw swordfish", ItemID.RAW_SWORDFISH, 50, 100, Fish.HARPOON);
		check("Raw shark", ItemID.RAW_SHARK, 76, 110, Fish.SHARK_HARPOON);
		check("Raw monkfish", ItemID.RAW_MONKFISH, 62, 120, Fish.MONKFISH);
		check("Raw karambwan", ItemID.TBWT_RAW_KARAMBWAN, 65, 50, Fish.KARAMBWAN);
		check("Raw anglerfish", ItemID.RAW_ANGLERFISH, 82, 120, Fish.ANGLERFISH);
		check("Raw dark crab", ItemID.RAW_DARK_CRAB, 85, 130, Fish.DARK_CRAB);
		check("Leaping trout", ItemID.BRUT_SPAWNING_TROUT, 48, 50, Fish.BARBARIAN);
		check("Leaping salmon", ItemID.BRUT_SPAWNING_SALMON, 58, 70, Fish.BARBARIAN);
		check("Leaping sturgeon", ItemID.BRUT_STURGEON, 70, 80, Fish.BARBARIAN);
		check("Bluegill", ItemID.AERIAL_FISHING_BLUEGILL, 43, 11.5, Fish.AERIAL);
		check("Common tench", ItemID.AERIAL_FISHING_COMMON_TENCH, 56, 40, Fish.AERIAL);
		check("Mottled eel", ItemID.AERIAL_FISHING_MOTTLED_EEL, 73, 65, Fish.AERIAL);
		check("Greater siren", ItemID.AERIAL_FISHING_GREATER_SIREN, 91, 100, Fish.AERIAL);
		assertEquals(25, Fish.all().size());
		assertEquals(87, Fish.byStore("Greater siren").hunter);
		// Trawled fish are logged apart.
		assertNull(Fish.byStore("Raw haddock"));
		assertNull(Fish.byItem(ItemID.RAW_GIANT_KRILL));
	}

	@Test
	public void wikiFiguresForSpotsPearlsAndTheShop()
	{
		assertEquals(250, Fish.Stay.MOST.minTicks);
		assertEquals(530, Fish.Stay.MOST.maxTicks);
		assertEquals(100, Fish.Stay.EELS.minTicks);
		assertEquals(300, Fish.Stay.EELS.maxTicks);
		assertEquals(8, Fish.Stay.ANGLERFISH.minTicks);
		assertEquals(830, Fish.Stay.ANGLERFISH.maxTicks);
		assertEquals(100, Fish.PearlGoal.PEARL_FISHING_ROD.price);
		assertEquals(120, Fish.PearlGoal.PEARL_FLY_FISHING_ROD.price);
		assertEquals(150, Fish.PearlGoal.PEARL_BARBARIAN_ROD.price);
		assertEquals(200, Fish.PearlGoal.ANGLER_OUTFIT.price);
		assertEquals(500, Fish.PearlGoal.FISH_SACK.price);
		assertEquals(1_000, (int) Fish.BIG_FISH_ONE_IN.get("Big bass"));
		assertEquals(2_500, (int) Fish.BIG_FISH_ONE_IN.get("Big swordfish"));
		assertEquals(5_000, (int) Fish.BIG_FISH_ONE_IN.get("Big shark"));
		assertEquals("Clue bottle (elite)", Fish.extra(ItemID.FISHING_CLUE_BOTTLE_ELITE));
		assertEquals("Golden tench", Fish.extra(ItemID.GOLDEN_TENCH));
		assertTrue(Fish.isClue("Clue bottle (beginner)"));
		assertFalse(Fish.isClue("Casket"));
		assertEquals(ItemID.POH_TROPHYDROP_SHARK, Fish.extraItem("Big shark"));
	}

	@Test
	public void theToolIsReadFromTheAnimationAndSuitsTheMethod()
	{
		assertEquals("Dragon harpoon", Fish.tool(AnimationID.HUMAN_HARPOON_DRAGON).name);
		assertEquals("Infernal harpoon", Fish.tool(AnimationID.HUMAN_HARPOON_TRAILBLAZER).name);
		assertEquals("Bare hands", Fish.tool(AnimationID.BRUT_PLAYER_HAND_FISHING_END_TUNA_1).name);
		assertEquals("Lobster pot", Fish.tool(AnimationID.HUMAN_LOBSTER).name);
		assertNull(Fish.tool(AnimationID.HUMAN_MINING_RUNE_PICKAXE));
		assertEquals("Dragon harpoon", Fish.toolFor(Fish.HARPOON, Fish.tool(AnimationID.HUMAN_HARPOON_DRAGON)));
		assertEquals("Bare hands", Fish.toolFor(Fish.SHARK_HARPOON, Fish.tool(AnimationID.BRUT_PLAYER_HAND_FISHING_READY)));
		// A plain rod cast for trout is the fly fishing rod; for pike it is the fishing rod.
		assertEquals("Fly fishing rod", Fish.toolFor(Fish.FLY, Fish.tool(AnimationID.HUMAN_FISHING_CASTING)));
		assertEquals("Fishing rod", Fish.toolFor(Fish.RIVER_BAIT, Fish.tool(AnimationID.HUMAN_FISHING_CASTING)));
		assertEquals("Pearl fly fishing rod", Fish.toolFor(Fish.FLY, Fish.tool(AnimationID.HUMAN_FISHING_CASTING_PEARL_FLY)));
		// Aerial fish show no tool; a stale harpoon is not their tool.
		assertEquals("Cormorant", Fish.toolFor(Fish.AERIAL, Fish.tool(AnimationID.HUMAN_HARPOON)));
		assertEquals("Karambwan vessel", Fish.toolFor(Fish.KARAMBWAN, null));
	}

	@Test
	public void catchMessagesNameTheFish()
	{
		assertEquals("Raw lobster", Fish.fromMessage("You catch a lobster.").store);
		assertEquals("Raw shrimps", Fish.fromMessage("You catch some shrimps.").store);
		assertEquals("Raw anchovies", Fish.fromMessage("You catch some anchovies.").store);
		assertEquals("Leaping trout", Fish.fromMessage("You catch a leaping trout.").store);
		assertEquals("Raw trout", Fish.fromMessage("You catch a trout.").store);
		assertNull(Fish.fromMessage("You catch a haddock!"));
		assertNull(Fish.fromMessage("Your cormorant returns with its catch."));
		assertNull(Fish.fromMessage(null));
	}

	@Test
	public void catchesAreTimedCatchToCatch()
	{
		Fishing log = new Fishing();
		Fish.Kind tuna = Fish.byStore("Raw tuna");
		Fish.Kind sword = Fish.byStore("Raw swordfish");
		for (int i = 0; i <= 60; i++)
		{
			log.caught(i % 3 == 0 ? sword : tuna, "Dragon harpoon", 1, NOW + i * 10_000L);
		}
		Fishing.Spot s = log.spots().get("Harpoon|Dragon harpoon");
		assertEquals(600_000L, s.getMs());
		assertEquals(61, s.count());
		assertEquals(21, (long) s.getFish().get("Raw swordfish"));
		assertEquals(366, s.perHour(), 1e-9);
		assertEquals((40 * 80 + 21 * 100) * 6, s.xpPerHour(), 1e-9);
		assertEquals(240, log.perHour("Raw tuna"), 1e-9);
		// A bank trip is not timed.
		log.caught(tuna, "Dragon harpoon", 1, NOW + 600_000L + 3 * 60_000L);
		assertEquals(600_000L, s.getMs());
		assertTrue(log.tried(Fish.HARPOON));
		assertFalse(log.tried(Fish.CAGE));
		assertEquals("Harpoon|Dragon harpoon", log.latest());
		assertSame(Fish.HARPOON, Fishing.methodOf(log.latest()));
		assertEquals("Dragon harpoon", Fishing.toolOf(log.latest()));
		// Too little time to tell: no pace.
		Fishing short_ = new Fishing();
		short_.caught(tuna, null, 1, NOW);
		short_.caught(tuna, null, 1, NOW + 60_000L);
		assertEquals(0, short_.perHour("Raw tuna"), 1e-9);
		assertNull(Fishing.toolOf(short_.latest()));
	}

	@Test
	public void pearlsAreCountedAgainstAerialFishing()
	{
		Fishing log = new Fishing();
		Fish.Kind tench = Fish.byStore("Common tench");
		for (int i = 0; i <= 200; i++)
		{
			log.caught(tench, "Cormorant", 1, NOW + i * 3_000L);
		}
		log.found(Fish.PEARL_NAME, 4);
		assertEquals(201, log.aerialCatches());
		assertEquals(600_000L, log.aerialMs());
		assertEquals(24, log.pearlsPerHour(), 1e-9);
	}

	@Test
	public void theLogIsSavedAndADamagedOneIsSetRight()
	{
		Works w = new Works(1);
		w.fishing().caught(Fish.byStore("Raw lobster"), "Lobster pot", 3, NOW);
		w.fishing().found("Casket", 1);
		w.fishing().stayed("Cage/Harpoon", 300);
		Gson gson = new Gson();
		String json = w.write(gson);
		Works back = Works.read(gson, json, 9).works;
		assertEquals(3, back.fishing().caught("Raw lobster"));
		assertEquals(1L, (long) back.fishing().found().get("Casket"));
		assertEquals(300, back.fishing().stays().get("Cage/Harpoon").average(), 1e-9);
		assertEquals(json, back.write(gson));
		// An older save has no fishing at all.
		Works old = Works.read(gson, json.replaceAll(",\"fishing\":\\{.*\\}\\}\\s*$", "}"), 9).works;
		assertTrue(old.fishing().isEmpty());
		// A way of fishing or a fish it no longer knows is dropped, not trusted.
		Works bad = Works.read(gson, json.replace("Cage|Lobster pot", "Gill net|Hands").replace("\"Raw lobster\":3", "\"Raw lobster\":-3"), 9).works;
		assertTrue(bad.fishing().spots().isEmpty());
		assertEquals(1L, (long) bad.fishing().found().get("Casket"));
	}
}
