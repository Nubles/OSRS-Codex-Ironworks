package com.codex.ironworks.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** The Ironworks mark: a gilt anvil with a hammer resting above it, and a teal spark struck from the face. */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		float stroke = Math.max(0.6f, 0.55f * s);
		g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// The spark: a four-pointed teal star off the anvil's face.
		Path2D spark = new Path2D.Float();
		float cx = x + 3.6f * s;
		float cy = y + 3.4f * s;
		float r = 2.4f * s;
		float w = 0.6f * s;
		spark.moveTo(cx, cy - r);
		spark.lineTo(cx + w, cy - w);
		spark.lineTo(cx + r, cy);
		spark.lineTo(cx + w, cy + w);
		spark.lineTo(cx, cy + r);
		spark.lineTo(cx - w, cy + w);
		spark.lineTo(cx - r, cy);
		spark.lineTo(cx - w, cy - w);
		spark.closePath();
		g.setColor(Theme.alpha(Theme.TEAL, 70));
		g.fill(new java.awt.geom.Ellipse2D.Float(cx - r * 0.8f, cy - r * 0.8f, r * 1.6f, r * 1.6f));
		g.setColor(Theme.mix(Theme.TEAL, Color.WHITE, 0.35));
		g.fill(spark);

		// The hammer: a dark handle and a gilt head, tilted as if just lifted.
		AffineTransform saved = g.getTransform();
		g.rotate(Math.toRadians(-38), x + 10.8f * s, y + 3.6f * s);
		RoundRectangle2D handle = new RoundRectangle2D.Float(x + 10.2f * s, y + 3.6f * s, 1.3f * s, 5.4f * s, 0.8f * s, 0.8f * s);
		g.setColor(Theme.mix(Theme.GOLD_DIM, Theme.INK_DEEP, 0.35));
		g.fill(handle);
		g.setColor(Theme.INK_DEEP);
		g.draw(handle);
		RoundRectangle2D head = new RoundRectangle2D.Float(x + 8.2f * s, y + 1.4f * s, 5.4f * s, 2.4f * s, 0.9f * s, 0.9f * s);
		g.setPaint(new GradientPaint(x + 8 * s, y + s, Theme.GOLD_BRIGHT, x + 8 * s, y + 4 * s, Theme.GOLD_DIM));
		g.fill(head);
		g.setColor(Theme.INK_DEEP);
		g.draw(head);
		g.setTransform(saved);

		// The anvil: a horned face, a narrow waist and a broad foot, gilt and lit from above.
		Path2D anvil = new Path2D.Float();
		anvil.moveTo(x + 0.6f * s, y + 7.0f * s);
		anvil.curveTo(x + 2.2f * s, y + 7.2f * s, x + 3.4f * s, y + 6.6f * s, x + 4.2f * s, y + 6.4f * s);
		anvil.lineTo(x + 15.2f * s, y + 6.4f * s);
		anvil.lineTo(x + 15.2f * s, y + 8.6f * s);
		anvil.lineTo(x + 11.6f * s, y + 9.2f * s);
		anvil.lineTo(x + 10.6f * s, y + 11.6f * s);
		anvil.lineTo(x + 12.8f * s, y + 13.0f * s);
		anvil.lineTo(x + 12.8f * s, y + 14.6f * s);
		anvil.lineTo(x + 3.6f * s, y + 14.6f * s);
		anvil.lineTo(x + 3.6f * s, y + 13.0f * s);
		anvil.lineTo(x + 5.8f * s, y + 11.6f * s);
		anvil.lineTo(x + 5.0f * s, y + 9.2f * s);
		anvil.curveTo(x + 3.6f * s, y + 8.6f * s, x + 2.0f * s, y + 8.0f * s, x + 0.6f * s, y + 7.0f * s);
		anvil.closePath();
		g.setPaint(new GradientPaint(x, y + 6 * s, Theme.GOLD, x, y + 15 * s, Theme.mix(Theme.GOLD_DIM, Theme.INK_DEEP, 0.45f)));
		g.fill(anvil);
		g.setColor(Theme.INK_DEEP);
		g.draw(anvil);
		g.setColor(Theme.alpha(Color.WHITE, 110));
		g.drawLine(Math.round(x + 4.6f * s), Math.round(y + 7.0f * s), Math.round(x + 14.6f * s), Math.round(y + 7.0f * s));
	}
}
