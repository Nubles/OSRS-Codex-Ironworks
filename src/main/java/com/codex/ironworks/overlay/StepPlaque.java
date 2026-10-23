package com.codex.ironworks.overlay;

import com.codex.ironworks.ui.Sigil;
import com.codex.ironworks.ui.Theme;
import com.codex.ironworks.ui.Ui;
import com.codex.ironworks.ui.View;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A gilt plaque in the game view while the guide points the way: the step in hand, what the game
 * still waits to see, and which way to head. It draws what the plugin hands it.
 */
public class StepPlaque extends Overlay
{
	private static final int PAD = 7;
	private static final int ICON = 16;
	/** The widest the step's text runs before it wraps. */
	private static final int TEXT = 200;
	/** Checks shown at most; the rest wait in the panel. */
	private static final int CHECKS = 6;

	private volatile Shown shown;

	private static final class Shown
	{
		final String heading;
		final String text;
		final List<View.CheckView> checks;
		volatile String way;

		Shown(String heading, String text, List<View.CheckView> checks, String way)
		{
			this.heading = heading;
			this.text = text;
			this.checks = checks;
			this.way = way;
		}
	}

	public StepPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/** @param g the guide as the panel shows it, or null to hide the plaque */
	public void show(View.GuideView g)
	{
		if (g == null || g.current == null)
		{
			shown = null;
			return;
		}
		View.StepView s = g.current;
		List<View.CheckView> checks = new ArrayList<>();
		for (View.CheckView c : s.checks)
		{
			if (!c.ok && checks.size() < CHECKS)
			{
				checks.add(c);
			}
		}
		for (View.NeedView n : g.needs)
		{
			if (n.stepsAway == 0 && n.have < n.need && checks.size() < CHECKS)
			{
				checks.add(new View.CheckView(n.item, com.codex.ironworks.ui.Format.amount(n.have) + " / " + com.codex.ironworks.ui.Format.amount(n.need), false));
			}
		}
		String way = g.way == null ? null : g.way.equals("Here") ? "You are here" : "Head " + g.way;
		shown = new Shown(s.section.toUpperCase(Locale.ENGLISH), s.text, checks, way);
	}

	/** Which way to head now, as {@link com.codex.ironworks.RouteGuide} words it; null when unknown. */
	public void way(String way)
	{
		Shown s = shown;
		if (s != null)
		{
			s.way = way == null ? null : way.equals("Here") ? "You are here" : "Head " + way;
		}
	}

	/** A line in place of the way, as it is. */
	public void say(String line)
	{
		Shown s = shown;
		if (s != null)
		{
			s.way = line;
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		String way = s.way;
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		List<String> rows = Ui.wrap(s.text, bold, TEXT);
		int textW = small.stringWidth(s.heading);
		for (String r : rows)
		{
			textW = Math.max(textW, bold.stringWidth(r));
		}
		for (View.CheckView c : s.checks)
		{
			textW = Math.max(textW, small.stringWidth(c.label) + 12 + small.stringWidth(c.figure));
		}
		if (way != null)
		{
			textW = Math.max(textW, small.stringWidth(way));
		}
		int x = PAD + ICON + 7;
		int width = x + textW + PAD;
		int line = small.getHeight();
		int height = PAD + line + rows.size() * bold.getHeight() + 2
			+ (s.checks.isEmpty() ? 0 : 4 + s.checks.size() * line)
			+ (way == null ? 0 : 3 + line) + PAD - 2;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 230), 0, height, Theme.alpha(Theme.INK_DEEP, 230)));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);
		Sigil.paint(g, PAD, PAD, ICON);

		int y = PAD + small.getAscent() - 1;
		Ui.shadowed(g, s.heading, x, y, Theme.GOLD_DIM);
		g.setFont(Theme.title());
		for (String r : rows)
		{
			y += bold.getHeight();
			Ui.shadowed(g, r, x, y, Theme.GOLD_BRIGHT);
		}
		g.setFont(Theme.small());
		y += 2;
		if (!s.checks.isEmpty())
		{
			g.setColor(Theme.alpha(Theme.GOLD, 50));
			g.drawLine(x, y + 2, x + textW, y + 2);
			y += 4;
			for (View.CheckView c : s.checks)
			{
				y += line;
				Ui.shadowed(g, c.label, x, y, Theme.TEXT_MUTED);
				Ui.shadowed(g, c.figure, x + textW - small.stringWidth(c.figure), y, Theme.EMBER);
			}
		}
		if (way != null)
		{
			y += 3 + line;
			Ui.shadowed(g, way, x, y, Theme.GOLD);
		}
		return new Dimension(width, height);
	}
}
