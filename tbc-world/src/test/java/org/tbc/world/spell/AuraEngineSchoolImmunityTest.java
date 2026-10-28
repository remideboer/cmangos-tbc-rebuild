package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-236 — SPELL_AURA_SCHOOL_IMMUNITY (39).
 * Sets UNIT_FLAG_IMMUNE and a school mask; the flag clears when the last aura drops.
 */
class AuraEngineSchoolImmunityTest {
    private static SpellEngine.SpellInfo immune(int id, int mask) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_SCHOOL_IMMUNITY, 0, 0, 1, 1, 0f, mask);
    }

    @Test
    void applyAuraWhenSchoolImmunityShouldSetTheFlagUntilTheLastAuraDrops() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_SCHOOL_IMMUNITY));
        Player player = new Player();
        SpellEngine.SpellInfo fire = immune(999039, 4);
        SpellEngine.SpellInfo frost = immune(999040, 16);
        eng.apply(player, player, fire);
        eng.apply(player, player, frost);
        int flags = player.getInt(UpdateFields.UNIT_FIELD_FLAGS);
        assertTrue((flags & Unit.UNIT_FLAG_IMMUNE) != 0);
        assertTrue((flags & Unit.UNIT_FLAG_PLAYER_CONTROLLED) != 0);
        assertTrue(player.isSchoolImmune(4));
        assertTrue(player.isSchoolImmune(16));
        assertFalse(player.isSchoolImmune(1));
        eng.auras().unapply(player, fire);
        assertTrue((player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_IMMUNE) != 0);
        assertFalse(player.isSchoolImmune(4));
        assertTrue(player.isSchoolImmune(16));
        eng.auras().unapply(player, frost);
        assertEquals(Unit.UNIT_FLAG_PLAYER_CONTROLLED, player.getInt(UpdateFields.UNIT_FIELD_FLAGS));
        assertFalse(player.isSchoolImmune(16));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, immune(999039, 4));
        new AuraEngine().unapply(null, immune(999039, 4));
    }
}
