package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-112 — SPELL_EFFECT_EVADE (21). Evade (DND) 108 misc 90 — one spell, creature passive.
 * CMaNGOS EffectEmpty: evade state comes from the AI, not this effect. Cataloged, mutates nothing.
 */
class SpellEngineEvadeTest {
    @Test
    void applyEvadeWhenCreatureShouldBeCatalogedAndMutateNothing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_EVADE));
        Creature c = new Creature();
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        c.setHealth(100);
        SpellEngine.SpellInfo evade = new SpellEngine.SpellInfo(
                108, SpellEngine.EFFECT_EVADE, 0, 0, 0, 0, 0, 0f, 90);
        assertEquals(0, eng.apply(c, c, evade));
        assertEquals(100, c.health());
        assertEquals(0, c.auras.size());
    }
}
