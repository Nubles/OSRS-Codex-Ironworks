package com.codex.ironworks;

import com.codex.ironworks.overlay.MineCoach;
import com.codex.ironworks.ui.Theme;
import net.runelite.api.coords.LocalPoint;

/**
 * What the plaque over a shooting star says: its size and the Mining level that layer needs, and
 * an estimate of the time left on the layer. The game only gives the share mined when the star is
 * prospected, and a layer only wears down while someone mines it, so the time is always marked as
 * an estimate, from the wiki's 7 minutes a layer or, once the player has watched enough layers end,
 * from their own.
 */
final class StarCoach
{
	private StarCoach()
	{
	}

	static MineCoach.Shown describe(StarWatch.Now n, LocalPoint at)
	{
		String title = "Size " + n.size + " · " + n.level;
		if (n.leftTicks < 0)
		{
			return new MineCoach.Shown(at, n.plane, title, "?", "Prospect it for time left", Theme.TEXT_MUTED, -1);
		}
		String basis = n.own ? "est., your layers" : "est., wiki 7 min";
		if (n.leftTicks <= 0)
		{
			return new MineCoach.Shown(at, n.plane, title, "Due", "next layer any moment", Theme.TEAL, 0);
		}
		// The bar fills as the layer is mined through.
		return new MineCoach.Shown(at, n.plane, title, "~" + MineCoach.clock(n.leftTicks), "layer left, " + basis,
			Theme.progressColor(n.mined), 1 - n.mined);
	}
}
