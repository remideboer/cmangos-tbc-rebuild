package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-074 — SPELL_EFFECT_FORCE_CAST (140). Portal Effect: Ironforge 17607.
 * CMaNGOS unitTarget casts the trigger. Dest for 44089 is later; Fireball 133 is the nested vehicle.
 */
class SpellEngineForceCastTest {
    @Test
    void applyForceCastWhenNestedCatalogedShouldApplyAsTargetCaster() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_FORCE_CAST));
        Player target = new Player();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        target.setHealth(50);
        SpellEngine.SpellInfo portal = new SpellEngine.SpellInfo(
                17607, SpellEngine.EFFECT_FORCE_CAST, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        int dmg = eng.apply(new Creature(), target, portal);
        assertEquals(10, dmg);
        assertEquals(40, target.health());
    }

    @Test
    void applyForceCastWhenUnknownTriggerShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player target = new Player();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        target.setHealth(50);
        SpellEngine.SpellInfo portal = new SpellEngine.SpellInfo(
                17607, SpellEngine.EFFECT_FORCE_CAST, 0, 0, 0, 0, 0, 0f, 44089);
        assertEquals(0, eng.apply(new Creature(), target, portal));
        assertEquals(50, target.health());
        assertEquals(0, eng.apply(new Creature(), target, new SpellEngine.SpellInfo(
                17607, SpellEngine.EFFECT_FORCE_CAST, 0, 0, 0, 0, 0, 0f, 0)));
        assertEquals(0, eng.forceCast(null, SpellEngine.FIREBALL));
    }
}
