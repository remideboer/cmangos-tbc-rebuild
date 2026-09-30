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
    /** ItemPrototype Class — ITEM_CLASS_WEAPON / ARMOR; offhand swing requires WEAPON. */
    public int itemClass;
    public int inventoryType;
    /** ItemPrototype SubClass — 2H axe/mace/sword/polearm/staff/spear for SoR handedness. */
    public int subClass;
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
        applyWeaponLine(inventoryType, 0, delayMs, minDmg, maxDmg);
    }

    public void applyWeaponLine(int inventoryType, int subClass, int delayMs, float minDmg, float maxDmg) {
        this.inventoryType = inventoryType;
        this.subClass = subClass;
        this.delay = delayMs;
        this.dmgMin = minDmg;
        this.dmgMax = maxDmg;
    }
}
