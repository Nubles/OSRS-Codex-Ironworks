package com.codex.ironworks.ui;

import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Runway;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

/** A store on the shopping list: how much is needed, and where it was bought or how it is made. */
class WantRow extends Card
{
	WantRow(View.Want w, ItemIcons icons)
	{
		super(new BorderLayout());
		setBorder(new EmptyBorder(4, 6, 4, 8));
		accent(Palette.of(w.status));
		add(new Head(w, icons), BorderLayout.CENTER);
		String how = how(w);
		setToolTipText("<html><b>" + Ui.escape(w.store) + "</b><br>Need " + Format.amount(w.need) + " more<br>" + Ui.escape(how)
			+ (w.shop != null ? " at " + Ui.escape(w.shop) : "")
			+ (w.refillMs > 0 ? "<br>The shop refills in about " + Format.estimate(w.refillMs) + " once emptied" : "") + "</html>");
	}

	/** What to do about it: buy so many at a shop, make it, or nothing known yet. */
	static String how(View.Want w)
	{
		if (w.shop != null)
		{
			long items = (w.need + w.units - 1) / Math.max(1, w.units);
			return "Buy " + Format.amount(items) + " \u00d7 " + w.ware;
		}
		if (w.skill != null)
		{
			return (Catalogue.gathered(w.store) ? "Gather it with " : "Make it with ") + w.skill;
		}
		return "No source seen yet";
	}

	/** The cost in coins, or the player's own pace of getting it, when either is known. */
	static String extra(View.Want w)
	{
		if (w.shop != null && w.coins > 0)
		{
			return Format.coins(w.coins) + " coins" + (w.perWorld > 0 && w.perWorld * w.units < w.need ? ", " + Format.amount(w.perWorld) + " a world" : "");
		}
		if (w.perHour > 0 && w.how != null)
		{
			return "About " + Format.estimate((long) (w.need / w.perHour * 3_600_000)) + " at your " + w.how + " pace";
		}
		return null;
	}

	private static final class Head extends JComponent
	{
		private final View.Want w;
		private final ItemIcons icons;

		Head(View.Want w, ItemIcons icons)
		{
			this.w = w;
			this.icons = icons;
			setOpaque(false);
			setPreferredSize(new Dimension(10, 40 + (w.shop != null ? 11 : 0) + (extra(w) != null ? 11 : 0)));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int width = getWidth();
			Color color = Palette.of(w.status);
			BufferedImage icon = icons == null || w.icon < 0 ? null : icons.icon(w.icon, this::repaint);
			Well.paint(g, 0, 4, icon, Theme.alpha(color, 160));
			int x = Well.SIZE + 10;
			int room = width - x;
			g.setFont(Theme.title());
			FontMetrics bold = g.getFontMetrics();
			String figure = "+" + Format.amount(w.need);
			int fw = bold.stringWidth(figure);
			Ui.shadowed(g, figure, width - fw, 15, color);
			g.setFont(Theme.body());
			Ui.shadowed(g, Ui.clip(w.store, g.getFontMetrics(), room - fw - 6), x, 15, Theme.TEXT);
			g.setFont(Theme.small());
			FontMetrics small = g.getFontMetrics();
			String when = w.status == Runway.Status.OUT ? "Out now" : "Lasts " + Format.estimate((long) (w.hours * 3_600_000));
			Ui.shadowed(g, Ui.clip(when, small, room), x, 26, color);
			Ui.shadowed(g, Ui.clip(how(w), small, room), x, 37, Theme.TEXT_MUTED);
			int y = 48;
			if (w.shop != null)
			{
				Ui.shadowed(g, Ui.clip("at " + w.shop, small, room), x, y, Theme.TEXT_FAINT);
				y += 11;
			}
			String extra = extra(w);
			if (extra != null)
			{
				Ui.shadowed(g, Ui.clip(extra, small, room), x, y, Theme.TEAL);
			}
			g.dispose();
		}
	}
}
