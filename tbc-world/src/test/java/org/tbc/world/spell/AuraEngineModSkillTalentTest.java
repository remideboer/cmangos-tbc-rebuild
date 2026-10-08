package org.tbc.world.spell;

import org.tbc.world.content.WeaponSkills;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-199 — SPELL_AURA_MOD_SKILL_TALENT (98).
 * CMaNGOS HandleAuraModSkill → ModifySkillBonus(skillId, amount, permanent=true).
 */
class AuraEngineModSkillTalentTest {
    private static final SpellEngine.SpellInfo DEFENSE_TALENT = new SpellEngine.SpellInfo(
            999098, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SKILL_TALENT, 0, 0, 5, 5, 0f, WeaponSkills.SKILL_DEFENSE);

    @Test
    void applyAuraWhenModSkillTalentShouldRaisePermanentSkillBonus() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SKILL_TALENT));
        Player player = new Player();
        player.setSkill(0, WeaponSkills.SKILL_DEFENSE, 100, 300);

        eng.apply(player, player, DEFENSE_TALENT);

        assertTrue(player.hasAura(999098));
        assertEquals(5, player.skillPermBonus(WeaponSkills.SKILL_DEFENSE));
        assertEquals(0, player.skillTempBonus(WeaponSkills.SKILL_DEFENSE));
        int bonusWord = player.getInt(UpdateFields.PLAYER_SKILL_INFO_1_1 + 2);
        assertEquals(5, (bonusWord >>> 16) & 0xFFFF);
    }

    @Test
    void unapplyWhenModSkillTalentShouldClearPermanentSkillBonus() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setSkill(0, WeaponSkills.SKILL_DEFENSE, 100, 300);
        eng.apply(player, player, DEFENSE_TALENT);
        eng.auras().unapply(player, DEFENSE_TALENT);
        assertEquals(0, player.skillPermBonus(WeaponSkills.SKILL_DEFENSE));
    }

    @Test
    void applyWhenSkillNotLearnedOrAmountZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, DEFENSE_TALENT);
        assertEquals(0, player.skillPermBonus(WeaponSkills.SKILL_DEFENSE));
        player.setSkill(0, WeaponSkills.SKILL_DEFENSE, 100, 300);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999099, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SKILL_TALENT,
                0, 0, 0, 0, 0f, WeaponSkills.SKILL_DEFENSE));
        assertEquals(0, player.skillPermBonus(WeaponSkills.SKILL_DEFENSE));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, DEFENSE_TALENT);
        new AuraEngine().unapply(null, DEFENSE_TALENT);
    }
}
