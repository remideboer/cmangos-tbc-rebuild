package org.tbc.world.classless;

import org.tbc.world.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Hero unlock quest givers: native starter-hub class trainers when present, otherwise one
 * custom NPC (hub race, or a faction race that can play the class). Warrior may use the hub
 * race even when that race cannot create a warrior (Blood Elf).
 */
public final class HeroStarterTrainers {
    public static final int FACTION_STORMWIND = 12;
    public static final int FACTION_IRONFORGE = 55;
    public static final int FACTION_DARNASSUS = 80;
    public static final int FACTION_ORGRIMMAR = 29;
    public static final int FACTION_THUNDER_BLUFF = 104;
    public static final int FACTION_UNDERCITY = 68;
    public static final int FACTION_SILVERMOON = HeroClassUnlock.FACTION_SILVERMOON;
    public static final int FACTION_EXODAR = 1638;
    /** Gnomeregan Exiles — Coldridge gnome trainers. */
    public static final int FACTION_GNOME_EXILE = 64;

    public static final int DISPLAY_LLANE = 3343;
    public static final int DISPLAY_SAMMUEL = 3346;
    public static final int DISPLAY_THORGAS = 3395;
    public static final int DISPLAY_KHELDEN = 5001;
    public static final int DISPLAY_DRUSILLA = 3345;
    public static final int DISPLAY_JORIK = 3351;
    public static final int DISPLAY_FIRMANVAAR = 17598;
    public static final int DISPLAY_MARDANT = 1732;
    public static final int DISPLAY_MEELA = 10180;
    public static final int DISPLAY_GENNIA = 3820;
    public static final int DISPLAY_JESTHENIS = HeroClassUnlock.DISPLAY_JESTHENIS;
    public static final int DISPLAY_TAIJIN = 1897;
    public static final int DISPLAY_UNTHUWA = 10171;
    public static final int DISPLAY_RWAG = 1886;
    public static final int DISPLAY_NARTOK = 1884;
    public static final int DISPLAY_JENSHAN = 1882;
    public static final int DISPLAY_SHIKRIK = 1878;

    public record Placement(
            int trainerClass,
            int entry,
            boolean custom,
            String name,
            int display,
            int faction,
            int map,
            float x,
            float y,
            float z,
            float o,
            int spawnGuid,
            float map0X,
            float map0Y
    ) {
        public boolean hasMap0Twin() {
            return map0X != 0f || map0Y != 0f;
        }
    }

    private static final List<Placement> ALL = List.of(
            // Sunstrider Isle
            custom(Player.CLASS_WARRIOR, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER,
                    HeroClassUnlock.NAME_LORVAEN_BLOODFEATHER, DISPLAY_JESTHENIS, FACTION_SILVERMOON,
                    530, HeroClassUnlock.SUNSTRIDER_SPAWN_X, HeroClassUnlock.SUNSTRIDER_SPAWN_Y,
                    HeroClassUnlock.SUNSTRIDER_SPAWN_Z, HeroClassUnlock.SUNSTRIDER_SPAWN_O, -8400f, -400f),
            nativeP(Player.CLASS_PALADIN, 15280, "Jesthenis Sunstriker", DISPLAY_JESTHENIS, FACTION_SILVERMOON,
                    530, 54986, 10366.7002f, -6431.5298f, 38.6157f, 0.73304f, -8402f, -402f),
            nativeP(Player.CLASS_HUNTER, 15513, "Ranger Sallina", 15520, FACTION_SILVERMOON,
                    530, 55431, 10384.7002f, -6410.6699f, 38.6156f, 3.19395f, -8404f, -404f),
            nativeP(Player.CLASS_ROGUE, 15285, "Pathstalker Kariel", 15519, FACTION_SILVERMOON,
                    530, 54991, 10384.5000f, -6404.9902f, 38.6156f, 3.42085f, -8406f, -406f),
            nativeP(Player.CLASS_PRIEST, 15284, "Matron Arena", 15518, FACTION_SILVERMOON,
                    530, 54990, 10372.4004f, -6428.8398f, 38.6155f, 3.33358f, -8408f, -408f),
            nativeP(Player.CLASS_MAGE, 15279, "Julia Sunstriker", 15522, FACTION_SILVERMOON,
                    530, 54985, 10337.2002f, -6419.4502f, 38.6156f, 6.10865f, -8410f, -410f),
            nativeP(Player.CLASS_WARLOCK, 15283, "Summoner Teli'Larien", 15524, FACTION_SILVERMOON,
                    530, 54989, 10337.5996f, -6405.0298f, 38.6156f, 5.98648f, -8412f, -412f),
            custom(Player.CLASS_SHAMAN, HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER,
                    HeroClassUnlock.NAME_HUURUN_STONESONG, DISPLAY_MEELA, FACTION_SILVERMOON,
                    530, 10348.5f, -6408.0f, 38.62f, 3.14f, -8414f, -414f),
            custom(Player.CLASS_DRUID, HeroClassUnlock.NPC_HERO_DRUID_TRAINER,
                    HeroClassUnlock.NAME_MESA_WILDHOOF, DISPLAY_GENNIA, FACTION_SILVERMOON,
                    530, 10351.0f, -6405.0f, 38.62f, 3.14f, -8416f, -416f),

            // Northshire
            nativeP(Player.CLASS_WARRIOR, 911, "Llane Beshere", DISPLAY_LLANE, FACTION_STORMWIND,
                    0, 79964, -8918.3604f, -208.4110f, 82.3088f, 6.05629f, 0f, 0f),
            nativeP(Player.CLASS_PALADIN, 925, "Brother Sammuel", DISPLAY_SAMMUEL, FACTION_STORMWIND,
                    0, 79967, -8914.5703f, -215.0160f, 82.2996f, 1.20428f, 0f, 0f),
            custom(Player.CLASS_HUNTER, 91010, "Bromlin Flintshot", DISPLAY_THORGAS, FACTION_IRONFORGE,
                    0, -8905.0f, -200.0f, 82.30f, 6.06f, 0f, 0f),
            nativeP(Player.CLASS_ROGUE, 915, "Jorik Kerridan", DISPLAY_JORIK, FACTION_STORMWIND,
                    0, 79986, -8863.4697f, -210.9050f, 80.7550f, 4.41568f, 0f, 0f),
            nativeP(Player.CLASS_PRIEST, 375, "Priestess Anetta", 3344, FACTION_STORMWIND,
                    0, 79963, -8853.5898f, -193.3360f, 82.1157f, 2.54818f, 0f, 0f),
            nativeP(Player.CLASS_MAGE, 198, "Khelden Bremen", DISPLAY_KHELDEN, FACTION_STORMWIND,
                    0, 79962, -8851.5703f, -188.2340f, 89.4958f, 2.91470f, 0f, 0f),
            nativeP(Player.CLASS_WARLOCK, 459, "Drusilla La Salle", DISPLAY_DRUSILLA, FACTION_STORMWIND,
                    0, 79966, -8926.7402f, -195.5890f, 80.7714f, 2.79253f, 0f, 0f),
            custom(Player.CLASS_SHAMAN, 91011, "Kureed", DISPLAY_FIRMANVAAR, FACTION_EXODAR,
                    0, -8903.0f, -198.0f, 82.30f, 6.06f, 0f, 0f),
            custom(Player.CLASS_DRUID, 91012, "Thaelon Leafwhisper", DISPLAY_MARDANT, FACTION_DARNASSUS,
                    0, -8901.0f, -196.0f, 82.30f, 6.06f, 0f, 0f),

            // Coldridge / Anvilmar
            nativeP(Player.CLASS_WARRIOR, 912, "Thran Khorman", 3399, FACTION_IRONFORGE,
                    0, 406, -6084.7700f, 382.1410f, 395.6260f, 3.45575f, 0f, 0f),
            nativeP(Player.CLASS_PALADIN, 926, "Bromos Grummner", 3393, FACTION_IRONFORGE,
                    0, 403, -6120.6802f, 382.0890f, 395.6260f, 6.16101f, 0f, 0f),
            nativeP(Player.CLASS_HUNTER, 895, "Thorgas Grimson", DISPLAY_THORGAS, FACTION_IRONFORGE,
                    0, 407, -6091.7900f, 365.1410f, 395.6230f, 2.33874f, 0f, 0f),
            nativeP(Player.CLASS_ROGUE, 916, "Solm Hargrin", 3407, FACTION_IRONFORGE,
                    0, 421, -6093.7500f, 404.9180f, 395.6200f, 4.52040f, 0f, 0f),
            nativeP(Player.CLASS_PRIEST, 837, "Branstock Khalder", 3401, FACTION_IRONFORGE,
                    0, 1023, -6056.7402f, 393.5480f, 392.8430f, 3.68265f, 0f, 0f),
            nativeP(Player.CLASS_MAGE, 944, "Marryk Nurribit", 10216, FACTION_GNOME_EXILE,
                    0, 1025, -6056.0898f, 388.1750f, 392.9440f, 3.33358f, 0f, 0f),
            nativeP(Player.CLASS_WARLOCK, 460, "Alamar Grimm", 1930, FACTION_GNOME_EXILE,
                    0, 1024, -6048.7900f, 391.0780f, 398.9580f, 3.63029f, 0f, 0f),
            custom(Player.CLASS_SHAMAN, 91013, "Naalor", DISPLAY_FIRMANVAAR, FACTION_EXODAR,
                    0, -6080.0f, 390.0f, 395.63f, 3.46f, 0f, 0f),
            custom(Player.CLASS_DRUID, 91014, "Saelara Shadeleaf", DISPLAY_MARDANT, FACTION_DARNASSUS,
                    0, -6078.0f, 388.0f, 395.63f, 3.46f, 0f, 0f),

            // Shadowglen
            nativeP(Player.CLASS_WARRIOR, 3593, "Alyissia", 1721, FACTION_DARNASSUS,
                    1, 46178, 10526.5996f, 778.0860f, 1329.6801f, 2.47837f, 0f, 0f),
            custom(Player.CLASS_PALADIN, 91015, "Aldric Brightmace", DISPLAY_SAMMUEL, FACTION_STORMWIND,
                    1, 10530.0f, 782.0f, 1329.68f, 2.48f, 0f, 0f),
            nativeP(Player.CLASS_HUNTER, 3596, "Ayanna Everstride", 1723, FACTION_DARNASSUS,
                    1, 46182, 10458.5000f, 827.8680f, 1381.0200f, 2.11185f, 0f, 0f),
            nativeP(Player.CLASS_ROGUE, 3594, "Frahun Shadewhisper", 1725, FACTION_DARNASSUS,
                    1, 46179, 10519.0996f, 778.0140f, 1329.6801f, 1.53589f, 0f, 0f),
            nativeP(Player.CLASS_PRIEST, 3595, "Shanda", 1733, FACTION_DARNASSUS,
                    1, 46181, 10458.7998f, 801.6230f, 1346.8400f, 3.75246f, 0f, 0f),
            custom(Player.CLASS_MAGE, 91016, "Corin Spellbrook", DISPLAY_KHELDEN, FACTION_STORMWIND,
                    1, 10532.0f, 784.0f, 1329.68f, 2.48f, 0f, 0f),
            custom(Player.CLASS_WARLOCK, 91017, "Maedra Voidwell", DISPLAY_DRUSILLA, FACTION_STORMWIND,
                    1, 10534.0f, 786.0f, 1329.68f, 2.48f, 0f, 0f),
            custom(Player.CLASS_SHAMAN, 91018, "Juraan", DISPLAY_FIRMANVAAR, FACTION_EXODAR,
                    1, 10536.0f, 788.0f, 1329.68f, 2.48f, 0f, 0f),
            nativeP(Player.CLASS_DRUID, 3597, "Mardant Strongoak", DISPLAY_MARDANT, FACTION_DARNASSUS,
                    1, 46183, 10464.0000f, 829.5380f, 1381.0200f, 2.89725f, 0f, 0f),

            // Ammen Vale
            nativeP(Player.CLASS_WARRIOR, 16503, "Kore", 16226, FACTION_EXODAR,
                    530, 84574, -4136.0400f, -13739.7998f, 74.6439f, 5.60251f, 0f, 0f),
            nativeP(Player.CLASS_PALADIN, 16501, "Aurelon", 16224, FACTION_EXODAR,
                    530, 57212, -4103.1899f, -13744.2002f, 74.6210f, 3.75246f, 0f, 0f),
            nativeP(Player.CLASS_HUNTER, 16499, "Keilnei", 16222, FACTION_EXODAR,
                    530, 84585, -4143.1899f, -13752.0000f, 74.6320f, 6.17846f, 0f, 0f),
            custom(Player.CLASS_ROGUE, 91019, "Tomas Quickfingers", DISPLAY_JORIK, FACTION_STORMWIND,
                    530, -4132.0f, -13736.0f, 74.64f, 5.60f, 0f, 0f),
            nativeP(Player.CLASS_PRIEST, 16502, "Zalduun", 16225, FACTION_EXODAR,
                    530, 57213, -4120.9902f, -13762.0996f, 73.5880f, 0.51177f, 0f, 0f),
            nativeP(Player.CLASS_MAGE, 16500, "Valaatu", 16223, FACTION_EXODAR,
                    530, 84581, -4117.4702f, -13739.5996f, 74.7147f, 4.92183f, 0f, 0f),
            custom(Player.CLASS_WARLOCK, 91020, "Edda Blackhearth", DISPLAY_DRUSILLA, FACTION_STORMWIND,
                    530, -4130.0f, -13734.0f, 74.64f, 5.60f, 0f, 0f),
            nativeP(Player.CLASS_SHAMAN, 17089, "Firmanvaar", DISPLAY_FIRMANVAAR, FACTION_EXODAR,
                    530, 59513, -4127.2798f, -13727.2002f, 74.7580f, 4.72984f, 0f, 0f),
            custom(Player.CLASS_DRUID, 91021, "Lethras Moonbough", DISPLAY_MARDANT, FACTION_DARNASSUS,
                    530, -4128.0f, -13732.0f, 74.64f, 5.60f, 0f, 0f),

            // Valley of Trials
            nativeP(Player.CLASS_WARRIOR, 3153, "Frang", 1880, FACTION_ORGRIMMAR,
                    1, 7651, -639.3440f, -4230.1899f, 38.5605f, 5.72468f, 0f, 0f),
            custom(Player.CLASS_PALADIN, 91022, "Nerine Sunblade", DISPLAY_JESTHENIS, FACTION_SILVERMOON,
                    1, -635.0f, -4225.0f, 38.56f, 5.72f, 0f, 0f),
            nativeP(Player.CLASS_HUNTER, 3154, "Jen'shan", DISPLAY_JENSHAN, FACTION_ORGRIMMAR,
                    1, 7649, -635.4620f, -4227.5200f, 38.3710f, 5.09636f, 0f, 0f),
            nativeP(Player.CLASS_ROGUE, 3155, "Rwag", DISPLAY_RWAG, FACTION_ORGRIMMAR,
                    1, 7284, -588.7030f, -4144.9399f, 41.1033f, 3.63029f, 0f, 0f),
            custom(Player.CLASS_PRIEST, 91023, "Vol'taka", DISPLAY_TAIJIN, FACTION_ORGRIMMAR,
                    1, -633.0f, -4223.0f, 38.56f, 5.72f, 0f, 0f),
            custom(Player.CLASS_MAGE, 91024, "Jin'thaka", DISPLAY_UNTHUWA, FACTION_ORGRIMMAR,
                    1, -631.0f, -4221.0f, 38.56f, 5.72f, 0f, 0f),
            nativeP(Player.CLASS_WARLOCK, 3156, "Nartok", DISPLAY_NARTOK, FACTION_ORGRIMMAR,
                    1, 4800, -606.8740f, -4111.8701f, 43.0280f, 0.24435f, 0f, 0f),
            nativeP(Player.CLASS_SHAMAN, 3157, "Shikrik", DISPLAY_SHIKRIK, FACTION_ORGRIMMAR,
                    1, 7281, -623.9390f, -4203.8799f, 38.4285f, 5.48033f, 0f, 0f),
            custom(Player.CLASS_DRUID, 91025, "Saern Stormhoof", DISPLAY_GENNIA, FACTION_THUNDER_BLUFF,
                    1, -629.0f, -4219.0f, 38.56f, 5.72f, 0f, 0f),

            // Camp Narache
            nativeP(Player.CLASS_WARRIOR, 3059, "Harutt Thunderhorn", 3793, FACTION_THUNDER_BLUFF,
                    1, 26897, -2880.4299f, -213.0200f, 54.9039f, 4.64258f, 0f, 0f),
            custom(Player.CLASS_PALADIN, 91026, "Velith Dawnblade", DISPLAY_JESTHENIS, FACTION_SILVERMOON,
                    1, -2876.0f, -210.0f, 54.90f, 4.64f, 0f, 0f),
            nativeP(Player.CLASS_HUNTER, 3061, "Lanka Farshot", 3810, FACTION_THUNDER_BLUFF,
                    1, 26899, -2865.3999f, -225.7310f, 54.9617f, 3.10669f, 0f, 0f),
            custom(Player.CLASS_ROGUE, 91027, "Gargok", DISPLAY_RWAG, FACTION_ORGRIMMAR,
                    1, -2874.0f, -208.0f, 54.90f, 4.64f, 0f, 0f),
            custom(Player.CLASS_PRIEST, 91028, "Shi'kala", DISPLAY_TAIJIN, FACTION_ORGRIMMAR,
                    1, -2872.0f, -206.0f, 54.90f, 4.64f, 0f, 0f),
            custom(Player.CLASS_MAGE, 91029, "Mai'zuni", DISPLAY_UNTHUWA, FACTION_ORGRIMMAR,
                    1, -2870.0f, -204.0f, 54.90f, 4.64f, 0f, 0f),
            custom(Player.CLASS_WARLOCK, 91030, "Groshnak", DISPLAY_NARTOK, FACTION_ORGRIMMAR,
                    1, -2868.0f, -202.0f, 54.90f, 4.64f, 0f, 0f),
            nativeP(Player.CLASS_SHAMAN, 3062, "Meela Dawnstrider", DISPLAY_MEELA, FACTION_THUNDER_BLUFF,
                    1, 26900, -2873.8799f, -264.7090f, 54.0072f, 3.70010f, 0f, 0f),
            nativeP(Player.CLASS_DRUID, 3064, "Gennia Runetotem", DISPLAY_GENNIA, FACTION_THUNDER_BLUFF,
                    1, 26903, -2315.7500f, -442.6340f, -5.3551f, 6.07375f, 0f, 0f),

            // Deathknell
            nativeP(Player.CLASS_WARRIOR, 2119, "Dannal Stern", 1578, FACTION_UNDERCITY,
                    0, 28464, 1862.4600f, 1556.3300f, 94.8790f, 2.49582f, 0f, 0f),
            custom(Player.CLASS_PALADIN, 91031, "Syrael Brightward", DISPLAY_JESTHENIS, FACTION_SILVERMOON,
                    0, 1866.0f, 1560.0f, 94.88f, 2.50f, 0f, 0f),
            custom(Player.CLASS_HUNTER, 91032, "Kargosh", DISPLAY_JENSHAN, FACTION_ORGRIMMAR,
                    0, 1868.0f, 1562.0f, 94.88f, 2.50f, 0f, 0f),
            nativeP(Player.CLASS_ROGUE, 2122, "David Trias", 1580, FACTION_UNDERCITY,
                    0, 28466, 1859.6500f, 1563.3000f, 94.3900f, 5.81195f, 0f, 0f),
            nativeP(Player.CLASS_PRIEST, 2123, "Dark Cleric Duesten", 1579, FACTION_UNDERCITY,
                    0, 28469, 1848.3199f, 1627.6300f, 97.0169f, 3.33358f, 0f, 0f),
            nativeP(Player.CLASS_MAGE, 2124, "Isabella", 1592, FACTION_UNDERCITY,
                    0, 28463, 1847.3900f, 1635.5200f, 97.0169f, 3.87463f, 0f, 0f),
            nativeP(Player.CLASS_WARLOCK, 2126, "Maximillion", 1581, FACTION_UNDERCITY,
                    0, 28467, 1839.0300f, 1636.5400f, 97.0169f, 5.20108f, 0f, 0f),
            custom(Player.CLASS_SHAMAN, 91033, "Rukka", DISPLAY_SHIKRIK, FACTION_ORGRIMMAR,
                    0, 1870.0f, 1564.0f, 94.88f, 2.50f, 0f, 0f),
            custom(Player.CLASS_DRUID, 91034, "Tawna Grassmane", DISPLAY_GENNIA, FACTION_THUNDER_BLUFF,
                    0, 1872.0f, 1566.0f, 94.88f, 2.50f, 0f, 0f)
    );

    private HeroStarterTrainers() {
    }

    public static List<Placement> all() {
        return ALL;
    }

    public static List<Placement> forClass(int trainerClass) {
        List<Placement> out = new ArrayList<>();
        for (Placement p : ALL) {
            if (p.trainerClass() == trainerClass) {
                out.add(p);
            }
        }
        return out;
    }

    public static int[] warriorEntries() {
        return entriesForClass(Player.CLASS_WARRIOR);
    }

    public static int[] entriesForClass(int trainerClass) {
        List<Placement> list = forClass(trainerClass);
        int[] ids = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ids[i] = list.get(i).entry();
        }
        return ids;
    }

    public static int canonical(int trainerClass) {
        HeroClassUnlock unlock = HeroClassUnlock.forClass(trainerClass);
        return unlock == null ? 0 : unlock.trainerEntry();
    }

    private static Placement custom(int clazz, int entry, String name, int display, int faction,
                                    int map, float x, float y, float z, float o,
                                    float map0X, float map0Y) {
        return new Placement(clazz, entry, true, name, display, faction, map, x, y, z, o,
                HeroClassUnlock.SPAWN_GUID_BASE_SUNSTRIDER + entry, map0X, map0Y);
    }

    private static Placement nativeP(int clazz, int entry, String name, int display, int faction,
                                     int map, int spawnGuid, float x, float y, float z, float o,
                                     float map0X, float map0Y) {
        return new Placement(clazz, entry, false, name, display, faction, map, x, y, z, o,
                spawnGuid, map0X, map0Y);
    }
}
