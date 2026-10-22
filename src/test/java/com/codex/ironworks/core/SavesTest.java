package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SavesTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
	private final Gson gson = new Gson();

	private Works sample()
	{
		Works w = new Works(1);
		w.setAccount(3);
		Map<String, Long> bank = new HashMap<>();
		bank.put("Shark", 400L);
		bank.put("Prayer potion", 120L);
		w.place(Place.BANK, bank, 5);
		w.place(Place.CARRIED, Collections.singletonMap("Shark", 20L), 5);
		w.icon("Shark", 385, true);
		w.played(TODAY, 3_600_000L);
		w.record(Collections.singletonList(Flow.out("Shark", 12, Use.CONSUMED, null)), TODAY);
		w.record(Collections.singletonList(Flow.in("Prayer potion", 30, Source.MADE, "Herblore")), TODAY);
		Shop s = w.visit("Gerrant's Fishy Business", 12082, 302, 10);
		w.stock(s, 11881, "Feather pack", 302, 7, 10);
		w.purchase(s, 11881, "Feather pack", 3, Collections.singletonMap("Coins", 6000L), 301, 11);
		w.pack("Feather pack", 100);
		w.getSacks().put("Coal", 27L);
		return w;
	}

	@Test
	public void aSaveReadsBackWhole()
	{
		String json = sample().write(gson);
		Works.Loaded back = Works.read(gson, json, 99);
		assertFalse(back.damaged);
		Works w = back.works;
		assertEquals(3, w.getAccount());
		assertEquals(420, w.stock("Shark"));
		assertEquals(385, w.icon("Shark"));
		assertEquals(12, w.window(TODAY, 7).tally("Shark").burned());
		assertEquals(30, w.window(TODAY, 0).tally("Prayer potion").self());
		assertEquals(3, w.getShops().get(0).getWare(11881).getBought());
		assertEquals(1, w.getRuns().size());
		assertEquals(27L, (long) w.getSacks().get("Coal"));
		assertEquals(json, w.write(gson));
	}

	@Test
	public void nothingSavedStartsFresh()
	{
		Works.Loaded fresh = Works.read(gson, null, 7);
		assertFalse(fresh.damaged);
		assertEquals(-1, fresh.works.getAccount());
		assertEquals(7, fresh.works.getSince());
	}

	@Test
	public void aSaveCutShortKeepsWhatCameBeforeTheCut()
	{
		String json = sample().write(gson);
		String cut = json.substring(0, json.indexOf("\"days\""));
		Works.Loaded back = Works.read(gson, cut, 99);
		assertTrue(back.damaged);
		assertEquals(420, back.works.stock("Shark"));
		assertEquals(3, back.works.getAccount());
	}

	@Test
	public void impossibleFiguresAreSetRight()
	{
		String json = "{\"account\":2,\"played\":-5,\"places\":{\"BANK\":{\"Shark\":-4,\"Lobster\":10}},"
			+ "\"days\":{\"not a day\":{\"played\":4},\"2026-10-20\":{\"played\":-1,\"stores\":{\"Shark\":{\"in\":{\"LOOTED\":-3}}}}},"
			+ "\"shops\":[null,{\"name\":\"\",\"wares\":{\"1\":{\"base\":-2,\"restockMs\":-9}}}],\"runs\":[null]}";
		Works.Loaded back = Works.read(gson, json, 99);
		Works w = back.works;
		assertEquals(0, w.getPlayed());
		assertEquals(0, w.stock("Shark"));
		assertEquals(10, w.stock("Lobster"));
		assertEquals(0, w.window(TODAY, 1).getPlayed());
		assertEquals(1, w.getShops().size());
		assertEquals("Shop", w.getShops().get(0).getName());
		assertEquals(0, w.getShops().get(0).getWare(1).getBase());
		assertEquals(0, w.getRuns().size());
	}

	@Test
	public void aFieldFromALaterVersionIsKeptAside()
	{
		String json = sample().write(gson);
		String later = "{\"newThing\":{\"a\":1}," + json.substring(1);
		assertTrue(Works.read(gson, later, 99).damaged);
	}

	@Test
	public void oldDaysFallAwayButTotalsStay()
	{
		Works w = sample();
		w.played(TODAY.minusDays(60), 1000);
		w.record(Collections.singletonList(Flow.out("Shark", 5, Use.CONSUMED, null)), TODAY.minusDays(60));
		w.trim(TODAY);
		assertEquals(0, w.window(TODAY, 90).tally("Shark").burned() - 12);
		assertEquals(17, w.window(TODAY, 0).tally("Shark").burned());
	}
}
