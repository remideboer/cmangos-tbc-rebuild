package org.tbc.world.entity;

public final class Item {
    public long guid;
    public int entry;
    public int count = 1;
    public int bag;
    public int slot;
    public int durability;
    public int ownerGuid;
    public int displayId;
    public int inventoryType;
    public int enchant;
    public int quality;
    public int flags;
    public boolean soulbound;

    public Item(long guid, int entry) {
        this.guid = guid;
        this.entry = entry;
    }
}
