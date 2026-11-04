package com.codex.ironworks.ui;

import com.codex.ironworks.core.Altar;
import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Summary;
import com.codex.ironworks.core.Spot;
import com.codex.ironworks.core.Tally;
import com.codex.ironworks.core.Ways;
import com.codex.ironworks.core.Workshop;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Everything the panel shows, worked out on the client thread and handed over whole, so the panel
 * never reads the plugin's live state while the game changes it.
 */
public final class View
{
	public boolean loggedIn;
	/** The ironman varbit: 0 none, 1 ironman, 2 ultimate, 3 hardcore, 4 group, 5 hardcore group, 6 unranked group; -1 unknown. */
	public int account = -1;
	public List<StoreView> stores = Collections.emptyList();
	public Summary today;
	public Summary week;
	public Summary all;
	public List<ShopView> shops = Collections.emptyList();
	public List<RunView> runs = Collections.emptyList();
	public List<Want> wants = Collections.emptyList();
	public double target = 10;
	public double warn = 2;
	public double hoursPerDay;
	/** Play over the last week, which the pace is measured from. */
	public long playedWeek;
	public boolean bankSeen;
	public long now;
	/** The picture of each store, by store. */
	public Map<String, Integer> icons = Collections.emptyMap();
	/** Each store's view, by store, for the tooltips. */
	public Map<String, StoreView> byStore = Collections.emptyMap();

	/** What the stores could become, at the player's levels. */
	public Workshop workshop = Workshop.empty();
	public Map<Skill, Integer> levels = Collections.emptyMap();
	public Map<Skill, Integer> xp = Collections.emptyMap();
	public Altar altar = Altar.GILDED;
	/** Second ingredients shown short on each skill's Make card. */
	public static final int SHORTS_SHOWN = 2;
	/** The recipe picked for a store on the Make tab, by store. */
	public Map<String, String> methods = Collections.emptyMap();
	/** The fastest ways to get more of each second ingredient the Make tab is short of, by store. */
	public Map<String, Getting> shortWays = Collections.emptyMap();

	/** The route and where the player is on it. */
	public GuideView guide = new GuideView();

	/** Every item the account owns, wherever it is kept, by name. */
	public List<Found> found = Collections.emptyList();
	public List<SpotView> spots = Collections.emptyList();
	/** The last death not yet taken back, or null. */
	public DeathView death;

	/** The ways to get more of each store, by store. */
	public Map<String, Getting> getting = Collections.emptyMap();
	/** Codex Wayfinder is on, so places can have a Route button. */
	public boolean wayfinder;
	/** Coins the shopping list costs, as far as prices are known. */
	public long listCoins;
	/** Coins held, everywhere. */
	public long coins;

	/** One thing owned, with where it is. */
	public static final class Found
	{
		public String name;
		public int icon = -1;
		public long total;
		/** Counted in doses, for potions, across every dose size. */
		public boolean doses;
		public Map<Spot, Long> where = Collections.emptyMap();
		/** The store it belongs to, or null. */
		public String store;
	}

	/** One place things are kept. */
	public static final class SpotView
	{
		public Spot spot;
		public long at;
		public int kinds;
		public long worth;
		/** The most valuable items in it, by item id, with quantities. */
		public List<long[]> top = Collections.emptyList();
	}

	/** The last death's items, still to take back. */
	public static final class DeathView
	{
		public long at;
		public int world;
		/** Play time left at the grave, or 0 when they wait at Death's Office. */
		public long graveLeft;
		public long worth;
		/** Item id and quantity, most valuable first. */
		public List<long[]> items = Collections.emptyList();
		public List<String> names = Collections.emptyList();
	}

	/** How to get more of one store. */
	public static final class Getting
	{
		public String store;
		public int icon = -1;
		public List<WayView> ways = Collections.emptyList();
		/** Units the plan below covers: the shortfall, or a runway's worth when nothing is short. */
		public long need;
		/** Every path to it, costed in time for this account, fastest first. */
		public List<PathView> plan = Collections.emptyList();
	}

	/** One costed path to a store. */
	public static final class PathView
	{
		/** "Gather", "Kill", "Buy", "Make", "Farm". */
		public String verb;
		/** The source it is filed under, for its colour. */
		public com.codex.ironworks.core.Source source;
		public String text;
		/** "5h 30m", "~640 kills", "Magic 33", or null. */
		public String figure;
		/** "204/h wiki rate", "your pace", or null. */
		public String basis;
		/** The quickest way there with Codex Wayfinder, or null. */
		public String travel;
		public boolean locked;
		public boolean timed;
		public String needs;
		public Goto spot;
	}

	/** A tile to route to. */
	public static final class Goto
	{
		public final int x;
		public final int y;
		public final int plane;
		public final String name;
		public final String key;

		public Goto(int x, int y, int plane, String name, String key)
		{
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.name = name;
			this.key = key;
		}
	}

	/** Runecrafting: the pouches carried, the essence on hand, and the player's own trips; null when none of it is known. */
	public RcView rc;

	/** The runecrafter's side of the account. */
	public static final class RcView
	{
		/** The essence pouches last carried, smallest first. */
		public List<PouchView> pouches = Collections.emptyList();
		/** Whether the pouches were read on this login, rather than remembered. */
		public boolean live;
		/** Essence carried now, in the pack and in the pouches. */
		public long pack;
		public long pouched;
		/** What the pouches hold between them when full, as worn today. */
		public long pouchRoom;
		/** Each rune the player has crafted, most trips first. */
		public List<TripView> trips = Collections.emptyList();
		/** Essence a trip over the recent trips, and the most on one. */
		public double essencePerTrip;
		public long bestTrip;
		/** The rune the latest trip made, or null. */
		public String current;
		/** Pure essence in the stores, and the trips and time it makes at the player's own rounds. */
		public long essence;
		public long tripsInStock;
		public long stockMs;
		/** Runes the essence in the stores would make at the player's own multiples; 0 while unknown. */
		public long runesInStock;
		/** The rune the Make tab sends pure essence to because the player's trips craft it; null when picked or unknown. */
		public String essenceTo;
	}

	/** One essence pouch as last read from the game. */
	public static final class PouchView
	{
		public String name;
		public int item;
		public int amount;
		/** What it holds now, and as new at the player's level. */
		public int capacity;
		public int full;
		public boolean degraded;
		public boolean wears;
		/** Essence it is sure to take before it can shrink, and takes on average. */
		public int safe;
		public int expected;
		/** What it holds once it next shrinks; 0 when that destroys it. */
		public int next;
		/** The next fill could shrink it. */
		public boolean atRisk;
	}

	/** One rune the player crafts, measured from their own trips. */
	public static final class TripView
	{
		public String rune;
		public int icon = -1;
		public long trips;
		public double essence;
		/** Runes an hour over whole rounds, bank to bank; 0 until timed. */
		public double perHour;
		public double runesPerEssence;
		public long roundMs;
	}

	/** The herbiboar: the player's own harvests and trails; null when neither hunted nor in reach. */
	public HerbiView herbiboar;

	/** The herbiboar as this account hunts it. */
	public static final class HerbiView
	{
		/** Harvests logged, and trails timed harvest to harvest. */
		public long harvests;
		public long timed;
		/** Trails an hour and the average trail; 0 until timed. */
		public double trailsPerHour;
		public long trailMs;
		/** Herbs a harvest at the stretch of Herblore levels last hunted at, and that stretch. */
		public double herbsPerHarvest;
		public long bandHarvests;
		public int bandLo;
		public int bandHi;
		/** Whether the player's Herblore today has left that stretch, so the mix may have changed. */
		public boolean bandStale;
		/** Each herb at that stretch, most first. */
		public List<HerbView> herbs = Collections.emptyList();
		/** Herbs an hour, all kinds; 0 until timed. */
		public double herbsPerHour;
		/** Herblore experience an hour from the harvests themselves; 0 until timed. */
		public double harvestXpPerHour;
		/** Herblore experience an hour the herbs bank, each made into its best potion at today's level; 0 until timed. */
		public double bankedXpPerHour;
		/** What is missing to hunt it, in words; empty when nothing is. */
		public List<String> missing = Collections.emptyList();
		/** The herbs the harvest can give at today's Herblore level that it has not given yet. */
		public List<String> unseen = Collections.emptyList();
	}

	/** One herb from the herbiboar, as this account's harvests give it. */
	public static final class HerbView
	{
		public String herb;
		public int icon = -1;
		/** Herbs a harvest, and an hour at the player's trails; 0 until timed. */
		public double perHarvest;
		public double perHour;
		/** The potion it banks as at today's level, and its experience; null and 0 when none. */
		public String potion;
		public double potionXp;
	}

	/** Woodcutting: the player's own logs an hour tree by tree; null when nothing has been cut. */
	public WoodView woodcutting;

	/** Woodcutting as this account does it. */
	public static final class WoodView
	{
		/** Logs cut in all, and trees felled while the coach watched. */
		public long logs;
		public long felled;
		/** The tree last chopped, its logs and their picture. */
		public String tree;
		public String logName;
		public int icon = -1;
		/** Its logs an hour at the player's pace, and the time that is timed over; 0 until timed. */
		public double perHour;
		public long timedMs;
		/** Woodcutting experience an hour at the wiki's experience for each log; 0 until timed. */
		public double xpPerHour;
		/** What its logs bank an hour in the skills that use them; empty until timed. */
		public List<BankView> banked = Collections.emptyList();
		/** Falls of this tree seen from the first log, their average ticks of chopping, and the wiki's. */
		public long timedFalls;
		public double ticksToFall;
		public int wikiFallTicks;
		/** Every tree cut, the latest first. */
		public List<TreeView> trees = Collections.emptyList();
	}

	/** Mining: the player's own ore an hour by rock and pickaxe; null when nothing has been mined. */
	public MineView mining;

	/** Mining as this account does it. */
	public static final class MineView
	{
		/** Ore mined in all (gems from gem rocks among it), and gems and clues turned up. */
		public long ore;
		public long gems;
		public long clues;
		/** The Mining level, when known; 0 when not. */
		public int level;
		/** The rock and pickaxe mined last, what it gives and its picture. */
		public String rock;
		public String pickaxe;
		public String oreName;
		public int icon = -1;
		/** Ore an hour at the player's pace there, and the time that is timed over; 0 until timed. */
		public double perHour;
		public long timedMs;
		/** Mining experience an hour at the wiki's experience for each ore; 0 until timed. */
		public double xpPerHour;
		/** Every rock and pickaxe mined, the latest first. */
		public List<SeamView> seams = Collections.emptyList();
		/** The rock with the most Mining experience an hour at the player's own pace, open at their level. */
		public SeamView best;
		/** A rock open at their level that gives more experience for each ore, never timed yet. */
		public String untried;
		public int untriedLevel;
		public double untriedXp;
		/** Ores the plans are short of, and how long the player's own pace takes to cover each. */
		public List<OrePlan> plans = Collections.emptyList();
		/** Gems and clues turned up, most first. */
		public List<FoundView> found = Collections.emptyList();
		/** Ore mined from rocks for each gem turned up, or 0 when none. */
		public double orePerGem;
		/** Rocks seen come back, against the wiki's time. */
		public List<ComebackView> comebacks = Collections.emptyList();
	}

	/** Shooting stars: the player's own stardust an hour by size; null when no star has been found. */
	public StarView stars;

	/** Shooting stars as this account mines them. */
	public static final class StarView
	{
		/** Stardust mined in all, discovery bonus among it, and the stars found. */
		public long dust;
		public long bonus;
		public int found;
		/** The Mining level, when known; 0 when not. */
		public int level;
		/** The size mined last. */
		public int size;
		/** Stardust and Mining experience an hour at the player's pace on it, and the time timed over; 0 until timed. */
		public double perHour;
		public double xpPerHour;
		public long timedMs;
		/** Every size mined, largest first. */
		public List<TierView> tiers = Collections.emptyList();
		/** The size with the most Mining experience an hour at the player's own pace, open at their level. */
		public TierView best;
		/** The largest size the level opens. */
		public int largest;
		/** Layers watched from start to end, their average seconds, and the wiki's. */
		public long layers;
		public double layerSeconds;
		public double wikiLayerSeconds;
		/** The stars found, the latest first. */
		public List<FindView> finds = Collections.emptyList();
		/** The Dusuri shop item chosen, its picture and price; null when none. */
		public String goal;
		public int goalIcon = -1;
		public int goalPrice;
		/** Stardust held now (pack and bank as last seen), and hours to the goal at the player's pace; 0 when not timed. */
		public long held;
		public double goalHours;
		/** The star by the player now; null when not at one. */
		public StarNow now;
	}

	/** One size of star as this account mines it. */
	public static final class TierView
	{
		public int size;
		public int level;
		public long dust;
		public double perHour;
		public double xpPerHour;
	}

	/** One star the player found. */
	public static final class FindView
	{
		public int world;
		public int size;
		public int lowest;
		public long dust;
		public long first;
		public long last;
		public boolean discovered;
		public boolean gone;
		/** Where it was: near a teleport the account owns when Wayfinder knows one, else its map tile. */
		public String where;
	}

	/** The star the player stands by. */
	public static final class StarNow
	{
		public int world;
		public int size;
		public int level;
		/** The share of the layer mined, 0 to 1, or -1 when not known. */
		public double mined = -1;
		/** Estimated seconds left on the layer, or -1 when not known. */
		public double leftSeconds = -1;
		/** The share the game gave when last prospected, or -1, and seconds since. */
		public int prospected = -1;
		public double prospectAgoSeconds;
		/** Whether the layer time is the player's own average rather than the wiki's 7 minutes. */
		public boolean own;
	}

	/** Fishing: the player's own fish an hour by way and tool; null when nothing has been caught. */
	public FishView fishing;

	/** Fishing as this account does it. */
	public static final class FishView
	{
		/** Fish caught in all, and clue bottles among the finds. */
		public long fish;
		public long clues;
		/** The Fishing level, when known; 0 when not. */
		public int level;
		/** The way fished last, with its tool, the fish it gave most and its picture. */
		public String method;
		public String tool;
		public String fishName;
		public int icon = -1;
		/** Fish an hour at the player's pace on it, and the time that is timed over; 0 until timed. */
		public double perHour;
		public long timedMs;
		/** Fishing experience an hour on it, at the wiki's experience for each fish; 0 until timed. */
		public double xpPerHour;
		/** Every way and tool fished, the latest first. */
		public List<CastView> casts = Collections.emptyList();
		/** The way and tool with the most Fishing experience an hour at the player's own pace, open at their level. */
		public CastView best;
		/** The way open at their level that gives the most experience a fish, never fished; null when none. */
		public String untried;
		public String untriedFish;
		public int untriedLevel;
		public double untriedXp;
		public int untriedIcon = -1;
		/** Fish the plans are short of, and how long the player's own pace takes to cover each. */
		public List<CatchPlan> plans = Collections.emptyList();
		/** What else fishing turned up, the most first, clue bottles last. */
		public List<FoundView> found = Collections.emptyList();
		/** Big fish against the wiki's chance for the fish caught. */
		public List<LuckView> luck = Collections.emptyList();
		/** Aerial catches and the Molch pearls they gave; 0 when no aerial fishing. */
		public long aerialCatches;
		public long pearls;
		/** Molch pearls an hour of aerial fishing at the player's pace; 0 until timed. */
		public double pearlsPerHour;
		/** Molch pearls in the pack and the bank, as last seen. */
		public long pearlsHeld;
		/** The Alry shop item the player chose to save pearls toward; null when none. */
		public String pearlGoal;
		public int pearlGoalIcon = -1;
		public int pearlGoalPrice;
		/** Hours of aerial fishing at the player's pace to the goal; 0 when not timed or reached. */
		public double pearlGoalHours;
		/** Spots the player watched stay put, by kind. */
		public List<StayView> stays = Collections.emptyList();
	}

	/** One way of fishing with one tool, as this account fishes it. */
	public static final class CastView
	{
		public String method;
		public String spot;
		public String tool;
		/** The fish it gave most, for its picture and name. */
		public String fish;
		public int icon = -1;
		public long count;
		public double perHour;
		public double xpPerHour;
	}

	/** One fish the plans are short of. */
	public static final class CatchPlan
	{
		public String store;
		public int icon = -1;
		public long need;
		public String method;
		/** The Fishing level it needs, when the player is short of it; 0 when open. */
		public int needs;
		/** The player's own pace on it; 0 when never timed. */
		public double perHour;
		public double hours;
	}

	/** A rare find against the wiki's chance: so many seen from so many fish, where one in so many was expected. */
	public static final class LuckView
	{
		public String name;
		public int icon = -1;
		public long seen;
		public long from;
		public int oneIn;
		/** Finds the wiki's chance gives for the fish caught. */
		public double expected;
	}

	/** One kind of fishing spot, as the player watched it stay put. */
	public static final class StayView
	{
		public String spot;
		public long seen;
		public double seconds;
		public double wikiMinSeconds;
		public double wikiMaxSeconds;
	}

	/** Deep sea trawling: the player's own fish an hour by kind; null when nothing has been trawled. */
	public TrawlView trawling;

	/** Deep sea trawling as this account does it. */
	public static final class TrawlView
	{
		/** Fish trawled in all, and the catches they came in. */
		public long fish;
		public long catches;
		/** The Fishing level, when known; 0 when not. */
		public int level;
		/** The fish trawled last, its shoal and picture. */
		public String shoal;
		public String fishName;
		public int icon = -1;
		/** Fish an hour at the player's pace on it, and the time that is timed over; 0 until timed. */
		public double perHour;
		public long timedMs;
		/** Fishing and Sailing experience an hour on it, as it came in; 0 until timed. */
		public double fishingPerHour;
		public double sailingPerHour;
		/** Every fish trawled, the latest first. */
		public List<HaulView> hauls = Collections.emptyList();
		/** The fish with the most Fishing experience an hour at the player's own pace, open at their level. */
		public HaulView best;
		/** The best shoal open at their level that gives more experience for each catch, never trawled. */
		public String untried;
		public int untriedLevel;
		public double untriedXp;
		/** Trawled fish the plans are short of, and how long the player's own pace takes to cover each. */
		public List<FishPlan> plans = Collections.emptyList();
	}

	/** One fish as this account trawls it. */
	public static final class HaulView
	{
		public String shoal;
		public String fish;
		public int icon = -1;
		public long count;
		public double perHour;
		public double fishingPerHour;
		public double sailingPerHour;
	}

	/** One trawled fish the plans are short of. */
	public static final class FishPlan
	{
		public String store;
		public int icon = -1;
		public long need;
		public String shoal;
		/** The Fishing level the shoal needs, when the player is short of it; 0 when open. */
		public int needs;
		/** The player's own pace on it; 0 when never timed. */
		public double perHour;
		public double hours;
	}

	/** One rock with one pickaxe, as this account mines it. */
	public static final class SeamView
	{
		public String rock;
		public String pickaxe;
		public int icon = -1;
		public long ore;
		public double perHour;
		public double xpPerHour;
	}

	/** One ore the plans are short of. */
	public static final class OrePlan
	{
		public String store;
		public int icon = -1;
		public long need;
		public String rock;
		/** The Mining level the rock needs, when the player is short of it; 0 when open. */
		public int needs;
		/** The player's own best pace on the rock; 0 when never timed. */
		public double perHour;
		public double hours;
	}

	/** A gem or clue turned up while mining. */
	public static final class FoundView
	{
		public String name;
		public int icon = -1;
		public long count;
		public boolean clue;
	}

	/** One rock as the player has seen it come back. */
	public static final class ComebackView
	{
		public String rock;
		public long seen;
		public double seconds;
		public double wikiSeconds;
		public String varies;
	}

	/** One skill a tree's logs bank experience in. */
	public static final class BankView
	{
		public net.runelite.api.Skill skill;
		/** What the logs are made into, in words: "burned", "Yew longbow". */
		public String how;
		/** Experience an hour at the player's pace, or 0 when the level is not there yet. */
		public double xpPerHour;
		/** The level it needs, when the player is short of it; 0 when open. */
		public int needs;
	}

	/** One tree as this account cuts it. */
	public static final class TreeView
	{
		public String tree;
		public int icon = -1;
		public long logs;
		public double perHour;
		public long felled;
	}

	/** One way to get more. */
	public static final class WayView
	{
		public Ways.Kind kind;
		public String text;
		/** The player's own pace this way, units an hour, or 0 when not yet known. */
		public double perHour;
		public Skill skill;
		public int level;
		/** Whether the player has the level for it. */
		public boolean open = true;
	}

	/** One store as the panel shows it. */
	public static final class StoreView
	{
		public String store;
		public Family family;
		public int icon = -1;
		public long stock;
		public Runway runway;
		public Tally week;
		public Tally all;
		public Map<Place, Long> where = Collections.emptyMap();
		/** Shops the player has bought it at, by name. */
		public List<String> shops = Collections.emptyList();
	}

	/** One shop and what it sells. */
	public static final class ShopView
	{
		public String key;
		public String name;
		/** The map square it stands in. */
		public int region;
		public int visits;
		public long first;
		public long last;
		public long bought;
		public Map<String, Long> spent = Collections.emptyMap();
		public List<WareView> wares = Collections.emptyList();
	}

	/** One item a shop sells. */
	public static final class WareView
	{
		public int item;
		public String name;
		/** The store it fills, directly or as a pack, or null. */
		public String store;
		/** Units of the store in one item: a hundred feathers in a pack. */
		public int units = 1;
		public int base;
		public long bought;
		public long coins;
		/** Milliseconds for one to come back, or 0 while unknown. */
		public double restockMs;
		public long refillMs;
		/** Coins for one at the usual stock, and more for each one below it; 0 while unknown. */
		public double price;
		public double climb;
		/** Worlds seen short of full, soonest full first. */
		public List<WorldView> worlds = Collections.emptyList();
	}

	/** A world a shop was last seen short on. */
	public static final class WorldView
	{
		public int world;
		public int stock;
		public long fullAt;
	}

	/** One shop run. */
	public static final class RunView
	{
		public long start;
		public long end;
		public List<String> shops = Collections.emptyList();
		public Map<String, Long> bought = Collections.emptyMap();
		public Map<String, Long> spent = Collections.emptyMap();
		public int worlds;
		public int visits;
	}

	/** A store short of the runway wanted, and where it was bought before. */
	public static final class Want
	{
		public String store;
		public int icon = -1;
		public long need;
		public double hours;
		public Runway.Status status;
		/** The shop to buy it at, or null when it was never bought. */
		public String shop;
		public String ware;
		public int wareItem = -1;
		public int units = 1;
		public int base;
		public long refillMs;
		/** The skill it is mostly made or gathered with, when it is not bought. */
		public String skill;
		/** Coins for what is needed, hopping as the price climbs, or 0 when the price is not yet known. */
		public long coins;
		/** How many to buy a world before the price has climbed by a tenth, or 0 when unknown. */
		public long perWorld;
		/** The player's own pace of getting it, units an hour, and how, when there is one. */
		public double perHour;
		public String how;
	}

	/** The guide: the step in hand, what comes next, and the supplies ahead. */
	public static final class GuideView
	{
		public boolean following;
		public int done;
		public int total;
		/** The step in hand, or null when the route is finished. */
		public StepView current;
		public List<StepView> next = Collections.emptyList();
		public List<SectionView> sections = Collections.emptyList();
		/** Supplies the next steps use up, against what the account owns. */
		public List<NeedView> needs = Collections.emptyList();
		/** "212 tiles north-east" to the step in hand, or null. */
		public String way;
		/** Route quests further on whose every requirement the account already meets. */
		public List<StepView> ready = Collections.emptyList();
		/** Route quests held back only by levels the stores already hold the experience for. */
		public List<StepView> unlock = Collections.emptyList();
		/** Other quests, off the route, the account could start now. */
		public List<String> readyElse = Collections.emptyList();
		/** Every supply the steps not yet done use up, in route order, for Find. */
		public List<UseView> uses = Collections.emptyList();
	}

	/** A supply one step of the route uses up. */
	public static final class UseView
	{
		public String item;
		public long qty;
		public String step;
	}

	/** One step of the route. */
	public static final class StepView
	{
		public String key;
		public String section;
		public String text;
		public List<String> tips = Collections.emptyList();
		/** Which tips are ticked off, as the wiki's checklist under each quest: by tip, in order. */
		public List<Boolean> tipsDone = Collections.emptyList();
		public boolean done;
		/** The game confirms it for good. */
		public boolean met;
		public boolean optional;
		/** Each thing the game checks for it, and where the player stands. */
		public List<CheckView> checks = Collections.emptyList();
		/** What Ironworks knows that helps: banked experience toward a level, an item already in the bank. */
		public List<String> hints = Collections.emptyList();
	}

	/** One thing a step checks: "Agility", "43 / 50". */
	public static final class CheckView
	{
		public String label;
		public String figure;
		public boolean ok;

		public CheckView(String label, String figure, boolean ok)
		{
			this.label = label;
			this.figure = figure;
			this.ok = ok;
		}
	}

	/** How far through one part of the route the player is. */
	public static final class SectionView
	{
		public String name;
		public int done;
		public int all;
		/** True when the step in hand is in this part. */
		public boolean current;
		public List<StepView> steps = Collections.emptyList();
	}

	/** A supply the next steps use up. */
	public static final class NeedView
	{
		public String item;
		public int icon = -1;
		public long need;
		public long have;
		/** The first step that needs it, counted from the step in hand (0). */
		public int stepsAway;
		/** Where to get what is short, from the shops you use and your own pace, or null. */
		public String how;
	}

	/** The key a tip is ticked off by: its step's key and its place under the step. */
	public static String tipKey(String step, int tip)
	{
		return step + "#" + tip;
	}

	/** A tip that heads the ones after it ("While in Port Sarim:") rather than asking for something. */
	public static boolean heading(String tip)
	{
		return tip.endsWith(":");
	}
}
