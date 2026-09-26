package org.tbc.world.entity;

public final class Item {
    public long guid;
    public int entry;
    public int count = 1;
    public int bag;
    public int slot;
    public int durability;
    public int maxDurability;
    public int ownerGuid;
    public int displayId;
    public int inventoryType;
    public int enchant;
    public int tempEnchant;
    public int quality;
    public int flags;
    public boolean soulbound;
    /** ItemPrototype Delay (ms) — Seal of Righteousness / client tooltips use proto, not UNIT_FIELD. */
    public int delay;
    /** ItemPrototype DmgMin[0] / DmgMax[0]. */
    public float dmgMin;
    public float dmgMax;

    public Item(long guid, int entry) {
        this.guid = guid;
        this.entry = entry;
    }

    /** Copy weapon line used by the 8606 client for SoR buff/action-bar tooltips. */
    public void applyWeaponLine(int inventoryType, int delayMs, float minDmg, float maxDmg) {
        this.inventoryType = inventoryType;
        this.delay = delayMs;
        this.dmgMin = minDmg;
        this.dmgMax = maxDmg;
    }
}
