package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-086 — SPELL_EFFECT_TRIGGER_MISSILE (32). Arcane Orb 34172 trigger 34190.
 * CMaNGOS caster CastSpell at dest/unit. SQL 34190 later; Fireball 133 is the nested vehicle.
 */
class SpellEngineTriggerMissileTest {
    @Test
    void applyTriggerMissileWhenNestedCatalogedShouldApplyAsCaster() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TRIGGER_MISSILE));
        Player caster = new Player();
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        target.setHealth(50);
        SpellEngine.SpellInfo orb = new SpellEngine.SpellInfo(
                34172, SpellEngine.EFFECT_TRIGGER_MISSILE, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        int dmg = eng.apply(caster, target, orb);
        assertEquals(10, dmg);
        assertEquals(40, target.health());
    }

    @Test
    void applyTriggerMissileWhenUnknownTriggerShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player caster = new Player();
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        target.setHealth(50);
        assertEquals(0, eng.apply(caster, target, new SpellEngine.SpellInfo(
                34172, SpellEngine.EFFECT_TRIGGER_MISSILE, 0, 0, 0, 0, 0, 0f, 34190)));
        assertEquals(50, target.health());
        assertEquals(0, eng.triggerMissile(null, target, SpellEngine.FIREBALL));
        assertEquals(0, eng.triggerMissile(caster, target, 0));
    }
}
