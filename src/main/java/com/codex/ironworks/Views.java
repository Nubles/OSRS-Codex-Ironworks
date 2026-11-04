package com.codex.ironworks;

import com.codex.ironworks.core.Acquire;
import com.codex.ironworks.core.Travel;
import com.codex.ironworks.core.Trips;
import com.codex.ironworks.core.Herbiboar;
import com.codex.ironworks.core.Herbiboars;

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
import com.codex.ironworks.core.Pouch;
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
		return build(works, lookup, target, warn, now, today, unitWorth, levels, xp, altar, priceOf, null, null);
	}

	/** As above, with Codex Wayfinder's atlas and where the player stands, for travel times; both may be null. */
	static View build(Works works, Catalogue.Lookup lookup, double target, double warn, long now, LocalDate today,
		ToDoubleFunction<String> unitWorth, Map<Skill, Integer> levels, Map<Skill, Integer> xp, Altar altar,
		ToLongFunction<Integer> priceOf, Travel travel, int[] here)
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
			sv.region = s.getRegion();
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
		v.methods = new HashMap<>(works.methods());
		v.workshop = Workshop.plan(stocks, lookup, levels, xp, altar, methods(works, v.methods));
		Map<String, View.Getting> getting = new HashMap<>();
		for (View.StoreView sv : stores)
		{
			if (!Catalogue.money(sv.store))
			{
				View.Getting g = getting(sv.store, sv.icon, works, lookup, levels);
				long need = sv.runway.need > 0 ? sv.runway.need : Math.round(sv.runway.burn * target);
				plan(g, need, works, levels, shops, travel, here);
				getting.put(sv.store, g);
			}
		}
		v.getting = getting;
		v.herbiboar = herbiboar(works, levels);
		v.shortWays = shortWays(v.workshop, works, lookup, levels, shops, travel, here);
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

	/**
	 * The player's own pace at a store one way. Runes are made in bursts at the altar with the run to
	 * the bank between, so a rune is timed by whole trips ({@link Trips}), never gain to gain; until
	 * enough trips are timed it has no pace rather than a pace that leaves the running out.
	 */
	static double pace(Works works, String store, Source src)
	{
		if (src == Source.GATHERED && Catalogue.family(store) == Family.HERBS && works.herbiboars().gave(store))
		{
			// Herbs come once a trail: timed by whole trails, never gain to gain.
			return works.herbiboars().perHour(store);
		}
		if ((src == Source.MADE || src == Source.GATHERED) && Catalogue.family(store) == Family.RUNES)
		{
			Trips.Leg leg = works.trips().leg(store);
			if (leg != null || src == Source.MADE)
			{
				return leg == null ? 0 : leg.perHour();
			}
		}
		Rate r = works.rate(store, src);
		return r == null ? 0 : r.perHour();
	}

	/** Hunter a hunter potion can be boosted from to track the herbiboar (OSRS Wiki: Herbiboar). */
	static final int HERBIBOAR_BOOSTABLE = 74;

	/**
	 * The herbiboar card on the Sources tab: the player's own harvests and trails, the herbs an hour
	 * they come to at the stretch of Herblore levels last hunted at, and the Herblore experience
	 * those herbs bank. Null when the account has never harvested one and cannot reach it yet.
	 */
	static View.HerbiView herbiboar(Works works, Map<Skill, Integer> levels)
	{
		Herbiboars log = works.herbiboars();
		Integer hunter = levels.get(Skill.HUNTER);
		Integer herbloreLevel = levels.get(Skill.HERBLORE);
		boolean reach = hunter != null && herbloreLevel != null && hunter >= HERBIBOAR_BOOSTABLE
			&& herbloreLevel >= Herbiboar.HERBLORE;
		if (log.isEmpty() && !reach)
		{
			return null;
		}
		int herblore = herbloreLevel == null ? 99 : herbloreLevel;
		View.HerbiView h = new View.HerbiView();
		h.harvests = log.getHarvests();
		h.timed = log.getTimed();
		h.trailsPerHour = log.trailsPerHour();
		h.trailMs = log.trailMs();
		List<String> missing = new ArrayList<>();
		if (hunter != null && hunter < Herbiboar.HUNTER)
		{
			missing.add(Herbiboar.HUNTER + " Hunter");
		}
		if (herbloreLevel != null && herbloreLevel < Herbiboar.HERBLORE)
		{
			missing.add(Herbiboar.HERBLORE + " Herblore");
		}
		h.missing = missing;
		String key = log.currentBand();
		Herbiboars.Band band = log.band(key);
		List<View.HerbView> herbs = new ArrayList<>();
		if (band != null)
		{
			String[] lohi = key.split("-");
			h.bandLo = Integer.parseInt(lohi[0]);
			h.bandHi = Integer.parseInt(lohi[1]);
			h.bandStale = herbloreLevel != null && !Herbiboar.bandKey(herblore).equals(key);
			h.bandHarvests = band.getHarvests();
			h.herbsPerHarvest = band.perHarvest();
			h.herbsPerHour = h.trailsPerHour * h.herbsPerHarvest;
			h.harvestXpPerHour = h.trailsPerHour * Math.max(0, h.herbsPerHarvest - 1) * Herbiboar.XP_PER_EXTRA_HERB;
			for (String herb : band.getByHerb().keySet())
			{
				View.HerbView hv = new View.HerbView();
				hv.herb = herb;
				Herbiboar.Range r = Herbiboar.range(herb);
				hv.icon = r != null ? r.grimy : works.icon(herb);
				hv.perHarvest = band.perHarvest(herb);
				hv.perHour = h.trailsPerHour * hv.perHarvest;
				Recipe best = bestPotion(herb, herblore);
				if (best != null)
				{
					hv.potion = best.product;
					hv.potionXp = best.xp;
					h.bankedXpPerHour += hv.perHour * best.xp;
				}
				herbs.add(hv);
			}
			herbs.sort((a, b) -> Double.compare(b.perHarvest, a.perHarvest));
		}
		h.herbs = herbs;
		List<String> unseen = new ArrayList<>();
		if (herbloreLevel != null && herblore >= Herbiboar.HERBLORE)
		{
			for (String herb : Herbiboar.herbs())
			{
				if (Herbiboar.drops(herb, herblore) && !log.gave(herb))
				{
					unseen.add(herb);
				}
			}
		}
		h.unseen = unseen;
		return h;
	}

	/**
	 * The potion one herb makes the most Herblore experience as at this level: the herb, or its
	 * unfinished potion, as a recipe's main input. The secondaries are the player's to find.
	 */
	static Recipe bestPotion(String herb, int herblore)
	{
		String unf = null;
		for (Recipe r : Recipes.all())
		{
			if (r.skill == Skill.HERBLORE && r.count > 0 && r.xp == 0 && herb.equals(r.primary()))
			{
				unf = r.product;
			}
		}
		Recipe best = null;
		for (Recipe r : Recipes.all())
		{
			if (r.skill != Skill.HERBLORE || r.xp <= 0 || r.level > herblore)
			{
				continue;
			}
			String main = r.primary();
			if ((herb.equals(main) || main.equals(unf)) && (best == null || r.xp > best.xp))
			{
				best = r;
			}
		}
		return best;
	}

	/** The store runes are crafted from, as the Make tab counts it. */
	static final String ESSENCE = "Pure essence";

	/**
	 * The recipes the Make tab plans with: the player's picks, and pure essence sent to the altar the
	 * player's latest trip crafted at, unless they picked otherwise.
	 */
	static Map<String, String> methods(Works works, Map<String, String> picked)
	{
		String rune = craftedFromEssence(works, picked);
		if (rune == null)
		{
			return picked;
		}
		Map<String, String> out = new HashMap<>(picked);
		out.put(ESSENCE, rune);
		return out;
	}

	/** The rune pure essence goes to because the player's trips craft it; null when picked or unknown. */
	static String craftedFromEssence(Works works, Map<String, String> picked)
	{
		if (picked.containsKey(ESSENCE))
		{
			return null;
		}
		String rune = works.trips().current();
		if (rune == null)
		{
			return null;
		}
		for (Recipe r : Workshop.methods(ESSENCE))
		{
			if (r.product.equals(rune))
			{
				return rune;
			}
		}
		return null;
	}

	/**
	 * The runecrafting card: the pouches as last read, the essence carried now, each rune's trips,
	 * and what the pure essence in the stores comes to at the player's own trips.
	 *
	 * @param pouches the pouches last carried, or empty
	 * @param pack essence in the pack now
	 * @param live whether the pouches were read on this login
	 */
	static void runecraft(View v, Works works, List<View.PouchView> pouches, long pack, boolean live)
	{
		Trips trips = works.trips();
		if (pouches.isEmpty() && trips.isEmpty())
		{
			v.rc = null;
			return;
		}
		View.RcView rc = new View.RcView();
		rc.pouches = pouches;
		rc.live = live;
		rc.pack = pack;
		for (View.PouchView p : pouches)
		{
			rc.pouched += p.amount;
			rc.pouchRoom += p.capacity;
		}
		List<View.TripView> list = new ArrayList<>();
		trips.legs().forEach((rune, leg) ->
		{
			View.TripView t = new View.TripView();
			t.rune = rune;
			t.icon = works.icon(rune);
			for (Recipe r : Workshop.methods(ESSENCE))
			{
				if (t.icon < 0 && r.product.equals(rune))
				{
					t.icon = r.productItem;
				}
			}
			t.trips = leg.getTrips();
			t.essence = leg.essencePerTrip();
			t.perHour = leg.perHour();
			t.runesPerEssence = leg.runesPerEssence();
			t.roundMs = leg.roundMs();
			list.add(t);
		});
		list.sort((a, b) -> Long.compare(b.trips, a.trips));
		rc.trips = list;
		rc.bestTrip = trips.bestTrip();
		rc.current = trips.current();
		// Essence a trip at the altar the player crafts at now; a trip to another altar carries what it needs.
		Trips.Leg now = rc.current == null ? null : trips.leg(rc.current);
		rc.essencePerTrip = now != null ? now.essencePerTrip() : trips.essencePerTrip();
		rc.essence = works.stock(ESSENCE);
		rc.essenceTo = craftedFromEssence(works, v.methods);
		String rune = v.methods.containsKey(ESSENCE) ? v.methods.get(ESSENCE) : rc.current;
		Trips.Leg leg = rune == null ? null : trips.leg(rune);
		if (rc.essence > 0 && rc.essencePerTrip >= 1)
		{
			rc.tripsInStock = (long) Math.ceil(rc.essence / rc.essencePerTrip);
			if (leg != null && leg.roundMs() > 0)
			{
				rc.stockMs = rc.tripsInStock * leg.roundMs();
			}
			if (leg != null && leg.runesPerEssence() > 0)
			{
				rc.runesInStock = Math.round(rc.essence * leg.runesPerEssence());
			}
		}
		v.rc = rc;
	}

	/** One pouch as the game's counts give it: what it holds, how worn it is, and what that means. */
	static View.PouchView pouch(Pouch p, int itemId, int amount, int wear, int runecraft)
	{
		View.PouchView pv = new View.PouchView();
		pv.name = p.getLabel();
		pv.item = itemId;
		pv.amount = Math.max(0, amount);
		pv.capacity = p.capacity(wear, runecraft);
		pv.full = p.capacityNew(runecraft);
		pv.degraded = Pouch.degraded(itemId) || pv.capacity < pv.full;
		pv.wears = p.wears();
		pv.safe = p.safeEssence(wear);
		pv.expected = p.expectedEssence(wear);
		pv.next = p.capacityAfterNext(wear, runecraft);
		pv.atRisk = p.atRisk(wear, runecraft);
		return pv;
	}

	/** The chat line for a pouch that could shrink on its next fill, or has worn already. */
	static String pouchWarning(View.PouchView pv)
	{
		String name = pv.name.toLowerCase(java.util.Locale.ENGLISH);
		String repair = "The Dark Mage in the Abyss repairs pouches for free; Astral Contact reaches him from anywhere.";
		if (pv.degraded)
		{
			return pv.capacity <= 0 ? "Your " + name + " is worn through. " + repair
				: "Your " + name + " has worn and holds " + pv.capacity + " of " + pv.full + ". " + repair;
		}
		return "Your " + name + " could shrink on its next fill: it holds " + pv.capacity
			+ (pv.next > 0 ? ", then " + pv.next : ", then it is gone") + ". " + repair;
	}

	/** Teleports in the bank are taken to cost a minute's bank stop: a planning assumption. */
	static final int BANK_STOP_SECONDS = 60;

	/** Costs every path to a store for this account, fastest first, onto its card. */
	static void plan(View.Getting g, long need, Works works, Map<Skill, Integer> levels, List<View.ShopView> shops,
		Travel travel, int[] here)
	{
		g.need = need;
		if (need <= 0)
		{
			return;
		}
		List<Acquire.Offer> offers = new ArrayList<>();
		for (View.ShopView s : shops)
		{
			for (View.WareView w : s.wares)
			{
				if (g.store.equals(w.store))
				{
					offers.add(new Acquire.Offer(s.name, s.region, (long) Math.max(0, w.base) * Math.max(1, w.units)));
					break;
				}
			}
		}
		List<Acquire.Make> makes = new ArrayList<>();
		for (View.WayView w : g.ways)
		{
			if (w.kind == Ways.Kind.MAKE && w.text.startsWith("Make it from"))
			{
				makes.add(new Acquire.Make(w.text, w.skill, w.level));
			}
		}
		if (!Acquire.knows(g.store) && offers.isEmpty() && makes.isEmpty())
		{
			return;
		}
		List<Acquire.Path> paths = Acquire.rank(g.store, need, levels, (store, src) -> pace(works, store, src),
			offers, makes, travel, here, BANK_STOP_SECONDS);
		List<View.PathView> out = new ArrayList<>();
		for (Acquire.Path p : paths)
		{
			View.PathView pv = new View.PathView();
			pv.verb = p.kind.getVerb();
			pv.source = p.kind.getSource();
			pv.text = p.text;
			pv.locked = p.locked != null;
			pv.timed = p.seconds >= 0;
			pv.needs = p.needs;
			pv.figure = p.locked != null ? p.locked : p.seconds >= 0 ? com.codex.ironworks.ui.Format.duration(p.seconds * 1000)
				: p.kills > 0 ? "~" + com.codex.ironworks.ui.Format.amount(p.kills) + " kills" : null;
			pv.basis = p.perWorld > 0 ? com.codex.ironworks.ui.Format.amount(p.perWorld) + " a world, " + p.basis
				: p.perHour > 0 ? com.codex.ironworks.ui.Format.amount(Math.round(p.perHour)) + "/h " + p.basis
				: p.basis;
			pv.travel = p.leg == null ? null : p.leg.say();
			if (p.located())
			{
				pv.spot = new View.Goto(p.x, p.y, p.plane, p.place == null ? p.text : p.place,
					"path:" + g.store + ":" + p.kind.name() + ":" + p.x + "," + p.y);
			}
			out.add(pv);
		}
		g.plan = out;
	}

	/**
	 * For each second ingredient that stops a skill's stores being used up, the fastest ways to get
	 * the units still wanted, as the Sources tab costs them.
	 */
	static Map<String, View.Getting> shortWays(Workshop workshop, Works works, Catalogue.Lookup lookup,
		Map<Skill, Integer> levels, List<View.ShopView> shops, Travel travel, int[] here)
	{
		Map<String, Long> wanted = new HashMap<>();
		for (Workshop.Plan p : workshop.plans())
		{
			for (Workshop.Short s : p.shorts.subList(0, Math.min(View.SHORTS_SHOWN, p.shorts.size())))
			{
				if (!Catalogue.money(s.input))
				{
					wanted.merge(s.input, s.need, Math::max);
				}
			}
		}
		Map<String, View.Getting> out = new HashMap<>();
		wanted.forEach((store, need) ->
		{
			View.Getting g = getting(store, works.icon(store), works, lookup, levels);
			plan(g, need, works, levels, shops, travel, here);
			out.put(store, g);
		});
		return out;
	}

	/** The ways to get more of a store: the player's own paces first, then what they have the levels for. */
	static View.Getting getting(String store, int icon, Works works, Catalogue.Lookup lookup, Map<Skill, Integer> levels)
	{
		View.Getting g = new View.Getting();
		g.store = store;
		g.icon = icon;
		List<View.WayView> ways = new ArrayList<>();
		int herblore = levels.getOrDefault(Skill.HERBLORE, 99);
		for (Ways.Way w : Ways.of(store))
		{
			if (!w.gives(herblore))
			{
				// The herbiboar stops giving low herbs once Herblore passes them.
				continue;
			}
			View.WayView wv = new View.WayView();
			wv.kind = w.kind;
			wv.text = w.text;
			wv.skill = w.skill;
			wv.level = w.level;
			wv.open = (w.skill == null || levels.getOrDefault(w.skill, 99) >= w.level) && w.herbloreOpen(herblore);
			if (w.skill != null && levels.getOrDefault(w.skill, 99) >= w.level && !w.herbloreOpen(herblore))
			{
				wv.skill = Skill.HERBLORE;
				wv.level = Math.max(Herbiboar.HERBLORE, w.herblore.min);
			}
			wv.perHour = pace(works, store, w.kind.getSource());
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
			wv.perHour = pace(works, store, Source.MADE);
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
			double r = pace(works, sv.store, src);
			if (r > fastest)
			{
				fastest = r;
				w.how = src.getLabel().toLowerCase(java.util.Locale.ENGLISH);
			}
		}
		w.perHour = fastest;
		return w;
	}

}
