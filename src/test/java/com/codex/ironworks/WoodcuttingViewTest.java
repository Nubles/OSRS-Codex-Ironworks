package com.codex.ironworks;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Chop;
import com.codex.ironworks.core.Flow;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Trees;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.overlay.TreeCoach;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/** The woodcutting card, its logs fed into the ways to get more, and what the coach says over a tree. */
public class WoodcuttingViewTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
	private static final long NOW = 2_000_000_000L;

	private static Map<Skill, Integer> levels(int fm, int fletch, int con)
	{
		Map<Skill, Integer> m = new EnumMap<>(Skill.class);
		m.put(Skill.WOODCUTTING, 70);
		m.put(Skill.FIREMAKING, fm);
		m.put(Skill.FLETCHING, fletch);
		m.put(Skill.CONSTRUCTION, con);
		return m;
	}

	private static View view(Works w, Map<Skill, Integer> levels)
	{
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		levels.forEach((s, l) -> xp.put(s, Experience.getXpForLevel(l)));
		return Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1, levels, xp, Altar.GILDED, id -> 0);
	}

	/** 61 logs 10 seconds apart: 360 an hour over ten minutes. */
	private static Works chopped(String logs)
	{
		Works w = new Works();
		w.played(TODAY, 3_600_000L);
		for (int i = 0; i <= 60; i++)
		{
			List<Flow> f = Collections.singletonList(Flow.in(logs, 1, Source.GATHERED, "Woodcutting"));
			w.rate(f, NOW - 700_000L + i * 10_000L);
			w.record(f, TODAY);
		}
		return w;
	}

	@Test
	public void noCardUntilALogIsCut()
	{
		assertNull(view(new Works(), levels(50, 50, 50)).woodcutting);
	}

	@Test
	public void theCardShowsLogsAnHourAndWhatTheyBank()
	{
		View.WoodView v = view(chopped("Yew logs"), levels(70, 75, 50)).woodcutting;
		assertNotNull(v);
		assertEquals(61, v.logs);
		assertEquals("Yew tree", v.tree);
		assertEquals(1515, v.icon);
		assertEquals(366, v.perHour, 1e-6);
		assertEquals(366 * 175, v.xpPerHour, 1e-6);
		assertEquals(190, v.wikiFallTicks);
		assertEquals(2, v.banked.size());
		View.BankView burn = v.banked.get(0);
		assertEquals(Skill.FIREMAKING, burn.skill);
		assertEquals(366 * 202.5, burn.xpPerHour, 1e-6);
		View.BankView fletch = v.banked.get(1);
		assertEquals(Skill.FLETCHING, fletch.skill);
		assertEquals("Yew longbow", fletch.how);
		assertEquals(366 * 150, fletch.xpPerHour, 1e-6);
	}

	@Test
	public void levelsShortOfAUseSayWhatTheyNeed()
	{
		View.WoodView v = view(chopped("Oak logs"), levels(10, 20, 50)).woodcutting;
		assertEquals(3, v.banked.size());
		assertEquals(15, v.banked.get(0).needs);
		assertEquals(0, v.banked.get(0).xpPerHour, 1e-9);
		assertTrue(v.banked.get(1).how.startsWith("Oak longbow"));
		assertEquals(25, v.banked.get(1).needs);
		View.BankView con = v.banked.get(2);
		assertEquals(Skill.CONSTRUCTION, con.skill);
		assertEquals(0, con.needs);
		assertEquals(366 * 60, con.xpPerHour, 1e-6);
		assertTrue(con.how.contains("oak plank"));
	}

	@Test
	public void choppingPaceTimesLogsInTheWaysToGetMore()
	{
		Works w = chopped("Maple logs");
		assertEquals(366, Views.pace(w, "Maple logs", Source.GATHERED), 1e-6);
		View.Getting g = Views.getting("Maple logs", 1517, w, Catalogue.standard(), levels(50, 50, 50));
		assertEquals("Chop maple trees", g.ways.get(0).text);
		assertEquals(366, g.ways.get(0).perHour, 1e-6);
	}

	@Test
	public void theCoachSaysTheTimeLeftAndWhoIsChopping()
	{
		Chop c = new Chop(Trees.byName("Yew tree"), false);
		c.tick(1, 0, false);
		TreeCoach.Shown s = TreeCoach.describe(c, true, null, 0);
		assertEquals("you alone", s.count);
		assertEquals("1:54", s.figure);
		assertEquals("starts at the first log", s.note);
		c.tick(1, 0, true);
		for (int i = 0; i < 9; i++)
		{
			c.tick(3, 2, false);
		}
		s = TreeCoach.describe(c, true, null, 0);
		assertEquals("3 chopping", s.count);
		assertEquals("~1:48", s.figure);
		Chop late = new Chop(Trees.byName("Magic tree"), false);
		late.tick(4, 4, false);
		s = TreeCoach.describe(late, false, null, 0);
		assertEquals("4 chopping", s.count);
		assertTrue(s.figure.startsWith("≤ 3:53"));
		assertEquals("No timer", TreeCoach.describe(new Chop(Trees.byName("Yew tree"), true), true, null, 0).figure);
		late.fell();
		assertEquals("Felled", TreeCoach.describe(late, false, null, 0).figure);
		assertTrue(TreeCoach.describe(late, false, null, 0).note.contains("1:59"));
	}

	@Test
	public void choppersAreThoseFacingTheTree()
	{
		assertArrayEquals(new int[]{0, -1}, TreeWatch.step(0));
		assertArrayEquals(new int[]{-1, 0}, TreeWatch.step(512));
		assertArrayEquals(new int[]{0, 1}, TreeWatch.step(1024));
		assertArrayEquals(new int[]{1, 0}, TreeWatch.step(1536));
		assertArrayEquals(new int[]{-1, -1}, TreeWatch.step(256));
		assertArrayEquals(new int[]{1, -1}, TreeWatch.step(1800));
		assertArrayEquals(new int[]{0, -1}, TreeWatch.step(2040));
		// A 2x2 yew at 3205,3500: the tile north of a player south of it is in it.
		assertTrue(TreeWatch.inside(3206, 3500, 3205, 3500, 2, 2));
		assertTrue(TreeWatch.inside(3205, 3501, 3205, 3500, 2, 2));
		assertFalse(TreeWatch.inside(3207, 3500, 3205, 3500, 2, 2));
		assertFalse(TreeWatch.inside(3205, 3499, 3205, 3500, 2, 2));
	}
}
