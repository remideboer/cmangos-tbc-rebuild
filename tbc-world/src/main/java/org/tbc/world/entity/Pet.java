package org.tbc.world.entity;

public final class Pet {
    public long guid;
    public int entry;
    public String name = "";
    public int level = 1;
    public int slot;
    public int happiness = 1;
    public final int[] actionBar = new int[10];
    public boolean summoned;
}
