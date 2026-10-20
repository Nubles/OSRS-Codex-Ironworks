package com.codex.ironworks.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.runelite.api.Skill;

/**
 * Says where each change to what the player carries came from or went to, from what else happened
 * on the same tick: the experience gained, the menu option just clicked, a shop or trade window
 * being open, a death. Changes made at a bank or other storage are moves, not gains or losses, and
 * never reach here. Pure, so every rule can be tested without a client.
 */
public final class Classify
{
	static final Set<Skill> GATHERING = EnumSet.of(Skill.FISHING, Skill.MINING, Skill.WOODCUTTING, Skill.HUNTER,
		Skill.THIEVING, Skill.AGILITY, Skill.SAILING);
	static final Set<Skill> PRODUCTION = EnumSet.of(Skill.HERBLORE, Skill.COOKING, Skill.SMITHING, Skill.FLETCHING,
		Skill.CRAFTING, Skill.CONSTRUCTION, Skill.RUNECRAFT, Skill.FIREMAKING);
	static final Set<Skill> COMBAT = EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED,
		Skill.HITPOINTS, Skill.SLAYER);
	/** The containers a player carries that items go into and come out of unseen. */
	private static final Pattern CONTAINER = Pattern.compile(".*\\b(sack|bag|box|barrel|basket|pouch|kit|vessel)$");

	private Classify()
	{
	}

	/** A menu option the player clicked in the last couple of ticks. */
	public static final class Action
	{
		final String option;
		final String store;
		final boolean container;
		final boolean pack;

		/**
		 * @param option the option as shown, such as "Eat" or "Take"
		 * @param target the item or thing it was clicked on, colour tags and all
		 * @param store the store the clicked item belongs to, or null
		 * @param pack whether the clicked item is a pack bought from a shop
		 */
		public Action(String option, String target, String store, boolean pack)
		{
			this.option = option == null ? "" : option.toLowerCase(Locale.ENGLISH).trim();
			String plain = target == null ? "" : target.replaceAll("<[^>]*>", "").toLowerCase(Locale.ENGLISH).trim();
			this.store = store;
			this.container = CONTAINER.matcher(plain).matches();
			this.pack = pack;
		}

		boolean is(String... options)
		{
			for (String o : options)
			{
				if (option.equals(o) || option.startsWith(o + "-"))
				{
					return true;
				}
			}
			return false;
		}
	}

	/** Everything known about the tick the change happened on. */
	public static final class Context
	{
		boolean shop;
		boolean trade;
		boolean died;
		Set<Skill> xp = Collections.emptySet();
		List<Action> actions = Collections.emptyList();
		Map<Skill, Long> recent = Collections.emptyMap();

		public Context shop(boolean open)
		{
			shop = open;
			return this;
		}

		public Context trade(boolean open)
		{
			trade = open;
			return this;
		}

		public Context died(boolean value)
		{
			died = value;
			return this;
		}

		/** The skills that gained experience on this tick or the one before. */
		public Context xp(Set<Skill> skills)
		{
			xp = skills;
			return this;
		}

		public Context actions(List<Action> list)
		{
			actions = list;
			return this;
		}

		/** Experience gained per skill over the last quarter hour, to place items that arrive unseen. */
		public Context recent(Map<Skill, Long> xpBySkill)
		{
			recent = xpBySkill;
			return this;
		}
	}

	/**
	 * Files one tick's changes.
	 *
	 * @param delta units of each store gained (positive) or lost (negative) on the tick
	 * @param sacks units of each store the player has put into carried containers, drawn on as
	 *              they come out again and added to as they go in
	 */
	public static List<Flow> tick(Map<String, Long> delta, Context ctx, Map<String, Long> sacks)
	{
		List<Flow> out = new ArrayList<>();
		boolean anyGain = false;
		boolean anyLoss = false;
		for (Map.Entry<String, Long> e : delta.entrySet())
		{
			anyGain |= e.getValue() > 0 && !Catalogue.money(e.getKey());
			anyLoss |= e.getValue() < 0 && !Catalogue.money(e.getKey());
		}
		for (Map.Entry<String, Long> e : delta.entrySet())
		{
			long units = e.getValue();
			if (units > 0)
			{
				gain(e.getKey(), units, ctx, sacks, anyLoss, out);
			}
			else if (units < 0)
			{
				loss(e.getKey(), -units, ctx, sacks, anyGain, out);
			}
		}
		return out;
	}

	private static void gain(String store, long units, Context ctx, Map<String, Long> sacks, boolean anyLoss, List<Flow> out)
	{
		boolean money = Catalogue.money(store);
		if (ctx.shop)
		{
			out.add(Flow.in(store, units, money ? Source.SOLD : Source.BOUGHT, null));
			return;
		}
		if (ctx.trade)
		{
			out.add(Flow.in(store, units, Source.TRADED, null));
			return;
		}
		Action action = clicked(ctx, store);
		if (action != null && action.is("take"))
		{
			out.add(Flow.in(store, units, Source.LOOTED, null));
			return;
		}
		Action any = latest(ctx);
		if (any != null && any.pack && any.is("open"))
		{
			out.add(Flow.in(store, units, Source.BOUGHT, null));
			return;
		}
		if (any != null && any.container && any.is("empty", "withdraw", "take", "check", "remove"))
		{
			// Out of a carried container: what went in is not new, the rest arrived there unseen.
			long back = Math.min(units, sacks.getOrDefault(store, 0L));
			if (back > 0)
			{
				put(sacks, store, -back);
			}
			if (units > back)
			{
				out.add(arrival(store, units - back, ctx.recent));
			}
			return;
		}
		if (money && ctx.xp.contains(Skill.MAGIC))
		{
			out.add(Flow.in(store, units, Source.ALCHED, null));
			return;
		}
		Skill skill = gainSkill(store, ctx.xp);
		if (skill != null)
		{
			Source source = skill == Skill.FARMING ? Source.FARMED
				: PRODUCTION.contains(skill) || skill == Skill.MAGIC ? Source.MADE
				: GATHERING.contains(skill) ? Source.GATHERED : Source.LOOTED;
			out.add(Flow.in(store, units, source, source == Source.LOOTED ? null : skill.getName()));
			return;
		}
		if (any != null && any.is("open", "search", "loot", "claim", "collect", "take-all"))
		{
			out.add(Flow.in(store, units, Source.REWARD, null));
			return;
		}
		out.add(Flow.in(store, units, Source.OTHER, null));
	}

	/** The skill a gain is put down to: farming first, then making or gathering as the item is. */
	private static Skill gainSkill(String store, Set<Skill> xp)
	{
		if (xp.isEmpty())
		{
			return null;
		}
		if (xp.contains(Skill.FARMING))
		{
			return Skill.FARMING;
		}
		Skill made = first(xp, PRODUCTION);
		Skill gathered = first(xp, GATHERING);
		if (made != null && gathered != null)
		{
			return Catalogue.gathered(store) ? gathered : made;
		}
		if (made != null)
		{
			return made;
		}
		if (gathered != null)
		{
			return gathered;
		}
		if (xp.contains(Skill.MAGIC) && first(xp, COMBAT) == null)
		{
			// Enchanting bolts, making planks: magic alone, no combat beside it.
			return Skill.MAGIC;
		}
		return first(xp, COMBAT);
	}

	private static void loss(String store, long units, Context ctx, Map<String, Long> sacks, boolean anyGain, List<Flow> out)
	{
		Family family = Catalogue.family(store);
		if (ctx.died)
		{
			out.add(Flow.out(store, units, Use.DIED, null));
			return;
		}
		if (ctx.shop)
		{
			out.add(Flow.out(store, units, family == Family.CURRENCY && anyGain ? Use.SPENT : Use.SOLD, null));
			return;
		}
		if (ctx.trade)
		{
			out.add(Flow.out(store, units, Use.TRADED, null));
			return;
		}
		Action action = clicked(ctx, store);
		if (action != null)
		{
			if (action.is("eat", "drink", "quaff", "sip"))
			{
				out.add(Flow.out(store, units, Use.CONSUMED, null));
				return;
			}
			if (action.is("drop", "destroy", "release"))
			{
				out.add(Flow.out(store, units, Use.DROPPED, null));
				return;
			}
			if (action.is("bury", "scatter"))
			{
				out.add(Flow.out(store, units, Use.OFFERED, Skill.PRAYER.getName()));
				return;
			}
		}
		Action any = latest(ctx);
		if (any != null && any.container && any.is("fill", "use", "store", "deposit", "add"))
		{
			put(sacks, store, units);
			return;
		}
		Set<Skill> xp = ctx.xp;
		if (xp.contains(Skill.PRAYER))
		{
			out.add(Flow.out(store, units, Use.OFFERED, Skill.PRAYER.getName()));
			return;
		}
		if (xp.contains(Skill.FARMING))
		{
			out.add(Flow.out(store, units, Use.PLANTED, Skill.FARMING.getName()));
			return;
		}
		if (family == Family.RUNES && (xp.contains(Skill.MAGIC) || first(xp, COMBAT) != null))
		{
			out.add(Flow.out(store, units, Use.CAST, null));
			return;
		}
		if (family == Family.AMMO && xp.contains(Skill.RANGED))
		{
			out.add(Flow.out(store, units, Use.FIRED, null));
			return;
		}
		Skill made = first(xp, PRODUCTION);
		if (made == null && xp.contains(Skill.MAGIC))
		{
			made = Skill.MAGIC;
		}
		if (made != null)
		{
			out.add(Flow.out(store, units, Use.MADE_INTO, made.getName()));
			return;
		}
		if (family == Family.RUNES)
		{
			// Teleports and spells that give no experience still use runes.
			out.add(Flow.out(store, units, Use.CAST, null));
			return;
		}
		if (family == Family.AMMO)
		{
			// A shot that misses gives no experience but is spent all the same.
			out.add(Flow.out(store, units, Use.FIRED, null));
			return;
		}
		out.add(Flow.out(store, units, Use.USED, null));
	}

	/**
	 * Something that turned up without being seen arrive, such as fish emptied from a barrel or
	 * herbs from a herb sack: put down to what the player has been doing lately.
	 */
	public static Flow arrival(String store, long units, Map<Skill, Long> recent)
	{
		Skill farming = recent.getOrDefault(Skill.FARMING, 0L) > 0 ? Skill.FARMING : null;
		Skill gathered = top(recent, GATHERING);
		Skill made = top(recent, PRODUCTION);
		Skill fought = top(recent, COMBAT);
		if (Catalogue.gathered(store))
		{
			if (gathered != null && (farming == null || recent.get(gathered) >= recent.get(Skill.FARMING)))
			{
				return Flow.in(store, units, Source.GATHERED, gathered.getName());
			}
			if (farming != null)
			{
				return Flow.in(store, units, Source.FARMED, farming.getName());
			}
		}
		else if (made != null)
		{
			return Flow.in(store, units, Source.MADE, made.getName());
		}
		if (fought != null)
		{
			return Flow.in(store, units, Source.LOOTED, null);
		}
		return Flow.in(store, units, Source.OTHER, null);
	}

	private static Action clicked(Context ctx, String store)
	{
		for (int i = ctx.actions.size() - 1; i >= 0; i--)
		{
			Action a = ctx.actions.get(i);
			if (store.equals(a.store))
			{
				return a;
			}
		}
		return null;
	}

	private static Action latest(Context ctx)
	{
		return ctx.actions.isEmpty() ? null : ctx.actions.get(ctx.actions.size() - 1);
	}

	private static Skill first(Set<Skill> xp, Set<Skill> kind)
	{
		for (Skill s : xp)
		{
			if (kind.contains(s))
			{
				return s;
			}
		}
		return null;
	}

	private static Skill top(Map<Skill, Long> recent, Set<Skill> kind)
	{
		Skill best = null;
		long most = 0;
		for (Map.Entry<Skill, Long> e : recent.entrySet())
		{
			if (kind.contains(e.getKey()) && e.getValue() > most)
			{
				most = e.getValue();
				best = e.getKey();
			}
		}
		return best;
	}

	private static void put(Map<String, Long> map, String key, long add)
	{
		long v = map.getOrDefault(key, 0L) + add;
		if (v <= 0)
		{
			map.remove(key);
		}
		else
		{
			map.put(key, v);
		}
	}
}
