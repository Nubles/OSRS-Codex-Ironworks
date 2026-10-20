package com.codex.ironworks.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CatalogueTest
{
	private final Catalogue.Lookup lookup = Catalogue.standard();

	@Test
	public void potionsAreOneStoreCountedInDoses()
	{
		Catalogue.Match four = lookup.match("Prayer potion(4)");
		Catalogue.Match one = lookup.match("Prayer potion(1)");
		assertEquals("Prayer potion", four.store);
		assertEquals(4, four.units);
		assertTrue(four.model);
		assertEquals("Prayer potion", one.store);
		assertEquals(1, one.units);
		assertFalse(one.model);
		assertEquals(Family.POTIONS, four.family);
	}

	@Test
	public void grimyAndCleanHerbsAreOneStore()
	{
		assertEquals("Ranarr weed", lookup.match("Grimy ranarr weed").store);
		assertEquals("Ranarr weed", lookup.match("Ranarr weed").store);
		assertFalse(lookup.match("Grimy ranarr weed").model);
		assertTrue(lookup.match("Ranarr weed").model);
		assertEquals(Family.HERBS, lookup.match("Grimy snapdragon").family);
	}

	@Test
	public void enchantedBoltsAreNotMistakenForDoses()
	{
		Catalogue.Match bolts = lookup.match("Ruby dragon bolts (e)");
		assertEquals("Ruby dragon bolts (e)", bolts.store);
		assertEquals(1, bolts.units);
		assertEquals(Family.AMMO, bolts.family);
	}

	@Test
	public void unknownItemsAreNoStore()
	{
		assertNull(lookup.match("Abyssal whip"));
		assertNull(lookup.match("null"));
		assertNull(lookup.match(""));
		assertNull(lookup.match(null));
		// A dose suffix on something that is not a potion is not a potion.
		assertNull(lookup.match("Games necklace(8)"));
	}

	@Test
	public void picksAndHiddenStoresFollowThePlayer()
	{
		Catalogue.Lookup own = Catalogue.with("Abyssal whip, games necklace(8)\n", "coins,  Feather");
		assertEquals(Family.PICKED, own.match("Abyssal whip").family);
		assertNull(own.match("Coins"));
		assertNull(own.match("Feather"));
		assertEquals("Shark", own.match("Shark").store);
	}

	@Test
	public void packsOpenIntoTheirStore()
	{
		assertEquals("Feather", lookup.packOf("Feather pack"));
		assertEquals("Eye of newt", lookup.packOf("Eye of newt pack"));
		assertEquals("Vial of water", lookup.packOf("Vial of water pack"));
		assertEquals("Air rune", lookup.packOf("Air rune pack"));
		assertNull(lookup.packOf("Shark"));
		assertNull(lookup.packOf("Backpack"));
	}

	@Test
	public void gatheredAndMadeAreToldApart()
	{
		assertTrue(Catalogue.gathered("Raw shark"));
		assertTrue(Catalogue.gathered("Coal"));
		assertTrue(Catalogue.gathered("Ranarr weed"));
		assertFalse(Catalogue.gathered("Shark"));
		assertFalse(Catalogue.gathered("Prayer potion"));
		assertFalse(Catalogue.gathered("Runite bar"));
		assertTrue(Catalogue.money("Coins"));
		assertFalse(Catalogue.money("Shark"));
	}
}
