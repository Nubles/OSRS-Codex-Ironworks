package com.codex.ironworks;

import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Classify;
import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Flow;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Route;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Settle;
import com.codex.ironworks.core.SharedBank;
import com.codex.ironworks.core.Shop;
import com.codex.ironworks.core.Spot;
import com.codex.ironworks.core.Tracker;
import com.codex.ironworks.core.Ware;
import com.codex.ironworks.core.Window;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.overlay.ItemTips;
import com.codex.ironworks.overlay.ShopPlaque;
import com.codex.ironworks.overlay.StepPlaque;
import com.codex.ironworks.ui.Arrivals;
import com.codex.ironworks.ui.Format;
import com.codex.ironworks.ui.IronworksPanel;
import com.codex.ironworks.ui.ItemIcons;
import com.codex.ironworks.ui.Sigil;
import com.codex.ironworks.ui.Theme;
import com.codex.ironworks.ui.View;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Codex Ironworks",
	description = "An all-in-one ironman app: store runways, banked experience, finding anything you own, sources, shops and a guide",
	tags = {"ironman", "iron", "supplies", "stores", "shops", "restock", "runway", "banked", "xp", "find", "death", "guide", "route", "codex"}
)
public class CodexIronworksPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexIronworksPlugin.class);
	private static final String WORKS_KEY = "works";
	/** Where a damaged save is set aside, as it was, before what was read from it is saved over it. */
	private static final String DAMAGED_KEY = "worksDamaged";
	private static final int SAVE_TICKS = 50;
	/** Ticks between panel refreshes while things change. */
	private static final int PUBLISH_TICKS = 3;
	/** Menu clicks are remembered this many ticks, as the item change they cause can come a tick or two later. */
	private static final int ACTION_TICKS = 3;
	/** Ticks after a death in which losses are the death's. */
	private static final int DEATH_TICKS = 10;
	/** Experience this recent places items that arrive unseen. */
	private static final long RECENT_MS = 15 * 60_000L;
	private static final int[] POUCH_RUNES = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
	};
	private static final int[] POUCH_AMOUNTS = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
	};
	private static final int[] POUCHES = {
		ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER
	};
	private static final int[] POTION_STORE_VARS = {
		VarPlayerID.POTIONSTORE_BASE_VAR_1, VarPlayerID.POTIONSTORE_BASE_VAR_2, VarPlayerID.POTIONSTORE_BASE_VAR_3,
		VarPlayerID.POTIONSTORE_BASE_VAR_4, VarPlayerID.POTIONSTORE_BASE_VAR_5, VarPlayerID.POTIONSTORE_BASE_VAR_6
	};
	/** Windows in which items move to and from a store, so a change to what is carried is a move. */
	private static final Set<Integer> STORAGE = new HashSet<>(java.util.Arrays.asList(
		InterfaceID.BANK_DEPOSITBOX, InterfaceID.SEED_VAULT, InterfaceID.SEED_VAULT_DEPOSIT, InterfaceID.SHARED_BANK,
		InterfaceID.FARMING_TOOLS));
	private static final Set<Integer> TRADE = new HashSet<>(java.util.Arrays.asList(InterfaceID.TRADEMAIN, InterfaceID.TRADECONFIRM,
		InterfaceID.GE_OFFERS, InterfaceID.GE_COLLECT));

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Gson gson;

	@Inject
	private CodexIronworksConfig config;

	@Inject
	private ItemTips tips;

	@Inject
	private EventBus eventBus;

	@Inject
	private ModelOutlineRenderer outlines;

	private Route route;
	private RouteGuide guide;
	private Highlights highlights;
	private Clicks clicks;

	private final ShopPlaque plaque = new ShopPlaque();
	private final StepPlaque stepPlaque = new StepPlaque();
	private final Settle settle = new Settle();
	private final Map<Integer, String> names = new HashMap<>();
	private final Map<Skill, Long> lastXp = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> xpTick = new EnumMap<>(Skill.class);
	private final Deque<long[]> recentXp = new ArrayDeque<>();
	private final Deque<Object[]> actions = new ArrayDeque<>();
	private final Set<Integer> storageOpen = new HashSet<>();
	private final Set<Integer> tradeOpen = new HashSet<>();
	/** Stores already warned about in chat this session. */
	private final Set<String> warned = new HashSet<>();

	private IronworksPanel panel;
	private NavigationButton navButton;

	private Catalogue.Lookup lookup = Catalogue.standard();
	private String profile;
	private Works works;
	private Tracker tracker;
	private Map<Integer, Long> lastItems;
	private Map<String, Long> lastUnits;
	private Shop shop;
	private boolean shopOpen;
	/** Ticks the open shop's title has been looked for. */
	private int shopLooks;
	private int shopGrace;
	private int tradeGrace;
	private int deathTick = -1_000;
	private int warnTicks = -1;
	private boolean readStorage;
	private int storageGrace;
	private Set<Integer> storageVars;
	private boolean bankOpen;
	private int ticks;
	private long lastTickMs;
	private boolean dirty;
	private boolean unsaved;
	/** What was carried when the player died, to tell what the death took. */
	private Map<Integer, Long> beforeDeath;
	/** Each carried place as last held, to save a place only when it changes. */
	private final Map<Spot, Map<Integer, Long>> held = new EnumMap<>(Spot.class);
	/** The view last handed to the panel, for the tooltips. */
	private volatile View view = new View();

	@Provides
	CodexIronworksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexIronworksConfig.class);
	}

	@Override
	protected void startUp()
	{
		Arrivals arrivals = new Arrivals();
		ItemIcons icons = (itemId, onLoaded) ->
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			arrivals.ask(image, onLoaded);
			return image;
		};
		route = Route.load();
		guide = new RouteGuide(client, eventBus, route, this::name);
		highlights = new Highlights(client, outlines, guide);
		clicks = new Clicks(client, guide, this::name);
		panel = new IronworksPanel(icons, new IronworksPanel.Actions()
		{
			@Override
			public void forgetDeath()
			{
				clientThread.invokeLater(() ->
				{
					if (works != null)
					{
						works.forgetDeath();
						unsaved = true;
						publish();
					}
				});
			}

			@Override
			public void tick(String key, boolean done)
			{
				clientThread.invokeLater(() ->
				{
					if (works != null && works.tick(key, done))
					{
						unsaved = true;
						publish();
					}
				});
			}

			@Override
			public void follow(boolean on)
			{
				clientThread.invokeLater(() ->
				{
					if (works != null)
					{
						works.setFollowing(on);
						if (!on)
						{
							guide.stop();
						}
						unsaved = true;
						publish();
					}
				});
			}
		});
		navButton = NavigationButton.builder()
			.tooltip("Codex Ironworks")
			.icon(Sigil.icon(16))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(plaque);
		overlayManager.add(stepPlaque);
		overlayManager.add(tips);
		overlayManager.add(highlights);
		overlayManager.add(clicks);
		lookup = Catalogue.with(config.picked(), config.hidden());
		clientThread.invokeLater(() ->
		{
			loadProfile();
			if (client.getGameState() == GameState.LOGGED_IN && works != null)
			{
				// Turned on while logged in: take what is carried, and the bank if it is open, as they are.
				ItemContainer bank = client.getItemContainer(InventoryID.BANK);
				if (bank != null)
				{
					Map<Integer, Long> items = contents(bank);
					works.place(Place.BANK, units(items), System.currentTimeMillis());
					works.hold(Spot.BANK, items, names(items.keySet()), System.currentTimeMillis());
				}
				settle.restart();
			}
			publish();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(plaque);
		overlayManager.remove(stepPlaque);
		overlayManager.remove(tips);
		overlayManager.remove(highlights);
		overlayManager.remove(clicks);
		clientToolbar.removeNavigation(navButton);
		RouteGuide leaving = guide;
		clientThread.invokeLater(() ->
		{
			leaving.stop();
			save();
			works = null;
			tracker = null;
			profile = null;
			reset();
		});
		panel = null;
	}

	private void reset()
	{
		names.clear();
		lastItems = null;
		lastUnits = null;
		shop = null;
		shopOpen = false;
		bankOpen = false;
		storageOpen.clear();
		tradeOpen.clear();
		actions.clear();
		warnTicks = -1;
		readStorage = false;
		storageGrace = 0;
		beforeDeath = null;
		held.clear();
		plaque.show(null, null, null);
		stepPlaque.show(null);
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		// ConfigManager writes its changes in this same event, before a save queued on the client thread runs.
		CompletableFuture<Void> done = new CompletableFuture<>();
		clientThread.invokeLater(() ->
		{
			try
			{
				if (save())
				{
					configManager.sendConfig();
				}
			}
			finally
			{
				done.complete(null);
			}
		});
		event.waitFor(done);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN)
		{
			if (tracker != null)
			{
				tracker.restart(LocalDate.now());
			}
			reset();
			lastXp.clear();
			guide.stop();
			guide.stale();
			save();
			dirty = true;
			publish();
		}
		else if (state == GameState.HOPPING)
		{
			if (tracker != null)
			{
				tracker.hop();
			}
			lastItems = null;
			lastUnits = null;
			shopOpen = false;
			bankOpen = false;
			storageOpen.clear();
			tradeOpen.clear();
			settle.restart();
		}
		else if (state == GameState.LOGGING_IN)
		{
			settle.restart();
			warned.clear();
			if (tracker != null)
			{
				tracker.restart(LocalDate.now());
			}
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		save();
		reset();
		warned.clear();
		lastXp.clear();
		loadProfile();
		publish();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CodexIronworksConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invokeLater(() ->
			{
				lookup = Catalogue.with(config.picked(), config.hidden());
				// A changed list files items afresh; the next reading is a new start, not a gain.
				lastUnits = null;
				lastItems = null;
				if (tracker != null)
				{
					tracker.hop();
				}
				dirty = true;
			});
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		long xp = event.getXp();
		Long was = lastXp.put(skill, xp);
		if (was != null && xp > was && settle.isSettled())
		{
			xpTick.put(skill, ticks);
			recentXp.addLast(new long[]{System.currentTimeMillis(), skill.ordinal(), xp - was});
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		int item = event.getItemId();
		MenuAction action = event.getMenuAction();
		if (item <= 0 && action != null && action.ordinal() >= MenuAction.GROUND_ITEM_FIRST_OPTION.ordinal()
			&& action.ordinal() <= MenuAction.GROUND_ITEM_FIFTH_OPTION.ordinal())
		{
			item = event.getId();
		}
		String store = null;
		boolean pack = false;
		if (item > 0)
		{
			int id = itemManager.canonicalize(item);
			String name = name(id);
			Catalogue.Match m = lookup.match(name);
			store = m == null ? null : m.store;
			pack = lookup.packOf(name) != null;
		}
		actions.addLast(new Object[]{ticks, new Classify.Action(event.getMenuOption(), event.getMenuTarget(), store, pack), item});
		while (actions.size() > 12)
		{
			actions.removeFirst();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			deathTick = ticks;
			beforeDeath = lastItems == null ? null : new HashMap<>(lastItems);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (works == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		int id = event.getContainerId();
		long now = System.currentTimeMillis();
		Place place;
		Spot spot;
		if (id == InventoryID.BANK)
		{
			place = Place.BANK;
			spot = Spot.BANK;
		}
		else if (id == InventoryID.SEED_VAULT)
		{
			place = Place.SEED_VAULT;
			spot = Spot.SEED_VAULT;
		}
		else if (id == InventoryID.LOOTING_BAG)
		{
			place = Place.LOOTING_BAG;
			spot = Spot.LOOTING_BAG;
		}
		else if (id == InventoryID.INV_GROUP_TEMP && storageOpen.contains(InterfaceID.SHARED_BANK))
		{
			place = Place.GROUP_STORAGE;
			spot = Spot.GROUP_STORAGE;
		}
		else
		{
			return;
		}
		Map<Integer, Long> items = contents(event.getItemContainer());
		works.place(place, units(items), now);
		works.hold(spot, items, names(items.keySet()), now);
		unsaved = true;
		dirty = true;
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		int group = event.getGroupId();
		if (group == InterfaceID.BANKMAIN)
		{
			bankOpen = true;
			readStorage = true;
			warnTicks = 4;
			if (tracker != null)
			{
				tracker.bankOpened();
			}
		}
		else if (STORAGE.contains(group) && storageOpen.add(group) && tracker != null)
		{
			tracker.storageOpened(group == InterfaceID.BANK_DEPOSITBOX);
		}
		else if (TRADE.contains(group))
		{
			tradeOpen.add(group);
		}
		else if (group == InterfaceID.SHOPMAIN)
		{
			shopOpen = true;
			// The shop's name and stock are drawn into the window a moment after it loads.
			shop = null;
			shopLooks = 0;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		int group = event.getGroupId();
		if (!event.isUnload())
		{
			return;
		}
		if (group == InterfaceID.BANKMAIN)
		{
			bankOpen = false;
			storageGrace = 2;
			if (tracker != null)
			{
				tracker.bankClosed();
			}
		}
		else if (storageOpen.remove(group) && tracker != null)
		{
			tracker.storageClosed();
		}
		else if (tradeOpen.remove(group))
		{
			tradeGrace = 2;
		}
		else if (group == InterfaceID.SHOPMAIN)
		{
			shopOpen = false;
			shopGrace = 1;
			plaque.show(null, null, null);
			dirty = true;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarpId();
		Set<Integer> vars = storageVars;
		boolean storage = vars != null && vars.contains(id);
		for (int varp : POTION_STORE_VARS)
		{
			storage |= id == varp;
		}
		if (storage && (bankOpen || storageGrace > 0))
		{
			readStorage = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		ticks++;
		long nowMs = System.currentTimeMillis();
		long sinceLast = lastTickMs == 0 ? 600 : Math.max(0, Math.min(1_200, nowMs - lastTickMs));
		lastTickMs = nowMs;
		if (works == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (settle.tick())
		{
			works.setAccount(client.getVarbitValue(VarbitID.IRONMAN));
			unsaved = true;
			dirty = true;
		}
		if (!settle.isSettled())
		{
			return;
		}
		LocalDate today = LocalDate.now();
		works.played(today, sinceLast);
		if (ticks % 100 == 0)
		{
			// The panel's hours played move on even when nothing else does.
			dirty = true;
		}
		while (!recentXp.isEmpty() && nowMs - recentXp.peekFirst()[0] > RECENT_MS)
		{
			recentXp.removeFirst();
		}
		while (!actions.isEmpty() && ticks - (int) actions.peekFirst()[0] > ACTION_TICKS)
		{
			actions.removeFirst();
		}

		if (readStorage && (bankOpen || storageGrace > 0))
		{
			readStorage = false;
			readPotionStorage(nowMs);
		}
		if (storageGrace > 0 && !bankOpen)
		{
			storageGrace--;
		}

		Map<Integer, Long> items = readCarried(nowMs);
		Map<String, Long> carriedUnits = units(items);
		if (lastItems != null)
		{
			deaths(lastItems, items, nowMs);
		}
		Classify.Context ctx = context();
		if (shopOpen)
		{
			readShop(nowMs);
		}
		if (lastItems != null && (shopOpen || shopGrace > 0) && shop != null)
		{
			purchases(lastItems, items, nowMs);
		}
		if (lastItems != null && lastUnits != null)
		{
			learnPacks(lastItems, items, lastUnits, carriedUnits);
		}
		List<Flow> flows = tracker.tick(carriedUnits, ctx, today, nowMs);
		works.rate(flows, nowMs);
		if (!flows.isEmpty())
		{
			unsaved = true;
			dirty = true;
		}
		if (lastUnits == null || !lastUnits.equals(carriedUnits))
		{
			dirty = true;
		}
		lastItems = items;
		lastUnits = carriedUnits;
		if (shopGrace > 0 && !shopOpen)
		{
			shopGrace--;
		}
		if (tradeGrace > 0 && tradeOpen.isEmpty())
		{
			tradeGrace--;
		}
		if (warnTicks >= 0 && --warnTicks < 0 && bankOpen)
		{
			warnLow(today);
		}
		if (ticks % 2 == 0)
		{
			followGuide();
		}
		if (ticks % 500 == 0)
		{
			works.trim(today);
		}
		if (unsaved && ticks % SAVE_TICKS == 0)
		{
			save();
		}
		if (dirty && ticks % PUBLISH_TICKS == 0)
		{
			publish();
		}
	}

	// ---- the guide ----

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (guide != null && (event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM))
		{
			guide.heard(Text.removeTags(event.getMessage()));
		}
	}

	/** Ticks the step in hand when the game shows it done, and points the way while following. */
	private void followGuide()
	{
		Set<Integer> carried = new HashSet<>();
		for (Spot spot : new Spot[]{Spot.INVENTORY, Spot.WORN, Spot.RUNE_POUCH, Spot.LOOTING_BAG})
		{
			Map<Integer, Long> items = held.get(spot);
			if (items != null)
			{
				carried.addAll(items.keySet());
			}
		}
		View shown = view;
		Route.Moment moment = guide.moment(carried, () ->
		{
			Set<String> all = new HashSet<>();
			for (View.Found f : shown.found)
			{
				all.add(f.name.toLowerCase(java.util.Locale.ENGLISH));
			}
			return all;
		}, here());
		boolean changed = false;
		for (Route.Step step : guide.observe(works.ticked(), moment))
		{
			changed |= works.tick(step.key(), true);
		}
		if (changed)
		{
			unsaved = true;
			dirty = true;
		}
		if (works.isFollowing())
		{
			Route.Step step = guide.current(works.ticked());
			guide.guide(step, config.guideArrow(), config.guidePeople(), config.guidePath());
			stepPlaque.way(step == null ? null : RouteGuide.way(here(), guide.target(step)));
		}
	}

	/** Where the player stands: x, y, plane; or null. */
	private int[] here()
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return null;
		}
		WorldPoint p = WorldPoint.fromLocalInstance(client, me.getLocalLocation());
		return new int[]{p.getX(), p.getY(), p.getPlane()};
	}

	/** What else happened on this tick, for filing changes to what is carried. */
	private Classify.Context context()
	{
		Set<Skill> xp = EnumSet.noneOf(Skill.class);
		xpTick.forEach((skill, t) ->
		{
			if (ticks - t <= 1)
			{
				xp.add(skill);
			}
		});
		List<Classify.Action> list = new ArrayList<>();
		for (Object[] a : actions)
		{
			list.add((Classify.Action) a[1]);
		}
		Map<Skill, Long> recent = new EnumMap<>(Skill.class);
		Skill[] all = Skill.values();
		for (long[] r : recentXp)
		{
			recent.merge(all[(int) r[1]], r[2], Long::sum);
		}
		return new Classify.Context()
			.shop(shopOpen || shopGrace > 0)
			.trade(!tradeOpen.isEmpty() || tradeGrace > 0)
			.died(ticks - deathTick <= DEATH_TICKS)
			.xp(xp)
			.actions(list)
			.recent(recent);
	}

	// ---- shops ----

	/** Reads the open shop's name, then its stock, into the shop's record. */
	private void readShop(long now)
	{
		Widget itemsWidget = client.getWidget(InterfaceID.Shopmain.ITEMS);
		if (itemsWidget == null)
		{
			return;
		}
		if (shop == null)
		{
			String name = shopName();
			Player me = client.getLocalPlayer();
			if (me == null)
			{
				return;
			}
			if (name == null)
			{
				// A shop whose title never shows is still a shop; the map square tells it apart.
				if (++shopLooks < 3)
				{
					return;
				}
				name = "Shop";
			}
			int region = WorldPoint.fromLocalInstance(client, me.getLocalLocation()).getRegionID();
			shop = works.visit(name, region, client.getWorld(), now);
			unsaved = true;
			dirty = true;
		}
		Widget[] children = itemsWidget.getDynamicChildren();
		if (children == null)
		{
			return;
		}
		for (Widget c : children)
		{
			int id = c.getItemId();
			if (id <= 0 || c.isHidden())
			{
				continue;
			}
			int item = itemManager.canonicalize(id);
			works.stock(shop, item, name(item), client.getWorld(), c.getItemQuantity(), now);
		}
		showPlaque(now);
	}

	/** The shop's title, from the window's frame. */
	private String shopName()
	{
		Widget frame = client.getWidget(InterfaceID.Shopmain.FRAME);
		if (frame == null)
		{
			return null;
		}
		for (Widget[] group : new Widget[][]{frame.getDynamicChildren(), frame.getStaticChildren()})
		{
			if (group == null)
			{
				continue;
			}
			for (Widget w : group)
			{
				String text = w == null ? null : w.getText();
				if (text != null && !Text.removeTags(text).trim().isEmpty())
				{
					return Text.removeTags(text).trim();
				}
			}
		}
		return null;
	}

	/** Items that came into the pack at a shop, and the coins or tokens that went out for them. */
	private void purchases(Map<Integer, Long> was, Map<Integer, Long> is, long now)
	{
		Map<Integer, Long> got = new HashMap<>();
		Map<String, Long> paid = new HashMap<>();
		Map<Integer, Long> delta = new HashMap<>(is);
		was.forEach((k, v) -> delta.merge(k, -v, Long::sum));
		delta.forEach((item, qty) ->
		{
			if (qty == 0)
			{
				return;
			}
			Catalogue.Match m = lookup.match(name(item));
			boolean money = m != null && m.family == Family.CURRENCY;
			if (money && qty < 0)
			{
				paid.merge(m.store, -qty, Long::sum);
			}
			else if (!money && qty > 0)
			{
				got.put(item, qty);
			}
		});
		if (got.isEmpty())
		{
			return;
		}
		long total = 0;
		for (long q : got.values())
		{
			total += q;
		}
		for (Map.Entry<Integer, Long> e : got.entrySet())
		{
			Map<String, Long> share = new HashMap<>();
			double part = e.getValue() / (double) total;
			paid.forEach((k, v) -> share.put(k, Math.round(v * part)));
			works.purchase(shop, e.getKey(), name(e.getKey()), e.getValue(), share, client.getWorld(), now);
		}
		unsaved = true;
		dirty = true;
	}

	/** Counts what an opened pack held, so the shopping list buys the right number. */
	private void learnPacks(Map<Integer, Long> was, Map<Integer, Long> is, Map<String, Long> unitsWas, Map<String, Long> unitsIs)
	{
		Object[] last = actions.peekLast();
		if (last == null || ticks - (int) last[0] > ACTION_TICKS)
		{
			return;
		}
		int item = (int) last[2];
		if (item <= 0)
		{
			return;
		}
		int pack = itemManager.canonicalize(item);
		String packName = name(pack);
		String store = lookup.packOf(packName);
		long opened = was.getOrDefault(pack, 0L) - is.getOrDefault(pack, 0L);
		long gained = unitsIs.getOrDefault(store, 0L) - unitsWas.getOrDefault(store, 0L);
		if (store != null && opened > 0 && gained > 0 && gained % opened == 0)
		{
			works.pack(packName, (int) (gained / opened));
		}
	}

	/** The plaque at a shop: what the stores want from it, and when emptied worlds fill again. */
	private void showPlaque(long now)
	{
		if (!config.shopPlaque() || shop == null)
		{
			plaque.show(null, null, null);
			return;
		}
		LocalDate today = LocalDate.now();
		Window week = works.window(today, 7);
		List<ShopPlaque.Row> rows = new ArrayList<>();
		int world = client.getWorld();
		for (Ware w : shop.getWares())
		{
			String store = storeOf(w.getName());
			if (store == null || rows.size() >= 5)
			{
				continue;
			}
			int perItem = perItem(w.getName());
			Runway r = Runway.of(store, works.stock(store), week, config.target(), config.warn());
			int stockNow = w.expected(world, now);
			String figure = (stockNow < 0 ? "?" : Format.amount(stockNow)) + " / " + Format.amount(w.getBase());
			String note;
			java.awt.Color color = Theme.TEXT_MUTED;
			if (r.need > 0)
			{
				long buy = (r.need + perItem - 1) / perItem;
				note = "Buy " + Format.amount(buy) + " for " + config.target() + "h of play";
				color = r.status == Runway.Status.OUT || r.status == Runway.Status.LOW ? Theme.EMBER : Theme.GOLD;
			}
			else if (r.status == Runway.Status.LEARNING || r.status == Runway.Status.IDLE)
			{
				note = "You have " + Format.amount(r.stock) + " " + store.toLowerCase();
			}
			else
			{
				note = "Stocked for " + Format.estimate((long) (r.hours * 3_600_000)) + " of play";
				color = Theme.TEAL;
			}
			if (w.getRestockMs() > 0 && w.getBase() > 0)
			{
				note += " · refills in " + Format.estimate(w.refillMs());
			}
			rows.add(new ShopPlaque.Row(w.getName(), figure, note, color));
		}
		String sub = "World " + world + " · " + shop.getVisits() + (shop.getVisits() == 1 ? " visit" : " visits");
		plaque.show(shop.getName(), sub, rows);
	}

	private String storeOf(String itemName)
	{
		return Views.storeOf(lookup, itemName);
	}

	private int perItem(String itemName)
	{
		return Views.perItem(works, lookup, itemName);
	}

	// ---- reading the game ----

	private String name(int itemId)
	{
		return names.computeIfAbsent(itemId, id -> itemManager.getItemComposition(id).getName());
	}

	/** Items in store units, noting the picture of each store as it goes. */
	private Map<String, Long> units(Map<Integer, Long> items)
	{
		Map<String, Long> out = new HashMap<>();
		items.forEach((id, qty) ->
		{
			Catalogue.Match m = lookup.match(name(id));
			if (m != null)
			{
				out.merge(m.store, qty * m.units, Long::sum);
				if (works != null)
				{
					works.icon(m.store, id, m.model);
				}
			}
		});
		return out;
	}

	private Map<Integer, Long> contents(ItemContainer container)
	{
		Map<Integer, Long> items = new HashMap<>();
		if (container == null)
		{
			return items;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			items.merge(itemManager.canonicalize(item.getId()), (long) item.getQuantity(), Long::sum);
		}
		return items;
	}

	/** The inventory, worn gear and the runes in a carried rune pouch; each is kept as it changes. */
	private Map<Integer, Long> readCarried(long now)
	{
		Map<Integer, Long> inventory = contents(client.getItemContainer(InventoryID.INV));
		Map<Integer, Long> worn = contents(client.getItemContainer(InventoryID.WORN));
		Map<Integer, Long> pouchRunes = new HashMap<>();
		Map<Integer, Long> items = new HashMap<>(inventory);
		worn.forEach((id, q) -> items.merge(id, q, Long::sum));
		boolean pouch = false;
		for (int id : POUCHES)
		{
			pouch |= items.containsKey(id);
		}
		if (pouch)
		{
			EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
			for (int i = 0; i < POUCH_RUNES.length; i++)
			{
				int type = client.getVarbitValue(POUCH_RUNES[i]);
				int amount = client.getVarbitValue(POUCH_AMOUNTS[i]);
				int itemId = type > 0 && runes != null ? runes.getIntValue(type) : -1;
				if (itemId > 0 && amount > 0)
				{
					items.merge(itemId, (long) amount, Long::sum);
					pouchRunes.merge(itemId, (long) amount, Long::sum);
				}
			}
		}
		keep(Spot.INVENTORY, inventory, now);
		keep(Spot.WORN, worn, now);
		keep(Spot.RUNE_POUCH, pouchRunes, now);
		return items;
	}

	/** Saves a carried place item by item when it has changed. */
	private void keep(Spot spot, Map<Integer, Long> items, long now)
	{
		if (!items.equals(held.get(spot)))
		{
			held.put(spot, items);
			works.hold(spot, items, names(items.keySet()), now);
			unsaved = true;
			dirty = true;
		}
	}

	/** Names for item ids, for finding them while logged out. */
	private Map<Integer, String> names(Iterable<Integer> ids)
	{
		Map<Integer, String> out = new HashMap<>();
		for (int id : ids)
		{
			out.put(id, name(id));
		}
		return out;
	}

	/**
	 * Works out what a death took, once the player is back on their feet, and watches for those
	 * items coming back from the grave or Death's Office.
	 */
	private void deaths(Map<Integer, Long> was, Map<Integer, Long> is, long now)
	{
		if (beforeDeath != null && ticks - deathTick >= DEATH_TICKS)
		{
			Map<Integer, Long> lost = new HashMap<>();
			beforeDeath.forEach((id, q) ->
			{
				long gone = q - is.getOrDefault(id, 0L);
				if (gone > 0)
				{
					lost.put(id, gone);
				}
			});
			beforeDeath = null;
			if (!lost.isEmpty())
			{
				works.died(lost, names(lost.keySet()), client.getWorld(), now);
				unsaved = true;
				dirty = true;
			}
			return;
		}
		if (beforeDeath != null || works.getDeath() == null)
		{
			return;
		}
		Map<Integer, Long> gains = new HashMap<>();
		is.forEach((id, q) ->
		{
			long more = q - was.getOrDefault(id, 0L);
			if (more > 0)
			{
				gains.put(id, more);
			}
		});
		if (works.regained(gains))
		{
			unsaved = true;
			dirty = true;
		}
	}

	/**
	 * Reads the doses in the bank's potion storage, which the bank itself does not list, the way
	 * RuneLite's bank tags do: the game's own script says how many doses of each are stored.
	 */
	private void readPotionStorage(long now)
	{
		EnumComposition potions = client.getEnum(EnumID.POTIONSTORE_POTIONS);
		if (potions == null)
		{
			return;
		}
		if (storageVars == null)
		{
			Widget w = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
			int[] trigger = w == null ? null : w.getVarTransmitTrigger();
			if (trigger != null)
			{
				Set<Integer> vars = new HashSet<>();
				for (int v : trigger)
				{
					vars.add(v);
				}
				storageVars = vars;
			}
		}
		Map<Integer, Long> doses = new HashMap<>();
		for (int potionEnum : potions.getIntVals())
		{
			EnumComposition potion = client.getEnum(potionEnum);
			if (potion == null)
			{
				continue;
			}
			client.runScript(ScriptID.POTIONSTORE_DOSES, potionEnum);
			int stored = client.getIntStack()[0];
			int oneDose = potion.getIntValue(1);
			if (stored > 0 && oneDose > 0)
			{
				doses.merge(oneDose, (long) stored, Long::sum);
			}
		}
		Map<String, Long> units = units(doses);
		if (!units.equals(works.place(Place.POTION_STORAGE)))
		{
			works.place(Place.POTION_STORAGE, units, now);
			works.hold(Spot.POTION_STORAGE, doses, names(doses.keySet()), now);
			unsaved = true;
			dirty = true;
		}
	}

	/** Says once a session which stores will not last long, when the bank opens. */
	private void warnLow(LocalDate today)
	{
		if (!config.chatWarnings())
		{
			return;
		}
		Window week = works.window(today, 7);
		List<Runway> low = new ArrayList<>();
		for (String store : works.stores())
		{
			if (Catalogue.money(store) || warned.contains(store) || lookup.match(store) == null)
			{
				continue;
			}
			Runway r = Runway.of(store, works.stock(store), week, config.target(), config.warn());
			if (r.status == Runway.Status.LOW || r.status == Runway.Status.OUT)
			{
				low.add(r);
			}
		}
		low.sort((a, b) -> Double.compare(a.hours, b.hours));
		for (Runway r : low.subList(0, Math.min(3, low.size())))
		{
			warned.add(r.store);
			String unit = Catalogue.family(r.store).getUnit();
			String text = r.stock <= 0
				? r.store + " is out, and you use about " + Format.amount(Math.round(r.burn)) + " " + unit + " an hour."
				: r.store + " lasts about " + Format.estimate((long) (r.hours * 3_600_000)) + " of play (" + Format.amount(r.stock) + " " + unit + ").";
			String message = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("Ironworks: ")
				.append(ChatColorType.NORMAL)
				.append(text)
				.build();
			chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
		}
	}

	// ---- saving ----

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		works = null;
		tracker = null;
		if (profile == null)
		{
			return;
		}
		String saved = configManager.getRSProfileConfiguration(CodexIronworksConfig.GROUP, WORKS_KEY);
		Works.Loaded read = Works.read(gson, saved, System.currentTimeMillis());
		if (read.damaged)
		{
			log.warn("The saved ironworks were damaged; kept what still reads and set the save as it was aside");
			configManager.setConfiguration(CodexIronworksConfig.GROUP, profile, DAMAGED_KEY, saved);
		}
		works = read.works;
		tracker = new Tracker(works);
		unsaved = false;
		adoptSharedBank();
		dirty = true;
	}

	/** Before the bank has been seen here, takes Codex Bank Steward's copy of it, if there is one. */
	private void adoptSharedBank()
	{
		if (works.seen(Place.BANK))
		{
			return;
		}
		SharedBank shared = SharedBank.read(gson, configManager.getRSProfileConfiguration(SharedBank.GROUP, SharedBank.KEY));
		if (shared != null)
		{
			works.place(Place.BANK, units(shared.getItems()), shared.getSavedAt());
			if (works.holding(Spot.BANK) == null)
			{
				works.hold(Spot.BANK, shared.getItems(), names(shared.getItems().keySet()), shared.getSavedAt());
			}
			unsaved = true;
		}
	}

	private boolean save()
	{
		if (profile == null || works == null || !unsaved)
		{
			return false;
		}
		unsaved = false;
		configManager.setConfiguration(CodexIronworksConfig.GROUP, profile, WORKS_KEY, works.write(gson));
		return true;
	}

	// ---- the panel ----

	/** Works out what the panel shows and hands it over. */
	private void publish()
	{
		dirty = false;
		IronworksPanel target = panel;
		if (target == null)
		{
			return;
		}
		boolean loggedIn = works != null && client.getGameState() == GameState.LOGGED_IN;
		Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		if (loggedIn)
		{
			for (Skill skill : Skill.values())
			{
				int level = client.getRealSkillLevel(skill);
				if (level > 0)
				{
					levels.put(skill, level);
					xp.put(skill, client.getSkillExperience(skill));
				}
			}
		}
		View built = works == null ? new View()
			: Views.build(works, lookup, config.target(), config.warn(), System.currentTimeMillis(), LocalDate.now(), this::unitWorth,
			levels, xp, config.altar(), itemManager::getItemPrice);
		built.loggedIn = loggedIn;
		if (works != null)
		{
			built.guide = Guides.build(route, works.ticked(), guide.account(), works.isFollowing(), built.found, built.workshop, xp);
			if (loggedIn && built.guide.current != null)
			{
				Route.Step step = route.steps().get(indexOf(built.guide.current.key));
				built.guide.way = RouteGuide.way(here(), guide.target(step));
			}
		}
		stepPlaque.show(loggedIn && built.guide != null && built.guide.following && config.guidePlaque() ? built.guide : null);
		view = built;
		SwingUtilities.invokeLater(() -> target.show(built));
	}

	private int indexOf(String key)
	{
		for (Route.Step s : route.steps())
		{
			if (s.key().equals(key))
			{
				return s.index;
			}
		}
		return 0;
	}

	/** The view the panel shows now, for the tooltips. */
	public View view()
	{
		return view;
	}

	public Catalogue.Lookup lookup()
	{
		return lookup;
	}

	/**
	 * On an ironman, a ground item that belongs to another player cannot be taken, so its Take sits
	 * below Walk here and a misclick does not walk the player over to it.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.othersLoot() || works == null || works.getAccount() <= 0)
		{
			return;
		}
		MenuEntry entry = event.getMenuEntry();
		MenuAction type = entry.getType();
		boolean ground = type == MenuAction.GROUND_ITEM_FIRST_OPTION || type == MenuAction.GROUND_ITEM_SECOND_OPTION
			|| type == MenuAction.GROUND_ITEM_THIRD_OPTION || type == MenuAction.GROUND_ITEM_FOURTH_OPTION
			|| type == MenuAction.GROUND_ITEM_FIFTH_OPTION;
		if (!ground || !"Take".equals(entry.getOption()))
		{
			return;
		}
		Scene scene = client.getTopLevelWorldView().getScene();
		int x = entry.getParam0();
		int y = entry.getParam1();
		int plane = client.getTopLevelWorldView().getPlane();
		Tile[][][] tiles = scene.getTiles();
		if (x < 0 || y < 0 || plane < 0 || plane >= tiles.length || x >= tiles[plane].length || y >= tiles[plane][x].length)
		{
			return;
		}
		Tile tile = tiles[plane][x][y];
		List<TileItem> onTile = tile == null ? null : tile.getGroundItems();
		if (onTile == null)
		{
			return;
		}
		boolean others = false;
		for (TileItem item : onTile)
		{
			if (item.getId() != entry.getIdentifier())
			{
				continue;
			}
			if (item.getOwnership() == TileItem.OWNERSHIP_OTHER)
			{
				others = true;
			}
			else
			{
				// One of this kind on the tile is the player's to take.
				return;
			}
		}
		if (others)
		{
			entry.setDeprioritized(true);
		}
	}

	/** What one unit of a store is worth: a dose of a potion is a quarter of the four-dose price. */
	private double unitWorth(String store)
	{
		if ("Coins".equals(store))
		{
			return 1;
		}
		int icon = works.icon(store);
		if (icon < 0)
		{
			return 0;
		}
		Catalogue.Match m = lookup.match(name(icon));
		int units = m == null ? 1 : Math.max(1, m.units);
		return itemManager.getItemPrice(icon) / (double) units;
	}
}
