package com.codex.ironworks;

import com.codex.ironworks.ui.Theme;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines in gold, in the bank and the inventory, the items the step in hand asks for, so a bank
 * trip for the route is a matter of taking what glows.
 */
class StepItems extends WidgetItemOverlay
{
	private final ItemManager items;
	private final RouteGuide guide;
	private final Map<Integer, Boolean> asked = new HashMap<>();
	private Set<String> seen;

	StepItems(ItemManager items, RouteGuide guide)
	{
		this.items = items;
		this.guide = guide;
		showOnInventory();
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem item)
	{
		Set<String> wanted = guide.items();
		if (wanted != seen)
		{
			seen = wanted;
			asked.clear();
		}
		if (wanted.isEmpty())
		{
			return;
		}
		int id = items.canonicalize(itemId);
		if (!asked.computeIfAbsent(id, i -> RouteGuide.asked(wanted, items.getItemComposition(i).getName())))
		{
			return;
		}
		Rectangle box = item.getCanvasBounds();
		BufferedImage outline = items.getItemOutline(itemId, item.getQuantity(), Theme.GOLD);
		g.drawImage(outline, box.x, box.y, null);
	}
}
