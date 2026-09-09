package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-116 — SPELL_EFFECT_BIND_SIGHT (82). Gnome Car Camera 6955.
 * CMaNGOS EffectNULL: farsight comes from ADD_FARSIGHT (72), not this effect. Cataloged, camera unchanged.
 */
class SpellEngineBindSightTest {
    @Test
    void applyBindSightWhenPlayerShouldBeCatalogedAndLeaveFarsightUnbound() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_BIND_SIGHT));
        Player p = new Player();
        Creature car = new Creature();
        car.guid = 0xF130000000000042L;
        SpellEngine.SpellInfo camera = new SpellEngine.SpellInfo(
                6955, SpellEngine.EFFECT_BIND_SIGHT, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, car, camera));
        assertEquals(0L, p.getGuid(UpdateFields.PLAYER_FARSIGHT));
        assertEquals(0, car.auras.size());
    }
}
