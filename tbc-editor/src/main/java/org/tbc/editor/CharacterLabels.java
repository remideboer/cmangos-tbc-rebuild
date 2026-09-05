package org.tbc.editor;

/** 8606 race/class labels. Ids from ChrRaces / ChrClasses. */
public final class CharacterLabels {
    private CharacterLabels() {}

    public static String race(int id) {
        return switch (id) {
            case 1 -> "Human";
            case 2 -> "Orc";
            case 3 -> "Dwarf";
            case 4 -> "Night Elf";
            case 5 -> "Undead";
            case 6 -> "Tauren";
            case 7 -> "Gnome";
            case 8 -> "Troll";
            case 10 -> "Blood Elf";
            case 11 -> "Draenei";
            default -> String.valueOf(id);
        };
    }

    public static String clazz(int id) {
        return switch (id) {
            case 1 -> "Warrior";
            case 2 -> "Paladin";
            case 3 -> "Hunter";
            case 4 -> "Rogue";
            case 5 -> "Priest";
            case 7 -> "Shaman";
            case 8 -> "Mage";
            case 9 -> "Warlock";
            case 11 -> "Druid";
            default -> String.valueOf(id);
        };
    }

    public static String gender(int id) {
        return id == 1 ? "Female" : "Male";
    }
}
