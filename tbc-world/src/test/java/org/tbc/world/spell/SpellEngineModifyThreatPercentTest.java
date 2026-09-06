package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-034 — SPELL_EFFECT_MODIFY_THREAT_PERCENT (125). Soulshatter 32835. */
class SpellEngineModifyThreatPercentTest {
    @Test
    void applyModifyThreatPercentWhenOnThreatListShouldScaleThreat() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_MODIFY_THREAT_PERCENT));
        Player caster = new Player();
        caster.guid = 2;
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        caster.setHealth(100);
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        mob.setHealth(80);
        mob.threatManager.add(caster, 100f);
        SpellEngine.SpellInfo shatter = new SpellEngine.SpellInfo(
                32835, SpellEngine.EFFECT_MODIFY_THREAT_PERCENT, 0, 0, 0, -50, -50, 0f);
        eng.apply(caster, mob, shatter);
        assertEquals(50f, mob.threatManager.threatOf(caster), 0.01f);
    }

    @Test
    void applyModifyThreatPercentWhenMissingPlayerOrWipeShouldNoOpOrClear() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.guid = 2;
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        mob.setHealth(80);
        SpellEngine.SpellInfo shatter = new SpellEngine.SpellInfo(
                32835, SpellEngine.EFFECT_MODIFY_THREAT_PERCENT, 0, 0, 0, -50, -50, 0f);
        eng.apply(caster, mob, shatter);
        assertEquals(0f, mob.threatManager.threatOf(caster), 0.01f);
        Player other = new Player();
        other.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        other.setHealth(50);
        eng.apply(caster, other, shatter);
        mob.threatManager.add(caster, 80f);
        SpellEngine.SpellInfo wipe = new SpellEngine.SpellInfo(
                32835, SpellEngine.EFFECT_MODIFY_THREAT_PERCENT, 0, 0, 0, -101, -101, 0f);
        eng.apply(caster, mob, wipe);
        assertEquals(0f, mob.threatManager.threatOf(caster), 0.01f);
    }
}
