package org.tbc.world.spell;

import org.tbc.world.content.WeaponSkills;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-198 — SPELL_AURA_MOD_SKILL (30).
 * CMaNGOS HandleAuraModSkill → ModifySkillBonus(skillId, amount, permanent=false).
 */
class AuraEngineModSkillTest {
    private static final SpellEngine.SpellInfo DEFENSE_BONUS = new SpellEngine.SpellInfo(
            999030, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SKILL, 0, 0, 10, 10, 0f, WeaponSkills.SKILL_DEFENSE);

    @Test
    void applyAuraWhenModSkillShouldRaiseTemporarySkillBonus() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SKILL));
        Player player = new Player();
        player.setSkill(0, WeaponSkills.SKILL_DEFENSE, 100, 300);

        eng.apply(player, player, DEFENSE_BONUS);

        assertTrue(player.hasAura(999030));
        assertEquals(10, player.skillTempBonus(WeaponSkills.SKILL_DEFENSE));
        assertEquals(0, player.skillPermBonus(WeaponSkills.SKILL_DEFENSE));
        int bonusWord = player.getInt(UpdateFields.PLAYER_SKILL_INFO_1_1 + 2);
        assertEquals(10, bonusWord & 0xFFFF);
    }

    @Test
    void unapplyWhenModSkillShouldClearTemporarySkillBonus() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setSkill(0, WeaponSkills.SKILL_DEFENSE, 100, 300);
        eng.apply(player, player, DEFENSE_BONUS);
        eng.auras().unapply(player, DEFENSE_BONUS);
        assertEquals(0, player.skillTempBonus(WeaponSkills.SKILL_DEFENSE));
    }

    @Test
    void applyWhenSkillNotLearnedShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, DEFENSE_BONUS);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_SKILL_INFO_1_1 + 2));
    }

    @Test
    void applyWhenAmountZeroOrCreatureShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setSkill(0, WeaponSkills.SKILL_DEFENSE, 100, 300);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999031, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SKILL,
                0, 0, 0, 0, 0f, WeaponSkills.SKILL_DEFENSE));
        assertEquals(0, player.skillTempBonus(WeaponSkills.SKILL_DEFENSE));
        eng.apply(new Player(), new Creature(), DEFENSE_BONUS);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, DEFENSE_BONUS);
        new AuraEngine().unapply(null, DEFENSE_BONUS);
    }
}
