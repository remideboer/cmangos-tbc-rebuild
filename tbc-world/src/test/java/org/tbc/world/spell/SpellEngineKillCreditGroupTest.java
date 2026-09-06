package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-046 — SPELL_EFFECT_KILL_CREDIT_GROUP (134). Kill Credit Greater Diemetradon 37907 entry 21924. */
class SpellEngineKillCreditGroupTest {
    @Test
    void applyKillCreditGroupWhenPlayerTargetShouldCreditMiscEntry() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_KILL_CREDIT_GROUP));
        Player target = new Player();
        SpellEngine.SpellInfo credit = new SpellEngine.SpellInfo(
                37907, SpellEngine.EFFECT_KILL_CREDIT_GROUP, 0, 0, 0, 0, 0, 0f, 21924);
        eng.apply(new Player(), target, credit);
        assertEquals(1, target.killCreditCount(21924));
    }

    @Test
    void applyKillCreditGroupWhenNonPlayerOrZeroEntryShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature npc = new Creature();
        SpellEngine.SpellInfo credit = new SpellEngine.SpellInfo(
                37907, SpellEngine.EFFECT_KILL_CREDIT_GROUP, 0, 0, 0, 0, 0, 0f, 21924);
        eng.apply(new Player(), npc, credit);
        Player p = new Player();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                37907, SpellEngine.EFFECT_KILL_CREDIT_GROUP, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(new Player(), p, zero);
        assertEquals(0, p.killCreditCount(21924));
    }
}
