package org.tbc.world.content;

/** Fallback ChrRaces / ChrClasses when DataDir DBC is not extracted. 8606 ids only. */
public final class ChrStatic {
    public record Race(int id, int faction, int modelM, int modelF, int teamLang, int cinematic, int expansion) {}
    public record Clazz(int id, int powerType) {}

    public static final Race[] RACES = {
            new Race(1, 1, 49, 50, 7, 81, 0),
            new Race(2, 2, 51, 52, 1, 21, 0),
            new Race(3, 3, 53, 54, 7, 101, 0),
            new Race(4, 4, 55, 56, 7, 61, 0),
            new Race(5, 5, 57, 58, 1, 2, 0),
            new Race(6, 6, 59, 60, 1, 141, 0),
            new Race(7, 115, 1563, 1564, 7, 101, 0),
            new Race(8, 116, 1478, 1479, 1, 21, 0),
            new Race(10, 1610, 15476, 15475, 1, 162, 1),
            new Race(11, 1629, 16125, 16126, 7, 163, 1)
    };

    public static Race race(int id) {
        for (Race r : RACES) {
            if (r.id == id) {
                return r;
            }
        }
        return RACES[0];
    }

    public static int team(int raceId) {
        return race(raceId).teamLang == 7 ? 469 : 67;
    }

    public static int powerType(int classId) {
        return switch (classId) {
            case 1 -> 1; // rage
            case 4 -> 3; // energy
            default -> 0; // mana
        };
    }

    public static boolean playable(int race, int clazz) {
        return race(race) != null && clazz >= 1 && clazz <= 11 && clazz != 6 && clazz != 10;
    }
}
