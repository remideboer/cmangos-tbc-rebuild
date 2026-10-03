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

    public static final int NPC_HERO_WARRIOR_TRAINER = 91001;
    public static final int NPC_HERO_PALADIN_TRAINER = 91002;
    public static final int NPC_HERO_HUNTER_TRAINER = 91003;
    public static final int NPC_HERO_ROGUE_TRAINER = 91004;
    public static final int NPC_HERO_PRIEST_TRAINER = 91005;
    public static final int NPC_HERO_MAGE_TRAINER = 91006;
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

    private static final List<FollowUp> FOLLOW_UPS = List.of(
            RALLY_THE_LINE, CLOSE_THE_DISTANCE, A_WOUND_TO_REMEMBER,
            STAND_FAST, STRENGTH_IN_SERVICE, MERCYS_LESSON,
            STEADY_AIM, VENOM_IN_THE_FIELD, A_CLEAN_SHOT,
            DISAPPEAR_FROM_SIGHT, FINISH_THE_OPENING, KEEP_THE_ADVANTAGE,
            JUDGMENT_FROM_AFAR, A_GUARDING_WORD, PAIN_AS_WARNING,
            A_COOLER_HEAD, SHARE_THE_STUDY, A_SECOND_SCHOOL);

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
        UNLOCK_BY_CLASS.put(WARRIOR.classId, WARRIOR);
        UNLOCK_BY_CLASS.put(PALADIN.classId, PALADIN);
        UNLOCK_BY_CLASS.put(HUNTER.classId, HUNTER);
        UNLOCK_BY_CLASS.put(ROGUE.classId, ROGUE);
        UNLOCK_BY_CLASS.put(PRIEST.classId, PRIEST);
        UNLOCK_BY_CLASS.put(MAGE.classId, MAGE);
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
     * Class trainers without a Hero unlock path stay open. Warrior through Priest require their
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
