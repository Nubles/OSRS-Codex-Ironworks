package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * The fastest way for this account to get more of a store. Every path (a spot to gather at, a
 * monster that drops it, a shop that sells it, a recipe that makes it) is costed in time to cover
 * the shortfall: the trip there, from where the player stands with the teleports they own (Codex
 * Wayfinder's atlas), plus the work at the player's own measured pace, or else the OSRS Wiki's
 * rate. Paths the player's levels do not reach are ranked last; drops with no pace yet are given
 * in kills, from the wiki's drop rate.
 *
 * <p>Rates and drop rates come from the OSRS Wiki (money making guides, item and monster pages);
 * spots come from RuneLite's own map data and Shortest Path's travel data.
 */
public final class Acquire
{
	/** A shop visit: hop, open, buy the world's stock. A planning assumption, not a game fact. */
	public static final int WORLD_SECONDS = 30;

	public enum Kind
	{
		GATHER("Gather", Source.GATHERED),
		DROP("Kill", Source.LOOTED),
		SHOP("Buy", Source.BOUGHT),
		MAKE("Make", Source.MADE),
		FARM("Farm", Source.FARMED);

		private final String verb;
		private final Source source;

		Kind(String verb, Source source)
		{
			this.verb = verb;
			this.source = source;
		}

		public String getVerb()
		{
			return verb;
		}

		public Source getSource()
		{
			return source;
		}
	}

	/** A known way to get a store, with the wiki's figures. */
	public static final class Lead
	{
		final Kind kind;
		final String text;
		final String place;
		final int x;
		final int y;
		final int plane;
		/** Units an hour, from the wiki; 0 when it gives none. */
		final double perHour;
		/** Units a kill on average, from the wiki's drop table; 0 for anything but a drop. */
		final double perKill;
		/** Units one shop world holds; 0 for anything but a shop. */
		final long perWorld;
		final Skill skill;
		final int level;
		/** A quest or unlock it asks for, in words, or null. */
		final String needs;
		/** The Herblore levels it gives the store at, for the herbiboar; null for any other lead. */
		Herbiboar.Range herblore;

		Lead(Kind kind, String text, String place, int x, int y, int plane, double perHour, double perKill, long perWorld,
			Skill skill, int level, String needs)
		{
			this.kind = kind;
			this.text = text;
			this.place = place;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.perHour = perHour;
			this.perKill = perKill;
			this.perWorld = perWorld;
			this.skill = skill;
			this.level = level;
			this.needs = needs;
		}
	}

	/** A shop the player has bought the store at, as Ironworks learned it. */
	public static final class Offer
	{
		public final String shop;
		public final int region;
		/** Units of the store a full world's stock holds; 0 while unknown. */
		public final long perWorld;

		public Offer(String shop, int region, long perWorld)
		{
			this.shop = shop;
			this.region = region;
			this.perWorld = perWorld;
		}
	}

	/** A recipe that makes the store, as the Make tab knows it. */
	public static final class Make
	{
		public final String text;
		public final Skill skill;
		public final int level;

		public Make(String text, Skill skill, int level)
		{
			this.text = text;
			this.skill = skill;
			this.level = level;
		}
	}

	/** One path, costed. */
	public static final class Path
	{
		public Kind kind;
		public String text;
		public String place;
		public int x;
		public int y;
		public int plane = -1;
		/** Units an hour this path is costed at, or 0. */
		public double perHour;
		/** Where the rate comes from: "your pace", "wiki rate". */
		public String basis;
		/** Seconds to cover the shortfall, the trip included, or -1 when it cannot be timed. */
		public long seconds = -1;
		/** Units one shop world holds, for a shop; 0 otherwise. */
		public long perWorld;
		/** Kills the shortfall takes, for a drop with no pace yet; 0 otherwise. */
		public long kills;
		public Travel.Leg leg;
		/** The level or unlock missing, in words, or null when open. */
		public String locked;
		/** A quest or unlock to check, in words, or null. */
		public String needs;

		public boolean located()
		{
			return plane >= 0;
		}
	}

	private static final Map<String, List<Lead>> LEADS = new HashMap<>();

	static
	{
		// Wine of Zamorak: the Chaos Temple north-west of Goblin Village; the wiki's guide makes 204 an hour.
		lead("Wine of Zamorak", new Lead(Kind.GATHER, "Telegrab it in the Chaos Temple", "Goblin Village",
			2956, 3505, 0, 204, 0, 0, Skill.MAGIC, 33, null));
		// Snape grass: six spawns on Waterbirth Island; the wiki's guide collects 950 an hour.
		lead("Snape grass", new Lead(Kind.GATHER, "Pick it on Waterbirth Island", "Waterbirth Island",
			2551, 3755, 0, 950, 0, 0, null, 0, null));
		lead("Snape grass", new Lead(Kind.FARM, "Grow it in allotments", null, 0, 0, -1, 0, 0, 0, Skill.FARMING, 61, null));
		// Red spiders' eggs: the Varrock Sewers swamp; the wiki's guide collects 390 an hour.
		lead("Red spiders' eggs", new Lead(Kind.GATHER, "Pick them up in the Varrock Sewers", "Varrock Sewers",
			3236, 3458, 0, 390, 0, 0, null, 0, null));
		// White berries: Isafdar, north-west of Lletya; the wiki's guide collects 400 an hour.
		lead("White berries", new Lead(Kind.GATHER, "Pick them in Isafdar", "Lletya",
			2352, 3162, 0, 400, 0, 0, Skill.AGILITY, 56, "Regicide and Mourning's End Part I started"));
		lead("White berries", new Lead(Kind.FARM, "Grow a whiteberry bush", null, 0, 0, -1, 0, 0, 0, Skill.FARMING, 59, null));
		// Mort myre fungus: Bloom on the logs north of Ver Sinhaza; the wiki's guide collects 1,620 an hour.
		lead("Mort myre fungus", new Lead(Kind.GATHER, "Cast Bloom on the logs by Ver Sinhaza", "Ver Sinhaza",
			3649, 3230, 0, 1620, 0, 0, null, 0, "Nature Spirit and a silver sickle (b)"));
		// Feathers: Gerrant's Fishy Business restocks to 100 packs of 100 feathers.
		lead("Feather", new Lead(Kind.SHOP, "Buy feather packs at Gerrant's Fishy Business", "Port Sarim",
			3014, 3222, 0, 0, 0, 100L * 100, null, 0, null));
		// Chickens drop 5 feathers 64/128 and 15 feathers 32/128: 6.25 a kill.
		lead("Feather", new Lead(Kind.DROP, "Kill chickens", "Lumbridge",
			3243, 3208, 0, 0, 5 * 64 / 128.0 + 15 * 32 / 128.0, 0, null, 0, null));
		// Limpwurt root: hill giants drop one 11/128.
		lead("Limpwurt root", new Lead(Kind.DROP, "Kill hill giants in Edgeville Dungeon", "Edgeville Dungeon",
			3096, 3469, 0, 0, 11 / 128.0, 0, null, 0, null));
		lead("Limpwurt root", new Lead(Kind.FARM, "Grow it in flower patches", null, 0, 0, -1, 0, 0, 0, Skill.FARMING, 26, null));
		// Grapes: the Aldarin vineyard foreman pays 5 noted a full barrel; the wiki gives no rate, so it is
		// timed only by the player's own barrels. Faustus' Fruit and Veg holds 5 (OSRS Wiki: Grapes).
		lead("Grapes", new Lead(Kind.GATHER, "Fill the foreman's grape barrel in Aldarin", "Aldarin",
			0, 0, -1, 0, 0, 0, null, 0, Vineyard.QUEST));
		lead("Grapes", new Lead(Kind.SHOP, "Buy them at Faustus' Fruit and Veg in Aldarin", "Aldarin",
			0, 0, -1, 0, 0, 5, null, 0, null));
		lead("Grapes", new Lead(Kind.FARM, "Grow them at the Hosidius Vinery", null, 0, 0, -1, 0, 0, 0, Skill.FARMING, 36, null));
		// Herbs from the herbiboar: the middle trail's start, from RuneLite's Herbiboar plugin. Timed only
		// by the player's own trails; the wiki's catch rate is not split into herbs of each kind.
		for (String herb : Herbiboar.herbs())
		{
			Lead l = new Lead(Kind.GATHER, "Hunt the herbiboar on Fossil Island", "Fossil Island",
				Herbiboar.START_X, Herbiboar.START_Y, 0, 0, 0, 0, Skill.HUNTER, Herbiboar.HUNTER, Herbiboar.QUEST);
			l.herblore = Herbiboar.range(herb);
			lead(herb, l);
		}
		// Runes crafted from pure essence at their altar: timed only by the player's own trips, as the
		// wiki gives no one rate (it hangs on the route, the pouches and the level's multiples).
		for (Recipe r : Recipes.all())
		{
			if (r.skill == Skill.RUNECRAFT)
			{
				lead(r.product, new Lead(Kind.MAKE, "Craft them at the " + altar(r.product), null, 0, 0, -1, 0, 0, 0,
					Skill.RUNECRAFT, r.level, null));
			}
		}
	}

	/** The altar a rune is crafted at from pure essence: blood runes at the True Blood Altar. */
	public static String altar(String rune)
	{
		String kind = rune.endsWith(" rune") ? rune.substring(0, rune.length() - " rune".length()) : rune;
		return ("Blood".equals(kind) ? "True Blood" : kind) + " Altar";
	}

	private Acquire()
	{
	}

	private static void lead(String store, Lead l)
	{
		LEADS.computeIfAbsent(store.toLowerCase(Locale.ENGLISH), k -> new ArrayList<>()).add(l);
	}

	/** The known leads for a store; empty when there are none. */
	static List<Lead> leads(String store)
	{
		List<Lead> l = LEADS.get(store.toLowerCase(Locale.ENGLISH));
		return l == null ? Collections.emptyList() : Collections.unmodifiableList(l);
	}

	public static boolean knows(String store)
	{
		return LEADS.containsKey(store.toLowerCase(Locale.ENGLISH));
	}

	/** The centre tile of a map square, for a shop known only by its square. */
	static int[] centre(int region)
	{
		return new int[]{((region >> 8) << 6) + 32, ((region & 0xff) << 6) + 32, 0};
	}

	/** The player's own pace one way, units an hour, or 0. */
	public interface Paces
	{
		double perHour(String store, Source source);
	}

	/**
	 * Every path to a store, fastest first.
	 *
	 * @param store the store
	 * @param need units still wanted
	 * @param levels the player's levels; a skill not in it counts as reached
	 * @param paces the player's own paces
	 * @param offers shops the player has bought it at
	 * @param travel Codex Wayfinder's atlas, or null
	 * @param here where the player stands, or null
	 * @param fetchSeconds what fetching a banked teleport is taken to cost
	 */
	public static List<Path> rank(String store, long need, Map<Skill, Integer> levels, Paces paces, List<Offer> offers,
		Travel travel, int[] here, int fetchSeconds)
	{
		return rank(store, need, levels, paces, offers, Collections.emptyList(), travel, here, fetchSeconds);
	}

	/** As above, with the recipes that make it; a recipe is timed at the player's own making pace. */
	public static List<Path> rank(String store, long need, Map<Skill, Integer> levels, Paces paces, List<Offer> offers,
		List<Make> makes, Travel travel, int[] here, int fetchSeconds)
	{
		List<Path> out = new ArrayList<>();
		long want = Math.max(1, need);
		boolean shopLead = false;
		for (Lead l : leads(store))
		{
			Path p = new Path();
			p.kind = l.kind;
			p.text = l.text;
			p.place = l.place;
			p.needs = l.needs;
			if (l.plane >= 0)
			{
				p.x = l.x;
				p.y = l.y;
				p.plane = l.plane;
			}
			int herblore = levels.getOrDefault(Skill.HERBLORE, 99);
			if (l.herblore != null && herblore > l.herblore.max)
			{
				// Past the Herblore levels the herbiboar gives this herb at.
				continue;
			}
			if (l.skill != null && levels.getOrDefault(l.skill, 99) < l.level)
			{
				p.locked = skillName(l.skill) + " " + l.level;
			}
			else if (l.herblore != null && herblore < Math.max(Herbiboar.HERBLORE, l.herblore.min))
			{
				p.locked = skillName(Skill.HERBLORE) + " " + Math.max(Herbiboar.HERBLORE, l.herblore.min);
			}
			double own = paces == null ? 0 : paces.perHour(store, l.kind.getSource());
			if (own > 0)
			{
				p.perHour = own;
				p.basis = "your pace";
			}
			else if (l.perHour > 0)
			{
				p.perHour = l.perHour;
				p.basis = "wiki rate";
			}
			if (l.kind == Kind.SHOP)
			{
				shopLead = true;
				p.basis = "wiki stock";
			}
			cost(p, want, l.perWorld, l.perKill, travel, here, fetchSeconds);
			out.add(p);
		}
		if (offers != null)
		{
			for (Offer o : offers)
			{
				if (shopLead && o.shop != null && leadsShop(store, o.shop))
				{
					continue;
				}
				Path p = new Path();
				p.kind = Kind.SHOP;
				p.text = "Buy at " + o.shop;
				p.place = o.shop;
				int[] c = centre(o.region);
				p.x = c[0];
				p.y = c[1];
				p.plane = c[2];
				p.basis = o.perWorld > 0 ? "stock you saw" : null;
				cost(p, want, o.perWorld, 0, travel, here, fetchSeconds);
				out.add(p);
			}
		}
		if (makes != null)
		{
			for (Make m : makes)
			{
				Path p = new Path();
				p.kind = Kind.MAKE;
				p.text = m.text;
				if (m.skill != null && levels.getOrDefault(m.skill, 99) < m.level)
				{
					p.locked = skillName(m.skill) + " " + m.level;
				}
				double own = paces == null ? 0 : paces.perHour(store, Source.MADE);
				if (own > 0)
				{
					p.perHour = own;
					p.basis = "your pace";
				}
				cost(p, want, 0, 0, travel, here, fetchSeconds);
				out.add(p);
			}
		}
		out.sort(Acquire::compare);
		return out;
	}

	private static boolean leadsShop(String store, String shop)
	{
		for (Lead l : leads(store))
		{
			if (l.kind == Kind.SHOP && l.text.contains(shop))
			{
				return true;
			}
		}
		return false;
	}

	private static void cost(Path p, long want, long perWorld, double perKill, Travel travel, int[] here, int fetchSeconds)
	{
		p.leg = travel == null || !p.located() ? null : travel.to(here, p.x, p.y, p.plane, fetchSeconds);
		long trip = p.leg == null ? 0 : p.leg.seconds;
		if (p.kind == Kind.SHOP && perWorld > 0)
		{
			long worlds = (want + perWorld - 1) / perWorld;
			p.perWorld = perWorld;
			p.seconds = trip + worlds * WORLD_SECONDS;
			p.perHour = perWorld * 3600.0 / WORLD_SECONDS;
			return;
		}
		if (p.perHour > 0)
		{
			p.seconds = trip + Math.round(want / p.perHour * 3600);
			return;
		}
		if (perKill > 0)
		{
			p.kills = (long) Math.ceil(want / perKill);
			p.basis = "wiki drop rate";
		}
	}

	/** Open before locked; timed before untimed, quickest first; then fewest kills. */
	static int compare(Path a, Path b)
	{
		if ((a.locked == null) != (b.locked == null))
		{
			return a.locked == null ? -1 : 1;
		}
		if ((a.seconds >= 0) != (b.seconds >= 0))
		{
			return a.seconds >= 0 ? -1 : 1;
		}
		if (a.seconds >= 0)
		{
			return Long.compare(a.seconds, b.seconds);
		}
		if ((a.kills > 0) != (b.kills > 0))
		{
			return a.kills > 0 ? -1 : 1;
		}
		return Long.compare(a.kills, b.kills);
	}

	private static String skillName(Skill s)
	{
		String n = s.getName();
		return n.isEmpty() ? n : n.substring(0, 1).toUpperCase(Locale.ENGLISH) + n.substring(1).toLowerCase(Locale.ENGLISH);
	}
}
