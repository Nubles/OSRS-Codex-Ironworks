package com.codex.ironworks.ui;

import com.codex.ironworks.core.Family;
import com.codex.ironworks.core.Runway;
import com.codex.ironworks.core.Source;
import com.codex.ironworks.core.Summary;
import com.codex.ironworks.core.Use;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Ironworks side panel: the crest, then three pages. Stores is every store with how long it
 * lasts at the player's own pace, Sources is where everything came from and went, and Shops is the
 * shopping list, the shops the player uses with their restock times, and past shop runs.
 */
public class IronworksPanel extends PluginPanel
{
	static final int WIDTH = 200;
	private static final String[] TABS = {"stores", "sources", "shops"};
	/** Stores shown per shelf before "Show all". */
	private static final int SHELF = 6;

	private final ItemIcons icons;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Stores", "Sources", "Shops");
	private final Segmented period = new Segmented(false, "Today", "7 days", "All time");
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane[] pages = {scroller(), scroller(), scroller()};
	private final boolean[] stale = {true, true, true};
	private final Set<String> open = new HashSet<>();
	private final Set<Family> shelvesOpen = new HashSet<>();

	private View view = new View();

	public IronworksPanel(ItemIcons icons)
	{
		super(false);
		this.icons = icons;
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
			content.add(pages[i], TABS[i]);
		}
		add(content, BorderLayout.CENTER);
		tabs.onChange(this::showTab);
		period.setSelected(1, false);
		period.onChange(i ->
		{
			stale[1] = true;
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
		JComponent body = page == 0 ? stores() : page == 1 ? sources() : shops();
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
					rebuild(0);
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
			rebuild(0);
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
			return p;
		}
		if (s.gained > 0)
		{
			p.add(selfCard(s));
		}
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
		p.add(new SectionHeader("Shopping list", wants.isEmpty() ? "" : wants.size() + (wants.size() == 1 ? " store" : " stores")));
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
	private static final class Figure extends JComponent
	{
		private final String figure;
		private final String label;
		private final Color tint;

		Figure(String figure, String label, Color tint)
		{
			this.figure = figure;
			this.label = label;
			this.tint = tint;
			setOpaque(false);
			setPreferredSize(new Dimension(10, 34));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int fw = Crest.paintLarge(g, figure, 0, 2, tint);
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			Ui.shadowed(g, Ui.clip(label, fm, getWidth() - fw - 6), fw + 6, 22, Theme.TEXT_MUTED);
			g.dispose();
		}
	}

	/** A label, a share and a thin bar of it. */
	private static final class Meter extends JComponent
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
			setOpaque(false);
			setPreferredSize(new Dimension(10, 20));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			int w = getWidth();
			Ui.shadowed(g, figure, w - fm.stringWidth(figure), 11, Theme.TEXT_MUTED);
			Ui.shadowed(g, Ui.clip(label, fm, w - fm.stringWidth(figure) - 6), 0, 11, Theme.TEXT);
			g.setColor(Theme.INK_DEEP);
			g.fillRoundRect(0, 15, w, 3, 3, 3);
			g.setColor(color);
			g.fillRoundRect(0, 15, Math.max(3, (int) Math.round(w * share)), 3, 3, 3);
			g.dispose();
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
			setToolTipText(name + ": " + Format.amount(units));
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
