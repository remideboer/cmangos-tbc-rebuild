package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-229 — SPELL_AURA_FORCE_MOVE_FORWARD (241).
 * CMaNGOS HandleForceMoveForward sets UNIT_FLAG2_FORCE_MOVE.
 */
class AuraEngineForceMoveForwardTest {
    private static final SpellEngine.SpellInfo FORCE = new SpellEngine.SpellInfo(
            999241, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_FORCE_MOVE_FORWARD, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenForceMoveForwardShouldSetTheFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_FORCE_MOVE_FORWARD));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_FLAGS_2, Unit.UNIT_FLAG2_COMPREHEND_LANG);
        eng.apply(player, player, FORCE);
        assertEquals(Unit.UNIT_FLAG2_COMPREHEND_LANG | Unit.UNIT_FLAG2_FORCE_MOVE,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2));
    }

    @Test
    void unapplyWhenForceMoveForwardShouldClearOnlyThatFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_FLAGS_2, Unit.UNIT_FLAG2_COMPREHEND_LANG);
        eng.apply(player, player, FORCE);
        eng.auras().unapply(player, FORCE);
        assertEquals(Unit.UNIT_FLAG2_COMPREHEND_LANG, player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FORCE);
    }
}
