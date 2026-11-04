package com.codex.ironworks.core;

/**
 * One tree as the coach watches it: who is chopping it, and how much of its Forestry timer is gone.
 *
 * <p>What the OSRS Wiki says, and so what this counts on: a tree "will fall after 45 ticks (27
 * seconds) of chopping" (each tree its own figure, {@link Trees.Kind#fallTicks}); the timer
 * "activates after the first cut"; and it "regenerates if players stop cutting the tree before it's
 * fully chopped". The wiki gives no rate for the regrowth, nor says whether more people chopping
 * run it down faster. Here, as the Tree Despawn Timer plugin describes it (unconfirmed by the
 * wiki), a tick with anyone chopping takes one tick off, and a tick with nobody chopping gives one
 * back.
 *
 * <p>Ticks chopped before the coach saw the tree cannot be known. When others were already at it,
 * the timer may be anything up to a whole tree gone, so the time left is kept as a range: at most
 * what the ticks seen leave, and at least that less what may have gone unseen. Ticks with nobody
 * chopping close the range again, since they give back what was unseen first.
 */
public final class Chop
{
	public final Trees.Kind kind;
	/** Whether a Farming patch grew it: no timer then (OSRS Wiki: Forestry). */
	public final boolean farmed;

	/** Whether the timer has begun: the first log came, or others were at it when first seen. */
	boolean started;
	/** Ticks of the timer known to be gone. */
	int gone;
	/** Ticks that may have gone besides, unseen before the coach came. */
	int unseen;
	/** People chopping it on the last tick, the player among them. */
	int choppers;
	/** Whether the tree has fallen while watched. */
	boolean felled;
	/** Ticks since it fell, to time its regrowth. */
	int sinceFelled;

	public Chop(Trees.Kind kind, boolean farmed)
	{
		this.kind = kind;
		this.farmed = farmed;
	}

	/**
	 * One game tick at the tree.
	 *
	 * @param choppers people chopping it this tick, the player among them
	 * @param others people other than the player chopping it this tick
	 * @param cut whether the player got a log from it this tick
	 */
	public void tick(int choppers, int others, boolean cut)
	{
		this.choppers = Math.max(0, choppers);
		if (felled)
		{
			sinceFelled++;
			return;
		}
		if (farmed || !kind.timed())
		{
			return;
		}
		if (!started)
		{
			if (others > 0)
			{
				// Others were chopping before the coach came: the timer may be anywhere.
				started = true;
				unseen = kind.fallTicks;
			}
			else if (cut)
			{
				// "The timer activates after the first cut": this tick is its first.
				started = true;
			}
			else
			{
				return;
			}
		}
		if (this.choppers > 0)
		{
			gone++;
		}
		else if (gone > 0)
		{
			gone--;
		}
		else if (unseen > 0)
		{
			unseen--;
		}
		if (gone == 0 && unseen == 0 && this.choppers == 0)
		{
			// Grown back whole: the next cut starts it afresh.
			started = false;
		}
	}

	/** The tree has fallen. */
	public void fell()
	{
		felled = true;
		sinceFelled = 0;
	}

	public boolean isFelled()
	{
		return felled;
	}

	public boolean isStarted()
	{
		return started;
	}

	public int getChoppers()
	{
		return choppers;
	}

	/** Whether the whole timer has been seen, so the time left is one estimate rather than a range. */
	public boolean whole()
	{
		return started && unseen == 0;
	}

	/** Ticks of chopping the timer has left, at most; the whole timer before it starts. */
	public int mostLeft()
	{
		return Math.max(0, kind.fallTicks - gone);
	}

	/** Ticks of chopping the timer has left, at least. */
	public int leastLeft()
	{
		return Math.max(0, kind.fallTicks - gone - unseen);
	}

	/** Ticks of chopping seen since the timer began, for timing a fall seen from the start. */
	public int getGone()
	{
		return gone;
	}

	/** Ticks until it grows back once felled, or -1 while standing or when not given. */
	public int regrowLeft()
	{
		return !felled || kind.respawnTicks <= 0 ? -1 : Math.max(0, kind.respawnTicks - sinceFelled);
	}

	/** Seconds of chopping left at most, as the wiki's ticks give it. */
	public long mostLeftMs()
	{
		return mostLeft() * Trees.TICK_MS;
	}

	public long leastLeftMs()
	{
		return leastLeft() * Trees.TICK_MS;
	}
}
