package org.tbc.world.entity;

import org.tbc.world.content.WeaponSkills;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL12-002 — CMaNGOS UpdateSkill / UpdateCombatSkills / UpdateWeaponSkill.
 * White swing can raise weapon skill toward max; no gain at cap or missing skill.
 */
class PlayerCombatSkillsTest {
    @Test
    void updateSkillWhenBelowMaxShouldRaiseValue() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(WeaponSkills.SKILL_SWORDS, 1, 5, 0);
        assertTrue(p.updateSkill(WeaponSkills.SKILL_SWORDS, 1));
        assertEquals(2, p.skillValue(WeaponSkills.SKILL_SWORDS));
        List<Integer> dirty = p.takeDirtySkillFields();
        assertFalse(dirty.isEmpty());
        assertEquals(2, p.getInt(dirty.get(1)) & 0xFFFF);
    }

    @Test
    void updateSkillWhenAtMaxShouldNoOp() {
        Player p = new Player();
        p.learnSkill(WeaponSkills.SKILL_SWORDS, 5, 5, 0);
        assertFalse(p.updateSkill(WeaponSkills.SKILL_SWORDS, 1));
        assertEquals(5, p.skillValue(WeaponSkills.SKILL_SWORDS));
    }

    @Test
    void updateSkillWhenMissingShouldNoOp() {
        Player p = new Player();
        assertFalse(p.updateSkill(WeaponSkills.SKILL_SWORDS, 1));
    }

    @Test
    void updateCombatSkillsWhenChanceSucceedsShouldRaiseMainhandWeaponSkill() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(WeaponSkills.SKILL_SWORDS, 1, 5, 0);
        Item sword = new Item(1, 25);
        sword.subClass = 7;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.items.put(1, sword);

        assertTrue(p.updateCombatSkills(false, false, () -> 0.0));
        assertEquals(2, p.skillValue(WeaponSkills.SKILL_SWORDS));
    }

    @Test
    void updateCombatSkillsWhenUnarmedShouldRaiseUnarmedSkill() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(WeaponSkills.SKILL_UNARMED, 1, 5, 0);
        assertTrue(p.updateCombatSkills(false, false, () -> 0.0));
        assertEquals(2, p.skillValue(WeaponSkills.SKILL_UNARMED));
    }

    @Test
    void updateCombatSkillsWhenDefenseShouldRaiseDefenseSkill() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(WeaponSkills.SKILL_DEFENSE, 1, 5, 0);
        assertTrue(p.updateCombatSkills(true, false, () -> 0.0));
        assertEquals(2, p.skillValue(WeaponSkills.SKILL_DEFENSE));
    }

    @Test
    void updateCombatSkillsWhenAtCapShouldNoOp() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(WeaponSkills.SKILL_SWORDS, 5, 5, 0);
        Item sword = new Item(1, 25);
        sword.subClass = 7;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.items.put(1, sword);
        assertFalse(p.updateCombatSkills(false, false, () -> 0.0));
        assertEquals(5, p.skillValue(WeaponSkills.SKILL_SWORDS));
    }

    @Test
    void updateCombatSkillsWhenChanceMissesShouldLeaveSkill() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(WeaponSkills.SKILL_SWORDS, 1, 5, 0);
        Item sword = new Item(1, 25);
        sword.subClass = 7;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.items.put(1, sword);
        assertFalse(p.updateCombatSkills(false, false, () -> 0.999));
        assertEquals(1, p.skillValue(WeaponSkills.SKILL_SWORDS));
    }

    @Test
    void weaponSkillIdForAttackWhenMainhandSwordShouldBeSwords() {
        Player p = new Player();
        Item sword = new Item(1, 25);
        sword.subClass = 7;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.items.put(1, sword);
        assertEquals(WeaponSkills.SKILL_SWORDS, p.weaponSkillIdForAttack(false));
    }
}
