package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineHealMaxHealthTest {
    @Test
    void applyWhenHealMaxHealthShouldSetHealthToMax() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(10);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(9997, SpellEngine.EFFECT_HEAL_MAX_HEALTH, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, sp);
        assertEquals(100, p.health());
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_HEAL_MAX_HEALTH));
    }
}
