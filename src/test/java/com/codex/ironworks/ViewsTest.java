package com.codex.ironworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Flow;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Shop;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Use;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ViewsTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
	private static final long NOW = 1_000_000_000L;

	private static Works feathers()
	{
		Works w = new Works();
		w.played(TODAY, 4 * 3_600_000L);
		w.record(Collections.singletonList(Flow.out("Feather", 2000, Use.MADE_INTO, "Fletching")), TODAY);
		w.record(Collections.singletonList(Flow.in("Feather", 1000, Source.BOUGHT, null)), TODAY);
		Map<String, Long> bank = new HashMap<>();
		bank.put("Feather", 1500L);
		bank.put("Shark", 300L);
		w.place(Place.BANK, bank, NOW);
		Shop s = w.visit("Gerrant's Fishy Business", 12082, 302, NOW - 60_000);
		w.stock(s, 11881, "Feather pack", 302, 10, NOW - 60_000);
		w.purchase(s, 11881, "Feather pack", 10, Collections.singletonMap("Coins", 20_000L), 301, NOW - 60_000);
		w.pack("Feather pack", 100);
		return w;
	}

	@Test
	public void aShortStoreIsListedWithTheShopItWasBoughtAt()
	{
		View v = Views.build(feathers(), Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1);
		assertEquals(1, v.wants.size());
		View.Want want = v.wants.get(0);
		assertEquals("Feather", want.store);
		// 500 an hour for ten hours, 1,500 held: 3,500 more, 35 packs.
		assertEquals(3500, want.need);
		assertEquals("Gerrant's Fishy Business", want.shop);
		assertEquals("Feather pack", want.ware);
		assertEquals(100, want.units);
		assertEquals(Runway.Status.SHORT, want.status);
		View.StoreView feather = null;
		for (View.StoreView s : v.stores)
		{
			if (s.store.equals("Feather"))
			{
				feather = s;
			}
		}
		assertTrue(feather.shops.contains("Gerrant's Fishy Business"));
		assertEquals(1500L, (long) feather.where.get(Place.BANK));
	}

	@Test
	public void routeSuppliesSayWhereToGetThem()
	{
		Works w = feathers();
		View v = Views.build(w, Catalogue.standard(), 10, 2, NOW, TODAY, s -> 1);
		v.guide = new View.GuideView();
		View.NeedView feather = new View.NeedView();
		feather.item = "Feather";
		feather.need = 5000;
		feather.have = 1500;
		View.NeedView shark = new View.NeedView();
		shark.item = "Shark";
		shark.need = 10;
		shark.have = 300;
		View.NeedView rope = new View.NeedView();
		rope.item = "Rope";
		rope.need = 1;
		v.guide.needs = java.util.Arrays.asList(feather, shark, rope);
		Views.sources(v, w, Catalogue.standard(), Collections.emptyMap());
		assertEquals("Sold at Gerrant's Fishy Business", feather.how);
		assertNull(shark.how);
	}

	@Test
	public void hiddenStoresAreLeftOut()
	{
		View v = Views.build(feathers(), Catalogue.with("", "feather"), 10, 2, NOW, TODAY, s -> 1);
		assertTrue(v.wants.isEmpty());
		for (View.StoreView s : v.stores)
		{
			assertTrue(!s.store.equals("Feather"));
		}
	}

	@Test
	public void packsFillTheirStoreAndCoinsFillNone()
	{
		Catalogue.Lookup lookup = Catalogue.standard();
		assertEquals("Feather", Views.storeOf(lookup, "Feather pack"));
		assertEquals("Feather", Views.storeOf(lookup, "Feather"));
		assertNull(Views.storeOf(lookup, "Coins"));
		assertNull(Views.storeOf(lookup, "Lobster pot"));
		Works w = new Works();
		w.pack("Feather pack", 100);
		assertEquals(100, Views.perItem(w, lookup, "Feather pack"));
		assertEquals(4, Views.perItem(w, lookup, "Prayer potion(4)"));
		assertEquals(1, Views.perItem(w, lookup, "Lobster pot"));
	}
}
