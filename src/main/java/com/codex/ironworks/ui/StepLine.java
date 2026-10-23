package com.codex.ironworks.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * One step of the route: a small box, ticked when the step is done, and its text wrapped beside it.
 * A step the game has confirmed is sealed; any other can be ticked or unticked with a click.
 */
public class StepLine extends Fitted
{
	private static final int BOX = 9;
	private static final int GAP = 6;
	private static final int ROW = 13;

	private final String text;
	private final boolean done;
	private final boolean sealed;
	private final boolean here;
	private boolean hover;

	/**
	 * @param done ticked by hand, in the moment, or met in game
	 * @param sealed the game confirms it, so a click would change nothing
	 * @param here the step in hand
	 * @param toggle run on a click, or null for a line that only shows
	 */
	public StepLine(String text, boolean done, boolean sealed, boolean here, Runnable toggle)
	{
		this.text = text;
		this.done = done;
		this.sealed = sealed;
		this.here = here;
		setFont(Theme.small());
		if (sealed)
		{
			setToolTipText("The game shows this done");
		}
		else if (toggle != null)
		{
			setToolTipText(done ? "Done. Click to undo" : "Click to mark done");
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hover = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hover = false;
					repaint();
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint()))
					{
						toggle.run();
					}
				}
			});
		}
	}

	private List<String> rows(FontMetrics fm, int width)
	{
		return Ui.wrap(text, fm, width - BOX - GAP - 2);
	}

	@Override
	int heightFor(int width)
	{
		return rows(getFontMetrics(getFont()), width).size() * ROW + 3;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setFont(getFont());
		FontMetrics fm = g.getFontMetrics();
		int w = getWidth();
		if (hover)
		{
			g.setColor(Theme.CARD_HOVER);
			g.fillRoundRect(-3, 0, w + 6, getHeight(), 6, 6);
		}
		int base = fm.getAscent() + 1;
		float top = base - fm.getAscent() / 2f - BOX / 2f;
		RoundRectangle2D box = new RoundRectangle2D.Float(1, top, BOX, BOX, 3, 3);
		Color edge = done ? Theme.GREEN : here ? Theme.GOLD : hover ? Theme.GOLD_DIM : Theme.STROKE;
		if (done)
		{
			g.setColor(Theme.alpha(Theme.GREEN, sealed ? 90 : 50));
			g.fill(box);
		}
		g.setColor(edge);
		g.draw(box);
		if (done)
		{
			Path2D tick = new Path2D.Float();
			tick.moveTo(3, top + BOX / 2f);
			tick.lineTo(1 + BOX * 0.42f, top + BOX - 2.5f);
			tick.lineTo(BOX - 1, top + 2.5f);
			g.setStroke(new java.awt.BasicStroke(1.5f));
			g.setColor(Theme.GREEN);
			g.draw(tick);
		}
		Color color = here ? Theme.GOLD_BRIGHT : done ? Theme.TEXT_FAINT : Theme.TEXT_MUTED;
		List<String> rows = rows(fm, w);
		for (int i = 0; i < rows.size(); i++)
		{
			Ui.shadowed(g, rows.get(i), BOX + GAP + 2, base + i * ROW, color);
		}
		g.dispose();
	}
}
