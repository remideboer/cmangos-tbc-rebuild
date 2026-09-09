package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-107 — SPELL_EFFECT_DODGE (20). Dodge 81 misc 90.
 * CMaNGOS EffectEmpty: one spell, marker only — cataloged, mutates nothing.
 */
class SpellEngineDodgeTest {
    @Test
    void applyDodgeWhenLivingPlayerShouldBeCatalogedAndMutateNothing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DODGE));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        p.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 5f);
        SpellEngine.SpellInfo dodge = new SpellEngine.SpellInfo(
                81, SpellEngine.EFFECT_DODGE, 0, 0, 0, 0, 0, 0f, 90);
        assertEquals(0, eng.apply(p, p, dodge));
        assertEquals(100, p.health());
        assertEquals(5f, p.getFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE), 0.001f);
        assertEquals(0, p.auras.size());
    }
}
