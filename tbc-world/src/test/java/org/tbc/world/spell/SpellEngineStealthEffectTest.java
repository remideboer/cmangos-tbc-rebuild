package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-117 — SPELL_EFFECT_STEALTH (48). Base Stealth 2426 — one spell.
 * CMaNGOS EffectUnused: real stealth is the MOD_STEALTH aura (16). Cataloged, bytes/auras unchanged.
 */
class SpellEngineStealthEffectTest {
    @Test
    void applyStealthEffectWhenPlayerShouldBeCatalogedAndNotStealth() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_STEALTH));
        Player p = new Player();
        int bytes1 = p.getInt(UpdateFields.UNIT_FIELD_BYTES_1);
        SpellEngine.SpellInfo baseStealth = new SpellEngine.SpellInfo(
                2426, SpellEngine.EFFECT_STEALTH, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, p, baseStealth));
        assertEquals(bytes1, p.getInt(UpdateFields.UNIT_FIELD_BYTES_1));
        assertEquals(0, p.auras.size());
    }
}
