package com.codex.ironworks;

import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Death;
import com.codex.ironworks.core.Holding;
import com.codex.ironworks.core.Price;
import com.codex.ironworks.core.Rate;
import com.codex.ironworks.core.Recipe;
import com.codex.ironworks.core.Recipes;
import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Run;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Shop;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Spot;
import com.codex.ironworks.core.Summary;
import com.codex.ironworks.core.Ware;
import com.codex.ironworks.core.Ways;
import com.codex.ironworks.core.Window;
import com.codex.ironworks.core.Workshop;
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
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import net.runelite.api.Skill;

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

	/** Share a shop's price may climb by before it is cheaper to hop worlds. */
	static final double HOP_SHARE = 0.1;

	static View build(Works works, Catalogue.Lookup lookup, double target, double warn, long now, LocalDate today,
		ToDoubleFunction<String> unitWorth)
	{
		return build(works, lookup, target, warn, now, today, unitWorth, Collections.emptyMap(), Collections.emptyMap(),
			Altar.GILDED, id -> 0);
	}

	/**
	 * @param unitWorth what one unit of a store is worth, such as one dose of a potion
	 * @param levels the player's levels, empty when not known
	 * @param priceOf what one of an item is worth, by item id
	 */
	static View build(Works works, Catalogue.Lookup lookup, double target, double warn, long now, LocalDate today,
		ToDoubleFunction<String> unitWorth, Map<Skill, Integer> levels, Map<Skill, Integer> xp, Altar altar,
		ToLongFunction<Integer> priceOf)
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
				Price price = w.price();
				if (price != null)
				{
					wv.price = price.first;
					wv.climb = price.climb;
				}
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
				wants.add(want(sv, v.shops, works, levels));
			}
		}
		stores.sort((a, b) -> a.family != b.family ? a.family.compareTo(b.family)
			: a.runway.burn != b.runway.burn ? Double.compare(b.runway.burn, a.runway.burn)
			: a.stock != b.stock ? Long.compare(b.stock, a.stock) : a.store.compareTo(b.store));
		wants.sort((a, b) -> Double.compare(a.hours, b.hours));
		v.stores = stores;
		v.wants = wants;
		v.icons = icons;
		Map<String, View.StoreView> byStore = new HashMap<>();
		for (View.StoreView sv : stores)
		{
			byStore.put(sv.store, sv);
		}
		v.byStore = byStore;

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

		v.levels = levels;
		v.xp = xp;
		v.altar = altar;
		v.workshop = Workshop.plan(stocks, lookup, levels, altar);
		Map<String, View.Getting> getting = new HashMap<>();
		for (View.StoreView sv : stores)
		{
			if (!Catalogue.money(sv.store))
			{
				getting.put(sv.store, getting(sv.store, sv.icon, works, lookup, levels));
			}
		}
		v.getting = getting;
		long listCoins = 0;
		for (View.Want w : wants)
		{
			listCoins += w.coins;
		}
		v.listCoins = listCoins;
		v.coins = stocks.getOrDefault("Coins", 0L);
		found(v, works, lookup, priceOf, now);
		return v;
	}

	/** Everything owned, wherever kept, and the places themselves, and the last death. */
	private static void found(View v, Works works, Catalogue.Lookup lookup, ToLongFunction<Integer> priceOf, long now)
	{
		Map<String, View.Found> byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		List<View.SpotView> spots = new ArrayList<>();
		for (Spot spot : Spot.values())
		{
			Holding h = works.holding(spot);
			if (h == null)
			{
				continue;
			}
			View.SpotView sv = new View.SpotView();
			sv.spot = spot;
			sv.at = h.getAt();
			sv.kinds = h.getItems().size();
			List<long[]> valued = new ArrayList<>();
			for (Map.Entry<Integer, Long> e : h.getItems().entrySet())
			{
				int id = e.getKey();
				long qty = e.getValue();
				String name = works.itemName(id);
				if (name == null)
				{
					continue;
				}
				long each = priceOf.applyAsLong(id);
				sv.worth += each * qty;
				valued.add(new long[]{id, qty, each * qty});
				Catalogue.Match m = lookup.match(name);
				boolean potion = m != null && m.family == Family.POTIONS;
				// Potion storage holds one-dose potions counted in doses; other places hold whole potions.
				long units = potion && spot != Spot.POTION_STORAGE ? qty * m.units : qty;
				String key = potion ? m.store : name;
				View.Found f = byName.get(key);
				if (f == null)
				{
					f = new View.Found();
					f.name = key;
					f.doses = potion;
					f.store = m == null ? null : m.store;
					f.where = new EnumMap<>(Spot.class);
					byName.put(key, f);
				}
				if (f.icon < 0 || potion && m.model)
				{
					f.icon = id;
				}
				f.total += units;
				f.where.merge(spot, units, Long::sum);
			}
			valued.sort((a, b) -> Long.compare(b[2], a[2]));
			sv.top = new ArrayList<>(valued.subList(0, Math.min(4, valued.size())));
			spots.add(sv);
		}
		v.found = new ArrayList<>(byName.values());
		v.spots = spots;
		Death d = works.getDeath();
		if (d != null)
		{
			View.DeathView dv = new View.DeathView();
			dv.at = d.getAt();
			dv.world = d.getWorld();
			dv.graveLeft = d.graveLeft(works.getPlayed());
			List<long[]> items = new ArrayList<>();
			for (Map.Entry<Integer, Long> e : d.getItems().entrySet())
			{
				long worth = priceOf.applyAsLong(e.getKey()) * e.getValue();
				dv.worth += worth;
				items.add(new long[]{e.getKey(), e.getValue(), worth});
			}
			items.sort((a, b) -> Long.compare(b[2], a[2]));
			dv.items = items;
			List<String> names = new ArrayList<>();
			for (long[] it : items)
			{
				String n = works.itemName((int) it[0]);
				names.add(n == null ? "?" : n);
			}
			dv.names = names;
			v.death = dv;
		}
	}

	/**
	 * Where to get what the route is short of: a shop you have bought from that sells it first, else the
	 * best way to get more, with how long the shortfall takes at your own pace when Ironworks knows it.
	 */
	static void sources(View view, Works works, Catalogue.Lookup lookup, Map<Skill, Integer> levels)
	{
		if (view.guide == null)
		{
			return;
		}
		for (View.NeedView n : view.guide.needs)
		{
			long short_ = n.need - n.have;
			if (short_ <= 0)
			{
				continue;
			}
			Catalogue.Match m = lookup.match(n.item);
			String store = m == null ? n.item : m.store;
			String shop = null;
			for (View.ShopView sv : view.shops)
			{
				for (View.WareView w : sv.wares)
				{
					if (n.item.equalsIgnoreCase(w.name) || store.equalsIgnoreCase(w.store))
					{
						shop = sv.name;
						break;
					}
				}
				if (shop != null)
				{
					break;
				}
			}
			if (shop != null)
			{
				n.how = "Sold at " + shop;
				continue;
			}
			for (View.WayView w : getting(store, n.icon, works, lookup, levels).ways)
			{
				if (!w.open)
				{
					continue;
				}
				n.how = w.text;
				if (w.perHour > 0)
				{
					n.how += ", about " + com.codex.ironworks.ui.Format.duration(Math.round(short_ / w.perHour * 3_600_000)) + " at your pace";
				}
				break;
			}
		}
	}

	/** The ways to get more of a store: the player's own paces first, then what they have the levels for. */
	static View.Getting getting(String store, int icon, Works works, Catalogue.Lookup lookup, Map<Skill, Integer> levels)
	{
		View.Getting g = new View.Getting();
		g.store = store;
		g.icon = icon;
		List<View.WayView> ways = new ArrayList<>();
		for (Ways.Way w : Ways.of(store))
		{
			View.WayView wv = new View.WayView();
			wv.kind = w.kind;
			wv.text = w.text;
			wv.skill = w.skill;
			wv.level = w.level;
			wv.open = w.skill == null || levels.getOrDefault(w.skill, 99) >= w.level;
			Rate r = works.rate(store, w.kind.getSource());
			wv.perHour = r == null ? 0 : r.perHour();
			ways.add(wv);
		}
		for (Recipe r : Recipes.all())
		{
			Catalogue.Match m = r.count > 0 ? lookup.match(r.product) : null;
			if (m == null || !m.store.equals(store))
			{
				continue;
			}
			View.WayView wv = new View.WayView();
			wv.kind = Ways.Kind.MAKE;
			List<String> in = new ArrayList<>();
			r.inputs.forEach((k, q) -> in.add(q > 1 ? q + " " + k.toLowerCase(java.util.Locale.ENGLISH) : k.toLowerCase(java.util.Locale.ENGLISH)));
			wv.text = "Make it from " + String.join(" and ", in);
			wv.skill = r.skill;
			wv.level = r.level;
			wv.open = levels.getOrDefault(r.skill, 99) >= r.level;
			Rate rate = works.rate(store, Source.MADE);
			wv.perHour = rate == null ? 0 : rate.perHour();
			ways.add(wv);
		}
		// Ways the player has a pace for come first, fastest first; then the open ones; locked ones last.
		ways.sort((a, b) -> a.perHour > 0 || b.perHour > 0 ? Double.compare(b.perHour, a.perHour)
			: Boolean.compare(b.open, a.open));
		g.ways = ways;
		return g;
	}

	/** A store short of its runway, with the shop it was bought at, or the skill it is made with. */
	private static View.Want want(View.StoreView sv, List<View.ShopView> shops, Works works, Map<Skill, Integer> levels)
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
			if (best.price > 0)
			{
				Price p = new Price(best.price, best.climb, best.base);
				long items = (w.need + w.units - 1) / Math.max(1, w.units);
				w.coins = p.costHopping(items, HOP_SHARE);
				w.perWorld = p.perWorld(HOP_SHARE);
			}
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
		double fastest = 0;
		for (Source src : new Source[]{Source.GATHERED, Source.FARMED, Source.MADE, Source.LOOTED})
		{
			Rate r = works.rate(sv.store, src);
			if (r != null && r.perHour() > fastest)
			{
				fastest = r.perHour();
				w.how = src.getLabel().toLowerCase(java.util.Locale.ENGLISH);
			}
		}
		w.perHour = fastest;
		return w;
	}

}
