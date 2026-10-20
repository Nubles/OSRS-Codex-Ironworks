package com.codex.ironworks.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WordsTest
{
	@Test
	public void ratesReadAtAGlance()
	{
		assertEquals("0", Words.rate(0));
		assertEquals("0.4", Words.rate(0.42));
		assertEquals("3", Words.rate(3.01));
		assertEquals("12", Words.rate(12.4));
		assertEquals("1,240", Words.rate(1240));
		assertEquals("41k", Words.rate(41_200));
	}

	@Test
	public void secondsForRestocks()
	{
		assertEquals("0.6s", Format.seconds(600));
		assertEquals("6.2s", Format.seconds(6_200));
		assertEquals("42s", Format.seconds(42_000));
		assertEquals("1m 30s", Format.seconds(90_000));
		assertEquals("2m", Format.seconds(120_000));
	}

	@Test
	public void accountTypes()
	{
		assertEquals("Ironman", Words.account(1));
		assertEquals("Ultimate ironman", Words.account(2));
		assertEquals("Hardcore ironman", Words.account(3));
		assertEquals("Group ironman", Words.account(4));
		assertEquals("Main account", Words.account(0));
		assertEquals("Ironworks", Words.account(-1));
	}

	@Test
	public void hoursBecomeDaysAtThePlayersPace()
	{
		assertEquals("about 3 days at your pace", Words.days(9, 3));
		assertEquals("under a day at your pace", Words.days(2, 3));
		assertEquals("about 1 day at your pace", Words.days(3, 3));
		assertNull(Words.days(9, 0));
		assertNull(Words.days(Double.POSITIVE_INFINITY, 3));
	}
}
