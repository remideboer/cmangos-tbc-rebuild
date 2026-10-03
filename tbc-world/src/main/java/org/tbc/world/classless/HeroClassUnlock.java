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

    public static final int NPC_HERO_WARRIOR_TRAINER = 91001;
    public static final int NPC_HERO_PALADIN_TRAINER = 91002;
    /** Blood Elf Sunstrider Isle warrior trainer display name. */
    public static final String NAME_LORVAEN_BLOODFEATHER = "Lorvaen Bloodfeather";
    /** Blood Elf Sunstrider Isle paladin trainer display name. */
    public static final String NAME_VELAARA_SUNWARD = "Velaara Sunward";
    public static final int CREATURE_MANA_WYRM = 15274;
    public static final int REQUIRED_HITS = 5;
    public static final int REQUIRED_KILLS = 1;
    public static final int FOLLOWUP_CHARGE_HITS = 3;
    /** Server-owned quest item for Rend follow-up. */
    public static final int ITEM_TRAINING_STRIP = 92001;
    /** Paladin unlock: protective token. */
    public static final int ITEM_PROTECTIVE_TOKEN = 92002;
    /** Blessing of Might follow-up delivery token. */
    public static final int ITEM_BLESSING_TOKEN = 92003;
    /** Holy Light follow-up healing kit. */
    public static final int ITEM_HEALING_KIT = 92004;
    /** Charge Rank 1 (Spell.dbc 100). */
    public static final int SPELL_CHARGE = 100;
    /** Rend Rank 1 (Spell.dbc 772). */
    public static final int SPELL_REND = 772;
    /** Blessing of Might Rank 1 (Spell.dbc 19740). */
    public static final int SPELL_BLESSING_OF_MIGHT = 19740;
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

    private static final List<FollowUp> FOLLOW_UPS = List.of(
            RALLY_THE_LINE, CLOSE_THE_DISTANCE, A_WOUND_TO_REMEMBER,
            STAND_FAST, STRENGTH_IN_SERVICE, MERCYS_LESSON);

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
        UNLOCK_BY_CLASS.put(WARRIOR.classId, WARRIOR);
        UNLOCK_BY_CLASS.put(PALADIN.classId, PALADIN);
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
     * Class trainers without a Hero unlock path stay open. Warrior/Paladin require their unlock
     * quest rewarded.
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
