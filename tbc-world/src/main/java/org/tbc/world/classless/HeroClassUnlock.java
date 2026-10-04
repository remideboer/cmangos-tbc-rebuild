package org.tbc.world.classless;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Player;
import org.tbc.world.spell.SpellEngine;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-owned Hero class unlock + per-spell follow-up quests. Unlock/follow-up state is
 * {@code rewardedQuests}.
 */
public final class HeroClassUnlock {
    public static final int QUEST_HEROS_FIRST_LESSON = 90001;
    public static final int QUEST_RALLY_THE_LINE = 90002;
    public static final int QUEST_CLOSE_THE_DISTANCE = 90003;
    public static final int QUEST_A_WOUND_TO_REMEMBER = 90004;
    public static final int QUEST_A_VOW_TESTED = 90005;
    public static final int QUEST_STAND_FAST = 90006;
    public static final int QUEST_STRENGTH_IN_SERVICE = 90007;
    public static final int QUEST_MERCYS_LESSON = 90008;
    public static final int QUEST_THE_MARKED_TRAIL = 90009;
    public static final int QUEST_STEADY_AIM = 90010;
    public static final int QUEST_VENOM_IN_THE_FIELD = 90011;
    public static final int QUEST_A_CLEAN_SHOT = 90012;
    public static final int QUEST_A_QUIET_HAND = 90013;
    public static final int QUEST_DISAPPEAR_FROM_SIGHT = 90014;
    public static final int QUEST_FINISH_THE_OPENING = 90015;
    public static final int QUEST_KEEP_THE_ADVANTAGE = 90016;
    public static final int QUEST_MERCY_AND_JUDGMENT = 90017;
    public static final int QUEST_JUDGMENT_FROM_AFAR = 90018;
    public static final int QUEST_A_GUARDING_WORD = 90019;
    public static final int QUEST_PAIN_AS_WARNING = 90020;
    public static final int QUEST_A_CONTROLLED_SPARK = 90021;
    public static final int QUEST_A_COOLER_HEAD = 90022;
    public static final int QUEST_SHARE_THE_STUDY = 90023;
    public static final int QUEST_A_SECOND_SCHOOL = 90024;
    public static final int QUEST_THE_BOUND_FLAME = 90025;
    public static final int QUEST_SHADOW_IN_RESERVE = 90026;
    public static final int QUEST_FEL_AT_THE_EDGE = 90027;
    public static final int QUEST_A_FAMILIARS_FIRST_TASK = 90028;
    public static final int QUEST_LISTEN_TO_THE_ELEMENTS = 90029;
    public static final int QUEST_MEND_THE_WOUNDED = 90030;
    public static final int QUEST_ANSWERING_SHOCK = 90031;
    public static final int QUEST_CALL_OF_EARTH = 90032;

    public static final int NPC_HERO_WARRIOR_TRAINER = 91001;
    public static final int NPC_HERO_PALADIN_TRAINER = 91002;
    public static final int NPC_HERO_HUNTER_TRAINER = 91003;
    public static final int NPC_HERO_ROGUE_TRAINER = 91004;
    public static final int NPC_HERO_PRIEST_TRAINER = 91005;
    public static final int NPC_HERO_MAGE_TRAINER = 91006;
    public static final int NPC_HERO_WARLOCK_TRAINER = 91007;
    public static final int NPC_HERO_SHAMAN_TRAINER = 91008;
    /** Blood Elf Sunstrider Isle warrior trainer display name. */
    public static final String NAME_LORVAEN_BLOODFEATHER = "Lorvaen Bloodfeather";
    /** Blood Elf Sunstrider Isle paladin trainer display name. */
    public static final String NAME_VELAARA_SUNWARD = "Velaara Sunward";
    /** Blood Elf Sunstrider Isle hunter trainer display name. */
    public static final String NAME_KAELAN_DAWNSTRIKE = "Kaelan Dawnstrike";
    /** Blood Elf Sunstrider Isle rogue trainer display name. */
    public static final String NAME_SYLARA_NIGHTWHISPER = "Sylara Nightwhisper";
    /** Blood Elf Sunstrider Isle priest trainer display name. */
    public static final String NAME_LIRAE_DAWNWHISPER = "Lirae Dawnwhisper";
    /** Blood Elf Sunstrider Isle mage trainer display name. */
    public static final String NAME_ARYN_FLAMEWEAVE = "Aryn Flameweave";
    /** Blood Elf Sunstrider Isle warlock trainer display name. */
    public static final String NAME_VAELITH_DARKBIND = "Vaelith Darkbind";
    /** Hero shaman trainer display name (Sunstrider rollout NPC). */
    public static final String NAME_TALAAN_STONESONG = "Talaan Stonesong";
    public static final int CREATURE_MANA_WYRM = 15274;
    public static final int REQUIRED_HITS = 5;
    public static final int REQUIRED_KILLS = 1;
    public static final int FOLLOWUP_CHARGE_HITS = 3;
    public static final int FOLLOWUP_MARKED_HITS = 3;
    public static final int FOLLOWUP_CLEAN_SHOT_HITS = 3;
    public static final int FOLLOWUP_EVISCERATE_HITS = 3;
    /** Server-owned quest item for Rend follow-up. */
    public static final int ITEM_TRAINING_STRIP = 92001;
    /** Paladin unlock: protective token. */
    public static final int ITEM_PROTECTIVE_TOKEN = 92002;
    /** Blessing of Might follow-up delivery token. */
    public static final int ITEM_BLESSING_TOKEN = 92003;
    /** Holy Light follow-up healing kit. */
    public static final int ITEM_HEALING_KIT = 92004;
    /** Hunter Serpent Sting follow-up venom sample. */
    public static final int ITEM_VENOM_SAMPLE = 92005;
    /** Rogue unlock: camp token. */
    public static final int ITEM_CAMP_TOKEN = 92006;
    /** Stealth follow-up: marked-area token. */
    public static final int ITEM_SHADOWED_TOKEN = 92007;
    /** Slice and Dice follow-up: finishing-form notes. */
    public static final int ITEM_FINISHING_NOTES = 92008;
    /** Priest unlock: healing supplies. */
    public static final int ITEM_HEALING_SUPPLIES = 92009;
    /** PW:F follow-up: warding scroll. */
    public static final int ITEM_WARDING_SCROLL = 92010;
    /** SW:P follow-up: shadow-marked token. */
    public static final int ITEM_SHADOW_MARKED_TOKEN = 92011;
    /** Mage unlock: arcane fragments. */
    public static final int ITEM_ARCANE_FRAGMENTS = 92012;
    /** Frost Armor follow-up: frost-treated focus. */
    public static final int ITEM_FROST_TREATED_FOCUS = 92013;
    /** Arcane Intellect follow-up: study notes. */
    public static final int ITEM_STUDY_NOTES = 92014;
    /** Warlock unlock: binding mark. */
    public static final int ITEM_BINDING_MARK = 92015;
    /** Shadow Bolt follow-up: shadowed page. */
    public static final int ITEM_SHADOWED_PAGE = 92016;
    /** Immolate follow-up: controlled fel ember. */
    public static final int ITEM_FEL_EMBER = 92017;
    /** Summon Imp follow-up: binding reagents. */
    public static final int ITEM_BINDING_REAGENTS = 92018;
    /** Shaman unlock: elemental token. */
    public static final int ITEM_ELEMENTAL_TOKEN = 92019;
    /** Healing Wave follow-up: healing herbs. */
    public static final int ITEM_HEALING_HERBS = 92020;
    /** Earth Shock follow-up: elemental marker. */
    public static final int ITEM_ELEMENTAL_MARKER = 92021;
    /** Call of Earth follow-up: earth sample. */
    public static final int ITEM_EARTH_SAMPLE = 92022;
    /** Charge Rank 1 (Spell.dbc 100). */
    public static final int SPELL_CHARGE = 100;
    /** Rend Rank 1 (Spell.dbc 772). */
    public static final int SPELL_REND = 772;
    /** Blessing of Might Rank 1 (Spell.dbc 19740). */
    public static final int SPELL_BLESSING_OF_MIGHT = 19740;
    /** Auto Shot (Spell.dbc 75). */
    public static final int SPELL_AUTO_SHOT = 75;
    /** Serpent Sting Rank 1 (Spell.dbc 1978). */
    public static final int SPELL_SERPENT_STING = 1978;
    /** Arcane Shot Rank 1 (Spell.dbc 3044). */
    public static final int SPELL_ARCANE_SHOT = 3044;
    /** Sinister Strike Rank 1 (Spell.dbc 1752). */
    public static final int SPELL_SINISTER_STRIKE = 1752;
    /** Eviscerate Rank 1 (Spell.dbc 2098). */
    public static final int SPELL_EVISCERATE = 2098;
    /** Slice and Dice Rank 1 (Spell.dbc 5171). */
    public static final int SPELL_SLICE_AND_DICE = 5171;
    /** Lesser Heal Rank 1 (Spell.dbc 2050). */
    public static final int SPELL_LESSER_HEAL = 2050;
    /** Smite Rank 1 (Spell.dbc 585). */
    public static final int SPELL_SMITE = 585;
    /** Shadow Word: Pain Rank 1 (Spell.dbc 589). */
    public static final int SPELL_SHADOW_WORD_PAIN = 589;
    /** Arcane Intellect Rank 1 (Spell.dbc 1459). */
    public static final int SPELL_ARCANE_INTELLECT = 1459;
    /** Corruption Rank 1 (Spell.dbc 172). */
    public static final int SPELL_CORRUPTION = 172;
    /** Shadow Bolt Rank 1 (Spell.dbc 686). */
    public static final int SPELL_SHADOW_BOLT = 686;
    /** Immolate Rank 1 (Spell.dbc 348). */
    public static final int SPELL_IMMOLATE = 348;
    /** Summon Imp (Spell.dbc 688); EffectMiscValue creature 416. */
    public static final int SPELL_SUMMON_IMP = 688;
    public static final int CREATURE_IMP = 416;
    /** Lightning Bolt Rank 1 (Spell.dbc 403). */
    public static final int SPELL_LIGHTNING_BOLT = 403;
    /** Healing Wave Rank 1 (Spell.dbc 331). */
    public static final int SPELL_HEALING_WAVE = 331;
    /** Earth Shock Rank 1 (Spell.dbc 8042). */
    public static final int SPELL_EARTH_SHOCK = 8042;
    /** Stoneskin Totem Rank 1 (Spell.dbc 8071) — Call of Earth reward. */
    public static final int SPELL_STONESKIN_TOTEM = 8071;
    /** Jesthenis Sunstriker (15280) ModelId1. */
    public static final int DISPLAY_JESTHENIS = 15521;
    /** FactionTemplate Silvermoon City NPC (creature 15280). Reputation faction 911. */
    public static final int FACTION_SILVERMOON = 1604;
    public static final int FACTION_SILVERMOON_REP = 911;
    /** Llane Beshere starter sword ItemDisplayInfo (item 1896). */
    public static final int VIRTUAL_ITEM_SWORD_DISPLAY = 7487;
    /** Llane Beshere wooden buckler ItemDisplayInfo (item 1961). */
    public static final int VIRTUAL_ITEM_SHIELD_DISPLAY = 1685;
    public static final float SUNSTRIDER_SPAWN_X = 10381.6f;
    public static final float SUNSTRIDER_SPAWN_Y = -6399.23f;
    public static final float SUNSTRIDER_SPAWN_Z = 38.5306f;
    public static final float SUNSTRIDER_SPAWN_O = 3.74096f;
    /** Paladin trainer a few yards from the warrior trainer. */
    public static final float SUNSTRIDER_PALADIN_X = 10383.6f;
    public static final float SUNSTRIDER_PALADIN_Y = -6401.23f;
    public static final float SUNSTRIDER_PALADIN_Z = 38.5306f;
    public static final float SUNSTRIDER_PALADIN_O = 3.74096f;
    /** Hunter trainer a few yards from the warrior trainer. */
    public static final float SUNSTRIDER_HUNTER_X = 10379.6f;
    public static final float SUNSTRIDER_HUNTER_Y = -6397.23f;
    public static final float SUNSTRIDER_HUNTER_Z = 38.5306f;
    public static final float SUNSTRIDER_HUNTER_O = 3.74096f;
    /** Rogue trainer a few yards from the warrior trainer. */
    public static final float SUNSTRIDER_ROGUE_X = 10377.6f;
    public static final float SUNSTRIDER_ROGUE_Y = -6395.23f;
    public static final float SUNSTRIDER_ROGUE_Z = 38.5306f;
    public static final float SUNSTRIDER_ROGUE_O = 3.74096f;
    /** Priest trainer a few yards from the warrior trainer. */
    public static final float SUNSTRIDER_PRIEST_X = 10385.6f;
    public static final float SUNSTRIDER_PRIEST_Y = -6403.23f;
    public static final float SUNSTRIDER_PRIEST_Z = 38.5306f;
    public static final float SUNSTRIDER_PRIEST_O = 3.74096f;
    /** Mage trainer a few yards from the warrior trainer. */
    public static final float SUNSTRIDER_MAGE_X = 10375.6f;
    public static final float SUNSTRIDER_MAGE_Y = -6393.23f;
    public static final float SUNSTRIDER_MAGE_Z = 38.5306f;
    public static final float SUNSTRIDER_MAGE_O = 3.74096f;
    /** Warlock trainer a few yards from the warrior trainer. */
    public static final float SUNSTRIDER_WARLOCK_X = 10387.6f;
    public static final float SUNSTRIDER_WARLOCK_Y = -6405.23f;
    public static final float SUNSTRIDER_WARLOCK_Z = 38.5306f;
    public static final float SUNSTRIDER_WARLOCK_O = 3.74096f;
    /** Shaman trainer a few yards from the warrior trainer. */
    public static final float SUNSTRIDER_SHAMAN_X = 10373.6f;
    public static final float SUNSTRIDER_SHAMAN_Y = -6391.23f;
    public static final float SUNSTRIDER_SHAMAN_Z = 38.5306f;
    public static final float SUNSTRIDER_SHAMAN_O = 3.74096f;

    public static final HeroClassUnlock WARRIOR = new HeroClassUnlock(
            Player.CLASS_WARRIOR,
            QUEST_HEROS_FIRST_LESSON,
            NPC_HERO_WARRIOR_TRAINER,
            SpellEngine.HEROIC_STRIKE,
            CREATURE_MANA_WYRM,
            REQUIRED_HITS,
            REQUIRED_KILLS);

    public static final HeroClassUnlock PALADIN = new HeroClassUnlock(
            Player.CLASS_PALADIN,
            QUEST_A_VOW_TESTED,
            NPC_HERO_PALADIN_TRAINER,
            SpellEngine.SEAL_OF_RIGHTEOUSNESS,
            CREATURE_MANA_WYRM,
            0,
            REQUIRED_KILLS);

    public static final HeroClassUnlock HUNTER = new HeroClassUnlock(
            Player.CLASS_HUNTER,
            QUEST_THE_MARKED_TRAIL,
            NPC_HERO_HUNTER_TRAINER,
            SpellEngine.HUNTERS_MARK,
            CREATURE_MANA_WYRM,
            FOLLOWUP_MARKED_HITS,
            REQUIRED_KILLS);

    public static final HeroClassUnlock ROGUE = new HeroClassUnlock(
            Player.CLASS_ROGUE,
            QUEST_A_QUIET_HAND,
            NPC_HERO_ROGUE_TRAINER,
            SPELL_SINISTER_STRIKE,
            0,
            0,
            0);

    public static final HeroClassUnlock PRIEST = new HeroClassUnlock(
            Player.CLASS_PRIEST,
            QUEST_MERCY_AND_JUDGMENT,
            NPC_HERO_PRIEST_TRAINER,
            SPELL_LESSER_HEAL,
            CREATURE_MANA_WYRM,
            0,
            REQUIRED_KILLS);

    public static final HeroClassUnlock MAGE = new HeroClassUnlock(
            Player.CLASS_MAGE,
            QUEST_A_CONTROLLED_SPARK,
            NPC_HERO_MAGE_TRAINER,
            SpellEngine.FIREBALL,
            CREATURE_MANA_WYRM,
            0,
            REQUIRED_KILLS);

    public static final HeroClassUnlock WARLOCK = new HeroClassUnlock(
            Player.CLASS_WARLOCK,
            QUEST_THE_BOUND_FLAME,
            NPC_HERO_WARLOCK_TRAINER,
            SPELL_CORRUPTION,
            CREATURE_MANA_WYRM,
            0,
            REQUIRED_KILLS);

    public static final HeroClassUnlock SHAMAN = new HeroClassUnlock(
            Player.CLASS_SHAMAN,
            QUEST_LISTEN_TO_THE_ELEMENTS,
            NPC_HERO_SHAMAN_TRAINER,
            SPELL_LIGHTNING_BOLT,
            CREATURE_MANA_WYRM,
            0,
            REQUIRED_KILLS);

    /** Hero-only follow-up: teaches one spell on turn-in; gates that spell on trainers. */
    public record FollowUp(int questId, int rewardSpell, int requiresQuest, int trainerClass) {
    }

    public static final FollowUp RALLY_THE_LINE = new FollowUp(
            QUEST_RALLY_THE_LINE, Content.SPELL_BATTLE_SHOUT, QUEST_HEROS_FIRST_LESSON, Player.CLASS_WARRIOR);
    public static final FollowUp CLOSE_THE_DISTANCE = new FollowUp(
            QUEST_CLOSE_THE_DISTANCE, SPELL_CHARGE, QUEST_HEROS_FIRST_LESSON, Player.CLASS_WARRIOR);
    public static final FollowUp A_WOUND_TO_REMEMBER = new FollowUp(
            QUEST_A_WOUND_TO_REMEMBER, SPELL_REND, QUEST_HEROS_FIRST_LESSON, Player.CLASS_WARRIOR);
    public static final FollowUp STAND_FAST = new FollowUp(
            QUEST_STAND_FAST, SpellEngine.DEVOTION_AURA, QUEST_A_VOW_TESTED, Player.CLASS_PALADIN);
    public static final FollowUp STRENGTH_IN_SERVICE = new FollowUp(
            QUEST_STRENGTH_IN_SERVICE, SPELL_BLESSING_OF_MIGHT, QUEST_A_VOW_TESTED, Player.CLASS_PALADIN);
    public static final FollowUp MERCYS_LESSON = new FollowUp(
            QUEST_MERCYS_LESSON, SpellEngine.HOLY_LIGHT, QUEST_A_VOW_TESTED, Player.CLASS_PALADIN);
    public static final FollowUp STEADY_AIM = new FollowUp(
            QUEST_STEADY_AIM, SPELL_AUTO_SHOT, QUEST_THE_MARKED_TRAIL, Player.CLASS_HUNTER);
    public static final FollowUp VENOM_IN_THE_FIELD = new FollowUp(
            QUEST_VENOM_IN_THE_FIELD, SPELL_SERPENT_STING, QUEST_THE_MARKED_TRAIL, Player.CLASS_HUNTER);
    public static final FollowUp A_CLEAN_SHOT = new FollowUp(
            QUEST_A_CLEAN_SHOT, SPELL_ARCANE_SHOT, QUEST_THE_MARKED_TRAIL, Player.CLASS_HUNTER);
    public static final FollowUp DISAPPEAR_FROM_SIGHT = new FollowUp(
            QUEST_DISAPPEAR_FROM_SIGHT, SpellEngine.SPELL_STEALTH, QUEST_A_QUIET_HAND, Player.CLASS_ROGUE);
    public static final FollowUp FINISH_THE_OPENING = new FollowUp(
            QUEST_FINISH_THE_OPENING, SPELL_EVISCERATE, QUEST_A_QUIET_HAND, Player.CLASS_ROGUE);
    public static final FollowUp KEEP_THE_ADVANTAGE = new FollowUp(
            QUEST_KEEP_THE_ADVANTAGE, SPELL_SLICE_AND_DICE, QUEST_A_QUIET_HAND, Player.CLASS_ROGUE);
    public static final FollowUp JUDGMENT_FROM_AFAR = new FollowUp(
            QUEST_JUDGMENT_FROM_AFAR, SPELL_SMITE, QUEST_MERCY_AND_JUDGMENT, Player.CLASS_PRIEST);
    public static final FollowUp A_GUARDING_WORD = new FollowUp(
            QUEST_A_GUARDING_WORD, SpellEngine.POWER_WORD_FORTITUDE, QUEST_MERCY_AND_JUDGMENT, Player.CLASS_PRIEST);
    public static final FollowUp PAIN_AS_WARNING = new FollowUp(
            QUEST_PAIN_AS_WARNING, SPELL_SHADOW_WORD_PAIN, QUEST_MERCY_AND_JUDGMENT, Player.CLASS_PRIEST);
    public static final FollowUp A_COOLER_HEAD = new FollowUp(
            QUEST_A_COOLER_HEAD, SpellEngine.FROST_ARMOR, QUEST_A_CONTROLLED_SPARK, Player.CLASS_MAGE);
    public static final FollowUp SHARE_THE_STUDY = new FollowUp(
            QUEST_SHARE_THE_STUDY, SPELL_ARCANE_INTELLECT, QUEST_A_CONTROLLED_SPARK, Player.CLASS_MAGE);
    public static final FollowUp A_SECOND_SCHOOL = new FollowUp(
            QUEST_A_SECOND_SCHOOL, SpellEngine.FROSTBOLT, QUEST_A_CONTROLLED_SPARK, Player.CLASS_MAGE);
    public static final FollowUp SHADOW_IN_RESERVE = new FollowUp(
            QUEST_SHADOW_IN_RESERVE, SPELL_SHADOW_BOLT, QUEST_THE_BOUND_FLAME, Player.CLASS_WARLOCK);
    public static final FollowUp FEL_AT_THE_EDGE = new FollowUp(
            QUEST_FEL_AT_THE_EDGE, SPELL_IMMOLATE, QUEST_THE_BOUND_FLAME, Player.CLASS_WARLOCK);
    public static final FollowUp A_FAMILIARS_FIRST_TASK = new FollowUp(
            QUEST_A_FAMILIARS_FIRST_TASK, SPELL_SUMMON_IMP, QUEST_THE_BOUND_FLAME, Player.CLASS_WARLOCK);
    public static final FollowUp MEND_THE_WOUNDED = new FollowUp(
            QUEST_MEND_THE_WOUNDED, SPELL_HEALING_WAVE, QUEST_LISTEN_TO_THE_ELEMENTS, Player.CLASS_SHAMAN);
    public static final FollowUp ANSWERING_SHOCK = new FollowUp(
            QUEST_ANSWERING_SHOCK, SPELL_EARTH_SHOCK, QUEST_LISTEN_TO_THE_ELEMENTS, Player.CLASS_SHAMAN);
    public static final FollowUp CALL_OF_EARTH = new FollowUp(
            QUEST_CALL_OF_EARTH, SPELL_STONESKIN_TOTEM, QUEST_LISTEN_TO_THE_ELEMENTS, Player.CLASS_SHAMAN);

    private static final List<FollowUp> FOLLOW_UPS = List.of(
            RALLY_THE_LINE, CLOSE_THE_DISTANCE, A_WOUND_TO_REMEMBER,
            STAND_FAST, STRENGTH_IN_SERVICE, MERCYS_LESSON,
            STEADY_AIM, VENOM_IN_THE_FIELD, A_CLEAN_SHOT,
            DISAPPEAR_FROM_SIGHT, FINISH_THE_OPENING, KEEP_THE_ADVANTAGE,
            JUDGMENT_FROM_AFAR, A_GUARDING_WORD, PAIN_AS_WARNING,
            A_COOLER_HEAD, SHARE_THE_STUDY, A_SECOND_SCHOOL,
            SHADOW_IN_RESERVE, FEL_AT_THE_EDGE, A_FAMILIARS_FIRST_TASK,
            MEND_THE_WOUNDED, ANSWERING_SHOCK, CALL_OF_EARTH);

    private static final Map<Integer, FollowUp> FOLLOW_UP_BY_QUEST = new HashMap<>();
    private static final Map<Integer, FollowUp> FOLLOW_UP_BY_SPELL = new HashMap<>();
    private static final Map<Integer, HeroClassUnlock> UNLOCK_BY_QUEST = new HashMap<>();
    private static final Map<Integer, HeroClassUnlock> UNLOCK_BY_CLASS = new HashMap<>();

    static {
        for (FollowUp fu : FOLLOW_UPS) {
            FOLLOW_UP_BY_QUEST.put(fu.questId(), fu);
            FOLLOW_UP_BY_SPELL.put(fu.rewardSpell(), fu);
        }
        UNLOCK_BY_QUEST.put(WARRIOR.questId, WARRIOR);
        UNLOCK_BY_QUEST.put(PALADIN.questId, PALADIN);
        UNLOCK_BY_QUEST.put(HUNTER.questId, HUNTER);
        UNLOCK_BY_QUEST.put(ROGUE.questId, ROGUE);
        UNLOCK_BY_QUEST.put(PRIEST.questId, PRIEST);
        UNLOCK_BY_QUEST.put(MAGE.questId, MAGE);
        UNLOCK_BY_QUEST.put(WARLOCK.questId, WARLOCK);
        UNLOCK_BY_QUEST.put(SHAMAN.questId, SHAMAN);
        UNLOCK_BY_CLASS.put(WARRIOR.classId, WARRIOR);
        UNLOCK_BY_CLASS.put(PALADIN.classId, PALADIN);
        UNLOCK_BY_CLASS.put(HUNTER.classId, HUNTER);
        UNLOCK_BY_CLASS.put(ROGUE.classId, ROGUE);
        UNLOCK_BY_CLASS.put(PRIEST.classId, PRIEST);
        UNLOCK_BY_CLASS.put(MAGE.classId, MAGE);
        UNLOCK_BY_CLASS.put(WARLOCK.classId, WARLOCK);
        UNLOCK_BY_CLASS.put(SHAMAN.classId, SHAMAN);
    }

    private final int classId;
    private final int questId;
    private final int trainerEntry;
    private final int starterSpell;
    private final int targetCreature;
    private final int requiredHits;
    private final int requiredKills;

    public HeroClassUnlock(int classId, int questId, int trainerEntry, int starterSpell,
                           int targetCreature, int requiredHits, int requiredKills) {
        this.classId = classId;
        this.questId = questId;
        this.trainerEntry = trainerEntry;
        this.starterSpell = starterSpell;
        this.targetCreature = targetCreature;
        this.requiredHits = requiredHits;
        this.requiredKills = requiredKills;
    }

    public int classId() {
        return classId;
    }

    public int questId() {
        return questId;
    }

    public int trainerEntry() {
        return trainerEntry;
    }

    public int starterSpell() {
        return starterSpell;
    }

    public int targetCreature() {
        return targetCreature;
    }

    public int requiredHits() {
        return requiredHits;
    }

    public int requiredKills() {
        return requiredKills;
    }

    public static Collection<FollowUp> followUps() {
        return FOLLOW_UPS;
    }

    public static FollowUp followUpForQuest(int questId) {
        return FOLLOW_UP_BY_QUEST.get(questId);
    }

    public static FollowUp followUpForSpell(int spellId) {
        return FOLLOW_UP_BY_SPELL.get(spellId);
    }

    public static HeroClassUnlock forQuest(int questId) {
        return UNLOCK_BY_QUEST.get(questId);
    }

    public static boolean isHeroOnly(int questId) {
        return forQuest(questId) != null || FOLLOW_UP_BY_QUEST.containsKey(questId);
    }

    /**
     * Class trainers without a Hero unlock path stay open. Warrior through Shaman require their
     * unlock quest rewarded.
     */
    public static boolean trainerClassUnlocked(Player p, int trainerClass) {
        HeroClassUnlock unlock = UNLOCK_BY_CLASS.get(trainerClass);
        if (unlock == null) {
            return true;
        }
        return p != null && p.rewardedQuests.contains(unlock.questId);
    }

    /**
     * Per-spell follow-up gate for Hero trainers. Spells without a follow-up stay open once the
     * class trainer is unlocked.
     */
    public static boolean spellUnlocked(Player p, int spellId) {
        FollowUp fu = FOLLOW_UP_BY_SPELL.get(spellId);
        if (fu == null) {
            return true;
        }
        return p != null && p.rewardedQuests.contains(fu.questId());
    }
}
