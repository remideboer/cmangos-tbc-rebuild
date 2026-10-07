package org.tbc.world.content;

import org.tbc.world.content.ObjectMgr.ItemTemplate;
import org.tbc.world.spell.SpellEngine;

/**
 * Hard-coded item_template rows the in-memory world seeds without a DB (CMaNGOS item_template
 * subset). Pure factories: each call returns a fresh {@link ItemTemplate}.
 */
public final class ItemSeed {

    private ItemSeed() {
    }

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
