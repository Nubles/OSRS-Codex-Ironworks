package com.codex.ironworks.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.AnimationID;

/**
 * The pickaxe a mining animation is swung with, by RuneLite's gameval AnimationID (the set RuneLite's
 * own Mining plugin counts as mining). Ornament kits only change a pickaxe's look (OSRS Wiki, Dragon
 * pickaxe: "This only alters its appearance"), so their animations count as the pickaxe beneath.
 */
public final class Pickaxes
{
	private static final Map<Integer, String> BY_ANIMATION = new HashMap<>();

	static
	{
		put(AnimationID.HUMAN_MINING_BRONZE_PICKAXE, "Bronze pickaxe");
		put(AnimationID.HUMAN_MINING_BRONZE_PICKAXE_WALL, "Bronze pickaxe");
		put(AnimationID.HUMAN_MINING_BRONZE_PICKAXE_NOREACHFORWARD, "Bronze pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_BRONZE, "Bronze pickaxe");
		put(AnimationID.HUMAN_MINING_IRON_PICKAXE, "Iron pickaxe");
		put(AnimationID.HUMAN_MINING_IRON_PICKAXE_WALL, "Iron pickaxe");
		put(AnimationID.HUMAN_MINING_IRON_PICKAXE_NOREACHFORWARD, "Iron pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_IRON, "Iron pickaxe");
		put(AnimationID.HUMAN_MINING_STEEL_PICKAXE, "Steel pickaxe");
		put(AnimationID.HUMAN_MINING_STEEL_PICKAXE_WALL, "Steel pickaxe");
		put(AnimationID.HUMAN_MINING_STEEL_PICKAXE_NOREACHFORWARD, "Steel pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_STEEL, "Steel pickaxe");
		put(AnimationID.HUMAN_MINING_BLACK_PICKAXE, "Black pickaxe");
		put(AnimationID.HUMAN_MINING_BLACK_PICKAXE_WALL, "Black pickaxe");
		put(AnimationID.HUMAN_MINING_BLACK_PICKAXE_NOREACHFORWARD, "Black pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_BLACK, "Black pickaxe");
		put(AnimationID.HUMAN_MINING_MITHRIL_PICKAXE, "Mithril pickaxe");
		put(AnimationID.HUMAN_MINING_MITHRIL_PICKAXE_WALL, "Mithril pickaxe");
		put(AnimationID.HUMAN_MINING_MITHRIL_PICKAXE_NOREACHFORWARD, "Mithril pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_MITHRIL, "Mithril pickaxe");
		put(AnimationID.HUMAN_MINING_ADAMANT_PICKAXE, "Adamant pickaxe");
		put(AnimationID.HUMAN_MINING_ADAMANT_PICKAXE_WALL, "Adamant pickaxe");
		put(AnimationID.HUMAN_MINING_ADAMANT_PICKAXE_NOREACHFORWARD, "Adamant pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_ADAMANT, "Adamant pickaxe");
		put(AnimationID.HUMAN_MINING_RUNE_PICKAXE, "Rune pickaxe");
		put(AnimationID.HUMAN_MINING_RUNE_PICKAXE_WALL, "Rune pickaxe");
		put(AnimationID.HUMAN_MINING_RUNE_PICKAXE_NOREACHFORWARD, "Rune pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_RUNE, "Rune pickaxe");
		put(AnimationID.HUMAN_MINING_GILDED_PICKAXE, "Gilded pickaxe");
		put(AnimationID.HUMAN_MINING_GILDED_PICKAXE_WALL, "Gilded pickaxe");
		put(AnimationID.HUMAN_MINING_GILDED_PICKAXE_NOREACHFORWARD, "Gilded pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_GILDED, "Gilded pickaxe");
		put(AnimationID.HUMAN_MINING_DRAGON_PICKAXE, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_DRAGON_PICKAXE_WALL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_DRAGON_PICKAXE_NOREACHFORWARD, "Dragon pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_DRAGON, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_ZALCANO_PICKAXE, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_ZALCANO_PICKAXE_WALL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_ZALCANO_PICKAXE_NOREACHFORWARD, "Dragon pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_ZALCANO, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_PICKAXE_NO_INFERNAL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_PICKAXE_NO_INFERNAL_WALL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_PICKAXE_NO_INFERNAL_NOREACHFORWARD, "Dragon pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_TRAILBLAZER_NO_INFERNAL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_RELOADED_PICKAXE_NO_INFERNAL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_RELOADED_PICKAXE_NO_INFERNAL_WALL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_RELOADED_PICKAXE_NO_INFERNAL_NOREACHFORWARD, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_DRAGON_PICKAXE_PRETTY, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_DRAGON_PICKAXE_PRETTY_WALL, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_DRAGON_PICKAXE_PRETTY_NOREACHFORWARD, "Dragon pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_PRETTY, "Dragon pickaxe");
		put(AnimationID.HUMAN_MINING_INFERNAL_PICKAXE, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_INFERNAL_PICKAXE_WALL, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_INFERNAL_PICKAXE_NOREACHFORWARD, "Infernal pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_INFERNAL, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_PICKAXE, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_PICKAXE_WALL, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_PICKAXE_NOREACHFORWARD, "Infernal pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_TRAILBLAZER, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_RELOADED_PICKAXE, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_RELOADED_PICKAXE_WALL, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_TRAILBLAZER_RELOADED_PICKAXE_NOREACHFORWARD, "Infernal pickaxe");
		put(AnimationID.HUMAN_MINING_3A_PICKAXE, "3rd age pickaxe");
		put(AnimationID.HUMAN_MINING_3A_PICKAXE_WALL, "3rd age pickaxe");
		put(AnimationID.HUMAN_MINING_3A_PICKAXE_NOREACHFORWARD, "3rd age pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_3A, "3rd age pickaxe");
		put(AnimationID.HUMAN_MINING_CRYSTAL_PICKAXE, "Crystal pickaxe");
		put(AnimationID.HUMAN_MINING_CRYSTAL_PICKAXE_WALL, "Crystal pickaxe");
		put(AnimationID.HUMAN_MINING_CRYSTAL_PICKAXE_NOREACHFORWARD, "Crystal pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_CRYSTAL, "Crystal pickaxe");
		put(AnimationID.HUMAN_MINING_LEAGUE_TRAILBLAZER_PICKAXE, "Trailblazer pickaxe");
		put(AnimationID.HUMAN_MINING_LEAGUE_TRAILBLAZER_PICKAXE_WALL, "Trailblazer pickaxe");
		put(AnimationID.HUMAN_MINING_LEAGUE_TRAILBLAZER_PICKAXE_NOREACHFORWARD, "Trailblazer pickaxe");
		put(AnimationID.PICKAXE_POWER_SWING_LEAGUE_TRAILBLAZER, "Trailblazer pickaxe");	}

	private Pickaxes()
	{
	}

	private static void put(int animation, String pickaxe)
	{
		BY_ANIMATION.put(animation, pickaxe);
	}

	/** The pickaxe a player swinging this animation mines with, or null when it is not mining. */
	public static String of(int animation)
	{
		return BY_ANIMATION.get(animation);
	}

	public static Map<Integer, String> all()
	{
		return Collections.unmodifiableMap(BY_ANIMATION);
	}
}
