package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-010 — SPELL_EFFECT_HEALTH_LEECH (9). */
class SpellEngineHealthLeechTest {
    @Test
    void applyHealthLeechWhenLivingShouldDamageTargetAndHealCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_HEALTH_LEECH));
        Player caster = new Player();
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        caster.setHealth(40);
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(80);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(689, SpellEngine.EFFECT_HEALTH_LEECH, 0, 5, 0, 20, 20, 30f);
        int dmg = eng.apply(caster, target, sp);
        assertEquals(20, dmg);
        assertEquals(60, target.health());
        assertEquals(60, caster.health());
    }

    @Test
    void applyHealthLeechWhenDeadTargetOrNonPositiveShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        caster.setHealth(40);
        Creature dead = new Creature();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(689, SpellEngine.EFFECT_HEALTH_LEECH, 0, 5, 0, 20, 20, 30f);
        assertEquals(0, eng.apply(caster, dead, sp));
        assertEquals(40, caster.health());
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(689, SpellEngine.EFFECT_HEALTH_LEECH, 0, 5, 0, 0, 0, 30f);
        Creature live = new Creature();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(50);
        assertEquals(0, eng.apply(caster, live, zero));
        assertEquals(50, live.health());
        assertEquals(40, caster.health());
    }
}
