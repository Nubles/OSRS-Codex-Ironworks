package com.codex.ironworks.core;

/**
 * How long one store lasts at the pace the player actually works through it, measured per hour
 * played rather than per day, so a week away does not make the stores look endless.
 */
public final class Runway
{
	/** Play needed before a pace is trusted. */
	public static final long LEARN_MS = 60 * 60_000L;

	public enum Status
	{
		/** None left, and it is being used. */
		OUT,
		/** Runs out within the warning time. */
		LOW,
		/** Short of the runway the player wants. */
		SHORT,
		/** Lasts at least the wanted runway. */
		OK,
		/** Coming in at least as fast as it goes. */
		STEADY,
		/** Not used lately. */
		IDLE,
		/** Not enough play yet to tell. */
		LEARNING
	}

	public final String store;
	public final long stock;
	/** Units used per hour played, or 0. */
	public final double burn;
	/** Units gained per hour played, or 0. */
	public final double gain;
	/** Hours of play the stock lasts at the burn alone; infinite when nothing is used. */
	public final double hours;
	/** Units to add to reach the wanted runway. */
	public final long need;
	public final Status status;

	private Runway(String store, long stock, double burn, double gain, double hours, long need, Status status)
	{
		this.store = store;
		this.stock = stock;
		this.burn = burn;
		this.gain = gain;
		this.hours = hours;
		this.need = need;
		this.status = status;
	}

	/**
	 * @param recent the last week or so, to measure the pace from
	 * @param target hours of play the player wants each store to last
	 * @param warn hours of play under which a store is running low
	 */
	public static Runway of(String store, long stock, Window recent, double target, double warn)
	{
		Tally t = recent.tally(store);
		if (recent.played < LEARN_MS)
		{
			return new Runway(store, stock, 0, 0, Double.POSITIVE_INFINITY, 0, Status.LEARNING);
		}
		double h = recent.played / 3_600_000.0;
		double burn = t.burned() / h;
		double gain = t.gained() / h;
		if (burn <= 0)
		{
			return new Runway(store, stock, 0, gain, Double.POSITIVE_INFINITY, 0, Status.IDLE);
		}
		double hours = stock / burn;
		long need = (long) Math.max(0, Math.ceil(target * burn - stock));
		Status status;
		if (stock <= 0)
		{
			status = Status.OUT;
		}
		else if (gain >= burn && !Catalogue.money(store))
		{
			status = Status.STEADY;
		}
		else if (hours < warn)
		{
			status = Status.LOW;
		}
		else if (hours < target)
		{
			status = Status.SHORT;
		}
		else
		{
			status = Status.OK;
		}
		return new Runway(store, stock, burn, gain, hours, need, status);
	}

	/** Whether the store wants topping up: out, low or short of the wanted runway. */
	public boolean wanting()
	{
		return status == Status.OUT || status == Status.LOW || status == Status.SHORT;
	}
}
