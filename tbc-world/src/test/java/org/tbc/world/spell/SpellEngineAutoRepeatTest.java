package org.tbc.world.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineAutoRepeatTest {
    @Test
    void interruptAutoRepeatWhenArmedShouldClearCurrentAutorepeat() {
        SpellEngine eng = SpellEngine.alwaysHit();
        eng.armAutoRepeat(1);
        assertTrue(eng.hasAutoRepeat(1));
        eng.interruptAutoRepeat(1);
        assertFalse(eng.hasAutoRepeat(1));
        eng.interruptAutoRepeat(1);
        assertFalse(eng.hasAutoRepeat(1));
    }
}
