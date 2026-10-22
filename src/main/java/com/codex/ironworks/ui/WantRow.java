package com.codex.ironworks.ui;

import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Runway;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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

	private static final class Head extends Fitted
	{
		private final View.Want w;
		private final ItemIcons icons;

		Head(View.Want w, ItemIcons icons)
		{
			this.w = w;
			this.icons = icons;
		}

		private String figure()
		{
			return "+" + Format.amount(w.need);
		}

		/** The smaller lines under the name, each with its colour. */
		private Object[][] lines()
		{
			Color color = Palette.of(w.status);
			String when = w.status == Runway.Status.OUT ? "Out now" : "Lasts " + Format.estimate((long) (w.hours * 3_600_000));
			String extra = extra(w);
			return new Object[][]{
				{when, color},
				{how(w), Theme.TEXT_MUTED},
				{w.shop == null ? null : "at " + w.shop, Theme.TEXT_FAINT},
				{extra, Theme.TEAL},
			};
		}

		/** Paints the text, or with no graphics only measures it; returns the height used. */
		private int lay(Graphics2D g, int width)
		{
			int x = Well.SIZE + 10;
			int room = width - x;
			FontMetrics bold = getFontMetrics(Theme.title());
			FontMetrics body = getFontMetrics(Theme.body());
			FontMetrics small = getFontMetrics(Theme.small());
			String figure = figure();
			int fw = bold.stringWidth(figure);
			int y = 15;
			java.util.List<String> name = Ui.wrap(w.store, body, room - fw - 6);
			if (g != null)
			{
				g.setFont(Theme.title());
				Ui.shadowed(g, figure, width - fw, 15, Palette.of(w.status));
				g.setFont(Theme.body());
			}
			for (String row : name)
			{
				if (g != null)
				{
					Ui.shadowed(g, row, x, y, Theme.TEXT);
				}
				y += 13;
			}
			y -= 2;
			if (g != null)
			{
				g.setFont(Theme.small());
			}
			for (Object[] line : lines())
			{
				if (line[0] == null)
				{
					continue;
				}
				for (String row : Ui.wrap((String) line[0], small, room))
				{
					if (g != null)
					{
						Ui.shadowed(g, row, x, y, (Color) line[1]);
					}
					y += 11;
				}
			}
			return y - 8;
		}

		@Override
		int heightFor(int width)
		{
			return Math.max(40, lay(null, width));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			BufferedImage icon = icons == null || w.icon < 0 ? null : icons.icon(w.icon, this::repaint);
			Well.paint(g, 0, 4, icon, Theme.alpha(Palette.of(w.status), 160));
			lay(g, getWidth());
			g.dispose();
		}
	}
}
