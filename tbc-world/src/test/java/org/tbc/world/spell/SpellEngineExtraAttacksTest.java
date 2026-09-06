package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-015 — SPELL_EFFECT_ADD_EXTRA_ATTACKS (19). */
class SpellEngineExtraAttacksTest {
    @Test
    void applyAddExtraAttacksWhenLivingShouldQueueAndCapAtFive() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ADD_EXTRA_ATTACKS));
        Player p = new Player();
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        SpellEngine.SpellInfo wf = new SpellEngine.SpellInfo(33750, SpellEngine.EFFECT_ADD_EXTRA_ATTACKS, 0, 0, 0, 2, 2, 0f);
        eng.apply(p, p, wf);
        assertEquals(2, p.extraAttacks());
        eng.apply(p, p, wf);
        assertEquals(4, p.extraAttacks());
        eng.apply(p, p, wf);
        assertEquals(5, p.extraAttacks());
    }

    @Test
    void applyAddExtraAttacksWhenDeadOrNonPositiveShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo wf = new SpellEngine.SpellInfo(33750, SpellEngine.EFFECT_ADD_EXTRA_ATTACKS, 0, 0, 0, 2, 2, 0f);
        eng.apply(new Player(), dead, wf);
        assertEquals(0, dead.extraAttacks());
        Player live = new Player();
        live.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(50);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(33750, SpellEngine.EFFECT_ADD_EXTRA_ATTACKS, 0, 0, 0, 0, 0, 0f);
        eng.apply(live, live, zero);
        assertEquals(0, live.extraAttacks());
        eng.apply(live, null, wf);
        assertEquals(0, live.extraAttacks());
    }
}
