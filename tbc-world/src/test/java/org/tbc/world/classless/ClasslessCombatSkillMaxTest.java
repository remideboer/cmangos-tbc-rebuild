package org.tbc.world.classless;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.content.WeaponSkills;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** TP-SL35-030 — Hero combat skill max rises by 5 per level (CMaNGOS GetSkillMaxForLevel). */
class ClasslessCombatSkillMaxTest {
    @AfterEach
    void reset() {
        ClasslessConfig.reset();
    }

    @Test
    void giveXpWhenHeroDingsShouldRaiseCombatSkillMaxByFive() {
        Player p = heroWithCombatSkillsAtLevel(1);
        assertEquals(5, p.skillMax(WeaponSkills.SKILL_SWORDS));
        assertEquals(5, p.skillMax(WeaponSkills.SKILL_DEFENSE));
        assertEquals(1, p.skillValue(WeaponSkills.SKILL_SWORDS));

        p.giveXp(400, null);
        assertEquals(2, p.level);
        assertEquals(10, p.skillMax(WeaponSkills.SKILL_SWORDS));
        assertEquals(10, p.skillMax(WeaponSkills.SKILL_DEFENSE));
        assertEquals(1, p.skillValue(WeaponSkills.SKILL_SWORDS));

        p.learnSkill(WeaponSkills.SKILL_SWORDS, 5, p.skillMax(WeaponSkills.SKILL_SWORDS), 0);
        int need = p.getInt(UpdateFields.PLAYER_NEXT_LEVEL_XP);
        p.giveXp(need, null);
        assertEquals(3, p.level);
        assertEquals(15, p.skillMax(WeaponSkills.SKILL_SWORDS));
        assertEquals(5, p.skillValue(WeaponSkills.SKILL_SWORDS));
    }

    @Test
    void ensureStartingProficienciesWhenHeroAboveLevelOneShouldRefreshCombatSkillMax() {
        Player p = heroWithCombatSkillsAtLevel(1);
        assertEquals(5, p.skillMax(WeaponSkills.SKILL_SWORDS));
        p.learnSkill(WeaponSkills.SKILL_SWORDS, 3, 5, 0);
        p.level = 10;

        ClasslessCharacterPolicy.ensureStartingProficiencies(p);

        assertEquals(50, p.skillMax(WeaponSkills.SKILL_SWORDS));
        assertEquals(50, p.skillMax(WeaponSkills.SKILL_DEFENSE));
        assertEquals(3, p.skillValue(WeaponSkills.SKILL_SWORDS));
    }

    private static Player heroWithCombatSkillsAtLevel(int level) {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 1L;
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = level;
        long[] next = {100L};
        ClasslessStartingLoadout.apply(p, mgr, () -> next[0]++);
        ClasslessCharacterPolicy.applyStartingStats(p);
        return p;
    }
}
