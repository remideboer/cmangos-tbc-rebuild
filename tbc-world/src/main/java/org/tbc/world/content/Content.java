package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.ReputationMgr;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.AuctionHandler;
import org.tbc.world.session.InventoryHandler;
import org.tbc.world.session.TaxiHandler;
import org.tbc.world.session.TrainerHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public static final int UNIT_NPC_FLAG_BANKER = 0x00020000;
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
    /** Player.h BuyBankSlotResult. */
    public static final int ERR_BANKSLOT_FAILED_TOO_MANY = 0;
    public static final int ERR_BANKSLOT_INSUFFICIENT_FUNDS = 1;
    public static final int ERR_BANKSLOT_NOTBANKER = 2;
    public static final int ERR_BANKSLOT_OK = 3;
    /**
     * BankBagSlotPrices.dbc id → copper. Index 0 unused.
     * Slot 1..7: 10s, 1g, 10g, 25g, 50g, 100g, 200g.
     */
    public static final int[] BANK_BAG_SLOT_PRICES = {0, 1000, 10_000, 100_000, 250_000, 500_000, 1_000_000, 2_000_000};
    public static final int BACKPACK_START = 23;
    public static final int BACKPACK_END = 39;

    private final ObjectMgr mgr;

    public Content(ObjectMgr mgr) {
        this.mgr = mgr;
    }

    public void gossipHello(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c)) {
            return;
        }
        if (p.ghost && (c.npcFlags & (UNIT_NPC_FLAG_SPIRITHEALER | UNIT_NPC_FLAG_SPIRITGUIDE)) == 0) {
            return;
        }
        send.accept(Opcodes.SMSG_GOSSIP_MESSAGE, encodeGossip(p, c));
    }

    public void gossipSelect(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 16) {
            return;
        }
        long guid = in.getU64();
        int menuId = in.getU32();
        int gossipListId = in.getU32();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c)) {
            return;
        }
        if (!p.hasGossipOption(menuId, gossipListId)) {
            return;
        }
        int option = p.gossipOptionId(gossipListId);
        if (option == GOSSIP_OPTION_VENDOR) {
            if ((c.npcFlags & UNIT_NPC_FLAG_VENDOR) == 0) {
                return;
            }
            send.accept(Opcodes.SMSG_LIST_INVENTORY, encodeVendorList(c));
        } else if (option == GOSSIP_OPTION_TRAINER) {
            TrainerHandler.sendList(p, c, mgr, send);
        } else if (option == GOSSIP_OPTION_BANKER) {
            InventoryHandler.sendShowBank(c, send);
        } else if (option == GOSSIP_OPTION_TAXIVENDOR) {
            TaxiHandler.sendMenu(p, c, mgr, send);
        } else if (option == GOSSIP_OPTION_INNKEEPER) {
            if ((c.npcFlags & UNIT_NPC_FLAG_INNKEEPER) == 0) {
                return;
            }
            send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
            send.accept(Opcodes.SMSG_BINDER_CONFIRM, encodeBinderConfirm(c));
        } else if (option == GOSSIP_OPTION_AUCTIONEER) {
            if ((c.npcFlags & UNIT_NPC_FLAG_AUCTIONEER) == 0) {
                return;
            }
            AuctionHandler.sendHello(c, send);
        } else if (option == GOSSIP_OPTION_SPIRITHEALER) {
            if (!p.ghost && p.alive()) {
                return;
            }
            WowBuffer confirm = new WowBuffer(8);
            confirm.putU64(c.guid);
            send.accept(Opcodes.SMSG_SPIRIT_HEALER_CONFIRM, confirm.array());
        } else if (option == GOSSIP_OPTION_GOSSIP) {
            int poiId = p.gossipActionPoi(gossipListId);
            if (poiId != 0) {
                ObjectMgr.PointOfInterest poi = mgr.pointsOfInterest.get(poiId);
                if (poi != null) {
                    send.accept(Opcodes.SMSG_GOSSIP_POI, encodeGossipPoi(poi));
                }
            }
            int next = p.gossipActionMenu(gossipListId);
            if (next > 0) {
                send.accept(Opcodes.SMSG_GOSSIP_MESSAGE, encodeGossip(p, c, next));
            } else if (next < 0) {
                send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
            }
        }
    }

    public void listInventory(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c) || (c.npcFlags & UNIT_NPC_FLAG_VENDOR) == 0) {
            return;
        }
        send.accept(Opcodes.SMSG_LIST_INVENTORY, encodeVendorList(c));
    }

    public void buy(Player p, GameMap map, WowBuffer in, boolean inSlot, LongSupplier nextItemGuid,
                    BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long vendor = in.getU64();
        int itemId = in.getU32();
        int requestedSlot = -1;
        if (inSlot) {
            if (in.remaining() >= 9) {
                in.getU64();
                requestedSlot = in.getU8() & 0xFF;
            }
        }
        int count = in.remaining() > 0 ? Math.max(1, in.getU8()) : 1;
        Creature c = creature(map, vendor);
        if (c == null || outOfRange(p, c) || (c.npcFlags & UNIT_NPC_FLAG_VENDOR) == 0) {
            return;
        }
        List<Integer> stock = mgr.itemsForVendor(c.entry);
        if (stock.isEmpty() || !stock.contains(itemId)) {
            return;
        }
        ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
        if (t == null) {
            return;
        }
        int price = t.buyPrice * count;
        if (p.money < price) {
            send.accept(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE, encodeEquipErr(EQUIP_ERR_NOT_ENOUGH_MONEY));
            return;
        }
        LongSupplier guids = nextItemGuid;
        if (guids == null) {
            return;
        }
        // Prefer merge into existing stacks (CMaNGOS CanStore); requestedSlot only when empty non-stack.
        List<ObjectMgr.StoredItem> stored;
        if (inSlot && requestedSlot >= BACKPACK_START && requestedSlot < BACKPACK_END
                && !slotOccupied(p, requestedSlot) && t.stackable <= 1) {
            Item it = new Item(guids.getAsLong(), itemId);
            it.ownerGuid = Guid.low(p.guid);
            it.bag = 0;
            it.slot = requestedSlot;
            it.count = count;
            it.displayId = t.displayId;
            it.quality = t.quality;
            ObjectMgr.applyWeaponProto(it, t);
            p.items.put(Guid.low(it.guid), it);
            p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + requestedSlot * 2,
                    Guid.HIGH_ITEM | (Guid.low(it.guid) & 0xFFFFFFFFL));
            stored = List.of(new ObjectMgr.StoredItem(it, count, true));
        } else {
            stored = mgr.storeNewItem(p, itemId, count, guids);
        }
        if (stored.isEmpty()) {
            return;
        }
        p.setMoney(p.money - price);
        p.dirty = true;
        int total = 0;
        for (Item x : p.items.values()) {
            if (x.entry == itemId) {
                total += x.count;
            }
        }
        boolean anyCreated = false;
        for (ObjectMgr.StoredItem s : stored) {
            if (s.created()) {
                anyCreated = true;
                var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(s.item(), p.guid));
                send.accept(created.opcode(), created.payload());
                int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + s.item().slot * 2;
                var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
                send.accept(inv.opcode(), inv.payload());
            } else {
                var stack = UpdateBuilder.maybeCompress(
                        UpdateBuilder.valuesItem(s.item(), UpdateFields.ITEM_FIELD_STACK_COUNT));
                send.accept(stack.opcode(), stack.payload());
            }
        }
        var coin = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
        send.accept(coin.opcode(), coin.payload());
        int vendorSlot = stock.indexOf(itemId) + 1;
        WowBuffer bought = new WowBuffer(20);
        bought.putU64(c.guid);
        bought.putU32(vendorSlot);
        bought.putU32(0xFFFFFFFF);
        bought.putU32(count);
        send.accept(Opcodes.SMSG_BUY_ITEM, bought.array());
        Item pushItem = stored.get(0).item();
        // storeNewItem appends creates after merges, so the last entry is the new slot when anyCreated.
        int pushSlot = anyCreated ? stored.get(stored.size() - 1).item().slot : 0xFFFFFFFF;
        send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodePush(p, pushItem, count, 1, total, pushSlot));
        itemAddedQuestCheck(p, map, itemId, count, send);
    }

    public void queryQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        long giverGuid = resolveQuestGiverGuid(p, map, guid, questId, true);
        if (giverGuid == 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null) {
            return;
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, encodeDetails(giverGuid, q));
    }

    /** HandleQuestgiverRequestRewardOpcode → PlayerMenu::SendQuestGiverOfferReward. */
    public void requestReward(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        long giverGuid = resolveQuestGiverGuid(p, map, guid, questId, false);
        if (giverGuid == 0) {
            return;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null || !readyToTurnIn(p, slot, q)) {
            return;
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD, encodeOfferReward(giverGuid, q));
    }

    /**
     * CMSG_QUESTGIVER_STATUS_QUERY. GossipDef.cpp SendQuestGiverStatus; QuestHandler.cpp getDialogStatus.
     * Missing questgiver: no packet. Found: raw guid + uint8 status.
     */
    public void questGiverStatusQuery(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = creature(map, guid);
        if (c == null) {
            GameObject go = map.gameObjects.get(guid);
            if (go == null) {
                return;
            }
            sendQuestGiverStatus(p, go.guid, goDialogStatus(p, go.entry), send);
            return;
        }
        sendQuestGiverStatus(p, c, send);
    }

    /** GossipDef.cpp SendQuestGiverStatus. Visible questgivers, not only those inside interact range. */
    public void sendQuestGiverStatus(Player p, Creature c, BiConsumer<Integer, byte[]> send) {
        WowBuffer out = new WowBuffer(9);
        out.putU64(c.guid);
        out.putU8(dialogStatus(p, c));
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS, out.array());
    }

    private void sendQuestGiverStatus(Player p, long guid, int status, BiConsumer<Integer, byte[]> send) {
        WowBuffer out = new WowBuffer(9);
        out.putU64(guid);
        out.putU8(status);
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS, out.array());
    }

    /**
     * CMSG_QUESTGIVER_STATUS_MULTIPLE_QUERY. Player.cpp SendQuestGiverStatusMultiple:
     * questgivers inside visibility, plus any questgiver the client already has
     * ({@code m_clientGUIDs} / session seen). Raw guid + uint8 status; count prefix.
     * Recv ignored. A game-object questgiver answers CMSG_QUESTGIVER_STATUS_QUERY.
     */
    public void questGiverStatusMultiple(Player p, GameMap map, BiConsumer<Integer, byte[]> send) {
        Map<Long, Creature> found = new LinkedHashMap<>();
        for (Creature c : map.nearbyCreatures(p, GameMap.VISIBILITY)) {
            if ((c.npcFlags & UNIT_NPC_FLAG_QUESTGIVER) != 0) {
                found.put(c.guid, c);
            }
        }
        if (p.session != null) {
            for (long guid : p.session.seenGuids()) {
                Creature c = creature(map, guid);
                if (c != null && (c.npcFlags & UNIT_NPC_FLAG_QUESTGIVER) != 0) {
                    found.putIfAbsent(c.guid, c);
                }
            }
        }
        WowBuffer out = new WowBuffer(4 + found.size() * 9);
        out.putU32(found.size());
        for (Creature c : found.values()) {
            out.putU64(c.guid);
            out.putU8(dialogStatus(p, c));
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE, out.array());
    }

    /**
     * QuestHandler.cpp getDialogStatus. Higher QuestDef.h value wins
     * (reward 8, available 6, incomplete 3).
     */
    int dialogStatus(Player p, Creature c) {
        int best = DIALOG_STATUS_NONE;
        best = raiseStatus(p, c.entry, mgr.questInvolved.get(c.entry), true, best);
        best = raiseStatus(p, c.entry, mgr.questGivers.get(c.entry), false, best);
        return best;
    }

    private int goDialogStatus(Player p, int entry) {
        int best = DIALOG_STATUS_NONE;
        best = raiseStatus(p, entry, mgr.goQuestInvolved.get(entry), true, best);
        best = raiseStatus(p, entry, mgr.goQuestGivers.get(entry), false, best);
        return best;
    }

    private int raiseStatus(Player p, int entry, List<Integer> quests, boolean involved, int best) {
        if (quests == null) {
            return best;
        }
        for (int questId : quests) {
            int status = relationStatus(p, entry, questId, involved);
            if (status > best) {
                best = status;
            }
        }
        return best;
    }

    private int relationStatus(Player p, int entry, int questId, boolean involved) {
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null) {
            return DIALOG_STATUS_NONE;
        }
        int slot = slotOf(p, questId);
        if (slot >= 0) {
            if (readyToTurnIn(p, slot, q)) {
                return involved ? DIALOG_STATUS_REWARD : DIALOG_STATUS_NONE;
            }
            return DIALOG_STATUS_INCOMPLETE;
        }
        if (!involved && canTake(p, q)) {
            return DIALOG_STATUS_AVAILABLE;
        }
        return DIALOG_STATUS_NONE;
    }

    /** Player::CanTakeQuest for level, race, previous quest, and already rewarded. */
    boolean canTake(Player p, ObjectMgr.QuestTemplate q) {
        if (p.rewardedQuests.contains(q.id())) {
            return false;
        }
        if (p.level < q.minLevel()) {
            return false;
        }
        if (!raceMatches(p, q.requiredRaces())) {
            return false;
        }
        if (!repAndDailyAllow(p, q)) {
            return false;
        }
        if (org.tbc.world.classless.HeroClassUnlock.isHeroOnly(q.id())
                && !org.tbc.world.classless.ClasslessCharacterPolicy.isClassless(p)) {
            return false;
        }
        return prevSatisfied(p, q.prevQuestId());
    }

    private boolean repAndDailyAllow(Player p, ObjectMgr.QuestTemplate q) {
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(q.id());
        if (extra != null && extra.isDaily() && p.dailyQuestDone.contains(q.id())) {
            return false;
        }
        if (extra != null && extra.reqRepFaction() > 0 && p.reputationStanding(extra.reqRepFaction()) < extra.reqRepValue()) {
            return false;
        }
        return true;
    }

    static boolean raceMatches(Player p, int requiredRaces) {
        if (requiredRaces == 0) {
            return true;
        }
        int bit = p.race <= 0 ? 0 : 1 << (p.race - 1);
        return (requiredRaces & bit) != 0;
    }

    static boolean prevSatisfied(Player p, int prevQuestId) {
        if (prevQuestId == 0) {
            return true;
        }
        if (prevQuestId > 0) {
            return p.rewardedQuests.contains(prevQuestId);
        }
        return slotOf(p, -prevQuestId) >= 0;
    }

    /**
     * CMaNGOS getDialogStatus uses QUEST_STATUS_COMPLETE, not a live objective recount.
     * Spell/event complete can set the log bit before counters catch up.
     */
    boolean readyToTurnIn(Player p, int slot, ObjectMgr.QuestTemplate q) {
        return p.questLogState[slot] == QUEST_STATE_COMPLETE || objectivesMet(p, slot, q);
    }

    /** Player::CompleteQuest — log bit, QUESTUPDATE_COMPLETE, refresh nearby ? / !. */
    private void markQuestObjectivesComplete(Player p, GameMap map, int slot, int questId,
                                             BiConsumer<Integer, byte[]> send) {
        p.questLogState[slot] = QUEST_STATE_COMPLETE;
        writeLogField(p, slot);
        send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
        sendLogUpdate(p, slot, send);
        questGiverStatusMultiple(p, map, send);
    }

    /** Creature, item, game-object, spell, and explore objectives. */
    boolean objectivesMet(Player p, int slot, ObjectMgr.QuestTemplate q) {
        for (int i = 0; i < 4; i++) {
            if (q.reqCreatureOrGOCount(i) > 0) {
                if (q.reqCreatureOrGOId(i) != 0) {
                    if (p.questLogCounts[slot][i] < q.reqCreatureOrGOCount(i)) {
                        return false;
                    }
                }
            }
            if (q.reqItemCount(i) > 0) {
                if (q.reqItemId(i) > 0) {
                    if (p.questLogItemCount[slot][i] < q.reqItemCount(i)) {
                        return false;
                    }
                }
            }
        }
        ObjectMgr.CreatureHitObjective hits = mgr.questCreatureHits.get(q.id());
        if (hits != null && p.questLogCounts[slot][1] < hits.count()) {
            return false;
        }
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(q.id());
        if (extra == null) {
            return true;
        }
        boolean event = extra.reqSpell1() > 0 || extra.exploreOrEvent() || q.type() == QUEST_TYPE_ESCORT;
        if (!event) {
            return true;
        }
        return p.questLogCounts[slot][0] >= 1;
    }

    public void acceptQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        Creature c = creature(map, guid);
        GameObject go = null;
        int giverEntry;
        if (c != null) {
            if (outOfRange(p, c) || !gives(c.entry, questId)) {
                return;
            }
            giverEntry = c.entry;
        } else {
            go = map.gameObjects.get(guid);
            if (go == null || p.distance2d(go) > INTERACT_RANGE || !goGives(go.entry, questId)) {
                return;
            }
            giverEntry = go.entry;
        }
        ObjectMgr.QuestTemplate taken = mgr.quests.get(questId);
        if (taken == null || !repAndDailyAllow(p, taken)) {
            return;
        }
        if (org.tbc.world.classless.HeroClassUnlock.isHeroOnly(questId)
                && !org.tbc.world.classless.ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        if (slotOf(p, questId) >= 0) {
            return;
        }
        int slot = freeSlot(p);
        if (slot < 0) {
            send.accept(Opcodes.SMSG_QUESTLOG_FULL, new byte[0]);
            return;
        }
        clearQuestSlot(p, slot);
        p.questLogId[slot] = questId;
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
        if (extra != null && extra.limitSeconds() > 0) {
            p.questExpiry[slot] = System.currentTimeMillis() + extra.limitSeconds() * 1000L;
        }
        if (c != null && taken.type() == QUEST_TYPE_ESCORT) {
            c.followTarget = p.guid;
        }
        writeLogField(p, slot);
        sendLogUpdate(p, slot, send);
        send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
        if (objectivesMet(p, slot, taken)) {
            markQuestObjectivesComplete(p, map, slot, questId, send);
        } else if (c != null) {
            sendQuestGiverStatus(p, c, send);
        } else {
            sendQuestGiverStatus(p, go.guid, goDialogStatus(p, giverEntry), send);
        }
    }

    /** HandleQuestLogRemoveQuest → SetQuestSlot(slot, 0). */
    public void removeQuest(Player p, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 1) {
            return;
        }
        int slot = in.getU8();
        if (slot >= MAX_QUEST_LOG_SIZE) {
            return;
        }
        clearQuestSlot(p, slot);
        writeLogField(p, slot);
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base, base + 1, base + 2));
        send.accept(upd.opcode(), upd.payload());
    }

    public void completeQuest(Player p, GameMap map, WowBuffer in, LongSupplier nextItemGuid,
                              BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        int reward = 0;
        if (in.remaining() >= 4) {
            reward = in.getU32();
        }
        if (reward >= QUEST_REWARD_CHOICES_COUNT) {
            return;
        }
        if (resolveQuestGiverGuid(p, map, guid, questId, false) == 0) {
            return;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null || !readyToTurnIn(p, slot, q)) {
            return;
        }
        if (org.tbc.world.classless.HeroClassUnlock.isHeroOnly(questId) && !p.alive()) {
            return;
        }
        // Player::RewardQuest — DestroyItemCount ReqItemId/ReqItemCount before rewards.
        for (int i = 0; i < 4; i++) {
            int reqId = q.reqItemId(i);
            if (reqId > 0) {
                destroyItemCount(p, reqId, q.reqItemCount(i), send);
            }
        }
        p.questLogState[slot] = QUEST_STATE_COMPLETE;
        send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
        int xp;
        int money = q.rewMoney();
        if (p.level < Player.MAX_LEVEL) {
            xp = QuestXp.xpValue(p.level, q.questLevel(), q.rewMoneyMaxLevel());
            int[] changed = p.giveXp(xp, null);
            if (changed.length > 0) {
                var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, changed));
                send.accept(upd.opcode(), upd.payload());
            }
        } else {
            xp = 0;
            money += q.rewMoneyMaxLevel();
        }
        p.setMoney(p.money + money);
        storeRewardItem(p, q.rewItemId1(), q.rewItemCount1(), nextItemGuid, send);
        storeRewardItem(p, q.rewChoiceItemId(reward), q.rewChoiceItemCount(reward), nextItemGuid, send);
        p.rewardedQuests.add(questId);
        int rewSpell = mgr.questRewSpell.getOrDefault(questId, 0);
        if (rewSpell > 0 && !p.spells.contains(rewSpell)) {
            p.spells.add(rewSpell);
            WowBuffer learned = new WowBuffer(4);
            learned.putU32(rewSpell);
            send.accept(Opcodes.SMSG_LEARNED_SPELL, learned.array());
        }
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
        if (extra != null && extra.rewRepFaction() > 0) {
            p.modifyReputation(extra.rewRepFaction(), extra.rewRepValue());
            byte[] standingPkt = p.reputations.encodeStandingUpdate(
                    ReputationMgr.listIdForFaction(extra.rewRepFaction()));
            if (standingPkt.length > 0) {
                send.accept(Opcodes.SMSG_SET_FACTION_STANDING, standingPkt);
            }
        }
        if (extra != null && extra.isDaily()) {
            p.dailyQuestDone.add(questId);
        }
        clearQuestSlot(p, slot);
        writeLogField(p, slot);
        sendLogUpdate(p, slot, send);
        send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE, encodeQuestComplete(q, xp, money, 0));
        questGiverStatusMultiple(p, map, send);
    }

    private void storeRewardItem(Player p, int itemId, int count, LongSupplier nextItemGuid,
                                 BiConsumer<Integer, byte[]> send) {
        if (itemId <= 0 || count <= 0) {
            return;
        }
        int bagSlot = nextBackpackSlot(p);
        if (bagSlot < 0) {
            return;
        }
        long itemGuid = nextItemGuid.getAsLong();
        if (itemGuid == 0) {
            return;
        }
        Item it = new Item(itemGuid, itemId);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = bagSlot;
        it.count = count;
        ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
        if (t != null) {
            it.displayId = t.displayId;
            it.quality = t.quality;
            ObjectMgr.applyWeaponProto(it, t);
        }
        p.items.put(Guid.low(it.guid), it);
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + bagSlot * 2, UpdateBuilder.itemGuid(it));
        p.dirty = true;
        var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(it, p.guid));
        send.accept(created.opcode(), created.payload());
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
        send.accept(inv.opcode(), inv.payload());
        send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodePush(p, it, it.count));
    }

    /**
     * Player::DestroyItemCount(entry, count, update=true). Walks backpack stacks matching
     * {@code itemId}, reducing or removing until {@code count} is consumed.
     */
    void destroyItemCount(Player p, int itemId, int count, BiConsumer<Integer, byte[]> send) {
        if (itemId <= 0 || count <= 0) {
            return;
        }
        int left = count;
        List<Item> stacks = new ArrayList<>();
        for (Item it : p.items.values()) {
            if (it.entry == itemId && it.bag == 0) {
                stacks.add(it);
            }
        }
        for (Item it : stacks) {
            if (left <= 0) {
                break;
            }
            if (it.count <= left) {
                left -= it.count;
                p.items.remove(Guid.low(it.guid));
                int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
                p.setGuid(field, 0);
                p.dirty = true;
                var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
                send.accept(inv.opcode(), inv.payload());
                WowBuffer destroy = new WowBuffer(8);
                destroy.putU64(UpdateBuilder.itemGuid(it));
                send.accept(Opcodes.SMSG_DESTROY_OBJECT, destroy.array());
            } else {
                it.count -= left;
                left = 0;
                p.dirty = true;
                var stack = UpdateBuilder.maybeCompress(
                        UpdateBuilder.valuesItem(it, UpdateFields.ITEM_FIELD_STACK_COUNT));
                send.accept(stack.opcode(), stack.payload());
            }
        }
    }

    public static boolean outOfRange(Player p, Creature c) {
        return p.distance2d(c) > INTERACT_RANGE;
    }

    public static Creature creature(GameMap map, long guid) {
        return guid == 0 ? null : map.creatures.get(guid);
    }

    boolean goGives(int entry, int questId) {
        List<Integer> list = mgr.goQuestGivers.get(entry);
        return list != null && list.contains(questId);
    }

    boolean goInvolves(int entry, int questId) {
        List<Integer> list = mgr.goQuestInvolved.get(entry);
        return list != null && list.contains(questId);
    }

    /** Creature or game-object questgiver in range; {@code offer} allows giver or involved. */
    private long resolveQuestGiverGuid(Player p, GameMap map, long guid, int questId, boolean offer) {
        Creature c = creature(map, guid);
        if (c != null) {
            if (outOfRange(p, c) || !questNpcRelated(c.entry, questId, offer, false)) {
                return 0;
            }
            return c.guid;
        }
        GameObject go = map.gameObjects.get(guid);
        if (go == null || p.distance2d(go) > INTERACT_RANGE
                || !questNpcRelated(go.entry, questId, offer, true)) {
            return 0;
        }
        return go.guid;
    }

    private boolean questNpcRelated(int entry, int questId, boolean offer, boolean gameObject) {
        if (gameObject) {
            if (offer && goGives(entry, questId)) {
                return true;
            }
            return goInvolves(entry, questId);
        }
        if (offer) {
            return offersOrInvolves(entry, questId);
        }
        return involves(entry, questId);
    }

    /** Using a questgiver object opens its quest. Using an objective object credits the negative id. */
    public boolean useGameObject(Player p, GameMap map, GameObject go, BiConsumer<Integer, byte[]> send) {
        if (go == null || p.distance2d(go) > INTERACT_RANGE) {
            return false;
        }
        boolean credited = creditGameObject(p, map, go, send);
        List<Integer> offered = mgr.goQuestGivers.get(go.entry);
        if (offered == null) {
            return credited;
        }
        for (int questId : offered) {
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            if (q != null && canTake(p, q)) {
                send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, encodeDetails(go.guid, q));
                return true;
            }
        }
        return credited;
    }

    public void exploreAreaTrigger(Player p, GameMap map, int triggerId, BiConsumer<Integer, byte[]> send) {
        Integer questId = mgr.areaTriggerQuests.get(triggerId);
        if (questId == null) {
            return;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null) {
            return;
        }
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
        if (extra == null || !extra.exploreOrEvent()) {
            return;
        }
        if (p.questLogCounts[slot][0] > 0) {
            return;
        }
        p.questLogCounts[slot][0] = 1;
        finishObjective(p, map, slot, q, q.id(), send);
    }

    public void spellCastCredit(Player p, GameMap map, int spellId, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
            if (extra == null || extra.reqSpell1() != spellId || p.questLogCounts[slot][0] > 0) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            if (q == null) {
                continue;
            }
            p.questLogCounts[slot][0] = 1;
            finishObjective(p, map, slot, q, spellId, send);
        }
    }

    public void tickEscort(Player p, GameMap map, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
            if (q == null || q.type() != QUEST_TYPE_ESCORT || extra == null || p.questLogCounts[slot][0] > 0) {
                continue;
            }
            double dx = p.x - extra.pointX();
            double dy = p.y - extra.pointY();
            if (dx * dx + dy * dy > 25) {
                continue;
            }
            p.questLogCounts[slot][0] = 1;
            finishObjective(p, map, slot, q, questId, send);
        }
    }

    public void failExpired(Player p, long nowMs, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questExpiry[slot] == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            if (nowMs < p.questExpiry[slot]) {
                continue;
            }
            p.questLogState[slot] = QUEST_STATE_FAIL;
            WowBuffer fail = new WowBuffer(4);
            fail.putU32(questId);
            send.accept(Opcodes.SMSG_QUESTUPDATE_FAILEDTIMER, fail.array());
        }
    }

    public void resetDailies(Player p) {
        for (int questId : p.dailyQuestDone) {
            p.rewardedQuests.remove(questId);
        }
        p.dailyQuestDone.clear();
    }

    private boolean creditGameObject(Player p, GameMap map, GameObject go, BiConsumer<Integer, byte[]> send) {
        boolean credited = false;
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            if (q == null) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                int req = q.reqCreatureOrGOId(i);
                if (req >= 0 || req != -go.entry) {
                    continue;
                }
                int have = p.questLogCounts[slot][i];
                int need = q.reqCreatureOrGOCount(i);
                if (have >= need) {
                    continue;
                }
                p.questLogCounts[slot][i] = have + 1;
                finishObjective(p, map, slot, q, req, send);
                credited = true;
            }
        }
        return credited;
    }

    private void finishObjective(Player p, GameMap map, int slot, ObjectMgr.QuestTemplate q, int objectiveId,
                                 BiConsumer<Integer, byte[]> send) {
        WowBuffer add = new WowBuffer(24);
        add.putU32(q.id());
        add.putU32(objectiveId);
        add.putU32(p.questLogCounts[slot][0]);
        add.putU32(1);
        add.putU64(0);
        send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
        writeLogField(p, slot);
        sendLogUpdate(p, slot, send);
        if (objectivesMet(p, slot, q)) {
            p.questLogState[slot] = QUEST_STATE_COMPLETE;
            writeLogField(p, slot);
            sendLogUpdate(p, slot, send);
            send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(q.id()));
            questGiverStatusMultiple(p, map, send);
        }
    }

    boolean gives(int entry, int questId) {
        List<Integer> list = mgr.questGivers.get(entry);
        return list != null && list.contains(questId);
    }

    boolean involves(int entry, int questId) {
        List<Integer> list = mgr.questInvolved.get(entry);
        return list != null && list.contains(questId);
    }

    boolean offersOrInvolves(int entry, int questId) {
        return gives(entry, questId) || involves(entry, questId);
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

    /**
     * Player.cpp KilledMonsterCredit / SendQuestUpdateAddCreatureOrGo.
     * Creature objective slots 1–4. Game-object objectives (negative ids) are later.
     */
    public void killedMonsterCredit(Player p, GameMap map, Creature victim, BiConsumer<Integer, byte[]> send) {
        if (victim == null) {
            return;
        }
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            if (q == null) {
                continue;
            }
            int objective = creditCreature(p, slot, q, victim.entry);
            if (objective < 0) {
                continue;
            }
            int cur = p.questLogCounts[slot][objective];
            int reqCount = q.reqCreatureOrGOCount(objective);
            WowBuffer add = new WowBuffer(24);
            add.putU32(questId);
            add.putU32(q.reqCreatureOrGOId(objective));
            add.putU32(cur);
            add.putU32(reqCount);
            add.putU64(victim.guid);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
            writeLogField(p, slot);
            boolean done = objectivesMet(p, slot, q);
            if (done) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
            }
            sendLogUpdate(p, slot, send);
            if (done) {
                questGiverStatusMultiple(p, map, send);
            }
        }
    }

    /**
     * Landed melee hits on a creature objective (not a kill). Misses and spells do not call this.
     */
    public void creatureHitCredit(Player p, GameMap map, Creature victim, BiConsumer<Integer, byte[]> send) {
        if (victim == null) {
            return;
        }
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.CreatureHitObjective hits = mgr.questCreatureHits.get(questId);
            if (hits == null || hits.creatureEntry() != victim.entry) {
                continue;
            }
            int cur = p.questLogCounts[slot][1];
            if (cur >= hits.count()) {
                continue;
            }
            cur++;
            p.questLogCounts[slot][1] = cur;
            WowBuffer add = new WowBuffer(24);
            add.putU32(questId);
            add.putU32(hits.creatureEntry());
            add.putU32(cur);
            add.putU32(hits.count());
            add.putU64(victim.guid);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
            writeLogField(p, slot);
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            boolean done = q != null && objectivesMet(p, slot, q);
            if (done) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
            }
            sendLogUpdate(p, slot, send);
            if (done) {
                questGiverStatusMultiple(p, map, send);
            }
        }
    }

    /** First matching creature slot that still needs a kill, or -1. */
    private static int creditCreature(Player p, int slot, ObjectMgr.QuestTemplate q, int entry) {
        for (int i = 0; i < 4; i++) {
            int reqId = q.reqCreatureOrGOId(i);
            int reqCount = q.reqCreatureOrGOCount(i);
            if (reqId <= 0 || reqId != entry) {
                continue;
            }
            int cur = p.questLogCounts[slot][i];
            if (cur >= reqCount) {
                continue;
            }
            p.questLogCounts[slot][i] = cur + 1;
            return i;
        }
        return -1;
    }

    /**
     * Player.cpp ItemAddedQuestCheck / SendQuestUpdateAddItem.
     * Item objective slots 1–4. Packet is item u32 + added count u32 — not quest id.
     */
    public void itemAddedQuestCheck(Player p, GameMap map, int entry, int count, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            if (q == null) {
                continue;
            }
            int[] added = new int[1];
            int objective = creditItem(p, slot, q, entry, count, added);
            if (objective < 0) {
                continue;
            }
            WowBuffer pkt = new WowBuffer(8);
            pkt.putU32(q.reqItemId(objective));
            pkt.putU32(added[0]);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM, pkt.array());
            if (objectivesMet(p, slot, q)) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
                sendLogUpdate(p, slot, send);
                questGiverStatusMultiple(p, map, send);
            }
        }
    }

    /** First matching item slot still short of its count, or -1. added[0] is the clamped amount. */
    private static int creditItem(Player p, int slot, ObjectMgr.QuestTemplate q, int entry, int count, int[] added) {
        for (int i = 0; i < 4; i++) {
            int reqId = q.reqItemId(i);
            int reqCount = q.reqItemCount(i);
            if (reqId <= 0 || reqId != entry) {
                continue;
            }
            int cur = p.questLogItemCount[slot][i];
            if (cur >= reqCount) {
                continue;
            }
            int add = cur + count <= reqCount ? count : reqCount - cur;
            p.questLogItemCount[slot][i] = cur + add;
            added[0] = add;
            return i;
        }
        return -1;
    }

    static void clearQuestSlot(Player p, int slot) {
        p.questLogId[slot] = 0;
        p.questLogState[slot] = 0;
        p.questLogCounts[slot][0] = 0;
        p.questLogCounts[slot][1] = 0;
        p.questLogCounts[slot][2] = 0;
        p.questLogCounts[slot][3] = 0;
        p.questLogItemCount[slot][0] = 0;
        p.questLogItemCount[slot][1] = 0;
        p.questExpiry[slot] = 0;
        p.questLogItemCount[slot][2] = 0;
        p.questLogItemCount[slot][3] = 0;
    }

    private void sendLogUpdate(Player p, int slot, BiConsumer<Integer, byte[]> send) {
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base, base + 1, base + 2));
        send.accept(upd.opcode(), upd.payload());
    }

    /** Mirror questLog* arrays into PLAYER_QUEST_LOG_* before create-self / VALUES. */
    public static void syncQuestLogFields(Player p) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            writeLogField(p, slot);
        }
    }

    static void writeLogField(Player p, int slot) {
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        p.setInt(base, p.questLogId[slot]);
        p.setInt(base + 1, p.questLogState[slot]);
        int packed = (p.questLogCounts[slot][0] & 0xFF)
                | ((p.questLogCounts[slot][1] & 0xFF) << 8)
                | ((p.questLogCounts[slot][2] & 0xFF) << 16)
                | ((p.questLogCounts[slot][3] & 0xFF) << 24);
        p.setInt(base + 2, packed);
        p.setInt(base + 3, 0);
    }

    private List<Integer> gossipQuests(Player p, Creature c) {
        List<Integer> out = new ArrayList<>();
        for (int id : mgr.questGivers.getOrDefault(c.entry, List.of())) {
            if (includeGossipQuest(p, c.entry, id, true)) {
                out.add(id);
            }
        }
        for (int id : mgr.questInvolved.getOrDefault(c.entry, List.of())) {
            if (!out.contains(id) && includeGossipQuest(p, c.entry, id, false)) {
                out.add(id);
            }
        }
        return out;
    }

    private boolean includeGossipQuest(Player p, int entry, int questId, boolean fromGiver) {
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null) {
            return true;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return fromGiver && canTake(p, q);
        }
        // Incomplete / complete rows come from involved relation (Player::PrepareQuestMenu).
        return involves(entry, questId);
    }

    byte[] encodeGossip(Player p, Creature c) {
        return encodeGossip(p, c, mgr.gossipMenuId(c.entry));
    }

    byte[] encodeGossip(Player p, Creature c, int menuId) {
        List<ObjectMgr.GossipMenuItem> items = mgr.gossipOptionsFor(p, c, menuId);
        int[] optionIds = new int[items.size()];
        int[] actionMenus = new int[items.size()];
        int[] actionPois = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            optionIds[i] = items.get(i).optionId();
            actionMenus[i] = items.get(i).actionMenu();
            actionPois[i] = items.get(i).actionPoi();
        }
        p.prepareGossipMenu(menuId, optionIds, actionMenus, actionPois);
        List<Integer> quests = gossipQuests(p, c);
        WowBuffer b = new WowBuffer(64);
        b.putU64(c.guid);
        b.putU32(menuId);
        b.putU32(mgr.gossipTextId(menuId));
        b.putU32(items.size());
        int index = 0;
        for (ObjectMgr.GossipMenuItem it : items) {
            b.putU32(index++);
            b.putU8(it.icon());
            b.putU8(it.coded());
            b.putU32(it.boxMoney());
            b.putCString(it.text());
            b.putCString(it.boxText());
        }
        b.putU32(quests.size());
        for (int id : quests) {
            ObjectMgr.QuestTemplate q = mgr.quests.get(id);
            b.putU32(id);
            b.putU32(questMenuIcon(p, id, q));
            b.putU32(q == null ? 1 : shownQuestLevel(q));
            b.putCString(q == null ? "" : q.title());
        }
        return b.array();
    }

    static byte[] encodeGossipPoi(ObjectMgr.PointOfInterest poi) {
        WowBuffer b = new WowBuffer(24 + poi.iconName().length());
        b.putU32(poi.flags());
        b.putFloat(poi.x());
        b.putFloat(poi.y());
        b.putU32(poi.icon());
        b.putU32(poi.data());
        b.putCString(poi.iconName());
        return b.array();
    }

    static byte[] encodeBinderConfirm(Creature c) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(c.guid);
        return b.array();
    }

    byte[] encodeVendorList(Creature c) {
        List<Integer> stock = mgr.itemsForVendor(c.entry);
        WowBuffer b = new WowBuffer(32 + stock.size() * 32);
        b.putU64(c.guid);
        if (stock.isEmpty()) {
            b.putU8(0);
            b.putU8(0);
            return b.array();
        }
        b.putU8(stock.size());
        int slot = 1;
        for (int itemId : stock) {
            ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
            b.putU32(slot++);
            b.putU32(itemId);
            b.putU32(t == null ? 0 : t.displayId);
            b.putU32(0xFFFFFFFF);
            b.putU32(t == null ? 0 : t.buyPrice);
            b.putU32(t == null ? 0 : t.maxDurability);
            b.putU32(1);
            b.putU32(0);
        }
        return b.array();
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

    byte[] encodeOfferReward(long guid, ObjectMgr.QuestTemplate q) {
        int choices = q.rewChoiceItemsCount();
        int items = q.rewItemsCount();
        WowBuffer b = new WowBuffer(64 + q.title().length() + choices * 12 + items * 12);
        b.putU64(guid);
        b.putU32(q.id());
        b.putCString(q.title());
        b.putCString("");
        b.putU32(1);
        b.putU32(0);
        b.putU32(0);
        b.putU32(choices);
        for (int i = 0; i < choices; i++) {
            int id = q.rewChoiceItemId(i);
            b.putU32(id);
            b.putU32(q.rewChoiceItemCount(i));
            b.putU32(displayId(id));
        }
        b.putU32(items);
        if (items > 0) {
            b.putU32(q.rewItemId1());
            b.putU32(q.rewItemCount1());
            b.putU32(displayId(q.rewItemId1()));
        }
        b.putU32(q.rewMoney());
        b.putU32(0);
        b.putU32(0x08);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    private int displayId(int itemId) {
        ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
        return t == null ? 0 : t.displayId;
    }

    private int questMenuIcon(Player p, int questId, ObjectMgr.QuestTemplate q) {
        if (q == null) {
            return DIALOG_STATUS_NONE;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return DIALOG_STATUS_AVAILABLE;
        }
        if (readyToTurnIn(p, slot, q)) {
            return DIALOG_STATUS_REWARD_REP;
        }
        return DIALOG_STATUS_INCOMPLETE;
    }

    /** GossipDef.cpp writes GetQuestLevel. A stored 0 falls back to MinLevel. */
    static int shownQuestLevel(ObjectMgr.QuestTemplate q) {
        return q.questLevel() != 0 ? q.questLevel() : q.minLevel();
    }

    byte[] encodeDetails(long guid, ObjectMgr.QuestTemplate q) {
        int choices = q.rewChoiceItemsCount();
        int items = q.rewItemsCount();
        WowBuffer b = new WowBuffer(96 + choices * 12 + items * 12
                + q.title().length() + q.details().length() + q.objectives().length());
        b.putU64(guid);
        b.putU32(q.id());
        b.putCString(q.title());
        b.putCString(q.details());
        b.putCString(q.objectives());
        b.putU32(1);
        b.putU32(0);
        b.putU32(choices);
        for (int i = 0; i < choices; i++) {
            int id = q.rewChoiceItemId(i);
            b.putU32(id);
            b.putU32(q.rewChoiceItemCount(i));
            b.putU32(displayId(id));
        }
        b.putU32(items);
        for (int i = 0; i < items; i++) {
            b.putU32(q.rewItemId1());
            b.putU32(q.rewItemCount1());
            b.putU32(displayId(q.rewItemId1()));
        }
        b.putU32(q.rewMoney());
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    static byte[] encodeQuestComplete(ObjectMgr.QuestTemplate q, int xp, int money, int honor) {
        int items = q.rewItemId1() > 0 ? 1 : 0;
        WowBuffer b = new WowBuffer(24 + items * 8);
        b.putU32(q.id());
        b.putU32(0x03);
        b.putU32(xp);
        b.putU32(money);
        b.putU32(honor);
        b.putU32(items);
        if (items > 0) {
            b.putU32(q.rewItemId1());
            b.putU32(q.rewItemCount1());
        }
        return b.array();
    }

    static byte[] u32(int v) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(v);
        return b.array();
    }
}
