package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-091 — SPELL_EFFECT_SUMMON (28). Eye of Kilrogg 126 misc 4277.
 * CMaNGOS EffectSummonType: creature at caster (dest stand-in).
 */
class SpellEngineSummonTest {
    @Test
    void applySummonWhenPlayerShouldPlaceKilroggAtCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON));
        Player p = new Player();
        p.guid = 11;
        p.relocate(4f, 5f, 6f, 1.2f);
        SpellEngine.SpellInfo eye = new SpellEngine.SpellInfo(
                126, SpellEngine.EFFECT_SUMMON, 0, 0, 0, 0, 0, 0f, 4277);
        eng.apply(p, p, eye);
        Creature summoned = p.lastSummon();
        assertNotNull(summoned);
        assertEquals(4277, summoned.entry);
        assertEquals(4f, summoned.x, 0.01f);
        assertEquals(5f, summoned.y, 0.01f);
        assertEquals(6f, summoned.z, 0.01f);
        assertNull(p.pet);
    }

    @Test
    void applySummonWhenMissingEntryShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                126, SpellEngine.EFFECT_SUMMON, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(p, p, none);
        assertNull(p.lastSummon());
        eng.summon(null, 4277);
        eng.summon(p, 0);
        assertNull(p.lastSummon());
    }
}
