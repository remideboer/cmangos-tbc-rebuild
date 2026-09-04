package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.DbPool;
import org.tbc.world.ai.DbScriptStore;
import org.tbc.world.ai.EventAiStore;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.DbcFile;
import org.tbc.world.script.ScriptRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/** ObjectMgr: playercreateinfo, creatures, gossip, quests from tbc-db. */
public final class ObjectMgr {
    private static final Logger log = LoggerFactory.getLogger(ObjectMgr.class);

    public record CreateInfo(int race, int clazz, int map, int zone, float x, float y, float z, float o) {}
    public record CreateItem(int itemId, int amount) {}
    public record CreateSkill(int raceMask, int classMask, int skill, int step) {}

    public record CreatureTemplate(int entry, String name, int display, int faction, int hp, int level, int npcFlags,
                                   String scriptName, String gossip, int trainerType,
                                   String subName, String iconName, int display2, int display3, int display4,
                                   int typeFlags, int type, int family, int rank, int petSpellDataId,
                                   float healthMultiplier, float powerMultiplier, int racialLeader) {
        public CreatureTemplate(int entry, String name, int display, int faction, int hp, int level, int npcFlags,
                                String scriptName, String gossip, int trainerType) {
            this(entry, name, display, faction, hp, level, npcFlags, scriptName, gossip, trainerType,
                    "", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0);
        }
    }

    public record QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives) {
        public QuestTemplate(int id, String title, int minLevel, int type) {
            this(id, title, minLevel, type, 0, "", "");
        }
    }
    public record GossipOption(int menuId, int id, String text, int npcOption) {}
    public record PageText(int id, String text, int nextPage) {}

    public static final class GameObjectTemplate {
        public final int entry;
        public final int type;
        public final int displayId;
        public final String name;
        public final String iconName;
        public final String openingText;
        public final String closingText;
        public final int[] data;
        public final float size;

        public GameObjectTemplate(int entry, int type, int displayId, String name, String iconName,
                                  String openingText, String closingText, int[] data, float size) {
            this.entry = entry;
            this.type = type;
            this.displayId = displayId;
            this.name = name == null ? "" : name;
            this.iconName = iconName == null ? "" : iconName;
            this.openingText = openingText == null ? "" : openingText;
            this.closingText = closingText == null ? "" : closingText;
            this.data = data == null ? new int[24] : data;
            this.size = size;
        }
    }

    /** Subset of item_template used by SMSG_ITEM_QUERY_SINGLE_RESPONSE. */
    public static final class ItemTemplate {
        public int entry;
        public int itemClass;
        public int subClass;
        public int unk = -1;
        public String name = "";
        public int displayId;
        public int quality;
        public int flags;
        public int buyPrice;
        public int sellPrice;
        public int inventoryType;
        public int allowableClass = -1;
        public int allowableRace = -1;
        public int itemLevel;
        public int requiredLevel;
        public int requiredSkill;
        public int requiredSkillRank;
        public int requiredSpell;
        public int requiredHonorRank;
        public int requiredCityRank;
        public int requiredReputationFaction;
        public int requiredReputationRank;
        public int maxCount;
        public int stackable = 1;
        public int containerSlots;
        public final int[] statType = new int[10];
        public final int[] statValue = new int[10];
        public final float[] dmgMin = new float[5];
        public final float[] dmgMax = new float[5];
        public final int[] dmgType = new int[5];
        public int armor;
        public int holyRes;
        public int fireRes;
        public int natureRes;
        public int frostRes;
        public int shadowRes;
        public int arcaneRes;
        public int delay = 1000;
        public int ammoType;
        public float rangedModRange;
        public int bonding;
        public String description = "";
        public int pageText;
        public int languageId;
        public int pageMaterial;
        public int startQuest;
        public int lockId;
        public int material;
        public int sheath;
        public int randomProperty;
        public int randomSuffix;
        public int block;
        public int itemSet;
        public int maxDurability;
        public int area;
        public int map;
        public int bagFamily;
        public int totemCategory;
        public final int[] socketColor = new int[3];
        public final int[] socketContent = new int[3];
        public int socketBonus;
        public int gemProperties;
        public int requiredDisenchantSkill = -1;
        public float armorDamageModifier;
        public int duration;

        /** Worn Shortsword — item 25 from CMaNGOS item_template, used by handleBuy. */
        public static ItemTemplate wornShortsword() {
            ItemTemplate t = new ItemTemplate();
            t.entry = 25;
            t.itemClass = 2;
            t.subClass = 7;
            t.unk = -1;
            t.name = "Worn Shortsword";
            t.displayId = 1542;
            t.quality = 1;
            t.buyPrice = 35;
            t.sellPrice = 7;
            t.inventoryType = 21;
            t.allowableClass = 32767;
            t.allowableRace = 511;
            t.itemLevel = 2;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.dmgMin[0] = 1;
            t.dmgMax[0] = 3;
            t.delay = 1900;
            t.languageId = 1;
            t.material = 1;
            t.sheath = 3;
            t.maxDurability = 20;
            t.requiredDisenchantSkill = -1;
            return t;
        }
    }

    public final Map<Long, CreateInfo> createInfo = new HashMap<>();
    public final EventAiStore eventAiStore = new EventAiStore();
    public final DbScriptStore dbScriptStore = new DbScriptStore();
    public final Map<Integer, List<Integer>> createSpells = new HashMap<>();
    public final Map<Integer, int[]> createActions = new HashMap<>();
    public final Map<Integer, List<CreateItem>> createItems = new HashMap<>();
    public final List<CreateSkill> createSkills = new ArrayList<>();
    public final Map<Integer, List<Integer>> startOutfit = new HashMap<>();
    public final Map<Integer, CreatureTemplate> creatures = new HashMap<>();
    public final Map<Integer, QuestTemplate> quests = new HashMap<>();
    public final Map<Integer, ItemTemplate> items = new HashMap<>();
    public final Map<Integer, GameObjectTemplate> gameObjects = new HashMap<>();
    public final Map<Integer, PageText> pageTexts = new HashMap<>();
    public final List<Spawn> spawns = new ArrayList<>();
    public final Map<Integer, List<Spawn>> eventCreatures = new HashMap<>();
    public record AreaTrigger(int id, int map, float x, float y, float z, float o) {}
    public final Map<Integer, AreaTrigger> areaTriggers = new HashMap<>();
    public final Map<Integer, List<Integer>> vendorItems = new HashMap<>();
    public final Map<Integer, List<Integer>> questGivers = new HashMap<>();
    public final Map<Integer, List<Integer>> questInvolved = new HashMap<>();
    public record TrainerSpell(int spell, int cost, int reqLevel) {}
    public record TaxiHop(int from, int to, int cost, float x, float y, float z) {}
    public record ZoneWeather(int zone, int state, float grade) {}
    public record Auction(int id, int itemEntry, long owner, int startBid, int buyout, int timeLeftMs, String name) {}
    public final Map<Integer, List<TrainerSpell>> trainerSpells = new HashMap<>();
    public final Map<Integer, Integer> trainerClass = new HashMap<>();
    public final Map<Long, TaxiHop> taxiPaths = new HashMap<>();
    public final Map<Integer, ZoneWeather> weather = new HashMap<>();
    public final List<Auction> auctions = new ArrayList<>();
    public final AtomicInteger nextCreatureLow = new AtomicInteger(1_000_000);
    public final AtomicInteger nextItemLow = new AtomicInteger(1);

    public record Spawn(int guid, int entry, int map, float x, float y, float z, float o) {}

    public void load(DbPool world, ScriptRegistry scripts) {
        load(world, scripts, null);
    }

    public void load(DbPool world, ScriptRegistry scripts, Path dataDir) {
        if (world == null) {
            seedDefaults();
            seedQueryDefaults();
            loadStartOutfit(dataDir);
            return;
        }
        try (Connection c = world.get()) {
            try {
                loadCreate(c);
            } catch (Exception e) {
                log.warn("playercreateinfo load failed: {}", e.getMessage());
            }
            loadCreatures(c);
            eventAiStore.load(c);
            dbScriptStore.load(c);
            try {
                loadSpawns(c);
            } catch (Exception e) {
                log.warn("creature spawn load failed: {}", e.getMessage());
            }
            loadQuests(c);
            loadAreaTriggers(c);
            loadItems(c);
            loadGameObjects(c);
            loadPageTexts(c);
            try {
                loadWeather(c);
            } catch (Exception e) {
                log.debug("game_weather load skipped: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("ObjectMgr SQL load failed, using defaults: {}", e.getMessage());
            seedDefaults();
        }
        if (createInfo.isEmpty()) {
            seedDefaults();
        }
        seedQueryDefaults();
        loadStartOutfit(dataDir);
    }

    private void loadCreate(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT race, class, map, zone, position_x, position_y, position_z, orientation FROM playercreateinfo");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int race = rs.getInt(1);
            int clazz = rs.getInt(2);
            createInfo.put(key(race, clazz), new CreateInfo(race, clazz, rs.getInt(3), rs.getInt(4),
                    rs.getFloat(5), rs.getFloat(6), rs.getFloat(7), rs.getFloat(8)));
        }
        try {
            PreparedStatement sp = c.prepareStatement("SELECT race, class, Spell FROM playercreateinfo_spell");
            ResultSet sr = sp.executeQuery();
            while (sr.next()) {
                int k = (int) key(sr.getInt(1), sr.getInt(2));
                createSpells.computeIfAbsent(k, x -> new ArrayList<>()).add(sr.getInt(3));
            }
        } catch (Exception ignored) {
            // table name Spell vs spell
        }
        try {
            PreparedStatement ac = c.prepareStatement(
                    "SELECT race, class, button, action, type FROM playercreateinfo_action");
            ResultSet ar = ac.executeQuery();
            while (ar.next()) {
                int k = (int) key(ar.getInt(1), ar.getInt(2));
                int[] buttons = createActions.computeIfAbsent(k, x -> new int[132]);
                int button = ar.getInt(3);
                if (button >= 0 && button < 132) {
                    buttons[button] = (ar.getInt(4) & 0xFFFFFF) | ((ar.getInt(5) & 0xFF) << 24);
                }
            }
        } catch (Exception ignored) {
        }
        try {
            PreparedStatement sk = c.prepareStatement(
                    "SELECT raceMask, classMask, skill, step FROM playercreateinfo_skills");
            ResultSet kr = sk.executeQuery();
            while (kr.next()) {
                createSkills.add(new CreateSkill(kr.getInt(1), kr.getInt(2), kr.getInt(3), kr.getInt(4)));
            }
            log.info("loaded {} playercreateinfo_skills", createSkills.size());
        } catch (Exception e) {
            log.warn("playercreateinfo_skills load failed: {}", e.getMessage());
        }
        try {
            PreparedStatement it = c.prepareStatement(
                    "SELECT race, class, itemid, amount FROM playercreateinfo_item");
            ResultSet ir = it.executeQuery();
            while (ir.next()) {
                int itemId = ir.getInt(3);
                int amount = ir.getInt(4);
                if (itemId <= 0 || amount <= 0) {
                    continue;
                }
                int k = (int) key(ir.getInt(1), ir.getInt(2));
                createItems.computeIfAbsent(k, x -> new ArrayList<>()).add(new CreateItem(itemId, amount));
            }
        } catch (Exception ignored) {
        }
    }

    private void loadCreatures(Connection c) {
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, ModelId1, ModelId2, ModelId3, ModelId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template LIMIT 50000",
                true)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, DisplayId1, DisplayId2, DisplayId3, DisplayId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template LIMIT 50000",
                true)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT Entry, Name, ModelId1, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template LIMIT 50000")) {
            log.info("loaded {} creature_template rows (simple)", creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT Entry, Name, DisplayId1, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template LIMIT 50000")) {
            log.info("loaded {} creature_template rows (simple)", creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT entry, name, modelid_1, faction_A, minhealth, minlevel, npcflag, ScriptName "
                        + "FROM creature_template LIMIT 50000")) {
            log.info("loaded {} creature_template rows (trinity)", creatures.size());
            return;
        }
        log.warn("creature_template column mismatch, using seed templates");
    }

    private boolean loadCreaturesSql(Connection c, String sql, boolean full) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int entry = rs.getInt(1);
                String name = nz(rs.getString(2));
                if (full) {
                    creatures.put(entry, new CreatureTemplate(
                            entry, name, rs.getInt(5), rs.getInt(17), Math.max(1, rs.getInt(18)), rs.getInt(19),
                            rs.getInt(20), nz(rs.getString(21)), "", 0,
                            nz(rs.getString(3)), nz(rs.getString(4)), rs.getInt(6), rs.getInt(7), rs.getInt(8),
                            rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13),
                            rs.getFloat(14), rs.getFloat(15), rs.getInt(16)));
                } else {
                    creatures.put(entry, new CreatureTemplate(
                            entry, name, rs.getInt(5), rs.getInt(17), Math.max(1, rs.getInt(18)), rs.getInt(19),
                            rs.getInt(20), nz(rs.getString(21)), "", 0));
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("creature_template query failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean loadCreaturesSimple(Connection c, String sql) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int entry = rs.getInt(1);
                creatures.put(entry, new CreatureTemplate(
                        entry, nz(rs.getString(2)), rs.getInt(3), rs.getInt(4),
                        Math.max(1, rs.getInt(5)), rs.getInt(6), rs.getInt(7), nz(rs.getString(8)), "", 0));
            }
            return true;
        } catch (Exception e) {
            log.warn("creature_template simple query failed: {}", e.getMessage());
            return false;
        }
    }

    private void loadSpawns(Connection c) throws Exception {
        String[] sqls = {
                "SELECT guid, id, map, position_x, position_y, position_z, orientation FROM creature "
                        + "WHERE map IN (0, 1) LIMIT 80000",
                "SELECT guid, id, map, position_x, position_y, position_z, orientation FROM creature LIMIT 80000"
        };
        Exception last = null;
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    spawns.add(new Spawn(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getFloat(4), rs.getFloat(5), rs.getFloat(6), rs.getFloat(7)));
                }
                log.info("loaded {} creature spawns", spawns.size());
                return;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    private void loadQuests(Connection c) {
        String[] sqls = {
                "SELECT entry, Title, MinLevel, Type FROM quest_template LIMIT 20000",
                "SELECT Entry, Title, MinLevel, Type FROM quest_template LIMIT 20000"
        };
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quests.put(rs.getInt(1), new QuestTemplate(rs.getInt(1), nz(rs.getString(2)),
                            rs.getInt(3), rs.getInt(4)));
                }
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private void loadItems(Connection c) {
        String sql = "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                + "RequiredDisenchantSkill FROM item_template LIMIT 50000";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemTemplate t = new ItemTemplate();
                t.entry = rs.getInt(1);
                t.itemClass = rs.getInt(2);
                t.subClass = rs.getInt(3);
                t.name = nz(rs.getString(4));
                t.displayId = rs.getInt(5);
                t.quality = rs.getInt(6);
                t.flags = rs.getInt(7);
                t.buyPrice = rs.getInt(8);
                t.sellPrice = rs.getInt(9);
                t.inventoryType = rs.getInt(10);
                t.allowableClass = rs.getInt(11);
                t.allowableRace = rs.getInt(12);
                t.itemLevel = rs.getInt(13);
                t.requiredLevel = rs.getInt(14);
                t.maxCount = rs.getInt(15);
                t.stackable = Math.max(1, rs.getInt(16));
                t.containerSlots = rs.getInt(17);
                t.armor = rs.getInt(18);
                t.delay = rs.getInt(19);
                t.bonding = rs.getInt(20);
                t.description = nz(rs.getString(21));
                t.maxDurability = rs.getInt(22);
                t.duration = rs.getInt(23);
                t.requiredDisenchantSkill = rs.getInt(24);
                t.unk = -1;
                items.put(t.entry, t);
            }
        } catch (Exception e) {
            log.debug("item_template load skipped: {}", e.getMessage());
        }
    }

    private void loadGameObjects(Connection c) {
        String sql = "SELECT entry, type, displayId, name, IconName, OpeningText, ClosingText, size, "
                + "data0, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, "
                + "data12, data13, data14, data15, data16, data17, data18, data19, data20, data21, data22, data23 "
                + "FROM gameobject_template LIMIT 20000";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int[] data = new int[24];
                for (int i = 0; i < 24; i++) {
                    data[i] = rs.getInt(9 + i);
                }
                int entry = rs.getInt(1);
                gameObjects.put(entry, new GameObjectTemplate(entry, rs.getInt(2), rs.getInt(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), data, rs.getFloat(8)));
            }
        } catch (Exception e) {
            log.debug("gameobject_template load skipped: {}", e.getMessage());
        }
    }

    private void loadPageTexts(Connection c) {
        try (PreparedStatement ps = c.prepareStatement("SELECT entry, text, next_page FROM page_text LIMIT 20000");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pageTexts.put(rs.getInt(1), new PageText(rs.getInt(1), nz(rs.getString(2)), rs.getInt(3)));
            }
        } catch (Exception e) {
            log.debug("page_text load skipped: {}", e.getMessage());
        }
    }

    private void loadWeather(Connection c) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT zone FROM game_weather");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int zone = rs.getInt(1);
                weather.put(zone, new ZoneWeather(zone, Content.WEATHER_STATE_FINE, 0f));
            }
        }
    }

    private void seedDefaults() {
        createInfo.put(key(1, 1), new CreateInfo(1, 1, 0, 12, -8949.95f, -132.493f, 83.5312f, 0f));
        createInfo.put(key(2, 1), new CreateInfo(2, 1, 1, 14, -618.518f, -4251.67f, 38.718f, 0f));
        createSpells.put((int) key(1, 1), new ArrayList<>(List.of(6603, 78, 81, 107, 196, 203, 204, 522, 668, 2382, 2479, 3050, 3365, 6233, 6246, 6247, 6477, 6478, 7266, 7267, 7355, 8386, 9078, 9125, 20597, 20598, 20599, 20864, 21651, 21652, 22027, 22810)));
        creatures.put(6, new CreatureTemplate(6, "Kobold Vermin", 10913, 7, 42, 1, 0, "", "", 0));
        creatures.put(103, new CreatureTemplate(103, "Garrick Padfoot", 3734, 21, 80, 5, 0, "", "", 0));
        areaTriggers.put(2230, new AreaTrigger(2230, 389, 0.797643f, -8.23429f, -15.5288f, 0f));
        creatures.put(Content.NPC_CORINA_STEELE, new CreatureTemplate(Content.NPC_CORINA_STEELE, "Corina Steele", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_VENDOR, "", "", 0));
        creatures.put(Content.NPC_MARSHAL_DUGHAN, new CreatureTemplate(Content.NPC_MARSHAL_DUGHAN, "Marshal Dughan", 0, 12, 100, 10,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER, "", "", 0));
        creatures.put(Content.NPC_DEPUTY_WILLEM, new CreatureTemplate(Content.NPC_DEPUTY_WILLEM, "Deputy Willem", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER, "", "", 0));
        creatures.put(Content.NPC_MARSHAL_MCBRIDE, new CreatureTemplate(Content.NPC_MARSHAL_MCBRIDE, "Marshal McBride", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER, "", "", 0));
        creatures.put(Content.NPC_LLANE_BESHERE, new CreatureTemplate(Content.NPC_LLANE_BESHERE, "Llane Beshere", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        trainerClass.put(Content.NPC_LLANE_BESHERE, 1);
        trainerSpells.put(Content.NPC_LLANE_BESHERE, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT, Content.TRAINER_SPELL_BATTLE_SHOUT_COST, 1))));
        creatures.put(Content.NPC_DUNGAR_LONGDRINK, new CreatureTemplate(Content.NPC_DUNGAR_LONGDRINK, "Dungar Longdrink", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_FLIGHTMASTER, "", "", 0));
        creatures.put(Content.NPC_AUCTIONEER_CHILTON, new CreatureTemplate(Content.NPC_AUCTIONEER_CHILTON, "Auctioneer Chilton", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_AUCTIONEER, "", "", 0));
        creatures.put(Content.NPC_OLIVIA_BURNSIDE, new CreatureTemplate(Content.NPC_OLIVIA_BURNSIDE, "Olivia Burnside", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_BANKER, "", "", 0));
        creatures.put(Content.NPC_LUMA_SKYMOTHER, new CreatureTemplate(Content.NPC_LUMA_SKYMOTHER, "Luma Skymother", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP, "", "", 0));
        auctions.add(new Auction(1, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 43_200_000, "Worn Shortsword"));
        taxiPaths.put(taxiKey(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE),
                new TaxiHop(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE, 0, -4821.13f, -1152.4f, 502.295f));
        weather.put(Content.ZONE_ELWYNN, new ZoneWeather(Content.ZONE_ELWYNN, Content.WEATHER_STATE_FINE, 0f));
        quests.put(Content.QUEST_A_THREAT_WITHIN, new QuestTemplate(Content.QUEST_A_THREAT_WITHIN, "A Threat Within", 1, 0,
                0, "Speak with Marshal McBride.", "Speak with Marshal McBride."));
        vendorItems.put(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(Content.ITEM_WORN_SHORTSWORD)));
        questGivers.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN)));
        questInvolved.put(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN)));
        if (spawns.isEmpty()) {
            spawns.add(new Spawn(1, 6, 0, -8900f, -120f, 80f, 0f));
            spawns.add(new Spawn(2, Content.NPC_MARSHAL_DUGHAN, 0, Content.GOLDSHIRE_X, Content.GOLDSHIRE_Y, Content.GOLDSHIRE_Z, 0f));
            spawns.add(new Spawn(3, Content.NPC_CORINA_STEELE, 0, -8903f, -125f, 80f, 0f));
            spawns.add(new Spawn(4, Content.NPC_DEPUTY_WILLEM, 0, -8906f, -128f, 80f, 0f));
            spawns.add(new Spawn(5, Content.NPC_MARSHAL_MCBRIDE, 0, -8908f, -130f, 80f, 0f));
            spawns.add(new Spawn(6, 103, 0, -8910f, -125f, 80f, 0f));
            spawns.add(new Spawn(7, Content.NPC_LLANE_BESHERE, 0, -8918.36f, -208.411f, 82.309f, 0f));
            spawns.add(new Spawn(8, Content.NPC_DUNGAR_LONGDRINK, 0, -8835.76f, 490.084f, 109.699f, 0f));
            spawns.add(new Spawn(9, Content.NPC_AUCTIONEER_CHILTON, 0, -8912f, -122f, 80f, 0f));
            spawns.add(new Spawn(10, Content.NPC_OLIVIA_BURNSIDE, 0, -8914f, -124f, 80f, 0f));
        }
        if (!eventCreatures.containsKey(Content.GAME_EVENT_MIDSUMMER)) {
            eventCreatures.put(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                    new Spawn(11, Content.NPC_LUMA_SKYMOTHER, 547, -92.45719f, -110.6642f, -2.866759f, 2.408554f))));
        }
    }

    private void seedQueryDefaults() {
        creatures.putIfAbsent(6, new CreatureTemplate(6, "Kobold Vermin", 10913, 7, 42, 1, 0, "", "", 0));
        creatures.putIfAbsent(Content.NPC_LLANE_BESHERE, new CreatureTemplate(Content.NPC_LLANE_BESHERE, "Llane Beshere", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        items.putIfAbsent(25, ItemTemplate.wornShortsword());
        quests.putIfAbsent(Content.QUEST_A_THREAT_WITHIN, new QuestTemplate(Content.QUEST_A_THREAT_WITHIN, "A Threat Within", 1, 0));
        vendorItems.putIfAbsent(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(Content.ITEM_WORN_SHORTSWORD)));
        questGivers.putIfAbsent(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN)));
        questInvolved.putIfAbsent(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN)));
        trainerClass.putIfAbsent(Content.NPC_LLANE_BESHERE, 1);
        trainerSpells.putIfAbsent(Content.NPC_LLANE_BESHERE, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT, Content.TRAINER_SPELL_BATTLE_SHOUT_COST, 1))));
        creatures.putIfAbsent(Content.NPC_DUNGAR_LONGDRINK, new CreatureTemplate(Content.NPC_DUNGAR_LONGDRINK, "Dungar Longdrink", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_FLIGHTMASTER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_AUCTIONEER_CHILTON, new CreatureTemplate(Content.NPC_AUCTIONEER_CHILTON, "Auctioneer Chilton", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_AUCTIONEER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_OLIVIA_BURNSIDE, new CreatureTemplate(Content.NPC_OLIVIA_BURNSIDE, "Olivia Burnside", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_BANKER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_LUMA_SKYMOTHER, new CreatureTemplate(Content.NPC_LUMA_SKYMOTHER, "Luma Skymother", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP, "", "", 0));
        if (auctions.isEmpty()) {
            auctions.add(new Auction(1, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 43_200_000, "Worn Shortsword"));
        }
        taxiPaths.putIfAbsent(taxiKey(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE),
                new TaxiHop(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE, 0, -4821.13f, -1152.4f, 502.295f));
        weather.putIfAbsent(Content.ZONE_ELWYNN, new ZoneWeather(Content.ZONE_ELWYNN, Content.WEATHER_STATE_FINE, 0f));
        eventCreatures.putIfAbsent(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                new Spawn(11, Content.NPC_LUMA_SKYMOTHER, 547, -92.45719f, -110.6642f, -2.866759f, 2.408554f))));
    }

    public static long taxiKey(int from, int to) {
        return ((long) from << 32) | (to & 0xFFFFFFFFL);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    public static long key(int race, int clazz) {
        return ((long) race << 8) | clazz;
    }

    private void loadAreaTriggers(Connection c) {
        try {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT id, target_map, target_position_x, target_position_y, target_position_z, target_orientation FROM areatrigger_teleport");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                areaTriggers.put(rs.getInt(1), new AreaTrigger(rs.getInt(1), rs.getInt(2),
                        rs.getFloat(3), rs.getFloat(4), rs.getFloat(5), rs.getFloat(6)));
            }
        } catch (Exception ignored) {
        }
    }

    void loadStartOutfit(Path dataDir) {
        if (dataDir == null) {
            return;
        }
        Path file = dataDir.resolve("dbc").resolve("CharStartOutfit.dbc");
        if (!Files.isRegularFile(file)) {
            log.debug("CharStartOutfit.dbc not at {}", file);
            return;
        }
        try {
            DbcFile dbc = DbcFile.load(file);
            int n = 0;
            for (int[] row : dbc.records) {
                if (row.length < 14) {
                    continue;
                }
                int rcg = row[1] & 0x00FFFFFF;
                List<Integer> ids = new ArrayList<>();
                for (int i = 2; i <= 13; i++) {
                    if (row[i] > 0) {
                        ids.add(row[i]);
                    }
                }
                if (!ids.isEmpty()) {
                    startOutfit.put(rcg, ids);
                    n++;
                }
            }
            log.info("CharStartOutfit {} race-class-gender rows from {}", n, file);
        } catch (Exception e) {
            log.warn("CharStartOutfit load failed: {}", e.getMessage());
        }
    }

    public void fillItemVisuals(Player p) {
        if (p == null) {
            return;
        }
        for (Item it : p.items.values()) {
            ItemTemplate t = items.get(it.entry);
            if (t == null) {
                continue;
            }
            it.displayId = t.displayId;
            it.inventoryType = t.inventoryType;
            it.quality = t.quality;
            if (it.durability <= 0) {
                it.durability = t.maxDurability;
            }
        }
    }

    /** CMaNGOS LearnDefaultSkills from playercreateinfo_skills. Languages are 300/300. */
    public void applyCreateSkills(Player p) {
        if (p == null || createSkills.isEmpty()) {
            return;
        }
        int raceBit = p.race <= 0 ? 0 : 1 << (p.race - 1);
        int classBit = p.clazz <= 0 ? 0 : 1 << (p.clazz - 1);
        for (CreateSkill cs : createSkills) {
            if (cs.skill() == 0) {
                continue;
            }
            if (cs.raceMask() != 0 && (cs.raceMask() & raceBit) == 0) {
                continue;
            }
            if (cs.classMask() != 0 && (cs.classMask() & classBit) == 0) {
                continue;
            }
            if (ChrStatic.isLanguageSkill(cs.skill())) {
                p.learnSkill(cs.skill(), 300, 300, cs.step());
            } else {
                p.learnSkill(cs.skill(), 1, Math.max(1, p.level * 5), cs.step());
            }
        }
    }

    public void giveStartItems(Player p, LongSupplier nextGuid) {
        if (p == null || nextGuid == null) {
            return;
        }
        int rcg = (p.race & 0xFF) | ((p.clazz & 0xFF) << 8) | ((p.gender & 0xFF) << 16);
        List<Integer> outfit = startOutfit.get(rcg);
        if (outfit != null) {
            for (int itemId : outfit) {
                storeCreateItem(p, itemId, 1, nextGuid);
            }
        }
        List<CreateItem> extra = createItems.get((int) key(p.race, p.clazz));
        if (extra != null) {
            for (CreateItem ci : extra) {
                storeCreateItem(p, ci.itemId(), ci.amount(), nextGuid);
            }
        }
    }

    private void storeCreateItem(Player p, int itemId, int amount, LongSupplier nextGuid) {
        ItemTemplate t = items.get(itemId);
        if (t == null || amount <= 0) {
            return;
        }
        int left = amount;
        while (left > 0) {
            int slot = firstFreeEquipSlot(p, t.inventoryType);
            if (slot < 0) {
                break;
            }
            addCreateItem(p, t, 1, slot, nextGuid);
            left--;
        }
        if (left <= 0) {
            return;
        }
        int bagSlot = firstFreeBackpack(p);
        if (bagSlot < 0) {
            return;
        }
        addCreateItem(p, t, left, bagSlot, nextGuid);
    }

    private static void addCreateItem(Player p, ItemTemplate t, int count, int slot, LongSupplier nextGuid) {
        Item it = new Item(nextGuid.getAsLong(), t.entry);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = slot;
        it.count = Math.max(1, count);
        it.displayId = t.displayId;
        it.inventoryType = t.inventoryType;
        it.quality = t.quality;
        it.durability = t.maxDurability;
        p.items.put(Guid.low(it.guid), it);
    }

    private static int firstFreeEquipSlot(Player p, int inventoryType) {
        for (int slot : equipSlots(inventoryType)) {
            if (p.itemAt(0, slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    private static int firstFreeBackpack(Player p) {
        for (int slot = Player.INVENTORY_SLOT_ITEM_START; slot < Player.INVENTORY_SLOT_ITEM_END; slot++) {
            if (p.itemAt(0, slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    /** InventoryType → equipment/bag slots. Player.cpp ViableEquipSlots; no dual-wield at create. */
    private static int[] equipSlots(int inventoryType) {
        return switch (inventoryType) {
            case 1 -> new int[]{0};
            case 2 -> new int[]{1};
            case 3 -> new int[]{2};
            case 4 -> new int[]{3};
            case 5, 20 -> new int[]{4};
            case 6 -> new int[]{5};
            case 7 -> new int[]{6};
            case 8 -> new int[]{7};
            case 9 -> new int[]{8};
            case 10 -> new int[]{9};
            case 11 -> new int[]{10, 11};
            case 12 -> new int[]{12, 13};
            case 13, 17, 21 -> new int[]{15};
            case 14, 22, 23 -> new int[]{16};
            case 15, 25, 26, 28 -> new int[]{17};
            case 16 -> new int[]{14};
            case 18 -> new int[]{19, 20, 21, 22};
            case 19 -> new int[]{18};
            default -> new int[0];
        };
    }

    public AreaTrigger areaTrigger(int id) {
        return areaTriggers.get(id);
    }

    public CreateInfo create(int race, int clazz) {
        CreateInfo i = createInfo.get(key(race, clazz));
        if (i == null) {
            i = createInfo.get(key(1, 1));
        }
        return i;
    }

    public Creature spawnCreature(int entry, int map, float x, float y, float z, float o, ScriptRegistry scripts) {
        return spawnCreature(entry, 0, map, x, y, z, o, scripts);
    }

    public Creature spawnCreature(Spawn s, ScriptRegistry scripts) {
        Creature c = spawnCreature(s.entry(), s.guid(), s.map(), s.x(), s.y(), s.z(), s.o(), scripts);
        if (s.guid() > 0) {
            c.guid = Guid.HIGH_CREATURE | (s.guid() & 0xFFFFFFFFL);
            c.setGuid(org.tbc.world.net.wow8606.UpdateFields.OBJECT_FIELD_GUID, c.guid);
        }
        return c;
    }

    private Creature spawnCreature(int entry, int spawnId, int map, float x, float y, float z, float o,
            ScriptRegistry scripts) {
        CreatureTemplate t = creatures.get(entry);
        if (t == null) {
            t = new CreatureTemplate(entry, "Creature", 10045, 7, 100, 1, 0, "", "", 0);
        }
        Creature c = new Creature();
        c.guid = Guid.HIGH_CREATURE | (nextCreatureLow.getAndIncrement() & 0xFFFFFFL);
        c.mapId = map;
        c.spawnId = Math.max(0, spawnId);
        c.relocate(x, y, z, o);
        c.spawnX = x;
        c.spawnY = y;
        c.spawnZ = z;
        c.spawnO = o;
        c.scriptName = t.scriptName();
        c.applyTemplate(entry, t.name(), t.display(), t.faction(), t.hp(), t.level());
        c.npcFlags = t.npcFlags();
        c.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_NPC_FLAGS, t.npcFlags());
        org.tbc.world.ai.FactorySelector.selectAI(c, scripts);
        java.util.List<org.tbc.world.ai.EventAi.Script> rows = eventAiStore.scriptsFor(entry, c.spawnId);
        if (!rows.isEmpty()) {
            if (c.eventAi == null) {
                c.eventAi = new org.tbc.world.ai.EventAi();
            }
            c.eventAi.load(rows);
        } else if (entry == 103) {
            if (c.eventAi == null) {
                c.eventAi = new org.tbc.world.ai.EventAi();
            }
            c.eventAi.load(java.util.List.of(org.tbc.world.ai.EventAi.Script.aggroCast(7164)));
        }
        return c;
    }
}
