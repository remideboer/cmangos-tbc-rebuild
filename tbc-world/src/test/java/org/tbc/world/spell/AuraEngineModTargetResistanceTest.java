package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-180 — SPELL_AURA_MOD_TARGET_RESISTANCE (123).
 * CMaNGOS HandleModTargetResistance: NORMAL → PLAYER_FIELD_MOD_TARGET_PHYSICAL_RESISTANCE;
 * full SPELL mask (124) → PLAYER_FIELD_MOD_TARGET_RESISTANCE (spell penetration).
 */
class AuraEngineModTargetResistanceTest {
    private static final SpellEngine.SpellInfo SPELL_PEN = new SpellEngine.SpellInfo(
            999123, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_TARGET_RESISTANCE, 0, 0, 20, 20, 0f,
            AuraEngine.SPELL_SCHOOL_MASK_SPELL);

    private static final SpellEngine.SpellInfo ARMOR_PEN = new SpellEngine.SpellInfo(
            999124, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_TARGET_RESISTANCE, 0, 0, 15, 15, 0f, 1);

    @Test
    void applyAuraWhenSpellSchoolMaskShouldRaiseTargetResistance() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_TARGET_RESISTANCE));
        Player player = new Player();

        eng.apply(player, player, SPELL_PEN);

        assertTrue(player.hasAura(999123));
        assertEquals(20, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_RESISTANCE));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_PHYSICAL_RESISTANCE));
    }

    @Test
    void applyWhenNormalSchoolMaskShouldRaiseTargetPhysicalResistance() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, ARMOR_PEN);
        assertEquals(15, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_PHYSICAL_RESISTANCE));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_RESISTANCE));
    }

    @Test
    void unapplyWhenModTargetResistanceShouldRestoreFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, SPELL_PEN);
        eng.apply(player, player, ARMOR_PEN);
        eng.auras().unapply(player, SPELL_PEN);
        eng.auras().unapply(player, ARMOR_PEN);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_RESISTANCE));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_PHYSICAL_RESISTANCE));
    }

    @Test
    void applyWhenPartialSpellMaskShouldLeaveSpellPen() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        // fire only — not full SPELL mask
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999125, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_TARGET_RESISTANCE,
                0, 0, 20, 20, 0f, 1 << 2));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_RESISTANCE));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999126, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_TARGET_RESISTANCE,
                0, 0, 0, 0, 0f, AuraEngine.SPELL_SCHOOL_MASK_SPELL));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_RESISTANCE));
    }

    @Test
    void applyWhenCreatureShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, SPELL_PEN);
        assertTrue(mob.hasAura(999123));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, SPELL_PEN);
        new AuraEngine().unapply(null, SPELL_PEN);
    }
}
