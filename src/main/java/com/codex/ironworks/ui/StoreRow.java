package com.codex.ironworks.ui;

import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Tally;
import com.codex.ironworks.core.Use;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

/**
 * One store: its picture and stack, how long it lasts at the player's pace against the runway
 * wanted, and the pace itself. Clicked, it opens to show where the store came from and went.
 */
class StoreRow extends Card
{
	private static final int HEAD = 40;

	StoreRow(View.StoreView s, View view, ItemIcons icons, boolean open, Runnable toggle)
	{
		super(new BorderLayout());
		setBorder(new EmptyBorder(4, 6, open ? 8 : 4, 8));
		Color color = Palette.of(s.runway.status);
		if (s.runway.wanting())
		{
			accent(color);
		}
		add(new Head(s, view, icons), BorderLayout.NORTH);
		if (open)
		{
			add(detail(s, view), BorderLayout.CENTER);
		}
		highlighted(open);
		onClick(toggle);
		String unit = s.family.getUnit();
		setToolTipText("<html><b>" + Ui.escape(s.store) + "</b><br>" + Format.amount(s.stock) + " " + unit + "<br>"
			+ Ui.escape(Words.pace(s.runway)) + (open ? "" : "<br>Click for where it came from") + "</html>");
	}

	/** The row itself, painted. */
	private static final class Head extends JComponent
	{
		private final View.StoreView s;
		private final View view;
		private final ItemIcons icons;

		Head(View.StoreView s, View view, ItemIcons icons)
		{
			this.s = s;
			this.view = view;
			this.icons = icons;
			setOpaque(false);
			setPreferredSize(new Dimension(10, HEAD));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			Runway r = s.runway;
			Color color = Palette.of(r.status);
			BufferedImage icon = icons == null || s.icon < 0 ? null : icons.icon(s.icon, this::repaint);
			Well.paint(g, 0, 4, icon, r.wanting() ? Theme.alpha(color, 160) : null);
			g.setFont(Theme.small());
			String stack = Format.stack(s.stock);
			Ui.shadowed(g, stack, 2, 13, s.stock <= 0 ? Theme.BLOOD : Format.stackColor(s.stock));

			int x = Well.SIZE + 10;
			int room = w - x;
			g.setFont(Theme.title());
			FontMetrics bold = g.getFontMetrics();
			String figure = Words.runway(r);
			int fw = bold.stringWidth(figure);
			Ui.shadowed(g, figure, w - fw, 15, color);
			g.setFont(Theme.body());
			FontMetrics body = g.getFontMetrics();
			Ui.shadowed(g, Ui.clip(s.store, body, room - fw - 6), x, 15, Theme.TEXT);

			// The runway against the one wanted; idle and unknown stores get an empty groove.
			double fill = r.status == Runway.Status.IDLE || r.status == Runway.Status.LEARNING ? 0
				: r.status == Runway.Status.STEADY ? 1 : Math.min(1, r.hours / Math.max(1, view.target));
			RoundRectangle2D groove = new RoundRectangle2D.Float(x, 21, room, 3, 3, 3);
			g.setColor(Theme.INK_DEEP);
			g.fill(groove);
			if (fill > 0)
			{
				g.setColor(color);
				g.fill(new RoundRectangle2D.Float(x, 21, Math.max(3, (float) (room * fill)), 3, 3, 3));
			}
			g.setFont(Theme.small());
			FontMetrics small = g.getFontMetrics();
			Ui.shadowed(g, Ui.clip(Words.pace(r), small, room), x, 36, Theme.TEXT_MUTED);
			g.dispose();
		}
	}

	private static JComponent detail(View.StoreView s, View view)
	{
		StackPanel p = new StackPanel(2, 0);
		p.setBorder(new EmptyBorder(6, Well.SIZE + 10, 0, 0));
		String unit = s.family.getUnit();
		Runway r = s.runway;

		if (r.need > 0)
		{
			p.add(new Line("Needed for " + Math.round(view.target) + "h", Format.amount(r.need) + " " + unit, null, Theme.TEXT_MUTED, Palette.of(r.status)));
		}
		if (!Double.isInfinite(r.hours) && r.burn > 0)
		{
			String days = Words.days(r.hours, view.hoursPerDay);
			if (days != null)
			{
				p.add(new Line("Lasts " + days, null, null));
			}
		}
		List<String> kept = new ArrayList<>();
		for (Map.Entry<Place, Long> e : s.where.entrySet())
		{
			kept.add(e.getKey().getLabel() + " " + Format.amount(e.getValue()));
		}
		if (!kept.isEmpty())
		{
			p.add(new Line(String.join(" · ", kept), null, null));
		}

		Tally all = s.all;
		long in = all.gained();
		if (in > 0)
		{
			p.space(2);
			p.add(new SectionHeader("Came from", Format.amount(in) + " " + unit));
			List<Double> parts = new ArrayList<>();
			List<Color> colors = new ArrayList<>();
			for (Source src : Source.values())
			{
				if (all.in(src) > 0)
				{
					parts.add((double) all.in(src));
					colors.add(Palette.of(src));
				}
			}
			p.add(new Stacked(parts, colors, 6));
			for (Source src : Source.values())
			{
				long n = all.in(src);
				if (n > 0)
				{
					p.add(new Line(src.getLabel() + skillNote(all, src), Format.amount(n) + "  " + Format.percent(n / (double) in), Palette.of(src)));
				}
			}
		}
		long out = 0;
		for (Use u : Use.values())
		{
			out += all.out(u);
		}
		if (out > 0)
		{
			p.space(2);
			p.add(new SectionHeader("Went to", Format.amount(out) + " " + unit));
			for (Use u : Use.values())
			{
				long n = all.out(u);
				if (n > 0)
				{
					p.add(new Line(u.getLabel(), Format.amount(n) + "  " + Format.percent(n / (double) out), Palette.of(u)));
				}
			}
		}
		if (!s.shops.isEmpty())
		{
			p.space(2);
			p.add(new Line("Bought at " + String.join(", ", s.shops), null, null));
		}
		if (in == 0 && out == 0 && !Catalogue.money(s.store))
		{
			p.add(new Line("Nothing in or out yet since Ironworks began", null, null));
		}
		return p;
	}

	/** The skill most of a self-made source came from, when there is one: " (Farming)". */
	private static String skillNote(Tally all, Source src)
	{
		if (!src.isSelf() || all.getSkills().isEmpty())
		{
			return "";
		}
		String best = null;
		long most = 0;
		for (Map.Entry<String, Long> e : all.getSkills().entrySet())
		{
			boolean fits = src == Source.FARMED ? "Farming".equals(e.getKey()) : !"Farming".equals(e.getKey());
			if (fits && e.getValue() > most)
			{
				most = e.getValue();
				best = e.getKey();
			}
		}
		return best == null ? "" : " (" + best + ")";
	}
}
