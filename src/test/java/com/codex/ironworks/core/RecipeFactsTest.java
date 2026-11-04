package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import net.runelite.client.plugins.skillcalculator.skills.ConstructionAction;
import net.runelite.client.plugins.skillcalculator.skills.CookingAction;
import net.runelite.client.plugins.skillcalculator.skills.CraftingAction;
import net.runelite.client.plugins.skillcalculator.skills.FiremakingAction;
import net.runelite.client.plugins.skillcalculator.skills.FletchingAction;
import net.runelite.client.plugins.skillcalculator.skills.HerbloreAction;
import net.runelite.client.plugins.skillcalculator.skills.ItemSkillAction;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;
import net.runelite.client.plugins.skillcalculator.skills.PrayerAction;
import net.runelite.client.plugins.skillcalculator.skills.RunecraftAction;
import net.runelite.client.plugins.skillcalculator.skills.SailingAction;
import net.runelite.client.plugins.skillcalculator.skills.SkillAction;
import net.runelite.client.plugins.skillcalculator.skills.SmithingAction;
import org.junit.Test;

/**
 * Every recipe's level and experience, and the item it makes, checked against RuneLite's own skill
 * calculator data (net.runelite.client.plugins.skillcalculator.skills), so a figure typed wrong, or
 * one the game changes and RuneLite follows, fails the build. Farming trees are left out: RuneLite
 * counts the fruit picked too, where Ironworks counts planting and the health check (OSRS Wiki).
 */
public class RecipeFactsTest
{
	private static SkillAction[] actions(Skill s)
	{
		switch (s)
		{
			case HERBLORE:
				return HerbloreAction.values();
			case COOKING:
				return CookingAction.values();
			case CRAFTING:
				return CraftingAction.values();
			case FLETCHING:
				return FletchingAction.values();
			case SMITHING:
				return SmithingAction.values();
			case CONSTRUCTION:
				return ConstructionAction.values();
			case FIREMAKING:
				return FiremakingAction.values();
			case PRAYER:
				return PrayerAction.values();
			case RUNECRAFT:
				return RunecraftAction.values();
			case SAILING:
				// Each salvage is in it twice, salvaged at sea and sorted at a station; only sorting
				// uses the salvage up.
				return java.util.Arrays.stream(SailingAction.values()).filter(a -> a.getName().endsWith("(Sorting)"))
					.toArray(SkillAction[]::new);
			default:
				return new SkillAction[0];
		}
	}

	private static int item(SkillAction a)
	{
		return a instanceof ItemSkillAction ? ((ItemSkillAction) a).getItemId() : a.getIcon();
	}

	private static SkillAction find(Skill skill, int item)
	{
		for (SkillAction a : actions(skill))
		{
			if (item(a) == item)
			{
				return a;
			}
		}
		return null;
	}

	/**
	 * Levels the game changed after RuneLite's calculator was written, from the OSRS Wiki: marlin
	 * cooks at 91 (changelog of 25 February 2026) and ironwood hull parts are made at 75 (22 July 2026).
	 */
	private static final java.util.Map<String, Integer> WIKI_LEVELS = new java.util.HashMap<>();

	static
	{
		WIKI_LEVELS.put("Marlin", 91);
		WIKI_LEVELS.put("Ironwood hull parts", 75);
	}

	@Test
	public void everyLevelAndExperienceIsRuneLites()
	{
		int checked = 0;
		for (Recipe r : Recipes.all())
		{
			if (r.xp == 0 || r.skill == Skill.FARMING)
			{
				continue;
			}
			if (r.skill == Skill.MAGIC)
			{
				assertEquals("Air orb", r.product);
				assertEquals(MagicAction.CHARGE_AIR_ORB.getLevel(), r.level);
				assertEquals(MagicAction.CHARGE_AIR_ORB.getXp(), r.xp, 0.05);
				checked++;
				continue;
			}
			SkillAction a = find(r.skill, r.productItem);
			assertNotNull(r.product + " (" + r.productItem + ") is not in RuneLite's " + r.skill + " calculator", a);
			Integer wiki = WIKI_LEVELS.get(r.product);
			if (wiki != null)
			{
				// RuneLite has not caught up with a level the game changed: the wiki's stands, and
				// once RuneLite agrees this entry is no longer needed.
				assertEquals(r.product, (int) wiki, r.level);
				assertTrue(r.product + ": RuneLite now agrees, drop it from WIKI_LEVELS", a.getLevel() != r.level);
			}
			else
			{
				assertEquals(r.product, a.getLevel(), r.level);
			}
			double xp = a.getXp();
			if (r.inputs.containsKey("Bow string"))
			{
				// A strung longbow made from logs: cutting the unstrung bow, then stringing it.
				SkillAction cut = FletchingAction.valueOf(((Enum<?>) a).name() + "_U");
				assertEquals(r.product, cut.getLevel(), r.level);
				xp += cut.getXp();
			}
			assertEquals(r.product, xp, r.xp, 0.05);
			checked++;
		}
		assertTrue(checked > 100);
	}

	@Test
	public void sawmillPlanksAreTheOnesConstructionUses()
	{
		int sawmill = 0;
		for (Recipe r : Recipes.all())
		{
			if (!r.optional || !r.inputs.containsKey("Coins"))
			{
				continue;
			}
			sawmill++;
			assertEquals(0, r.xp, 0);
			assertEquals(1, r.count);
			boolean used = false;
			for (Recipe o : Recipes.all())
			{
				used |= o.skill == Skill.CONSTRUCTION && !o.optional && o.primary().equals(r.product);
			}
			assertTrue(r.product + " is made but never used", used);
		}
		assertEquals(7, sawmill);
	}

	@Test
	public void boatPartsAreFiveOfOneStoreAndOnlyWhenPicked()
	{
		int parts = 0;
		for (Recipe r : Recipes.all())
		{
			if (!r.product.endsWith(" parts"))
			{
				continue;
			}
			parts++;
			assertEquals(r.product, 1, r.inputs.size());
			assertEquals(r.product, 5, (int) r.inputs.get(r.primary()));
			// Hull parts from the Sailing planks are those planks' only Construction use; the rest wait for a pick.
			boolean sailingPlank = r.primary().startsWith("Camphor") || r.primary().startsWith("Ironwood")
				|| r.primary().startsWith("Rosewood");
			assertEquals(r.product, !sailingPlank, r.optional);
		}
		assertEquals(13, parts);
	}

	@Test
	public void everySailingStoreIsCatalogued()
	{
		for (Recipe r : Recipes.all())
		{
			for (String in : r.inputs.keySet())
			{
				if (!"Coins".equals(in))
				{
					assertTrue(in + " is used by " + r.product + " but is no store", Catalogue.all().contains(in));
				}
			}
		}
		int salvage = 0;
		for (Recipe r : Recipes.all())
		{
			if (r.skill == Skill.SAILING)
			{
				salvage++;
				assertTrue(r.usesUp());
				assertTrue(Catalogue.gathered(r.primary()));
				assertTrue(r.primary(), !Ways.of(r.primary()).isEmpty() && Ways.of(r.primary()).get(0).skill == Skill.SAILING);
			}
		}
		assertEquals(8, salvage);
	}
}
