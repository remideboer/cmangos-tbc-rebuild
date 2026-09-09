package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-106 — SPELL_EFFECT_LANGUAGE (39). Language Common 668 misc LANG_COMMON 7.
 * CMaNGOS EffectEmpty: marker / client inform only — cataloged, mutates nothing.
 */
class SpellEngineLanguageTest {
    @Test
    void applyLanguageWhenLivingPlayerShouldBeCatalogedAndMutateNothing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_LANGUAGE));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        p.spells.add(668);
        SpellEngine.SpellInfo common = new SpellEngine.SpellInfo(
                668, SpellEngine.EFFECT_LANGUAGE, 0, 0, 0, 0, 0, 0f, 7);
        assertEquals(0, eng.apply(p, p, common));
        assertEquals(100, p.health());
        assertEquals(0, p.auras.size());
        assertEquals(1, p.spells.size());
        assertNull(p.lastDynObject());
    }
}
