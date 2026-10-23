package com.codex.ironworks;

import com.codex.ironworks.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A gold arrow on the minimap pointing toward the step in hand, however far it is, and a ring on
 * the spot itself once it is on the minimap: the way Quest Helper points across the map.
 */
class Compass extends Overlay
{
	/** Tiles from the player the arrow sits at; the minimap shows about 19 in each direction. */
	static final int REACH = 15;

	private final Client client;
	private final RouteGuide guide;

	Compass(Client client, RouteGuide guide)
	{
		this.client = client;
		this.guide = guide;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		WorldPoint target = guide.aim();
		Player me = client.getLocalPlayer();
		if (target == null || me == null)
		{
			return null;
		}
		LocalPoint local = me.getLocalLocation();
		WorldPoint at = WorldPoint.fromLocalInstance(client, local);
		int[] step = toward(target.getX() - at.getX(), target.getY() - at.getY());
		if (step == null)
		{
			return null;
		}
		Point from = Perspective.localToMinimap(client, local);
		Point to = Perspective.localToMinimap(client, new LocalPoint(
			local.getX() + step[0] * Perspective.LOCAL_TILE_SIZE,
			local.getY() + step[1] * Perspective.LOCAL_TILE_SIZE, me.getWorldView()));
		if (from == null || to == null)
		{
			return null;
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (step[2] == 1)
		{
			g.setStroke(new BasicStroke(2f));
			g.setColor(Theme.INK);
			g.drawOval(to.getX() - 5, to.getY() - 5, 11, 11);
			g.setColor(Theme.GOLD);
			g.drawOval(to.getX() - 4, to.getY() - 4, 9, 9);
			return null;
		}
		double angle = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
		Polygon head = arrow(to.getX(), to.getY(), angle);
		g.setColor(Theme.INK);
		g.setStroke(new BasicStroke(3f));
		g.drawPolygon(head);
		g.setColor(Theme.GOLD);
		g.fillPolygon(head);
		return null;
	}

	/**
	 * Where on the minimap to point, in tiles from the player, for a target dx, dy tiles away: the
	 * target itself when it is in reach (third value 1), else a spot REACH tiles along the way (0).
	 * Null when the player stands on it.
	 */
	static int[] toward(int dx, int dy)
	{
		double d = Math.hypot(dx, dy);
		if (d < 1.5)
		{
			return null;
		}
		if (d <= REACH)
		{
			return new int[]{dx, dy, 1};
		}
		return new int[]{(int) Math.round(dx * REACH / d), (int) Math.round(dy * REACH / d), 0};
	}

	private static Polygon arrow(int x, int y, double angle)
	{
		Polygon p = new Polygon();
		double[][] shape = {{7, 0}, {-5, -6}, {-2, 0}, {-5, 6}};
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		for (double[] v : shape)
		{
			p.addPoint(x + (int) Math.round(v[0] * cos - v[1] * sin), y + (int) Math.round(v[0] * sin + v[1] * cos));
		}
		return p;
	}
}
