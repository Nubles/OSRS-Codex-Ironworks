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
			assertEquals(r.product, a.getLevel(), r.level);
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
			if (!r.optional)
			{
				continue;
			}
			sawmill++;
			assertEquals(0, r.xp, 0);
			assertEquals(1, r.count);
			assertTrue(r.product, r.inputs.containsKey("Coins"));
			SkillAction a = find(Skill.CONSTRUCTION, r.productItem);
			assertNotNull(r.product, a);
			boolean used = false;
			for (Recipe o : Recipes.all())
			{
				used |= o.skill == Skill.CONSTRUCTION && !o.optional && o.primary().equals(r.product) && o.productItem == r.productItem;
			}
			assertTrue(r.product + " is made but never used", used);
		}
		assertEquals(4, sawmill);
	}
}
