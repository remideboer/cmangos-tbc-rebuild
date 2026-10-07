package org.tbc.world.content;

import org.tbc.world.content.ObjectMgr.CreatureHitObjective;
import org.tbc.world.content.ObjectMgr.CreatureTemplate;
import org.tbc.world.content.ObjectMgr.EmoteNearNpcObjective;
import org.tbc.world.content.ObjectMgr.ItemTemplate;
import org.tbc.world.content.ObjectMgr.QuestTemplate;
import org.tbc.world.content.ObjectMgr.Spawn;
import org.tbc.world.content.ObjectMgr.TrainerSpell;
import org.tbc.world.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory seed of the classless Hero starter paths: one trainer per class with its unlock quest,
 * three follow-up quests, quest items and spawns (refactoring plan cycle 4.1, carved from ObjectMgr).
 */
final class HeroClassSeed {
    private final ObjectMgr m;

    private HeroClassSeed(ObjectMgr m) {
        this.m = m;
    }

    static void seed(ObjectMgr m) {
        new HeroClassSeed(m).all();
    }
    private void all() {
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

    private void installHeroTrainer(org.tbc.world.classless.HeroStarterTrainers.Placement p,
                                    java.util.List<TrainerSpell> spells) {
        int flags = Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_QUESTGIVER | Content.UNIT_NPC_FLAG_TRAINER;
        CreatureTemplate t = new CreatureTemplate(p.entry(), p.name(), p.display(), p.faction(), 100, 5,
                flags, "", "", org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS,
                heroTrainerSubName(p.trainerClass()), "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0);
        if (p.custom()) {
            m.creatures.put(p.entry(), t);
        } else {
            m.creatures.putIfAbsent(p.entry(), t);
        }
        m.trainerTypeByEntry.putIfAbsent(p.entry(), org.tbc.world.session.TrainerHandler.TRAINER_TYPE_CLASS);
        m.trainerClass.putIfAbsent(p.entry(), p.trainerClass());
        m.trainerSpells.putIfAbsent(p.entry(), new ArrayList<>(spells));
        if (p.trainerClass() == Player.CLASS_WARRIOR) {
            m.ensureBattlecasterOnTrainer(p.entry());
        }
        boolean present = false;
        for (Spawn s : m.spawns) {
            if (s.entry() == p.entry() && s.map() == p.map()) {
                present = true;
                break;
            }
        }
        if (!present) {
            m.addSpawnIfMissing(p.spawnGuid(), p.entry(), p.map(), p.x(), p.y(), p.z(), p.o());
        }
        if (p.hasMap0Twin()) {
            m.addSpawnIfMissing(org.tbc.world.classless.HeroClassUnlock.map0SpawnGuid(p.entry()),
                    p.entry(), 0, p.map0X(), p.map0Y(), 80f, 0f);
        }
    }

    private void relateHeroQuests(int npcEntry, int... questIds) {
        for (int questId : questIds) {
            ObjectMgr.addQuestRelation(m.questGivers, npcEntry, questId);
            ObjectMgr.addQuestRelation(m.questInvolved, npcEntry, questId);
        }
    }

    private static String heroTrainerSubName(int cls) {
        return switch (cls) {
            case Player.CLASS_PALADIN -> "Paladin Trainer";
            case Player.CLASS_HUNTER -> "Hunter Trainer";
            case Player.CLASS_ROGUE -> "Rogue Trainer";
            case Player.CLASS_PRIEST -> "Priest Trainer";
            case Player.CLASS_SHAMAN -> "Shaman Trainer";
            case Player.CLASS_MAGE -> "Mage Trainer";
            case Player.CLASS_WARLOCK -> "Warlock Trainer";
            case Player.CLASS_DRUID -> "Druid Trainer";
            default -> "Warrior Trainer";
        };
    }

    private void seedHeroWarriorUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_HEROS_FIRST_LESSON;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        java.util.List<TrainerSpell> spells = List.of(
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
                        org.tbc.world.classless.CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_PLATE));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "The Hero's First Lesson",
                "Practice with your weapon on the Mana Wyrms, then prove you can finish one. Return alive.",
                "Land 5 weapon hits on a Mana Wyrm and defeat 1 Mana Wyrm.",
                wyrm, org.tbc.world.classless.HeroClassUnlock.REQUIRED_KILLS, 0, 0, 0));
        m.questCreatureHits.putIfAbsent(questId, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.REQUIRED_HITS));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.HEROIC_STRIKE);
        seedHeroWarriorFollowUps(wyrm, questId);
        int rally = org.tbc.world.classless.HeroClassUnlock.QUEST_RALLY_THE_LINE;
        int chargeQ = org.tbc.world.classless.HeroClassUnlock.QUEST_CLOSE_THE_DISTANCE;
        int rendQ = org.tbc.world.classless.HeroClassUnlock.QUEST_A_WOUND_TO_REMEMBER;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_WARRIOR)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, rally, chargeQ, rendQ);
        }
    }

    private void seedHeroWarriorFollowUps(int wyrm, int unlockQuest) {
        int strip = org.tbc.world.classless.HeroClassUnlock.ITEM_TRAINING_STRIP;
        m.items.putIfAbsent(strip, ItemTemplate.heroTrainingStrip());

        int rally = org.tbc.world.classless.HeroClassUnlock.QUEST_RALLY_THE_LINE;
        m.quests.putIfAbsent(rally, heroFollowUpQuest(rally, "Rally the Line",
                "Roar at the trainer's banner so the line hears you, then defeat a Mana Wyrm.",
                "Use /roar near the Warrior Trainer. Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        m.questRewSpell.putIfAbsent(rally, Content.SPELL_BATTLE_SHOUT);
        m.questEmoteNearNpc.putIfAbsent(rally, new EmoteNearNpcObjective(
                org.tbc.world.classless.HeroClassUnlock.TEXT_EMOTE_ROAR,
                org.tbc.world.classless.HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER, Content.SPELL_BATTLE_SHOUT));

        int chargeQ = org.tbc.world.classless.HeroClassUnlock.QUEST_CLOSE_THE_DISTANCE;
        m.quests.putIfAbsent(chargeQ, heroFollowUpQuest(chargeQ, "Close the Distance",
                "Close on a Mana Wyrm and land three solid weapon hits. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm.",
                0, 0, 0, 0, unlockQuest));
        m.questCreatureHits.putIfAbsent(chargeQ, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_CHARGE_HITS));
        m.questRewSpell.putIfAbsent(chargeQ, org.tbc.world.classless.HeroClassUnlock.SPELL_CHARGE);

        int rendQ = org.tbc.world.classless.HeroClassUnlock.QUEST_A_WOUND_TO_REMEMBER;
        m.quests.putIfAbsent(rendQ, heroFollowUpQuest(rendQ, "A Wound to Remember",
                "Recover a training strip from the practice ground and return alive.",
                "Collect 1 Training Strip.",
                0, 0, strip, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(rendQ, org.tbc.world.classless.HeroClassUnlock.SPELL_REND);
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
                prevQuestId, 0,
                org.tbc.world.classless.HeroClassUnlock.questLogZoneOrSortForQuest(id));
    }

    private void seedHeroPaladinUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_VOW_TESTED;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int token = org.tbc.world.classless.HeroClassUnlock.ITEM_PROTECTIVE_TOKEN;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.spell.SpellEngine.DEVOTION_AURA, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_BLESSING_OF_MIGHT, 100, 1),
                new TrainerSpell(org.tbc.world.spell.SpellEngine.HOLY_LIGHT, 100, 1));
        m.items.putIfAbsent(token, ItemTemplate.heroQuestJunk(
                token, "Protective Token"));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Vow Tested",
                "Recover the lost protective token and defeat a Mana Wyrm that threatens the ward. Return alive.",
                "Collect 1 Protective Token. Defeat 1 Mana Wyrm.",
                wyrm, 1, token, 1, 0));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.SEAL_OF_RIGHTEOUSNESS);
        seedHeroPaladinFollowUps(wyrm, questId);
        int stand = org.tbc.world.classless.HeroClassUnlock.QUEST_STAND_FAST;
        int strength = org.tbc.world.classless.HeroClassUnlock.QUEST_STRENGTH_IN_SERVICE;
        int mercy = org.tbc.world.classless.HeroClassUnlock.QUEST_MERCYS_LESSON;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_PALADIN)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, stand, strength, mercy);
        }
    }

    private void seedHeroPaladinFollowUps(int wyrm, int unlockQuest) {
        int blessing = org.tbc.world.classless.HeroClassUnlock.ITEM_BLESSING_TOKEN;
        int kit = org.tbc.world.classless.HeroClassUnlock.ITEM_HEALING_KIT;
        m.items.putIfAbsent(blessing, ItemTemplate.heroQuestJunk(blessing, "Blessing Token"));
        m.items.putIfAbsent(kit, ItemTemplate.heroQuestJunk(kit, "Healing Kit"));

        int stand = org.tbc.world.classless.HeroClassUnlock.QUEST_STAND_FAST;
        m.quests.putIfAbsent(stand, heroFollowUpQuest(stand, "Stand Fast",
                "Hold the trainer's ward by defeating a Mana Wyrm. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        m.questRewSpell.putIfAbsent(stand, org.tbc.world.spell.SpellEngine.DEVOTION_AURA);

        int strength = org.tbc.world.classless.HeroClassUnlock.QUEST_STRENGTH_IN_SERVICE;
        m.quests.putIfAbsent(strength, heroFollowUpQuest(strength, "Strength in Service",
                "Deliver the trainer's blessing token as proof of service. Return alive.",
                "Collect 1 Blessing Token.",
                0, 0, blessing, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(strength, org.tbc.world.classless.HeroClassUnlock.SPELL_BLESSING_OF_MIGHT);

        int mercy = org.tbc.world.classless.HeroClassUnlock.QUEST_MERCYS_LESSON;
        m.quests.putIfAbsent(mercy, heroFollowUpQuest(mercy, "Mercy's Lesson",
                "Recover a healing kit for a wounded trainee. Return alive.",
                "Collect 1 Healing Kit.",
                0, 0, kit, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(mercy, org.tbc.world.spell.SpellEngine.HOLY_LIGHT);
    }

    private void seedHeroDruidUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_LIVING_BALANCE;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int seed = org.tbc.world.classless.HeroClassUnlock.ITEM_BLIGHTED_SEED;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_TOUCH, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_MOONFIRE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_MARK_OF_THE_WILD, 100, 1));
        m.items.putIfAbsent(seed, ItemTemplate.heroQuestJunk(seed, "Blighted Seed"));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Living Balance",
                "Recover a blighted seed from the grove edge, then defeat a Mana Wyrm that feeds on it. Return alive.",
                "Collect 1 Blighted Seed. Defeat 1 Mana Wyrm.",
                wyrm, 1, seed, 1, 0));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_WRATH);
        seedHeroDruidFollowUps(wyrm, questId);
        int touch = org.tbc.world.classless.HeroClassUnlock.QUEST_TOUCH_OF_THE_GROVE;
        int silent = org.tbc.world.classless.HeroClassUnlock.QUEST_A_SILENT_MARK;
        int gift = org.tbc.world.classless.HeroClassUnlock.QUEST_A_GIFT_OF_THE_WILD;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_DRUID)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, touch, silent, gift);
        }
    }

    private void seedHeroDruidFollowUps(int wyrm, int unlockQuest) {
        int salve = org.tbc.world.classless.HeroClassUnlock.ITEM_GROVE_SALVE;
        int mark = org.tbc.world.classless.HeroClassUnlock.ITEM_MOONLIGHT_MARK;
        int offering = org.tbc.world.classless.HeroClassUnlock.ITEM_WILD_OFFERING;
        m.items.putIfAbsent(salve, ItemTemplate.heroQuestJunk(salve, "Grove Salve"));
        m.items.putIfAbsent(mark, ItemTemplate.heroQuestJunk(mark, "Moonlight Mark"));
        m.items.putIfAbsent(offering, ItemTemplate.heroQuestJunk(offering, "Wild Offering"));

        int touch = org.tbc.world.classless.HeroClassUnlock.QUEST_TOUCH_OF_THE_GROVE;
        m.quests.putIfAbsent(touch, heroFollowUpQuest(touch, "Touch of the Grove",
                "Gather grove salve for a wounded ally. Return alive.",
                "Collect 1 Grove Salve.",
                0, 0, salve, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(touch, org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_TOUCH);

        int silent = org.tbc.world.classless.HeroClassUnlock.QUEST_A_SILENT_MARK;
        m.quests.putIfAbsent(silent, heroFollowUpQuest(silent, "A Silent Mark",
                "Place a moonlight mark, then defeat its local threat with your existing kit. Return alive.",
                "Collect 1 Moonlight Mark. Defeat 1 Mana Wyrm.",
                wyrm, 1, mark, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(silent, org.tbc.world.classless.HeroClassUnlock.SPELL_MOONFIRE);

        int gift = org.tbc.world.classless.HeroClassUnlock.QUEST_A_GIFT_OF_THE_WILD;
        m.quests.putIfAbsent(gift, heroFollowUpQuest(gift, "A Gift of the Wild",
                "Recover a wild offering from the trainer's grove trial. Return alive.",
                "Collect 1 Wild Offering.",
                0, 0, offering, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(gift, org.tbc.world.classless.HeroClassUnlock.SPELL_MARK_OF_THE_WILD);
    }

    private void seedHeroShamanUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int token = org.tbc.world.classless.HeroClassUnlock.ITEM_ELEMENTAL_TOKEN;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_WAVE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_EARTH_SHOCK, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_STONESKIN_TOTEM, 100, 1));
        m.items.putIfAbsent(token, ItemTemplate.heroQuestJunk(token, "Elemental Token"));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "Listen to the Elements",
                "Recover a local elemental token, present it at the trainer's shrine, and defeat a Mana Wyrm that disturbs the site. Return alive.",
                "Collect 1 Elemental Token. Defeat 1 Mana Wyrm.",
                wyrm, 1, token, 1, 0));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_LIGHTNING_BOLT);
        seedHeroShamanFollowUps(wyrm, questId);
        int mend = org.tbc.world.classless.HeroClassUnlock.QUEST_MEND_THE_WOUNDED;
        int shock = org.tbc.world.classless.HeroClassUnlock.QUEST_ANSWERING_SHOCK;
        int call = org.tbc.world.classless.HeroClassUnlock.QUEST_CALL_OF_EARTH;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_SHAMAN)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, mend, shock, call);
        }
    }

    private void seedHeroShamanFollowUps(int wyrm, int unlockQuest) {
        int herbs = org.tbc.world.classless.HeroClassUnlock.ITEM_HEALING_HERBS;
        int marker = org.tbc.world.classless.HeroClassUnlock.ITEM_ELEMENTAL_MARKER;
        int earth = org.tbc.world.classless.HeroClassUnlock.ITEM_EARTH_SAMPLE;
        m.items.putIfAbsent(herbs, ItemTemplate.heroQuestJunk(herbs, "Healing Herbs"));
        m.items.putIfAbsent(marker, ItemTemplate.heroQuestJunk(marker, "Elemental Marker"));
        m.items.putIfAbsent(earth, ItemTemplate.heroQuestJunk(earth, "Earth Sample"));

        int mend = org.tbc.world.classless.HeroClassUnlock.QUEST_MEND_THE_WOUNDED;
        m.quests.putIfAbsent(mend, heroFollowUpQuest(mend, "Mend the Wounded",
                "Gather healing herbs for a wounded ally. Return alive.",
                "Collect 1 Healing Herbs.",
                0, 0, herbs, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(mend, org.tbc.world.classless.HeroClassUnlock.SPELL_HEALING_WAVE);

        int shock = org.tbc.world.classless.HeroClassUnlock.QUEST_ANSWERING_SHOCK;
        m.quests.putIfAbsent(shock, heroFollowUpQuest(shock, "Answering Shock",
                "Restore a disturbed elemental marker, then defeat its local threat with your existing kit. Return alive.",
                "Collect 1 Elemental Marker. Defeat 1 Mana Wyrm.",
                wyrm, 1, marker, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(shock, org.tbc.world.classless.HeroClassUnlock.SPELL_EARTH_SHOCK);

        int call = org.tbc.world.classless.HeroClassUnlock.QUEST_CALL_OF_EARTH;
        m.quests.putIfAbsent(call, heroFollowUpQuest(call, "Call of Earth",
                "Recover an earth sample from the trainer's shrine trial. Return alive.",
                "Collect 1 Earth Sample.",
                0, 0, earth, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(call, org.tbc.world.classless.HeroClassUnlock.SPELL_STONESKIN_TOTEM);
    }

    private void seedHeroWarlockUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_THE_BOUND_FLAME;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int mark = org.tbc.world.classless.HeroClassUnlock.ITEM_BINDING_MARK;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_BOLT, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_IMMOLATE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SUMMON_IMP, 100, 1));
        m.items.putIfAbsent(mark, ItemTemplate.heroQuestJunk(mark, "Binding Mark"));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "The Bound Flame",
                "Recover a binding mark from the local cult and contain a Mana Wyrm that threatens the site. Return alive.",
                "Collect 1 Binding Mark. Defeat 1 Mana Wyrm.",
                wyrm, 1, mark, 1, 0));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_CORRUPTION);
        seedHeroWarlockFollowUps(wyrm, questId);
        int shadow = org.tbc.world.classless.HeroClassUnlock.QUEST_SHADOW_IN_RESERVE;
        int fel = org.tbc.world.classless.HeroClassUnlock.QUEST_FEL_AT_THE_EDGE;
        int familiar = org.tbc.world.classless.HeroClassUnlock.QUEST_A_FAMILIARS_FIRST_TASK;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_WARLOCK)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, shadow, fel, familiar);
        }
    }

    private void seedHeroWarlockFollowUps(int wyrm, int unlockQuest) {
        int page = org.tbc.world.classless.HeroClassUnlock.ITEM_SHADOWED_PAGE;
        int ember = org.tbc.world.classless.HeroClassUnlock.ITEM_FEL_EMBER;
        int reagents = org.tbc.world.classless.HeroClassUnlock.ITEM_BINDING_REAGENTS;
        m.items.putIfAbsent(page, ItemTemplate.heroQuestJunk(page, "Shadowed Page"));
        m.items.putIfAbsent(ember, ItemTemplate.heroQuestJunk(ember, "Controlled Fel Ember"));
        m.items.putIfAbsent(reagents, ItemTemplate.heroQuestJunk(reagents, "Binding Reagents"));

        int shadow = org.tbc.world.classless.HeroClassUnlock.QUEST_SHADOW_IN_RESERVE;
        m.quests.putIfAbsent(shadow, heroFollowUpQuest(shadow, "Shadow in Reserve",
                "Recover a shadowed page and defeat a marked target with your existing kit. Return alive.",
                "Collect 1 Shadowed Page. Defeat 1 Mana Wyrm.",
                wyrm, 1, page, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(shadow, org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_BOLT);

        int fel = org.tbc.world.classless.HeroClassUnlock.QUEST_FEL_AT_THE_EDGE;
        m.quests.putIfAbsent(fel, heroFollowUpQuest(fel, "Fel at the Edge",
                "Collect a controlled fel ember from a local threat. Return alive.",
                "Collect 1 Controlled Fel Ember.",
                0, 0, ember, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(fel, org.tbc.world.classless.HeroClassUnlock.SPELL_IMMOLATE);

        int familiar = org.tbc.world.classless.HeroClassUnlock.QUEST_A_FAMILIARS_FIRST_TASK;
        m.quests.putIfAbsent(familiar, heroFollowUpQuest(familiar, "A Familiar's First Task",
                "Recover the trainer's binding reagents. Return alive.",
                "Collect 1 Binding Reagents.",
                0, 0, reagents, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(familiar, org.tbc.world.classless.HeroClassUnlock.SPELL_SUMMON_IMP);
    }

    private void seedHeroMageUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_CONTROLLED_SPARK;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int fragments = org.tbc.world.classless.HeroClassUnlock.ITEM_ARCANE_FRAGMENTS;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.spell.SpellEngine.FROST_ARMOR, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_INTELLECT, 100, 1),
                new TrainerSpell(org.tbc.world.spell.SpellEngine.FROSTBOLT, 100, 1));
        m.items.putIfAbsent(fragments, ItemTemplate.heroQuestJunk(fragments, "Arcane Fragments"));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Controlled Spark",
                "Recover arcane fragments, stabilize them at the trainer's focus, and defeat a Mana Wyrm. Return alive.",
                "Collect 1 Arcane Fragments. Defeat 1 Mana Wyrm.",
                wyrm, 1, fragments, 1, 0));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.FIREBALL);
        seedHeroMageFollowUps(wyrm, questId);
        int cooler = org.tbc.world.classless.HeroClassUnlock.QUEST_A_COOLER_HEAD;
        int study = org.tbc.world.classless.HeroClassUnlock.QUEST_SHARE_THE_STUDY;
        int second = org.tbc.world.classless.HeroClassUnlock.QUEST_A_SECOND_SCHOOL;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_MAGE)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, cooler, study, second);
        }
    }

    private void seedHeroMageFollowUps(int wyrm, int unlockQuest) {
        int focus = org.tbc.world.classless.HeroClassUnlock.ITEM_FROST_TREATED_FOCUS;
        int notes = org.tbc.world.classless.HeroClassUnlock.ITEM_STUDY_NOTES;
        m.items.putIfAbsent(focus, ItemTemplate.heroQuestJunk(focus, "Frost-Treated Focus"));
        m.items.putIfAbsent(notes, ItemTemplate.heroQuestJunk(notes, "Study Notes"));

        int cooler = org.tbc.world.classless.HeroClassUnlock.QUEST_A_COOLER_HEAD;
        m.quests.putIfAbsent(cooler, heroFollowUpQuest(cooler, "A Cooler Head",
                "Recover a frost-treated focus from a local hazard. Return alive.",
                "Collect 1 Frost-Treated Focus.",
                0, 0, focus, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(cooler, org.tbc.world.spell.SpellEngine.FROST_ARMOR);

        int study = org.tbc.world.classless.HeroClassUnlock.QUEST_SHARE_THE_STUDY;
        m.quests.putIfAbsent(study, heroFollowUpQuest(study, "Share the Study",
                "Deliver the trainer's study notes to an ally. Return alive.",
                "Collect 1 Study Notes.",
                0, 0, notes, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(study, org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_INTELLECT);

        int second = org.tbc.world.classless.HeroClassUnlock.QUEST_A_SECOND_SCHOOL;
        m.quests.putIfAbsent(second, heroFollowUpQuest(second, "A Second School",
                "Defeat a marked target with your existing kit. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        m.questRewSpell.putIfAbsent(second, org.tbc.world.spell.SpellEngine.FROSTBOLT);
    }

    private void seedHeroPriestUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        int supplies = org.tbc.world.classless.HeroClassUnlock.ITEM_HEALING_SUPPLIES;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SMITE, 100, 1),
                new TrainerSpell(org.tbc.world.spell.SpellEngine.POWER_WORD_FORTITUDE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_WORD_PAIN, 100, 1));
        m.items.putIfAbsent(supplies, ItemTemplate.heroQuestJunk(supplies, "Healing Supplies"));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "Mercy and Judgment",
                "Recover healing supplies for a wounded trainee and defeat a Mana Wyrm that threatens the route. Return alive.",
                "Collect 1 Healing Supplies. Defeat 1 Mana Wyrm.",
                wyrm, 1, supplies, 1, 0));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_LESSER_HEAL);
        seedHeroPriestFollowUps(wyrm, questId);
        int judgment = org.tbc.world.classless.HeroClassUnlock.QUEST_JUDGMENT_FROM_AFAR;
        int guarding = org.tbc.world.classless.HeroClassUnlock.QUEST_A_GUARDING_WORD;
        int pain = org.tbc.world.classless.HeroClassUnlock.QUEST_PAIN_AS_WARNING;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_PRIEST)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, judgment, guarding, pain);
        }
    }

    private void seedHeroPriestFollowUps(int wyrm, int unlockQuest) {
        int scroll = org.tbc.world.classless.HeroClassUnlock.ITEM_WARDING_SCROLL;
        int shadow = org.tbc.world.classless.HeroClassUnlock.ITEM_SHADOW_MARKED_TOKEN;
        m.items.putIfAbsent(scroll, ItemTemplate.heroQuestJunk(scroll, "Warding Scroll"));
        m.items.putIfAbsent(shadow, ItemTemplate.heroQuestJunk(shadow, "Shadow-Marked Token"));

        int judgment = org.tbc.world.classless.HeroClassUnlock.QUEST_JUDGMENT_FROM_AFAR;
        m.quests.putIfAbsent(judgment, heroFollowUpQuest(judgment, "Judgment from Afar",
                "Defeat a marked target with your existing kit. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        m.questRewSpell.putIfAbsent(judgment, org.tbc.world.classless.HeroClassUnlock.SPELL_SMITE);

        int guarding = org.tbc.world.classless.HeroClassUnlock.QUEST_A_GUARDING_WORD;
        m.quests.putIfAbsent(guarding, heroFollowUpQuest(guarding, "A Guarding Word",
                "Deliver a warding scroll to an ally. Return alive.",
                "Collect 1 Warding Scroll.",
                0, 0, scroll, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(guarding, org.tbc.world.spell.SpellEngine.POWER_WORD_FORTITUDE);

        int pain = org.tbc.world.classless.HeroClassUnlock.QUEST_PAIN_AS_WARNING;
        m.quests.putIfAbsent(pain, heroFollowUpQuest(pain, "Pain as Warning",
                "Recover a shadow-marked token from a local hostile's camp. Return alive.",
                "Collect 1 Shadow-Marked Token.",
                0, 0, shadow, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(pain, org.tbc.world.classless.HeroClassUnlock.SPELL_SHADOW_WORD_PAIN);
    }

    private void seedHeroRogueUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_A_QUIET_HAND;
        int token = org.tbc.world.classless.HeroClassUnlock.ITEM_CAMP_TOKEN;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.spell.SpellEngine.SPELL_STEALTH, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_EVISCERATE, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SLICE_AND_DICE, 100, 1));
        m.items.putIfAbsent(token, ItemTemplate.heroQuestJunk(token, "Camp Token"));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "A Quiet Hand",
                "Recover the trainer's token from a local hostile's camp. Return alive.",
                "Collect 1 Camp Token.",
                0, 0, token, 1, 0));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.classless.HeroClassUnlock.SPELL_SINISTER_STRIKE);
        seedHeroRogueFollowUps(questId);
        int disappear = org.tbc.world.classless.HeroClassUnlock.QUEST_DISAPPEAR_FROM_SIGHT;
        int finish = org.tbc.world.classless.HeroClassUnlock.QUEST_FINISH_THE_OPENING;
        int advantage = org.tbc.world.classless.HeroClassUnlock.QUEST_KEEP_THE_ADVANTAGE;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_ROGUE)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, disappear, finish, advantage);
        }
    }

    private void seedHeroRogueFollowUps(int unlockQuest) {
        int shadowed = org.tbc.world.classless.HeroClassUnlock.ITEM_SHADOWED_TOKEN;
        int notes = org.tbc.world.classless.HeroClassUnlock.ITEM_FINISHING_NOTES;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        m.items.putIfAbsent(shadowed, ItemTemplate.heroQuestJunk(shadowed, "Shadowed Token"));
        m.items.putIfAbsent(notes, ItemTemplate.heroQuestJunk(notes, "Finishing-Form Notes"));

        int disappear = org.tbc.world.classless.HeroClassUnlock.QUEST_DISAPPEAR_FROM_SIGHT;
        m.quests.putIfAbsent(disappear, heroFollowUpQuest(disappear, "Disappear from Sight",
                "Retrieve a marked token from the practice grounds. Stealth is not required yet. Return alive.",
                "Collect 1 Shadowed Token.",
                0, 0, shadowed, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(disappear, org.tbc.world.spell.SpellEngine.SPELL_STEALTH);

        int finish = org.tbc.world.classless.HeroClassUnlock.QUEST_FINISH_THE_OPENING;
        m.quests.putIfAbsent(finish, heroFollowUpQuest(finish, "Finish the Opening",
                "Land three solid hits on a practice target. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm.",
                0, 0, 0, 0, unlockQuest));
        m.questCreatureHits.putIfAbsent(finish, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_EVISCERATE_HITS));
        m.questRewSpell.putIfAbsent(finish, org.tbc.world.classless.HeroClassUnlock.SPELL_EVISCERATE);

        int advantage = org.tbc.world.classless.HeroClassUnlock.QUEST_KEEP_THE_ADVANTAGE;
        m.quests.putIfAbsent(advantage, heroFollowUpQuest(advantage, "Keep the Advantage",
                "Recover the trainer's finishing-form notes from a local cache. Return alive.",
                "Collect 1 Finishing-Form Notes.",
                0, 0, notes, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(advantage, org.tbc.world.classless.HeroClassUnlock.SPELL_SLICE_AND_DICE);
    }

    private void seedHeroHunterUnlock() {
        int questId = org.tbc.world.classless.HeroClassUnlock.QUEST_THE_MARKED_TRAIL;
        int wyrm = org.tbc.world.classless.HeroClassUnlock.CREATURE_MANA_WYRM;
        java.util.List<TrainerSpell> spells = List.of(
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_AUTO_SHOT, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_SERPENT_STING, 100, 1),
                new TrainerSpell(org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_SHOT, 100, 1));
        m.quests.putIfAbsent(questId, heroFollowUpQuest(questId, "The Marked Trail",
                "Track the local prey, land three solid hits, then finish one. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm and defeat 1 Mana Wyrm.",
                wyrm, org.tbc.world.classless.HeroClassUnlock.REQUIRED_KILLS, 0, 0, 0));
        m.questCreatureHits.putIfAbsent(questId, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_MARKED_HITS));
        m.questRewSpell.putIfAbsent(questId, org.tbc.world.spell.SpellEngine.HUNTERS_MARK);
        seedHeroHunterFollowUps(wyrm, questId);
        int steady = org.tbc.world.classless.HeroClassUnlock.QUEST_STEADY_AIM;
        int venomQ = org.tbc.world.classless.HeroClassUnlock.QUEST_VENOM_IN_THE_FIELD;
        int clean = org.tbc.world.classless.HeroClassUnlock.QUEST_A_CLEAN_SHOT;
        for (var p : org.tbc.world.classless.HeroStarterTrainers.forClass(Player.CLASS_HUNTER)) {
            installHeroTrainer(p, spells);
            relateHeroQuests(p.entry(), questId, steady, venomQ, clean);
        }
    }

    private void seedHeroHunterFollowUps(int wyrm, int unlockQuest) {
        int venom = org.tbc.world.classless.HeroClassUnlock.ITEM_VENOM_SAMPLE;
        m.items.putIfAbsent(venom, ItemTemplate.heroQuestJunk(venom, "Venom Sample"));

        int steady = org.tbc.world.classless.HeroClassUnlock.QUEST_STEADY_AIM;
        m.quests.putIfAbsent(steady, heroFollowUpQuest(steady, "Steady Aim",
                "Prove you can finish a local threat with your basic kit. Return alive.",
                "Defeat 1 Mana Wyrm.",
                wyrm, 1, 0, 0, unlockQuest));
        m.questRewSpell.putIfAbsent(steady, org.tbc.world.classless.HeroClassUnlock.SPELL_AUTO_SHOT);

        int venomQ = org.tbc.world.classless.HeroClassUnlock.QUEST_VENOM_IN_THE_FIELD;
        m.quests.putIfAbsent(venomQ, heroFollowUpQuest(venomQ, "Venom in the Field",
                "Recover venom samples from local beasts. Return alive.",
                "Collect 1 Venom Sample.",
                0, 0, venom, 1, unlockQuest));
        m.questRewSpell.putIfAbsent(venomQ, org.tbc.world.classless.HeroClassUnlock.SPELL_SERPENT_STING);

        int clean = org.tbc.world.classless.HeroClassUnlock.QUEST_A_CLEAN_SHOT;
        m.quests.putIfAbsent(clean, heroFollowUpQuest(clean, "A Clean Shot",
                "Land three solid hits on a marked target. Return alive.",
                "Land 3 weapon hits on a Mana Wyrm.",
                0, 0, 0, 0, unlockQuest));
        m.questCreatureHits.putIfAbsent(clean, new CreatureHitObjective(wyrm,
                org.tbc.world.classless.HeroClassUnlock.FOLLOWUP_CLEAN_SHOT_HITS));
        m.questRewSpell.putIfAbsent(clean, org.tbc.world.classless.HeroClassUnlock.SPELL_ARCANE_SHOT);
    }
}
