package com.codex.ironworks;

import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Run;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Shop;
import com.codex.ironworks.core.Summary;
import com.codex.ironworks.core.Ware;
import com.codex.ironworks.core.Window;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.ui.View;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/** Works out everything the panel shows from the saved works, on the client thread, as a copy the panel can keep. */
final class Views
{
	private Views()
	{
	}

	/** The store an item a shop sells fills, directly or as a pack; null for coins and things that are no store. */
	static String storeOf(Catalogue.Lookup lookup, String itemName)
	{
		Catalogue.Match m = lookup.match(itemName);
		if (m != null)
		{
			return m.family == Family.CURRENCY ? null : m.store;
		}
		return lookup.packOf(itemName);
	}

	/** Units of its store in one of the item: a pack's counted contents, a potion's doses. */
	static int perItem(Works works, Catalogue.Lookup lookup, String itemName)
	{
		Catalogue.Match m = lookup.match(itemName);
		if (m == null && lookup.packOf(itemName) != null)
		{
			return works.packSize(itemName);
		}
		return m == null ? 1 : m.units;
	}

	/**
	 * @param unitWorth what one unit of a store is worth, such as one dose of a potion
	 */
	static View build(Works works, Catalogue.Lookup lookup, double target, double warn, long now, LocalDate today,
		ToDoubleFunction<String> unitWorth)
	{
		View v = new View();
		v.now = now;
		v.account = works.getAccount();
		v.target = target;
		v.warn = warn;
		v.hoursPerDay = works.hoursPerDay(today);
		Window week = works.window(today, 7);
		Window day = works.window(today, 1);
		Window all = works.window(today, 0);
		v.playedWeek = week.getPlayed();
		v.bankSeen = works.seen(Place.BANK);
		Map<String, Double> worth = new HashMap<>();
		ToDoubleFunction<String> cached = store -> worth.computeIfAbsent(store, unitWorth::applyAsDouble);
		v.today = Summary.of(day, cached);
		v.week = Summary.of(week, cached);
		v.all = Summary.of(all, cached);

		Map<String, Integer> icons = new HashMap<>();
		Map<String, List<String>> boughtAt = new HashMap<>();
		List<View.ShopView> shops = new ArrayList<>();
		for (Shop s : works.getShops())
		{
			View.ShopView sv = new View.ShopView();
			sv.key = s.key();
			sv.name = s.getName();
			sv.visits = s.getVisits();
			sv.first = s.getFirst();
			sv.last = s.getLast();
			sv.bought = s.bought();
			sv.spent = new HashMap<>(s.spent());
			List<View.WareView> wares = new ArrayList<>();
			for (Ware w : s.getWares())
			{
				View.WareView wv = new View.WareView();
				wv.item = w.getItem();
				wv.name = w.getName();
				wv.store = storeOf(lookup, w.getName());
				wv.units = perItem(works, lookup, w.getName());
				wv.base = w.getBase();
				wv.bought = w.getBought();
				wv.coins = w.getSpent().getOrDefault("Coins", 0L);
				wv.restockMs = w.getRestockMs();
				wv.refillMs = w.refillMs();
				List<View.WorldView> worlds = new ArrayList<>();
				w.getWorlds().forEach((world, seen) ->
				{
					try
					{
						int n = Integer.parseInt(world);
						long full = w.fullAt(n);
						if (full > now)
						{
							View.WorldView wd = new View.WorldView();
							wd.world = n;
							wd.stock = w.expected(n, now);
							wd.fullAt = full;
							worlds.add(wd);
						}
					}
					catch (NumberFormatException ignored)
					{
						// Not a world.
					}
				});
				worlds.sort((a, b) -> Long.compare(a.fullAt, b.fullAt));
				wv.worlds = worlds;
				wares.add(wv);
				if (wv.store != null && wv.bought > 0)
				{
					List<String> at = boughtAt.computeIfAbsent(wv.store, k -> new ArrayList<>());
					if (!at.contains(s.getName()))
					{
						at.add(s.getName());
					}
				}
			}
			sv.wares = wares;
			shops.add(sv);
		}
		shops.sort((a, b) -> Long.compare(b.last, a.last));
		v.shops = shops;

		Map<String, Long> stocks = works.stocks();
		List<View.StoreView> stores = new ArrayList<>();
		List<View.Want> wants = new ArrayList<>();
		Set<String> keys = new HashSet<>(stocks.keySet());
		keys.addAll(all.getStores().keySet());
		for (String store : keys)
		{
			Catalogue.Match m = lookup.match(store);
			if (m == null)
			{
				continue;
			}
			View.StoreView sv = new View.StoreView();
			sv.store = store;
			sv.family = m.family;
			sv.icon = works.icon(store);
			sv.stock = stocks.getOrDefault(store, 0L);
			sv.runway = Runway.of(store, sv.stock, week, v.target, v.warn);
			sv.week = week.tally(store);
			sv.all = all.tally(store);
			Map<Place, Long> where = new EnumMap<>(Place.class);
			for (Place p : Place.values())
			{
				long n = works.place(p).getOrDefault(store, 0L);
				if (n > 0)
				{
					where.put(p, n);
				}
			}
			sv.where = where;
			sv.shops = boughtAt.getOrDefault(store, Collections.emptyList());
			icons.put(store, sv.icon);
			stores.add(sv);
			if (sv.runway.wanting() && !Catalogue.money(store))
			{
				wants.add(want(sv, v.shops));
			}
		}
		stores.sort((a, b) -> a.family != b.family ? a.family.compareTo(b.family)
			: a.runway.burn != b.runway.burn ? Double.compare(b.runway.burn, a.runway.burn)
			: a.stock != b.stock ? Long.compare(b.stock, a.stock) : a.store.compareTo(b.store));
		wants.sort((a, b) -> Double.compare(a.hours, b.hours));
		v.stores = stores;
		v.wants = wants;
		v.icons = icons;

		List<View.RunView> runs = new ArrayList<>();
		List<Run> list = works.getRuns();
		for (int i = list.size() - 1; i >= 0 && runs.size() < 12; i--)
		{
			Run r = list.get(i);
			View.RunView rv = new View.RunView();
			rv.start = r.getStart();
			rv.end = r.getEnd();
			List<String> named = new ArrayList<>();
			for (String key : r.getShops())
			{
				Shop s = works.shop(key);
				named.add(s == null ? key.replaceAll("@\\d+$", "") : s.getName());
			}
			rv.shops = named;
			rv.bought = new HashMap<>(r.getBought());
			rv.spent = new HashMap<>(r.getSpent());
			rv.worlds = r.worlds();
			rv.visits = r.getVisits();
			runs.add(rv);
		}
		v.runs = runs;
		return v;
	}

	/** A store short of its runway, with the shop it was bought at, or the skill it is made with. */
	private static View.Want want(View.StoreView sv, List<View.ShopView> shops)
	{
		View.Want w = new View.Want();
		w.store = sv.store;
		w.icon = sv.icon;
		w.need = sv.runway.need;
		w.hours = sv.runway.hours;
		w.status = sv.runway.status;
		View.WareView best = null;
		View.ShopView at = null;
		for (View.ShopView s : shops)
		{
			for (View.WareView ware : s.wares)
			{
				if (sv.store.equals(ware.store) && (best == null || ware.bought > best.bought))
				{
					best = ware;
					at = s;
				}
			}
		}
		if (best != null && (best.bought > 0 || sv.all.self() == 0))
		{
			w.shop = at.name;
			w.ware = best.name;
			w.wareItem = best.item;
			w.units = best.units;
			w.base = best.base;
			w.refillMs = best.refillMs;
		}
		else
		{
			String skill = null;
			long most = 0;
			for (Map.Entry<String, Long> e : sv.all.getSkills().entrySet())
			{
				if (e.getValue() > most)
				{
					most = e.getValue();
					skill = e.getKey();
				}
			}
			w.skill = skill;
		}
		return w;
	}

}
