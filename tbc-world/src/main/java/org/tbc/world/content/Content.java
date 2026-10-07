package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.LongSupplier;

/** Gossip, vendor buy, starter quest. Packets: gossip.md, quest.md, inventory-gossip-quest.md. */
public final class Content {
    public static final float INTERACT_RANGE = 5f;
    public static final float GOLDSHIRE_X = -9465f;
    public static final float GOLDSHIRE_Y = 16f;
    public static final float GOLDSHIRE_Z = 57f;
    public static final int UNIT_NPC_FLAG_GOSSIP = 0x1;
    public static final int UNIT_NPC_FLAG_QUESTGIVER = 0x2;
    public static final int UNIT_NPC_FLAG_VENDOR = 0x80;
    public static final int UNIT_NPC_FLAG_TRAINER = 0x10;
    public static final int UNIT_NPC_FLAG_FLIGHTMASTER = 0x2000;
    public static final int UNIT_NPC_FLAG_AUCTIONEER = 0x200000;
    public static final int UNIT_NPC_FLAG_BANKER = Banker.UNIT_NPC_FLAG_BANKER;
    public static final int UNIT_NPC_FLAG_INNKEEPER = 0x00010000;
    /** Unit.h UNIT_NPC_FLAG_REPAIR. */
    public static final int UNIT_NPC_FLAG_REPAIR = 0x00001000;
    /** Unit.h UNIT_NPC_FLAG_PETITIONER. */
    public static final int UNIT_NPC_FLAG_PETITIONER = 0x00040000;
    /** Unit.h UNIT_NPC_FLAG_TABARDDESIGNER. */
    public static final int UNIT_NPC_FLAG_TABARDDESIGNER = 0x00080000;
    /** GossipDef.h DEFAULT_GOSSIP_MESSAGE; menu 0 title text. */
    public static final int DEFAULT_GOSSIP_MESSAGE = 0x00FFFFFF;
    /** NPCHandler.h MAX_GOSSIP_TEXT_OPTIONS. */
    public static final int MAX_GOSSIP_TEXT_OPTIONS = 8;
    /** QueryHandler.cpp missing npc_text fill. */
    public static final String DEFAULT_NPC_TEXT = "Greetings $N";
    /** GossipDef.h GOSSIP_ICON_VENDOR; brown bag. */
    public static final int GOSSIP_ICON_VENDOR = 1;
    /** GossipDef.h GOSSIP_ICON_TAXI; flight. */
    public static final int GOSSIP_ICON_TAXI = 2;
    /** GossipDef.h GOSSIP_ICON_INTERACT_2; innkeeper. */
    public static final int GOSSIP_ICON_INTERACT_2 = 5;
    /** GossipDef.h GOSSIP_ICON_TRAINER; book. */
    public static final int GOSSIP_ICON_TRAINER = 3;
    /** GossipDef.h GOSSIP_ICON_MONEY_BAG; banker. */
    public static final int GOSSIP_ICON_MONEY_BAG = 6;
    /** GossipDef.h GOSSIP_OPTION_GOSSIP. */
    public static final int GOSSIP_OPTION_GOSSIP = 1;
    /** GossipDef.h GOSSIP_OPTION_VENDOR. */
    public static final int GOSSIP_OPTION_VENDOR = 3;
    /** GossipDef.h GOSSIP_OPTION_TAXIVENDOR. */
    public static final int GOSSIP_OPTION_TAXIVENDOR = 4;
    /** GossipDef.h GOSSIP_OPTION_TRAINER. */
    public static final int GOSSIP_OPTION_TRAINER = 5;
    /** GossipDef.h GOSSIP_OPTION_SPIRITHEALER. */
    public static final int GOSSIP_OPTION_SPIRITHEALER = 6;
    /** Unit.h UNIT_NPC_FLAG_SPIRITHEALER. */
    public static final int UNIT_NPC_FLAG_SPIRITHEALER = 0x00004000;
    /** Unit.h UNIT_NPC_FLAG_SPIRITGUIDE. */
    public static final int UNIT_NPC_FLAG_SPIRITGUIDE = 0x00008000;
    /** GossipDef.h GOSSIP_OPTION_BANKER. */
    public static final int GOSSIP_OPTION_BANKER = 9;
    /** GossipDef.h GOSSIP_OPTION_INNKEEPER. */
    public static final int GOSSIP_OPTION_INNKEEPER = 8;
    /** GossipDef.h GOSSIP_OPTION_AUCTIONEER. */
    public static final int GOSSIP_OPTION_AUCTIONEER = 13;
    /** AuctionHouseMgr.cpp GetAuctionHouseEntry faction 12 (human). */
    public static final int AUCTION_HOUSE_HUMAN = 1;
    public static final int NPC_AUCTIONEER_CHILTON = 8670;
    public static final int NPC_OLIVIA_BURNSIDE = 2455;
    /** Stormwind tabard designer (creature_template 5193). */
    public static final int NPC_REBECCA_LAUGHLIN = 5193;
    /** PetitionsHandler.cpp GUILD_CHARTER. */
    public static final int ITEM_GUILD_CHARTER = 5863;
    /** PetitionsHandler.cpp GUILD_CHARTER_COST. */
    public static final int GUILD_CHARTER_COST = 1000;
    /** PetitionsHandler.cpp CHARTER_DISPLAY_ID. */
    public static final int CHARTER_DISPLAY_ID = 16161;
    public static final int GAME_EVENT_MIDSUMMER = 1;
    public static final int NPC_LUMA_SKYMOTHER = 25697;
    public static final int GO_ICE_STONE = 187882;
    public static final int GO_ICE_BLOCK = 188067;
    public static final int AUCTION_LIST_DELAY_MS = 300;
    public static final int EQUIP_ERR_NOT_ENOUGH_MONEY = 29;
    public static final int QUEST_STATE_COMPLETE = 0x1;
    public static final int QUEST_STATE_FAIL = 0x2;
    public static final int QUEST_TYPE_ESCORT = 84;
    /** QuestDef.h MAX_QUEST_LOG_SIZE. */
    public static final int MAX_QUEST_LOG_SIZE = 25;
    /** QuestDef.h dialog marks: floating ? uses REWARD; gossip turn-in row uses REWARD_REP. */
    public static final int DIALOG_STATUS_NONE = 0;
    public static final int DIALOG_STATUS_INCOMPLETE = 3;
    public static final int DIALOG_STATUS_REWARD_REP = 4;
    public static final int DIALOG_STATUS_AVAILABLE = 6;
    public static final int DIALOG_STATUS_REWARD = 8;
    public static final int QUEST_A_THREAT_WITHIN = 783;
    /** quest_template 2158 Rest and Relaxation; RewItemId1 159 × 5. */
    public static final int QUEST_REST_AND_RELAXATION = 2158;
    /** quest_template 7 Kobold Camp Cleanup; ReqCreatureOrGOId1 6, ReqCreatureOrGOCount1 10. */
    public static final int QUEST_KOBOLD_CAMP_CLEANUP = 7;
    /** quest_template 18 Brotherhood of Thieves; ReqItemId1 752, ReqItemCount1 12. */
    public static final int QUEST_BROTHERHOOD_OF_THIEVES = 18;
    public static final int NPC_KOBOLD_VERMIN = 6;
    public static final int NPC_CORINA_STEELE = 54;
    public static final int NPC_MARSHAL_MCBRIDE = 197;
    public static final int NPC_MARSHAL_DUGHAN = 240;
    public static final int NPC_DEPUTY_WILLEM = 823;
    public static final int NPC_LLANE_BESHERE = 911;
    /** Northshire mage trainer (TrainerClass mage). */
    public static final int NPC_KHELDEN_BREMEN = 1985;
    /** Goldshire blacksmith trainer. */
    public static final int NPC_DANE_LINDGREN = 1103;
    public static final int NPC_DUNGAR_LONGDRINK = 352;
    public static final int NPC_INNKEEPER_FARLEY = 295;
    /** creature_template GossipMenuId for Farley 295. */
    public static final int GOSSIP_MENU_FARLEY = 1291;
    /** gossip_menu 1291 text_id. */
    public static final int GOSSIP_TEXT_FARLEY = 820;
    /** gossip_menu_option action_menu_id for Farley inn-info. */
    public static final int GOSSIP_MENU_FARLEY_INN_INFO = 1221;
    /** gossip_menu 1221 text_id. */
    public static final int GOSSIP_TEXT_FARLEY_INN_INFO = 1853;
    /** gossip_menu_option.option_text on Farley menu 1291. */
    public static final String GOSSIP_FARLEY_INN_INFO = "What can I do at an inn?";
    public static final int ITEM_WORN_SHORTSWORD = 25;
    public static final int ITEM_SKINNING_KNIFE = 7005;
    /** tbc-db item_template 821; stat_type1 STAMINA 7 / stat_value1 2, armor 65. */
    public static final int ITEM_RIVERPAW_LEATHER_VEST = 821;
    /** tbc-db item_template 2041; stat_type1 AGILITY 3 / 11, stat_type2 STAMINA 7 / 5, armor 92. */
    public static final int ITEM_TUNIC_OF_WESTFALL = 2041;
    /** tbc-db item_template 3306; stat_type1 STRENGTH 4 / 4, stat_type2 STAMINA 7 / 3, armor 162. */
    public static final int ITEM_BRACKWATER_VEST = 3306;
    /** tbc-db item_template 2981; stat_type1 INTELLECT 5 / 6, stat_type2 SPIRIT 6 / 3, armor 35. */
    public static final int ITEM_SEERS_ROBE = 2981;
    /** tbc-db item_template 10399; STR 4 / AGI 3 / STA 11, armor 92. */
    public static final int ITEM_BLACKENED_DEFIAS_ARMOR = 10399;
    /** tbc-db item_template 16726; STR 13 / STA 21 / INT 16 / SPI 8, armor 657. */
    public static final int ITEM_LIGHTFORGE_BREASTPLATE = 16726;
    /** tbc-db item_template 16853; STR 8 / STA 26 / INT 21 / SPI 13, armor 855, FireRes 10. */
    public static final int ITEM_LAWBRINGER_CHESTGUARD = 16853;
    /** tbc-db item_template 15059; STA 10 / SPI 25, armor 169, NatureRes 5. */
    public static final int ITEM_LIVING_BREASTPLATE = 15059;
    /** tbc-db item_template 22669; STR 12 / STA 24, armor 1027, FrostRes 42. */
    public static final int ITEM_ICEBANE_BREASTPLATE = 22669;
    /** tbc-db item_template 32404; STA 54, armor 1428, ShadowRes 72. */
    public static final int ITEM_SHADESTEEL_GREAVES = 32404;
    /** tbc-db item_template 21865; STA 24 / INT 20 / SPI 16, armor 170, ArcaneRes 45. */
    public static final int ITEM_SOULCLOTH_VEST = 21865;
    /** tbc-db item_template 2801; STR/AGI/STA/INT/SPI 11, dmg 101–152, delay 2100. */
    public static final int ITEM_BLADE_OF_HANNA = 2801;
    /** tbc-db item_template 30113; STR 25 / AGI 26 / STA 57 / DEF 27 / DODGE 24 / HIT 24, armor 1668. */
    public static final int ITEM_DESTROYER_CHESTGUARD = 30113;
    /** tbc-db item_template 30118; STR 50 / STA 48 / CRIT 33 / HIT 15, armor 1668. */
    public static final int ITEM_DESTROYER_BREASTPLATE = 30118;
    /** tbc.cavernoftime.com item 24544; STA 49 / STR 23 / CRIT 30 / RES 23 / HIT 12, armor 1547. */
    public static final int ITEM_GLADIATORS_PLATE_CHESTPIECE = 24544;
    /** tbc-db item_template 30976; AGI 37 / STA 69 / DEF 37 / PARRY 28 / BLOCK 23, armor 1825. */
    public static final int ITEM_ONSLAUGHT_CHESTGUARD = 30976;
    /** tbc.cavernoftime.com item 34215; STR 61 / STA 67 / CRIT 41 / HASTE 32, armor 1983. */
    public static final int ITEM_WARHARNESS_OF_RECKLESS_FURY = 34215;
    /** tbc.cavernoftime.com item 32280; STA 70 / DEF 32 / EXPERTISE 21, armor 1103, hands. */
    public static final int ITEM_GAUNTLETS_OF_ENFORCEMENT = 32280;
    /** tbc-db item_template 33675; STA/STR/INT/AGI/RES/HIT + CRIT 19 in stat_type7, armor 529. */
    public static final int ITEM_VENGEFUL_GLADIATORS_DRAGONHIDE_TUNIC = 33675;
    /** tbc-db item_template 29341; INT 24 / ITEM_MOD_HIT_SPELL_RATING 18 / 23, armor 136, robe. */
    public static final int ITEM_AUCHENAI_ANCHORITES_ROBE = 29341;
    /** tbc-db item_template 34229; STA 48 / INT 41 / ITEM_MOD_CRIT_SPELL_RATING 21 / 25, armor 1110. */
    public static final int ITEM_GARMENTS_OF_SERENE_SHORES = 34229;
    /** tbc-db item_template 34212; STA 48 / INT 41 / ITEM_MOD_HASTE_SPELL_RATING 30 / 33, armor 499. */
    public static final int ITEM_SUNGLOW_VEST = 34212;
    /** tbc-db item_template 2362; shield InventoryType 14, block 1, armor 5. */
    public static final int ITEM_WORN_WOODEN_SHIELD = 2362;
    /** tbc-db item_template 33122; STA 25 / ITEM_MOD_CRIT_MELEE_RATING 19 / 24 / STR 23, armor 101. */
    public static final int ITEM_CLOAK_OF_DARKNESS = 33122;
    /** tbc-db item_template 30318; ITEM_MOD_CRIT_RANGED_RATING 20 / 50 / STA 20, bow. */
    public static final int ITEM_NETHERSTRAND_LONGBOW = 30318;
    /** tbc-db item_template 18582; physical 148–155 plus shadow 40–60 and arcane 40–60. */
    public static final int ITEM_TWIN_BLADES_OF_AZZINOTH = 18582;
    /** tbc-db item_template 1259; five damage lines including frost 1000 and shadow 4000. */
    public static final int ITEM_JYOO_TEST_ITEM = 1259;
    /** tbc-db item_template 32954; STR 30 / STA 43 / CRIT 23 / HIT_MELEE 15 / HIT_RANGED 15, armor 997. */
    public static final int ITEM_TOMS_BOOTS_1 = 32954;
    /** tbc-db item_template 6673; ITEM_MOD_HEALTH 1 / −60, finger. */
    /** Test seed — HolyRes 10 for UNIT_FIELD_RESISTANCES+1 (SPELL_SCHOOL_HOLY). */
    public static final int ITEM_TEST_HOLY_RESIST_VEST = 6675;
    /** tbc-db item_template 6673; ITEM_MOD_HEALTH 10 for +HP. */
    public static final int ITEM_TEST_HP_RING = 6673;
    /** tbc-db item_template 6674; ITEM_MOD_MANA 0 / −60, finger. */
    public static final int ITEM_TEST_MP_RING = 6674;
    /** tbc-db item_template 29301; ON_EQUIP Attack Power 60 (spell 14052). */
    public static final int ITEM_BAND_OF_THE_ETERNAL_CHAMPION = 29301;
    /** spell_template 14052; EFFECT_APPLY_AURA SPELL_AURA_MOD_ATTACK_POWER 99 / 59. */
    public static final int SPELL_ATTACK_POWER_60 = 14052;
    /** Spell.dbc 14052 EffectBasePoints 59; Aura amount is base points + 1. */
    public static final int SPELL_ATTACK_POWER_60_AMOUNT = 60;
    /** QuestDef.h QUEST_REWARD_CHOICES_COUNT. */
    public static final int QUEST_REWARD_CHOICES_COUNT = 6;
    /** locales_item 2224 Militia Dagger; quest 18 RewChoiceItemId1. */
    public static final int ITEM_MILITIA_DAGGER = 2224;
    /** locales_item 5580 Militia Hammer; quest 18 RewChoiceItemId2. */
    public static final int ITEM_MILITIA_HAMMER = 5580;
    /** locales_item 159 Refreshing Spring Water; quest 2158 RewItemId1. */
    public static final int ITEM_REFRESHING_SPRING_WATER = 159;
    /** locales_item 752 Red Burlap Bandana; quest 18 ReqItemId1. */
    public static final int ITEM_RED_BURLAP_BANDANA = 752;
    /** locales_item 6948 Hearthstone; item_template spellid_1 8690. */
    public static final int ITEM_HEARTHSTONE = 6948;
    /** locales_item 117 Tough Jerky; item_template spellid_1 433 charges −1. */
    public static final int ITEM_TOUGH_JERKY = 117;
    /** locales_item 118 Minor Healing Potion; item_template spellid_1 439 charges −1. */
    public static final int ITEM_MINOR_HEALING_POTION = 118;
    /** Tough Hunk of Bread — stackable food (item_template.stackable 20). */
    public static final int ITEM_TOUGH_HUNK_OF_BREAD = 4540;
    public static final int ITEM_ROUGH_ARROW = 2512;
    public static final int ITEM_SMALL_BROWN_POUCH = 4496;
    /** locales_item 889; PageText 16 is locales_page_text (Stalvan to Crillian). */
    public static final int ITEM_DUSTY_UNSENT_LETTER = 889;
    public static final int PAGE_TEXT_STALVAN_CRILLIAN = 16;
    /** locales_item 6351 Dented Crate / Venture Co. supplies. */
    public static final int ITEM_DENTED_CRATE = 6351;
    /** locales_item 5042 Red Ribboned Wrapping Paper. */
    public static final int ITEM_RED_RIBBONED_WRAPPING_PAPER = 5042;
    /** ItemPrototype.h ITEM_FLAG_IS_WRAPPER; named in inventory.md. */
    public static final int ITEM_FLAG_IS_WRAPPER = 0x00000200;
    /** Item.h ITEM_DYNFLAG_UNLOCKED; locked items after EffectOpenLock. */
    public static final int ITEM_DYNFLAG_UNLOCKED = 0x00000004;
    /** Item.h ITEM_DYNFLAG_WRAPPED; named in inventory.md. */
    public static final int ITEM_DYNFLAG_WRAPPED = 0x00000008;
    public static final int SPELL_BATTLE_SHOUT = 6673;
    /** Next Battle Shout rank for trainer reqAbility chain (Spell.dbc 5242). */
    public static final int SPELL_BATTLE_SHOUT_RANK2 = 5242;
    public static final int TRAINER_SPELL_BATTLE_SHOUT_COST = 200;
    /** Spell.dbc Fireball rank 1 — mage starter trainer row. */
    public static final int SPELL_FIREBALL = 133;
    public static final int TRAINER_SPELL_FIREBALL_COST = 10;
    public static final int SPELL_APPRENTICE_BLACKSMITH = 2020;
    public static final int TRAINER_SPELL_APPRENTICE_BLACKSMITH_COST = 10;
    public static final int SKILL_BLACKSMITHING = 164;
    public static final int SKILL_HERBALISM = 182;
    public static final int SKILL_MINING = 186;
    public static final int SKILL_FISHING = 356;
    public static final int SKILL_FIRST_AID = 129;
    public static final int SKILL_COOKING = 185;
    public static final int SKILL_SKINNING = 393;
    public static final int SKILL_LOCKPICKING = 633;
    public static final int SKILL_JEWELCRAFTING = 755;
    public static final int TAXI_STORMWIND = 2;
    public static final int TAXI_IRONFORGE = 6;
    public static final int ERR_TAXIOK = 0;
    public static final int ERR_TAXINOTVISITED = 6;
    public static final int ZONE_ELWYNN = 12;
    public static final int WEATHER_STATE_FINE = 0;
    /** Weather.h WEATHER_STATE_LIGHT_RAIN. */
    public static final int WEATHER_STATE_LIGHT_RAIN = 3;
    public static final int WEATHER_INSTANT_SMOOTH = 0;
    public static final int ERR_BANKSLOT_FAILED_TOO_MANY = Banker.ERR_BANKSLOT_FAILED_TOO_MANY;
    public static final int ERR_BANKSLOT_INSUFFICIENT_FUNDS = Banker.ERR_BANKSLOT_INSUFFICIENT_FUNDS;
    public static final int ERR_BANKSLOT_NOTBANKER = Banker.ERR_BANKSLOT_NOTBANKER;
    public static final int ERR_BANKSLOT_OK = Banker.ERR_BANKSLOT_OK;
    public static final int[] BANK_BAG_SLOT_PRICES = Banker.BANK_BAG_SLOT_PRICES;
    public static final int BACKPACK_START = 23;
    public static final int BACKPACK_END = 39;

    private final ObjectMgr mgr;
    private final Vendor vendor;
    private final QuestGiver quests;
    private final Gossip gossip;

    public Content(ObjectMgr mgr) {
        this.mgr = mgr;
        this.quests = new QuestGiver(mgr);
        this.vendor = new Vendor(mgr, quests::itemAddedQuestCheck);
        this.gossip = new Gossip(mgr, quests, vendor);
    }

    public void gossipHello(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        gossip.gossipHello(p, map, in, send);
    }

    public void gossipSelect(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        gossip.gossipSelect(p, map, in, send);
    }

    public void listInventory(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        vendor.listInventory(p, map, in, send);
    }

    public void buy(Player p, GameMap map, WowBuffer in, boolean inSlot, LongSupplier nextItemGuid,
                    BiConsumer<Integer, byte[]> send) {
        vendor.buy(p, map, in, inSlot, nextItemGuid, send);
    }

    public void queryQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        quests.queryQuest(p, map, in, send);
    }

    public void requestReward(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        quests.requestReward(p, map, in, send);
    }

    public void questGiverStatusQuery(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        quests.questGiverStatusQuery(p, map, in, send);
    }

    public void sendQuestGiverStatus(Player p, Creature c, BiConsumer<Integer, byte[]> send) {
        quests.sendQuestGiverStatus(p, c, send);
    }

    public void questGiverStatusMultiple(Player p, GameMap map, BiConsumer<Integer, byte[]> send) {
        quests.questGiverStatusMultiple(p, map, send);
    }

    boolean canTake(Player p, ObjectMgr.QuestTemplate q) {
        return quests.canTake(p, q);
    }

    boolean objectivesMet(Player p, int slot, ObjectMgr.QuestTemplate q) {
        return quests.objectivesMet(p, slot, q);
    }

    public void acceptQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        quests.acceptQuest(p, map, in, send);
    }

    public void removeQuest(Player p, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        quests.removeQuest(p, in, send);
    }

    public void completeQuest(Player p, GameMap map, WowBuffer in, LongSupplier nextItemGuid,
                              BiConsumer<Integer, byte[]> send) {
        quests.completeQuest(p, map, in, nextItemGuid, send);
    }

    public void destroyItemCount(Player p, int itemId, int count, BiConsumer<Integer, byte[]> send) {
        quests.destroyItemCount(p, itemId, count, send);
    }

    /**
     * Player::GetNPCIfCanInteractWith → IsWithinDistInMap(INTERACTION_DISTANCE): 3D distance
     * against INTERACTION_DISTANCE plus both combat reaches.
     */
    public static boolean outOfRange(Player p, Creature c) {
        float maxDist = INTERACT_RANGE + p.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH)
                + c.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH);
        double dx = p.x - c.x;
        double dy = p.y - c.y;
        double dz = p.z - c.z;
        return dx * dx + dy * dy + dz * dz >= (double) maxDist * maxDist;
    }

    public static Creature creature(GameMap map, long guid) {
        return guid == 0 ? null : map.creatures.get(guid);
    }

    boolean goGives(int entry, int questId) {
        return quests.goGives(entry, questId);
    }

    boolean goInvolves(int entry, int questId) {
        return quests.goInvolves(entry, questId);
    }

    public boolean useGameObject(Player p, GameMap map, GameObject go, BiConsumer<Integer, byte[]> send) {
        return quests.useGameObject(p, map, go, send);
    }

    public void exploreAreaTrigger(Player p, GameMap map, int triggerId, BiConsumer<Integer, byte[]> send) {
        quests.exploreAreaTrigger(p, map, triggerId, send);
    }

    public void spellCastCredit(Player p, GameMap map, int spellId, BiConsumer<Integer, byte[]> send) {
        quests.spellCastCredit(p, map, spellId, send);
    }

    public void tickEscort(Player p, GameMap map, BiConsumer<Integer, byte[]> send) {
        quests.tickEscort(p, map, send);
    }

    public void failExpired(Player p, long nowMs, BiConsumer<Integer, byte[]> send) {
        quests.failExpired(p, nowMs, send);
    }

    public void resetDailies(Player p) {
        quests.resetDailies(p);
    }

    boolean gives(int entry, int questId) {
        return quests.gives(entry, questId);
    }

    boolean involves(int entry, int questId) {
        return quests.involves(entry, questId);
    }

    boolean offersOrInvolves(int entry, int questId) {
        return quests.offersOrInvolves(entry, questId);
    }

    static int slotOf(Player p, int questId) {
        for (int i = 0; i < 25; i++) {
            if (p.questLogId[i] == questId) {
                return i;
            }
        }
        return -1;
    }

    static int freeSlot(Player p) {
        for (int i = 0; i < 25; i++) {
            if (p.questLogId[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    static int nextBackpackSlot(Player p) {
        for (int slot = BACKPACK_START; slot < BACKPACK_END; slot++) {
            if (!slotOccupied(p, slot)) {
                return slot;
            }
        }
        return -1;
    }

    static boolean slotOccupied(Player p, int slot) {
        for (Item it : p.items.values()) {
            if (it.bag == 0 && it.slot == slot) {
                return true;
            }
        }
        return false;
    }

    public void killedMonsterCredit(Player p, GameMap map, Creature victim, BiConsumer<Integer, byte[]> send) {
        quests.killedMonsterCredit(p, map, victim, send);
    }

    public void creatureHitCredit(Player p, GameMap map, Creature victim, BiConsumer<Integer, byte[]> send) {
        quests.creatureHitCredit(p, map, victim, send);
    }

    public ObjectMgr.EmoteNearNpcObjective creditTextEmoteNearNpc(Player p, GameMap map, int textEmote,
                                           BiConsumer<Integer, byte[]> send) {
        return quests.creditTextEmoteNearNpc(p, map, textEmote, send);
    }

    public void itemAddedQuestCheck(Player p, GameMap map, int entry, int count, BiConsumer<Integer, byte[]> send) {
        quests.itemAddedQuestCheck(p, map, entry, count, send);
    }

    public static void syncQuestLogFields(Player p) {
        QuestGiver.syncQuestLogFields(p);
    }

    static void writeLogField(Player p, int slot) {
        QuestGiver.writeLogField(p, slot);
    }

    byte[] encodeVendorList(Creature c) {
        return vendor.encodeVendorList(c);
    }

    static byte[] encodePush(Player p, Item it, int count) {
        return encodePush(p, it, count, 1, count, it.slot);
    }

    public static byte[] encodeLootPush(Player p, Item it, int inventoryTotal) {
        return encodePush(p, it, it.count, 0, inventoryTotal, it.slot);
    }

    static byte[] encodePush(Player p, Item it, int count, int received, int inventoryTotal) {
        return encodePush(p, it, count, received, inventoryTotal, it.slot);
    }

    public static byte[] encodePush(Player p, Item it, int count, int received, int inventoryTotal, int itemSlot) {
        WowBuffer b = new WowBuffer(48);
        b.putU64(p.guid);
        b.putU32(received);
        b.putU32(0);
        b.putU32(1);
        b.putU8(it.bag);
        b.putU32(itemSlot);
        b.putU32(it.entry);
        b.putU32(0);
        b.putU32(0);
        b.putU32(count);
        b.putU32(inventoryTotal);
        return b.array();
    }

    static byte[] encodeEquipErr(int result) {
        WowBuffer b = new WowBuffer(18);
        b.putU8(result);
        b.putU64(0);
        b.putU64(0);
        b.putU8(0);
        return b.array();
    }

    static byte[] u32(int v) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(v);
        return b.array();
    }
}
