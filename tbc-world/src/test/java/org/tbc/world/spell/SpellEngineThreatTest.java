package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-024 — SPELL_EFFECT_THREAT (63). */
class SpellEngineThreatTest {
    @Test
    void applyThreatWhenLivingCreatureShouldAddThreatFromCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_THREAT));
        Player caster = new Player();
        caster.guid = 2;
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        caster.setHealth(100);
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        mob.setHealth(80);
        SpellEngine.SpellInfo threat = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_THREAT, 0, 0, 0, 50, 50, 0f);
        eng.apply(caster, mob, threat);
        assertEquals(50f, mob.threatManager.threatOf(caster), 0.01f);
        eng.apply(caster, mob, threat);
        assertEquals(100f, mob.threatManager.threatOf(caster), 0.01f);
    }

    @Test
    void applyThreatWhenDeadCasterTargetOrPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.guid = 2;
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        caster.setHealth(100);
        Creature dead = new Creature();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo threat = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_THREAT, 0, 0, 0, 50, 50, 0f);
        eng.apply(caster, dead, threat);
        assertEquals(0f, dead.threatManager.threatOf(caster), 0.01f);
        Player other = new Player();
        other.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        other.setHealth(50);
        eng.apply(caster, other, threat);
        Creature live = new Creature();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(80);
        Player deadCaster = new Player();
        deadCaster.guid = 3;
        deadCaster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        deadCaster.setHealth(0);
        eng.apply(deadCaster, live, threat);
        assertEquals(0f, live.threatManager.threatOf(deadCaster), 0.01f);
    }
}
