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

    /** SkillLine.dbc ids from CMaNGOS lang_description (not invented). */
    public static int[] languageSkills(int raceId) {
        int faction = race(raceId).teamLang == 7 ? SKILL_LANG_COMMON : SKILL_LANG_ORCISH;
        int racial = switch (raceId) {
            case 3 -> SKILL_LANG_DWARVEN;
            case 4 -> SKILL_LANG_DARNASSIAN;
            case 5 -> SKILL_LANG_GUTTERSPEAK;
            case 6 -> SKILL_LANG_TAURAHE;
            case 7 -> SKILL_LANG_GNOMISH;
            case 8 -> SKILL_LANG_TROLL;
            case 10 -> SKILL_LANG_THALASSIAN;
            case 11 -> SKILL_LANG_DRAENEI;
            default -> 0;
        };
        if (racial == 0) {
            return new int[]{faction};
        }
        return new int[]{faction, racial};
    }

    /** Spell ids from CMaNGOS LanguageDesc (lang_description). */
    public static int[] languageSpells(int raceId) {
        int faction = race(raceId).teamLang == 7 ? SPELL_LANG_COMMON : SPELL_LANG_ORCISH;
        int racial = switch (raceId) {
            case 3 -> SPELL_LANG_DWARVISH;
            case 4 -> SPELL_LANG_DARNASSIAN;
            case 5 -> SPELL_LANG_GUTTERSPEAK;
            case 6 -> SPELL_LANG_TAURAHE;
            case 7 -> SPELL_LANG_GNOMISH;
            case 8 -> SPELL_LANG_TROLL;
            case 10 -> SPELL_LANG_THALASSIAN;
            case 11 -> SPELL_LANG_DRAENEI;
            default -> 0;
        };
        if (racial == 0) {
            return new int[]{faction};
        }
        return new int[]{faction, racial};
    }

    public static final int SKILL_LANG_COMMON = 98;
    public static final int SKILL_LANG_ORCISH = 109;
    public static final int SKILL_LANG_DWARVEN = 111;
    public static final int SKILL_LANG_DARNASSIAN = 113;
    public static final int SKILL_LANG_TAURAHE = 115;
    public static final int SKILL_LANG_THALASSIAN = 137;
    public static final int SKILL_LANG_GNOMISH = 313;
    public static final int SKILL_LANG_TROLL = 315;
    public static final int SKILL_LANG_GUTTERSPEAK = 673;
    public static final int SKILL_LANG_DRAENEI = 759;
    public static final int SPELL_LANG_ORCISH = 669;
    public static final int SPELL_LANG_TAURAHE = 670;
    public static final int SPELL_LANG_DARNASSIAN = 671;
    public static final int SPELL_LANG_DWARVISH = 672;
    public static final int SPELL_LANG_COMMON = 668;
    public static final int SPELL_LANG_THALASSIAN = 813;
    public static final int SPELL_LANG_GNOMISH = 7340;
    public static final int SPELL_LANG_TROLL = 7341;
    public static final int SPELL_LANG_GUTTERSPEAK = 17737;
    public static final int SPELL_LANG_DRAENEI = 29932;

    public static boolean isLanguageSkill(int skillId) {
        int s = skillId & 0xFFFF;
        return s == SKILL_LANG_COMMON || s == SKILL_LANG_ORCISH || s == SKILL_LANG_DWARVEN
                || s == SKILL_LANG_DARNASSIAN || s == SKILL_LANG_TAURAHE || s == SKILL_LANG_THALASSIAN
                || s == SKILL_LANG_GNOMISH || s == SKILL_LANG_TROLL || s == SKILL_LANG_GUTTERSPEAK
                || s == SKILL_LANG_DRAENEI;
    }

    public static boolean playable(int race, int clazz) {
        return race(race) != null && clazz >= 1 && clazz <= 11 && clazz != 6 && clazz != 10;
    }
}
