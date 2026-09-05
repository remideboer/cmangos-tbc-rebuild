package org.tbc.world.loot;

/** One visible SMSG_LOOT_RESPONSE slot. loot.md */
public record LootSlot(int slot, int itemId, int count, int displayId) {
}
