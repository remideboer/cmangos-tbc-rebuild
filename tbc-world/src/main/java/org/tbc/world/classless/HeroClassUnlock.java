package org.tbc.world.classless;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Player;
import org.tbc.world.spell.SpellEngine;

import java.util.Collection;
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
    public static final int NPC_HERO_WARRIOR_TRAINER = 91001;
    /** Blood Elf Sunstrider Isle warrior trainer display name. */
    public static final String NAME_LORVAEN_BLOODFEATHER = "Lorvaen Bloodfeather";
    public static final int CREATURE_MANA_WYRM = 15274;
    public static final int REQUIRED_HITS = 5;
    public static final int REQUIRED_KILLS = 1;
    public static final int FOLLOWUP_CHARGE_HITS = 3;
    /** Server-owned quest item for Rend follow-up. */
    public static final int ITEM_TRAINING_STRIP = 92001;
    /** Charge Rank 1 (Spell.dbc 100). */
    public static final int SPELL_CHARGE = 100;
    /** Rend Rank 1 (Spell.dbc 772). */
    public static final int SPELL_REND = 772;
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

    public static final HeroClassUnlock WARRIOR = new HeroClassUnlock(
            Player.CLASS_WARRIOR,
            QUEST_HEROS_FIRST_LESSON,
            NPC_HERO_WARRIOR_TRAINER,
            SpellEngine.HEROIC_STRIKE,
            CREATURE_MANA_WYRM,
            REQUIRED_HITS,
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

    private static final List<FollowUp> FOLLOW_UPS = List.of(
            RALLY_THE_LINE, CLOSE_THE_DISTANCE, A_WOUND_TO_REMEMBER);

    private static final Map<Integer, FollowUp> FOLLOW_UP_BY_QUEST = Map.of(
            QUEST_RALLY_THE_LINE, RALLY_THE_LINE,
            QUEST_CLOSE_THE_DISTANCE, CLOSE_THE_DISTANCE,
            QUEST_A_WOUND_TO_REMEMBER, A_WOUND_TO_REMEMBER);

    private static final Map<Integer, FollowUp> FOLLOW_UP_BY_SPELL = Map.of(
            Content.SPELL_BATTLE_SHOUT, RALLY_THE_LINE,
            SPELL_CHARGE, CLOSE_THE_DISTANCE,
            SPELL_REND, A_WOUND_TO_REMEMBER);

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
        if (questId == WARRIOR.questId) {
            return WARRIOR;
        }
        return null;
    }

    public static boolean isHeroOnly(int questId) {
        return forQuest(questId) != null || FOLLOW_UP_BY_QUEST.containsKey(questId);
    }

    /** Other class trainers stay open until their unlock quest exists. */
    public static boolean trainerClassUnlocked(Player p, int trainerClass) {
        if (trainerClass != Player.CLASS_WARRIOR) {
            return true;
        }
        return p != null && p.rewardedQuests.contains(WARRIOR.questId);
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
