package com.codex.ironworks.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

/**
 * Tabs drawn as one engraved strip: the chosen segment is lit from below by a
 * gold glow, the rest sit quietly in the ink.
 */
public class Segmented extends JComponent
{
	private final String[] labels;
	private final boolean large;
	private final Animator slide = new Animator(this);
	private int selected;
	/** Where the light slides from, for a strip of more than one row. */
	private int from;
	private int rows = 1;
	private int hover = -1;
	private IntConsumer listener;

	public Segmented(boolean large, String... labels)
	{
		this.labels = labels;
		this.large = large;
		setFont(large ? Theme.title() : Theme.small());
		int h = large ? 28 : 22;
		setPreferredSize(new Dimension(10, h));
		setMinimumSize(new Dimension(10, h));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int index = indexAt(e.getX(), e.getY());
				if (index != hover)
				{
					hover = index;
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = -1;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				setSelected(indexAt(e.getX(), e.getY()), true);
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	/** Lays the segments out in this many rows, filled left to right, when one row is too narrow for them. */
	public Segmented rows(int rows)
	{
		this.rows = Math.max(1, rows);
		int h = (large ? 28 : 22) * this.rows;
		setPreferredSize(new Dimension(10, h));
		setMinimumSize(new Dimension(10, h));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
		return this;
	}

	private int cols()
	{
		return (labels.length + rows - 1) / rows;
	}

	public void onChange(IntConsumer listener)
	{
		this.listener = listener;
	}

	public int getSelected()
	{
		return selected;
	}

	public void setSelected(int index, boolean notify)
	{
		if (index < 0 || index >= labels.length)
		{
			return;
		}
		boolean changed = index != selected;
		from = selected;
		selected = index;
		slide.set(index, true);
		repaint();
		if (changed && notify && listener != null)
		{
			listener.accept(index);
		}
	}

	private int indexAt(int x)
	{
		return indexAt(x, 0);
	}

	private int indexAt(int x, int y)
	{
		int w = Math.max(1, getWidth());
		int h = Math.max(1, getHeight());
		int col = Math.max(0, Math.min(cols() - 1, x * cols() / w));
		int row = Math.max(0, Math.min(rows - 1, y * rows / h));
		return Math.max(0, Math.min(labels.length - 1, row * cols() + col));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		int cols = cols();
		float seg = w / (float) cols;
		float rowH = h / (float) rows;

		RoundRectangle2D strip = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(strip);
		if (rows > 1)
		{
			g.setColor(Theme.STROKE_SOFT);
			for (int r = 1; r < rows; r++)
			{
				g.drawLine(6, Math.round(r * rowH), w - 7, Math.round(r * rowH));
			}
		}

		// The light slides from the last segment to the chosen one, across rows as well.
		double s = slide.get();
		double t = selected == from ? 1 : Math.max(0, Math.min(1, (s - from) / (double) (selected - from)));
		float x = (float) ((from % cols) * seg + ((selected % cols) - (from % cols)) * seg * t);
		float y = (float) ((from / cols) * rowH + ((selected / cols) - (from / cols)) * rowH * t);
		RoundRectangle2D lit = new RoundRectangle2D.Float(x + 2, y + 2, seg - 4, rowH - 4, 6, 6);
		g.setPaint(new GradientPaint(0, y + 2, Theme.CARD_TOP, 0, y + rowH, Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.35)));
		g.fill(lit);
		g.setColor(Theme.alpha(Theme.GOLD, 120));
		g.draw(lit);
		g.setColor(Theme.GOLD_BRIGHT);
		g.fillRect(Math.round(x + seg * 0.3f), Math.round(y + rowH) - 4, Math.round(seg * 0.4f), 1);

		g.setFont(getFont());
		FontMetrics fm = g.getFontMetrics();
		for (int i = 0; i < labels.length; i++)
		{
			int col = i % cols;
			int row = i / cols;
			int tx = Math.round(col * seg + (seg - fm.stringWidth(labels[i])) / 2);
			int ty = Math.round(row * rowH + (rowH + fm.getAscent()) / 2) - (large ? 2 : 1);
			java.awt.Color color = i == selected ? Theme.GOLD_BRIGHT : i == hover ? Theme.TEXT : Theme.TEXT_MUTED;
			Ui.shadowed(g, labels[i], tx, ty, color);
		}
		g.setColor(Theme.STROKE_SOFT);
		g.draw(strip);
		g.dispose();
	}
}
