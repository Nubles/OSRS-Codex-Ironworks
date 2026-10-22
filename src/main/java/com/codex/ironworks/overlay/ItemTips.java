package com.codex.ironworks.overlay;

import com.codex.ironworks.CodexIronworksConfig;
import com.codex.ironworks.CodexIronworksPlugin;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Recipe;
import com.codex.ironworks.core.Recipes;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Workshop;
import com.codex.ironworks.ui.Format;
import com.codex.ironworks.ui.View;
import com.codex.ironworks.ui.Words;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * A tooltip on an item in the inventory, the bank or the seed vault: how much of its store there
 * is and how long it lasts at the player's pace, and what the Workshop would make of it.
 */
public class ItemTips extends Overlay
{
	private static final String GOLD = "<col=d6b26a>";
	private static final String MUTED = "<col=a0947e>";
	private static final String END = "</col>";

	private final Client client;
	private final ItemManager itemManager;
	private final TooltipManager tooltipManager;
	private final CodexIronworksPlugin plugin;
	private final CodexIronworksConfig config;

	@Inject
	ItemTips(Client client, ItemManager itemManager, TooltipManager tooltipManager, CodexIronworksPlugin plugin, CodexIronworksConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.tooltipManager = tooltipManager;
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.itemTips() || client.isMenuOpen())
		{
			return null;
		}
		MenuEntry[] menu = client.getMenuEntries();
		if (menu.length == 0)
		{
			return null;
		}
		Widget widget = menu[menu.length - 1].getWidget();
		if (widget == null)
		{
			return null;
		}
		int group = WidgetUtil.componentToInterface(widget.getId());
		boolean shown = widget.getId() == InterfaceID.Inventory.ITEMS || widget.getId() == InterfaceID.Bankmain.ITEMS
			|| group == InterfaceID.BANKSIDE || group == InterfaceID.SEED_VAULT;
		int itemId = shown ? widget.getItemId() : -1;
		if (itemId <= 0)
		{
			return null;
		}
		String text = text(itemManager.getItemComposition(itemManager.canonicalize(itemId)).getName());
		if (text != null)
		{
			tooltipManager.add(new Tooltip(text));
		}
		return null;
	}

	/** The tooltip for an item, or null when it is no store. */
	String text(String itemName)
	{
		Catalogue.Match m = plugin.lookup().match(itemName);
		if (m == null || Catalogue.money(m.store))
		{
			return null;
		}
		View view = plugin.view();
		View.StoreView sv = view.byStore.get(m.store);
		List<String> lines = new ArrayList<>();
		String unit = m.family.getUnit();
		if (sv != null)
		{
			String head = GOLD + m.store + END + ": " + Format.amount(sv.stock) + " " + unit;
			Runway r = sv.runway;
			if (r.status == Runway.Status.LEARNING || r.status == Runway.Status.IDLE || r.status == Runway.Status.STEADY)
			{
				lines.add(head);
			}
			else
			{
				lines.add(head + ", " + (r.status == Runway.Status.OUT ? "out" : "lasts " + Words.runway(r) + " of play"));
			}
		}
		else
		{
			lines.add(GOLD + m.store + END);
		}
		for (Workshop.Plan p : view.workshop.plans())
		{
			for (Workshop.Make make : p.makes)
			{
				if (make.recipe.primary().equals(m.store) && !make.locked)
				{
					lines.add("Makes " + Format.amount(make.times) + " " + make.recipe.product + ": "
						+ Format.amount(Math.round(make.xp)) + " " + p.skill.getName() + " xp");
					break;
				}
			}
			for (Workshop.Short s : p.shorts)
			{
				if (s.input.equals(m.store))
				{
					lines.add(MUTED + Format.amount(s.need) + " more would finish " + Format.amount(s.more) + " " + s.recipe.product + END);
					break;
				}
			}
			if (lines.size() >= 4)
			{
				break;
			}
		}
		if (lines.size() == 1)
		{
			List<String> into = new ArrayList<>();
			for (Recipe r : Recipes.using(m.store))
			{
				if (!r.primary().equals(m.store) && into.size() < 2)
				{
					into.add(r.product.replaceAll("\\(\\d\\)$", ""));
				}
			}
			if (!into.isEmpty())
			{
				lines.add(MUTED + "Goes into " + String.join(", ", into).toLowerCase(Locale.ENGLISH) + END);
			}
		}
		return String.join("</br>", lines);
	}
}
