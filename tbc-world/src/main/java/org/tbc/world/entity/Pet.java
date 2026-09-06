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
    public final java.util.List<Integer> spells = new java.util.ArrayList<>();

    /** CMaNGOS Pet::learnSpell. Fire Shield 2949 teaches 2947. */
    public void learnSpell(int spellId) {
        if (spellId <= 0 || spells.contains(spellId)) {
            return;
        }
        spells.add(spellId);
    }
}
