package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-119 — SPELL_EFFECT_CREATE_HOUSE (81). Create House (TEST) 6757 misc 20668 — one spell.
 * CMaNGOS EffectUnused. Cataloged, no gameobject spawned.
 */
class SpellEngineCreateHouseTest {
    @Test
    void applyCreateHouseWhenPlayerShouldBeCatalogedAndSpawnNothing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_CREATE_HOUSE));
        Player p = new Player();
        SpellEngine.SpellInfo createHouse = new SpellEngine.SpellInfo(
                6757, SpellEngine.EFFECT_CREATE_HOUSE, 0, 0, 0, 0, 0, 0f, 20668);
        assertEquals(0, eng.apply(p, p, createHouse));
        assertNull(p.lastWildObject());
        assertNull(p.lastTransmittedObject());
        assertEquals(0, p.auras.size());
    }
}
