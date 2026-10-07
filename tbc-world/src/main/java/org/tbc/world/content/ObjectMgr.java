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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    static final int GOSSIP_OPTION_GOSSIP = 1;
    static final int GOSSIP_OPTION_QUESTGIVER = 2;
    static final int GOSSIP_OPTION_VENDOR = 3;
    static final int GOSSIP_OPTION_TAXIVENDOR = 4;
    static final int GOSSIP_OPTION_TRAINER = 5;
    static final int GOSSIP_OPTION_SPIRITHEALER = 6;
    static final int GOSSIP_OPTION_SPIRITGUIDE = 7;
    static final int GOSSIP_OPTION_INNKEEPER = 8;
    static final int GOSSIP_OPTION_BANKER = 9;
    static final int GOSSIP_OPTION_PETITIONER = 10;
    static final int GOSSIP_OPTION_TABARDDESIGNER = 11;
    static final int GOSSIP_OPTION_BATTLEFIELD = 12;
    static final int GOSSIP_OPTION_AUCTIONEER = 13;
    static final int GOSSIP_OPTION_STABLEPET = 14;
    static final int GOSSIP_OPTION_ARMORER = 15;
    static final int GOSSIP_OPTION_UNLEARNTALENTS = 16;
    static final int GOSSIP_OPTION_UNLEARNPETSKILLS = 17;
    static final int GOSSIP_OPTION_BOT = 99;
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

        /** Same template with a new name and primary display. Other columns stay as loaded. */
        public CreatureTemplate withNameAndDisplay(String name, int display) {
            return withEdited(name, display, type, faction);
        }

        /** Name, primary display, creature type, and faction. Other columns stay as loaded. */
        public CreatureTemplate withEdited(String name, int display, int type, int faction) {
            return new CreatureTemplate(entry, name, display, faction, hp, level, npcFlags, scriptName, gossip,
                    trainerType, subName, iconName, display2, display3, display4, typeFlags, type, family, rank,
                    petSpellDataId, healthMultiplier, powerMultiplier, racialLeader, aiName, extraFlags,
                    minMeleeDmg, maxMeleeDmg, meleeAttackTime, combatReach, lootId, minLootGold, maxLootGold,
                    inhabitType);
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

        /** tbc-db trade-good ore / coal (class 7/7). SellPrice is vendor copper; assay payouts stay above this. */
        public static ItemTemplate tradeOre(int entry, String name, int displayId, int buyPrice, int sellPrice) {
            ItemTemplate t = new ItemTemplate();
            t.entry = entry;
            t.itemClass = 7;
            t.subClass = 7;
            t.name = name;
            t.displayId = displayId;
            t.quality = 1;
            t.buyPrice = buyPrice;
            t.sellPrice = sellPrice;
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
    /** creature_template.EquipmentTemplateId by creature entry. Missing or 0 means no virtual gear. */
    public final Map<Integer, Integer> equipmentByEntry = new HashMap<>();
    /** creature_equip_template.entry → equipentry1..3 item ids. */
    public final Map<Integer, int[]> equipmentItems = new HashMap<>();
    public final Map<Integer, GameObjectTemplate> gameObjects = new HashMap<>();
    public final Map<Integer, PageText> pageTexts = new HashMap<>();
    /** Character DB `item_text` (ObjectMgr::GetItemText). */
    public final Map<Integer, String> itemTexts = new HashMap<>();
    public final Map<Integer, NpcText> npcTexts = new HashMap<>();
    public final List<Spawn> spawns = new ArrayList<>();
    /** Creature guids that were loaded from the world `creature` table (not memory seeds). */
    final Set<Integer> dbCreatureGuids = new HashSet<>();
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
            WorldDefaultsSeed.defaults(this);
            WorldDefaultsSeed.queryDefaults(this);
            loadStartOutfit(dataDir);
            loadTalents(dataDir);
            levelStats.loadGt(dataDir);
            DurabilityCosts.load(dataDir);
            skillLineAbilities.loadFromDataDir(dataDir);
            loadPublishedQuestOverlays();
            return;
        }
        try (Connection c = world.get()) {
            try {
                PlayerCreateLoader.load(this, c);
            } catch (Exception e) {
                log.warn("playercreateinfo load failed: {}", e.getMessage());
            }
            levelStats.load(c);
            CreatureLoader.load(this, c);
            eventAiStore.load(c);
            dbScriptStore.load(c);
            SpawnLoader.load(this, c);
            QuestLoader.load(this, c);
            WorldTableLoader.areaTriggers(this, c);
            ItemLoader.load(this, c);
            VendorLoader.load(this, c);
            try {
                GossipLoader.load(this, c);
            } catch (Exception e) {
                log.debug("gossip load skipped: {}", e.getMessage());
            }
            GameObjectLoader.load(this, c);
            WorldTableLoader.weatherAndBattlemasters(this, c);
            TrainerLoader.load(this, c);
        } catch (Exception e) {
            log.warn("ObjectMgr SQL load failed, using defaults: {}", e.getMessage());
            WorldDefaultsSeed.defaults(this);
        }
        if (createInfo.isEmpty()) {
            WorldDefaultsSeed.defaults(this);
        }
        if (createActions.isEmpty()) {
            CreateActionSeed.seed(this);
        }
        // SQL item rows may lack spell columns until loadItemSpells; always force usable seeds.
        mergeUsableItemSpells(ItemTemplate.hearthstone());
        mergeUsableItemSpells(ItemTemplate.toughJerky());
        mergeUsableItemSpells(ItemTemplate.refreshingSpringWater());
        WorldDefaultsSeed.queryDefaults(this);
        loadStartOutfit(dataDir);
        loadTalents(dataDir);
        levelStats.loadGt(dataDir);
        DurabilityCosts.load(dataDir);
        skillLineAbilities.loadFromDataDir(dataDir);
        loadPublishedQuestOverlays();
    }

    public void loadPublishedQuestOverlays() {
        loadPublishedQuestOverlays(Path.of("content", "quests", "published"));
    }

    public void loadPublishedQuestOverlays(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return Files.isRegularFile(p) && (n.endsWith(".yaml") || n.endsWith(".yml"));
                    })
                    .sorted()
                    .toList();
            for (Path file : files) {
                try {
                    applyQuestOverlay(QuestOverlayYaml.read(file));
                } catch (Exception e) {
                    log.warn("quest overlay {}: {}", file, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("quest overlay dir skipped: {}", e.getMessage());
        }
    }

    void applyQuestOverlay(QuestOverlayYaml.Overlay o) {
        if (o == null || o.id <= 0) {
            return;
        }
        quests.put(o.id, new QuestTemplate(
                o.id, o.title, o.minLevel, o.type, o.rewMoney, o.details, o.objectives,
                o.reqCreatureId[0], o.reqCreatureCount[0], o.reqItemId[0], o.reqItemCount[0],
                o.questLevel, 0, o.rewItemId, o.rewItemCount, 0, 0, 0, 0,
                o.reqCreatureId[1], o.reqCreatureCount[1],
                o.reqCreatureId[2], o.reqCreatureCount[2],
                o.reqCreatureId[3], o.reqCreatureCount[3],
                o.reqItemId[1], o.reqItemCount[1], o.reqItemId[2], o.reqItemCount[2],
                o.reqItemId[3], o.reqItemCount[3], o.prevQuestId, o.requiredRaces, o.zoneOrSort));
        if (o.giverNpc != 0) {
            addQuestRelation(questGivers, o.giverNpc, o.id);
        }
        if (o.turnInNpc != 0) {
            addQuestRelation(questInvolved, o.turnInNpc, o.id);
        }
        if (o.rewSpell != 0) {
            questRewSpell.put(o.id, o.rewSpell);
        }
        for (QuestOverlayYaml.CreatureOverlay c : o.creatures) {
            creatures.put(c.entry(), new CreatureTemplate(c.entry(), c.name(), c.display(), c.faction(),
                    100, 1, c.npcFlags(), "", "", 0));
        }
        for (QuestOverlayYaml.SpawnOverlay s : o.spawns) {
            boolean present = false;
            for (Spawn existing : spawns) {
                if (existing.guid() == s.guid()) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                spawns.add(new Spawn(s.guid(), s.entry(), s.map(), s.x(), s.y(), s.z(), s.o(),
                        0f, s.movementType()));
            }
        }
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

    public boolean dbCreature(int guid) {
        return dbCreatureGuids.contains(guid);
    }

    public void markDbCreature(int guid) {
        if (guid != 0) {
            dbCreatureGuids.add(guid);
        }
    }

    public Spawn creatureSpawn(int guid) {
        for (Spawn s : spawns) {
            if (s.guid() == guid) {
                return s;
            }
        }
        return null;
    }

    static void addQuestRelation(Map<Integer, List<Integer>> dest, int entry, int questId) {
        List<Integer> list = dest.computeIfAbsent(entry, k -> new ArrayList<>());
        if (!list.contains(questId)) {
            list.add(questId);
        }
    }

    void mergeUsableItemSpells(ItemTemplate seed) {
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

    /** MapDataContainer::GetBattleMasterBG — 0 if the entry is not a battlemaster. */
    public int battleMasterBgType(int creatureEntry) {
        return battleMasterBg.getOrDefault(creatureEntry, 0);
    }

    /** Pack one playercreateinfo_action row into the 132-slot bar of (race, class). */
    void putCreateAction(int race, int clazz, int button, int action, int type) {
        int[] buttons = createActions.computeIfAbsent((int) key(race, clazz), x -> new int[132]);
        if (button >= 0 && button < 132) {
            buttons[button] = (action & 0xFFFFFF) | ((type & 0xFF) << 24);
        }
    }

    /** Append Hero Battlecaster ranks when SQL loaded the trainer without them. */
    void ensureBattlecasterOnTrainer(int entry) {
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

    void addSpawnIfMissing(int guid, int entry, int map, float x, float y, float z, float o) {
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

    static String nz(String s) {
        return s == null ? "" : s;
    }

    public static long key(int race, int clazz) {
        return ((long) race << 8) | clazz;
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

    void ensureStackable(int entry, int minStack) {
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
            go.spellFocusId = t.data[0];
            go.spellFocusDist = t.data[1];
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
        applyEquipment(c, entry);
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

    /**
     * CMaNGOS Creature::LoadEquipment from the template's EquipmentTemplateId.
     * A missing item id leaves the slot empty. The hero-trainer override still runs after this.
     */
    private void applyEquipment(Creature c, int entry) {
        Integer set = equipmentByEntry.get(entry);
        if (set == null || set <= 0) {
            return;
        }
        int[] slots = equipmentItems.get(set);
        if (slots == null) {
            return;
        }
        for (int i = 0; i < 3 && i < slots.length; i++) {
            int itemId = slots[i];
            if (itemId <= 0) {
                c.clearVirtualItem(i);
                continue;
            }
            ItemTemplate item = items.get(itemId);
            if (item == null) {
                continue;
            }
            c.setVirtualItem(i, item.displayId, item.itemClass, item.subClass, item.unk, item.material,
                    item.inventoryType, item.sheath);
        }
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
