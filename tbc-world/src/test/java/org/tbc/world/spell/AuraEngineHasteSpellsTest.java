package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-213 — SPELL_AURA_HASTE_SPELLS (216).
 * CMaNGOS uses HandleModCastingSpeed, the same path as aura 65.
 */
class AuraEngineHasteSpellsTest {
    private static final float EPS = 1e-5f;
    private static final SpellEngine.SpellInfo HASTE = new SpellEngine.SpellInfo(
            999216, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_HASTE_SPELLS, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenHasteSpellsShouldReduceModCastSpeed() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_HASTE_SPELLS));
        Player player = new Player();
        Creature mob = new Creature();
        mob.setFloat(UpdateFields.UNIT_MOD_CAST_SPEED, 1.0f);
        eng.apply(player, player, HASTE);
        eng.apply(player, mob, HASTE);
        assertEquals(100.0f / 120.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
        assertEquals(100.0f / 120.0f, mob.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }

    @Test
    void unapplyWhenHasteSpellsShouldRestoreModCastSpeed() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, HASTE);
        eng.auras().unapply(player, HASTE);
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }

    @Test
    void applyWhenAmountZeroOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HASTE);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999217, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_HASTE_SPELLS,
                0, 0, 0, 0, 0f));
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }
}
