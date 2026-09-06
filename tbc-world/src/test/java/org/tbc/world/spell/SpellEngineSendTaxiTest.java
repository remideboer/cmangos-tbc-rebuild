package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-045 — SPELL_EFFECT_SEND_TAXI (123). Taxi Stair of Destiny to Honor Hold 34907 path 564. */
class SpellEngineSendTaxiTest {
    @Test
    void applySendTaxiWhenPlayerTargetShouldActivatePathFromMisc() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SEND_TAXI));
        Player target = new Player();
        SpellEngine.SpellInfo taxi = new SpellEngine.SpellInfo(
                34907, SpellEngine.EFFECT_SEND_TAXI, 0, 0, 0, 0, 0, 0f, 564);
        eng.apply(new Player(), target, taxi);
        assertEquals(564, target.taxiPath);
    }

    @Test
    void applySendTaxiWhenNonPlayerOrZeroPathShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature npc = new Creature();
        SpellEngine.SpellInfo taxi = new SpellEngine.SpellInfo(
                34907, SpellEngine.EFFECT_SEND_TAXI, 0, 0, 0, 0, 0, 0f, 564);
        eng.apply(new Player(), npc, taxi);
        Player p = new Player();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                34907, SpellEngine.EFFECT_SEND_TAXI, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(new Player(), p, zero);
        assertEquals(0, p.taxiPath);
    }
}
