package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-085 — SPELL_EFFECT_TRIGGER_SPELL_WITH_VALUE (142). Prayer of Mending 33076 trigger 41635.
 * CMaNGOS caster CastCustomSpell with bp = damage. SQL 41635 later; Fireball 133 is the nested vehicle.
 */
class SpellEngineTriggerSpellWithValueTest {
    @Test
    void applyTriggerSpellWithValueWhenNestedDamageShouldUseParentBasePoints() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TRIGGER_SPELL_WITH_VALUE));
        Player caster = new Player();
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        target.setHealth(50);
        SpellEngine.SpellInfo pom = new SpellEngine.SpellInfo(
                33076, SpellEngine.EFFECT_TRIGGER_SPELL_WITH_VALUE, 0, 0, 0, 1, 1, 0f, SpellEngine.FIREBALL);
        int dmg = eng.apply(caster, target, pom);
        assertEquals(1, dmg);
        assertEquals(49, target.health());
    }

    @Test
    void applyTriggerSpellWithValueWhenUnknownOrZeroValueShouldNoOpOrUseCatalog() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player caster = new Player();
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        target.setHealth(50);
        assertEquals(0, eng.apply(caster, target, new SpellEngine.SpellInfo(
                33076, SpellEngine.EFFECT_TRIGGER_SPELL_WITH_VALUE, 0, 0, 0, 1, 1, 0f, 41635)));
        assertEquals(50, target.health());
        int catalog = eng.apply(caster, target, new SpellEngine.SpellInfo(
                33076, SpellEngine.EFFECT_TRIGGER_SPELL_WITH_VALUE, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL));
        assertEquals(10, catalog);
        assertEquals(40, target.health());
        assertEquals(0, eng.triggerSpellWithValue(null, target, SpellEngine.FIREBALL, 1));
        assertEquals(0, eng.triggerSpellWithValue(caster, target, 0, 1));
    }
}
