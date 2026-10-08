package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-192 — SPELL_AURA_EMPATHY (121).
 * CMaNGOS HandleAuraEmpathy → UNIT_DYNFLAG_SPECIALINFO (player / beast).
 */
class AuraEngineEmpathyTest {
    private static final SpellEngine.SpellInfo EMPATHY = new SpellEngine.SpellInfo(
            999121, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_EMPATHY, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenEmpathyShouldSetSpecialInfoDynflag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_EMPATHY));
        Player player = new Player();
        eng.apply(player, player, EMPATHY);
        assertTrue(player.hasAura(999121));
        assertEquals(Unit.UNIT_DYNFLAG_SPECIALINFO,
                player.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_SPECIALINFO);
    }

    @Test
    void unapplyWhenEmpathyShouldClearSpecialInfoDynflag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, EMPATHY);
        eng.auras().unapply(player, EMPATHY);
        assertEquals(0, player.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_SPECIALINFO);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, EMPATHY);
        new AuraEngine().unapply(null, EMPATHY);
    }
}
