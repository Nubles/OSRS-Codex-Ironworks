package com.codex.ironworks.overlay;

import com.codex.ironworks.core.Mines;
import com.codex.ironworks.core.RockTimers;
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
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Small gilt plaques over the mined rocks near the player: when each should be back. The game never
 * says, so every figure is an estimate, from the OSRS Wiki's respawn time or, once the player has
 * seen enough rocks come back at that place, from their own; the plaque says which.
 */
public class MineCoach extends Overlay
{
	private static final int PAD = 5;
	private static final int BAR = 3;
	/** Height above the rock's base the plaque floats at. */
	private static final int LIFT = 140;
	/** Height above a crashed star the plaque floats at: a star stands taller than a rock. */
	public static final int STAR_LIFT = 260;
	/** Height above a fishing spot the plaque floats at: a spot lies flat on the water. */
	public static final int SPOT_LIFT = 90;

	/** One plaque, worked out on the client thread. */
	public static final class Shown
	{
		final LocalPoint at;
		final int plane;
		public final String title;
		public final String figure;
		public final String note;
		public final Color tint;
		/** The share of the wait left, for the bar; -1 for none. */
		public final double left;

		public Shown(LocalPoint at, int plane, String title, String figure, String note, Color tint, double left)
		{
			this.at = at;
			this.plane = plane;
			this.title = title;
			this.figure = figure;
			this.note = note;
			this.tint = tint;
			this.left = left;
		}
	}

	private final Client client;
	private final int lift;
	private volatile List<Shown> shown = Collections.emptyList();

	public MineCoach(Client client)
	{
		this(client, LIFT);
	}

	public MineCoach(Client client, int lift)
	{
		this.client = client;
		this.lift = lift;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void show(List<Shown> s)
	{
		shown = s == null ? Collections.<Shown>emptyList() : s;
	}

	/** A wait as the game counts it: "0:54", "11:58". */
	public static String clock(double ticks)
	{
		long s = Math.round(Math.max(0, ticks) * 0.6);
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
	}

	/** What to say over one mined rock at this tick. */
	public static Shown describe(RockTimers.Depleted d, int tick, Mines log, LocalPoint at)
	{
		double whole = RockTimers.estimate(d, log);
		double left = RockTimers.left(d, tick, log);
		boolean own = log != null && log.learned(d.kind, d.region);
		String title = d.kind.shortName();
		if (left <= 0)
		{
			return new Shown(at, d.plane, title, "Due", own ? "past your usual" : "past the wiki's time", Theme.TEAL, 0);
		}
		String note = own ? "est., yours here" : d.kind.varies != null ? "est., wiki; varies" : "est., wiki";
		double share = whole <= 0 ? 0 : left / whole;
		// The bar fills as the rock comes back: green when it is close.
		return new Shown(at, d.plane, title, "~" + clock(left), note, Theme.progressColor(1 - share), share);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		List<Shown> list = shown;
		if (list.isEmpty() || client.getTopLevelWorldView() == null)
		{
			return null;
		}
		int plane = client.getTopLevelWorldView().getPlane();
		for (Shown s : list)
		{
			if (s.at == null || s.plane != plane)
			{
				continue;
			}
			Point p = Perspective.localToCanvas(client, s.at, s.plane, lift);
			if (p == null)
			{
				continue;
			}
			Dimension d = size(g, s);
			paint(g, s, p.getX() - d.width / 2, p.getY() - d.height);
		}
		return null;
	}

	static Dimension size(Graphics2D g, Shown s)
	{
		FontMetrics small = g.getFontMetrics(Theme.small());
		FontMetrics bold = g.getFontMetrics(Theme.title());
		int top = small.stringWidth(s.title) + 8 + bold.stringWidth(s.figure);
		int w = PAD * 2 + Math.max(top, small.stringWidth(s.note));
		int h = PAD + bold.getHeight() + small.getHeight() - 2 + (s.left >= 0 ? BAR + 3 : 0) + PAD - 3;
		return new Dimension(w, h);
	}

	/** Draws one plaque with its top-left corner at x, y. */
	public static Dimension paint(Graphics2D g, Shown s, int x, int y)
	{
		Theme.smooth(g);
		Dimension d = size(g, s);
		FontMetrics small = g.getFontMetrics(Theme.small());
		FontMetrics bold = g.getFontMetrics(Theme.title());
		RoundRectangle2D shape = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, d.width - 1, d.height - 1, 8, 8);
		g.setPaint(new GradientPaint(0, y, Theme.alpha(Theme.CARD_TOP, 220), 0, y + d.height, Theme.alpha(Theme.INK_DEEP, 220)));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD, 130));
		g.draw(shape);

		int left = x + PAD;
		int right = x + d.width - PAD;
		int line = y + PAD + bold.getAscent() - 2;
		g.setFont(Theme.small());
		Ui.shadowed(g, s.title, left, line, Theme.GOLD);
		g.setFont(Theme.title());
		Ui.shadowed(g, s.figure, right - bold.stringWidth(s.figure), line, s.tint);
		line += small.getAscent() + 1;
		g.setFont(Theme.small());
		Ui.shadowed(g, s.note, left, line, Theme.TEXT_MUTED);
		if (s.left >= 0)
		{
			int by = line + 3;
			int bw = right - left;
			g.setColor(Theme.alpha(Theme.INK_DEEP, 230));
			g.fillRoundRect(left, by, bw, BAR, BAR, BAR);
			int done = (int) Math.round(bw * Math.max(0, Math.min(1, 1 - s.left)));
			if (done > 0)
			{
				g.setColor(s.tint);
				g.fillRoundRect(left, by, done, BAR, BAR, BAR);
			}
		}
		return d;
	}
}
