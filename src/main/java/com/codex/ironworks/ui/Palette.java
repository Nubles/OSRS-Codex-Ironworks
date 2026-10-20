package com.codex.ironworks.ui;

import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Use;
import java.awt.Color;

/** The colour each source, use and runway is drawn in, the same wherever it appears. */
public final class Palette
{
	private Palette()
	{
	}

	public static Color of(Source s)
	{
		switch (s)
		{
			case GATHERED:
				return Theme.TEAL;
			case MADE:
				return Theme.GOLD;
			case FARMED:
				return Theme.GREEN;
			case LOOTED:
				return Theme.EMBER;
			case REWARD:
				return Theme.VIOLET;
			case BOUGHT:
				return Theme.BLOCKED;
			case SOLD:
			case ALCHED:
				return Theme.GOLD_DIM;
			case TRADED:
				return Theme.TEXT_MUTED;
			default:
				return Theme.TEXT_FAINT;
		}
	}

	public static Color of(Use u)
	{
		switch (u)
		{
			case CONSUMED:
				return Theme.EMBER;
			case CAST:
				return Theme.VIOLET;
			case FIRED:
				return Theme.GOLD;
			case MADE_INTO:
				return Theme.TEAL;
			case PLANTED:
				return Theme.GREEN;
			case OFFERED:
				return Theme.GOLD_BRIGHT;
			case SPENT:
				return Theme.BLOCKED;
			case DIED:
				return Theme.BLOOD;
			case SOLD:
				return Theme.GOLD_DIM;
			case TRADED:
				return Theme.TEXT_MUTED;
			default:
				return Theme.TEXT_FAINT;
		}
	}

	public static Color of(Runway.Status s)
	{
		switch (s)
		{
			case OUT:
				return Theme.BLOOD;
			case LOW:
				return Theme.EMBER;
			case SHORT:
				return Theme.GOLD;
			case OK:
				return Theme.TEAL;
			case STEADY:
				return Theme.GREEN;
			default:
				return Theme.TEXT_MUTED;
		}
	}
}
