package com.codex.ironworks;

import com.codex.ironworks.core.Catalogue;
import com.codex.ironworks.core.Classify;
import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Flow;
import com.codex.ironworks.core.Place;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Settle;
import com.codex.ironworks.core.SharedBank;
import com.codex.ironworks.core.Shop;
import com.codex.ironworks.core.Tracker;
import com.codex.ironworks.core.Ware;
import com.codex.ironworks.core.Window;
import com.codex.ironworks.core.Works;
import com.codex.ironworks.overlay.ShopPlaque;
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
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
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
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Codex Ironworks",
	description = "An ironman's economy: how long each store lasts at your pace, where everything came from, and the shops you restock at",
	tags = {"ironman", "iron", "supplies", "stores", "shops", "restock", "runway", "self-sufficient", "codex"}
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

	private final ShopPlaque plaque = new ShopPlaque();
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
		panel = new IronworksPanel(icons);
		navButton = NavigationButton.builder()
			.tooltip("Codex Ironworks")
			.icon(Sigil.icon(16))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(plaque);
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
					works.place(Place.BANK, units(contents(bank)), System.currentTimeMillis());
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
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
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
		plaque.show(null, null, null);
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
		if (id == InventoryID.BANK)
		{
			works.place(Place.BANK, units(contents(event.getItemContainer())), now);
		}
		else if (id == InventoryID.SEED_VAULT)
		{
			works.place(Place.SEED_VAULT, units(contents(event.getItemContainer())), now);
		}
		else if (id == InventoryID.LOOTING_BAG)
		{
			works.place(Place.LOOTING_BAG, units(contents(event.getItemContainer())), now);
		}
		else if (id == InventoryID.INV_GROUP_TEMP && storageOpen.contains(InterfaceID.SHARED_BANK))
		{
			works.place(Place.GROUP_STORAGE, units(contents(event.getItemContainer())), now);
		}
		else
		{
			return;
		}
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

		Map<Integer, Long> items = readCarried();
		Map<String, Long> carriedUnits = units(items);
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
			works.purchase(shop, e.getKey(), name(e.getKey()), e.getValue(), share, now);
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

	/** The inventory, worn gear and the runes in a carried rune pouch. */
	private Map<Integer, Long> readCarried()
	{
		Map<Integer, Long> items = contents(client.getItemContainer(InventoryID.INV));
		contents(client.getItemContainer(InventoryID.WORN)).forEach((id, q) -> items.merge(id, q, Long::sum));
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
				}
			}
		}
		return items;
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
		View view = works == null ? new View()
			: Views.build(works, lookup, config.target(), config.warn(), System.currentTimeMillis(), LocalDate.now(), this::unitWorth);
		view.loggedIn = works != null && client.getGameState() == GameState.LOGGED_IN;
		SwingUtilities.invokeLater(() -> target.show(view));
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
