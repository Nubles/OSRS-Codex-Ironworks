package com.codex.ironworks.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Everything Ironworks keeps for one account: the stores as last seen in each place, every unit
 * gained and used by where it came from and went, day by day and in all, the shops used and the
 * runs made to them. Saved as one blob per RuneScape profile.
 */
public final class Works
{
	public static final int VERSION = 1;
	/** Days kept in detail; older days live on only in the all-time totals. */
	static final int DAYS = 45;
	static final int RUNS = 40;
	static final int SHOPS = 60;
	/** A pack's contents until one has been opened and counted. */
	public static final int PACK = 100;

	int version = VERSION;
	/** The account's ironman varbit: 0 none, 1 ironman, 2 ultimate, 3 hardcore, 4-6 group; -1 not yet read. */
	int account = -1;
	long since;
	/** Milliseconds played in all. */
	long played;
	/** Units of each store in each place, by place name. */
	Map<String, Map<String, Long>> places = new HashMap<>();
	Map<String, Long> seen = new HashMap<>();
	Map<String, Integer> icons = new HashMap<>();
	/** Units the player put into carried containers, such as a coal bag, not yet seen come out. */
	Map<String, Long> sacks = new HashMap<>();
	Map<String, Tally> totals = new HashMap<>();
	/** Days by ISO date. */
	Map<String, Day> days = new HashMap<>();
	List<Shop> shops = new ArrayList<>();
	List<Run> runs = new ArrayList<>();
	/** Units in one pack, by the pack's name, as counted when one was opened. */
	Map<String, Integer> packs = new HashMap<>();
	/** How fast each store comes in one way, by "store|SOURCE". */
	Map<String, Rate> rates = new HashMap<>();
	/** Every item in each place, by spot name, for finding anything the account owns. */
	Map<String, Holding> holdings = new HashMap<>();
	/** Names of the items held and lost, so they can be found while logged out. */
	Map<Integer, String> names = new HashMap<>();
	/** The last death's items not yet taken back, or null. */
	Death death;
	/** Guide steps ticked by hand or seen done in the moment, by step key. */
	List<String> ticked = new ArrayList<>();
	/** True while the player follows the guide, so the game points the way. */
	boolean following;

	public Works()
	{
	}

	Works(long now)
	{
		since = now;
	}

	// ---- places ----

	public void place(Place place, Map<String, Long> units, long now)
	{
		Map<String, Long> clean = new HashMap<>();
		units.forEach((k, v) ->
		{
			if (k != null && v != null && v > 0)
			{
				clean.put(k, v);
			}
		});
		places.put(place.name(), clean);
		seen.put(place.name(), now);
	}

	public Map<String, Long> place(Place place)
	{
		Map<String, Long> m = places.get(place.name());
		return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
	}

	public boolean seen(Place place)
	{
		return places.containsKey(place.name());
	}

	public long seenAt(Place place)
	{
		return seen.getOrDefault(place.name(), 0L);
	}

	public long stock(String store)
	{
		long sum = 0;
		for (Map<String, Long> m : places.values())
		{
			sum += m.getOrDefault(store, 0L);
		}
		return sum;
	}

	/** Every store held anywhere, with how many units. */
	public Map<String, Long> stocks()
	{
		Map<String, Long> sum = new HashMap<>();
		places.values().forEach(m -> m.forEach((k, v) -> sum.merge(k, v, Long::sum)));
		return sum;
	}

	/** Every store with stock now or with anything recorded. */
	public Set<String> stores()
	{
		Set<String> all = new HashSet<>(stocks().keySet());
		all.addAll(totals.keySet());
		return all;
	}

	// ---- holdings ----

	/** Everything in one place, item by item, as seen now. */
	public void hold(Spot spot, Map<Integer, Long> items, Map<Integer, String> itemNames, long now)
	{
		holdings.put(spot.name(), new Holding(items, now));
		name(items.keySet(), itemNames);
	}

	private void name(Iterable<Integer> ids, Map<Integer, String> itemNames)
	{
		for (Integer id : ids)
		{
			String n = itemNames.get(id);
			if (n != null && !n.isEmpty())
			{
				names.put(id, n);
			}
		}
	}

	/** One place as last seen, or null when never seen. */
	public Holding holding(Spot spot)
	{
		return holdings.get(spot.name());
	}

	public String itemName(int id)
	{
		return names.get(id);
	}

	// ---- death ----

	/** The player died and lost these items; a death not yet reclaimed before it is gone for good. */
	public void died(Map<Integer, Long> items, Map<Integer, String> itemNames, int world, long now)
	{
		if (items.isEmpty())
		{
			return;
		}
		death = new Death(items, now, played, world);
		name(items.keySet(), itemNames);
	}

	/**
	 * Items came into the inventory; those the last death lost are taken back.
	 *
	 * @return true when the death changed
	 */
	public boolean regained(Map<Integer, Long> gains)
	{
		if (death == null || gains.isEmpty())
		{
			return false;
		}
		boolean changed = false;
		boolean settled = false;
		for (Map.Entry<Integer, Long> e : gains.entrySet())
		{
			if (e.getValue() > 0 && death.items.containsKey(e.getKey()))
			{
				changed = true;
				settled = death.back(e.getKey(), e.getValue());
			}
		}
		if (settled)
		{
			death = null;
		}
		return changed;
	}

	public Death getDeath()
	{
		return death;
	}

	public void forgetDeath()
	{
		death = null;
	}

	// ---- guide ----

	/** The guide steps ticked by hand or in the moment. */
	public java.util.Set<String> ticked()
	{
		return new java.util.HashSet<>(ticked);
	}

	public boolean isFollowing()
	{
		return following;
	}

	public void setFollowing(boolean following)
	{
		this.following = following;
	}

	/** Ticks or unticks a guide step; returns true when that changed anything. */
	public boolean tick(String key, boolean done)
	{
		if (key == null || done == ticked.contains(key))
		{
			return false;
		}
		if (done)
		{
			ticked.add(key);
		}
		else
		{
			ticked.remove(key);
		}
		return true;
	}

	// ---- rates ----

	/** Times the gains that come from the player's own work or loot, store by store. */
	public void rate(List<Flow> flows, long now)
	{
		for (Flow f : flows)
		{
			if (f.isGain() && (f.source == Source.GATHERED || f.source == Source.FARMED || f.source == Source.MADE
				|| f.source == Source.LOOTED))
			{
				rates.computeIfAbsent(f.store + "|" + f.source.name(), k -> new Rate()).add(f.units, now);
			}
		}
	}

	/** The pace a store comes in one way, or null when never seen. */
	public Rate rate(String store, Source source)
	{
		return rates.get(store + "|" + source.name());
	}

	// ---- pictures ----

	/** Notes the item a store is pictured by; the model item (a full potion, a clean herb) wins. */
	public void icon(String store, int item, boolean model)
	{
		if (model || !icons.containsKey(store))
		{
			icons.put(store, item);
		}
	}

	public int icon(String store)
	{
		return icons.getOrDefault(store, -1);
	}

	// ---- flows ----

	public Map<String, Long> getSacks()
	{
		return sacks;
	}

	public void record(List<Flow> flows, LocalDate date)
	{
		if (flows.isEmpty())
		{
			return;
		}
		Day day = day(date);
		for (Flow f : flows)
		{
			if (f.units <= 0)
			{
				continue;
			}
			totals.computeIfAbsent(f.store, k -> new Tally()).add(f);
			day.tally(f.store).add(f);
		}
	}

	public void played(LocalDate date, long ms)
	{
		if (ms <= 0)
		{
			return;
		}
		day(date).played += ms;
		played += ms;
	}

	public long getPlayed()
	{
		return played;
	}

	Day day(LocalDate date)
	{
		return days.computeIfAbsent(date.toString(), k -> new Day());
	}

	/**
	 * The last few calendar days summed, today included.
	 *
	 * @param count days to sum; 0 or less for all time
	 */
	public Window window(LocalDate today, int count)
	{
		if (count <= 0)
		{
			Map<String, Tally> all = new HashMap<>();
			totals.forEach((k, t) -> all.computeIfAbsent(k, x -> new Tally()).add(t));
			return new Window(played, all, 0);
		}
		long ms = 0;
		Map<String, Tally> sum = new HashMap<>();
		for (int i = 0; i < count; i++)
		{
			Day d = days.get(today.minusDays(i).toString());
			if (d == null)
			{
				continue;
			}
			ms += d.played;
			d.stores.forEach((k, t) -> sum.computeIfAbsent(k, x -> new Tally()).add(t));
		}
		return new Window(ms, sum, count);
	}

	/** Hours played a day on average over the last fortnight, to turn hours of play into days. */
	public double hoursPerDay(LocalDate today)
	{
		return window(today, 14).played / 3_600_000.0 / 14;
	}

	// ---- shops ----

	public List<Shop> getShops()
	{
		return Collections.unmodifiableList(shops);
	}

	public List<Run> getRuns()
	{
		return Collections.unmodifiableList(runs);
	}

	public Shop shop(String key)
	{
		for (Shop s : shops)
		{
			if (s.key().equals(key))
			{
				return s;
			}
		}
		return null;
	}

	/** A shop's window opened: counts the visit and adds it to the run under way, or starts one. */
	public Shop visit(String name, int region, int world, long now)
	{
		Shop shop = shop(Shop.key(name, region));
		if (shop == null)
		{
			shop = new Shop(name, region, now);
			shops.add(shop);
		}
		shop.visits++;
		shop.last = now;
		Run run = run(now);
		run.visits++;
		run.worlds.add(world);
		if (!run.shops.contains(shop.key()))
		{
			run.shops.add(shop.key());
		}
		trimShops();
		return shop;
	}

	/** The run under way, or a new one if the last ended long enough ago. */
	private Run run(long now)
	{
		Run last = runs.isEmpty() ? null : runs.get(runs.size() - 1);
		if (last == null || now - last.end > Run.GAP)
		{
			last = new Run(now);
			runs.add(last);
			while (runs.size() > RUNS)
			{
				runs.remove(0);
			}
		}
		last.end = Math.max(last.end, now);
		return last;
	}

	/** The shop shows this much of an item in stock. */
	public void stock(Shop shop, int item, String name, int world, int stock, long now)
	{
		shop.ware(item, name).observe(world, stock, now);
		Run run = runs.isEmpty() ? null : runs.get(runs.size() - 1);
		if (run != null && now - run.end <= Run.GAP)
		{
			run.end = Math.max(run.end, now);
		}
	}

	/** Items bought at a shop, and what was paid, by currency store. */
	public void purchase(Shop shop, int item, String name, long qty, Map<String, Long> paid, int world, long now)
	{
		if (qty <= 0)
		{
			return;
		}
		Ware ware = shop.ware(item, name);
		ware.bought(qty, paid);
		long coins = paid.getOrDefault("Coins", 0L);
		int after = ware.expected(world, now);
		if (coins > 0 && after >= 0)
		{
			ware.price(after + (int) Math.min(Integer.MAX_VALUE / 2, qty), qty, coins);
		}
		shop.last = now;
		Run run = run(now);
		run.bought.merge(name == null ? String.valueOf(item) : name, qty, Long::sum);
		paid.forEach((k, v) -> run.spent.merge(k, v, Long::sum));
	}

	/** Counts what a pack held, once one has been opened. */
	public void pack(String packName, int units)
	{
		if (packName != null && units > 0)
		{
			packs.put(packName, units);
		}
	}

	/** Units in one pack of this name, as counted, or the usual hundred. */
	public int packSize(String packName)
	{
		return packs.getOrDefault(packName, PACK);
	}

	private void trimShops()
	{
		while (shops.size() > SHOPS)
		{
			Shop oldest = shops.get(0);
			for (Shop s : shops)
			{
				if (s.last < oldest.last)
				{
					oldest = s;
				}
			}
			shops.remove(oldest);
		}
	}

	// ---- account ----

	public int getAccount()
	{
		return account;
	}

	public void setAccount(int value)
	{
		account = value;
	}

	public long getSince()
	{
		return since;
	}

	/** Forgets days older than the detail kept. */
	public void trim(LocalDate today)
	{
		String oldest = today.minusDays(DAYS - 1).toString();
		days.keySet().removeIf(k -> k.compareTo(oldest) < 0);
	}

	// ---- saving ----

	/** A read save, and whether it was damaged so the save as it was should be kept aside. */
	public static final class Loaded
	{
		public final Works works;
		public final boolean damaged;

		Loaded(Works works, boolean damaged)
		{
			this.works = works;
			this.damaged = damaged;
		}
	}

	public String write(Gson gson)
	{
		return gson.toJson(this);
	}

	public static Loaded read(Gson gson, String saved, long now)
	{
		if (saved == null || saved.trim().isEmpty())
		{
			return new Loaded(new Works(now), false);
		}
		try
		{
			Works whole = gson.fromJson(saved, Works.class);
			if (whole != null)
			{
				whole.repair(now);
				return new Loaded(whole, Unread.any(gson, saved, whole));
			}
		}
		catch (RuntimeException e)
		{
			// Read it field by field below.
		}
		Works w = new Works(now);
		JsonReader in = new JsonReader(new StringReader(saved));
		in.setLenient(true);
		try
		{
			if (in.peek() == JsonToken.BEGIN_OBJECT)
			{
				in.beginObject();
				while (in.hasNext())
				{
					String name = in.nextName();
					w.field(gson, name, gson.fromJson(in, JsonElement.class));
				}
			}
		}
		catch (IOException | RuntimeException e)
		{
			// A save cut short keeps every field before the cut.
		}
		return new Loaded(w.repair(now), true);
	}

	private static final Type PLACES = new TypeToken<Map<String, Map<String, Long>>>()
	{
	}.getType();
	private static final Type COUNTS = new TypeToken<Map<String, Long>>()
	{
	}.getType();
	private static final Type ICONS = new TypeToken<Map<String, Integer>>()
	{
	}.getType();
	private static final Type TALLIES = new TypeToken<Map<String, Tally>>()
	{
	}.getType();
	private static final Type RATES = new TypeToken<Map<String, Rate>>()
	{
	}.getType();
	private static final Type HOLDINGS = new TypeToken<Map<String, Holding>>()
	{
	}.getType();
	private static final Type NAMES = new TypeToken<Map<Integer, String>>()
	{
	}.getType();
	private static final Type DAYS_TYPE = new TypeToken<Map<String, Day>>()
	{
	}.getType();

	/** Sets one field from a damaged save; a field that does not read is left as new. */
	private void field(Gson gson, String name, JsonElement value)
	{
		try
		{
			switch (name)
			{
				case "account":
					Long a = Json.whole(value);
					account = a == null ? -1 : (int) Math.max(-1, Math.min(10, a));
					break;
				case "since":
					Long s = Json.whole(value);
					since = s == null ? since : s;
					break;
				case "played":
					Long p = Json.whole(value);
					played = p == null ? 0 : p;
					break;
				case "places":
					places = gson.fromJson(value, PLACES);
					break;
				case "seen":
					seen = gson.fromJson(value, COUNTS);
					break;
				case "icons":
					icons = gson.fromJson(value, ICONS);
					break;
				case "sacks":
					sacks = gson.fromJson(value, COUNTS);
					break;
				case "totals":
					totals = gson.fromJson(value, TALLIES);
					break;
				case "days":
					days = readDays(gson, value);
					break;
				case "shops":
					shops = readList(gson, value, Shop.class);
					break;
				case "runs":
					runs = readList(gson, value, Run.class);
					break;
				case "packs":
					packs = gson.fromJson(value, ICONS);
					break;
				case "rates":
					rates = gson.fromJson(value, RATES);
					break;
				case "holdings":
					holdings = gson.fromJson(value, HOLDINGS);
					break;
				case "names":
					names = gson.fromJson(value, NAMES);
					break;
				case "death":
					death = gson.fromJson(value, Death.class);
					break;
				case "following":
					following = value.getAsBoolean();
					break;
				case "ticked":
					ticked = readList(gson, value, String.class);
					break;
				default:
					break;
			}
		}
		catch (RuntimeException e)
		{
			// This field stays as new; the others still read.
		}
	}

	private static Map<String, Day> readDays(Gson gson, JsonElement value)
	{
		Map<String, Day> out = new HashMap<>();
		if (value == null || !value.isJsonObject())
		{
			return out;
		}
		for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet())
		{
			try
			{
				out.put(e.getKey(), gson.fromJson(e.getValue(), Day.class));
			}
			catch (RuntimeException ignored)
			{
				// One unreadable day does not spoil the rest.
			}
		}
		return out;
	}

	private static <T> List<T> readList(Gson gson, JsonElement value, Class<T> type)
	{
		List<T> out = new ArrayList<>();
		if (value == null || !value.isJsonArray())
		{
			return out;
		}
		for (JsonElement e : value.getAsJsonArray())
		{
			try
			{
				out.add(gson.fromJson(e, type));
			}
			catch (RuntimeException ignored)
			{
				// One unreadable entry does not spoil the rest.
			}
		}
		return out;
	}

	/** Sets right whatever a damaged or older save left missing or impossible. */
	Works repair(long now)
	{
		version = VERSION;
		if (since <= 0)
		{
			since = now;
		}
		played = Math.max(0, played);
		Map<String, Map<String, Long>> p = new HashMap<>();
		if (places != null)
		{
			places.forEach((k, m) ->
			{
				if (k != null && m != null)
				{
					p.put(k, counts(m));
				}
			});
		}
		places = p;
		seen = counts(seen);
		sacks = counts(sacks);
		Map<String, Integer> ic = new HashMap<>();
		if (icons != null)
		{
			icons.forEach((k, v) ->
			{
				if (k != null && v != null && v >= 0)
				{
					ic.put(k, v);
				}
			});
		}
		icons = ic;
		Map<String, Integer> pk = new HashMap<>();
		if (packs != null)
		{
			packs.forEach((k, v) ->
			{
				if (k != null && v != null && v > 0)
				{
					pk.put(k, v);
				}
			});
		}
		packs = pk;
		Map<String, Tally> t = new HashMap<>();
		if (totals != null)
		{
			totals.forEach((k, v) ->
			{
				if (k != null && v != null && !v.repair().isEmpty())
				{
					t.put(k, v);
				}
			});
		}
		totals = t;
		Map<String, Day> d = new TreeMap<>();
		if (days != null)
		{
			days.forEach((k, v) ->
			{
				if (k != null && v != null && k.matches("\\d{4}-\\d{2}-\\d{2}"))
				{
					d.put(k, v.repair());
				}
			});
		}
		days = new HashMap<>(d);
		List<Shop> s = new ArrayList<>();
		if (shops != null)
		{
			for (Shop shop : shops)
			{
				if (shop != null)
				{
					s.add(shop.repair());
				}
			}
		}
		shops = s;
		List<Run> r = new ArrayList<>();
		if (runs != null)
		{
			for (Run run : runs)
			{
				if (run != null)
				{
					r.add(run.repair());
				}
			}
		}
		runs = r;
		Map<String, Rate> rt = new HashMap<>();
		if (rates != null)
		{
			rates.forEach((k, v) ->
			{
				if (k != null && v != null)
				{
					rt.put(k, v.repair());
				}
			});
		}
		rates = rt;
		Map<String, Holding> h = new HashMap<>();
		if (holdings != null)
		{
			holdings.forEach((k, v) ->
			{
				if (k != null && v != null)
				{
					try
					{
						Spot.valueOf(k);
						h.put(k, v.repair());
					}
					catch (IllegalArgumentException ignored)
					{
						// A place this version does not know.
					}
				}
			});
		}
		holdings = h;
		List<String> tk = new ArrayList<>();
		if (ticked != null)
		{
			for (String k : ticked)
			{
				if (k != null && !k.isEmpty() && !tk.contains(k))
				{
					tk.add(k);
				}
			}
		}
		ticked = tk;
		death = death == null ? null : death.repair();
		if (death != null && death.items.isEmpty())
		{
			death = null;
		}
		// Only names of items still held or lost are kept.
		Set<Integer> used = new HashSet<>();
		holdings.values().forEach(x -> used.addAll(x.items.keySet()));
		if (death != null)
		{
			used.addAll(death.items.keySet());
		}
		Map<Integer, String> n = new HashMap<>();
		if (names != null)
		{
			names.forEach((k, v) ->
			{
				if (k != null && v != null && used.contains(k))
				{
					n.put(k, v);
				}
			});
		}
		names = n;
		return this;
	}

	private static Map<String, Long> counts(Map<String, Long> m)
	{
		Map<String, Long> c = new HashMap<>();
		if (m != null)
		{
			m.forEach((k, v) ->
			{
				if (k != null && v != null && v > 0)
				{
					c.put(k, v);
				}
			});
		}
		return c;
	}
}
