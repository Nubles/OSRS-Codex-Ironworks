package com.codex.ironworks.overlay;

import com.codex.ironworks.core.Chop;
import com.codex.ironworks.ui.Theme;
import com.codex.ironworks.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A small gilt plaque over the tree the player is chopping: how many are chopping it, and how much
 * of its Forestry timer is left as far as the ticks seen can tell. The timer is hidden in the game,
 * so the figure is always an estimate, and a range when others were at the tree first.
 */
public class TreeCoach extends Overlay
{
	private static final int PAD = 6;
	private static final int BAR = 3;
	/** Height above the tree's base the plaque floats at. */
	private static final int LIFT = 260;

	/** What the plaque says, worked out on the client thread. */
	public static final class Shown
	{
		final LocalPoint at;
		final int plane;
		public final String title;
		public final String count;
		public final String figure;
		public final String note;
		public final Color tint;
		/** The share of the timer left, at most and at least; -1 for no bar. */
		public final double most;
		public final double least;

		Shown(LocalPoint at, int plane, String title, String count, String figure, String note, Color tint, double most, double least)
		{
			this.at = at;
			this.plane = plane;
			this.title = title;
			this.count = count;
			this.figure = figure;
			this.note = note;
			this.tint = tint;
			this.most = most;
			this.least = least;
		}
	}

	private final Client client;
	private volatile Shown shown;

	public TreeCoach(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void show(Shown s)
	{
		shown = s;
	}

	/** A time of chopping as the game counts it: "1:54". */
	static String clock(long ms)
	{
		long s = (ms + 500) / 1000;
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
	}

	/**
	 * What to say about a tree.
	 *
	 * @param self whether the player is among those chopping it
	 */
	public static Shown describe(Chop c, boolean self, LocalPoint at, int plane)
	{
		int n = c.getChoppers();
		String count = n == 0 ? "nobody chopping" : n == 1 && self ? "you alone" : n + " chopping";
		String title = c.kind.name;
		if (c.isFelled())
		{
			int back = c.regrowLeft();
			return new Shown(at, plane, title, null, "Felled", back > 0 ? "grows back in ~" + clock(back * 600L) : "growing back",
				Theme.TEXT_MUTED, -1, -1);
		}
		if (c.farmed)
		{
			return new Shown(at, plane, title, count, "No timer", "grown in a Farming patch", Theme.TEAL, -1, -1);
		}
		if (!c.kind.timed())
		{
			return new Shown(at, plane, title, count, "One log", "falls with its first log", Theme.TEXT_MUTED, -1, -1);
		}
		double fall = c.kind.fallTicks;
		if (!c.isStarted())
		{
			return new Shown(at, plane, title, count, clock(c.mostLeftMs()), "starts at the first log", Theme.GOLD_BRIGHT, 1, 1);
		}
		double most = c.mostLeft() / fall;
		double least = c.leastLeft() / fall;
		if (c.mostLeft() == 0)
		{
			return new Shown(at, plane, title, count, "Any moment", "past the wiki's time", Theme.EMBER, 0, 0);
		}
		Color tint = Theme.progressColor(most);
		if (c.whole())
		{
			return new Shown(at, plane, title, count, "~" + clock(c.mostLeftMs()), "left, estimated", tint, most, most);
		}
		String figure = c.leastLeft() > 0 ? clock(c.leastLeftMs()) + "-" + clock(c.mostLeftMs()) : "≤ " + clock(c.mostLeftMs());
		return new Shown(at, plane, title, count, figure, "left; chopped before you came", tint, most, least);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null || s.at == null || client.getTopLevelWorldView() == null || client.getTopLevelWorldView().getPlane() != s.plane)
		{
			return null;
		}
		Point p = Perspective.localToCanvas(client, s.at, s.plane, LIFT);
		if (p == null)
		{
			return null;
		}
		Dimension d = size(g, s);
		paint(g, s, p.getX() - d.width / 2, p.getY() - d.height);
		return null;
	}

	static Dimension size(Graphics2D g, Shown s)
	{
		FontMetrics small = g.getFontMetrics(Theme.small());
		FontMetrics bold = g.getFontMetrics(Theme.title());
		int top = small.stringWidth(s.title) + (s.count == null ? 0 : 10 + small.stringWidth(s.count));
		int mid = bold.stringWidth(s.figure) + 6 + small.stringWidth(s.note);
		int w = PAD * 2 + Math.max(top, mid);
		int h = PAD + small.getHeight() + bold.getHeight() + (s.most >= 0 ? BAR + 5 : 1) + PAD - 3;
		return new Dimension(w, h);
	}

	/** Draws the plaque with its top-left corner at x, y. */
	public static Dimension paint(Graphics2D g, Shown s, int x, int y)
	{
		Theme.smooth(g);
		Dimension d = size(g, s);
		FontMetrics small = g.getFontMetrics(Theme.small());
		FontMetrics bold = g.getFontMetrics(Theme.title());
		RoundRectangle2D shape = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, d.width - 1, d.height - 1, 9, 9);
		g.setPaint(new GradientPaint(0, y, Theme.alpha(Theme.CARD_TOP, 225), 0, y + d.height, Theme.alpha(Theme.INK_DEEP, 225)));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD, 140));
		g.draw(shape);

		int left = x + PAD;
		int right = x + d.width - PAD;
		int line = y + PAD + small.getAscent() - 1;
		g.setFont(Theme.small());
		Ui.shadowed(g, s.title, left, line, Theme.GOLD);
		if (s.count != null)
		{
			Ui.shadowed(g, s.count, right - small.stringWidth(s.count), line, Theme.TEAL);
		}
		line += bold.getAscent() + 2;
		g.setFont(Theme.title());
		Ui.shadowed(g, s.figure, left, line, s.tint);
		g.setFont(Theme.small());
		Ui.shadowed(g, s.note, left + bold.stringWidth(s.figure) + 6, line, Theme.TEXT_MUTED);
		if (s.most >= 0)
		{
			int by = line + 5;
			int bw = right - left;
			g.setColor(Theme.alpha(Theme.INK_DEEP, 230));
			g.fillRoundRect(left, by, bw, BAR, BAR, BAR);
			int most = (int) Math.round(bw * Math.max(0, Math.min(1, s.most)));
			int least = (int) Math.round(bw * Math.max(0, Math.min(1, s.least)));
			if (most > least)
			{
				// What may have been chopped unseen: the range, faint.
				g.setColor(Theme.alpha(s.tint, 90));
				g.fillRoundRect(left, by, most, BAR, BAR, BAR);
			}
			if (least > 0)
			{
				g.setColor(s.tint);
				g.fillRoundRect(left, by, least, BAR, BAR, BAR);
			}
		}
		return d;
	}
}
