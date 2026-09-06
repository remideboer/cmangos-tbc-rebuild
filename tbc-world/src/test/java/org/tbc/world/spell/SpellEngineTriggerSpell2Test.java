package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-077 — SPELL_EFFECT_TRIGGER_SPELL_2 (151). Ritual of Summoning 698.
 * CMaNGOS EffectTriggerRitualOfSummoning: caster CastSpell(target, trigger).
 * SQL trigger 46546 is SUMMON; nested catalog uses Fireball 133.
 */
class SpellEngineTriggerSpell2Test {
    @Test
    void applyTriggerSpell2WhenNestedCatalogedShouldApplyAsCasterOnTarget() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TRIGGER_SPELL_2));
        Player caster = new Player();
        Creature target = new Creature();
        target.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        int hp = target.health();
        SpellEngine.SpellInfo ritual = new SpellEngine.SpellInfo(
                698, SpellEngine.EFFECT_TRIGGER_SPELL_2, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        assertEquals(10, eng.apply(caster, target, ritual));
        assertEquals(hp - 10, target.health());
    }

    @Test
    void applyTriggerSpell2WhenUnknownTriggerShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player caster = new Player();
        Creature target = new Creature();
        target.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        int hp = target.health();
        assertEquals(0, eng.apply(caster, target, new SpellEngine.SpellInfo(
                698, SpellEngine.EFFECT_TRIGGER_SPELL_2, 0, 0, 0, 0, 0, 0f, 46546)));
        assertEquals(0, eng.apply(caster, target, new SpellEngine.SpellInfo(
                698, SpellEngine.EFFECT_TRIGGER_SPELL_2, 0, 0, 0, 0, 0, 0f, 0)));
        assertEquals(hp, target.health());
        assertEquals(0, eng.triggerRitualOfSummoning(null, target, SpellEngine.FIREBALL));
        assertEquals(0, eng.triggerRitualOfSummoning(caster, null, SpellEngine.FIREBALL));
    }
}
