package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-094 — SPELL_EFFECT_SEND_EVENT (61). Summon Myzrael 4141 misc 420.
 * CMaNGOS StartEvents_Event(misc) from the caster.
 */
class SpellEngineSendEventTest {
    @Test
    void applySendEventWhenCasterShouldRecordEventId() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SEND_EVENT));
        Player p = new Player();
        SpellEngine.SpellInfo myzrael = new SpellEngine.SpellInfo(
                4141, SpellEngine.EFFECT_SEND_EVENT, 0, 0, 0, 0, 0, 0f, 420);
        eng.apply(p, p, myzrael);
        assertEquals(420, p.lastSendEvent());
    }

    @Test
    void applySendEventWhenMissingIdShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                4141, SpellEngine.EFFECT_SEND_EVENT, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(p, p, none);
        assertEquals(0, p.lastSendEvent());
        eng.sendEvent(null, 420);
        eng.sendEvent(p, 0);
        assertEquals(0, p.lastSendEvent());
    }
}
