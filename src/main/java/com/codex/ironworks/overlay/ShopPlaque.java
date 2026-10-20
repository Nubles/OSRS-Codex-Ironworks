package com.codex.ironworks.overlay;

import com.codex.ironworks.ui.Sigil;
import com.codex.ironworks.ui.Theme;
import com.codex.ironworks.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A gilt plaque in the game view while a shop is open: what the stores need from this shop, how
 * much it has, and when an emptied world fills again. It draws what the plugin hands it.
 */
public class ShopPlaque extends Overlay
{
	private static final int PAD = 7;
	private static final int ICON = 16;
	private static final int GAP = 12;

	private volatile Shown shown;

	/** One line of the plaque. */
	public static final class Row
	{
		final String name;
		final String figure;
		final String note;
		final Color color;

		/**
		 * @param name the item, such as "Feather pack"
		 * @param figure the stock now, such as "8 / 10"
		 * @param note what to do, such as "Buy 24 for 10h of play"
		 */
		public Row(String name, String figure, String note, Color color)
		{
			this.name = name;
			this.figure = figure;
			this.note = note;
			this.color = color;
		}
	}

	private static final class Shown
	{
		final String title;
		final String subtitle;
		final List<Row> rows;

		Shown(String title, String subtitle, List<Row> rows)
		{
			this.title = title;
			this.subtitle = subtitle;
			this.rows = rows;
		}
	}

	public ShopPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/** @param title the shop's name, or null to hide the plaque */
	public void show(String title, String subtitle, List<Row> rows)
	{
		shown = title == null || rows == null || rows.isEmpty() ? null
			: new Shown(title, subtitle, Collections.unmodifiableList(rows));
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		int nameW = 0;
		int figureW = 0;
		int noteW = small.stringWidth(s.subtitle == null ? "" : s.subtitle);
		for (Row r : s.rows)
		{
			nameW = Math.max(nameW, small.stringWidth(r.name));
			figureW = Math.max(figureW, small.stringWidth(r.figure));
			noteW = Math.max(noteW, small.stringWidth(r.note));
		}
		int textW = Math.max(bold.stringWidth(s.title), Math.max(nameW + GAP + figureW, noteW));
		int width = PAD + ICON + 7 + textW + PAD;
		int rowH = small.getHeight() * 2 + 3;
		int head = bold.getHeight() + (s.subtitle == null ? 0 : small.getHeight());
		int height = PAD + head + 4 + s.rows.size() * rowH + PAD - 2;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 230), 0, height, Theme.alpha(Theme.INK_DEEP, 230)));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);

		Sigil.paint(g, PAD, PAD, ICON);
		int x = PAD + ICON + 7;
		int y = PAD + bold.getAscent() - 2;
		g.setFont(Theme.title());
		Ui.shadowed(g, s.title, x, y, Theme.GOLD_BRIGHT);
		g.setFont(Theme.small());
		if (s.subtitle != null)
		{
			y += small.getHeight();
			Ui.shadowed(g, s.subtitle, x, y, Theme.TEXT_MUTED);
		}
		y += 4;
		for (Row r : s.rows)
		{
			g.setColor(Theme.alpha(Theme.GOLD, 50));
			g.drawLine(x, y + 2, x + textW, y + 2);
			y += small.getHeight() + 1;
			Ui.shadowed(g, r.name, x, y, Theme.TEXT);
			Ui.shadowed(g, r.figure, x + textW - small.stringWidth(r.figure), y, r.color);
			y += small.getHeight() + 1;
			Ui.shadowed(g, r.note, x, y, Theme.TEXT_MUTED);
			y += 1;
		}
		return new Dimension(width, height);
	}
}
