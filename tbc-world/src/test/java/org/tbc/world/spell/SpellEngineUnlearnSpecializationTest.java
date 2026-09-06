package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-054 — SPELL_EFFECT_UNLEARN_SPECIALIZATION (133). Unlearn Spellfire Tailoring 41299 / 26797. */
class SpellEngineUnlearnSpecializationTest {
    @Test
    void applyUnlearnSpecializationWhenKnownShouldRemoveTriggerSpell() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_UNLEARN_SPECIALIZATION));
        Player target = new Player();
        target.spells.add(26797);
        SpellEngine.SpellInfo forget = new SpellEngine.SpellInfo(
                41299, SpellEngine.EFFECT_UNLEARN_SPECIALIZATION, 0, 0, 0, 0, 0, 0f, 26797);
        eng.apply(new Player(), target, forget);
        assertFalse(target.spells.contains(26797));
    }

    @Test
    void applyUnlearnSpecializationWhenMissingOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo forget = new SpellEngine.SpellInfo(
                41299, SpellEngine.EFFECT_UNLEARN_SPECIALIZATION, 0, 0, 0, 0, 0, 0f, 26797);
        eng.apply(new Player(), p, forget);
        assertEquals(0, p.spells.size());
        Creature npc = new Creature();
        eng.apply(new Player(), npc, forget);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                41299, SpellEngine.EFFECT_UNLEARN_SPECIALIZATION, 0, 0, 0, 0, 0, 0f, 0);
        p.spells.add(26797);
        eng.apply(new Player(), p, zero);
        assertTrue(p.spells.contains(26797));
    }
}
