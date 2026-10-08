package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-191 — SPELL_AURA_UNTRACKABLE (120).
 * CMaNGOS HandleAuraUntrackable → UNIT_VIS_FLAG_UNTRACKABLE on UNIT_FIELD_BYTES_1.
 */
class AuraEngineUntrackableTest {
    private static final SpellEngine.SpellInfo UNTRACKABLE = new SpellEngine.SpellInfo(
            999120, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_UNTRACKABLE, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenUntrackableShouldSetVisFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_UNTRACKABLE));
        Player player = new Player();
        eng.apply(player, player, UNTRACKABLE);
        assertTrue(player.hasAura(999120));
        assertTrue(player.hasVisFlagUntrackable());
    }

    @Test
    void unapplyWhenUntrackableShouldClearVisFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, UNTRACKABLE);
        eng.auras().unapply(player, UNTRACKABLE);
        assertFalse(player.hasVisFlagUntrackable());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, UNTRACKABLE);
        new AuraEngine().unapply(null, UNTRACKABLE);
    }
}
