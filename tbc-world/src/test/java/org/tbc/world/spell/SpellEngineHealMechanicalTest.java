package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-030 — SPELL_EFFECT_HEAL_MECHANICAL (75). Mechanical Patch Kit 15057. */
class SpellEngineHealMechanicalTest {
    @Test
    void applyHealMechanicalWhenLivingShouldHealByDamage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_HEAL_MECHANICAL));
        Creature mech = new Creature();
        mech.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 1000);
        mech.setHealth(100);
        SpellEngine.SpellInfo kit = new SpellEngine.SpellInfo(
                15057, SpellEngine.EFFECT_HEAL_MECHANICAL, 0, 0, 0, 700, 700, 0f);
        eng.apply(new Player(), mech, kit);
        assertEquals(800, mech.health());
    }

    @Test
    void applyHealMechanicalWhenDeadOrZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature dead = new Creature();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 1000);
        dead.setHealth(0);
        SpellEngine.SpellInfo kit = new SpellEngine.SpellInfo(
                15057, SpellEngine.EFFECT_HEAL_MECHANICAL, 0, 0, 0, 700, 700, 0f);
        eng.apply(new Player(), dead, kit);
        assertEquals(0, dead.health());
        Creature live = new Creature();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 1000);
        live.setHealth(100);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                15057, SpellEngine.EFFECT_HEAL_MECHANICAL, 0, 0, 0, 0, 0, 0f);
        eng.apply(new Player(), live, zero);
        assertEquals(100, live.health());
    }
}
