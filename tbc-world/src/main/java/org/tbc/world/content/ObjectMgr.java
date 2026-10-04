package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.DbPool;
import org.tbc.world.ai.DbScriptStore;
import org.tbc.world.ai.EventAiStore;
import org.tbc.world.combat.Factions;
import org.tbc.world.combat.MainhandWeaponStats;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Guild;
import org.tbc.world.entity.ArenaTeam;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.DbcFile;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.spell.SpellEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/** ObjectMgr: playercreateinfo, creatures, gossip, quests from tbc-db. */
public final class ObjectMgr {
    private static final Logger log = LoggerFactory.getLogger(ObjectMgr.class);
    private static final int GOSSIP_MAX_MENU_ITEMS = 32;
    /** GridDefines.h MAP_HALFSIZE = SIZE_OF_GRIDS * MAX_NUMBER_OF_GRIDS / 2. */
    private static final float MAP_HALFSIZE = 533.33333f * 64 / 2;
    private static final int GOSSIP_OPTION_GOSSIP = 1;
    private static final int GOSSIP_OPTION_QUESTGIVER = 2;
    private static final int GOSSIP_OPTION_VENDOR = 3;
    private static final int GOSSIP_OPTION_TAXIVENDOR = 4;
    private static final int GOSSIP_OPTION_TRAINER = 5;
    private static final int GOSSIP_OPTION_SPIRITHEALER = 6;
    private static final int GOSSIP_OPTION_SPIRITGUIDE = 7;
    private static final int GOSSIP_OPTION_INNKEEPER = 8;
    private static final int GOSSIP_OPTION_BANKER = 9;
    private static final int GOSSIP_OPTION_PETITIONER = 10;
    private static final int GOSSIP_OPTION_TABARDDESIGNER = 11;
    private static final int GOSSIP_OPTION_BATTLEFIELD = 12;
    private static final int GOSSIP_OPTION_AUCTIONEER = 13;
    private static final int GOSSIP_OPTION_STABLEPET = 14;
    private static final int GOSSIP_OPTION_ARMORER = 15;
    private static final int GOSSIP_OPTION_UNLEARNTALENTS = 16;
    private static final int GOSSIP_OPTION_UNLEARNPETSKILLS = 17;
    private static final int GOSSIP_OPTION_BOT = 99;
    private static final int CLASS_HUNTER = 3;

    public record CreateInfo(int race, int clazz, int map, int zone, float x, float y, float z, float o) {}
    public record CreateItem(int itemId, int amount) {}
    public record CreateSkill(int raceMask, int classMask, int skill, int step) {}

    public record CreatureTemplate(int entry, String name, int display, int faction, int hp, int level, int npcFlags,
                                   String scriptName, String gossip, int trainerType,
                                   String subName, String iconName, int display2, int display3, int display4,
                                   int typeFlags, int type, int family, int rank, int petSpellDataId,
                                   float healthMultiplier, float powerMultiplier, int racialLeader,
                                   String aiName, int extraFlags, float minMeleeDmg, float maxMeleeDmg,
                                   int meleeAttackTime, float combatReach, int lootId, int minLootGold, int maxLootGold,
                                   int inhabitType) {
        public CreatureTemplate(int entry, String name, int display, int faction, int hp, int level, int npcFlags,
                                String scriptName, String gossip, int trainerType) {
            this(entry, name, display, faction, hp, level, npcFlags, scriptName, gossip, trainerType,
                    "", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0);
        }

        public CreatureTemplate(int entry, String name, int display, int faction, int hp, int level, int npcFlags,
                                String scriptName, String gossip, int trainerType,
                                String subName, String iconName, int display2, int display3, int display4,
                                int typeFlags, int type, int family, int rank, int petSpellDataId,
                                float healthMultiplier, float powerMultiplier, int racialLeader) {
            this(entry, name, display, faction, hp, level, npcFlags, scriptName, gossip, trainerType,
                    subName, iconName, display2, display3, display4, typeFlags, type, family, rank, petSpellDataId,
                    healthMultiplier, powerMultiplier, racialLeader,
                    "", 0, 1f, 3f, 2000, 1.5f, 0, 0, 0,
                    org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT);
        }
    }

    public record QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives,
                               int reqCreatureOrGOId1, int reqCreatureOrGOCount1, int reqItemId1, int reqItemCount1,
                               int questLevel, int rewMoneyMaxLevel, int rewItemId1, int rewItemCount1,
                               int rewChoiceItemId1, int rewChoiceItemCount1, int rewChoiceItemId2, int rewChoiceItemCount2,
                               int reqCreatureOrGOId2, int reqCreatureOrGOCount2,
                               int reqCreatureOrGOId3, int reqCreatureOrGOCount3,
                               int reqCreatureOrGOId4, int reqCreatureOrGOCount4,
                               int reqItemId2, int reqItemCount2, int reqItemId3, int reqItemCount3,
                               int reqItemId4, int reqItemCount4, int prevQuestId, int requiredRaces, int zoneOrSort) {
        public QuestTemplate(int id, String title, int minLevel, int type) {
            this(id, title, minLevel, type, 0, "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives) {
            this(id, title, minLevel, type, rewMoney, details, objectives, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives,
                             int reqCreatureOrGOId1, int reqCreatureOrGOCount1) {
            this(id, title, minLevel, type, rewMoney, details, objectives, reqCreatureOrGOId1, reqCreatureOrGOCount1,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives,
                             int reqCreatureOrGOId1, int reqCreatureOrGOCount1, int reqItemId1, int reqItemCount1) {
            this(id, title, minLevel, type, rewMoney, details, objectives, reqCreatureOrGOId1, reqCreatureOrGOCount1,
                    reqItemId1, reqItemCount1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives,
                             int reqCreatureOrGOId1, int reqCreatureOrGOCount1, int reqItemId1, int reqItemCount1,
                             int questLevel, int rewMoneyMaxLevel, int rewItemId1, int rewItemCount1) {
            this(id, title, minLevel, type, rewMoney, details, objectives, reqCreatureOrGOId1, reqCreatureOrGOCount1,
                    reqItemId1, reqItemCount1, questLevel, rewMoneyMaxLevel, rewItemId1, rewItemCount1, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives,
                             int reqCreatureOrGOId1, int reqCreatureOrGOCount1, int reqItemId1, int reqItemCount1,
                             int questLevel, int rewMoneyMaxLevel, int rewItemId1, int rewItemCount1,
                             int rewChoiceItemId1, int rewChoiceItemCount1, int rewChoiceItemId2, int rewChoiceItemCount2) {
            this(id, title, minLevel, type, rewMoney, details, objectives, reqCreatureOrGOId1, reqCreatureOrGOCount1,
                    reqItemId1, reqItemCount1, questLevel, rewMoneyMaxLevel, rewItemId1, rewItemCount1,
                    rewChoiceItemId1, rewChoiceItemCount1, rewChoiceItemId2, rewChoiceItemCount2,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public QuestTemplate(int id, String title, int minLevel, int type, int rewMoney, String details, String objectives,
                             int reqCreatureOrGOId1, int reqCreatureOrGOCount1, int reqItemId1, int reqItemCount1,
                             int questLevel, int rewMoneyMaxLevel, int rewItemId1, int rewItemCount1,
                             int rewChoiceItemId1, int rewChoiceItemCount1, int rewChoiceItemId2, int rewChoiceItemCount2,
                             int reqCreatureOrGOId2, int reqCreatureOrGOCount2,
                             int reqCreatureOrGOId3, int reqCreatureOrGOCount3,
                             int reqCreatureOrGOId4, int reqCreatureOrGOCount4,
                             int reqItemId2, int reqItemCount2, int reqItemId3, int reqItemCount3,
                             int reqItemId4, int reqItemCount4, int prevQuestId, int requiredRaces) {
            this(id, title, minLevel, type, rewMoney, details, objectives, reqCreatureOrGOId1, reqCreatureOrGOCount1,
                    reqItemId1, reqItemCount1, questLevel, rewMoneyMaxLevel, rewItemId1, rewItemCount1,
                    rewChoiceItemId1, rewChoiceItemCount1, rewChoiceItemId2, rewChoiceItemCount2,
                    reqCreatureOrGOId2, reqCreatureOrGOCount2, reqCreatureOrGOId3, reqCreatureOrGOCount3,
                    reqCreatureOrGOId4, reqCreatureOrGOCount4, reqItemId2, reqItemCount2, reqItemId3, reqItemCount3,
                    reqItemId4, reqItemCount4, prevQuestId, requiredRaces, 0);
        }

        int rewChoiceItemId(int index) {
            return switch (index) {
                case 0 -> rewChoiceItemId1;
                case 1 -> rewChoiceItemId2;
                default -> 0;
            };
        }

        int rewChoiceItemCount(int index) {
            return switch (index) {
                case 0 -> rewChoiceItemCount1;
                case 1 -> rewChoiceItemCount2;
                default -> 0;
            };
        }

        int rewChoiceItemsCount() {
            int n = 0;
            if (rewChoiceItemId1 > 0) {
                n++;
            }
            if (rewChoiceItemId2 > 0) {
                n++;
            }
            return n;
        }

        int rewItemsCount() {
            return rewItemId1 > 0 ? 1 : 0;
        }

        public int reqCreatureOrGOId(int index) {
            return switch (index) {
                case 0 -> reqCreatureOrGOId1;
                case 1 -> reqCreatureOrGOId2;
                case 2 -> reqCreatureOrGOId3;
                case 3 -> reqCreatureOrGOId4;
                default -> 0;
            };
        }

        public int reqCreatureOrGOCount(int index) {
            return switch (index) {
                case 0 -> reqCreatureOrGOCount1;
                case 1 -> reqCreatureOrGOCount2;
                case 2 -> reqCreatureOrGOCount3;
                case 3 -> reqCreatureOrGOCount4;
                default -> 0;
            };
        }

        public int reqItemId(int index) {
            return switch (index) {
                case 0 -> reqItemId1;
                case 1 -> reqItemId2;
                case 2 -> reqItemId3;
                case 3 -> reqItemId4;
                default -> 0;
            };
        }

        public int reqItemCount(int index) {
            return switch (index) {
                case 0 -> reqItemCount1;
                case 1 -> reqItemCount2;
                case 2 -> reqItemCount3;
                case 3 -> reqItemCount4;
                default -> 0;
            };
        }
    }
    public record GossipMenuItem(int menuId, int id, int icon, String text, int optionId, int npcFlag,
                                 int coded, int boxMoney, String boxText, int actionMenu, int actionPoi,
                                 int conditionId) {
        public GossipMenuItem(int menuId, int id, int icon, String text, int optionId, int npcFlag,
                              int coded, int boxMoney, String boxText, int actionMenu) {
            this(menuId, id, icon, text, optionId, npcFlag, coded, boxMoney, boxText, actionMenu, 0, 0);
        }

        public GossipMenuItem(int menuId, int id, int icon, String text, int optionId, int npcFlag,
                              int coded, int boxMoney, String boxText, int actionMenu, int actionPoi) {
            this(menuId, id, icon, text, optionId, npcFlag, coded, boxMoney, boxText, actionMenu, actionPoi, 0);
        }
    }
    /** points_of_interest; ObjectMgr.cpp LoadPointsOfInterest. */
    public record PointOfInterest(int entry, float x, float y, int icon, int flags, int data, String iconName) {
        public PointOfInterest {
            iconName = iconName == null ? "" : iconName;
        }
    }
    /** locales_points_of_interest entry 1; classic/TBC dump row (Lion's Pride Inn). */
    public static PointOfInterest lionsPrideInnPoi() {
        return new PointOfInterest(1, -9459f, 42.0805f, 7, 99, 0, "Lion's Pride Inn");
    }
    public record PageText(int id, String text, int nextPage) {}
    public record NpcTextSlot(float probability, String text0, String text1, int language, int[] emotes) {
        public NpcTextSlot {
            text0 = text0 == null ? "" : text0;
            text1 = text1 == null ? "" : text1;
            emotes = emotes == null || emotes.length != 6 ? new int[6] : emotes;
        }
    }
    public record NpcText(int id, NpcTextSlot[] slots) {
        public NpcText {
            NpcTextSlot[] eight = new NpcTextSlot[Content.MAX_GOSSIP_TEXT_OPTIONS];
            for (int i = 0; i < eight.length; i++) {
                eight[i] = slots != null && i < slots.length && slots[i] != null
                        ? slots[i] : new NpcTextSlot(0f, "", "", 0, new int[6]);
            }
            slots = eight;
        }
    }
    public record LootRow(int item, float chance, int minCount, int maxCount, boolean needsQuest) {
        /** Non-quest loot row. */
        public LootRow(int item, float chance, int minCount, int maxCount) {
            this(item, chance, minCount, maxCount, false);
        }
    }

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
        /** item_template spellid_1..5 / spelltrigger_1..5 / spellcharges_1..5. */
        public final int[] spellId = new int[5];
        public final int[] spellTrigger = new int[5];
        public final int[] spellCharges = new int[5];

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

        /** Server-owned Hero Rend follow-up quest item (92001). */
        public static ItemTemplate heroTrainingStrip() {
            return heroQuestJunk(org.tbc.world.classless.HeroClassUnlock.ITEM_TRAINING_STRIP, "Training Strip");
        }

        /** Server-owned Hero quest junk (class unlock / follow-up evidence items). */
        public static ItemTemplate heroQuestJunk(int entry, String name) {
            ItemTemplate t = new ItemTemplate();
            t.entry = entry;
            t.itemClass = 12;
            t.subClass = 0;
            t.unk = -1;
            t.name = name;
            t.displayId = 7412;
            t.quality = 1;
            t.inventoryType = 0;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.requiredLevel = 1;
            t.stackable = 20;
            return t;
        }

        /** Skinning Knife — item 7005; hunter create kit. */
        public static ItemTemplate skinningKnife() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_SKINNING_KNIFE;
            t.itemClass = 2;
            t.subClass = 14;
            t.unk = -1;
            t.name = "Skinning Knife";
            t.displayId = 6440;
            t.quality = 1;
            t.buyPrice = 82;
            t.sellPrice = 16;
            t.inventoryType = 13;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 4;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.dmgMin[0] = 1;
            t.dmgMax[0] = 3;
            t.delay = 1600;
            t.maxDurability = 20;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Recruit's Shirt — item 38 (CharStartOutfit human warrior). */
        public static ItemTemplate recruitsShirt() {
            ItemTemplate t = new ItemTemplate();
            t.entry = 38;
            t.itemClass = 4;
            t.subClass = 0;
            t.unk = -1;
            t.name = "Recruit's Shirt";
            t.displayId = 9891;
            t.quality = 1;
            t.buyPrice = 1;
            t.sellPrice = 1;
            t.inventoryType = 4;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.stackable = 1;
            return t;
        }

        /** Recruit's Pants — item 39. */
        public static ItemTemplate recruitsPants() {
            ItemTemplate t = new ItemTemplate();
            t.entry = 39;
            t.itemClass = 4;
            t.subClass = 1;
            t.unk = -1;
            t.name = "Recruit's Pants";
            t.displayId = 9892;
            t.quality = 0;
            t.buyPrice = 5;
            t.sellPrice = 1;
            t.inventoryType = 7;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.stackable = 1;
            t.armor = 2;
            return t;
        }

        /** Recruit's Boots — item 40. */
        public static ItemTemplate recruitsBoots() {
            ItemTemplate t = new ItemTemplate();
            t.entry = 40;
            t.itemClass = 4;
            t.subClass = 0;
            t.unk = -1;
            t.name = "Recruit's Boots";
            t.displayId = 10141;
            t.quality = 1;
            t.buyPrice = 5;
            t.sellPrice = 1;
            t.inventoryType = 8;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.stackable = 1;
            return t;
        }

        /** Riverpaw Leather Vest — tbc-db item_template 821 (ITEM_MOD_STAMINA 7 / 2, armor 65). */
        public static ItemTemplate riverpawLeatherVest() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_RIVERPAW_LEATHER_VEST;
            t.itemClass = 4;
            t.subClass = 2;
            t.name = "Riverpaw Leather Vest";
            t.displayId = 17102;
            t.quality = 2;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 13;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 2;
            t.armor = 65;
            t.bonding = 2;
            t.maxDurability = 60;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Tunic of Westfall — tbc-db item_template 2041 (ITEM_MOD_AGILITY 3 / 11, ITEM_MOD_STAMINA 7 / 5, armor 92). */
        public static ItemTemplate tunicOfWestfall() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TUNIC_OF_WESTFALL;
            t.itemClass = 4;
            t.subClass = 2;
            t.name = "Tunic of Westfall";
            t.quality = 3;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 24;
            t.stackable = 1;
            t.statType[0] = 3;
            t.statValue[0] = 11;
            t.statType[1] = 7;
            t.statValue[1] = 5;
            t.armor = 92;
            t.bonding = 1;
            t.maxDurability = 90;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Brackwater Vest — tbc-db item_template 3306 (ITEM_MOD_STRENGTH 4 / 4, ITEM_MOD_STAMINA 7 / 3, armor 162). */
        public static ItemTemplate brackwaterVest() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_BRACKWATER_VEST;
            t.itemClass = 4;
            t.subClass = 3;
            t.name = "Brackwater Vest";
            t.quality = 2;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 18;
            t.requiredLevel = 13;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 4;
            t.statType[1] = 7;
            t.statValue[1] = 3;
            t.armor = 162;
            t.bonding = 2;
            t.sellPrice = 654;
            t.maxDurability = 80;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Seer's Robe — tbc-db item_template 2981 (ITEM_MOD_INTELLECT 5 / 6, ITEM_MOD_SPIRIT 6 / 3, armor 35). */
        public static ItemTemplate seersRobe() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_SEERS_ROBE;
            t.itemClass = 4;
            t.subClass = 1;
            t.name = "Seer's Robe";
            t.quality = 2;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 21;
            t.requiredLevel = 16;
            t.stackable = 1;
            t.statType[0] = 5;
            t.statValue[0] = 6;
            t.statType[1] = 6;
            t.statValue[1] = 3;
            t.armor = 35;
            t.bonding = 2;
            t.sellPrice = 648;
            t.maxDurability = 60;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Blackened Defias Armor — tbc-db 10399 (STR 4, AGI 3, STA 11, armor 92). */
        public static ItemTemplate blackenedDefiasArmor() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_BLACKENED_DEFIAS_ARMOR;
            t.itemClass = 4;
            t.subClass = 2;
            t.name = "Blackened Defias Armor";
            t.quality = 3;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 24;
            t.requiredLevel = 19;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 4;
            t.statType[1] = 3;
            t.statValue[1] = 3;
            t.statType[2] = 7;
            t.statValue[2] = 11;
            t.armor = 92;
            t.bonding = 1;
            t.sellPrice = 1467;
            t.maxDurability = 90;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Lightforge Breastplate — tbc-db 16726 (STR 13, STA 21, INT 16, SPI 8, armor 657). */
        public static ItemTemplate lightforgeBreastplate() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_LIGHTFORGE_BREASTPLATE;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Lightforge Breastplate";
            t.quality = 3;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 63;
            t.requiredLevel = 58;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 13;
            t.statType[1] = 7;
            t.statValue[1] = 21;
            t.statType[2] = 5;
            t.statValue[2] = 16;
            t.statType[3] = 6;
            t.statValue[3] = 8;
            t.armor = 657;
            t.bonding = 1;
            t.sellPrice = 35078;
            t.maxDurability = 135;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Lawbringer Chestguard — tbc-db 16853 (STR 8, STA 26, INT 21, SPI 13, armor 855, FireRes 10). */
        public static ItemTemplate lawbringerChestguard() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_LAWBRINGER_CHESTGUARD;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Lawbringer Chestguard";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 66;
            t.requiredLevel = 60;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 8;
            t.statType[1] = 7;
            t.statValue[1] = 26;
            t.statType[2] = 5;
            t.statValue[2] = 21;
            t.statType[3] = 6;
            t.statValue[3] = 13;
            t.armor = 855;
            t.fireRes = 10;
            t.bonding = 1;
            t.sellPrice = 52573;
            t.maxDurability = 165;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Living Breastplate — tbc-db 15059 (STA 10, SPI 25, armor 169, NatureRes 5). */
        public static ItemTemplate livingBreastplate() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_LIVING_BREASTPLATE;
            t.itemClass = 4;
            t.subClass = 2;
            t.name = "Living Breastplate";
            t.quality = 3;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 60;
            t.requiredLevel = 55;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 10;
            t.statType[1] = 6;
            t.statValue[1] = 25;
            t.armor = 169;
            t.natureRes = 5;
            t.bonding = 2;
            t.sellPrice = 24776;
            t.maxDurability = 100;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Icebane Breastplate — tbc-db 22669 (STR 12, STA 24, armor 1027, FrostRes 42). */
        public static ItemTemplate icebaneBreastplate() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_ICEBANE_BREASTPLATE;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Icebane Breastplate";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 80;
            t.requiredLevel = 60;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 12;
            t.statType[1] = 7;
            t.statValue[1] = 24;
            t.armor = 1027;
            t.frostRes = 42;
            t.bonding = 2;
            t.sellPrice = 57715;
            t.maxDurability = 165;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Shadesteel Greaves — tbc-db 32404 (STA 54, armor 1428, ShadowRes 72). */
        public static ItemTemplate shadesteelGreaves() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_SHADESTEEL_GREAVES;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Shadesteel Greaves";
            t.quality = 4;
            t.inventoryType = 7;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 130;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 54;
            t.armor = 1428;
            t.shadowRes = 72;
            t.bonding = 2;
            t.sellPrice = 95498;
            t.maxDurability = 120;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Soulcloth Vest — tbc-db 21865 (STA 24, INT 20, SPI 16, armor 170, ArcaneRes 45). */
        public static ItemTemplate soulclothVest() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_SOULCLOTH_VEST;
            t.itemClass = 4;
            t.subClass = 1;
            t.name = "Soulcloth Vest";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 100;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 24;
            t.statType[1] = 5;
            t.statValue[1] = 20;
            t.statType[2] = 6;
            t.statValue[2] = 16;
            t.armor = 170;
            t.arcaneRes = 45;
            t.bonding = 2;
            t.sellPrice = 41251;
            t.maxDurability = 100;
            t.requiredDisenchantSkill = 300;
            return t;
        }

        /** Blade of Hanna — tbc-db 2801 (STR/AGI/STA/INT/SPI 11, dmg 101–152, delay 2100). */
        public static ItemTemplate bladeOfHanna() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_BLADE_OF_HANNA;
            t.itemClass = 2;
            t.subClass = 8;
            t.name = "Blade of Hanna";
            t.quality = 4;
            t.inventoryType = 17;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 64;
            t.requiredLevel = 59;
            t.stackable = 1;
            t.maxCount = 1;
            t.statType[0] = 4;
            t.statValue[0] = 11;
            t.statType[1] = 3;
            t.statValue[1] = 11;
            t.statType[2] = 7;
            t.statValue[2] = 11;
            t.statType[3] = 5;
            t.statValue[3] = 11;
            t.statType[4] = 6;
            t.statValue[4] = 11;
            t.dmgMin[0] = 101;
            t.dmgMax[0] = 152;
            t.delay = 2100;
            t.bonding = 2;
            t.sellPrice = 90978;
            t.maxDurability = 120;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Destroyer Chestguard — tbc-db 30113 (STR 25, AGI 26, STA 57, DEF 27, DODGE 24, HIT 24, armor 1668). */
        public static ItemTemplate destroyerChestguard() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_DESTROYER_CHESTGUARD;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Destroyer Chestguard";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 133;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 25;
            t.statType[1] = 3;
            t.statValue[1] = 26;
            t.statType[2] = 7;
            t.statValue[2] = 57;
            t.statType[3] = 12;
            t.statValue[3] = 27;
            t.statType[4] = 13;
            t.statValue[4] = 24;
            t.statType[5] = 31;
            t.statValue[5] = 24;
            t.armor = 1668;
            t.bonding = 1;
            t.maxDurability = 165;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Destroyer Breastplate — tbc.cavernoftime.com 30118 (STR 50, STA 48, CRIT 33, HIT 15, armor 1668). */
        public static ItemTemplate destroyerBreastplate() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_DESTROYER_BREASTPLATE;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Destroyer Breastplate";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 133;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 50;
            t.statType[1] = 7;
            t.statValue[1] = 48;
            t.statType[2] = 32;
            t.statValue[2] = 33;
            t.statType[3] = 31;
            t.statValue[3] = 15;
            t.armor = 1668;
            t.bonding = 1;
            t.maxDurability = 165;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Gladiator's Plate Chestpiece — tbc.cavernoftime.com 24544 (STA 49, STR 23, CRIT 30, RES 23, HIT 12, armor 1547). */
        public static ItemTemplate gladiatorsPlateChestpiece() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_GLADIATORS_PLATE_CHESTPIECE;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Gladiator's Plate Chestpiece";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 123;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 49;
            t.statType[1] = 4;
            t.statValue[1] = 23;
            t.statType[2] = 32;
            t.statValue[2] = 30;
            t.statType[3] = 35;
            t.statValue[3] = 23;
            t.statType[4] = 31;
            t.statValue[4] = 12;
            t.armor = 1547;
            t.bonding = 1;
            t.maxDurability = 165;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Warharness of Reckless Fury — tbc.cavernoftime.com 34215 (STR 61, STA 67, CRIT 41, HASTE 32, armor 1983). */
        public static ItemTemplate warharnessOfRecklessFury() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_WARHARNESS_OF_RECKLESS_FURY;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Warharness of Reckless Fury";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 159;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 61;
            t.statType[1] = 7;
            t.statValue[1] = 67;
            t.statType[2] = 32;
            t.statValue[2] = 41;
            t.statType[3] = 36;
            t.statValue[3] = 32;
            t.armor = 1983;
            t.bonding = 1;
            t.maxDurability = 165;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Gauntlets of Enforcement — tbc.cavernoftime.com 32280 (STA 70, DEF 32, EXPERTISE 21, armor 1103). */
        public static ItemTemplate gauntletsOfEnforcement() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_GAUNTLETS_OF_ENFORCEMENT;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Gauntlets of Enforcement";
            t.quality = 4;
            t.inventoryType = 10;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 141;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 70;
            t.statType[1] = 12;
            t.statValue[1] = 32;
            t.statType[2] = 37;
            t.statValue[2] = 21;
            t.armor = 1103;
            t.bonding = 1;
            t.maxDurability = 55;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Onslaught Chestguard — tbc-db 30976 (AGI 37, STA 69, DEF 37, PARRY 28, BLOCK 23, armor 1825). */
        public static ItemTemplate onslaughtChestguard() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_ONSLAUGHT_CHESTGUARD;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Onslaught Chestguard";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 146;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 3;
            t.statValue[0] = 37;
            t.statType[1] = 7;
            t.statValue[1] = 69;
            t.statType[2] = 12;
            t.statValue[2] = 37;
            t.statType[3] = 14;
            t.statValue[3] = 28;
            t.statType[4] = 15;
            t.statValue[4] = 23;
            t.armor = 1825;
            t.bonding = 1;
            t.maxDurability = 165;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Auchenai Anchorite's Robe — tbc-db 29341 (INT 24, HIT_SPELL_RATING 23, armor 136).
         */
        public static ItemTemplate auchenaiAnchoritesRobe() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_AUCHENAI_ANCHORITES_ROBE;
            t.itemClass = 4;
            t.subClass = 1;
            t.name = "Auchenai Anchorite's Robe";
            t.quality = 3;
            t.inventoryType = 20;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 100;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 5;
            t.statValue[0] = 24;
            t.statType[1] = 18;
            t.statValue[1] = 23;
            t.armor = 136;
            t.bonding = 1;
            t.maxDurability = 80;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Garments of Serene Shores — tbc-db 34229 (STA 48, INT 41, CRIT_SPELL_RATING 25, armor 1110).
         */
        public static ItemTemplate garmentsOfSereneShores() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_GARMENTS_OF_SERENE_SHORES;
            t.itemClass = 4;
            t.subClass = 3;
            t.name = "Garments of Serene Shores";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 159;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 48;
            t.statType[1] = 5;
            t.statValue[1] = 41;
            t.statType[2] = 21;
            t.statValue[2] = 25;
            t.armor = 1110;
            t.bonding = 1;
            t.maxDurability = 140;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Sunglow Vest — tbc-db 34212 (STA 48, INT 41, HASTE_SPELL_RATING 33, armor 499).
         */
        public static ItemTemplate sunglowVest() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_SUNGLOW_VEST;
            t.itemClass = 4;
            t.subClass = 2;
            t.name = "Sunglow Vest";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 159;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 48;
            t.statType[1] = 5;
            t.statValue[1] = 41;
            t.statType[2] = 30;
            t.statValue[2] = 33;
            t.armor = 499;
            t.bonding = 1;
            t.maxDurability = 120;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Worn Wooden Shield — tbc-db 2362 (InventoryType 14, block 1, armor 5).
         */
        public static ItemTemplate wornWoodenShield() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_WORN_WOODEN_SHIELD;
            t.itemClass = 4;
            t.subClass = 6;
            t.name = "Worn Wooden Shield";
            t.displayId = 18730;
            t.quality = 0;
            t.buyPrice = 7;
            t.sellPrice = 1;
            t.inventoryType = 14;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.armor = 5;
            t.block = 1;
            t.sheath = 4;
            t.maxDurability = 20;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Cloak of Darkness — tbc-db 33122 (STA 25, CRIT_MELEE_RATING 24, STR 23, armor 101).
         */
        public static ItemTemplate cloakOfDarkness() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_CLOAK_OF_DARKNESS;
            t.itemClass = 4;
            t.subClass = 1;
            t.name = "Cloak of Darkness";
            t.displayId = 26202;
            t.quality = 4;
            t.buyPrice = 190146;
            t.sellPrice = 38029;
            t.inventoryType = 16;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 120;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 25;
            t.statType[1] = 19;
            t.statValue[1] = 24;
            t.statType[2] = 4;
            t.statValue[2] = 23;
            t.armor = 101;
            t.bonding = 2;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Netherstrand Longbow — tbc-db 30318 (CRIT_RANGED_RATING 50, STA 20, bow).
         */
        public static ItemTemplate netherstrandLongbow() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_NETHERSTRAND_LONGBOW;
            t.itemClass = 2;
            t.subClass = 2;
            t.name = "Netherstrand Longbow";
            t.displayId = 41875;
            t.quality = 5;
            t.inventoryType = 15;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 175;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 20;
            t.statValue[0] = 50;
            t.statType[1] = 7;
            t.statValue[1] = 20;
            t.dmgMin[0] = 256;
            t.dmgMax[0] = 385;
            t.delay = 2900;
            t.maxDurability = 110;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Twin Blades of Azzinoth — tbc-db 18582 (physical 148–155, shadow 40–60, arcane 40–60).
         */
        public static ItemTemplate twinBladesOfAzzinoth() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TWIN_BLADES_OF_AZZINOTH;
            t.itemClass = 2;
            t.subClass = 7;
            t.name = "The Twin Blades of Azzinoth";
            t.displayId = 30936;
            t.quality = 6;
            t.inventoryType = 13;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 100;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.dmgMin[0] = 148;
            t.dmgMax[0] = 155;
            t.dmgMin[1] = 40;
            t.dmgMax[1] = 60;
            t.dmgType[1] = 5;
            t.dmgMin[2] = 40;
            t.dmgMax[2] = 60;
            t.dmgType[2] = 6;
            t.delay = 1500;
            t.bonding = 1;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * JYoo test item — tbc-db 1259 (five damage lines; frost 1000 school 4, shadow 4000 school 5).
         */
        public static ItemTemplate jyooTestItem() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_JYOO_TEST_ITEM;
            t.itemClass = 2;
            t.subClass = 0;
            t.name = "JYoo test item";
            t.inventoryType = 21;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.dmgMin[0] = 1;
            t.dmgMax[0] = 1;
            t.dmgMin[1] = 3000;
            t.dmgMax[1] = 3000;
            t.dmgType[1] = 2;
            t.dmgMin[2] = 2000;
            t.dmgMax[2] = 2000;
            t.dmgType[2] = 3;
            t.dmgMin[3] = 1000;
            t.dmgMax[3] = 1000;
            t.dmgType[3] = 4;
            t.dmgMin[4] = 4000;
            t.dmgMax[4] = 4000;
            t.dmgType[4] = 5;
            t.delay = 1000;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Tom's Boots 1 — tbc-db 32954 (STR 30, STA 43, CRIT 23, HIT_MELEE 15, HIT_RANGED 15).
         */
        public static ItemTemplate tomsBoots1() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TOMS_BOOTS_1;
            t.itemClass = 4;
            t.subClass = 4;
            t.name = "Tom's Boots 1";
            t.displayId = 29863;
            t.quality = 4;
            t.inventoryType = 8;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 115;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 4;
            t.statValue[0] = 30;
            t.statType[1] = 7;
            t.statValue[1] = 43;
            t.statType[2] = 32;
            t.statValue[2] = 23;
            t.statType[3] = 16;
            t.statValue[3] = 15;
            t.statType[4] = 17;
            t.statValue[4] = 15;
            t.armor = 997;
            t.bonding = 1;
            t.maxDurability = 75;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Test HP Ring — tbc-db 6673 (ITEM_MOD_HEALTH −60, finger).
         */
        public static ItemTemplate testHpRing() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TEST_HP_RING;
            t.itemClass = 4;
            t.subClass = 0;
            t.name = "Test HP Ring";
            t.displayId = 9832;
            t.inventoryType = 11;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 1;
            t.statValue[0] = -60;
            t.bonding = 2;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Test Holy Resist Vest — HolyRes 10, chest (SPELL_SCHOOL_HOLY → RESISTANCES+1). */
        public static ItemTemplate testHolyResistVest() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TEST_HOLY_RESIST_VEST;
            t.itemClass = 4;
            t.subClass = 1;
            t.name = "Test Holy Resist Vest";
            t.quality = 2;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.armor = 10;
            t.holyRes = 10;
            t.bonding = 2;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Test MP Ring — tbc-db 6674 (ITEM_MOD_MANA −60, finger).
         */
        public static ItemTemplate testMpRing() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TEST_MP_RING;
            t.itemClass = 4;
            t.subClass = 0;
            t.name = "Test MP Ring";
            t.displayId = 9832;
            t.inventoryType = 11;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 1;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 0;
            t.statValue[0] = -60;
            t.bonding = 2;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Band of the Eternal Champion — tbc-db 29301 (AGI 29, ON_EQUIP 14052 + 35080).
         */
        public static ItemTemplate bandOfTheEternalChampion() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_BAND_OF_THE_ETERNAL_CHAMPION;
            t.itemClass = 4;
            t.subClass = 0;
            t.name = "Band of the Eternal Champion";
            t.displayId = 39126;
            t.quality = 4;
            t.inventoryType = 11;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 152;
            t.requiredLevel = 1;
            t.stackable = 1;
            t.statType[0] = 3;
            t.statValue[0] = 29;
            t.spellId[0] = Content.SPELL_ATTACK_POWER_60;
            t.spellTrigger[0] = 1;
            t.spellId[1] = 35080;
            t.spellTrigger[1] = 1;
            t.bonding = 1;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /**
         * Vengeful Gladiator's Dragonhide Tunic — tbc-db 33675 (STA 54, STR 30, INT 22, AGI 31,
         * RES 26, HIT 12, CRIT 19 in stat_type7, armor 529).
         */
        public static ItemTemplate vengefulGladiatorsDragonhideTunic() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_VENGEFUL_GLADIATORS_DRAGONHIDE_TUNIC;
            t.itemClass = 4;
            t.subClass = 2;
            t.name = "Vengeful Gladiator's Dragonhide Tunic";
            t.quality = 4;
            t.inventoryType = 5;
            t.allowableClass = -1;
            t.allowableRace = -1;
            t.itemLevel = 146;
            t.requiredLevel = 70;
            t.stackable = 1;
            t.statType[0] = 7;
            t.statValue[0] = 54;
            t.statType[1] = 4;
            t.statValue[1] = 30;
            t.statType[2] = 5;
            t.statValue[2] = 22;
            t.statType[3] = 3;
            t.statValue[3] = 31;
            t.statType[4] = 35;
            t.statValue[4] = 26;
            t.statType[5] = 31;
            t.statValue[5] = 12;
            t.statType[6] = 32;
            t.statValue[6] = 19;
            t.armor = 529;
            t.bonding = 1;
            t.maxDurability = 120;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Guild Charter — item 5863. PetitionsHandler.cpp GUILD_CHARTER. */
        public static ItemTemplate guildCharter() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_GUILD_CHARTER;
            t.name = "Guild Charter";
            t.displayId = Content.CHARTER_DISPLAY_ID;
            t.quality = 1;
            t.buyPrice = Content.GUILD_CHARTER_COST;
            t.stackable = 1;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Hearthstone — item 6948 ON_USE spell 8690 (ITEM_SPELLTRIGGER_ON_USE, charges 0). */
        public static ItemTemplate hearthstone() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_HEARTHSTONE;
            t.itemClass = 15;
            t.name = "Hearthstone";
            t.displayId = 6418;
            t.quality = 1;
            t.flags = 64;
            t.stackable = 1;
            t.maxCount = 1;
            t.bonding = 1;
            t.requiredDisenchantSkill = -1;
            t.spellId[0] = SpellEngine.HEARTHSTONE;
            t.spellTrigger[0] = 0;
            t.spellCharges[0] = 0;
            return t;
        }

        /** Tough Jerky — item 117 ON_USE Food 433, expendable charges −1. */
        public static ItemTemplate toughJerky() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TOUGH_JERKY;
            t.name = "Tough Jerky";
            t.itemClass = 0;
            t.subClass = 0;
            t.quality = 1;
            t.stackable = 20;
            t.requiredDisenchantSkill = -1;
            t.spellId[0] = SpellEngine.SPELL_FOOD;
            t.spellTrigger[0] = 0;
            t.spellCharges[0] = -1;
            return t;
        }

        /** Tough Hunk of Bread — item 4540, stackable 20 (item_template). */
        public static ItemTemplate toughHunkOfBread() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_TOUGH_HUNK_OF_BREAD;
            t.name = "Tough Hunk of Bread";
            t.itemClass = 0;
            t.subClass = 0;
            t.displayId = 6399;
            t.quality = 1;
            t.buyPrice = 25;
            t.sellPrice = 1;
            t.stackable = 20;
            t.requiredDisenchantSkill = -1;
            t.spellId[0] = SpellEngine.SPELL_FOOD;
            t.spellTrigger[0] = 0;
            t.spellCharges[0] = -1;
            return t;
        }

        /** Red Burlap Bandana — item 752, quest 18 objective, stackable 20. */
        public static ItemTemplate redBurlapBandana() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_RED_BURLAP_BANDANA;
            t.name = "Red Burlap Bandana";
            t.itemClass = 4;
            t.subClass = 0;
            t.displayId = 16815;
            t.quality = 1;
            t.stackable = 20;
            t.requiredDisenchantSkill = -1;
            return t;
        }

        /** Refreshing Spring Water — item 159 ON_USE Drink 430, expendable charges −1. */
        public static ItemTemplate refreshingSpringWater() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_REFRESHING_SPRING_WATER;
            t.name = "Refreshing Spring Water";
            t.itemClass = 0;
            t.subClass = 5;
            t.quality = 1;
            t.stackable = 20;
            t.requiredDisenchantSkill = -1;
            t.spellId[0] = SpellEngine.SPELL_DRINK;
            t.spellTrigger[0] = 0;
            t.spellCharges[0] = -1;
            return t;
        }

        /** Minor Healing Potion — item 118 ON_USE spell 439, stackable 5 (item_template). */
        public static ItemTemplate minorHealingPotion() {
            ItemTemplate t = new ItemTemplate();
            t.entry = Content.ITEM_MINOR_HEALING_POTION;
            t.name = "Minor Healing Potion";
            t.itemClass = 0;
            t.subClass = 1;
            t.displayId = 15710;
            t.quality = 1;
            t.buyPrice = 20;
            t.sellPrice = 5;
            t.stackable = 5;
            t.requiredDisenchantSkill = -1;
            t.spellId[0] = 439;
            t.spellTrigger[0] = 0;
            t.spellCharges[0] = -1;
            return t;
        }
    }

    /**
     * One placement from {@link #storeNewItem}: either a new bag object ({@code created}) or an
     * existing stack that absorbed {@code added} units.
     */
    public record StoredItem(Item item, int added, boolean created) {}

    public final Map<Long, CreateInfo> createInfo = new HashMap<>();
    public final EventAiStore eventAiStore = new EventAiStore();
    public final DbScriptStore dbScriptStore = new DbScriptStore();
    /** Applies EventAI spawn casts (Permanent Feign Death, etc.) without a live World session. */
    private final SpellEngine eventAiSpells = new SpellEngine();
    /**
     * creature_template MinLevelMana (and seeds). Entry → mana pool before PowerMultiplier.
     * Mana Wyrm 15274 and other caster NPCs need this for the client mana bar / Mana Tap.
     */
    public final Map<Integer, Integer> creatureMana = new HashMap<>();
    public final Map<Integer, List<Integer>> createSpells = new HashMap<>();
    /** player_classlevelstats / player_levelstats (create-self.md "Stats (level 1)"). */
    public final LevelStats levelStats = new LevelStats();
    /** SkillLineAbility.dbc — craft skill-up bands by spell id. */
    public final SkillLineAbility skillLineAbilities = SkillLineAbility.seeded();
    /** Packed like {@code SMSG_ACTION_BUTTONS}: action in low 24 bits, {@code ActionButtonType} in high 8. */
    public final Map<Integer, int[]> createActions = new HashMap<>();
    public final Map<Integer, List<CreateItem>> createItems = new HashMap<>();
    public final List<CreateSkill> createSkills = new ArrayList<>();
    public final Map<Integer, List<Integer>> startOutfit = new HashMap<>();
    public final Map<Integer, CreatureTemplate> creatures = new HashMap<>();
    public Factions factions;
    public final Map<Integer, List<LootRow>> creatureLoot = new HashMap<>();
    /** gameobject_loot_template keyed by chest loot id (gameobject_template.data[1]). */
    public final Map<Integer, List<LootRow>> gameObjectLoot = new HashMap<>();
    public final Map<Integer, Float> modelCombatReach = new HashMap<>();
    public final Map<Integer, QuestTemplate> quests = new HashMap<>();
    public final Map<Integer, ItemTemplate> items = new HashMap<>();
    public final Map<Integer, GameObjectTemplate> gameObjects = new HashMap<>();
    public final Map<Integer, PageText> pageTexts = new HashMap<>();
    /** Character DB `item_text` (ObjectMgr::GetItemText). */
    public final Map<Integer, String> itemTexts = new HashMap<>();
    public final Map<Integer, NpcText> npcTexts = new HashMap<>();
    public final List<Spawn> spawns = new ArrayList<>();
    public final List<Spawn> goSpawns = new ArrayList<>();
    public final Map<Integer, List<Spawn>> eventCreatures = new HashMap<>();
    public final Map<Integer, List<Spawn>> eventGameObjects = new HashMap<>();
    public record AreaTrigger(int id, int map, float x, float y, float z, float o) {}
    public final Map<Integer, AreaTrigger> areaTriggers = new HashMap<>();
    public final Map<Integer, List<Integer>> vendorItems = new HashMap<>();
    /** npc_vendor_template keyed by template id (CMaNGOS m_mCacheVendorTemplateItemMap). */
    public final Map<Integer, List<Integer>> vendorTemplateItems = new HashMap<>();
    /** creature_template.VendorTemplateId → npc_vendor_template.entry. */
    public final Map<Integer, Integer> vendorTemplateId = new HashMap<>();
    public final Map<Integer, Integer> gossipMenuIds = new HashMap<>();
    public final Map<Integer, Integer> gossipTextIds = new HashMap<>();
    public final Map<Integer, List<GossipMenuItem>> gossipOptions = new HashMap<>();
    public final Map<Integer, PointOfInterest> pointsOfInterest = new HashMap<>();
    public final Map<Integer, List<Integer>> questGivers = new HashMap<>();
    public final Map<Integer, List<Integer>> questInvolved = new HashMap<>();
    public final Map<Integer, List<Integer>> goQuestGivers = new HashMap<>();
    public final Map<Integer, List<Integer>> goQuestInvolved = new HashMap<>();
    public final Map<Integer, QuestExtras> questExtras = new HashMap<>();
    /** Hero unlock: landed melee hits on creature (not kill credit). */
    public record CreatureHitObjective(int creatureEntry, int count) {}
    /** Hero quest: text emote (`EmotesText.dbc`) within interact range of an NPC, who then casts. */
    public record EmoteNearNpcObjective(int textEmote, int npcEntry, int castSpellId) {}
    public final Map<Integer, CreatureHitObjective> questCreatureHits = new HashMap<>();
    public final Map<Integer, EmoteNearNpcObjective> questEmoteNearNpc = new HashMap<>();
    /** quest_template.RewSpell / RewSpellCast. */
    public final Map<Integer, Integer> questRewSpell = new HashMap<>();
    public final Map<Integer, Integer> areaTriggerQuests = new HashMap<>();

    /** Columns beyond the kill/item row: spell, explore, reputation, timer, daily, escort point. */
    public record QuestExtras(int reqSpell1, int specialFlags, int questFlags, int limitSeconds,
                              int pointMapId, float pointX, float pointY,
                              int reqRepFaction, int reqRepValue, int rewRepFaction, int rewRepValue) {
        public static final int EXPLORE = 0x002;
        public static final int DAILY = 0x1000;

        public static QuestExtras explore() {
            return new QuestExtras(0, EXPLORE, 0, 0, 0, 0f, 0f, 0, 0, 0, 0);
        }

        public static QuestExtras spell(int spellId) {
            return new QuestExtras(spellId, 0, 0, 0, 0, 0f, 0f, 0, 0, 0, 0);
        }

        public static QuestExtras reputation(int rewFaction, int rewValue, int reqFaction, int reqValue) {
            return new QuestExtras(0, 0, 0, 0, 0, 0f, 0f, reqFaction, reqValue, rewFaction, rewValue);
        }

        public static QuestExtras point(int mapId, float x, float y) {
            return new QuestExtras(0, 0, 0, 0, mapId, x, y, 0, 0, 0, 0);
        }

        public static QuestExtras limit(int seconds) {
            return new QuestExtras(0, 0, 0, seconds, 0, 0f, 0f, 0, 0, 0, 0);
        }

        public static QuestExtras daily() {
            return new QuestExtras(0, 0, DAILY, 0, 0, 0f, 0f, 0, 0, 0, 0);
        }

        public boolean exploreOrEvent() {
            return (specialFlags & EXPLORE) != 0;
        }

        public boolean isDaily() {
            return (questFlags & DAILY) != 0;
        }
    }
    public record TrainerSpell(int spell, int cost, int reqLevel, int reqSkill, int reqSkillValue,
                               int reqAbility0, int reqAbility1, int reqAbility2,
                               boolean primaryProfessionFirstRank) {
        public TrainerSpell(int spell, int cost, int reqLevel) {
            this(spell, cost, reqLevel, 0, 0, 0, 0, 0, false);
        }

        public TrainerSpell(int spell, int cost, int reqLevel, int reqSkill, int reqSkillValue) {
            this(spell, cost, reqLevel, reqSkill, reqSkillValue, 0, 0, 0, false);
        }
    }
    public record TaxiHop(int from, int to, int cost, float x, float y, float z) {}
    /** TaxiNodes.dbc row used by GetNearestTaxiNode. Mount flags = MountCreatureID != 0. */
    public record TaxiNode(int id, int mapId, float x, float y, float z, boolean alliance, boolean horde) {}
    public record ZoneWeather(int zone, int state, float grade) {}
    public record Auction(int id, int itemEntry, long owner, int startBid, int buyout, int timeLeftMs, String name,
                          long itemGuid, long bidder, int currentBid, int ownerAccount) {
        public Auction(int id, int itemEntry, long owner, int startBid, int buyout, int timeLeftMs, String name) {
            this(id, itemEntry, owner, startBid, buyout, timeLeftMs, name, 0, 0, 0, 0);
        }
    }
    public final Map<Integer, Integer> battleMasterBg = new HashMap<>();
    /** npc_trainer keyed by creature entry (CMaNGOS m_mCacheTrainerSpellMap). */
    public final Map<Integer, List<TrainerSpell>> trainerSpells = new HashMap<>();
    /** npc_trainer_template keyed by template id (CMaNGOS m_mCacheTrainerTemplateSpellMap). */
    public final Map<Integer, List<TrainerSpell>> trainerTemplateSpells = new HashMap<>();
    public final Map<Integer, Integer> trainerClass = new HashMap<>();
    /** creature_template.TrainerType (0 class, 1 mounts, 2 tradeskills, 3 pets). */
    public final Map<Integer, Integer> trainerTypeByEntry = new HashMap<>();
    /** creature_template.TrainerTemplateId → npc_trainer_template.entry. */
    public final Map<Integer, Integer> trainerTemplateId = new HashMap<>();
    /** spell_chain keyed by spell_id (CMaNGOS SpellMgr). */
    public record SpellChainNode(int spellId, int prev, int first, int rank, int req) {}
    public final Map<Integer, SpellChainNode> spellChain = new HashMap<>();
    /** spell_template.BaseLevel — fallback when trainer row reqlevel is 0. */
    public final Map<Integer, Integer> spellBaseLevel = new HashMap<>();
    public final Map<Integer, TaxiNode> taxiNodes = new HashMap<>();
    public final Map<Long, TaxiHop> taxiPaths = new HashMap<>();
    public final Map<Integer, ZoneWeather> weather = new HashMap<>();
    public final List<Auction> auctions = new ArrayList<>();
    public final Map<Integer, Guild> guilds = new HashMap<>();
    public final Map<Integer, ArenaTeam> arenaTeams = new HashMap<>();
    /** Talent.dbc / TalentTab.dbc. Player.cpp LearnTalent. */
    public record Talent(int id, int tab, int row, int col, int rank0, int rank1, int rank2, int rank3, int rank4,
                         int dependsOn, int dependsOnRank, int dependsOnSpell) {
        public int rank(int i) {
            return switch (i) {
                case 0 -> rank0;
                case 1 -> rank1;
                case 2 -> rank2;
                case 3 -> rank3;
                case 4 -> rank4;
                default -> 0;
            };
        }
    }
    public record TalentTab(int id, int classMask) {}
    public final Map<Integer, Talent> talents = new HashMap<>();
    public final Map<Integer, TalentTab> talentTabs = new HashMap<>();
    public final AtomicInteger nextGuildId = new AtomicInteger(1);
    public final AtomicInteger nextAuctionId = new AtomicInteger(2);
    public final AtomicInteger nextCreatureLow = new AtomicInteger(1_000_000);
    public final AtomicInteger nextItemLow = new AtomicInteger(1);

    /** petition / petition_sign. PetitionsHandler.cpp. */
    public static final class Petition {
        public int guidLow;
        public long ownerGuid;
        public int ownerAccount;
        public String name = "";
        public int type;
        public final List<Long> signers = new ArrayList<>();
        public final List<Integer> signerAccounts = new ArrayList<>();
    }

    public final Map<Integer, Petition> petitions = new HashMap<>();

    /** creature / gameobject row; respawn min/max = spawntimesecsmin/max (seeded rows keep the 300 s default). */
    public record Spawn(int guid, int entry, int map, float x, float y, float z, float o,
            float spawnDist, int movementType, int respawnMinSecs, int respawnMaxSecs) {
        public static final int DEFAULT_RESPAWN_SECS = 300;

        public Spawn(int guid, int entry, int map, float x, float y, float z, float o) {
            this(guid, entry, map, x, y, z, o, 0f, 0);
        }

        public Spawn(int guid, int entry, int map, float x, float y, float z, float o,
                float spawnDist, int movementType) {
            this(guid, entry, map, x, y, z, o, spawnDist, movementType, DEFAULT_RESPAWN_SECS, DEFAULT_RESPAWN_SECS);
        }

        /** CreatureData::GetRandomRespawnTime — urand(min, max); max below min is clamped to min (ObjectMgr). */
        public int randomRespawnSecs() {
            int max = Math.max(respawnMinSecs, respawnMaxSecs);
            return respawnMinSecs + ThreadLocalRandom.current().nextInt(max - respawnMinSecs + 1);
        }
    }

    public void load(DbPool world, ScriptRegistry scripts) {
        load(world, scripts, null);
    }

    public void load(DbPool world, ScriptRegistry scripts, Path dataDir) {
        if (world == null) {
            seedDefaults();
            seedQueryDefaults();
            loadStartOutfit(dataDir);
            loadTalents(dataDir);
            levelStats.loadGt(dataDir);
            DurabilityCosts.load(dataDir);
            skillLineAbilities.loadFromDataDir(dataDir);
            return;
        }
        try (Connection c = world.get()) {
            try {
                loadCreate(c);
            } catch (Exception e) {
                log.warn("playercreateinfo load failed: {}", e.getMessage());
            }
            levelStats.load(c);
            loadCreatures(c);
            loadCreatureMana(c);
            loadModelInfo(c);
            loadCreatureLoot(c);
            loadGameObjectLoot(c);
            eventAiStore.load(c);
            dbScriptStore.load(c);
            try {
                loadSpawns(c);
            } catch (Exception e) {
                log.warn("creature spawn load failed: {}", e.getMessage());
            }
            try {
                loadGoSpawns(c);
            } catch (Exception e) {
                log.warn("gameobject spawn load failed: {}", e.getMessage());
            }
            try {
                loadEventCreatures(c);
            } catch (Exception e) {
                log.debug("game_event_creature load skipped: {}", e.getMessage());
            }
            try {
                loadEventGameObjects(c);
            } catch (Exception e) {
                log.debug("game_event_gameobject load skipped: {}", e.getMessage());
            }
            loadQuests(c);
            loadQuestRelations(c);
            loadQuestExtras(c);
            loadAreaTriggers(c);
            loadItems(c);
            loadItemSpells(c);
            loadItemSheath(c);
            loadNpcVendors(c);
            try {
                loadVendorMeta(c);
            } catch (Exception e) {
                log.debug("vendor meta load skipped: {}", e.getMessage());
            }
            try {
                loadGossip(c);
            } catch (Exception e) {
                log.debug("gossip load skipped: {}", e.getMessage());
            }
            loadGameObjects(c);
            loadPageTexts(c);
            try {
                loadWeather(c);
            } catch (Exception e) {
                log.debug("game_weather load skipped: {}", e.getMessage());
            }
            try {
                loadBattleMasters(c);
            } catch (Exception e) {
                log.debug("battlemaster_entry load skipped: {}", e.getMessage());
            }
            try {
                loadTrainers(c);
            } catch (Exception e) {
                log.debug("npc_trainer load skipped: {}", e.getMessage());
            }
            try {
                loadTrainerMeta(c);
            } catch (Exception e) {
                log.debug("trainer meta load skipped: {}", e.getMessage());
            }
            try {
                loadSpellChains(c);
            } catch (Exception e) {
                log.debug("spell_chain load skipped: {}", e.getMessage());
            }
            try {
                loadSpellBaseLevels(c);
            } catch (Exception e) {
                log.debug("spell BaseLevel load skipped: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("ObjectMgr SQL load failed, using defaults: {}", e.getMessage());
            seedDefaults();
        }
        if (createInfo.isEmpty()) {
            seedDefaults();
        }
        if (createActions.isEmpty()) {
            seedCreateActions();
        }
        // SQL item rows may lack spell columns until loadItemSpells; always force usable seeds.
        mergeUsableItemSpells(ItemTemplate.hearthstone());
        mergeUsableItemSpells(ItemTemplate.toughJerky());
        mergeUsableItemSpells(ItemTemplate.refreshingSpringWater());
        seedQueryDefaults();
        loadStartOutfit(dataDir);
        loadTalents(dataDir);
        levelStats.loadGt(dataDir);
        DurabilityCosts.load(dataDir);
        skillLineAbilities.loadFromDataDir(dataDir);
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
                    "SELECT race, `class`, button, action, `type` FROM playercreateinfo_action");
            ResultSet ar = ac.executeQuery();
            while (ar.next()) {
                putCreateAction(ar.getInt(1), ar.getInt(2), ar.getInt(3), ar.getInt(4), ar.getInt(5));
            }
            log.info("loaded {} playercreateinfo_action race/class keys", createActions.size());
        } catch (Exception e) {
            log.warn("playercreateinfo_action load failed: {}", e.getMessage());
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

    /** CMaNGOS ObjectMgr::LoadTrainers — separate entry vs template maps (ids may collide). */
    private void loadTrainers(Connection c) throws Exception {
        loadTrainerTable(c, "npc_trainer", trainerSpells);
        loadTrainerTable(c, "npc_trainer_template", trainerTemplateSpells);
        log.info("loaded trainer spell lists for {} creature entries and {} templates",
                trainerSpells.size(), trainerTemplateSpells.size());
    }

    private void loadTrainerTable(Connection c, String table, Map<Integer, List<TrainerSpell>> into) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT entry, spell, spellcost, reqskill, reqskillvalue, reqlevel, "
                        + "ReqAbility1, ReqAbility2, ReqAbility3 FROM " + table);
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int entry = rs.getInt(1);
            int spell = rs.getInt(2);
            if (spell <= 0) {
                continue;
            }
            int cost = rs.getInt(3);
            int reqSkill = rs.getInt(4);
            int reqSkillValue = rs.getInt(5);
            int reqLevel = rs.getInt(6);
            int a0 = rs.getObject(7) == null ? 0 : rs.getInt(7);
            int a1 = rs.getObject(8) == null ? 0 : rs.getInt(8);
            int a2 = rs.getObject(9) == null ? 0 : rs.getInt(9);
            boolean firstProf = spell == Content.SPELL_APPRENTICE_BLACKSMITH;
            TrainerSpell row = new TrainerSpell(spell, cost, reqLevel, reqSkill, reqSkillValue, a0, a1, a2, firstProf);
            into.computeIfAbsent(entry, k -> new ArrayList<>()).add(row);
            n++;
        }
        log.info("loaded {} rows from {}", n, table);
    }

    /** creature_template TrainerType / TrainerClass / TrainerTemplateId (CMaNGOS Creature::IsTrainerOf). */
    private void loadTrainerMeta(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT Entry, TrainerType, TrainerClass, TrainerTemplateId FROM creature_template");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int entry = rs.getInt(1);
            trainerTypeByEntry.put(entry, rs.getInt(2));
            int clazz = rs.getInt(3);
            if (clazz != 0) {
                trainerClass.put(entry, clazz);
            }
            int tmpl = rs.getInt(4);
            if (tmpl != 0) {
                trainerTemplateId.put(entry, tmpl);
            }
            n++;
        }
        log.info("loaded trainer meta for {} creature_template rows", n);
    }

    /** CMaNGOS SpellMgr::LoadSpellChains. */
    private void loadSpellChains(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT spell_id, prev_spell, first_spell, `rank`, req_spell FROM spell_chain");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int id = rs.getInt(1);
            spellChain.put(id, new SpellChainNode(id, rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)));
            n++;
        }
        log.info("loaded {} spell_chain rows", n);
    }

    /** spell_template.BaseLevel for trainer reqLevel fallback when SQL reqlevel is 0. */
    private void loadSpellBaseLevels(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement("SELECT Id, BaseLevel FROM spell_template WHERE BaseLevel > 0");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            spellBaseLevel.put(rs.getInt(1), rs.getInt(2));
            n++;
        }
        log.info("loaded BaseLevel for {} spells", n);
    }

    /**
     * CMaNGOS SendTrainerList — npc_trainer rows for the entry plus npc_trainer_template via TrainerTemplateId.
     */
    public List<TrainerSpell> spellsForTrainer(int entry) {
        List<TrainerSpell> direct = trainerSpells.getOrDefault(entry, List.of());
        int tmpl = trainerTemplateId.getOrDefault(entry, 0);
        List<TrainerSpell> fromTemplate = tmpl == 0
                ? List.of()
                : trainerTemplateSpells.getOrDefault(tmpl, List.of());
        if (direct.isEmpty()) {
            return fromTemplate;
        }
        if (fromTemplate.isEmpty()) {
            return direct;
        }
        List<TrainerSpell> merged = new ArrayList<>(direct.size() + fromTemplate.size());
        merged.addAll(direct);
        merged.addAll(fromTemplate);
        return merged;
    }

    public int trainerType(int entry) {
        Integer fromMeta = trainerTypeByEntry.get(entry);
        if (fromMeta != null) {
            return fromMeta;
        }
        CreatureTemplate t = creatures.get(entry);
        return t == null ? 0 : t.trainerType();
    }

    private void loadCreatures(Connection c) {
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, DisplayId1, DisplayId2, DisplayId3, DisplayId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold, InhabitType "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, ModelId1, ModelId2, ModelId3, ModelId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold, InhabitType "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, ModelId1, ModelId2, ModelId3, ModelId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, DisplayId1, DisplayId2, DisplayId3, DisplayId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, ModelId1, ModelId2, ModelId3, ModelId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template",
                true, false)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, DisplayId1, DisplayId2, DisplayId3, DisplayId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template",
                true, false)) {
            log.info("loaded {} creature_template rows", creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT Entry, Name, ModelId1, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template")) {
            log.info("loaded {} creature_template rows (simple)", creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT Entry, Name, DisplayId1, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template")) {
            log.info("loaded {} creature_template rows (simple)", creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT entry, name, modelid_1, faction_A, minhealth, minlevel, npcflag, ScriptName "
                        + "FROM creature_template")) {
            log.info("loaded {} creature_template rows (trinity)", creatures.size());
            return;
        }
        log.warn("creature_template column mismatch, using seed templates");
    }

    /** creature_template MinLevelMana → {@link #creatureMana} (CMaNGOS SelectLevel). */
    private void loadCreatureMana(Connection c) {
        String[] sqls = {
                "SELECT Entry, MinLevelMana FROM creature_template WHERE MinLevelMana > 0",
                "SELECT entry, minmana FROM creature_template WHERE minmana > 0",
                "SELECT Entry, MinLevelMana FROM creature_template WHERE MinLevelMana > 0"
        };
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                int n = 0;
                while (rs.next()) {
                    int mana = rs.getInt(2);
                    if (mana > 0) {
                        creatureMana.put(rs.getInt(1), mana);
                        n++;
                    }
                }
                log.info("loaded MinLevelMana for {} creatures", n);
                return;
            } catch (Exception e) {
                log.debug("creature mana query skipped: {}", e.getMessage());
            }
        }
    }

    private boolean loadCreaturesSql(Connection c, String sql, boolean full, boolean combat) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int entry = rs.getInt(1);
                String name = nz(rs.getString(2));
                if (full && combat) {
                    int inhabit = inhabitTypeOrDefault(rs);
                    creatures.put(entry, new CreatureTemplate(
                            entry, name, rs.getInt(5), rs.getInt(17), Math.max(1, rs.getInt(18)), rs.getInt(19),
                            rs.getInt(20), nz(rs.getString(21)), "", 0,
                            nz(rs.getString(3)), nz(rs.getString(4)), rs.getInt(6), rs.getInt(7), rs.getInt(8),
                            rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13),
                            rs.getFloat(14), rs.getFloat(15), rs.getInt(16),
                            nz(rs.getString(22)), rs.getInt(23), rs.getFloat(24), rs.getFloat(25),
                            Math.max(1, rs.getInt(26)), 0f, rs.getInt(27), rs.getInt(28), rs.getInt(29), inhabit));
                } else if (full) {
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

    private static int inhabitTypeOrDefault(ResultSet rs) {
        try {
            int v = rs.getInt("InhabitType");
            if (rs.wasNull() || v <= 0) {
                return org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT;
            }
            return v;
        } catch (Exception e) {
            return org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT;
        }
    }

    private void loadModelInfo(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT modelid, combat_reach FROM creature_model_info");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelCombatReach.put(rs.getInt(1), rs.getFloat(2));
            }
            log.info("loaded {} creature_model_info rows", modelCombatReach.size());
        } catch (Exception e) {
            log.debug("creature_model_info load skipped: {}", e.getMessage());
        }
    }

    private void loadCreatureLoot(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, item, ChanceOrQuestChance, mincountOrRef, maxcount FROM creature_loot_template");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int minCount = rs.getInt(4);
                if (minCount < 0) {
                    // Reference rows are not expanded yet.
                    continue;
                }
                float chanceRaw = rs.getFloat(3);
                boolean needsQuest = chanceRaw < 0f;
                float chance = Math.abs(chanceRaw);
                creatureLoot.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>())
                        .add(new LootRow(rs.getInt(2), chance, minCount, Math.max(minCount, rs.getInt(5)), needsQuest));
                n++;
            }
            log.info("loaded {} creature_loot_template rows", n);
        } catch (Exception e) {
            log.debug("creature_loot_template load skipped: {}", e.getMessage());
        }
    }

    private void loadGameObjectLoot(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, item, ChanceOrQuestChance, mincountOrRef, maxcount FROM gameobject_loot_template");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int minCount = rs.getInt(4);
                if (minCount < 0) {
                    continue;
                }
                float chanceRaw = rs.getFloat(3);
                boolean needsQuest = chanceRaw < 0f;
                float chance = Math.abs(chanceRaw);
                gameObjectLoot.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>())
                        .add(new LootRow(rs.getInt(2), chance, minCount, Math.max(minCount, rs.getInt(5)), needsQuest));
                n++;
            }
            log.info("loaded {} gameobject_loot_template rows", n);
        } catch (Exception e) {
            log.debug("gameobject_loot_template load skipped: {}", e.getMessage());
        }
    }

    private void loadNpcVendors(Connection c) {
        loadNpcVendorTable(c, "npc_vendor", vendorItems);
        loadNpcVendorTable(c, "npc_vendor_template", vendorTemplateItems);
    }

    private void loadNpcVendorTable(Connection c, String table, Map<Integer, List<Integer>> into) {
        if (loadNpcVendorQuery(c, "SELECT entry, item FROM " + table + " ORDER BY slot, item", into)) {
            return;
        }
        loadNpcVendorQuery(c, "SELECT entry, item FROM " + table, into);
    }

    private boolean loadNpcVendorQuery(Connection c, String sql, Map<Integer, List<Integer>> into) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int item = rs.getInt(2);
                if (item <= 0) {
                    continue;
                }
                List<Integer> stock = into.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>());
                if (!stock.contains(item)) {
                    stock.add(item);
                }
                n++;
            }
            log.info("loaded {} rows via {}", n, sql.contains("template") ? "npc_vendor_template" : "npc_vendor");
            return true;
        } catch (Exception e) {
            log.debug("vendor load skipped ({}): {}", sql, e.getMessage());
            return false;
        }
    }

    /** creature_template.VendorTemplateId (CMaNGOS Creature::GetVendorTemplateItems). */
    private void loadVendorMeta(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement("SELECT Entry, VendorTemplateId FROM creature_template");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int entry = rs.getInt(1);
            int tmpl = rs.getInt(2);
            if (tmpl != 0) {
                vendorTemplateId.put(entry, tmpl);
                n++;
            }
        }
        log.info("loaded VendorTemplateId for {} creatures", n);
    }

    /**
     * CMaNGOS SendListInventory — npc_vendor for the entry plus npc_vendor_template via VendorTemplateId.
     */
    public List<Integer> itemsForVendor(int entry) {
        List<Integer> direct = vendorItems.getOrDefault(entry, List.of());
        int tmpl = vendorTemplateId.getOrDefault(entry, 0);
        List<Integer> fromTemplate = tmpl == 0
                ? List.of()
                : vendorTemplateItems.getOrDefault(tmpl, List.of());
        if (direct.isEmpty()) {
            return fromTemplate;
        }
        if (fromTemplate.isEmpty()) {
            return direct;
        }
        List<Integer> merged = new ArrayList<>(direct.size() + fromTemplate.size());
        for (int item : direct) {
            if (!merged.contains(item)) {
                merged.add(item);
            }
        }
        for (int item : fromTemplate) {
            if (!merged.contains(item)) {
                merged.add(item);
            }
        }
        return merged;
    }

    public boolean hasVendorStock(Creature c) {
        return c != null && !itemsForVendor(c.entry).isEmpty();
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

    /** Creature spawn SELECTs, first match wins. Event rows stay out via the join. */
    public static java.util.List<String> creatureSpawnQueries() {
        String cols = "c.guid, c.id, c.map, c.position_x, c.position_y, c.position_z, c.orientation";
        String motionCols = cols + ", c.spawndist, c.MovementType";
        String respawnCols = motionCols + ", c.spawntimesecsmin, c.spawntimesecsmax";
        String join = " FROM creature c LEFT OUTER JOIN game_event_creature gec ON c.guid = gec.guid AND gec.`event` > 0";
        return java.util.List.of(
                "SELECT " + respawnCols + join + " WHERE gec.guid IS NULL",
                "SELECT " + motionCols + join + " WHERE gec.guid IS NULL",
                "SELECT " + cols + join + " WHERE gec.guid IS NULL",
                "SELECT guid, id, map, position_x, position_y, position_z, orientation FROM creature");
    }

    /** Gameobject spawn SELECTs, first match wins. Event rows stay out via the join. */
    public static java.util.List<String> gameObjectSpawnQueries() {
        String cols = "g.guid, g.id, g.map, g.position_x, g.position_y, g.position_z, g.orientation";
        String join = " FROM gameobject g LEFT OUTER JOIN game_event_gameobject geg ON g.guid = geg.guid AND geg.`event` > 0";
        return java.util.List.of(
                "SELECT " + cols + join + " WHERE geg.guid IS NULL",
                "SELECT guid, id, map, position_x, position_y, position_z, orientation FROM gameobject");
    }

    private void loadSpawns(Connection c) throws Exception {
        String[] sqls = creatureSpawnQueries().toArray(String[]::new);
        Exception last = null;
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean motion = sql.contains("spawndist");
                boolean respawn = sql.contains("spawntimesecsmin");
                while (rs.next()) {
                    float spawnDist = motion ? rs.getFloat(8) : 0f;
                    int movementType = motion ? rs.getInt(9) : 0;
                    int respawnMin = respawn ? rs.getInt(10) : Spawn.DEFAULT_RESPAWN_SECS;
                    int respawnMax = respawn ? rs.getInt(11) : Spawn.DEFAULT_RESPAWN_SECS;
                    spawns.add(new Spawn(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getFloat(4), rs.getFloat(5), rs.getFloat(6), rs.getFloat(7),
                            spawnDist, movementType, respawnMin, respawnMax));
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

    private void loadGoSpawns(Connection c) throws Exception {
        String[] sqls = gameObjectSpawnQueries().toArray(String[]::new);
        Exception last = null;
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    goSpawns.add(new Spawn(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getFloat(4), rs.getFloat(5), rs.getFloat(6), rs.getFloat(7)));
                }
                log.info("loaded {} gameobject spawns", goSpawns.size());
                return;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    private void loadEventCreatures(Connection c) throws Exception {
        String sql = "SELECT gec.`event`, c.guid, c.id, c.map, c.position_x, c.position_y, c.position_z, c.orientation "
                + "FROM game_event_creature gec INNER JOIN creature c ON c.guid = gec.guid";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int eventId = rs.getInt(1);
                if (eventId <= 0) {
                    continue;
                }
                eventCreatures.computeIfAbsent(eventId, k -> new ArrayList<>()).add(
                        new Spawn(rs.getInt(2), rs.getInt(3), rs.getInt(4),
                                rs.getFloat(5), rs.getFloat(6), rs.getFloat(7), rs.getFloat(8)));
            }
            log.info("loaded game_event_creature for {} events", eventCreatures.size());
        }
    }

    private void loadEventGameObjects(Connection c) throws Exception {
        String sql = "SELECT geg.`event`, g.guid, g.id, g.map, g.position_x, g.position_y, g.position_z, g.orientation "
                + "FROM game_event_gameobject geg INNER JOIN gameobject g ON g.guid = geg.guid";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int eventId = rs.getInt(1);
                if (eventId <= 0) {
                    continue;
                }
                eventGameObjects.computeIfAbsent(eventId, k -> new ArrayList<>()).add(
                        new Spawn(rs.getInt(2), rs.getInt(3), rs.getInt(4),
                                rs.getFloat(5), rs.getFloat(6), rs.getFloat(7), rs.getFloat(8)));
            }
            log.info("loaded game_event_gameobject for {} events", eventGameObjects.size());
        }
    }

    /** quest_template SELECTs, richest first. No row cap. */
    public static java.util.List<String> questTemplateQueries() {
        String full = "SELECT entry, Title, MinLevel, Type, Details, Objectives, "
                + "ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqCreatureOrGOId2, ReqCreatureOrGOCount2, "
                + "ReqCreatureOrGOId3, ReqCreatureOrGOCount3, ReqCreatureOrGOId4, ReqCreatureOrGOCount4, "
                + "ReqItemId1, ReqItemCount1, ReqItemId2, ReqItemCount2, ReqItemId3, ReqItemCount3, "
                + "ReqItemId4, ReqItemCount4, QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1, "
                + "RewChoiceItemId1, RewChoiceItemCount1, RewChoiceItemId2, RewChoiceItemCount2, "
                + "PrevQuestId, RewOrReqMoney, RequiredRaces, ZoneOrSort FROM quest_template";
        return java.util.List.of(
                full,
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1, "
                        + "RewChoiceItemId1, RewChoiceItemCount1, RewChoiceItemId2, RewChoiceItemCount2 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1, "
                        + "RewChoiceItemId1, RewChoiceItemCount1, RewChoiceItemId2, RewChoiceItemCount2 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type FROM quest_template");
    }

    /** Starter and turn-in links. Index 0 offers, index 1 finishes. */
    public static java.util.List<String> questRelationQueries() {
        return java.util.List.of(
                "SELECT id, quest FROM creature_questrelation",
                "SELECT id, quest FROM creature_involvedrelation");
    }

    private void loadQuests(Connection c) {
        for (String sql : questTemplateQueries()) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    if (cols >= 34) {
                        quests.put(rs.getInt("entry"), fullQuest(rs));
                    } else {
                        int reqId = cols >= 6 ? rs.getInt(5) : 0;
                        int reqCount = cols >= 6 ? rs.getInt(6) : 0;
                        int itemId = cols >= 8 ? rs.getInt(7) : 0;
                        int itemCount = cols >= 8 ? rs.getInt(8) : 0;
                        int qLevel = cols >= 12 ? rs.getInt(9) : 0;
                        int maxMoney = cols >= 12 ? rs.getInt(10) : 0;
                        int rewItem = cols >= 12 ? rs.getInt(11) : 0;
                        int rewCount = cols >= 12 ? rs.getInt(12) : 0;
                        int choiceId1 = cols >= 16 ? rs.getInt(13) : 0;
                        int choiceCount1 = cols >= 16 ? rs.getInt(14) : 0;
                        int choiceId2 = cols >= 16 ? rs.getInt(15) : 0;
                        int choiceCount2 = cols >= 16 ? rs.getInt(16) : 0;
                        quests.put(rs.getInt(1), new QuestTemplate(rs.getInt(1), nz(rs.getString(2)),
                                rs.getInt(3), rs.getInt(4), 0, "", "", reqId, reqCount, itemId, itemCount,
                                qLevel, maxMoney, rewItem, rewCount, choiceId1, choiceCount1, choiceId2, choiceCount2));
                    }
                }
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private static QuestTemplate fullQuest(ResultSet rs) throws Exception {
        return new QuestTemplate(rs.getInt("entry"), nz(rs.getString("Title")), rs.getInt("MinLevel"), rs.getInt("Type"),
                rs.getInt("RewOrReqMoney"), nz(rs.getString("Details")), nz(rs.getString("Objectives")),
                rs.getInt("ReqCreatureOrGOId1"), rs.getInt("ReqCreatureOrGOCount1"),
                rs.getInt("ReqItemId1"), rs.getInt("ReqItemCount1"),
                rs.getInt("QuestLevel"), rs.getInt("RewMoneyMaxLevel"),
                rs.getInt("RewItemId1"), rs.getInt("RewItemCount1"),
                rs.getInt("RewChoiceItemId1"), rs.getInt("RewChoiceItemCount1"),
                rs.getInt("RewChoiceItemId2"), rs.getInt("RewChoiceItemCount2"),
                rs.getInt("ReqCreatureOrGOId2"), rs.getInt("ReqCreatureOrGOCount2"),
                rs.getInt("ReqCreatureOrGOId3"), rs.getInt("ReqCreatureOrGOCount3"),
                rs.getInt("ReqCreatureOrGOId4"), rs.getInt("ReqCreatureOrGOCount4"),
                rs.getInt("ReqItemId2"), rs.getInt("ReqItemCount2"),
                rs.getInt("ReqItemId3"), rs.getInt("ReqItemCount3"),
                rs.getInt("ReqItemId4"), rs.getInt("ReqItemCount4"),
                rs.getInt("PrevQuestId"), rs.getInt("RequiredRaces"), rs.getInt("ZoneOrSort"));
    }

    private void loadQuestRelations(Connection c) {
        java.util.List<String> sqls = questRelationQueries();
        loadOneRelation(c, sqls.get(0), questGivers);
        loadOneRelation(c, sqls.get(1), questInvolved);
        loadOneRelation(c, "SELECT id, quest FROM gameobject_questrelation", goQuestGivers);
        loadOneRelation(c, "SELECT id, quest FROM gameobject_involvedrelation", goQuestInvolved);
        loadAreaTriggerQuests(c);
    }

    private void loadAreaTriggerQuests(Connection c) {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, quest FROM areatrigger_involvedrelation");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                areaTriggerQuests.put(rs.getInt(1), rs.getInt(2));
            }
        } catch (Exception ignored) {
        }
    }

    private void loadQuestExtras(Connection c) {
        String sql = "SELECT entry, ReqSpellCast1, SpecialFlags, QuestFlags, LimitTime, PointMapId, PointX, PointY, "
                + "RequiredMinRepFaction, RequiredMinRepValue, RewRepFaction1, RewRepValue1 FROM quest_template";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                questExtras.put(rs.getInt(1), new QuestExtras(
                        rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6),
                        rs.getFloat(7), rs.getFloat(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12)));
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadOneRelation(Connection c, String sql, Map<Integer, List<Integer>> dest) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                addQuestRelation(dest, rs.getInt(1), rs.getInt(2));
            }
        } catch (Exception ignored) {
        }
    }

    static void addQuestRelation(Map<Integer, List<Integer>> dest, int entry, int questId) {
        List<Integer> list = dest.computeIfAbsent(entry, k -> new ArrayList<>());
        if (!list.contains(questId)) {
            list.add(questId);
        }
    }

    private void mergeUsableItemSpells(ItemTemplate seed) {
        ItemTemplate t = items.get(seed.entry);
        if (t == null) {
            items.put(seed.entry, seed);
            return;
        }
        for (int i = 0; i < 5; i++) {
            if (seed.spellId[i] != 0) {
                t.spellId[i] = seed.spellId[i];
                t.spellTrigger[i] = seed.spellTrigger[i];
                t.spellCharges[i] = seed.spellCharges[i];
            }
        }
    }

    /**
     * item_template spellid_1..5 / spelltrigger_1..5 / spellcharges_1..5 — not in the base SELECT.
     * Merges onto templates already loaded by {@link #loadItems}.
     */
    private void loadItemSpells(Connection c) {
        String sql = "SELECT entry, spellid_1, spelltrigger_1, spellcharges_1, "
                + "spellid_2, spelltrigger_2, spellcharges_2, "
                + "spellid_3, spelltrigger_3, spellcharges_3, "
                + "spellid_4, spelltrigger_4, spellcharges_4, "
                + "spellid_5, spelltrigger_5, spellcharges_5 FROM item_template";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemTemplate t = items.get(rs.getInt(1));
                if (t == null) {
                    continue;
                }
                for (int i = 0; i < 5; i++) {
                    int base = 2 + i * 3;
                    t.spellId[i] = rs.getInt(base);
                    t.spellTrigger[i] = rs.getInt(base + 1);
                    t.spellCharges[i] = rs.getInt(base + 2);
                }
            }
        } catch (Exception e) {
            log.warn("item_template spell columns load failed: {}", e.getMessage());
        }
    }

    /**
     * item_template.sheath — not in the base SELECT. Client uses this for sheathed
     * attachment points (back/hip/shield); 0 = SHEATHETYPE_NONE (models despawn).
     */
    private void loadItemSheath(Connection c) {
        String sql = "SELECT entry, sheath FROM item_template";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemTemplate t = items.get(rs.getInt(1));
                if (t == null) {
                    continue;
                }
                t.sheath = rs.getInt(2);
            }
        } catch (Exception e) {
            log.warn("item_template sheath load failed: {}", e.getMessage());
        }
    }

    private void loadItems(Connection c) {
        String[] sqls = {
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block`, dmg_min3, dmg_max3, dmg_type3, dmg_min4, dmg_max4, dmg_type4, "
                        + "dmg_min5, dmg_max5, dmg_type5, stat_type8, stat_value8, stat_type9, stat_value9, "
                        + "stat_type10, stat_value10 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block`, dmg_min3, dmg_max3, dmg_type3, dmg_min4, dmg_max4, dmg_type4, "
                        + "dmg_min5, dmg_max5, dmg_type5 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block`, dmg_min3, dmg_max3, dmg_type3 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7, "
                        + "`block` FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6, stat_type7, stat_value7 "
                        + "FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2, stat_type6, stat_value6 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res, "
                        + "dmg_min2, dmg_max2, dmg_type2 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res, arcane_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res, shadow_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res, frost_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res, nature_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5, fire_res FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4, "
                        + "stat_type5, stat_value5 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3, stat_type4, stat_value4 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2, stat_type3, stat_value3 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1, "
                        + "stat_type2, stat_value2 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill, dmg_min1, dmg_max1, stat_type1, stat_value1 FROM item_template LIMIT 50000",
                "SELECT entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice, "
                        + "InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount, stackable, "
                        + "ContainerSlots, armor, delay, bonding, description, MaxDurability, Duration, "
                        + "RequiredDisenchantSkill FROM item_template LIMIT 50000"
        };
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
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
                    if (cols >= 28) {
                        t.dmgMin[0] = rs.getFloat(25);
                        t.dmgMax[0] = rs.getFloat(26);
                        t.statType[0] = rs.getInt(27);
                        t.statValue[0] = rs.getInt(28);
                    }
                    if (cols >= 30) {
                        t.statType[1] = rs.getInt(29);
                        t.statValue[1] = rs.getInt(30);
                    }
                    if (cols >= 32) {
                        t.statType[2] = rs.getInt(31);
                        t.statValue[2] = rs.getInt(32);
                    }
                    if (cols >= 34) {
                        t.statType[3] = rs.getInt(33);
                        t.statValue[3] = rs.getInt(34);
                    }
                    if (cols >= 36) {
                        t.statType[4] = rs.getInt(35);
                        t.statValue[4] = rs.getInt(36);
                    }
                    if (cols >= 37) {
                        t.fireRes = rs.getInt(37);
                    }
                    if (cols >= 38) {
                        t.natureRes = rs.getInt(38);
                    }
                    if (cols >= 39) {
                        t.frostRes = rs.getInt(39);
                    }
                    if (cols >= 40) {
                        t.shadowRes = rs.getInt(40);
                    }
                    if (cols >= 41) {
                        t.arcaneRes = rs.getInt(41);
                    }
                    if (cols >= 44) {
                        t.dmgMin[1] = rs.getFloat(42);
                        t.dmgMax[1] = rs.getFloat(43);
                        t.dmgType[1] = rs.getInt(44);
                    }
                    if (cols >= 46) {
                        t.statType[5] = rs.getInt(45);
                        t.statValue[5] = rs.getInt(46);
                    }
                    if (cols >= 48) {
                        t.statType[6] = rs.getInt(47);
                        t.statValue[6] = rs.getInt(48);
                    }
                    if (cols >= 49) {
                        t.block = rs.getInt(49);
                    }
                    if (cols >= 52) {
                        t.dmgMin[2] = rs.getFloat(50);
                        t.dmgMax[2] = rs.getFloat(51);
                        t.dmgType[2] = rs.getInt(52);
                    }
                    if (cols >= 55) {
                        t.dmgMin[3] = rs.getFloat(53);
                        t.dmgMax[3] = rs.getFloat(54);
                        t.dmgType[3] = rs.getInt(55);
                    }
                    if (cols >= 58) {
                        t.dmgMin[4] = rs.getFloat(56);
                        t.dmgMax[4] = rs.getFloat(57);
                        t.dmgType[4] = rs.getInt(58);
                    }
                    if (cols >= 60) {
                        t.statType[7] = rs.getInt(59);
                        t.statValue[7] = rs.getInt(60);
                    }
                    if (cols >= 62) {
                        t.statType[8] = rs.getInt(61);
                        t.statValue[8] = rs.getInt(62);
                    }
                    if (cols >= 64) {
                        t.statType[9] = rs.getInt(63);
                        t.statValue[9] = rs.getInt(64);
                    }
                    items.put(t.entry, t);
                }
                return;
            } catch (Exception e) {
                log.debug("item_template load skipped: {}", e.getMessage());
            }
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

    /** BattleGroundMgr::LoadBattleMastersEntry — entry → BattleGroundTypeId. */
    private void loadBattleMasters(Connection c) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT entry, bg_template FROM battlemaster_entry");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                battleMasterBg.put(rs.getInt(1), rs.getInt(2));
            }
        }
    }

    /** MapDataContainer::GetBattleMasterBG — 0 if the entry is not a battlemaster. */
    public int battleMasterBgType(int creatureEntry) {
        return battleMasterBg.getOrDefault(creatureEntry, 0);
    }

    /** tbc-db playercreateinfo_action (race, class, button, action, type). */
    private void seedCreateActions() {
        int[][] rows = {
            {1, 1, 72, 6603, 0},
            {1, 1, 73, 78, 0},
            {1, 1, 83, 117, 128},
            {1, 2, 0, 6603, 0},
            {1, 2, 1, 20154, 0},
            {1, 2, 2, 635, 0},
            {1, 2, 10, 159, 128},
            {1, 2, 11, 2070, 128},
            {1, 4, 0, 6603, 0},
            {1, 4, 1, 1752, 0},
            {1, 4, 2, 2098, 0},
            {1, 4, 3, 2764, 0},
            {1, 4, 11, 2070, 128},
            {1, 5, 0, 6603, 0},
            {1, 5, 1, 585, 0},
            {1, 5, 2, 2050, 0},
            {1, 5, 10, 159, 128},
            {1, 5, 11, 2070, 128},
            {1, 8, 0, 6603, 0},
            {1, 8, 1, 133, 0},
            {1, 8, 2, 168, 0},
            {1, 8, 10, 159, 128},
            {1, 8, 11, 2070, 128},
            {1, 9, 0, 6603, 0},
            {1, 9, 1, 686, 0},
            {1, 9, 2, 687, 0},
            {1, 9, 10, 159, 128},
            {1, 9, 11, 4604, 128},
            {2, 1, 72, 6603, 0},
            {2, 1, 73, 78, 0},
            {2, 1, 83, 117, 128},
            {2, 3, 0, 6603, 0},
            {2, 3, 1, 2973, 0},
            {2, 3, 2, 75, 0},
            {2, 3, 10, 159, 128},
            {2, 3, 11, 117, 128},
            {2, 4, 0, 6603, 0},
            {2, 4, 1, 1752, 0},
            {2, 4, 2, 2098, 0},
            {2, 4, 11, 117, 128},
            {2, 7, 0, 6603, 0},
            {2, 7, 1, 403, 0},
            {2, 7, 2, 331, 0},
            {2, 7, 10, 159, 128},
            {2, 7, 11, 117, 128},
            {2, 9, 0, 6603, 0},
            {2, 9, 1, 686, 0},
            {2, 9, 2, 687, 0},
            {2, 9, 10, 159, 128},
            {2, 9, 11, 117, 128},
            {3, 1, 72, 6603, 0},
            {3, 1, 73, 78, 0},
            {3, 1, 83, 117, 128},
            {3, 2, 0, 6603, 0},
            {3, 2, 1, 20154, 0},
            {3, 2, 2, 635, 0},
            {3, 2, 10, 159, 128},
            {3, 2, 11, 4540, 128},
            {3, 3, 0, 6603, 0},
            {3, 3, 1, 2973, 0},
            {3, 3, 2, 75, 0},
            {3, 3, 10, 159, 128},
            {3, 3, 11, 117, 128},
            {3, 4, 0, 6603, 0},
            {3, 4, 1, 1752, 0},
            {3, 4, 2, 2098, 0},
            {3, 4, 3, 2764, 0},
            {3, 4, 11, 4540, 128},
            {3, 5, 0, 6603, 0},
            {3, 5, 1, 585, 0},
            {3, 5, 2, 2050, 0},
            {3, 5, 10, 159, 128},
            {3, 5, 11, 4540, 128},
            {4, 1, 72, 6603, 0},
            {4, 1, 73, 78, 0},
            {4, 1, 74, 20580, 0},
            {4, 1, 83, 117, 128},
            {4, 3, 0, 6603, 0},
            {4, 3, 1, 2973, 0},
            {4, 3, 2, 75, 0},
            {4, 3, 3, 20580, 0},
            {4, 3, 10, 159, 128},
            {4, 3, 11, 117, 128},
            {4, 4, 0, 6603, 0},
            {4, 4, 1, 1752, 0},
            {4, 4, 2, 2098, 0},
            {4, 4, 3, 2764, 0},
            {4, 4, 4, 20580, 0},
            {4, 4, 11, 4540, 128},
            {4, 5, 0, 6603, 0},
            {4, 5, 1, 585, 0},
            {4, 5, 2, 2050, 0},
            {4, 5, 3, 20580, 0},
            {4, 5, 10, 159, 128},
            {4, 5, 11, 2070, 128},
            {4, 11, 0, 6603, 0},
            {4, 11, 1, 5176, 0},
            {4, 11, 2, 5185, 0},
            {4, 11, 3, 20580, 0},
            {4, 11, 10, 159, 128},
            {4, 11, 11, 4536, 128},
            {5, 1, 72, 6603, 0},
            {5, 1, 73, 78, 0},
            {5, 1, 83, 4604, 128},
            {5, 4, 0, 6603, 0},
            {5, 4, 1, 1752, 0},
            {5, 4, 2, 2098, 0},
            {5, 4, 3, 2764, 0},
            {5, 4, 11, 4604, 128},
            {5, 5, 0, 6603, 0},
            {5, 5, 1, 585, 0},
            {5, 5, 2, 2050, 0},
            {5, 5, 10, 159, 128},
            {5, 5, 11, 4604, 128},
            {5, 8, 0, 6603, 0},
            {5, 8, 1, 133, 0},
            {5, 8, 2, 168, 0},
            {5, 8, 10, 159, 128},
            {5, 8, 11, 4604, 128},
            {5, 9, 0, 6603, 0},
            {5, 9, 1, 686, 0},
            {5, 9, 2, 687, 0},
            {5, 9, 10, 159, 128},
            {5, 9, 11, 4604, 128},
            {6, 1, 72, 6603, 0},
            {6, 1, 73, 78, 0},
            {6, 1, 74, 20549, 0},
            {6, 1, 83, 4540, 128},
            {6, 3, 0, 6603, 0},
            {6, 3, 1, 2973, 0},
            {6, 3, 2, 75, 0},
            {6, 3, 3, 20549, 0},
            {6, 3, 10, 159, 128},
            {6, 3, 11, 117, 128},
            {6, 7, 0, 6603, 0},
            {6, 7, 1, 403, 0},
            {6, 7, 2, 331, 0},
            {6, 7, 3, 20549, 0},
            {6, 7, 10, 159, 128},
            {6, 7, 11, 4604, 128},
            {6, 11, 0, 6603, 0},
            {6, 11, 1, 5176, 0},
            {6, 11, 2, 5185, 0},
            {6, 11, 3, 20549, 0},
            {6, 11, 10, 159, 128},
            {6, 11, 11, 4536, 128},
            {7, 1, 72, 6603, 0},
            {7, 1, 73, 78, 0},
            {7, 1, 83, 117, 128},
            {7, 4, 0, 6603, 0},
            {7, 4, 1, 1752, 0},
            {7, 4, 2, 2098, 0},
            {7, 4, 3, 2764, 0},
            {7, 4, 11, 117, 128},
            {7, 8, 0, 6603, 0},
            {7, 8, 1, 133, 0},
            {7, 8, 2, 168, 0},
            {7, 8, 10, 159, 128},
            {7, 8, 11, 4536, 128},
            {7, 9, 0, 6603, 0},
            {7, 9, 1, 686, 0},
            {7, 9, 2, 687, 0},
            {7, 9, 10, 159, 128},
            {7, 9, 11, 4604, 128},
            {8, 1, 72, 6603, 0},
            {8, 1, 73, 78, 0},
            {8, 1, 74, 2764, 0},
            {8, 1, 83, 117, 128},
            {8, 3, 0, 6603, 0},
            {8, 3, 1, 2973, 0},
            {8, 3, 2, 75, 0},
            {8, 3, 10, 159, 128},
            {8, 3, 11, 4604, 128},
            {8, 4, 0, 6603, 0},
            {8, 4, 1, 1752, 0},
            {8, 4, 2, 2098, 0},
            {8, 4, 3, 2764, 0},
            {8, 4, 11, 117, 128},
            {8, 5, 0, 6603, 0},
            {8, 5, 1, 585, 0},
            {8, 5, 2, 2050, 0},
            {8, 5, 10, 159, 128},
            {8, 5, 11, 4540, 128},
            {8, 7, 0, 6603, 0},
            {8, 7, 1, 403, 0},
            {8, 7, 2, 331, 0},
            {8, 7, 10, 159, 128},
            {8, 7, 11, 117, 128},
            {8, 8, 0, 6603, 0},
            {8, 8, 1, 133, 0},
            {8, 8, 2, 168, 0},
            {8, 8, 10, 159, 128},
            {8, 8, 11, 117, 128},
            {10, 2, 0, 6603, 0},
            {10, 2, 1, 20154, 0},
            {10, 2, 2, 635, 0},
            {10, 2, 3, 28734, 0},
            {10, 2, 4, 28730, 0},
            {10, 2, 10, 159, 128},
            {10, 2, 11, 20857, 128},
            {10, 3, 0, 6603, 0},
            {10, 3, 1, 2973, 0},
            {10, 3, 2, 75, 0},
            {10, 3, 3, 28734, 0},
            {10, 3, 4, 28730, 0},
            {10, 3, 10, 159, 128},
            {10, 3, 11, 20857, 128},
            {10, 4, 0, 6603, 0},
            {10, 4, 1, 1752, 0},
            {10, 4, 2, 2098, 0},
            {10, 4, 3, 2764, 0},
            {10, 4, 4, 28734, 0},
            {10, 4, 5, 25046, 0},
            {10, 4, 11, 20857, 128},
            {10, 5, 0, 6603, 0},
            {10, 5, 1, 585, 0},
            {10, 5, 2, 2050, 0},
            {10, 5, 3, 28734, 0},
            {10, 5, 4, 28730, 0},
            {10, 5, 10, 159, 128},
            {10, 5, 11, 20857, 128},
            {10, 8, 0, 6603, 0},
            {10, 8, 1, 133, 0},
            {10, 8, 2, 168, 0},
            {10, 8, 3, 28734, 0},
            {10, 8, 4, 28730, 0},
            {10, 8, 10, 159, 128},
            {10, 8, 11, 20857, 128},
            {10, 9, 0, 6603, 0},
            {10, 9, 1, 686, 0},
            {10, 9, 2, 687, 0},
            {10, 9, 3, 28734, 0},
            {10, 9, 4, 28730, 0},
            {10, 9, 10, 159, 128},
            {10, 9, 11, 20857, 128},
            {11, 1, 0, 6603, 0},
            {11, 1, 72, 6603, 0},
            {11, 1, 73, 78, 0},
            {11, 1, 74, 28880, 0},
            {11, 1, 83, 4540, 128},
            {11, 1, 84, 6603, 0},
            {11, 1, 96, 6603, 0},
            {11, 1, 108, 6603, 0},
            {11, 2, 0, 6603, 0},
            {11, 2, 1, 20154, 0},
            {11, 2, 2, 635, 0},
            {11, 2, 3, 28880, 0},
            {11, 2, 10, 159, 128},
            {11, 2, 11, 4540, 128},
            {11, 2, 83, 4540, 128},
            {11, 3, 0, 6603, 0},
            {11, 3, 1, 2973, 0},
            {11, 3, 2, 75, 0},
            {11, 3, 3, 28880, 0},
            {11, 3, 10, 159, 128},
            {11, 3, 11, 4540, 128},
            {11, 3, 72, 6603, 0},
            {11, 3, 73, 2973, 0},
            {11, 3, 74, 75, 0},
            {11, 3, 82, 159, 128},
            {11, 3, 83, 4540, 128},
            {11, 5, 0, 6603, 0},
            {11, 5, 1, 585, 0},
            {11, 5, 2, 2050, 0},
            {11, 5, 3, 28880, 0},
            {11, 5, 10, 159, 128},
            {11, 5, 11, 4540, 128},
            {11, 5, 83, 4540, 128},
            {11, 7, 0, 6603, 0},
            {11, 7, 1, 403, 0},
            {11, 7, 2, 331, 0},
            {11, 7, 3, 28880, 0},
            {11, 7, 10, 159, 128},
            {11, 7, 11, 4540, 128},
            {11, 8, 0, 6603, 0},
            {11, 8, 1, 133, 0},
            {11, 8, 2, 168, 0},
            {11, 8, 3, 28880, 0},
            {11, 8, 10, 159, 128},
            {11, 8, 11, 4540, 128},
            {11, 8, 83, 4540, 128}
        };
        for (int[] r : rows) {
            putCreateAction(r[0], r[1], r[2], r[3], r[4]);
        }
    }

    private void putCreateAction(int race, int clazz, int button, int action, int type) {
        int[] buttons = createActions.computeIfAbsent((int) key(race, clazz), x -> new int[132]);
        if (button >= 0 && button < 132) {
            buttons[button] = (action & 0xFFFFFF) | ((type & 0xFF) << 24);
        }
    }

    /** Append Hero Battlecaster ranks when SQL loaded the trainer without them. */
    private void ensureBattlecasterOnTrainer(int entry) {
        List<TrainerSpell> list = trainerSpells.computeIfAbsent(entry, e -> new ArrayList<>());
        appendBattlecasterIfMissing(list, org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_LEATHER);
        appendBattlecasterIfMissing(list, org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_MAIL);
        appendBattlecasterIfMissing(list, org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_PLATE);
    }

    private static void appendBattlecasterIfMissing(List<TrainerSpell> list, int spell, int reqLevel) {
        for (TrainerSpell t : list) {
            if (t.spell() == spell) {
                return;
            }
        }
        list.add(new TrainerSpell(spell, org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER, reqLevel));
    }

    private void seedDefaults() {
        levelStats.seedDefaults();
        seedCreateActions();
        createInfo.put(key(1, 1), new CreateInfo(1, 1, 0, 12, -8949.95f, -132.493f, 83.5312f, 0f));
        createInfo.put(key(2, 1), new CreateInfo(2, 1, 1, 14, -618.518f, -4251.67f, 38.718f, 0f));
        // Night Elf warrior — Shadowglen / Teldrassil (classless createForRace).
        createInfo.put(key(4, 1), new CreateInfo(4, 1, 1, 141, 10311.3f, 831.463f, 1326.41f, 0f));
        // Blood Elf has no warrior; mage coords = Sunstrider (classless createForRace).
        createInfo.put(key(10, 8), new CreateInfo(10, 8, 530, 3431, 10349.6f, -6357.29f, 33.4026f, 0f));
        // Draenei warrior — Ammen Vale (classless / GY wire tests).
        createInfo.put(key(11, 1), new CreateInfo(11, 1, 530, 3526, -3961.64f, -13931.2f, 100.615f, 2.08364f));
        // Undead / Tauren starters (GY void-prevention wire tests).
        createInfo.put(key(5, 1), new CreateInfo(5, 1, 0, 85, 1676.35f, 1677.45f, 121.67f, 2.70526f));
        createInfo.put(key(6, 1), new CreateInfo(6, 1, 1, 215, -2917.58f, -257.98f, 52.9968f, 0f));
        createSpells.put((int) key(1, 1), new ArrayList<>(List.of(6603, 78, 81, 107, 196, 203, 204, 522, 668, 2382, 2457, 2479, 3050, 3365, 6233, 6246, 6247, 6477, 6478, 7266, 7267, 7355, 8386, 9078, 9125, 20597, 20598, 20599, 20864, 21651, 21652, 22027, 22810)));
        creatures.put(6, seedKoboldVermin());
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
        trainerTypeByEntry.put(Content.NPC_LLANE_BESHERE, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.put(Content.NPC_LLANE_BESHERE, 1);
        trainerSpells.put(Content.NPC_LLANE_BESHERE, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT, Content.TRAINER_SPELL_BATTLE_SHOUT_COST, 1),
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT_RANK2, 500, 12, 0, 0,
                        Content.SPELL_BATTLE_SHOUT, 0, 0, false),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_LEATHER),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_MAIL),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_PLATE))));
        spellChain.putIfAbsent(Content.SPELL_BATTLE_SHOUT_RANK2,
                new SpellChainNode(Content.SPELL_BATTLE_SHOUT_RANK2, Content.SPELL_BATTLE_SHOUT,
                        Content.SPELL_BATTLE_SHOUT, 2, 0));
        spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER, 2, 0));
        spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL, 3, 0));
        creatures.put(Content.NPC_KHELDEN_BREMEN, new CreatureTemplate(Content.NPC_KHELDEN_BREMEN, "Khelden Bremen", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        trainerTypeByEntry.put(Content.NPC_KHELDEN_BREMEN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.put(Content.NPC_KHELDEN_BREMEN, Player.CLASS_MAGE);
        trainerSpells.put(Content.NPC_KHELDEN_BREMEN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_FIREBALL, Content.TRAINER_SPELL_FIREBALL_COST, 1))));
        creatures.put(Content.NPC_DANE_LINDGREN, new CreatureTemplate(Content.NPC_DANE_LINDGREN, "Dane Lindgren", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER, "", "",
                org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS));
        trainerTypeByEntry.put(Content.NPC_DANE_LINDGREN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS);
        trainerSpells.put(Content.NPC_DANE_LINDGREN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_APPRENTICE_BLACKSMITH, Content.TRAINER_SPELL_APPRENTICE_BLACKSMITH_COST, 1,
                        0, 0, 0, 0, 0, true))));
        creatures.put(Content.NPC_DUNGAR_LONGDRINK, new CreatureTemplate(Content.NPC_DUNGAR_LONGDRINK, "Dungar Longdrink", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_FLIGHTMASTER, "", "", 0));
        creatures.put(Content.NPC_INNKEEPER_FARLEY, new CreatureTemplate(Content.NPC_INNKEEPER_FARLEY, "Innkeeper Farley", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_INNKEEPER, "", "", 0));
        creatures.put(Content.NPC_AUCTIONEER_CHILTON, new CreatureTemplate(Content.NPC_AUCTIONEER_CHILTON, "Auctioneer Chilton", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_AUCTIONEER, "", "", 0));
        creatures.put(Content.NPC_OLIVIA_BURNSIDE, new CreatureTemplate(Content.NPC_OLIVIA_BURNSIDE, "Olivia Burnside", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_BANKER, "", "", 0));
        creatures.put(Content.NPC_REBECCA_LAUGHLIN, new CreatureTemplate(Content.NPC_REBECCA_LAUGHLIN, "Rebecca Laughlin", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_PETITIONER | Content.UNIT_NPC_FLAG_TABARDDESIGNER, "", "", 0));
        creatures.put(Content.NPC_LUMA_SKYMOTHER, new CreatureTemplate(Content.NPC_LUMA_SKYMOTHER, "Luma Skymother", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP, "", "", 0));
        auctions.add(new Auction(1, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 43_200_000, "Worn Shortsword"));
        taxiPaths.put(taxiKey(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE),
                new TaxiHop(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE, 0, -4821.13f, -1152.4f, 502.295f));
        taxiNodes.put(Content.TAXI_STORMWIND, new TaxiNode(Content.TAXI_STORMWIND, 0,
                -8835.76f, 490.084f, 109.699f, true, false));
        weather.put(Content.ZONE_ELWYNN, new ZoneWeather(Content.ZONE_ELWYNN, Content.WEATHER_STATE_FINE, 0f));
        seedTalents();
        quests.put(Content.QUEST_A_THREAT_WITHIN, new QuestTemplate(Content.QUEST_A_THREAT_WITHIN, "A Threat Within", 1, 0,
                0, "Speak with Marshal McBride.", "Speak with Marshal McBride.", 0, 0, 0, 0, 1, 24, 0, 0));
        quests.put(Content.QUEST_REST_AND_RELAXATION, new QuestTemplate(Content.QUEST_REST_AND_RELAXATION,
                "Rest and Relaxation", 1, 0, 0, "", "", 0, 0, 0, 0, 5, 27,
                Content.ITEM_REFRESHING_SPRING_WATER, 5));
        quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, new QuestTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                "Kobold Camp Cleanup", 1, 0, 0, "", "", Content.NPC_KOBOLD_VERMIN, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Content.ZONE_ELWYNN));
        quests.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, new QuestTemplate(Content.QUEST_BROTHERHOOD_OF_THIEVES,
                "Brotherhood of Thieves", 2, 0, 0, "", "", 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 12, 4, 216, 0, 0,
                Content.ITEM_MILITIA_DAGGER, 1, Content.ITEM_MILITIA_HAMMER, 1));
        vendorItems.put(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(Content.ITEM_WORN_SHORTSWORD)));
        creatureLoot.computeIfAbsent(6, k -> new ArrayList<>())
                .add(new LootRow(Content.ITEM_WORN_SHORTSWORD, 100f, 1, 1));
        questGivers.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        questGivers.put(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        questInvolved.put(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        questInvolved.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        if (spawns.isEmpty()) {
            // Hostiles stay outside abbey NPC attack distance — otherwise faction NPCs
            // DetectOrAttack them on every World.tick (GuardAI / UnitAI MoveInLineOfSight).
            spawns.add(new Spawn(1, 6, 0, -8550f, -150f, 80f, 0f));
            spawns.add(new Spawn(2, Content.NPC_MARSHAL_DUGHAN, 0, Content.GOLDSHIRE_X, Content.GOLDSHIRE_Y, Content.GOLDSHIRE_Z, 0f));
            spawns.add(new Spawn(3, Content.NPC_CORINA_STEELE, 0, -8903f, -125f, 80f, 0f));
            spawns.add(new Spawn(4, Content.NPC_DEPUTY_WILLEM, 0, -8906f, -128f, 80f, 0f));
            spawns.add(new Spawn(5, Content.NPC_MARSHAL_MCBRIDE, 0, -8908f, -130f, 80f, 0f));
            spawns.add(new Spawn(6, 103, 0, -8600f, -180f, 80f, 0f));
            spawns.add(new Spawn(7, Content.NPC_LLANE_BESHERE, 0, -8918.36f, -208.411f, 82.309f, 0f));
            spawns.add(new Spawn(15, Content.NPC_KHELDEN_BREMEN, 0, -8920f, -210f, 82.3f, 0f));
            spawns.add(new Spawn(14, Content.NPC_DANE_LINDGREN, 0, -8910f, -200f, 82f, 0f));
            spawns.add(new Spawn(8, Content.NPC_DUNGAR_LONGDRINK, 0, -8835.76f, 490.084f, 109.699f, 0f));
            spawns.add(new Spawn(9, Content.NPC_AUCTIONEER_CHILTON, 0, -8912f, -122f, 80f, 0f));
            spawns.add(new Spawn(10, Content.NPC_OLIVIA_BURNSIDE, 0, -8914f, -124f, 80f, 0f));
            spawns.add(new Spawn(13, Content.NPC_REBECCA_LAUGHLIN, 0, -8916f, -126f, 80f, 0f));
            spawns.add(new Spawn(12, Content.NPC_INNKEEPER_FARLEY, 0, -9462.66f, 16.1915f, 57.0459f, 0f));
        }
        if (!eventCreatures.containsKey(Content.GAME_EVENT_MIDSUMMER)) {
            eventCreatures.put(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                    new Spawn(11, Content.NPC_LUMA_SKYMOTHER, 547, -92.45719f, -110.6642f, -2.866759f, 2.408554f))));
        }
        if (!eventGameObjects.containsKey(Content.GAME_EVENT_MIDSUMMER)) {
            eventGameObjects.put(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                    new Spawn(5470020, Content.GO_ICE_STONE, 547, -69.9045f, -162.245f, -2.36656f, 2.42601f))));
        }
        seedHeroWarriorUnlock();
        seedHeroPaladinUnlock();
        seedHeroHunterUnlock();
        seedHeroRogueUnlock();
        seedHeroPriestUnlock();
        seedHeroMageUnlock();
        seedHeroWarlockUnlock();
        seedHeroShamanUnlock();
        seedHeroDruidUnlock();
    }

    private void seedHeroWarriorUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_HEROS_FIRST_LESSON;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_LORVAEN_BLOODFEATHER,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Warrior Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_WARRIOR);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT, Content.TRAINER_SPELL_BATTLE_SHOUT_COST, 1),
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT_RANK2, 500, 12, 0, 0,
                        Content.SPELL_BATTLE_SHOUT, 0, 0, false),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_LEATHER),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_MAIL),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_PLATE))));
        ensureBattlecasterOnTrainer(trainer);
        quests.putIfAbsent(questId, new QuestTemplate(questId, "The Hero's First Lesson", 1, 0,
                0,
                "Practice with your weapon on the Mana Wyrms, then prove you can finish one. Return alive.",
                "Land 5 weapon hits on a Mana Wyrm and defeat 1 Mana Wyrm.",
                wyrm, org.tbc.world.classless.HeroClassUnlock.REQUIRED_KILLS));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questCreatureHits.putIfAbsent(questId, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.REQUIRED_HITS));
        questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.HEROIC_STRIKE);
        seedHeroWarriorFollowUps(trainer, wyrm, questId);
        // Map-0 twin is for TP-SL35 find() only. Faction 1604 is Horde — keep it outside
        // abbey DetectOrAttack range (same rule as seeded hostiles).
        addSpawnIfMissing(16, trainer, 0, -8400f, -400f, 80f, 0f);
        replaceSpawn(17, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SPAWN_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SPAWN_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SPAWN_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SPAWN_O);
    }

    private void seedHeroWarriorFollowUps(int trainer, int wyrm, int unlockQuest) {
        int strip = org.tbc.world.classless.HeroClassUnlock.ITEM_TRAINING_STRIP;
        items.putIfAbsent(strip, ItemTemplate.heroTrainingStrip());

        int rally = org.tbc.world.classless.HeroClassUnlock.QUEST_RALLY_THE_LINE;
        quests.putIfAbsent(rally, heroFollowUpQuest(rally, "Rally the Line",
                "Roar at the trainer's banner so the line hears you, then defeat a Mana Wyrm.",
                "Use /roar near the Warrior Trainer. Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, rally);
        addQuestRelation(questInvolved, trainer, rally);
        questRewSpell.putIfAbsent(rally, Content.SPELL_BATTLE_SHOUT);
        questEmoteNearNpc.putIfAbsent(rally, new EmoteNearNpcObjective(
                org.tbc.world.classless.HeroClassUnlock.TEXT_EMOTE_ROAR,
                trainer, Content.SPELL_BATTLE_SHOUT));

        int chargeQ = org.tbc.world.classless.HeroClassUnlock.QUEST_CLOSE_THE_DISTANCE;
        quests.putIfAbsent(chargeQ, heroFollowUpQuest(chargeQ, "Close the Distance",
                "Close on a Mana Wyrm and land three solid weapon hits. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm.",
                0, 0, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, chargeQ);
        addQuestRelation(questInvolved, trainer, chargeQ);
        questCreatureHits.putIfAbsent(chargeQ, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_CHARGE_HITS));
        questRewSpell.putIfAbsent(chargeQ, org.tbc.world.classless.HeroClassUnlock.SPELL_CHARGE);

        int rendQ = org.tbc.world.classless.HeroClassUnlock.QUEST_A_WOUND_TO_REMEMBER;
        quests.putIfAbsent(rendQ, heroFollowUpQuest(rendQ, "A Wound to Remember",
                "Recover a training strip from the practice ground and return alive.",
                "Collect 1 Training Strip.",
                0, 0, strip, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, rendQ);
        addQuestRelation(questInvolved, trainer, rendQ);
        questRewSpell.putIfAbsent(rendQ, org.tbc.world.classless.HeroClassUnlock.SPELL_REND);
    }

    private static QuestTemplate heroFollowUpQuest(int id, String title, String details, String objectives,
                                                   int creatureId, int creatureCount, int itemId, int itemCount,
                                                   int prevQuestId) {
        return new QuestTemplate(id, title, 1, 0, 0, details, objectives,
                creatureId, creatureCount, itemId, itemCount,
                1, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                prevQuestId, 0);
    }

    private void seedHeroPaladinUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_PALADIN_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_VOW_TESTED;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int token = org.tbc.world.classless.HeroClassUnlock.ITEM_PROTECTIVE_TOKEN;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_VELAARA_SUNWARD,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Paladin Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_PALADIN);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.spell.SpellEngine.DEVOTION_AURA, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_BLESSING_OF_MIGHT, 100, 1),
                new TrainerSpell(org.tbc.world.spell.SpellEngine.HOLY_LIGHT, 100, 1))));
        items.putIfAbsent(token, ItemTemplate.heroQuestJunk(
                token, "Protective Token"));
        quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Vow Tested",
                "Recover the lost protective token and defeat a Mana Wyrm that threatens the ward. Return alive.",
                "Collect 1 Protective Token. Defeat 1 Mana Wyrm.",
                wyrm, 1, token, 1, 0));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.SEAL_OF_RIGHTEOUSNESS);
        seedHeroPaladinFollowUps(trainer, wyrm, questId);
        addSpawnIfMissing(18, trainer, 0, -8402f, -402f, 80f, 0f);
        replaceSpawn(19, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PALADIN_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PALADIN_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PALADIN_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PALADIN_O);
    }

    private void seedHeroPaladinFollowUps(int trainer, int wyrm, int unlockQuest) {
        int blessing = org.tbc.world.classless.HeroClassUnlock.ITEM_BLESSING_TOKEN;
        int kit = org.tbc.world.classless.HeroClassUnlock.ITEM_HEALING_KIT;
        items.putIfAbsent(blessing, ItemTemplate.heroQuestJunk(blessing, "Blessing Token"));
        items.putIfAbsent(kit, ItemTemplate.heroQuestJunk(kit, "Healing Kit"));

        int stand = org.tbc.world.classless.HeroClassUnlock.QUEST_STAND_FAST;
        quests.putIfAbsent(stand, heroFollowUpQuest(stand, "Stand Fast",
                "Hold the trainer's ward by defeating a Mana Wyrm. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, stand);
        addQuestRelation(questInvolved, trainer, stand);
        questRewSpell.putIfAbsent(stand, org.tbc.world.spell.SpellEngine.DEVOTION_AURA);

        int strength = org.tbc.world.classless.HeroClassUnlock.QUEST_STRENGTH_IN_SERVICE;
        quests.putIfAbsent(strength, heroFollowUpQuest(strength, "Strength in Service",
                "Deliver the trainer's blessing token as proof of service. Return alive.",
                "Collect 1 Blessing Token.",
                0, 0, blessing, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, strength);
        addQuestRelation(questInvolved, trainer, strength);
        questRewSpell.putIfAbsent(strength, org.tbc.world.classless.HeroClassUnlock.SPELL_BLESSING_OF_MIGHT);

        int mercy = org.tbc.world.classless.HeroClassUnlock.QUEST_MERCYS_LESSON;
        quests.putIfAbsent(mercy, heroFollowUpQuest(mercy, "Mercy's Lesson",
                "Recover a healing kit for a wounded trainee. Return alive.",
                "Collect 1 Healing Kit.",
                0, 0, kit, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, mercy);
        addQuestRelation(questInvolved, trainer, mercy);
        questRewSpell.putIfAbsent(mercy, org.tbc.world.spell.SpellEngine.HOLY_LIGHT);
    }

    private void seedHeroDruidUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_DRUID_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_LIVING_BALANCE;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int seed = org.tbc.world.classless.HeroClassUnlock.ITEM_BLIGHTED_SEED;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_LIRAEN_WILDLEAF,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Druid Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_DRUID);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_TOUCH, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_MOONFIRE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_MARK_OF_THE_WILD, 100, 1))));
        items.putIfAbsent(seed, ItemTemplate.heroQuestJunk(seed, "Blighted Seed"));
        quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Living Balance",
                "Recover a blighted seed from the grove edge, then defeat a Mana Wyrm that feeds on it. Return alive.",
                "Collect 1 Blighted Seed. Defeat 1 Mana Wyrm.",
                wyrm, 1, seed, 1, 0));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_WRATH);
        seedHeroDruidFollowUps(trainer, wyrm, questId);
        addSpawnIfMissing(32, trainer, 0, -8416f, -416f, 80f, 0f);
        replaceSpawn(33, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_DRUID_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_DRUID_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_DRUID_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_DRUID_O);
    }

    private void seedHeroDruidFollowUps(int trainer, int wyrm, int unlockQuest) {
        int salve = org.tbc.world.classless.HeroClassUnlock.ITEM_GROVE_SALVE;
        int mark = org.tbc.world.classless.HeroClassUnlock.ITEM_MOONLIGHT_MARK;
        int offering = org.tbc.world.classless.HeroClassUnlock.ITEM_WILD_OFFERING;
        items.putIfAbsent(salve, ItemTemplate.heroQuestJunk(salve, "Grove Salve"));
        items.putIfAbsent(mark, ItemTemplate.heroQuestJunk(mark, "Moonlight Mark"));
        items.putIfAbsent(offering, ItemTemplate.heroQuestJunk(offering, "Wild Offering"));

        int touch = org.tbc.world.classless.HeroClassUnlock.QUEST_TOUCH_OF_THE_GROVE;
        quests.putIfAbsent(touch, heroFollowUpQuest(touch, "Touch of the Grove",
                "Gather grove salve for a wounded ally. Return alive.",
                "Collect 1 Grove Salve.",
                0, 0, salve, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, touch);
        addQuestRelation(questInvolved, trainer, touch);
        questRewSpell.putIfAbsent(touch, org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_TOUCH);

        int silent = org.tbc.world.classless.HeroClassUnlock.QUEST_A_SILENT_MARK;
        quests.putIfAbsent(silent, heroFollowUpQuest(silent, "A Silent Mark",
                "Place a moonlight mark, then defeat its local threat with your existing kit. Return alive.",
                "Collect 1 Moonlight Mark. Defeat 1 Mana Wyrm.",
                wyrm, 1, mark, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, silent);
        addQuestRelation(questInvolved, trainer, silent);
        questRewSpell.putIfAbsent(silent, org.tbc.world.classless.HeroClassUnlock.SPELL_MOONFIRE);

        int gift = org.tbc.world.classless.HeroClassUnlock.QUEST_A_GIFT_OF_THE_WILD;
        quests.putIfAbsent(gift, heroFollowUpQuest(gift, "A Gift of the Wild",
                "Recover a wild offering from the trainer's grove trial. Return alive.",
                "Collect 1 Wild Offering.",
                0, 0, offering, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, gift);
        addQuestRelation(questInvolved, trainer, gift);
        questRewSpell.putIfAbsent(gift, org.tbc.world.classless.HeroClassUnlock.SPELL_MARK_OF_THE_WILD);
    }

    private void seedHeroShamanUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int token = org.tbc.world.classless.HeroClassUnlock.ITEM_ELEMENTAL_TOKEN;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_TALAAN_STONESONG,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Shaman Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_SHAMAN);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_WAVE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_EARTH_SHOCK, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_STONESKIN_TOTEM, 100, 1))));
        items.putIfAbsent(token, ItemTemplate.heroQuestJunk(token, "Elemental Token"));
        quests.putIfAbsent(questId, heroFollowUpQuest(questId, "Listen to the Elements",
                "Recover a local elemental token, present it at the trainer's shrine, and defeat a Mana Wyrm that disturbs the site. Return alive.",
                "Collect 1 Elemental Token. Defeat 1 Mana Wyrm.",
                wyrm, 1, token, 1, 0));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_LIGHTNING_BOLT);
        seedHeroShamanFollowUps(trainer, wyrm, questId);
        addSpawnIfMissing(30, trainer, 0, -8414f, -414f, 80f, 0f);
        replaceSpawn(31, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SHAMAN_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SHAMAN_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SHAMAN_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_SHAMAN_O);
    }

    private void seedHeroShamanFollowUps(int trainer, int wyrm, int unlockQuest) {
        int herbs = org.tbc.world.classless.HeroClassUnlock.ITEM_HEALING_HERBS;
        int marker = org.tbc.world.classless.HeroClassUnlock.ITEM_ELEMENTAL_MARKER;
        int earth = org.tbc.world.classless.HeroClassUnlock.ITEM_EARTH_SAMPLE;
        items.putIfAbsent(herbs, ItemTemplate.heroQuestJunk(herbs, "Healing Herbs"));
        items.putIfAbsent(marker, ItemTemplate.heroQuestJunk(marker, "Elemental Marker"));
        items.putIfAbsent(earth, ItemTemplate.heroQuestJunk(earth, "Earth Sample"));

        int mend = org.tbc.world.classless.HeroClassUnlock.QUEST_MEND_THE_WOUNDED;
        quests.putIfAbsent(mend, heroFollowUpQuest(mend, "Mend the Wounded",
                "Gather healing herbs for a wounded ally. Return alive.",
                "Collect 1 Healing Herbs.",
                0, 0, herbs, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, mend);
        addQuestRelation(questInvolved, trainer, mend);
        questRewSpell.putIfAbsent(mend, org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_WAVE);

        int shock = org.tbc.world.classless.HeroClassUnlock.QUEST_ANSWERING_SHOCK;
        quests.putIfAbsent(shock, heroFollowUpQuest(shock, "Answering Shock",
                "Restore a disturbed elemental marker, then defeat its local threat with your existing kit. Return alive.",
                "Collect 1 Elemental Marker. Defeat 1 Mana Wyrm.",
                wyrm, 1, marker, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, shock);
        addQuestRelation(questInvolved, trainer, shock);
        questRewSpell.putIfAbsent(shock, org.tbc.world.classless.HeroClassUnlock.SPELL_EARTH_SHOCK);

        int call = org.tbc.world.classless.HeroClassUnlock.QUEST_CALL_OF_EARTH;
        quests.putIfAbsent(call, heroFollowUpQuest(call, "Call of Earth",
                "Recover an earth sample from the trainer's shrine trial. Return alive.",
                "Collect 1 Earth Sample.",
                0, 0, earth, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, call);
        addQuestRelation(questInvolved, trainer, call);
        questRewSpell.putIfAbsent(call, org.tbc.world.classless.HeroClassUnlock.SPELL_STONESKIN_TOTEM);
    }

    private void seedHeroWarlockUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_WARLOCK_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_THE_BOUND_FLAME;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int mark = org.tbc.world.classless.HeroClassUnlock.ITEM_BINDING_MARK;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_VAELITH_DARKBIND,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Warlock Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_WARLOCK);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_BOLT, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_IMMOLATE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SUMMON_IMP, 100, 1))));
        items.putIfAbsent(mark, ItemTemplate.heroQuestJunk(mark, "Binding Mark"));
        quests.putIfAbsent(questId, heroFollowUpQuest(questId, "The Bound Flame",
                "Recover a binding mark from the local cult and contain a Mana Wyrm that threatens the site. Return alive.",
                "Collect 1 Binding Mark. Defeat 1 Mana Wyrm.",
                wyrm, 1, mark, 1, 0));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_CORRUPTION);
        seedHeroWarlockFollowUps(trainer, wyrm, questId);
        addSpawnIfMissing(28, trainer, 0, -8412f, -412f, 80f, 0f);
        replaceSpawn(29, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_WARLOCK_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_WARLOCK_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_WARLOCK_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_WARLOCK_O);
    }

    private void seedHeroWarlockFollowUps(int trainer, int wyrm, int unlockQuest) {
        int page = org.tbc.world.classless.HeroClassUnlock.ITEM_SHADOWED_PAGE;
        int ember = org.tbc.world.classless.HeroClassUnlock.ITEM_FEL_EMBER;
        int reagents = org.tbc.world.classless.HeroClassUnlock.ITEM_BINDING_REAGENTS;
        items.putIfAbsent(page, ItemTemplate.heroQuestJunk(page, "Shadowed Page"));
        items.putIfAbsent(ember, ItemTemplate.heroQuestJunk(ember, "Controlled Fel Ember"));
        items.putIfAbsent(reagents, ItemTemplate.heroQuestJunk(reagents, "Binding Reagents"));

        int shadow = org.tbc.world.classless.HeroClassUnlock.QUEST_SHADOW_IN_RESERVE;
        quests.putIfAbsent(shadow, heroFollowUpQuest(shadow, "Shadow in Reserve",
                "Recover a shadowed page and defeat a marked target with your existing kit. Return alive.",
                "Collect 1 Shadowed Page. Defeat 1 Mana Wyrm.",
                wyrm, 1, page, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, shadow);
        addQuestRelation(questInvolved, trainer, shadow);
        questRewSpell.putIfAbsent(shadow, org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_BOLT);

        int fel = org.tbc.world.classless.HeroClassUnlock.QUEST_FEL_AT_THE_EDGE;
        quests.putIfAbsent(fel, heroFollowUpQuest(fel, "Fel at the Edge",
                "Collect a controlled fel ember from a local threat. Return alive.",
                "Collect 1 Controlled Fel Ember.",
                0, 0, ember, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, fel);
        addQuestRelation(questInvolved, trainer, fel);
        questRewSpell.putIfAbsent(fel, org.tbc.world.classless.HeroClassUnlock.SPELL_IMMOLATE);

        int familiar = org.tbc.world.classless.HeroClassUnlock.QUEST_A_FAMILIARS_FIRST_TASK;
        quests.putIfAbsent(familiar, heroFollowUpQuest(familiar, "A Familiar's First Task",
                "Recover the trainer's binding reagents. Return alive.",
                "Collect 1 Binding Reagents.",
                0, 0, reagents, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, familiar);
        addQuestRelation(questInvolved, trainer, familiar);
        questRewSpell.putIfAbsent(familiar, org.tbc.world.classless.HeroClassUnlock.SPELL_SUMMON_IMP);
    }

    private void seedHeroMageUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_MAGE_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_CONTROLLED_SPARK;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int fragments = org.tbc.world.classless.HeroClassUnlock.ITEM_ARCANE_FRAGMENTS;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_ARYN_FLAMEWEAVE,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Mage Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_MAGE);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.spell.SpellEngine.FROST_ARMOR, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_INTELLECT, 100, 1),
                new TrainerSpell(org.tbc.world.spell.SpellEngine.FROSTBOLT, 100, 1))));
        items.putIfAbsent(fragments, ItemTemplate.heroQuestJunk(fragments, "Arcane Fragments"));
        quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Controlled Spark",
                "Recover arcane fragments, stabilize them at the trainer's focus, and defeat a Mana Wyrm. Return alive.",
                "Collect 1 Arcane Fragments. Defeat 1 Mana Wyrm.",
                wyrm, 1, fragments, 1, 0));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.FIREBALL);
        seedHeroMageFollowUps(trainer, wyrm, questId);
        addSpawnIfMissing(26, trainer, 0, -8410f, -410f, 80f, 0f);
        replaceSpawn(27, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_MAGE_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_MAGE_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_MAGE_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_MAGE_O);
    }

    private void seedHeroMageFollowUps(int trainer, int wyrm, int unlockQuest) {
        int focus = org.tbc.world.classless.HeroClassUnlock.ITEM_FROST_TREATED_FOCUS;
        int notes = org.tbc.world.classless.HeroClassUnlock.ITEM_STUDY_NOTES;
        items.putIfAbsent(focus, ItemTemplate.heroQuestJunk(focus, "Frost-Treated Focus"));
        items.putIfAbsent(notes, ItemTemplate.heroQuestJunk(notes, "Study Notes"));

        int cooler = org.tbc.world.classless.HeroClassUnlock.QUEST_A_COOLER_HEAD;
        quests.putIfAbsent(cooler, heroFollowUpQuest(cooler, "A Cooler Head",
                "Recover a frost-treated focus from a local hazard. Return alive.",
                "Collect 1 Frost-Treated Focus.",
                0, 0, focus, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, cooler);
        addQuestRelation(questInvolved, trainer, cooler);
        questRewSpell.putIfAbsent(cooler, org.tbc.world.spell.SpellEngine.FROST_ARMOR);

        int study = org.tbc.world.classless.HeroClassUnlock.QUEST_SHARE_THE_STUDY;
        quests.putIfAbsent(study, heroFollowUpQuest(study, "Share the Study",
                "Deliver the trainer's study notes to an ally. Return alive.",
                "Collect 1 Study Notes.",
                0, 0, notes, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, study);
        addQuestRelation(questInvolved, trainer, study);
        questRewSpell.putIfAbsent(study, org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_INTELLECT);

        int second = org.tbc.world.classless.HeroClassUnlock.QUEST_A_SECOND_SCHOOL;
        quests.putIfAbsent(second, heroFollowUpQuest(second, "A Second School",
                "Defeat a marked target with your existing kit. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, second);
        addQuestRelation(questInvolved, trainer, second);
        questRewSpell.putIfAbsent(second, org.tbc.world.spell.SpellEngine.FROSTBOLT);
    }

    private void seedHeroPriestUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_PRIEST_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int supplies = org.tbc.world.classless.HeroClassUnlock.ITEM_HEALING_SUPPLIES;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_LIRAE_DAWNWHISPER,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Priest Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_PRIEST);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SMITE, 100, 1),
                new TrainerSpell(org.tbc.world.spell.SpellEngine.POWER_WORD_FORTITUDE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_WORD_PAIN, 100, 1))));
        items.putIfAbsent(supplies, ItemTemplate.heroQuestJunk(supplies, "Healing Supplies"));
        quests.putIfAbsent(questId, heroFollowUpQuest(questId, "Mercy and Judgment",
                "Recover healing supplies for a wounded trainee and defeat a Mana Wyrm that threatens the route. Return alive.",
                "Collect 1 Healing Supplies. Defeat 1 Mana Wyrm.",
                wyrm, 1, supplies, 1, 0));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_LESSER_HEAL);
        seedHeroPriestFollowUps(trainer, wyrm, questId);
        addSpawnIfMissing(24, trainer, 0, -8408f, -408f, 80f, 0f);
        replaceSpawn(25, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PRIEST_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PRIEST_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PRIEST_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_PRIEST_O);
    }

    private void seedHeroPriestFollowUps(int trainer, int wyrm, int unlockQuest) {
        int scroll = org.tbc.world.classless.HeroClassUnlock.ITEM_WARDING_SCROLL;
        int shadow = org.tbc.world.classless.HeroClassUnlock.ITEM_SHADOW_MARKED_TOKEN;
        items.putIfAbsent(scroll, ItemTemplate.heroQuestJunk(scroll, "Warding Scroll"));
        items.putIfAbsent(shadow, ItemTemplate.heroQuestJunk(shadow, "Shadow-Marked Token"));

        int judgment = org.tbc.world.classless.HeroClassUnlock.QUEST_JUDGMENT_FROM_AFAR;
        quests.putIfAbsent(judgment, heroFollowUpQuest(judgment, "Judgment from Afar",
                "Defeat a marked target with your existing kit. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, judgment);
        addQuestRelation(questInvolved, trainer, judgment);
        questRewSpell.putIfAbsent(judgment, org.tbc.world.classless.HeroClassUnlock.SPELL_SMITE);

        int guarding = org.tbc.world.classless.HeroClassUnlock.QUEST_A_GUARDING_WORD;
        quests.putIfAbsent(guarding, heroFollowUpQuest(guarding, "A Guarding Word",
                "Deliver a warding scroll to an ally. Return alive.",
                "Collect 1 Warding Scroll.",
                0, 0, scroll, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, guarding);
        addQuestRelation(questInvolved, trainer, guarding);
        questRewSpell.putIfAbsent(guarding, org.tbc.world.spell.SpellEngine.POWER_WORD_FORTITUDE);

        int pain = org.tbc.world.classless.HeroClassUnlock.QUEST_PAIN_AS_WARNING;
        quests.putIfAbsent(pain, heroFollowUpQuest(pain, "Pain as Warning",
                "Recover a shadow-marked token from a local hostile's camp. Return alive.",
                "Collect 1 Shadow-Marked Token.",
                0, 0, shadow, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, pain);
        addQuestRelation(questInvolved, trainer, pain);
        questRewSpell.putIfAbsent(pain, org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_WORD_PAIN);
    }

    private void seedHeroRogueUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_ROGUE_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_QUIET_HAND;
        int token = org.tbc.world.classless.HeroClassUnlock.ITEM_CAMP_TOKEN;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_SYLARA_NIGHTWHISPER,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Rogue Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_ROGUE);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.spell.SpellEngine.SPELL_STEALTH, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_EVISCERATE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SLICE_AND_DICE, 100, 1))));
        items.putIfAbsent(token, ItemTemplate.heroQuestJunk(token, "Camp Token"));
        quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Quiet Hand",
                "Recover the trainer's token from a local hostile's camp. Return alive.",
                "Collect 1 Camp Token.",
                0, 0, token, 1, 0));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_SINISTER_STRIKE);
        seedHeroRogueFollowUps(trainer, questId);
        addSpawnIfMissing(22, trainer, 0, -8406f, -406f, 80f, 0f);
        replaceSpawn(23, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_ROGUE_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_ROGUE_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_ROGUE_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_ROGUE_O);
    }

    private void seedHeroRogueFollowUps(int trainer, int unlockQuest) {
        int shadowed = org.tbc.world.classless.HeroClassUnlock.ITEM_SHADOWED_TOKEN;
        int notes = org.tbc.world.classless.HeroClassUnlock.ITEM_FINISHING_NOTES;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        items.putIfAbsent(shadowed, ItemTemplate.heroQuestJunk(shadowed, "Shadowed Token"));
        items.putIfAbsent(notes, ItemTemplate.heroQuestJunk(notes, "Finishing-Form Notes"));

        int disappear = org.tbc.world.classless.HeroClassUnlock.QUEST_DISAPPEAR_FROM_SIGHT;
        quests.putIfAbsent(disappear, heroFollowUpQuest(disappear, "Disappear from Sight",
                "Retrieve a marked token from the practice grounds. Stealth is not required yet. Return alive.",
                "Collect 1 Shadowed Token.",
                0, 0, shadowed, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, disappear);
        addQuestRelation(questInvolved, trainer, disappear);
        questRewSpell.putIfAbsent(disappear, org.tbc.world.spell.SpellEngine.SPELL_STEALTH);

        int finish = org.tbc.world.classless.HeroClassUnlock.QUEST_FINISH_THE_OPENING;
        quests.putIfAbsent(finish, heroFollowUpQuest(finish, "Finish the Opening",
                "Land three solid hits on a practice target. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm.",
                0, 0, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, finish);
        addQuestRelation(questInvolved, trainer, finish);
        questCreatureHits.putIfAbsent(finish, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_EVISCERATE_HITS));
        questRewSpell.putIfAbsent(finish, org.tbc.world.classless.HeroClassUnlock.SPELL_EVISCERATE);

        int advantage = org.tbc.world.classless.HeroClassUnlock.QUEST_KEEP_THE_ADVANTAGE;
        quests.putIfAbsent(advantage, heroFollowUpQuest(advantage, "Keep the Advantage",
                "Recover the trainer's finishing-form notes from a local cache. Return alive.",
                "Collect 1 Finishing-Form Notes.",
                0, 0, notes, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, advantage);
        addQuestRelation(questInvolved, trainer, advantage);
        questRewSpell.putIfAbsent(advantage, org.tbc.world.classless.HeroClassUnlock.SPELL_SLICE_AND_DICE);
    }

    private void seedHeroHunterUnlock() {
        int trainer = org.tbc.world.classless.HeroClassUnlock.NPC_HERO_HUNTER_TRAINER;
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_THE_MARKED_TRAIL;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        creatures.put(trainer, new CreatureTemplate(trainer,
                org.tbc.world.classless.HeroClassUnlock.NAME_KAELAN_DAWNSTRIKE,
                org.tbc.world.classless.HeroClassUnlock.DISPLAY_JESTHENIS,
                org.tbc.world.classless.HeroClassUnlock.FACTION_SILVERMOON, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER,
                "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                "Hunter Trainer", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0));
        trainerTypeByEntry.putIfAbsent(trainer, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(trainer, Player.CLASS_HUNTER);
        trainerSpells.putIfAbsent(trainer, new ArrayList<>(List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_AUTO_SHOT, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SERPENT_STING, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_SHOT, 100, 1))));
        quests.putIfAbsent(questId, new QuestTemplate(questId, "The Marked Trail", 1, 0,
                0,
                "Track the local prey, land three solid hits, then finish one. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm and defeat 1 Mana Wyrm.",
                wyrm, org.tbc.world.classless.HeroClassUnlock.REQUIRED_KILLS));
        addQuestRelation(questGivers, trainer, questId);
        addQuestRelation(questInvolved, trainer, questId);
        questCreatureHits.putIfAbsent(questId, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_MARKED_HITS));
        questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.HUNTERS_MARK);
        seedHeroHunterFollowUps(trainer, wyrm, questId);
        addSpawnIfMissing(20, trainer, 0, -8404f, -404f, 80f, 0f);
        replaceSpawn(21, trainer, 530,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_HUNTER_X,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_HUNTER_Y,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_HUNTER_Z,
                org.tbc.world.classless.HeroClassUnlock.SUNSTRIDER_HUNTER_O);
    }

    private void seedHeroHunterFollowUps(int trainer, int wyrm, int unlockQuest) {
        int venom = org.tbc.world.classless.HeroClassUnlock.ITEM_VENOM_SAMPLE;
        items.putIfAbsent(venom, ItemTemplate.heroQuestJunk(venom, "Venom Sample"));

        int steady = org.tbc.world.classless.HeroClassUnlock.QUEST_STEADY_AIM;
        quests.putIfAbsent(steady, heroFollowUpQuest(steady, "Steady Aim",
                "Prove you can finish a local threat with your basic kit. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, steady);
        addQuestRelation(questInvolved, trainer, steady);
        questRewSpell.putIfAbsent(steady, org.tbc.world.classless.HeroClassUnlock.SPELL_AUTO_SHOT);

        int venomQ = org.tbc.world.classless.HeroClassUnlock.QUEST_VENOM_IN_THE_FIELD;
        quests.putIfAbsent(venomQ, heroFollowUpQuest(venomQ, "Venom in the Field",
                "Recover venom samples from local beasts. Return alive.",
                "Collect 1 Venom Sample.",
                0, 0, venom, 1, unlockQuest));
        addQuestRelation(questGivers, trainer, venomQ);
        addQuestRelation(questInvolved, trainer, venomQ);
        questRewSpell.putIfAbsent(venomQ, org.tbc.world.classless.HeroClassUnlock.SPELL_SERPENT_STING);

        int clean = org.tbc.world.classless.HeroClassUnlock.QUEST_A_CLEAN_SHOT;
        quests.putIfAbsent(clean, heroFollowUpQuest(clean, "A Clean Shot",
                "Land three solid hits on a marked target. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm.",
                0, 0, 0, 0, unlockQuest));
        addQuestRelation(questGivers, trainer, clean);
        addQuestRelation(questInvolved, trainer, clean);
        questCreatureHits.putIfAbsent(clean, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_CLEAN_SHOT_HITS));
        questRewSpell.putIfAbsent(clean, org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_SHOT);
    }

    private void addSpawnIfMissing(int guid, int entry, int map, float x, float y, float z, float o) {
        for (Spawn s : spawns) {
            if (s.guid() == guid) {
                return;
            }
        }
        spawns.add(new Spawn(guid, entry, map, x, y, z, o));
    }

    private void replaceSpawn(int guid, int entry, int map, float x, float y, float z, float o) {
        spawns.removeIf(s -> s.guid() == guid);
        spawns.add(new Spawn(guid, entry, map, x, y, z, o));
    }

    private static CreatureTemplate seedKoboldVermin() {
        return new CreatureTemplate(6, "Kobold Vermin", 10913, 7, 42, 1, 0, "", "", 0,
                "", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0,
                "", 0, 1f, 3f, 2000, 1.5f, 0, 1, 1,
                org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT);
    }

    private void seedQueryDefaults() {
        creatures.putIfAbsent(6, seedKoboldVermin());
        // Mana Wyrm — Eversong; MinLevelMana for Mana Tap 28734 / client mana bar.
        creatures.putIfAbsent(15274, new CreatureTemplate(15274, "Mana Wyrm", 15404, 7, 55, 1, 0, "", "", 0));
        creatureMana.putIfAbsent(15274, 65);
        // battlemaster_entry: Kurak (2302) → BATTLEGROUND_WS = 2
        battleMasterBg.putIfAbsent(2302, 2);
        creatures.putIfAbsent(Content.NPC_LLANE_BESHERE, new CreatureTemplate(Content.NPC_LLANE_BESHERE, "Llane Beshere", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        items.putIfAbsent(25, ItemTemplate.wornShortsword());
        items.putIfAbsent(Content.ITEM_SKINNING_KNIFE, ItemTemplate.skinningKnife());
        items.putIfAbsent(38, ItemTemplate.recruitsShirt());
        items.putIfAbsent(39, ItemTemplate.recruitsPants());
        items.putIfAbsent(40, ItemTemplate.recruitsBoots());
        items.putIfAbsent(Content.ITEM_RIVERPAW_LEATHER_VEST, ItemTemplate.riverpawLeatherVest());
        items.putIfAbsent(Content.ITEM_TUNIC_OF_WESTFALL, ItemTemplate.tunicOfWestfall());
        items.putIfAbsent(Content.ITEM_BRACKWATER_VEST, ItemTemplate.brackwaterVest());
        items.putIfAbsent(Content.ITEM_SEERS_ROBE, ItemTemplate.seersRobe());
        items.putIfAbsent(Content.ITEM_BLACKENED_DEFIAS_ARMOR, ItemTemplate.blackenedDefiasArmor());
        items.putIfAbsent(Content.ITEM_LIGHTFORGE_BREASTPLATE, ItemTemplate.lightforgeBreastplate());
        items.putIfAbsent(Content.ITEM_LAWBRINGER_CHESTGUARD, ItemTemplate.lawbringerChestguard());
        items.putIfAbsent(Content.ITEM_LIVING_BREASTPLATE, ItemTemplate.livingBreastplate());
        items.putIfAbsent(Content.ITEM_ICEBANE_BREASTPLATE, ItemTemplate.icebaneBreastplate());
        items.putIfAbsent(Content.ITEM_SHADESTEEL_GREAVES, ItemTemplate.shadesteelGreaves());
        items.putIfAbsent(Content.ITEM_SOULCLOTH_VEST, ItemTemplate.soulclothVest());
        items.putIfAbsent(Content.ITEM_BLADE_OF_HANNA, ItemTemplate.bladeOfHanna());
        items.putIfAbsent(Content.ITEM_DESTROYER_CHESTGUARD, ItemTemplate.destroyerChestguard());
        items.putIfAbsent(Content.ITEM_DESTROYER_BREASTPLATE, ItemTemplate.destroyerBreastplate());
        items.putIfAbsent(Content.ITEM_GLADIATORS_PLATE_CHESTPIECE, ItemTemplate.gladiatorsPlateChestpiece());
        items.putIfAbsent(Content.ITEM_ONSLAUGHT_CHESTGUARD, ItemTemplate.onslaughtChestguard());
        items.putIfAbsent(Content.ITEM_WARHARNESS_OF_RECKLESS_FURY, ItemTemplate.warharnessOfRecklessFury());
        items.putIfAbsent(Content.ITEM_GAUNTLETS_OF_ENFORCEMENT, ItemTemplate.gauntletsOfEnforcement());
        items.putIfAbsent(Content.ITEM_VENGEFUL_GLADIATORS_DRAGONHIDE_TUNIC,
                ItemTemplate.vengefulGladiatorsDragonhideTunic());
        items.putIfAbsent(Content.ITEM_AUCHENAI_ANCHORITES_ROBE, ItemTemplate.auchenaiAnchoritesRobe());
        items.putIfAbsent(Content.ITEM_GARMENTS_OF_SERENE_SHORES, ItemTemplate.garmentsOfSereneShores());
        items.putIfAbsent(Content.ITEM_SUNGLOW_VEST, ItemTemplate.sunglowVest());
        items.putIfAbsent(Content.ITEM_WORN_WOODEN_SHIELD, ItemTemplate.wornWoodenShield());
        items.putIfAbsent(Content.ITEM_CLOAK_OF_DARKNESS, ItemTemplate.cloakOfDarkness());
        items.putIfAbsent(Content.ITEM_NETHERSTRAND_LONGBOW, ItemTemplate.netherstrandLongbow());
        items.putIfAbsent(Content.ITEM_TWIN_BLADES_OF_AZZINOTH, ItemTemplate.twinBladesOfAzzinoth());
        items.putIfAbsent(Content.ITEM_JYOO_TEST_ITEM, ItemTemplate.jyooTestItem());
        items.putIfAbsent(Content.ITEM_TOMS_BOOTS_1, ItemTemplate.tomsBoots1());
        items.putIfAbsent(Content.ITEM_TEST_HP_RING, ItemTemplate.testHpRing());
        items.putIfAbsent(Content.ITEM_TEST_HOLY_RESIST_VEST, ItemTemplate.testHolyResistVest());
        items.putIfAbsent(Content.ITEM_TEST_MP_RING, ItemTemplate.testMpRing());
        items.putIfAbsent(Content.ITEM_BAND_OF_THE_ETERNAL_CHAMPION, ItemTemplate.bandOfTheEternalChampion());
        items.putIfAbsent(Content.ITEM_GUILD_CHARTER, ItemTemplate.guildCharter());
        items.putIfAbsent(Content.ITEM_HEARTHSTONE, ItemTemplate.hearthstone());
        items.putIfAbsent(Content.ITEM_TOUGH_JERKY, ItemTemplate.toughJerky());
        items.putIfAbsent(Content.ITEM_TOUGH_HUNK_OF_BREAD, ItemTemplate.toughHunkOfBread());
        items.putIfAbsent(Content.ITEM_RED_BURLAP_BANDANA, ItemTemplate.redBurlapBandana());
        items.putIfAbsent(Content.ITEM_REFRESHING_SPRING_WATER, ItemTemplate.refreshingSpringWater());
        items.putIfAbsent(Content.ITEM_MINOR_HEALING_POTION, ItemTemplate.minorHealingPotion());
        // SQL load may have created empty spell rows; force usable-item spells from seeds.
        mergeUsableItemSpells(ItemTemplate.hearthstone());
        mergeUsableItemSpells(ItemTemplate.toughJerky());
        mergeUsableItemSpells(ItemTemplate.toughHunkOfBread());
        mergeUsableItemSpells(ItemTemplate.refreshingSpringWater());
        mergeUsableItemSpells(ItemTemplate.minorHealingPotion());
        // Keep consumable max-stack for 8606 client even if a thin SQL row set stackable=1.
        ensureStackable(Content.ITEM_TOUGH_JERKY, 20);
        ensureStackable(Content.ITEM_TOUGH_HUNK_OF_BREAD, 20);
        ensureStackable(Content.ITEM_RED_BURLAP_BANDANA, 20);
        ensureStackable(Content.ITEM_REFRESHING_SPRING_WATER, 20);
        ensureStackable(Content.ITEM_MINOR_HEALING_POTION, 5);
        quests.putIfAbsent(Content.QUEST_A_THREAT_WITHIN, new QuestTemplate(Content.QUEST_A_THREAT_WITHIN, "A Threat Within", 1, 0,
                0, "Speak with Marshal McBride.", "Speak with Marshal McBride.", 0, 0, 0, 0, 1, 24, 0, 0));
        quests.putIfAbsent(Content.QUEST_REST_AND_RELAXATION, new QuestTemplate(Content.QUEST_REST_AND_RELAXATION,
                "Rest and Relaxation", 1, 0, 0, "", "", 0, 0, 0, 0, 5, 27,
                Content.ITEM_REFRESHING_SPRING_WATER, 5));
        quests.putIfAbsent(Content.QUEST_KOBOLD_CAMP_CLEANUP, new QuestTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                "Kobold Camp Cleanup", 1, 0, 0, "", "", Content.NPC_KOBOLD_VERMIN, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Content.ZONE_ELWYNN));
        quests.putIfAbsent(Content.QUEST_BROTHERHOOD_OF_THIEVES, new QuestTemplate(Content.QUEST_BROTHERHOOD_OF_THIEVES,
                "Brotherhood of Thieves", 2, 0, 0, "", "", 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 12, 4, 216, 0, 0,
                Content.ITEM_MILITIA_DAGGER, 1, Content.ITEM_MILITIA_HAMMER, 1));
        vendorItems.putIfAbsent(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(Content.ITEM_WORN_SHORTSWORD)));
        creatureLoot.computeIfAbsent(6, k -> new ArrayList<>());
        if (creatureLoot.get(6).isEmpty()) {
            creatureLoot.get(6).add(new LootRow(Content.ITEM_WORN_SHORTSWORD, 100f, 1, 1));
        }
        questGivers.putIfAbsent(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        List<Integer> willemQuests = questGivers.get(Content.NPC_DEPUTY_WILLEM);
        if (willemQuests != null && !willemQuests.contains(Content.QUEST_BROTHERHOOD_OF_THIEVES)) {
            willemQuests.add(Content.QUEST_BROTHERHOOD_OF_THIEVES);
        }
        questGivers.putIfAbsent(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        questInvolved.putIfAbsent(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        questInvolved.putIfAbsent(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        List<Integer> willemInvolved = questInvolved.get(Content.NPC_DEPUTY_WILLEM);
        if (willemInvolved != null && !willemInvolved.contains(Content.QUEST_BROTHERHOOD_OF_THIEVES)) {
            willemInvolved.add(Content.QUEST_BROTHERHOOD_OF_THIEVES);
        }
        trainerTypeByEntry.putIfAbsent(Content.NPC_LLANE_BESHERE, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(Content.NPC_LLANE_BESHERE, 1);
        trainerSpells.putIfAbsent(Content.NPC_LLANE_BESHERE, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT, Content.TRAINER_SPELL_BATTLE_SHOUT_COST, 1),
                new TrainerSpell(Content.SPELL_BATTLE_SHOUT_RANK2, 500, 12, 0, 0,
                        Content.SPELL_BATTLE_SHOUT, 0, 0, false),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_LEATHER),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_MAIL),
                new TrainerSpell(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                        org.tbc.world.classless.CasterArmorPolicy.TRAINER_COST_BATTLECASTER,
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_PLATE))));
        ensureBattlecasterOnTrainer(Content.NPC_LLANE_BESHERE);
        spellChain.putIfAbsent(Content.SPELL_BATTLE_SHOUT_RANK2,
                new SpellChainNode(Content.SPELL_BATTLE_SHOUT_RANK2, Content.SPELL_BATTLE_SHOUT,
                        Content.SPELL_BATTLE_SHOUT, 2, 0));
        spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER, 2, 0));
        spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL, 3, 0));
        creatures.putIfAbsent(Content.NPC_KHELDEN_BREMEN, new CreatureTemplate(Content.NPC_KHELDEN_BREMEN, "Khelden Bremen", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        trainerTypeByEntry.putIfAbsent(Content.NPC_KHELDEN_BREMEN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        trainerClass.putIfAbsent(Content.NPC_KHELDEN_BREMEN, Player.CLASS_MAGE);
        trainerSpells.putIfAbsent(Content.NPC_KHELDEN_BREMEN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_FIREBALL, Content.TRAINER_SPELL_FIREBALL_COST, 1))));
        creatures.putIfAbsent(Content.NPC_DANE_LINDGREN, new CreatureTemplate(Content.NPC_DANE_LINDGREN, "Dane Lindgren", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER, "", "",
                org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS));
        trainerTypeByEntry.putIfAbsent(Content.NPC_DANE_LINDGREN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS);
        trainerSpells.putIfAbsent(Content.NPC_DANE_LINDGREN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_APPRENTICE_BLACKSMITH, Content.TRAINER_SPELL_APPRENTICE_BLACKSMITH_COST, 1,
                        0, 0, 0, 0, 0, true))));
        creatures.putIfAbsent(Content.NPC_DUNGAR_LONGDRINK, new CreatureTemplate(Content.NPC_DUNGAR_LONGDRINK, "Dungar Longdrink", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_FLIGHTMASTER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_INNKEEPER_FARLEY, new CreatureTemplate(Content.NPC_INNKEEPER_FARLEY, "Innkeeper Farley", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_INNKEEPER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_AUCTIONEER_CHILTON, new CreatureTemplate(Content.NPC_AUCTIONEER_CHILTON, "Auctioneer Chilton", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_AUCTIONEER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_OLIVIA_BURNSIDE, new CreatureTemplate(Content.NPC_OLIVIA_BURNSIDE, "Olivia Burnside", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_BANKER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_REBECCA_LAUGHLIN, new CreatureTemplate(Content.NPC_REBECCA_LAUGHLIN, "Rebecca Laughlin", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_PETITIONER | Content.UNIT_NPC_FLAG_TABARDDESIGNER, "", "", 0));
        creatures.putIfAbsent(Content.NPC_LUMA_SKYMOTHER, new CreatureTemplate(Content.NPC_LUMA_SKYMOTHER, "Luma Skymother", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP, "", "", 0));
        if (auctions.isEmpty()) {
            auctions.add(new Auction(1, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 43_200_000, "Worn Shortsword"));
        }
        taxiPaths.putIfAbsent(taxiKey(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE),
                new TaxiHop(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE, 0, -4821.13f, -1152.4f, 502.295f));
        taxiNodes.putIfAbsent(Content.TAXI_STORMWIND, new TaxiNode(Content.TAXI_STORMWIND, 0,
                -8835.76f, 490.084f, 109.699f, true, false));
        weather.putIfAbsent(Content.ZONE_ELWYNN, new ZoneWeather(Content.ZONE_ELWYNN, Content.WEATHER_STATE_FINE, 0f));
        seedTalents();
        pointsOfInterest.putIfAbsent(lionsPrideInnPoi().entry(), lionsPrideInnPoi());
        eventCreatures.putIfAbsent(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                new Spawn(11, Content.NPC_LUMA_SKYMOTHER, 547, -92.45719f, -110.6642f, -2.866759f, 2.408554f))));
        eventGameObjects.putIfAbsent(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                new Spawn(5470020, Content.GO_ICE_STONE, 547, -69.9045f, -162.245f, -2.36656f, 2.42601f))));
        boolean hasPetitioner = false;
        for (Spawn s : spawns) {
            if (s.entry() == Content.NPC_REBECCA_LAUGHLIN) {
                hasPetitioner = true;
                break;
            }
        }
        if (!hasPetitioner) {
            spawns.add(new Spawn(1_000_013, Content.NPC_REBECCA_LAUGHLIN, 0, -8916f, -126f, 80f, 0f));
        }
        seedHeroWarriorUnlock();
        seedMenu0();
        seedFarleyGossip();
    }

    public static long taxiKey(int from, int to) {
        return ((long) from << 32) | (to & 0xFFFFFFFFL);
    }

    /** ObjectMgr.h GetItemText miss. */
    public static final String MISSING_ITEM_TEXT = "There is no info for this item";

    public String itemText(int id) {
        String text = itemTexts.get(id);
        return text != null ? text : MISSING_ITEM_TEXT;
    }

    /** ObjectMgr.cpp GetNearestTaxiNode. Alliance team 469 uses MountCreatureID[1]. */
    public int nearestTaxiNode(float x, float y, float z, int mapId, int team) {
        boolean alliance = team == 469;
        int id = 0;
        float best = Float.MAX_VALUE;
        boolean found = false;
        for (TaxiNode n : taxiNodes.values()) {
            if (n.mapId() != mapId) {
                continue;
            }
            if (alliance ? !n.alliance() : !n.horde()) {
                continue;
            }
            float dx = n.x() - x;
            float dy = n.y() - y;
            float dz = n.z() - z;
            float dist2 = dx * dx + dy * dy + dz * dz;
            if (!found || dist2 < best) {
                found = true;
                best = dist2;
                id = n.id();
            }
        }
        return id;
    }

    public int gossipMenuId(int entry) {
        return gossipMenuIds.getOrDefault(entry, 0);
    }

    public int gossipTextId(int menuId) {
        if (menuId == 0) {
            return Content.DEFAULT_GOSSIP_MESSAGE;
        }
        return gossipTextIds.getOrDefault(menuId, Content.DEFAULT_GOSSIP_MESSAGE);
    }

    public List<GossipMenuItem> gossipOptionsFor(Player p, Creature c) {
        if (c == null) {
            return List.of();
        }
        return gossipOptionsFor(p, c, gossipMenuId(c.entry));
    }

    public List<GossipMenuItem> gossipOptionsFor(Player p, Creature c, int menuId) {
        if (c == null) {
            return List.of();
        }
        List<GossipMenuItem> rows = gossipOptions.getOrDefault(menuId, List.of());
        List<GossipMenuItem> out = new ArrayList<>();
        for (GossipMenuItem it : rows) {
            if (includeGossipOption(p, c, it)) {
                out.add(it);
                if (out.size() == GOSSIP_MAX_MENU_ITEMS) {
                    break;
                }
            }
        }
        return out;
    }

    private boolean includeGossipOption(Player p, Creature c, GossipMenuItem it) {
        if (it.conditionId() != 0) {
            // Hero (classless): waive SQL classmask/level gates on Train only; isTrainerOf still applies.
            boolean classlessTrainer = it.optionId() == GOSSIP_OPTION_TRAINER
                    && org.tbc.world.classless.ClasslessCharacterPolicy.isClassless(p);
            if (!classlessTrainer) {
                return false;
            }
        }
        if ((it.npcFlag() & c.npcFlags) == 0) {
            return false;
        }
        return switch (it.optionId()) {
            case GOSSIP_OPTION_GOSSIP -> true;
            case GOSSIP_OPTION_QUESTGIVER, GOSSIP_OPTION_ARMORER, GOSSIP_OPTION_BOT,
                    GOSSIP_OPTION_UNLEARNTALENTS, GOSSIP_OPTION_UNLEARNPETSKILLS,
                    GOSSIP_OPTION_BATTLEFIELD -> false;
            case GOSSIP_OPTION_VENDOR -> hasVendorStock(c);
            case GOSSIP_OPTION_TRAINER -> isTrainerOf(p, c);
            case GOSSIP_OPTION_SPIRITHEALER -> p != null && (p.ghost || !p.alive());
            case GOSSIP_OPTION_STABLEPET -> p != null && p.clazz == CLASS_HUNTER;
            case GOSSIP_OPTION_TAXIVENDOR, GOSSIP_OPTION_SPIRITGUIDE, GOSSIP_OPTION_INNKEEPER,
                    GOSSIP_OPTION_BANKER, GOSSIP_OPTION_PETITIONER, GOSSIP_OPTION_TABARDDESIGNER,
                    GOSSIP_OPTION_AUCTIONEER -> true;
            default -> false;
        };
    }

    /**
     * CMaNGOS Creature::IsTrainerOf — non-empty spell list (entry or template) and type/class rules.
     * Used for gossip TRAINER option and CMSG_TRAINER_LIST / BUY.
     */
    public boolean isTrainerOf(Player p, Creature c) {
        if (p == null || c == null) {
            return false;
        }
        List<TrainerSpell> spells = spellsForTrainer(c.entry);
        if (spells.isEmpty()) {
            return false;
        }
        if (org.tbc.world.classless.ClasslessTrainerPolicy.isTrainerOf(p, c, this)) {
            return true;
        }
        int type = trainerType(c.entry);
        int reqClass = trainerClass.getOrDefault(c.entry, 0);
        return switch (type) {
            // Creature.cpp IsTrainerOf TRAINER_TYPE_CLASS — exact TrainerClass match.
            case org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS -> p.clazz == reqClass;
            case org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS -> true;
            case org.tbc.world.session.TrainerHandler.TRAINER_TYPE_PETS -> p.clazz == CLASS_HUNTER;
            // Mount race/exalted gating deferred; empty list already refused above.
            case org.tbc.world.session.TrainerHandler.TRAINER_TYPE_MOUNTS -> true;
            default -> false;
        };
    }

    private void seedMenu0() {
        if (gossipOptions.containsKey(0) && !gossipOptions.get(0).isEmpty()) {
            return;
        }
        List<GossipMenuItem> rows = new ArrayList<>();
        rows.add(menu0(0, 0, "GOSSIP_OPTION_QUESTGIVER", GOSSIP_OPTION_QUESTGIVER, 2));
        rows.add(menu0(1, 1, "GOSSIP_OPTION_VENDOR", GOSSIP_OPTION_VENDOR, 128));
        rows.add(menu0(2, 2, "GOSSIP_OPTION_TAXIVENDOR", GOSSIP_OPTION_TAXIVENDOR, 8192));
        rows.add(menu0(3, 3, "GOSSIP_OPTION_TRAINER", GOSSIP_OPTION_TRAINER, 16));
        rows.add(menu0(4, 4, "GOSSIP_OPTION_SPIRITHEALER", GOSSIP_OPTION_SPIRITHEALER, 16384));
        rows.add(menu0(5, 4, "GOSSIP_OPTION_SPIRITGUIDE", GOSSIP_OPTION_SPIRITGUIDE, 32768));
        rows.add(menu0(6, 5, "GOSSIP_OPTION_INNKEEPER", GOSSIP_OPTION_INNKEEPER, 65536));
        rows.add(menu0(7, 6, "GOSSIP_OPTION_BANKER", GOSSIP_OPTION_BANKER, 131072));
        rows.add(menu0(8, 7, "GOSSIP_OPTION_PETITIONER", GOSSIP_OPTION_PETITIONER, 262144));
        rows.add(menu0(9, 8, "GOSSIP_OPTION_TABARDDESIGNER", GOSSIP_OPTION_TABARDDESIGNER, 524288));
        rows.add(menu0(10, 9, "GOSSIP_OPTION_BATTLEFIELD", GOSSIP_OPTION_BATTLEFIELD, 1048576));
        rows.add(menu0(11, 6, "GOSSIP_OPTION_AUCTIONEER", GOSSIP_OPTION_AUCTIONEER, 2097152));
        rows.add(menu0(12, 0, "GOSSIP_OPTION_STABLEPET", GOSSIP_OPTION_STABLEPET, 4194304));
        rows.add(menu0(13, 1, "GOSSIP_OPTION_ARMORER", GOSSIP_OPTION_ARMORER, 4096));
        rows.add(menu0(14, 0, "GOSSIP_OPTION_UNLEARNTALENTS", GOSSIP_OPTION_UNLEARNTALENTS, 16));
        rows.add(menu0(15, 2, "GOSSIP_OPTION_UNLEARNPETSKILLS", GOSSIP_OPTION_UNLEARNPETSKILLS, 16));
        rows.add(menu0(16, 0, "GOSSIP_OPTION_BOT", GOSSIP_OPTION_BOT, 1));
        gossipOptions.put(0, rows);
    }

    private static GossipMenuItem menu0(int id, int icon, String text, int optionId, int npcFlag) {
        return new GossipMenuItem(0, id, icon, text, optionId, npcFlag, 0, 0, "", 0);
    }

    private void seedFarleyGossip() {
        gossipMenuIds.putIfAbsent(Content.NPC_INNKEEPER_FARLEY, Content.GOSSIP_MENU_FARLEY);
        gossipTextIds.putIfAbsent(Content.GOSSIP_MENU_FARLEY, Content.GOSSIP_TEXT_FARLEY);
        gossipTextIds.putIfAbsent(Content.GOSSIP_MENU_FARLEY_INN_INFO, Content.GOSSIP_TEXT_FARLEY_INN_INFO);
        if (!gossipOptions.containsKey(Content.GOSSIP_MENU_FARLEY)) {
            gossipOptions.put(Content.GOSSIP_MENU_FARLEY, new ArrayList<>(List.of(
                    new GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 1, Content.GOSSIP_ICON_INTERACT_2,
                            "Make this inn your home.", GOSSIP_OPTION_INNKEEPER, Content.UNIT_NPC_FLAG_INNKEEPER,
                            0, 0, "", 0),
                    new GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 3, 0, Content.GOSSIP_FARLEY_INN_INFO,
                            GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "",
                            Content.GOSSIP_MENU_FARLEY_INN_INFO))));
        }
    }

    private void loadGossip(Connection c) {
        loadPointsOfInterest(c);
        loadGossipMenus(c);
        loadGossipOptions(c);
        loadGossipMenuIds(c);
        loadNpcTexts(c);
    }

    private void loadPointsOfInterest(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, x, y, icon, flags, data, icon_name FROM points_of_interest");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                float x = rs.getFloat(2);
                float y = rs.getFloat(3);
                if (!validMapCoord(x, y)) {
                    log.debug("points_of_interest entry {} invalid coordinates, ignored", rs.getInt(1));
                    continue;
                }
                int entry = rs.getInt(1);
                pointsOfInterest.put(entry, new PointOfInterest(entry, x, y, rs.getInt(4), rs.getInt(5),
                        rs.getInt(6), nz(rs.getString(7))));
                n++;
            }
            log.info("loaded {} points_of_interest", n);
        } catch (Exception e) {
            log.debug("points_of_interest load skipped: {}", e.getMessage());
        }
    }

    /** GridDefines.h MaNGOS::IsValidMapCoord. */
    static boolean validMapCoord(float x, float y) {
        return validMapCoord(x) && validMapCoord(y);
    }

    static boolean validMapCoord(float c) {
        return Float.isFinite(c) && Math.abs(c) <= MAP_HALFSIZE - 0.5f;
    }

    private void loadGossipMenus(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, text_id, condition_id FROM gossip_menu");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int menuId = rs.getInt(1);
                int conditionId = rs.getInt(3);
                if (conditionId != 0) {
                    continue;
                }
                gossipTextIds.putIfAbsent(menuId, rs.getInt(2));
                n++;
            }
            log.info("loaded {} gossip_menu rows", n);
        } catch (Exception e) {
            log.debug("gossip_menu load skipped: {}", e.getMessage());
        }
    }

    private void loadGossipOptions(Connection c) {
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "action_menu_id, action_poi_id, box_coded, box_money, box_text, condition_id "
                        + "FROM gossip_menu_option ORDER BY menu_id, id")) {
            return;
        }
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "action_menu_id, action_poi_id, box_coded, box_money, box_text FROM gossip_menu_option "
                        + "ORDER BY menu_id, id")) {
            return;
        }
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "action_menu_id, box_coded, box_money, box_text FROM gossip_menu_option "
                        + "ORDER BY menu_id, id")) {
            return;
        }
        if (loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag, "
                        + "box_coded, box_money, box_text FROM gossip_menu_option ORDER BY menu_id, id")) {
            return;
        }
        loadGossipOptionQuery(c,
                "SELECT menu_id, id, option_icon, option_text, option_id, npc_option_npcflag "
                        + "FROM gossip_menu_option ORDER BY menu_id, id");
    }

    private boolean loadGossipOptionQuery(Connection c, String sql) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean action = sql.contains("action_menu_id");
            boolean poiCol = sql.contains("action_poi_id");
            boolean boxed = sql.contains("box_coded");
            boolean condCol = sql.contains("condition_id");
            int n = 0;
            while (rs.next()) {
                int menuId = rs.getInt(1);
                int actionMenu = 0;
                int actionPoi = 0;
                int coded = 0;
                int boxMoney = 0;
                String boxText = "";
                int conditionId = 0;
                if (action) {
                    actionMenu = rs.getInt(7);
                    int col = 8;
                    if (poiCol) {
                        actionPoi = rs.getInt(col++);
                    }
                    if (boxed) {
                        coded = rs.getInt(col++);
                        boxMoney = rs.getInt(col++);
                        boxText = nz(rs.getString(col++));
                    }
                    if (condCol) {
                        conditionId = rs.getInt(col);
                    }
                } else if (boxed) {
                    coded = rs.getInt(7);
                    boxMoney = rs.getInt(8);
                    boxText = nz(rs.getString(9));
                }
                if (actionPoi != 0 && !pointsOfInterest.containsKey(actionPoi)) {
                    actionPoi = 0;
                }
                gossipOptions.computeIfAbsent(menuId, k -> new ArrayList<>()).add(new GossipMenuItem(
                        menuId, rs.getInt(2), rs.getInt(3), nz(rs.getString(4)), rs.getInt(5), rs.getInt(6),
                        coded, boxMoney, boxText, actionMenu, actionPoi, conditionId));
                n++;
            }
            log.info("loaded {} gossip_menu_option rows", n);
            return true;
        } catch (Exception e) {
            log.debug("gossip_menu_option load skipped: {}", e.getMessage());
            return false;
        }
    }

    private void loadGossipMenuIds(Connection c) {
        if (loadGossipMenuIdQuery(c, "SELECT Entry, GossipMenuId FROM creature_template")) {
            return;
        }
        loadGossipMenuIdQuery(c, "SELECT entry, GossipMenuId FROM creature_template");
    }

    private boolean loadGossipMenuIdQuery(Connection c, String sql) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int menuId = rs.getInt(2);
                if (menuId != 0) {
                    gossipMenuIds.put(rs.getInt(1), menuId);
                    n++;
                }
            }
            log.info("loaded {} creature GossipMenuId values", n);
            return true;
        } catch (Exception e) {
            log.debug("creature GossipMenuId load skipped: {}", e.getMessage());
            return false;
        }
    }

    private void loadNpcTexts(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(npcTextSelectSql()); ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int id = rs.getInt(1);
                if (id == 0) {
                    continue;
                }
                NpcTextSlot[] slots = new NpcTextSlot[Content.MAX_GOSSIP_TEXT_OPTIONS];
                int col = 2;
                for (int i = 0; i < slots.length; i++) {
                    String text0 = nz(rs.getString(col++));
                    String text1 = nz(rs.getString(col++));
                    int language = rs.getInt(col++);
                    float probability = rs.getFloat(col++);
                    int[] emotes = new int[6];
                    for (int e = 0; e < 6; e++) {
                        emotes[e] = rs.getInt(col++);
                    }
                    slots[i] = new NpcTextSlot(probability, text0, text1, language, emotes);
                }
                npcTexts.put(id, new NpcText(id, slots));
                n++;
            }
            log.info("loaded {} npc_text rows", n);
        } catch (Exception e) {
            log.debug("npc_text load skipped: {}", e.getMessage());
        }
    }

    private static String npcTextSelectSql() {
        StringBuilder sql = new StringBuilder("SELECT ID");
        for (int i = 0; i < Content.MAX_GOSSIP_TEXT_OPTIONS; i++) {
            sql.append(", text").append(i).append("_0, text").append(i).append("_1, lang").append(i)
                    .append(", prob").append(i);
            for (int e = 0; e < 6; e++) {
                sql.append(", em").append(i).append("_").append(e);
            }
        }
        return sql.append(" FROM npc_text").toString();
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

    /** Talent.dbc / TalentTab.dbc. In-memory seed is Improved Heroic Strike 124 when DataDir is absent. */
    void loadTalents(Path dataDir) {
        if (dataDir == null) {
            return;
        }
        Path talentFile = dataDir.resolve("dbc").resolve("Talent.dbc");
        if (Files.isRegularFile(talentFile)) {
            try {
                DbcFile dbc = DbcFile.load(talentFile);
                for (int[] row : dbc.records) {
                    if (row.length < 21 || row[0] == 0) {
                        continue;
                    }
                    talents.put(row[0], new Talent(row[0], row[1], row[2], row[3],
                            row[4], row[5], row[6], row[7], row[8],
                            row[13], row[16], row[20]));
                }
            } catch (Exception e) {
                log.warn("Talent.dbc load failed: {}", e.getMessage());
            }
        }
        Path tabFile = dataDir.resolve("dbc").resolve("TalentTab.dbc");
        if (Files.isRegularFile(tabFile)) {
            try {
                DbcFile dbc = DbcFile.load(tabFile);
                for (int[] row : dbc.records) {
                    if (row.length < 21 || row[0] == 0) {
                        continue;
                    }
                    talentTabs.put(row[0], new TalentTab(row[0], row[20]));
                }
            } catch (Exception e) {
                log.warn("TalentTab.dbc load failed: {}", e.getMessage());
            }
        }
    }

    private void seedTalents() {
        talents.putIfAbsent(124, new Talent(124, 161, 0, 0, 12282, 12663, 12664, 0, 0, 0, 0, 0));
        talentTabs.putIfAbsent(161, new TalentTab(161, 1));
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
            it.quality = t.quality;
            applyWeaponProto(it, t);
            if (it.durability <= 0) {
                it.durability = t.maxDurability;
            }
        }
    }

    /** CMaNGOS LearnDefaultSkills from playercreateinfo_skills. Languages are 300/300. */
    public void applyCreateSkills(Player p) {
        if (p == null) {
            return;
        }
        if (!createSkills.isEmpty()) {
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
        if (p.clazz == 3 && p.skillValue(Content.SKILL_SKINNING) < 1) {
            p.learnSkill(Content.SKILL_SKINNING, 1, Math.max(1, p.level * 5), 0);
        }
    }

    /** Cooking / First Aid / Fishing at 1/75 so the professions tab can level. After language slots. */
    public void grantCreateSecondaries(Player p) {
        if (p == null) {
            return;
        }
        grantCreateSecondary(p, Content.SKILL_FIRST_AID);
        grantCreateSecondary(p, Content.SKILL_COOKING);
        grantCreateSecondary(p, Content.SKILL_FISHING);
    }

    private static void grantCreateSecondary(Player p, int skill) {
        if (p.skillValue(skill) < 1) {
            p.learnSkill(skill, 1, 75, 0);
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
        if (p.clazz == 3) {
            storeCreateItem(p, Content.ITEM_SKINNING_KNIFE, 1, nextGuid);
        }
        applyEquippedMelee(p);
    }

    /** Classless / custom start kits — equip or backpack listed item ids then recalc melee. */
    public void giveNamedStartItems(Player p, int[] itemIds, LongSupplier nextGuid) {
        if (p == null || nextGuid == null || itemIds == null) {
            return;
        }
        for (int itemId : itemIds) {
            storeCreateItem(p, itemId, 1, nextGuid);
        }
        applyEquippedMelee(p);
    }

    /**
     * CMaNGOS CanStoreNewItem / StoreNewItem: fill existing stacks up to {@code stackable}, then
     * new backpack slots. Returns each touched stack (merged or created). Missing templates are
     * treated as non-stackable (loot rows may reference entries before a seed exists).
     */
    public List<StoredItem> storeNewItem(Player p, int itemId, int count, LongSupplier nextGuid) {
        List<StoredItem> out = new ArrayList<>();
        if (p == null || nextGuid == null || count <= 0) {
            return out;
        }
        ItemTemplate t = items.get(itemId);
        int left = count;
        int maxStack = t == null ? 1 : Math.max(1, t.stackable);
        if (maxStack > 1) {
            for (int slot = Player.INVENTORY_SLOT_ITEM_START; slot < Player.INVENTORY_SLOT_ITEM_END && left > 0; slot++) {
                Item existing = p.itemAt(0, slot);
                if (existing == null || existing.entry != itemId || existing.count >= maxStack) {
                    continue;
                }
                int room = maxStack - existing.count;
                int add = Math.min(room, left);
                existing.count += add;
                left -= add;
                out.add(new StoredItem(existing, add, false));
            }
        }
        while (left > 0) {
            int bagSlot = firstFreeBackpack(p);
            if (bagSlot < 0) {
                break;
            }
            int stack = Math.min(left, maxStack);
            Item it = new Item(nextGuid.getAsLong(), itemId);
            it.ownerGuid = Guid.low(p.guid);
            it.bag = 0;
            it.slot = bagSlot;
            it.count = stack;
            if (t != null) {
                it.displayId = t.displayId;
                it.quality = t.quality;
                applyWeaponProto(it, t);
                it.durability = t.maxDurability;
            }
            p.items.put(Guid.low(it.guid), it);
            p.setGuid(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + bagSlot * 2,
                    Guid.HIGH_ITEM | (Guid.low(it.guid) & 0xFFFFFFFFL));
            out.add(new StoredItem(it, stack, true));
            left -= stack;
        }
        if (!out.isEmpty()) {
            p.dirty = true;
        }
        return out;
    }

    private void ensureStackable(int entry, int minStack) {
        ItemTemplate t = items.get(entry);
        if (t != null && t.stackable < minStack) {
            t.stackable = minStack;
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
        if (left > 0) {
            storeNewItem(p, itemId, left, nextGuid);
        }
    }

    /** First free viable equipment slot, else the first viable slot (swap). */
    public int destEquipSlot(Player p, int inventoryType) {
        int free = firstFreeEquipSlot(p, inventoryType);
        if (free >= 0) {
            return free;
        }
        int[] slots = equipSlots(inventoryType);
        return slots.length == 0 ? -1 : slots[0];
    }

    private static void addCreateItem(Player p, ItemTemplate t, int count, int slot, LongSupplier nextGuid) {
        Item it = new Item(nextGuid.getAsLong(), t.entry);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = slot;
        it.count = Math.max(1, count);
        it.displayId = t.displayId;
        it.quality = t.quality;
        applyWeaponProto(it, t);
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

    /**
     * Race starter from {@code playercreateinfo}: prefer warrior (class 1), else any row for the race,
     * else human warrior. Used by classless spawn (BE has no warrior row).
     */
    public CreateInfo createForRace(int race) {
        CreateInfo warrior = createInfo.get(key(race, 1));
        if (warrior != null) {
            return warrior;
        }
        for (var e : createInfo.entrySet()) {
            if ((int) (e.getKey() >> 8) == race) {
                return e.getValue();
            }
        }
        return createInfo.get(key(1, 1));
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
        c.spawnDist = s.spawnDist();
        c.movementType = s.movementType();
        c.respawnDelayMs = s.randomRespawnSecs() * 1000;
        c.corpseDelayMs = Math.min(c.respawnDelayMs * 9 / 10,
                org.tbc.world.combat.Combat.corpseDelayForRank(c.rank));
        c.startOocMotion();
        return c;
    }

    public GameObject spawnGameObject(Spawn s) {
        GameObject go = new GameObject();
        go.guid = Guid.HIGH_GAMEOBJECT | (s.guid() & 0xFFFFFFFFL);
        go.entry = s.entry();
        go.mapId = s.map();
        go.relocate(s.x(), s.y(), s.z(), s.o());
        go.setGuid(org.tbc.world.net.wow8606.UpdateFields.OBJECT_FIELD_GUID, go.guid);
        GameObjectTemplate t = gameObjects.get(s.entry());
        if (t != null) {
            go.type = t.type;
            go.displayId = t.displayId;
            go.name = t.name;
        }
        return go;
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
        c.aiName = t.aiName() == null ? "" : t.aiName();
        c.extraFlags = t.extraFlags();
        c.inhabitType = t.inhabitType() > 0 ? t.inhabitType() : org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT;
        c.applyTemplate(entry, t.name(), t.display(), t.faction(), t.hp(), t.level());
        applyHeroWarriorTrainerVirtualItems(c, entry);
        c.applyCombatStats(t.minMeleeDmg(), t.maxMeleeDmg(), t.meleeAttackTime(), combatReach(t));
        applyCreatureMana(c, t);
        c.npcFlags = t.npcFlags();
        c.rank = t.rank();
        c.corpseDelayMs = Math.min(c.respawnDelayMs * 9 / 10,
                org.tbc.world.combat.Combat.corpseDelayForRank(c.rank));
        c.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_NPC_FLAGS, t.npcFlags());
        if (factions != null) {
            org.tbc.world.combat.FactionTemplate ft = factions.template(c);
            c.neutralToAll = ft != null && ft.isNeutralToAll();
        }
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
        } else if (entry == 17849) {
            // ACID Slain Outrunner — Permanent Feign Death on spawn (in-memory / missing SQL row).
            if (c.eventAi == null) {
                c.eventAi = new org.tbc.world.ai.EventAi();
            }
            c.eventAi.load(java.util.List.of(org.tbc.world.ai.EventAi.Script.spawnedCast(
                    SpellEngine.PERMANENT_FEIGN_DEATH,
                    org.tbc.world.ai.EventAi.CAST_FORCE_TARGET_SELF
                            | org.tbc.world.ai.EventAi.CAST_AURA_NOT_PRESENT)));
        }
        fireEventAiSpawned(c);
        return c;
    }

    private static void applyHeroWarriorTrainerVirtualItems(Creature c, int entry) {
        if (entry != org.tbc.world.classless.HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER) {
            return;
        }
        c.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_VIRTUAL_ITEM_SLOT_DISPLAY,
                org.tbc.world.classless.HeroClassUnlock.VIRTUAL_ITEM_SWORD_DISPLAY);
        c.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_VIRTUAL_ITEM_SLOT_DISPLAY + 1,
                org.tbc.world.classless.HeroClassUnlock.VIRTUAL_ITEM_SHIELD_DISPLAY);
    }

    /** CreatureEventAI EVENT_T_SPAWNED — must run before the first CREATE so corpse flags are on the wire. */
    public void fireEventAiSpawned(Creature c) {
        if (c == null || c.eventAi == null) {
            return;
        }
        c.eventAi.onSpawned(c, (cr, t, spellId) -> {
            SpellEngine.SpellInfo info = eventAiSpells.info(spellId);
            if (info != null) {
                eventAiSpells.apply(cr, t == null ? cr : t, info, 0L);
            }
        });
    }

    /**
     * CMaNGOS SelectLevel mana: MinLevelMana × PowerMultiplier, UnitClass mage → POWER_MANA.
     */
    void applyCreatureMana(Creature c, CreatureTemplate t) {
        if (c == null) {
            return;
        }
        int mana = creatureMana.getOrDefault(c.entry, 0);
        if (mana <= 0) {
            return;
        }
        float mult = t == null ? 1f : t.powerMultiplier();
        if (mult > 0f && mult != 1f) {
            mana = Math.max(1, Math.round(mana * mult));
        }
        c.applyMana(mana);
    }

    private float combatReach(CreatureTemplate t) {
        if (t.combatReach() > 0f) {
            return t.combatReach();
        }
        Float fromModel = modelCombatReach.get(t.display());
        if (fromModel != null && fromModel > 0f) {
            return fromModel;
        }
        return 1.5f;
    }

    /**
     * CMaNGOS Loot::FillLoot for a corpse. Negative ChanceOrQuestChance rows are quest-only
     * (LootStoreItem::needs_quest) and require {@link #hasQuestForItem}.
     */
    public void fillCorpseLoot(Creature c) {
        fillCorpseLoot(c, null);
    }

    public void fillCorpseLoot(Creature c, Player lootOwner) {
        if (c == null) {
            return;
        }
        c.lootGold = 0;
        c.lootItems.clear();
        CreatureTemplate t = creatures.get(c.entry);
        int lootId = c.entry;
        int minG = 0;
        int maxG = 0;
        if (t != null) {
            if (t.lootId() != 0) {
                lootId = t.lootId();
            }
            minG = t.minLootGold();
            maxG = t.maxLootGold();
        }
        if (maxG < minG) {
            maxG = minG;
        }
        if (maxG > 0) {
            c.lootGold = minG + java.util.concurrent.ThreadLocalRandom.current().nextInt(maxG - minG + 1);
        }
        fillLootSlots(creatureLoot.get(lootId), lootOwner, c.lootItems);
    }

    /**
     * Fill a gameobject chest loot window from gameobject_loot_template (data[1] loot id).
     * Quest-only rows use the same HasQuestForItem gate as creature loot.
     */
    public void fillGameObjectLoot(GameObject go, Player lootOwner) {
        if (go == null) {
            return;
        }
        go.lootGold = 0;
        go.lootItems.clear();
        GameObjectTemplate t = gameObjects.get(go.entry);
        int lootId = go.entry;
        if (t != null && t.data.length > 1 && t.data[1] != 0) {
            lootId = t.data[1];
        }
        fillLootSlots(gameObjectLoot.get(lootId), lootOwner, go.lootItems);
        go.lootable = !go.lootItems.isEmpty() || go.lootGold > 0;
    }

    private void fillLootSlots(List<LootRow> rows, Player lootOwner,
                               List<org.tbc.world.loot.LootSlot> into) {
        if (rows == null) {
            return;
        }
        int slot = 0;
        for (LootRow row : rows) {
            if (row.minCount() < 0) {
                continue;
            }
            if (row.needsQuest()) {
                if (lootOwner == null || !hasQuestForItem(lootOwner, row.item())) {
                    continue;
                }
            }
            if (row.chance() < 100f
                    && java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 100f >= row.chance()) {
                continue;
            }
            int count = row.minCount();
            if (row.maxCount() > row.minCount()) {
                count += java.util.concurrent.ThreadLocalRandom.current().nextInt(row.maxCount() - row.minCount() + 1);
            }
            ItemTemplate it = items.get(row.item());
            int display = it != null ? it.displayId : 0;
            into.add(new org.tbc.world.loot.LootSlot(slot, row.item(), Math.max(1, count), display));
            slot++;
        }
    }

    /**
     * CMaNGOS Player::HasQuestForItem — incomplete quest still needs this ReqItemId.
     */
    public boolean hasQuestForItem(Player p, int itemId) {
        if (p == null || itemId <= 0) {
            return false;
        }
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == Content.QUEST_STATE_COMPLETE) {
                continue;
            }
            QuestTemplate q = quests.get(questId);
            if (q == null) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                if (q.reqItemId(i) == itemId && p.questLogItemCount[slot][i] < q.reqItemCount(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** ItemPrototype.h ITEM_SPELLTRIGGER_ON_EQUIP. */
    private static final int ITEM_SPELLTRIGGER_ON_EQUIP = 1;
    /** ItemPrototype.h ITEM_MOD_MANA / HEALTH / AGILITY / STRENGTH / INTELLECT / SPIRIT / STAMINA / HIT_RATING. */
    private static final int ITEM_MOD_MANA = 0;
    private static final int ITEM_MOD_HEALTH = 1;
    private static final int ITEM_MOD_AGILITY = 3;
    private static final int ITEM_MOD_STRENGTH = 4;
    private static final int ITEM_MOD_INTELLECT = 5;
    private static final int ITEM_MOD_SPIRIT = 6;
    private static final int ITEM_MOD_STAMINA = 7;
    private static final int ITEM_MOD_DEFENSE_SKILL_RATING = 12;
    private static final int ITEM_MOD_DODGE_RATING = 13;
    private static final int ITEM_MOD_PARRY_RATING = 14;
    private static final int ITEM_MOD_BLOCK_RATING = 15;
    private static final int ITEM_MOD_HIT_MELEE_RATING = 16;
    private static final int ITEM_MOD_HIT_RANGED_RATING = 17;
    private static final int ITEM_MOD_HIT_SPELL_RATING = 18;
    private static final int ITEM_MOD_CRIT_MELEE_RATING = 19;
    private static final int ITEM_MOD_CRIT_RANGED_RATING = 20;
    private static final int ITEM_MOD_CRIT_SPELL_RATING = 21;
    private static final int ITEM_MOD_HASTE_SPELL_RATING = 30;
    private static final int ITEM_MOD_HIT_RATING = 31;
    private static final int ITEM_MOD_CRIT_RATING = 32;
    private static final int ITEM_MOD_RESILIENCE_RATING = 35;
    private static final int ITEM_MOD_HASTE_RATING = 36;
    private static final int ITEM_MOD_EXPERTISE_RATING = 37;
    /** Unit.h CombatRating — ITEM_MOD_HIT_RATING / ITEM_MOD_CRIT_RATING / ITEM_MOD_HASTE_RATING write melee and ranged, not spell. */
    private static final int CR_DEFENSE_SKILL = 1;
    private static final int CR_DODGE = 2;
    private static final int CR_PARRY = 3;
    private static final int CR_BLOCK = 4;
    private static final int CR_HIT_MELEE = 5;
    private static final int CR_HIT_RANGED = 6;
    private static final int CR_HIT_SPELL = 7;
    private static final int CR_CRIT_MELEE = 8;
    private static final int CR_CRIT_RANGED = 9;
    private static final int CR_CRIT_SPELL = 10;
    private static final int CR_CRIT_TAKEN_MELEE = 14;
    private static final int CR_CRIT_TAKEN_RANGED = 15;
    private static final int CR_CRIT_TAKEN_SPELL = 16;
    private static final int CR_HASTE_MELEE = 17;
    private static final int CR_HASTE_RANGED = 18;
    private static final int CR_HASTE_SPELL = 19;
    private static final int CR_EXPERTISE = 23;

    /** UNIT_FIELD_MIN/MAXDAMAGE + BASEATTACKTIME from weapons; STAT2 / RESISTANCES from _ApplyItemBonuses. */
    public void applyEquippedMelee(Player p) {
        if (p == null) {
            return;
        }
        if (p.getFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_COMBATREACH) <= 0f) {
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_COMBATREACH, 1.5f);
        }
        syncEquippedWeaponAttack(p);
        Item offItem = p.itemAt(0, Player.EQUIPMENT_SLOT_OFFHAND);
        ItemTemplate off = equippedTemplate(p, Player.EQUIPMENT_SLOT_OFFHAND);
        if (offItem != null && off != null) {
            offItem.itemClass = off.itemClass;
        }
        // GetWeaponForAttack: Class == ITEM_CLASS_WEAPON only — shields must not keep OH swing fields.
        if (off != null && off.itemClass == Player.ITEM_CLASS_WEAPON && off.dmgMax[0] > 0f) {
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, off.dmgMin[0]);
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE, off.dmgMax[0]);
            p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1, off.delay > 0 ? off.delay : 2000);
        } else {
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, 0f);
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE, 0f);
        }
        int stamina = 0;
        int itemHealth = 0;
        int itemMana = 0;
        int agility = 0;
        int strength = 0;
        int intellect = 0;
        int spirit = 0;
        int armor = 0;
        int holy = 0;
        int fire = 0;
        int nature = 0;
        int frost = 0;
        int shadow = 0;
        int arcane = 0;
        int hitRating = 0;
        int hitMeleeRating = 0;
        int hitRangedRating = 0;
        int spellHitRating = 0;
        int defenseRating = 0;
        int dodgeRating = 0;
        int parryRating = 0;
        int blockRating = 0;
        int critRating = 0;
        int critMeleeRating = 0;
        int critRangedRating = 0;
        int spellCritRating = 0;
        int resilienceRating = 0;
        int hasteRating = 0;
        int spellHasteRating = 0;
        int expertiseRating = 0;
        int shieldBlock = 0;
        float classlessSpeedPenalty = 0f;
        boolean classless = org.tbc.world.classless.ClasslessCharacterPolicy.isClassless(p);
        for (int slot = 0; slot < Player.EQUIPMENT_SLOT_END; slot++) {
            ItemTemplate t = equippedTemplate(p, slot);
            if (t == null) {
                continue;
            }
            if (classless && t.itemClass == Player.ITEM_CLASS_ARMOR) {
                var mods = org.tbc.world.classless.ArmorPenaltyPolicy.forPiece(p, t);
                armor += mods.armor();
                strength += mods.strength();
                agility += mods.agility();
                classlessSpeedPenalty -= mods.speedPenaltyPct() * 100f;
                shieldBlock += t.block;
                holy += t.holyRes;
                fire += t.fireRes;
                nature += t.natureRes;
                frost += t.frostRes;
                shadow += t.shadowRes;
                arcane += t.arcaneRes;
                for (int i = 0; i < t.statType.length; i++) {
                    if (t.statType[i] == ITEM_MOD_STAMINA) {
                        stamina += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_HEALTH) {
                        itemHealth += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_MANA) {
                        itemMana += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_INTELLECT) {
                        intellect += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_SPIRIT) {
                        spirit += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_HIT_RATING) {
                        hitRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_HIT_MELEE_RATING) {
                        hitMeleeRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_HIT_RANGED_RATING) {
                        hitRangedRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_HIT_SPELL_RATING) {
                        spellHitRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_CRIT_SPELL_RATING) {
                        spellCritRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_HASTE_SPELL_RATING) {
                        spellHasteRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_DEFENSE_SKILL_RATING) {
                        defenseRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_DODGE_RATING) {
                        dodgeRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_PARRY_RATING) {
                        parryRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_BLOCK_RATING) {
                        blockRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_CRIT_RATING) {
                        critRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_CRIT_MELEE_RATING) {
                        critMeleeRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_CRIT_RANGED_RATING) {
                        critRangedRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_RESILIENCE_RATING) {
                        resilienceRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_HASTE_RATING) {
                        hasteRating += t.statValue[i];
                    } else if (t.statType[i] == ITEM_MOD_EXPERTISE_RATING) {
                        expertiseRating += t.statValue[i];
                    }
                }
                continue;
            }
            armor += t.armor;
            shieldBlock += t.block;
            holy += t.holyRes;
            fire += t.fireRes;
            nature += t.natureRes;
            frost += t.frostRes;
            shadow += t.shadowRes;
            arcane += t.arcaneRes;
            for (int i = 0; i < t.statType.length; i++) {
                if (t.statType[i] == ITEM_MOD_STAMINA) {
                    stamina += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_HEALTH) {
                    itemHealth += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_MANA) {
                    itemMana += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_AGILITY) {
                    agility += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_STRENGTH) {
                    strength += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_INTELLECT) {
                    intellect += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_SPIRIT) {
                    spirit += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_HIT_RATING) {
                    hitRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_HIT_MELEE_RATING) {
                    hitMeleeRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_HIT_RANGED_RATING) {
                    hitRangedRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_HIT_SPELL_RATING) {
                    spellHitRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_CRIT_SPELL_RATING) {
                    spellCritRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_HASTE_SPELL_RATING) {
                    spellHasteRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_DEFENSE_SKILL_RATING) {
                    defenseRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_DODGE_RATING) {
                    dodgeRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_PARRY_RATING) {
                    parryRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_BLOCK_RATING) {
                    blockRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_CRIT_RATING) {
                    critRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_CRIT_MELEE_RATING) {
                    critMeleeRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_CRIT_RANGED_RATING) {
                    critRangedRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_RESILIENCE_RATING) {
                    resilienceRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_HASTE_RATING) {
                    hasteRating += t.statValue[i];
                } else if (t.statType[i] == ITEM_MOD_EXPERTISE_RATING) {
                    expertiseRating += t.statValue[i];
                }
            }
        }
        p.applyGearBonuses(stamina, armor, agility, strength, intellect, spirit, itemHealth, itemMana);
        p.setEquipmentSpeedPenaltyPct(classless ? classlessSpeedPenalty : 0f);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_RESISTANCES + 1, holy);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_RESISTANCES + 2, fire);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_RESISTANCES + 3, nature);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_RESISTANCES + 4, frost);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_RESISTANCES + 5, shadow);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_RESISTANCES + 6, arcane);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_MELEE, hitRating + hitMeleeRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_RANGED, hitRating + hitRangedRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_SPELL, spellHitRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_DEFENSE_SKILL, defenseRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_DODGE, dodgeRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_PARRY, parryRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_BLOCK, blockRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE, critRating + critMeleeRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_RANGED, critRating + critRangedRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_SPELL, spellCritRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_TAKEN_MELEE, resilienceRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_TAKEN_RANGED, resilienceRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_TAKEN_SPELL, resilienceRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_HASTE_MELEE, hasteRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_HASTE_RANGED, hasteRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_HASTE_SPELL, spellHasteRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + CR_EXPERTISE, expertiseRating);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_SHIELD_BLOCK, shieldBlock);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS, onEquipAttackPower(p));
        applyEquippedItemSpells(p);
        applySpellDamageDoneFields(p);
    }

    /**
     * CMaNGOS UpdateSpellDamageBonus (flat ON_EQUIP contributions). Registers known
     * {@code SPELL_AURA_MOD_DAMAGE_DONE} item spells; unknown ids contribute 0 so POS stays cleared.
     */
    private void applySpellDamageDoneFields(Player p) {
        int holy = 0;
        int fire = 0;
        int nature = 0;
        int frost = 0;
        int shadow = 0;
        int arcane = 0;
        for (int slot = 0; slot < Player.EQUIPMENT_SLOT_END; slot++) {
            ItemTemplate t = equippedTemplate(p, slot);
            if (t == null) {
                continue;
            }
            for (int i = 0; i < t.spellId.length; i++) {
                if (t.spellTrigger[i] != ITEM_SPELLTRIGGER_ON_EQUIP || t.spellId[i] == 0) {
                    continue;
                }
                int[] bySchool = onEquipSpellDamageDone.get(t.spellId[i]);
                if (bySchool == null) {
                    continue;
                }
                holy += bySchool[1];
                fire += bySchool[2];
                nature += bySchool[3];
                frost += bySchool[4];
                shadow += bySchool[5];
                arcane += bySchool[6];
            }
        }
        p.updateSpellDamageBonusDone(holy, fire, nature, frost, shadow, arcane);
    }

    /**
     * ON_EQUIP spell id → damage done per school index 1..6 (index 0 unused).
     * Extend when cataloguing item spells with SPELL_AURA_MOD_DAMAGE_DONE.
     */
    private final Map<Integer, int[]> onEquipSpellDamageDone = new HashMap<>();

    /** Test / content hook — register flat school bonus for an ON_EQUIP spell. */
    public void registerOnEquipSpellDamageDone(int spellId, int school, int amount) {
        if (spellId == 0 || school < 1 || school > 6) {
            return;
        }
        int[] row = onEquipSpellDamageDone.computeIfAbsent(spellId, id -> new int[7]);
        row[school] = amount;
    }

    /** Aura 99 on ON_EQUIP spell 14052 — positive half of UNIT_FIELD_ATTACK_POWER_MODS. */
    private int onEquipAttackPower(Player p) {
        int pos = 0;
        for (int slot = 0; slot < Player.EQUIPMENT_SLOT_END; slot++) {
            ItemTemplate t = equippedTemplate(p, slot);
            if (t == null) {
                continue;
            }
            for (int i = 0; i < t.spellId.length; i++) {
                if (t.spellTrigger[i] == ITEM_SPELLTRIGGER_ON_EQUIP
                        && t.spellId[i] == Content.SPELL_ATTACK_POWER_60) {
                    pos += Content.SPELL_ATTACK_POWER_60_AMOUNT;
                }
            }
        }
        return pos;
    }

    /** Player::ApplyItemEquipSpell — ITEM_SPELLTRIGGER_ON_EQUIP writes UNIT_FIELD_AURA. */
    private void applyEquippedItemSpells(Player p) {
        Set<Integer> wanted = new LinkedHashSet<>();
        for (int slot = 0; slot < Player.EQUIPMENT_SLOT_END; slot++) {
            ItemTemplate t = equippedTemplate(p, slot);
            if (t == null) {
                continue;
            }
            for (int i = 0; i < t.spellId.length; i++) {
                if (t.spellId[i] == 0 || t.spellTrigger[i] != ITEM_SPELLTRIGGER_ON_EQUIP) {
                    continue;
                }
                wanted.add(t.spellId[i]);
            }
        }
        p.syncItemEquipAuras(wanted);
    }

    /**
     * Heal mainhand Item weapon line from template and push UNIT_FIELD attack speed/damage.
     * Cheap enough to call before seal procs so SoR handedness matches the client buff tooltip.
     */
    public void syncEquippedWeaponAttack(Player p) {
        if (p == null) {
            return;
        }
        healEquippedWeaponLine(p, Player.EQUIPMENT_SLOT_MAINHAND);
        healEquippedWeaponLine(p, Player.EQUIPMENT_SLOT_OFFHAND);
        ItemTemplate main = equippedTemplate(p, Player.EQUIPMENT_SLOT_MAINHAND);
        if (main != null && main.dmgMax[0] > 0f) {
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MINDAMAGE, main.dmgMin[0]);
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXDAMAGE, main.dmgMax[0]);
            p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME, main.delay > 0 ? main.delay : 2000);
        } else {
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MINDAMAGE, 1.0f);
            p.setFloat(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXDAMAGE, 3.0f);
            p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        }
    }

    /**
     * Copy subclass / inventoryType / delay / dmg from template. Normalizes InventoryType to
     * {@link SpellEngine#INVTYPE_2HWEAPON} when subclass is a 2H weapon (client $HND / SoR).
     */
    public static void applyWeaponProto(Item it, ItemTemplate t) {
        if (it == null || t == null) {
            return;
        }
        it.itemClass = t.itemClass;
        it.subClass = t.subClass;
        it.inventoryType = t.inventoryType;
        it.delay = t.delay;
        it.dmgMin = t.dmgMin[0];
        it.dmgMax = t.dmgMax[0];
        if (MainhandWeaponStats.isTwoHandSubclass(t.subClass)) {
            it.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        }
    }

    /**
     * Copy inventoryType / delay / dmg from {@link ItemTemplate} onto the equipped {@link Item}
     * whenever the template has a weapon line. Fixes delay-only fills that left inventoryType as
     * 1H (SoR combat log used the 1H formula while the client buff used Item.dbc 2H).
     */
    private void healEquippedWeaponLine(Player p, int slot) {
        Item it = p.itemAt(0, slot);
        if (it == null) {
            return;
        }
        ItemTemplate t = items.get(it.entry);
        if (t == null || t.delay <= 0) {
            return;
        }
        applyWeaponProto(it, t);
    }

    private ItemTemplate equippedTemplate(Player p, int slot) {
        Item it = p.itemAt(0, slot);
        if (it == null) {
            return null;
        }
        return items.get(it.entry);
    }
}
