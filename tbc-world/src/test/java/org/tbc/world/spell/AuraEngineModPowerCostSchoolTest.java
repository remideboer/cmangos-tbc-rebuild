package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-164 — SPELL_AURA_MOD_POWER_COST_SCHOOL (73). Frost Focus 11068:
 * EffectBasePoints −2 → −1 after +1? Wait EffectBasePoints -2 + 1 = −1 mana cost frost (mask 16).
 * CMaNGOS HandleModPowerCost → UNIT_FIELD_POWER_COST_MODIFIER + school.
 */
class AuraEngineModPowerCostSchoolTest {
    /** Frost school bit 4 → mask 16. Amount −1 (EffectBasePoints −2 + 1). */
    private static final SpellEngine.SpellInfo FROST_FOCUS = new SpellEngine.SpellInfo(
            SpellEngine.FROST_FOCUS, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL, 0, 0, -1, -1, 0f, 16);

    @Test
    void applyAuraWhenModPowerCostSchoolOnPlayerShouldLowerFrostCostModifier() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL));
        Player player = new Player();

        eng.apply(player, player, FROST_FOCUS);

        assertTrue(player.hasAura(SpellEngine.FROST_FOCUS));
        assertEquals(-1, player.getInt(UpdateFields.UNIT_FIELD_POWER_COST_MODIFIER + 4));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_POWER_COST_MODIFIER));
    }

    @Test
    void unapplyWhenModPowerCostSchoolShouldRestoreModifier() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, FROST_FOCUS);
        eng.unapplyAura(player, SpellEngine.FROST_FOCUS);
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_POWER_COST_MODIFIER + 4));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveModifiers() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999073, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL,
                0, 0, 0, 0, 0f, 16));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_POWER_COST_MODIFIER + 4));
    }

    @Test
    void applyWhenMaskZeroShouldLeaveModifiers() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999074, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL,
                0, 0, -1, -1, 0f, 0));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_POWER_COST_MODIFIER + 4));
    }

    @Test
    void applyWhenCreatureShouldRaiseMatchingSchoolModifier() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FROST_FOCUS);
        assertTrue(mob.hasAura(SpellEngine.FROST_FOCUS));
        assertEquals(-1, mob.getInt(UpdateFields.UNIT_FIELD_POWER_COST_MODIFIER + 4));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FROST_FOCUS);
        new AuraEngine().unapply(null, FROST_FOCUS);
    }
}
