package org.tbc.world.entity;

public final class Pet {
    /** Pet.h PetType */
    public static final int SUMMON_PET = 0;
    public static final int HUNTER_PET = 1;
    /** ObjectMgr.h MAX_PET_NAME; World.cpp MinPetName default. */
    public static final int MAX_PET_NAME = 12;
    public static final int MIN_PET_NAME = 2;
    /** Pet.h PetNameInvalidReason */
    public static final int PET_NAME_SUCCESS = 0;
    public static final int PET_NAME_TOO_SHORT = 3;
    public static final int PET_NAME_TOO_LONG = 4;
    /** Unit.h UNIT_CAN_BE_RENAMED on UNIT_BYTES_2_OFFSET_PET_FLAGS. */
    public static final int UNIT_CAN_BE_RENAMED = 0x01;
    public static final int UNIT_BYTES_2_OFFSET_PET_FLAGS = 2;

    public long guid;
    public int entry;
    public String name = "";
    public int level = 1;
    public int slot;
    public int happiness = 1;
    public int petType = SUMMON_PET;
    public boolean canRename;
    public int nameTimestamp;
    public final int[] actionBar = new int[10];
    public boolean summoned;
    public final java.util.List<Integer> spells = new java.util.ArrayList<>();

    /** ObjectMgr.cpp CheckPetName — length only (strict names off). */
    public static int checkName(String name) {
        int n = name.length();
        if (n > MAX_PET_NAME) {
            return PET_NAME_TOO_LONG;
        }
        if (n < MIN_PET_NAME) {
            return PET_NAME_TOO_SHORT;
        }
        return PET_NAME_SUCCESS;
    }

    /** UNIT_FIELD_BYTES_2 with pet flags in byte 2. */
    public int unitBytes2() {
        int flags = canRename ? UNIT_CAN_BE_RENAMED : 0;
        return flags << (8 * UNIT_BYTES_2_OFFSET_PET_FLAGS);
    }

    /** CMaNGOS Pet::learnSpell. Fire Shield 2949 teaches 2947. */
    public void learnSpell(int spellId) {
        if (spellId <= 0 || spells.contains(spellId)) {
            return;
        }
        spells.add(spellId);
    }
}
