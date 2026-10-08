package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-189 — SPELL_AURA_MOD_UNATTACKABLE (93).
 * CMaNGOS HandleModUnattackable → UNIT_FLAG_UNTARGETABLE + CombatStop on apply.
 */
class AuraEngineModUnattackableTest {
    private static final SpellEngine.SpellInfo UNATTACKABLE = new SpellEngine.SpellInfo(
            999093, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_UNATTACKABLE, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModUnattackableShouldSetUntargetableFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_UNATTACKABLE));
        Player player = new Player();
        eng.apply(player, player, UNATTACKABLE);
        assertTrue(player.hasAura(999093));
        assertEquals(Unit.UNIT_FLAG_UNTARGETABLE,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_UNTARGETABLE);
    }

    @Test
    void applyWhenModUnattackableShouldCombatStop() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.inCombat = true;
        player.victim = 42;
        eng.apply(player, player, UNATTACKABLE);
        assertFalse(player.inCombat);
        assertEquals(0, player.victim);
    }

    @Test
    void unapplyWhenModUnattackableShouldClearUntargetableFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, UNATTACKABLE);
        eng.auras().unapply(player, UNATTACKABLE);
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_UNTARGETABLE);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, UNATTACKABLE);
        new AuraEngine().unapply(null, UNATTACKABLE);
    }
}
