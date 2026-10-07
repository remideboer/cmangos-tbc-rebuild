package org.tbc.world.content;

import org.tbc.world.content.ObjectMgr.AreaTrigger;
import org.tbc.world.content.ObjectMgr.Auction;
import org.tbc.world.content.ObjectMgr.CreateInfo;
import org.tbc.world.content.ObjectMgr.CreatureTemplate;
import org.tbc.world.content.ObjectMgr.GossipMenuItem;
import org.tbc.world.content.ObjectMgr.ItemTemplate;
import org.tbc.world.content.ObjectMgr.LootRow;
import org.tbc.world.content.ObjectMgr.QuestTemplate;
import org.tbc.world.content.ObjectMgr.Spawn;
import org.tbc.world.content.ObjectMgr.SpellChainNode;
import org.tbc.world.content.ObjectMgr.Talent;
import org.tbc.world.content.ObjectMgr.TalentTab;
import org.tbc.world.content.ObjectMgr.TaxiHop;
import org.tbc.world.content.ObjectMgr.TaxiNode;
import org.tbc.world.content.ObjectMgr.TrainerSpell;
import org.tbc.world.content.ObjectMgr.ZoneWeather;
import org.tbc.world.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_ARMORER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_AUCTIONEER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_BANKER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_BATTLEFIELD;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_BOT;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_GOSSIP;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_INNKEEPER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_PETITIONER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_QUESTGIVER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_SPIRITGUIDE;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_SPIRITHEALER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_STABLEPET;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_TABARDDESIGNER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_TAXIVENDOR;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_TRAINER;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_UNLEARNPETSKILLS;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_UNLEARNTALENTS;
import static org.tbc.world.content.ObjectMgr.GOSSIP_OPTION_VENDOR;

/**
 * In-memory world defaults (no tbc-db): playercreateinfo, Northshire NPCs, items, taxi, weather, talents,
 * gossip menu 0 and Innkeeper Farley. {@code defaults} replaces the SQL load; {@code queryDefaults} fills
 * gaps after either path. Carved from ObjectMgr in refactoring plan cycle 4.1.
 */
final class WorldDefaultsSeed {
    private final ObjectMgr m;

    private WorldDefaultsSeed(ObjectMgr m) {
        this.m = m;
    }

    /** Full in-memory seed used when there is no world database. */
    static void defaults(ObjectMgr m) {
        new WorldDefaultsSeed(m).defaults();
    }

    /** Idempotent fill of rows the 8606 client queries that SQL may lack. */
    static void queryDefaults(ObjectMgr m) {
        new WorldDefaultsSeed(m).queryDefaults();
    }
    private void defaults() {
        m.levelStats.seedDefaults();
        CreateActionSeed.seed(m);
        m.createInfo.put(ObjectMgr.key(1, 1), new CreateInfo(1, 1, 0, 12, -8949.95f, -132.493f, 83.5312f, 0f));
        m.createInfo.put(ObjectMgr.key(2, 1), new CreateInfo(2, 1, 1, 14, -618.518f, -4251.67f, 38.718f, 0f));
        // Night Elf warrior — Shadowglen / Teldrassil (classless createForRace).
        m.createInfo.put(ObjectMgr.key(4, 1), new CreateInfo(4, 1, 1, 141, 10311.3f, 831.463f, 1326.41f, 0f));
        // Blood Elf has no warrior; mage coords = Sunstrider (classless createForRace).
        m.createInfo.put(ObjectMgr.key(10, 8), new CreateInfo(10, 8, 530, 3431, 10349.6f, -6357.29f, 33.4026f, 0f));
        // Draenei warrior — Ammen Vale (classless / GY wire tests).
        m.createInfo.put(ObjectMgr.key(11, 1), new CreateInfo(11, 1, 530, 3526, -3961.64f, -13931.2f, 100.615f, 2.08364f));
        // Undead / Tauren starters (GY void-prevention wire tests).
        m.createInfo.put(ObjectMgr.key(5, 1), new CreateInfo(5, 1, 0, 85, 1676.35f, 1677.45f, 121.67f, 2.70526f));
        m.createInfo.put(ObjectMgr.key(6, 1), new CreateInfo(6, 1, 1, 215, -2917.58f, -257.98f, 52.9968f, 0f));
        m.createSpells.put((int) ObjectMgr.key(1, 1), new ArrayList<>(List.of(6603, 78, 81, 107, 196, 203, 204, 522, 668, 2382, 2457, 2479, 3050, 3365, 6233, 6246, 6247, 6477, 6478, 7266, 7267, 7355, 8386, 9078, 9125, 20597, 20598, 20599, 20864, 21651, 21652, 22027, 22810)));
        m.creatures.put(6, koboldVermin());
        m.creatures.put(103, new CreatureTemplate(103, "Garrick Padfoot", 3734, 21, 80, 5, 0, "", "", 0));
        m.areaTriggers.put(2230, new AreaTrigger(2230, 389, 0.797643f, -8.23429f, -15.5288f, 0f));
        m.creatures.put(Content.NPC_CORINA_STEELE, new CreatureTemplate(Content.NPC_CORINA_STEELE, "Corina Steele", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_VENDOR, "", "", 0));
        m.creatures.put(Content.NPC_MARSHAL_DUGHAN, new CreatureTemplate(Content.NPC_MARSHAL_DUGHAN, "Marshal Dughan", 0, 12, 100, 10,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER, "", "", 0));
        m.creatures.put(Content.NPC_DEPUTY_WILLEM, new CreatureTemplate(Content.NPC_DEPUTY_WILLEM, "Deputy Willem", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER, "", "", 0));
        m.creatures.put(Content.NPC_MARSHAL_MCBRIDE, new CreatureTemplate(Content.NPC_MARSHAL_MCBRIDE, "Marshal McBride", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER, "", "", 0));
        m.creatures.put(Content.NPC_LLANE_BESHERE, new CreatureTemplate(Content.NPC_LLANE_BESHERE, "Llane Beshere", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        m.trainerTypeByEntry.put(Content.NPC_LLANE_BESHERE, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        m.trainerClass.put(Content.NPC_LLANE_BESHERE, 1);
        m.trainerSpells.put(Content.NPC_LLANE_BESHERE, new ArrayList<>(List.of(
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
        m.spellChain.putIfAbsent(Content.SPELL_BATTLE_SHOUT_RANK2,
                new SpellChainNode(Content.SPELL_BATTLE_SHOUT_RANK2, Content.SPELL_BATTLE_SHOUT,
                        Content.SPELL_BATTLE_SHOUT, 2, 0));
        m.spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER, 2, 0));
        m.spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL, 3, 0));
        m.creatures.put(Content.NPC_KHELDEN_BREMEN, new CreatureTemplate(Content.NPC_KHELDEN_BREMEN, "Khelden Bremen", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        m.trainerTypeByEntry.put(Content.NPC_KHELDEN_BREMEN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        m.trainerClass.put(Content.NPC_KHELDEN_BREMEN, Player.CLASS_MAGE);
        m.trainerSpells.put(Content.NPC_KHELDEN_BREMEN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_FIREBALL, Content.TRAINER_SPELL_FIREBALL_COST, 1))));
        m.creatures.put(Content.NPC_DANE_LINDGREN, new CreatureTemplate(Content.NPC_DANE_LINDGREN, "Dane Lindgren", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER, "", "",
                org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS));
        m.trainerTypeByEntry.put(Content.NPC_DANE_LINDGREN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS);
        m.trainerSpells.put(Content.NPC_DANE_LINDGREN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_APPRENTICE_BLACKSMITH, Content.TRAINER_SPELL_APPRENTICE_BLACKSMITH_COST, 1,
                        0, 0, 0, 0, 0, true))));
        m.creatures.put(Content.NPC_DUNGAR_LONGDRINK, new CreatureTemplate(Content.NPC_DUNGAR_LONGDRINK, "Dungar Longdrink", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_FLIGHTMASTER, "", "", 0));
        m.creatures.put(Content.NPC_INNKEEPER_FARLEY, new CreatureTemplate(Content.NPC_INNKEEPER_FARLEY, "Innkeeper Farley", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_INNKEEPER, "", "", 0));
        m.creatures.put(Content.NPC_AUCTIONEER_CHILTON, new CreatureTemplate(Content.NPC_AUCTIONEER_CHILTON, "Auctioneer Chilton", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_AUCTIONEER, "", "", 0));
        m.creatures.put(Content.NPC_OLIVIA_BURNSIDE, new CreatureTemplate(Content.NPC_OLIVIA_BURNSIDE, "Olivia Burnside", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_BANKER, "", "", 0));
        m.creatures.put(Content.NPC_REBECCA_LAUGHLIN, new CreatureTemplate(Content.NPC_REBECCA_LAUGHLIN, "Rebecca Laughlin", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_PETITIONER | Content.UNIT_NPC_FLAG_TABARDDESIGNER, "", "", 0));
        m.creatures.put(Content.NPC_LUMA_SKYMOTHER, new CreatureTemplate(Content.NPC_LUMA_SKYMOTHER, "Luma Skymother", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP, "", "", 0));
        m.auctions.add(new Auction(1, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 43_200_000, "Worn Shortsword"));
        m.taxiPaths.put(ObjectMgr.taxiKey(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE),
                new TaxiHop(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE, 0, -4821.13f, -1152.4f, 502.295f));
        m.taxiNodes.put(Content.TAXI_STORMWIND, new TaxiNode(Content.TAXI_STORMWIND, 0,
                -8835.76f, 490.084f, 109.699f, true, false));
        m.weather.put(Content.ZONE_ELWYNN, new ZoneWeather(Content.ZONE_ELWYNN, Content.WEATHER_STATE_FINE, 0f));
        talents();
        m.quests.put(Content.QUEST_A_THREAT_WITHIN, new QuestTemplate(Content.QUEST_A_THREAT_WITHIN, "A Threat Within", 1, 0,
                0, "Speak with Marshal McBride.", "Speak with Marshal McBride.", 0, 0, 0, 0, 1, 24, 0, 0));
        m.quests.put(Content.QUEST_REST_AND_RELAXATION, new QuestTemplate(Content.QUEST_REST_AND_RELAXATION,
                "Rest and Relaxation", 1, 0, 0, "", "", 0, 0, 0, 0, 5, 27,
                Content.ITEM_REFRESHING_SPRING_WATER, 5));
        m.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, new QuestTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                "Kobold Camp Cleanup", 1, 0, 0, "", "", Content.NPC_KOBOLD_VERMIN, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Content.ZONE_ELWYNN));
        m.quests.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, new QuestTemplate(Content.QUEST_BROTHERHOOD_OF_THIEVES,
                "Brotherhood of Thieves", 2, 0, 0, "", "", 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 12, 4, 216, 0, 0,
                Content.ITEM_MILITIA_DAGGER, 1, Content.ITEM_MILITIA_HAMMER, 1));
        m.vendorItems.put(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(Content.ITEM_WORN_SHORTSWORD)));
        m.creatureLoot.computeIfAbsent(6, k -> new ArrayList<>())
                .add(new LootRow(Content.ITEM_WORN_SHORTSWORD, 100f, 1, 1));
        m.questGivers.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        m.questGivers.put(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        m.questInvolved.put(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        m.questInvolved.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        if (m.spawns.isEmpty()) {
            // Hostiles stay outside abbey NPC attack distance — otherwise faction NPCs
            // DetectOrAttack them on every World.tick (GuardAI / UnitAI MoveInLineOfSight).
            m.spawns.add(new Spawn(1, 6, 0, -8550f, -150f, 80f, 0f));
            m.spawns.add(new Spawn(2, Content.NPC_MARSHAL_DUGHAN, 0, Content.GOLDSHIRE_X, Content.GOLDSHIRE_Y, Content.GOLDSHIRE_Z, 0f));
            m.spawns.add(new Spawn(3, Content.NPC_CORINA_STEELE, 0, -8903f, -125f, 80f, 0f));
            m.spawns.add(new Spawn(4, Content.NPC_DEPUTY_WILLEM, 0, -8906f, -128f, 80f, 0f));
            m.spawns.add(new Spawn(5, Content.NPC_MARSHAL_MCBRIDE, 0, -8908f, -130f, 80f, 0f));
            m.spawns.add(new Spawn(6, 103, 0, -8600f, -180f, 80f, 0f));
            m.spawns.add(new Spawn(7, Content.NPC_LLANE_BESHERE, 0, -8918.36f, -208.411f, 82.309f, 0f));
            m.spawns.add(new Spawn(15, Content.NPC_KHELDEN_BREMEN, 0, -8920f, -210f, 82.3f, 0f));
            m.spawns.add(new Spawn(14, Content.NPC_DANE_LINDGREN, 0, -8910f, -200f, 82f, 0f));
            m.spawns.add(new Spawn(8, Content.NPC_DUNGAR_LONGDRINK, 0, -8835.76f, 490.084f, 109.699f, 0f));
            m.spawns.add(new Spawn(9, Content.NPC_AUCTIONEER_CHILTON, 0, -8912f, -122f, 80f, 0f));
            m.spawns.add(new Spawn(10, Content.NPC_OLIVIA_BURNSIDE, 0, -8914f, -124f, 80f, 0f));
            m.spawns.add(new Spawn(13, Content.NPC_REBECCA_LAUGHLIN, 0, -8916f, -126f, 80f, 0f));
            m.spawns.add(new Spawn(12, Content.NPC_INNKEEPER_FARLEY, 0, -9462.66f, 16.1915f, 57.0459f, 0f));
        }
        if (!m.eventCreatures.containsKey(Content.GAME_EVENT_MIDSUMMER)) {
            m.eventCreatures.put(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                    new Spawn(11, Content.NPC_LUMA_SKYMOTHER, 547, -92.45719f, -110.6642f, -2.866759f, 2.408554f))));
        }
        if (!m.eventGameObjects.containsKey(Content.GAME_EVENT_MIDSUMMER)) {
            m.eventGameObjects.put(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                    new Spawn(5470020, Content.GO_ICE_STONE, 547, -69.9045f, -162.245f, -2.36656f, 2.42601f))));
        }
        HeroClassSeed.seed(m);
    }

    private static CreatureTemplate koboldVermin() {
        return new CreatureTemplate(6, "Kobold Vermin", 10913, 7, 42, 1, 0, "", "", 0,
                "", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0,
                "", 0, 1f, 3f, 2000, 1.5f, 0, 1, 1,
                org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT);
    }

    private void queryDefaults() {
        m.creatures.putIfAbsent(6, koboldVermin());
        // Mana Wyrm — Eversong; MinLevelMana for Mana Tap 28734 / client mana bar.
        m.creatures.putIfAbsent(15274, new CreatureTemplate(15274, "Mana Wyrm", 15404, 7, 55, 1, 0, "", "", 0));
        m.creatureMana.putIfAbsent(15274, 65);
        // battlemaster_entry: Kurak (2302) → BATTLEGROUND_WS = 2
        m.battleMasterBg.putIfAbsent(2302, 2);
        m.creatures.putIfAbsent(Content.NPC_LLANE_BESHERE, new CreatureTemplate(Content.NPC_LLANE_BESHERE, "Llane Beshere", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        m.items.putIfAbsent(25, ItemTemplate.wornShortsword());
        m.items.putIfAbsent(Content.ITEM_SKINNING_KNIFE, ItemTemplate.skinningKnife());
        m.items.putIfAbsent(38, ItemTemplate.recruitsShirt());
        m.items.putIfAbsent(39, ItemTemplate.recruitsPants());
        m.items.putIfAbsent(40, ItemTemplate.recruitsBoots());
        m.items.putIfAbsent(Content.ITEM_RIVERPAW_LEATHER_VEST, ItemTemplate.riverpawLeatherVest());
        m.items.putIfAbsent(Content.ITEM_TUNIC_OF_WESTFALL, ItemTemplate.tunicOfWestfall());
        m.items.putIfAbsent(Content.ITEM_BRACKWATER_VEST, ItemTemplate.brackwaterVest());
        m.items.putIfAbsent(Content.ITEM_SEERS_ROBE, ItemTemplate.seersRobe());
        m.items.putIfAbsent(Content.ITEM_BLACKENED_DEFIAS_ARMOR, ItemTemplate.blackenedDefiasArmor());
        m.items.putIfAbsent(Content.ITEM_LIGHTFORGE_BREASTPLATE, ItemTemplate.lightforgeBreastplate());
        m.items.putIfAbsent(Content.ITEM_LAWBRINGER_CHESTGUARD, ItemTemplate.lawbringerChestguard());
        m.items.putIfAbsent(Content.ITEM_LIVING_BREASTPLATE, ItemTemplate.livingBreastplate());
        m.items.putIfAbsent(Content.ITEM_ICEBANE_BREASTPLATE, ItemTemplate.icebaneBreastplate());
        m.items.putIfAbsent(Content.ITEM_SHADESTEEL_GREAVES, ItemTemplate.shadesteelGreaves());
        m.items.putIfAbsent(Content.ITEM_SOULCLOTH_VEST, ItemTemplate.soulclothVest());
        m.items.putIfAbsent(Content.ITEM_BLADE_OF_HANNA, ItemTemplate.bladeOfHanna());
        m.items.putIfAbsent(Content.ITEM_DESTROYER_CHESTGUARD, ItemTemplate.destroyerChestguard());
        m.items.putIfAbsent(Content.ITEM_DESTROYER_BREASTPLATE, ItemTemplate.destroyerBreastplate());
        m.items.putIfAbsent(Content.ITEM_GLADIATORS_PLATE_CHESTPIECE, ItemTemplate.gladiatorsPlateChestpiece());
        m.items.putIfAbsent(Content.ITEM_ONSLAUGHT_CHESTGUARD, ItemTemplate.onslaughtChestguard());
        m.items.putIfAbsent(Content.ITEM_WARHARNESS_OF_RECKLESS_FURY, ItemTemplate.warharnessOfRecklessFury());
        m.items.putIfAbsent(Content.ITEM_GAUNTLETS_OF_ENFORCEMENT, ItemTemplate.gauntletsOfEnforcement());
        m.items.putIfAbsent(Content.ITEM_VENGEFUL_GLADIATORS_DRAGONHIDE_TUNIC,
                ItemTemplate.vengefulGladiatorsDragonhideTunic());
        m.items.putIfAbsent(Content.ITEM_AUCHENAI_ANCHORITES_ROBE, ItemTemplate.auchenaiAnchoritesRobe());
        m.items.putIfAbsent(Content.ITEM_GARMENTS_OF_SERENE_SHORES, ItemTemplate.garmentsOfSereneShores());
        m.items.putIfAbsent(Content.ITEM_SUNGLOW_VEST, ItemTemplate.sunglowVest());
        m.items.putIfAbsent(Content.ITEM_WORN_WOODEN_SHIELD, ItemTemplate.wornWoodenShield());
        m.items.putIfAbsent(Content.ITEM_CLOAK_OF_DARKNESS, ItemTemplate.cloakOfDarkness());
        m.items.putIfAbsent(Content.ITEM_NETHERSTRAND_LONGBOW, ItemTemplate.netherstrandLongbow());
        m.items.putIfAbsent(Content.ITEM_TWIN_BLADES_OF_AZZINOTH, ItemTemplate.twinBladesOfAzzinoth());
        m.items.putIfAbsent(Content.ITEM_JYOO_TEST_ITEM, ItemTemplate.jyooTestItem());
        m.items.putIfAbsent(Content.ITEM_TOMS_BOOTS_1, ItemTemplate.tomsBoots1());
        m.items.putIfAbsent(Content.ITEM_TEST_HP_RING, ItemTemplate.testHpRing());
        m.items.putIfAbsent(Content.ITEM_TEST_HOLY_RESIST_VEST, ItemTemplate.testHolyResistVest());
        m.items.putIfAbsent(Content.ITEM_TEST_MP_RING, ItemTemplate.testMpRing());
        m.items.putIfAbsent(Content.ITEM_BAND_OF_THE_ETERNAL_CHAMPION, ItemTemplate.bandOfTheEternalChampion());
        m.items.putIfAbsent(Content.ITEM_GUILD_CHARTER, ItemTemplate.guildCharter());
        m.items.putIfAbsent(Content.ITEM_HEARTHSTONE, ItemTemplate.hearthstone());
        m.items.putIfAbsent(Content.ITEM_TOUGH_JERKY, ItemTemplate.toughJerky());
        m.items.putIfAbsent(Content.ITEM_TOUGH_HUNK_OF_BREAD, ItemTemplate.toughHunkOfBread());
        m.items.putIfAbsent(Content.ITEM_RED_BURLAP_BANDANA, ItemTemplate.redBurlapBandana());
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_COPPER_ORE,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_COPPER_ORE, "Copper Ore", 4681, 20, 5));
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_TIN_ORE,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_TIN_ORE, "Tin Ore", 4690, 100, 25));
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_IRON_ORE,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_IRON_ORE, "Iron Ore", 4689, 600, 150));
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_MITHRIL_ORE,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_MITHRIL_ORE, "Mithril Ore", 20661, 1000, 250));
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_THORIUM_ORE,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_THORIUM_ORE, "Thorium Ore", 20658, 1000, 250));
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_FEL_IRON_ORE,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_FEL_IRON_ORE, "Fel Iron Ore", 38645, 4000, 1000));
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_ADAMANTITE_ORE,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_ADAMANTITE_ORE, "Adamantite Ore", 38648, 6000, 1500));
        m.items.putIfAbsent(org.tbc.world.profession.CoinFromOre.ITEM_COAL,
                ItemTemplate.tradeOre(org.tbc.world.profession.CoinFromOre.ITEM_COAL, "Coal", 7340, 500, 125));
        m.items.putIfAbsent(Content.ITEM_REFRESHING_SPRING_WATER, ItemTemplate.refreshingSpringWater());
        m.items.putIfAbsent(Content.ITEM_MINOR_HEALING_POTION, ItemTemplate.minorHealingPotion());
        // SQL load may have created empty spell rows; force usable-item spells from seeds.
        m.mergeUsableItemSpells(ItemTemplate.hearthstone());
        m.mergeUsableItemSpells(ItemTemplate.toughJerky());
        m.mergeUsableItemSpells(ItemTemplate.toughHunkOfBread());
        m.mergeUsableItemSpells(ItemTemplate.refreshingSpringWater());
        m.mergeUsableItemSpells(ItemTemplate.minorHealingPotion());
        // Keep consumable max-stack for 8606 client even if a thin SQL row set stackable=1.
        m.ensureStackable(Content.ITEM_TOUGH_JERKY, 20);
        m.ensureStackable(Content.ITEM_TOUGH_HUNK_OF_BREAD, 20);
        m.ensureStackable(Content.ITEM_RED_BURLAP_BANDANA, 20);
        m.ensureStackable(Content.ITEM_REFRESHING_SPRING_WATER, 20);
        m.ensureStackable(Content.ITEM_MINOR_HEALING_POTION, 5);
        m.quests.putIfAbsent(Content.QUEST_A_THREAT_WITHIN, new QuestTemplate(Content.QUEST_A_THREAT_WITHIN, "A Threat Within", 1, 0,
                0, "Speak with Marshal McBride.", "Speak with Marshal McBride.", 0, 0, 0, 0, 1, 24, 0, 0));
        m.quests.putIfAbsent(Content.QUEST_REST_AND_RELAXATION, new QuestTemplate(Content.QUEST_REST_AND_RELAXATION,
                "Rest and Relaxation", 1, 0, 0, "", "", 0, 0, 0, 0, 5, 27,
                Content.ITEM_REFRESHING_SPRING_WATER, 5));
        m.quests.putIfAbsent(Content.QUEST_KOBOLD_CAMP_CLEANUP, new QuestTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                "Kobold Camp Cleanup", 1, 0, 0, "", "", Content.NPC_KOBOLD_VERMIN, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Content.ZONE_ELWYNN));
        m.quests.putIfAbsent(Content.QUEST_BROTHERHOOD_OF_THIEVES, new QuestTemplate(Content.QUEST_BROTHERHOOD_OF_THIEVES,
                "Brotherhood of Thieves", 2, 0, 0, "", "", 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 12, 4, 216, 0, 0,
                Content.ITEM_MILITIA_DAGGER, 1, Content.ITEM_MILITIA_HAMMER, 1));
        m.vendorItems.putIfAbsent(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(Content.ITEM_WORN_SHORTSWORD)));
        m.creatureLoot.computeIfAbsent(6, k -> new ArrayList<>());
        if (m.creatureLoot.get(6).isEmpty()) {
            m.creatureLoot.get(6).add(new LootRow(Content.ITEM_WORN_SHORTSWORD, 100f, 1, 1));
        }
        m.questGivers.putIfAbsent(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        List<Integer> willemQuests = m.questGivers.get(Content.NPC_DEPUTY_WILLEM);
        if (willemQuests != null && !willemQuests.contains(Content.QUEST_BROTHERHOOD_OF_THIEVES)) {
            willemQuests.add(Content.QUEST_BROTHERHOOD_OF_THIEVES);
        }
        m.questGivers.putIfAbsent(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        m.questInvolved.putIfAbsent(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        m.questInvolved.putIfAbsent(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        List<Integer> willemInvolved = m.questInvolved.get(Content.NPC_DEPUTY_WILLEM);
        if (willemInvolved != null && !willemInvolved.contains(Content.QUEST_BROTHERHOOD_OF_THIEVES)) {
            willemInvolved.add(Content.QUEST_BROTHERHOOD_OF_THIEVES);
        }
        m.trainerTypeByEntry.putIfAbsent(Content.NPC_LLANE_BESHERE, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        m.trainerClass.putIfAbsent(Content.NPC_LLANE_BESHERE, 1);
        m.trainerSpells.putIfAbsent(Content.NPC_LLANE_BESHERE, new ArrayList<>(List.of(
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
        m.ensureBattlecasterOnTrainer(Content.NPC_LLANE_BESHERE);
        m.spellChain.putIfAbsent(Content.SPELL_BATTLE_SHOUT_RANK2,
                new SpellChainNode(Content.SPELL_BATTLE_SHOUT_RANK2, Content.SPELL_BATTLE_SHOUT,
                        Content.SPELL_BATTLE_SHOUT, 2, 0));
        m.spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER, 2, 0));
        m.spellChain.putIfAbsent(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                new SpellChainNode(org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL,
                        org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL, 3, 0));
        m.creatures.putIfAbsent(Content.NPC_KHELDEN_BREMEN, new CreatureTemplate(Content.NPC_KHELDEN_BREMEN, "Khelden Bremen", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        m.trainerTypeByEntry.putIfAbsent(Content.NPC_KHELDEN_BREMEN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        m.trainerClass.putIfAbsent(Content.NPC_KHELDEN_BREMEN, Player.CLASS_MAGE);
        m.trainerSpells.putIfAbsent(Content.NPC_KHELDEN_BREMEN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_FIREBALL, Content.TRAINER_SPELL_FIREBALL_COST, 1))));
        m.creatures.putIfAbsent(Content.NPC_DANE_LINDGREN, new CreatureTemplate(Content.NPC_DANE_LINDGREN, "Dane Lindgren", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER, "", "",
                org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS));
        m.trainerTypeByEntry.putIfAbsent(Content.NPC_DANE_LINDGREN, org.tbc.world.session.TrainerHandler.TRAINER_TYPE_TRADESKILLS);
        m.trainerSpells.putIfAbsent(Content.NPC_DANE_LINDGREN, new ArrayList<>(List.of(
                new TrainerSpell(Content.SPELL_APPRENTICE_BLACKSMITH, Content.TRAINER_SPELL_APPRENTICE_BLACKSMITH_COST, 1,
                        0, 0, 0, 0, 0, true))));
        m.creatures.putIfAbsent(Content.NPC_DUNGAR_LONGDRINK, new CreatureTemplate(Content.NPC_DUNGAR_LONGDRINK, "Dungar Longdrink", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_FLIGHTMASTER, "", "", 0));
        m.creatures.putIfAbsent(Content.NPC_INNKEEPER_FARLEY, new CreatureTemplate(Content.NPC_INNKEEPER_FARLEY, "Innkeeper Farley", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_INNKEEPER, "", "", 0));
        m.creatures.putIfAbsent(Content.NPC_AUCTIONEER_CHILTON, new CreatureTemplate(Content.NPC_AUCTIONEER_CHILTON, "Auctioneer Chilton", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_AUCTIONEER, "", "", 0));
        m.creatures.putIfAbsent(Content.NPC_OLIVIA_BURNSIDE, new CreatureTemplate(Content.NPC_OLIVIA_BURNSIDE, "Olivia Burnside", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_BANKER, "", "", 0));
        m.creatures.putIfAbsent(Content.NPC_REBECCA_LAUGHLIN, new CreatureTemplate(Content.NPC_REBECCA_LAUGHLIN, "Rebecca Laughlin", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_PETITIONER | Content.UNIT_NPC_FLAG_TABARDDESIGNER, "", "", 0));
        m.creatures.putIfAbsent(Content.NPC_LUMA_SKYMOTHER, new CreatureTemplate(Content.NPC_LUMA_SKYMOTHER, "Luma Skymother", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_GOSSIP, "", "", 0));
        if (m.auctions.isEmpty()) {
            m.auctions.add(new Auction(1, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 43_200_000, "Worn Shortsword"));
        }
        m.taxiPaths.putIfAbsent(ObjectMgr.taxiKey(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE),
                new TaxiHop(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE, 0, -4821.13f, -1152.4f, 502.295f));
        m.taxiNodes.putIfAbsent(Content.TAXI_STORMWIND, new TaxiNode(Content.TAXI_STORMWIND, 0,
                -8835.76f, 490.084f, 109.699f, true, false));
        m.weather.putIfAbsent(Content.ZONE_ELWYNN, new ZoneWeather(Content.ZONE_ELWYNN, Content.WEATHER_STATE_FINE, 0f));
        talents();
        m.pointsOfInterest.putIfAbsent(ObjectMgr.lionsPrideInnPoi().entry(), ObjectMgr.lionsPrideInnPoi());
        m.eventCreatures.putIfAbsent(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                new Spawn(11, Content.NPC_LUMA_SKYMOTHER, 547, -92.45719f, -110.6642f, -2.866759f, 2.408554f))));
        m.eventGameObjects.putIfAbsent(Content.GAME_EVENT_MIDSUMMER, new ArrayList<>(List.of(
                new Spawn(5470020, Content.GO_ICE_STONE, 547, -69.9045f, -162.245f, -2.36656f, 2.42601f))));
        boolean hasPetitioner = false;
        for (Spawn s : m.spawns) {
            if (s.entry() == Content.NPC_REBECCA_LAUGHLIN) {
                hasPetitioner = true;
                break;
            }
        }
        if (!hasPetitioner) {
            m.spawns.add(new Spawn(1_000_013, Content.NPC_REBECCA_LAUGHLIN, 0, -8916f, -126f, 80f, 0f));
        }
        HeroClassSeed.seed(m);
        menu0();
        farleyGossip();
    }

    private void talents() {
        m.talents.putIfAbsent(124, new Talent(124, 161, 0, 0, 12282, 12663, 12664, 0, 0, 0, 0, 0));
        m.talentTabs.putIfAbsent(161, new TalentTab(161, 1));
    }

    private void menu0() {
        if (m.gossipOptions.containsKey(0) && !m.gossipOptions.get(0).isEmpty()) {
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
        m.gossipOptions.put(0, rows);
    }

    private static GossipMenuItem menu0(int id, int icon, String text, int optionId, int npcFlag) {
        return new GossipMenuItem(0, id, icon, text, optionId, npcFlag, 0, 0, "", 0);
    }

    private void farleyGossip() {
        m.gossipMenuIds.putIfAbsent(Content.NPC_INNKEEPER_FARLEY, Content.GOSSIP_MENU_FARLEY);
        m.gossipTextIds.putIfAbsent(Content.GOSSIP_MENU_FARLEY, Content.GOSSIP_TEXT_FARLEY);
        m.gossipTextIds.putIfAbsent(Content.GOSSIP_MENU_FARLEY_INN_INFO, Content.GOSSIP_TEXT_FARLEY_INN_INFO);
        if (!m.gossipOptions.containsKey(Content.GOSSIP_MENU_FARLEY)) {
            m.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, new ArrayList<>(List.of(
                    new GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 1, Content.GOSSIP_ICON_INTERACT_2,
                            "Make this inn your home.", GOSSIP_OPTION_INNKEEPER, Content.UNIT_NPC_FLAG_INNKEEPER,
                            0, 0, "", 0),
                    new GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 3, 0, Content.GOSSIP_FARLEY_INN_INFO,
                            GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "",
                            Content.GOSSIP_MENU_FARLEY_INN_INFO))));
        }
    }
}
