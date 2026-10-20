package com.codex.ironworks.ui;

import com.codex.ironworks.core.Runway;
import java.util.Locale;

/** The panel's short phrases for runways, rates and account types. */
public final class Words
{
	private Words()
	{
	}

	/** How long a store lasts, in a word or two: "Out", "3h 10m", "41h", "Steady", "Idle". */
	public static String runway(Runway r)
	{
		switch (r.status)
		{
			case OUT:
				return "Out";
			case STEADY:
				return "Steady";
			case IDLE:
				return "Idle";
			case LEARNING:
				return "Learning";
			default:
				return r.hours >= 1000 ? "999h+" : Format.estimate((long) (r.hours * 3_600_000));
		}
	}

	/** A rate per hour played: "0.4", "12", "1,240", "41k". */
	public static String rate(double perHour)
	{
		if (perHour <= 0)
		{
			return "0";
		}
		if (perHour < 10)
		{
			String one = String.format(Locale.ENGLISH, "%.1f", perHour);
			return one.endsWith(".0") ? one.substring(0, one.length() - 2) : one;
		}
		return Format.amount(Math.round(perHour));
	}

	/** The pace under a store: "Uses 42 an hour, gains 12", "Gains 6 an hour", "Not used this week". */
	public static String pace(Runway r)
	{
		if (r.status == Runway.Status.LEARNING)
		{
			return "Measuring your pace";
		}
		if (r.burn <= 0 && r.gain <= 0)
		{
			return "Not used this week";
		}
		if (r.burn <= 0)
		{
			return "Gains " + rate(r.gain) + " an hour";
		}
		return "Uses " + rate(r.burn) + " an hour" + (r.gain > 0 ? ", gains " + rate(r.gain) : "");
	}

	/** The account type from the ironman varbit, as the heading shows it. */
	public static String account(int varbit)
	{
		switch (varbit)
		{
			case 0:
				return "Main account";
			case 1:
				return "Ironman";
			case 2:
				return "Ultimate ironman";
			case 3:
				return "Hardcore ironman";
			case 4:
				return "Group ironman";
			case 5:
				return "Hardcore group ironman";
			case 6:
				return "Unranked group ironman";
			default:
				return "Ironworks";
		}
	}

	/** Hours of play as days at the player's pace: "about 3 days", or null when the pace is unknown. */
	public static String days(double hours, double hoursPerDay)
	{
		if (hoursPerDay <= 0.05 || Double.isInfinite(hours))
		{
			return null;
		}
		double days = hours / hoursPerDay;
		if (days < 1)
		{
			return "under a day at your pace";
		}
		long whole = Math.round(days);
		return "about " + whole + (whole == 1 ? " day" : " days") + " at your pace";
	}
}
