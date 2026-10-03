package org.tbc.world.classless;

import org.tbc.world.entity.Player;
import org.tbc.world.spell.SpellEngine;

/**
 * Server-owned Hero class unlock quest (warrior pilot). Unlock is {@code rewardedQuests}.
 */
public final class HeroClassUnlock {
    public static final int QUEST_HEROS_FIRST_LESSON = 90001;
    public static final int NPC_HERO_WARRIOR_TRAINER = 91001;
    public static final int CREATURE_MANA_WYRM = 15274;
    public static final int REQUIRED_HITS = 5;
    public static final int REQUIRED_KILLS = 1;
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

    public static HeroClassUnlock forQuest(int questId) {
        if (questId == WARRIOR.questId) {
            return WARRIOR;
        }
        return null;
    }

    public static boolean isHeroOnly(int questId) {
        return forQuest(questId) != null;
    }

    /** Other class trainers stay open until their unlock quest exists. */
    public static boolean trainerClassUnlocked(Player p, int trainerClass) {
        if (trainerClass != Player.CLASS_WARRIOR) {
            return true;
        }
        return p != null && p.rewardedQuests.contains(WARRIOR.questId);
    }
}
