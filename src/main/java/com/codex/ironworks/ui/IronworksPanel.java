package com.codex.ironworks.ui;

import com.codex.ironworks.core.Death;
import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Spot;
import com.codex.ironworks.core.Summary;
import com.codex.ironworks.core.Use;
import com.codex.ironworks.core.Ways;
import com.codex.ironworks.core.Workshop;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Experience;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Ironworks side panel: the crest, then five pages. Stores is every store with how long it
 * lasts at the player's own pace; Make is what the stores could become and the experience in them;
 * Find is everything the account owns, wherever it is kept, and the last death's items; Sources is
 * where everything came from and the ways to get more; Shops is the shopping list, the shops the
 * player uses with their restock times and prices, and past shop runs.
 */
public class IronworksPanel extends PluginPanel
{
	static final int WIDTH = 200;
	private static final String[] TABS = {"stores", "make", "find", "sources", "shops", "guide"};
	private static final int STORES = 0;
	private static final int MAKE = 1;
	private static final int FIND = 2;
	private static final int SOURCES = 3;
	private static final int SHOPS = 4;
	private static final int GUIDE = 5;
	/** Things found shown before the list is cut short. */
	private static final int FOUND = 60;
	/** Stores shown per shelf before "Show all". */
	private static final int SHELF = 6;

	private final ItemIcons icons;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(false, "Stores", "Make", "Find", "Sources", "Shops", "Guide").rows(2);
	private final Segmented period = new Segmented(false, "Today", "7 days", "All time");
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane[] pages = {scroller(), scroller(), scroller(), scroller(), scroller(), scroller()};
	private final boolean[] stale = {true, true, true, true, true, true};
	private final JTextField search = new SearchField();
	private final Actions actions;
	private final Set<String> open = new HashSet<>();
	private final Set<Family> shelvesOpen = new HashSet<>();

	private View view = new View();

	/** What the panel asks of the plugin, run on the client thread there. */
	public interface Actions
	{
		/** The last death's items are all taken back. */
		void forgetDeath();

		/** A guide step was done another way, or ticked by mistake. */
		void tick(String key, boolean done);

		/** Start or stop pointing the way to the step in hand. */
		void follow(boolean on);
	}

	public IronworksPanel(ItemIcons icons, Actions actions)
	{
		super(false);
		this.icons = icons;
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.setBorder(new EmptyBorder(8, 8, 2, 8));
		tabHolder.add(tabs, BorderLayout.CENTER);
		north.add(tabHolder, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		content.setOpaque(false);
		for (int i = 0; i < TABS.length; i++)
		{
			if (i == FIND)
			{
				JPanel find = new JPanel(new BorderLayout());
				find.setOpaque(false);
				JPanel holder = new JPanel(new BorderLayout());
				holder.setOpaque(false);
				holder.setBorder(new EmptyBorder(6, 8, 0, 8));
				holder.add(search, BorderLayout.CENTER);
				find.add(holder, BorderLayout.NORTH);
				find.add(pages[i], BorderLayout.CENTER);
				content.add(find, TABS[i]);
			}
			else
			{
				content.add(pages[i], TABS[i]);
			}
		}
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				rebuild(FIND);
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				rebuild(FIND);
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				rebuild(FIND);
			}
		});
		add(content, BorderLayout.CENTER);
		tabs.onChange(this::showTab);
		period.setSelected(1, false);
		period.onChange(i ->
		{
			stale[SOURCES] = true;
			refresh(true);
		});
		updateCrest();
		refresh(true);
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		return scroll;
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void show(View view)
	{
		this.view = view;
		updateCrest();
		java.util.Arrays.fill(stale, true);
		refresh(false);
	}

	public void showTab(int index)
	{
		tabs.setSelected(index, false);
		cards.show(content, TABS[index]);
		refresh(true);
	}

	/** For the screenshot harness: types into Find. */
	public void find(String text)
	{
		search.setText(text);
	}

	/** For the screenshot harness: picks the Sources period. */
	public void showPeriod(int index)
	{
		period.setSelected(index, true);
	}

	@Override
	public void onActivate()
	{
		refresh(true);
	}

	/** Rebuilds the page in view if it is out of date; nothing is rebuilt while the panel is closed. */
	private void refresh(boolean force)
	{
		if (!force && !isShowing())
		{
			return;
		}
		int page = tabs.getSelected();
		if (!stale[page])
		{
			return;
		}
		stale[page] = false;
		JComponent body = page == STORES ? stores() : page == MAKE ? workshop() : page == FIND ? find()
			: page == SOURCES ? sources() : page == SHOPS ? shops() : guide();
		JScrollPane scroll = pages[page];
		int y = scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(body);
		scroll.revalidate();
		scroll.getVerticalScrollBar().setValue(y);
		scroll.repaint();
	}

	private void rebuild(int page)
	{
		stale[page] = true;
		refresh(true);
	}

	// ---- the crest ----

	private void updateCrest()
	{
		View v = view;
		String heading = Words.account(v.account).toUpperCase(Locale.ENGLISH);
		if (!v.loggedIn && v.stores.isEmpty())
		{
			crest.update("IRONWORKS", "-", "", -1, "Log in to open the ironworks", Theme.TEXT_MUTED, Theme.GOLD_DIM);
			return;
		}
		Summary s = v.week;
		boolean known = s != null && s.self >= 0;
		String figure = known ? Format.percent(s.self) : "-";
		String unit = known ? "self-made" : "";
		Runway first = null;
		int wanting = 0;
		for (View.StoreView sv : v.stores)
		{
			if (sv.runway.wanting() && !com.codex.ironworks.core.Catalogue.money(sv.store))
			{
				wanting++;
				if (first == null || sv.runway.hours < first.hours)
				{
					first = sv.runway;
				}
			}
		}
		String status;
		String shortStatus = null;
		Color color;
		if (v.playedWeek < Runway.LEARN_MS)
		{
			status = "Measuring your pace: " + Format.duration(v.playedWeek) + " of 1h";
			color = Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.3);
		}
		else if (first == null)
		{
			status = "Every store lasts " + Math.round(v.target) + "h or more";
			color = Theme.TEAL;
		}
		else if (first.status == Runway.Status.OUT)
		{
			status = first.store + " is out";
			shortStatus = wanting == 1 ? "A store is out" : wanting + " stores short, one out";
			color = Theme.BLOOD;
		}
		else
		{
			status = first.store + " runs out in " + Words.runway(first) + (wanting > 1 ? " · " + wanting + " short" : "");
			shortStatus = wanting + " short · first in " + Words.runway(first);
			color = Palette.of(first.status);
		}
		crest.update(heading, figure, unit, known ? s.self : -1, status, shortStatus, color, known ? Theme.progressColor(s.self) : Theme.GOLD_DIM);
	}

	// ---- stores ----

	private JComponent stores()
	{
		StackPanel p = new StackPanel(6, 8);
		View v = view;
		if (v.stores.isEmpty())
		{
			p.add(note(v.loggedIn
				? "Your stores fill in as you play. Open your bank once so Ironworks can count what you keep."
				: "Log in to see your stores: how long each lasts at your pace, and where it all came from."));
			return p;
		}
		if (!v.bankSeen)
		{
			p.add(note("Open your bank so the stores count what you keep there, not only what you carry."));
		}
		List<View.StoreView> low = new ArrayList<>();
		Map<Family, List<View.StoreView>> shelves = new EnumMap<>(Family.class);
		for (View.StoreView s : v.stores)
		{
			if (s.runway.wanting() && !com.codex.ironworks.core.Catalogue.money(s.store))
			{
				low.add(s);
			}
			else if (s.stock > 0 || s.week.gained() > 0 || s.week.burned() > 0)
			{
				shelves.computeIfAbsent(s.family, k -> new ArrayList<>()).add(s);
			}
		}
		if (!low.isEmpty())
		{
			low.sort((a, b) -> Double.compare(a.runway.hours, b.runway.hours));
			p.add(new SectionHeader("Running short", low.size() + " under " + Math.round(v.target) + "h"));
			for (View.StoreView s : low)
			{
				p.add(row(s));
			}
		}
		for (Map.Entry<Family, List<View.StoreView>> e : shelves.entrySet())
		{
			Family f = e.getKey();
			List<View.StoreView> list = e.getValue();
			p.add(new SectionHeader(f.getLabel(), String.valueOf(list.size())));
			boolean all = shelvesOpen.contains(f) || list.size() <= SHELF + 1;
			for (int i = 0; i < list.size() && (all || i < SHELF); i++)
			{
				p.add(row(list.get(i)));
			}
			if (list.size() > SHELF + 1)
			{
				JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
				holder.setOpaque(false);
				holder.add(new CodexButton(all ? "Show fewer" : "Show all " + list.size(), false, () ->
				{
					if (!shelvesOpen.remove(f))
					{
						shelvesOpen.add(f);
					}
					rebuild(STORES);
				}));
				p.add(holder);
			}
		}
		p.add(foot("Runways are hours of your own play, measured over the last 7 days."));
		return p;
	}

	private JComponent row(View.StoreView s)
	{
		return new StoreRow(s, view, icons, open.contains(s.store), () ->
		{
			if (!open.remove(s.store))
			{
				open.add(s.store);
			}
			rebuild(STORES);
		});
	}

	// ---- sources ----

	private JComponent sources()
	{
		StackPanel p = new StackPanel(6, 8);
		p.add(period);
		View v = view;
		Summary s = period.getSelected() == 0 ? v.today : period.getSelected() == 1 ? v.week : v.all;
		if (s == null || s.gained <= 0 && s.used <= 0)
		{
			p.add(note(v.loggedIn || !v.stores.isEmpty()
				? "Nothing has come in or gone out " + (period.getSelected() == 0 ? "today" : period.getSelected() == 1 ? "this week" : "yet")
				+ ". Gather, make, loot or buy something and it shows here."
				: "Log in to see where your supplies come from."));
			getMore(p);
			return p;
		}
		if (s.gained > 0)
		{
			p.add(selfCard(s));
		}
		getMore(p);
		if (!s.skills.isEmpty())
		{
			Card hands = new Card(new BorderLayout());
			StackPanel in = new StackPanel(2, 0);
			in.add(new SectionHeader("Your own hands", "by worth"));
			double total = 0;
			for (double d : s.skills.values())
			{
				total += d;
			}
			List<Map.Entry<String, Double>> list = new ArrayList<>(s.skills.entrySet());
			list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
			for (Map.Entry<String, Double> e : list.subList(0, Math.min(6, list.size())))
			{
				in.add(new Meter(e.getKey(), Format.percent(e.getValue() / total), e.getValue() / total, Theme.TEAL));
			}
			hands.add(in, BorderLayout.CENTER);
			p.add(hands);
		}
		for (Source src : Source.values())
		{
			List<Summary.Line> lines = s.bySource.get(src);
			if (lines == null || lines.isEmpty())
			{
				continue;
			}
			p.add(itemsCard(src.getLabel(), Format.percent(s.sources.get(src) / s.gained), Palette.of(src), lines, true));
		}
		if (s.used > 0)
		{
			Card went = new Card(new BorderLayout());
			StackPanel in = new StackPanel(2, 0);
			in.add(new SectionHeader("Where it went", "by worth"));
			List<Double> parts = new ArrayList<>();
			List<Color> colors = new ArrayList<>();
			for (Use u : Use.values())
			{
				Double d = s.uses.get(u);
				if (d != null && d > 0)
				{
					parts.add(d);
					colors.add(Palette.of(u));
				}
			}
			in.add(new Stacked(parts, colors, 8));
			for (Use u : Use.values())
			{
				Double d = s.uses.get(u);
				List<Summary.Line> lines = s.byUse.get(u);
				if (d != null && d > 0 && lines != null)
				{
					in.add(new Line(u.getLabel(), Format.percent(d / s.used), Palette.of(u), Theme.TEXT, Theme.TEXT_MUTED));
					for (Summary.Line l : lines.subList(0, Math.min(3, lines.size())))
					{
						in.add(new Line("   " + l.store, Format.amount(l.units), null));
					}
				}
			}
			went.add(in, BorderLayout.CENTER);
			p.add(went);
		}
		if (s.played > 0)
		{
			p.add(foot("Over " + Format.duration(s.played) + " of play. Worth is the exchange price; coins are left out."));
		}
		return p;
	}

	private JComponent selfCard(Summary s)
	{
		Card card = new Card(new BorderLayout());
		card.highlighted(true);
		StackPanel in = new StackPanel(3, 0);
		in.add(new Figure(Format.percent(Math.max(0, s.self)), "your own work", Theme.progressColor(Math.max(0, s.self))));
		List<Double> parts = new ArrayList<>();
		List<Color> colors = new ArrayList<>();
		for (Source src : Source.values())
		{
			Double d = s.sources.get(src);
			if (d != null && d > 0)
			{
				parts.add(d);
				colors.add(Palette.of(src));
			}
		}
		in.add(new Stacked(parts, colors, 8));
		for (Source src : Source.values())
		{
			Double d = s.sources.get(src);
			if (d != null && d > 0)
			{
				in.add(new Line(src.getLabel(), Format.amount(Math.round(d)) + "  " + Format.percent(d / s.gained), Palette.of(src)));
			}
		}
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	private JComponent itemsCard(String title, String trailing, Color accent, List<Summary.Line> lines, boolean units)
	{
		Card card = new Card(new BorderLayout());
		card.accent(accent);
		StackPanel in = new StackPanel(2, 0);
		in.add(new SectionHeader(title, trailing));
		JPanel wells = new JPanel(new java.awt.GridLayout(1, 4, 4, 0));
		wells.setOpaque(false);
		int shown = 0;
		for (Summary.Line l : lines)
		{
			if (shown == 4)
			{
				break;
			}
			wells.add(new Slot(view.icons.getOrDefault(l.store, -1), l.units, l.store, icons));
			shown++;
		}
		while (shown++ < 4)
		{
			JPanel blank = new JPanel();
			blank.setOpaque(false);
			wells.add(blank);
		}
		in.add(wells);
		if (lines.size() > 4)
		{
			List<String> rest = new ArrayList<>();
			for (Summary.Line l : lines.subList(4, Math.min(lines.size(), 7)))
			{
				rest.add(l.store);
			}
			in.add(new Line("Also " + String.join(", ", rest) + (lines.size() > 7 ? " and more" : ""), null, null));
		}
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	// ---- shops ----

	private JComponent shops()
	{
		StackPanel p = new StackPanel(6, 8);
		View v = view;
		List<View.Want> wants = new ArrayList<>();
		for (View.Want w : v.wants)
		{
			wants.add(w);
		}
		p.add(new SectionHeader("Shopping list", wants.isEmpty() ? ""
			: v.listCoins > 0 ? Format.coins(v.listCoins) + " coins" : wants.size() + (wants.size() == 1 ? " store" : " stores")));
		if (v.listCoins > 0 && v.coins < v.listCoins)
		{
			p.add(note("The list costs " + Format.coins(v.listCoins) + " coins and you hold " + Format.coins(v.coins) + "."));
		}
		if (wants.isEmpty())
		{
			p.add(note(v.playedWeek < Runway.LEARN_MS
				? "Play an hour this week and Ironworks lists what to restock before it runs out."
				: "Nothing is short of " + Math.round(v.target) + "h of play. Well stocked."));
		}
		for (View.Want w : wants)
		{
			p.add(new WantRow(w, icons));
		}

		p.add(new SectionHeader("Your shops", v.shops.isEmpty() ? "" : String.valueOf(v.shops.size())));
		if (v.shops.isEmpty())
		{
			p.add(note("Open any shop and Ironworks remembers its stock, learns how fast it restocks, and tracks your runs."));
		}
		for (View.ShopView s : v.shops)
		{
			p.add(shopCard(s));
		}

		if (!v.runs.isEmpty())
		{
			p.add(new SectionHeader("Shop runs", v.runs.size() == 1 ? "1 run" : v.runs.size() + " runs"));
			for (View.RunView r : v.runs)
			{
				p.add(runCard(r));
			}
		}
		return p;
	}

	private JComponent shopCard(View.ShopView s)
	{
		Card card = new Card(new BorderLayout());
		StackPanel in = new StackPanel(2, 0);
		in.add(new Line(s.name, Format.ago(view.now - s.last), null, Theme.GOLD_BRIGHT, Theme.TEXT_MUTED));
		String spent = s.spent.isEmpty() ? "" : " · " + spentText(s.spent);
		in.add(new Line(s.visits + (s.visits == 1 ? " visit" : " visits") + spent, null, null));
		int shown = 0;
		for (View.WareView w : s.wares)
		{
			if (shown++ == 5)
			{
				break;
			}
			String figure = w.bought > 0 ? "bought " + Format.amount(w.bought) : "stock " + Format.amount(w.base);
			Color tint = w.store != null ? Theme.TEXT : Theme.TEXT_MUTED;
			in.add(new Line(w.name, figure, w.store != null ? Theme.GOLD : Theme.STROKE, tint, Theme.TEXT_MUTED));
			StringBuilder note = new StringBuilder();
			if (w.restockMs > 0)
			{
				note.append("1 back every ").append(Format.seconds(w.restockMs));
				if (w.refillMs > 0)
				{
					note.append(" · full in ").append(Format.estimate(w.refillMs));
				}
			}
			if (w.price > 0)
			{
				long top = Math.round(w.price + w.climb * Math.max(0, w.base - 1));
				String price = w.climb > 0 && top > Math.round(w.price)
					? Format.amount(Math.round(w.price)) + " to " + Format.amount(top) + " coins each"
					: Format.amount(Math.round(w.price)) + " coins each";
				note.insert(0, price + (note.length() > 0 ? " · " : ""));
			}
			if (note.length() > 0)
			{
				in.add(new Line("   " + note, null, null, Theme.TEXT_FAINT, Theme.TEXT_FAINT));
			}
			List<String> worlds = new ArrayList<>();
			for (View.WorldView wd : w.worlds.subList(0, Math.min(3, w.worlds.size())))
			{
				worlds.add("W" + wd.world + " " + Format.estimate(wd.fullAt - view.now));
			}
			if (!worlds.isEmpty())
			{
				in.add(new Line("   Filling: " + String.join(", ", worlds), null, null, Theme.TEXT_FAINT, Theme.TEAL));
			}
		}
		if (s.wares.size() > 5)
		{
			in.add(new Line("and " + (s.wares.size() - 5) + " more", null, null, Theme.TEXT_FAINT, Theme.TEXT_FAINT));
		}
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	private JComponent runCard(View.RunView r)
	{
		Card card = new Card(new BorderLayout());
		StackPanel in = new StackPanel(2, 0);
		in.add(new Line(Format.day(r.start) + " " + Format.time(r.start), Format.duration(Math.max(60_000, r.end - r.start)), null, Theme.TEXT, Theme.GOLD));
		in.add(new Line(String.join(", ", r.shops), null, null));
		String worlds = r.worlds + (r.worlds == 1 ? " world" : " worlds") + " · " + r.visits + (r.visits == 1 ? " visit" : " visits");
		in.add(new Line(worlds, r.spent.isEmpty() ? null : spentText(r.spent), null));
		List<Map.Entry<String, Long>> bought = new ArrayList<>(r.bought.entrySet());
		bought.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
		List<String> top = new ArrayList<>();
		for (Map.Entry<String, Long> e : bought.subList(0, Math.min(3, bought.size())))
		{
			top.add(Format.amount(e.getValue()) + " " + e.getKey());
		}
		if (!top.isEmpty())
		{
			in.add(new Line(String.join(", ", top) + (bought.size() > 3 ? " and more" : ""), null, null, Theme.TEXT_FAINT, Theme.TEXT_FAINT));
		}
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	private static String spentText(Map<String, Long> spent)
	{
		List<String> parts = new ArrayList<>();
		spent.forEach((k, v) -> parts.add(Format.amount(v) + ("Coins".equals(k) ? " coins" : " " + k.toLowerCase(Locale.ENGLISH))));
		return String.join(", ", parts);
	}

	// ---- getting more ----

	/** The ways to get more of what is short, or of what is used most when nothing is. */
	private void getMore(StackPanel p)
	{
		View v = view;
		List<String> stores = new ArrayList<>();
		for (View.Want w : v.wants)
		{
			stores.add(w.store);
		}
		// What is short first, then the stores used fastest.
		List<View.StoreView> used = new ArrayList<>(v.stores);
		used.removeIf(sv -> sv.runway.burn <= 0 || com.codex.ironworks.core.Catalogue.money(sv.store));
		used.sort((a, b) -> Double.compare(b.runway.burn * worthOf(b), a.runway.burn * worthOf(a)));
		for (View.StoreView sv : used)
		{
			if (stores.size() >= 4)
			{
				break;
			}
			if (!stores.contains(sv.store))
			{
				stores.add(sv.store);
			}
		}
		String title = "Getting more";
		List<View.Getting> list = new ArrayList<>();
		for (String store : stores)
		{
			View.Getting g = v.getting.get(store);
			if (g != null && !g.ways.isEmpty())
			{
				list.add(g);
			}
		}
		if (list.isEmpty())
		{
			return;
		}
		p.add(new SectionHeader(title, "ways to get more"));
		for (View.Getting g : list.subList(0, Math.min(6, list.size())))
		{
			Card card = new Card(new BorderLayout());
			StackPanel in = new StackPanel(2, 0);
			in.add(new IconLine(g.icon, g.store, null, icons, Theme.GOLD_BRIGHT, Theme.TEXT_MUTED));
			int shown = 0;
			for (View.WayView w : g.ways)
			{
				if (shown++ == 3)
				{
					break;
				}
				Color tint = w.open ? Palette.of(w.kind.getSource()) : Theme.TEXT_FAINT;
				String figure = w.perHour > 0 ? Words.rate(w.perHour) + "/h"
					: !w.open && w.skill != null ? Ui.skill(w.skill) + " " + w.level : null;
				in.add(new Line(w.text, figure, tint, w.open ? Theme.TEXT : Theme.TEXT_FAINT, w.perHour > 0 ? Theme.TEAL : Theme.TEXT_FAINT));
			}
			card.add(in, BorderLayout.CENTER);
			p.add(card);
		}
		p.add(foot("A pace per hour is your own, timed while you were at it."));
	}

	/** Ranks busy stores by how much of the player's play they eat, so feathers do not crowd out food. */
	private static double worthOf(View.StoreView sv)
	{
		return sv.family == Family.FOOD || sv.family == Family.POTIONS ? 50 : sv.family == Family.RUNES || sv.family == Family.AMMO ? 5 : 1;
	}

	// ---- make ----

	private JComponent workshop()
	{
		StackPanel p = new StackPanel(6, 8);
		View v = view;
		Workshop w = v.workshop;
		if (w.plans().isEmpty())
		{
			p.add(note(v.stores.isEmpty()
				? "Log in and open your bank, and Ironworks works out the experience waiting in your stores."
				: "Nothing in your stores makes experience yet. Herbs, bones, logs, hides, ores and raw fish all show here."));
			return p;
		}
		Card hero = new Card(new BorderLayout());
		hero.highlighted(true);
		StackPanel top = new StackPanel(3, 0);
		top.add(new Figure(Format.amount(Math.round(w.total())), "xp in your stores", Theme.GOLD_BRIGHT));
		if (w.lockedTotal() > 0)
		{
			top.add(new Line("More above your levels", "+" + Format.amount(Math.round(w.lockedTotal())), null, Theme.TEXT_MUTED, Theme.TEXT_FAINT));
		}
		top.add(new Line("Bones counted at", v.altar.toString(), null, Theme.TEXT_MUTED, Theme.TEXT_FAINT));
		hero.add(top, BorderLayout.CENTER);
		p.add(hero);
		for (Workshop.Plan plan : w.plans())
		{
			p.add(skillCard(plan));
		}
		p.add(foot("Each store goes to its best use first, and unfinished potions and bars count toward what they make. Choose where bones go in the settings."));
		return p;
	}

	private JComponent skillCard(Workshop.Plan plan)
	{
		View v = view;
		Card card = new Card(new BorderLayout());
		card.accent(Theme.GOLD_DIM);
		StackPanel in = new StackPanel(2, 0);
		String name = Ui.skill(plan.skill);
		String total = plan.xp > 0 ? Format.amount(Math.round(plan.xp)) + " xp" : "+" + Format.amount(Math.round(plan.lockedXp)) + " later";
		in.add(new Line(name, total, null, Theme.GOLD_BRIGHT, plan.xp > 0 ? Theme.GOLD : Theme.TEXT_FAINT));
		Integer xpNow = v.xp.get(plan.skill);
		if (xpNow != null && plan.xp > 0)
		{
			int from = Experience.getLevelForXp(xpNow);
			int to = Experience.getLevelForXp((int) Math.min(Experience.MAX_SKILL_XP, xpNow + Math.round(plan.xp)));
			in.add(new Line(to > from ? "Level " + from + " to " + to : "Level " + from + ", toward " + Math.min(99, from + 1), null, null,
				Theme.TEXT_MUTED, Theme.TEXT_MUTED));
		}
		int shown = 0;
		for (Workshop.Make m : plan.makes)
		{
			if (shown++ == 4)
			{
				break;
			}
			String label = Format.amount(m.times) + " × " + m.recipe.product;
			String figure = m.locked ? "needs " + m.recipe.level : Format.amount(Math.round(m.xp));
			in.add(new IconLine(m.recipe.productItem, label, figure, icons, m.locked ? Theme.TEXT_FAINT : Theme.TEXT,
				m.locked ? Theme.TEXT_FAINT : Theme.TEXT_MUTED));
		}
		if (plan.makes.size() > 4)
		{
			in.add(new Line("and " + (plan.makes.size() - 4) + " more", null, null, Theme.TEXT_FAINT, Theme.TEXT_FAINT));
		}
		for (Workshop.Short s : plan.shorts.subList(0, Math.min(2, plan.shorts.size())))
		{
			in.add(new Line("Short " + Format.amount(s.need) + " " + s.input.toLowerCase(Locale.ENGLISH),
				"+" + Format.amount(Math.round(s.xp)), Theme.EMBER, Theme.EMBER, Theme.EMBER));
		}
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	// ---- guide ----

	private JComponent guide()
	{
		StackPanel p = new StackPanel(6, 8);
		View v = view;
		View.GuideView g = v.guide;
		if (!v.loggedIn)
		{
			p.add(note("Log in and the guide reads your quests, levels and bank to find where you are on the route."));
		}
		if (g.current == null)
		{
			if (g.total > 0 && v.loggedIn)
			{
				p.add(note("You have finished the route. Everything from here is yours to choose."));
			}
		}
		else
		{
			p.add(stepCard(g));
		}

		if (!g.needs.isEmpty())
		{
			p.add(new SectionHeader("Supplies ahead", "owned / needed"));
			Card card = new Card(new BorderLayout());
			StackPanel in = new StackPanel(2, 0);
			for (View.NeedView n : g.needs.subList(0, Math.min(8, g.needs.size())))
			{
				boolean enough = n.have >= n.need;
				String figure = Format.amount(n.have) + " / " + Format.amount(n.need);
				in.add(new IconLine(n.icon, n.item + (n.stepsAway == 0 ? ", now" : ""), figure, icons, Theme.TEXT,
					enough ? Theme.GREEN : Theme.EMBER));
			}
			if (g.needs.size() > 8)
			{
				in.add(new Line("and " + (g.needs.size() - 8) + " more further on", null, null, Theme.TEXT_FAINT, Theme.TEXT_FAINT));
			}
			card.add(in, BorderLayout.CENTER);
			p.add(card);
		}

		if (!g.next.isEmpty())
		{
			p.add(new SectionHeader("Coming up", ""));
			Card card = new Card(new BorderLayout());
			StackPanel in = new StackPanel(3, 0);
			for (View.StepView s : g.next)
			{
				in.add(new Line(s.text + (s.optional ? " (optional)" : ""), null, Theme.GOLD_DIM, Theme.TEXT_MUTED, Theme.TEXT_MUTED));
			}
			card.add(in, BorderLayout.CENTER);
			p.add(card);
		}

		if (!g.sections.isEmpty())
		{
			p.add(new SectionHeader("The route", g.done + " of " + g.total));
			Card card = new Card(new BorderLayout());
			StackPanel in = new StackPanel(4, 0);
			for (View.SectionView sv : g.sections)
			{
				double share = sv.all == 0 ? 0 : sv.done / (double) sv.all;
				in.add(new Meter(sv.name, sv.done + " / " + sv.all, share, share >= 1 ? Theme.GREEN : Theme.GOLD));
			}
			card.add(in, BorderLayout.CENTER);
			p.add(card);
		}
		p.add(foot("Steps tick themselves as the game shows them done: quests, levels, and items in your bank or pack. "
			+ "Done one another way? Mark it done."));
		return p;
	}

	private JComponent stepCard(View.GuideView g)
	{
		View.StepView s = g.current;
		Card card = new Card(new BorderLayout());
		card.accent(Theme.GOLD);
		card.highlighted(true);
		StackPanel in = new StackPanel(3, 0);
		in.add(new Line(s.section.toUpperCase(Locale.ENGLISH), g.done + " of " + g.total, null, Theme.GOLD_DIM, Theme.TEXT_FAINT));
		in.add(new Progress(g.total == 0 ? 0 : g.done / (double) g.total));
		Line text = new Line(s.text, null, null, Theme.TEXT, Theme.TEXT);
		text.setFont(Theme.body());
		in.add(text);
		if (s.optional)
		{
			in.add(new Line("Optional", null, null, Theme.TEXT_FAINT, Theme.TEXT_FAINT));
		}
		for (String tip : s.tips)
		{
			in.add(new Line(tip, null, null, Theme.GOLD_DIM, Theme.GOLD_DIM));
		}
		for (View.CheckView c : s.checks)
		{
			in.add(new Line(c.label, c.figure, c.ok ? Theme.GREEN : Theme.EMBER, Theme.TEXT_MUTED, c.ok ? Theme.GREEN : Theme.TEXT));
		}
		for (String hint : s.hints)
		{
			in.add(new Line(hint, null, null, Theme.TEAL, Theme.TEAL));
		}
		if (g.following && g.way != null)
		{
			in.add(new Line(g.way.equals("Here") ? "You are here" : "Head " + g.way, null, null, Theme.GOLD, Theme.GOLD));
		}
		JPanel buttons = new JPanel(new java.awt.GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.setBorder(new EmptyBorder(4, 0, 0, 0));
		String key = s.key;
		buttons.add(new CodexButton(g.following ? "Stop pointing" : "Point the way", !g.following, () -> actions.follow(!g.following)));
		buttons.add(new CodexButton("Mark done", false, () -> actions.tick(key, true)));
		in.add(buttons);
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	/** A thin bar of how far through the route the player is. */
	private static final class Progress extends JComponent
	{
		private final double share;

		Progress(double share)
		{
			this.share = share;
			setOpaque(false);
			setPreferredSize(new Dimension(10, 8));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			Bar.paint(g, 0, 2, getWidth(), 4, share);
			g.dispose();
		}
	}

	// ---- find ----

	private JComponent find()
	{
		StackPanel p = new StackPanel(6, 8);
		View v = view;
		String query = search.getText().trim().toLowerCase(Locale.ENGLISH);
		if (!query.isEmpty())
		{
			List<View.Found> hits = new ArrayList<>();
			for (View.Found f : v.found)
			{
				if (f.name.toLowerCase(Locale.ENGLISH).contains(query))
				{
					hits.add(f);
				}
			}
			hits.sort((a, b) ->
			{
				boolean as = a.name.toLowerCase(Locale.ENGLISH).startsWith(query);
				boolean bs = b.name.toLowerCase(Locale.ENGLISH).startsWith(query);
				return as != bs ? (as ? -1 : 1) : a.name.compareToIgnoreCase(b.name);
			});
			p.add(new SectionHeader(hits.isEmpty() ? "Nothing found" : "Found", hits.isEmpty() ? "" : String.valueOf(hits.size())));
			if (hits.isEmpty())
			{
				p.add(note("Nothing called \"" + search.getText().trim() + "\" in the places Ironworks has seen. Open a storage once and it is counted."));
			}
			for (View.Found f : hits.subList(0, Math.min(FOUND, hits.size())))
			{
				p.add(foundRow(f));
			}
			return p;
		}
		if (v.death != null)
		{
			p.add(deathCard(v.death));
		}
		if (v.spots.isEmpty())
		{
			p.add(note(v.loggedIn
				? "Open your bank, seed vault and looting bag once, and you can find anything you own from here."
				: "Log in to find anything you own: bank, seed vault, looting bag, potion storage and more."));
			return p;
		}
		p.add(new SectionHeader("Where things are", v.found.size() + " kinds"));
		for (View.SpotView sv : v.spots)
		{
			if (sv.kinds == 0)
			{
				continue;
			}
			Card card = new Card(new BorderLayout());
			StackPanel in = new StackPanel(2, 0);
			in.add(new Line(sv.spot.getLabel(), sv.worth > 0 ? Format.coins(sv.worth) : null, null, Theme.GOLD_BRIGHT, Theme.GOLD));
			in.add(new Line(sv.kinds + (sv.kinds == 1 ? " kind of item" : " kinds of items"), "seen " + Format.ago(v.now - sv.at), null,
				Theme.TEXT_MUTED, Theme.TEXT_FAINT));
			if (!sv.top.isEmpty())
			{
				in.add(wells(sv.top));
			}
			card.add(in, BorderLayout.CENTER);
			p.add(card);
		}
		p.add(foot("Type above to find anything. Each place is as you last had it open."));
		return p;
	}

	private JComponent foundRow(View.Found f)
	{
		Card card = new Card(new BorderLayout());
		StackPanel in = new StackPanel(1, 0);
		in.add(new IconLine(f.icon, f.name, Format.amount(f.total) + (f.doses ? " doses" : ""), icons, Theme.TEXT, Theme.GOLD));
		List<String> where = new ArrayList<>();
		f.where.forEach((spot, n) -> where.add(spot.getLabel() + " " + Format.amount(n)));
		in.add(new Line(String.join(" · ", where), null, null, Theme.TEXT_MUTED, Theme.TEXT_MUTED));
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	private JComponent deathCard(View.DeathView d)
	{
		Card card = new Card(new BorderLayout());
		card.accent(Theme.BLOOD);
		card.highlighted(true);
		StackPanel in = new StackPanel(2, 0);
		boolean grave = d.graveLeft > 0;
		in.add(new Line(grave ? "At your grave" : "At Death's Office", d.worth > 0 ? Format.coins(d.worth) : null, Theme.BLOOD, Theme.TEXT, Theme.EMBER));
		in.add(new Line(grave ? Format.estimate(d.graveLeft) + " of play left, world " + d.world
			: "Reclaim it before you die again, or it is lost", null, null, grave ? Theme.EMBER : Theme.TEXT_MUTED, null));
		in.add(wells(d.items));
		if (d.items.size() > 4)
		{
			in.add(new Line("and " + (d.items.size() - 4) + " more items", null, null, Theme.TEXT_FAINT, Theme.TEXT_FAINT));
		}
		JPanel holder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		holder.setOpaque(false);
		holder.add(new CodexButton("Taken back", false, actions::forgetDeath));
		in.add(holder);
		card.add(in, BorderLayout.CENTER);
		return card;
	}

	/** Up to four items in their slots, from item id and quantity pairs. */
	private JComponent wells(List<long[]> items)
	{
		JPanel wells = new JPanel(new java.awt.GridLayout(1, 4, 4, 0));
		wells.setOpaque(false);
		int shown = 0;
		for (long[] it : items)
		{
			if (shown == 4)
			{
				break;
			}
			wells.add(new Slot((int) it[0], it[1], "", icons));
			shown++;
		}
		while (shown++ < 4)
		{
			JPanel blank = new JPanel();
			blank.setOpaque(false);
			wells.add(blank);
		}
		return wells;
	}

	// ---- small pieces ----

	private static JComponent note(String text)
	{
		Card card = new Card(new BorderLayout());
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		return card;
	}

	private static JComponent foot(String text)
	{
		JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.setBorder(new EmptyBorder(4, 2, 0, 2));
		holder.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_FAINT, WIDTH - 14), BorderLayout.CENTER);
		return holder;
	}

	/** A large figure with a quiet line beside it. */
	private static final class Figure extends Fitted
	{
		private final String figure;
		private final String label;
		private final Color tint;

		Figure(String figure, String label, Color tint)
		{
			this.figure = figure;
			this.label = label;
			this.tint = tint;
			setFont(Theme.small());
		}

		private int figureWidth()
		{
			return Crest.largeWidth(this, figure);
		}

		@Override
		int heightFor(int width)
		{
			int n = Ui.wrap(label, getFontMetrics(getFont()), width - figureWidth() - 6).size();
			return Math.max(34, 12 + n * 13);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int fw = Crest.paintLarge(g, figure, 0, 2, tint);
			g.setFont(getFont());
			List<String> rows = Ui.wrap(label, g.getFontMetrics(), getWidth() - fw - 6);
			int y = rows.size() == 1 ? 22 : 22 - 13 * (rows.size() - 1) / 2;
			for (int i = 0; i < rows.size(); i++)
			{
				Ui.shadowed(g, rows.get(i), fw + 6, y + i * 13, Theme.TEXT_MUTED);
			}
			g.dispose();
		}
	}

	/** A label, a share and a thin bar of it; a long label wraps above the bar. */
	private static final class Meter extends Fitted
	{
		private final String label;
		private final String figure;
		private final double share;
		private final Color color;

		Meter(String label, String figure, double share, Color color)
		{
			this.label = label;
			this.figure = figure;
			this.share = share;
			this.color = color;
			setFont(Theme.small());
		}

		private List<String> rows(FontMetrics fm, int width)
		{
			return Ui.wrap(label, fm, width - fm.stringWidth(figure) - 6);
		}

		@Override
		int heightFor(int width)
		{
			return 7 + rows(getFontMetrics(getFont()), width).size() * 13;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(getFont());
			FontMetrics fm = g.getFontMetrics();
			int w = getWidth();
			List<String> rows = rows(fm, w);
			Ui.shadowed(g, figure, w - fm.stringWidth(figure), 11, Theme.TEXT_MUTED);
			for (int i = 0; i < rows.size(); i++)
			{
				Ui.shadowed(g, rows.get(i), 0, 11 + i * 13, Theme.TEXT);
			}
			int bar = 2 + rows.size() * 13;
			g.setColor(Theme.INK_DEEP);
			g.fillRoundRect(0, bar, w, 3, 3, 3);
			g.setColor(color);
			g.fillRoundRect(0, bar, Math.max(3, (int) Math.round(w * share)), 3, 3, 3);
			g.dispose();
		}
	}

	/** A small item picture, a label and a figure; a long label wraps beside the picture. */
	private static final class IconLine extends Fitted
	{
		private static final int ICON = 18;
		private static final int ROW = 13;
		private final int item;
		private final String label;
		private final String figure;
		private final ItemIcons icons;
		private final Color labelColor;
		private final Color figureColor;

		IconLine(int item, String label, String figure, ItemIcons icons, Color labelColor, Color figureColor)
		{
			this.item = item;
			this.label = label;
			this.figure = figure;
			this.icons = icons;
			this.labelColor = labelColor;
			this.figureColor = figureColor;
			setFont(Theme.small());
			setToolTipText(figure == null ? label : label + ": " + figure);
		}

		private List<String> rows(FontMetrics fm, int width)
		{
			int fw = figure == null ? 0 : fm.stringWidth(figure) + 6;
			return Ui.wrap(label, fm, width - ICON - 5 - fw);
		}

		@Override
		int heightFor(int width)
		{
			int n = rows(getFontMetrics(getFont()), width).size();
			return Math.max(ICON + 2, n * ROW + 6);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			BufferedImage icon = icons == null || item < 0 ? null : icons.icon(item, this::repaint);
			if (icon != null)
			{
				double scale = Math.min(ICON / (double) icon.getWidth(), ICON / (double) icon.getHeight());
				int w = (int) Math.round(icon.getWidth() * scale);
				int h = (int) Math.round(icon.getHeight() * scale);
				g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(icon, (ICON - w) / 2, (ICON - h) / 2 + 1, w, h, null);
			}
			g.setFont(getFont());
			FontMetrics fm = g.getFontMetrics();
			int x = ICON + 5;
			List<String> rows = rows(fm, getWidth());
			int base = rows.size() == 1 ? (getHeight() + fm.getAscent()) / 2 - 1 : 3 + fm.getAscent();
			if (figure != null)
			{
				Ui.shadowed(g, figure, getWidth() - fm.stringWidth(figure), base, figureColor);
			}
			for (int i = 0; i < rows.size(); i++)
			{
				Ui.shadowed(g, rows.get(i), x, base + i * ROW, labelColor);
			}
			g.dispose();
		}
	}

	/** The search box on the Find page, drawn as an engraved well. */
	private static final class SearchField extends JTextField
	{
		SearchField()
		{
			setOpaque(false);
			setFont(Theme.body());
			setForeground(Theme.TEXT);
			setCaretColor(Theme.GOLD_BRIGHT);
			setBorder(new EmptyBorder(4, 8, 4, 8));
			setPreferredSize(new Dimension(10, 26));
			setToolTipText("Find anything you own by name");
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setColor(Theme.INK_DEEP);
			g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
			g.setColor(isFocusOwner() ? Theme.GOLD_DIM : Theme.STROKE_SOFT);
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
			if (getText().isEmpty())
			{
				g.setFont(Theme.body());
				Ui.shadowed(g, "Find anything you own", 8, (getHeight() + g.getFontMetrics().getAscent()) / 2 - 2, Theme.TEXT_FAINT);
			}
			g.dispose();
			super.paintComponent(graphics);
		}
	}

	/** An item in its slot with the units counted, like a bank slot. */
	private static final class Slot extends JComponent
	{
		private final int item;
		private final long units;
		private final ItemIcons icons;

		Slot(int item, long units, String name, ItemIcons icons)
		{
			this.item = item;
			this.units = units;
			this.icons = icons;
			setOpaque(false);
			setPreferredSize(new Dimension(Well.SIZE + 5, Well.SIZE + 1));
			setToolTipText(name.isEmpty() ? null : name + ": " + Format.amount(units));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			BufferedImage icon = icons == null || item < 0 ? null : icons.icon(item, this::repaint);
			Well.paint(g, 0, 0, icon, null);
			g.setFont(Theme.small());
			Ui.shadowed(g, Format.stack(units), 2, 10, Format.stackColor(units));
			g.dispose();
		}
	}
}
