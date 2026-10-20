package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class ClassifyTest
{
	private final Map<String, Long> sacks = new HashMap<>();

	private static Map<String, Long> delta(Object... pairs)
	{
		Map<String, Long> m = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
		}
		return m;
	}

	private static Classify.Context xp(Skill... skills)
	{
		return new Classify.Context().xp(skills.length == 0 ? EnumSet.noneOf(Skill.class) : EnumSet.copyOf(Arrays.asList(skills)));
	}

	private static Classify.Action click(String option, String target, String store)
	{
		return new Classify.Action(option, target, store, false);
	}

	private Flow only(Map<String, Long> delta, Classify.Context ctx)
	{
		List<Flow> flows = Classify.tick(delta, ctx, sacks);
		assertEquals(flows.toString(), 1, flows.size());
		return flows.get(0);
	}

	@Test
	public void eatingAndDrinkingAreConsumed()
	{
		Flow eat = only(delta("Shark", -1), xp().actions(Collections.singletonList(click("Eat", "<col=ff9040>Shark", "Shark"))));
		assertEquals(Use.CONSUMED, eat.use);
		Flow sip = only(delta("Prayer potion", -1), xp().actions(Collections.singletonList(click("Drink", "Prayer potion(3)", "Prayer potion"))));
		assertEquals(Use.CONSUMED, sip.use);
		assertEquals(1, sip.units);
	}

	@Test
	public void herbloreTurnsInputsIntoAMadePotion()
	{
		List<Flow> flows = Classify.tick(delta("Ranarr potion (unf)", -1, "Snape grass", -1, "Prayer potion", 3), xp(Skill.HERBLORE), sacks);
		assertEquals(3, flows.size());
		for (Flow f : flows)
		{
			if (f.isGain())
			{
				assertEquals(Source.MADE, f.source);
				assertEquals("Herblore", f.skill);
				assertEquals(3, f.units);
			}
			else
			{
				assertEquals(Use.MADE_INTO, f.use);
				assertEquals("Herblore", f.skill);
			}
		}
	}

	@Test
	public void gatheringAndFarmingAreTheirOwnHands()
	{
		Flow fish = only(delta("Raw shark", 1), xp(Skill.FISHING));
		assertEquals(Source.GATHERED, fish.source);
		assertEquals("Fishing", fish.skill);
		Flow herbs = only(delta("Ranarr weed", 6), xp(Skill.FARMING));
		assertEquals(Source.FARMED, herbs.source);
		assertTrue(herbs.source.isSelf());
		Flow planted = only(delta("Ranarr seed", -1), xp(Skill.FARMING));
		assertEquals(Use.PLANTED, planted.use);
	}

	@Test
	public void aTickWithBothKindsFilesByTheItem()
	{
		// Barbarian fishing gives Fishing and Agility; cooking on the spot would add Cooking.
		List<Flow> flows = Classify.tick(delta("Raw karambwan", 1, "Cooked karambwan", 1), xp(Skill.FISHING, Skill.COOKING), sacks);
		for (Flow f : flows)
		{
			assertEquals(f.store.startsWith("Raw") ? Source.GATHERED : Source.MADE, f.source);
		}
	}

	@Test
	public void pickingUpIsLootAndDroppingIsNoBurn()
	{
		Flow take = only(delta("Ranarr seed", 2), xp(Skill.HITPOINTS).actions(Collections.singletonList(click("Take", "Ranarr seed", "Ranarr seed"))));
		assertEquals(Source.LOOTED, take.source);
		Flow drop = only(delta("Bones", -1), xp().actions(Collections.singletonList(click("Drop", "Bones", "Bones"))));
		assertEquals(Use.DROPPED, drop.use);
		assertTrue(!drop.use.isBurn());
	}

	@Test
	public void runesAndAmmoAreSpentEvenWithoutExperience()
	{
		assertEquals(Use.CAST, only(delta("Law rune", -1), xp()).use);
		assertEquals(Use.FIRED, only(delta("Ruby dragon bolts (e)", -1), xp()).use);
		assertEquals(Use.CAST, only(delta("Blood rune", -2), xp(Skill.MAGIC, Skill.HITPOINTS)).use);
		assertEquals(Use.FIRED, only(delta("Amethyst arrow", -1), xp(Skill.RANGED, Skill.HITPOINTS)).use);
	}

	@Test
	public void enchantingBoltsIsMagicMaking()
	{
		List<Flow> flows = Classify.tick(delta("Ruby dragon bolts", -10, "Ruby dragon bolts (e)", 10, "Cosmic rune", -1, "Blood rune", -1),
			xp(Skill.MAGIC), sacks);
		for (Flow f : flows)
		{
			if (f.isGain())
			{
				assertEquals(Source.MADE, f.source);
				assertEquals("Magic", f.skill);
			}
			else if (Catalogue.family(f.store) == Family.RUNES)
			{
				assertEquals(Use.CAST, f.use);
			}
		}
	}

	@Test
	public void shopsAreBoughtAndSpent()
	{
		List<Flow> flows = Classify.tick(delta("Feather", 100, "Coins", -300), xp().shop(true), sacks);
		for (Flow f : flows)
		{
			if (f.isGain())
			{
				assertEquals(Source.BOUGHT, f.source);
			}
			else
			{
				assertEquals(Use.SPENT, f.use);
				assertTrue(f.use.isBurn());
			}
		}
		Flow sold = only(delta("Coins", 50), xp().shop(true));
		assertEquals(Source.SOLD, sold.source);
		Flow alched = only(delta("Coins", 1200), xp(Skill.MAGIC));
		assertEquals(Source.ALCHED, alched.source);
	}

	@Test
	public void deathAndTradeAreNotThePaceOfPlay()
	{
		assertEquals(Use.DIED, only(delta("Shark", -20), xp().died(true)).use);
		assertEquals(Use.TRADED, only(delta("Shark", -20), xp().trade(true)).use);
		assertEquals(Source.TRADED, only(delta("Shark", 20), xp().trade(true)).source);
	}

	@Test
	public void containersHoldWhatWentInAndPlaceWhatArrivedUnseen()
	{
		// Twenty coal put in a coal bag is a move, not a loss.
		List<Flow> in = Classify.tick(delta("Coal", -20), xp().actions(Collections.singletonList(click("Fill", "Coal bag", null))), sacks);
		assertTrue(in.isEmpty());
		assertEquals(20L, (long) sacks.get("Coal"));

		// Thirty come out: twenty were the player's, ten went in unseen while mining.
		Map<Skill, Long> recent = new HashMap<>();
		recent.put(Skill.MINING, 5000L);
		Flow out = only(delta("Coal", 30), xp().actions(Collections.singletonList(click("Empty", "Coal bag", null))).recent(recent));
		assertEquals(Source.GATHERED, out.source);
		assertEquals(10, out.units);
		assertEquals("Mining", out.skill);
		assertNull(sacks.get("Coal"));
	}

	@Test
	public void packsAreBoughtAndOpenedCratesAreRewards()
	{
		Flow feathers = only(delta("Feather", 100), xp().actions(Collections.singletonList(new Classify.Action("Open", "Feather pack", null, true))));
		assertEquals(Source.BOUGHT, feathers.source);
		Flow crate = only(delta("Pure essence", 40), xp().actions(Collections.singletonList(click("Open", "Supply crate", null))));
		assertEquals(Source.REWARD, crate.source);
		assertEquals(Source.OTHER, only(delta("Coins", 5000), xp()).source);
	}

	@Test
	public void arrivalsFollowRecentWork()
	{
		Map<Skill, Long> recent = new HashMap<>();
		recent.put(Skill.FISHING, 20_000L);
		recent.put(Skill.HITPOINTS, 100L);
		assertEquals(Source.GATHERED, Classify.arrival("Raw shark", 28, recent).source);
		assertEquals(Source.LOOTED, Classify.arrival("Shark", 3, recent).source);
		assertEquals(Source.OTHER, Classify.arrival("Shark", 3, Collections.emptyMap()).source);
	}
}
