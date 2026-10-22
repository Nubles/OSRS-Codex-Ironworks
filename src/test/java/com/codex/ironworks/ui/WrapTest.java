package com.codex.ironworks.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WrapTest
{
	private final FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
		.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

	@Test
	public void everyWordShowsAndEveryRowFits()
	{
		String text = "Gerrant's Fishy Business, Jatix's Herblore Shop and Aubury's Rune Shop";
		List<String> rows = Ui.wrap(text, fm, 90);
		assertTrue(rows.size() > 1);
		for (String row : rows)
		{
			assertTrue(row, fm.stringWidth(row) <= 90);
		}
		assertEquals(text, String.join(" ", rows));
	}

	@Test
	public void aWordWiderThanTheRowIsBrokenNotCut()
	{
		String word = "Supercalifragilisticexpialidocious";
		List<String> rows = Ui.wrap(word, fm, 40);
		assertTrue(rows.size() > 1);
		assertEquals(word, String.join("", rows));
	}

	@Test
	public void shortTextStaysOnOneRow()
	{
		assertEquals(1, Ui.wrap("Feather", fm, 100).size());
	}
}
