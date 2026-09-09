package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-105 — SPELL_EFFECT_REDIRECT_THREAT (130). Misdirection 34477 effect 3.
 * CMaNGOS EffectRedirectThreat: caster HostileRefManager SetThreatRedirection(unitTarget).
 */
class SpellEngineRedirectThreatTest {
    @Test
    void applyRedirectThreatWhenTargetShouldPointCasterThreatAtTarget() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_REDIRECT_THREAT));
        Player hunter = new Player();
        hunter.guid = 4;
        Player tank = new Player();
        tank.guid = 7;
        SpellEngine.SpellInfo misdirection = new SpellEngine.SpellInfo(
                34477, SpellEngine.EFFECT_REDIRECT_THREAT, 0, 0, 0, 0, 0, 0f);
        eng.apply(hunter, tank, misdirection);
        assertEquals(7, hunter.threatRedirectionGuid());
        assertEquals(0, tank.threatRedirectionGuid());
    }

    @Test
    void redirectThreatWhenMissingCasterOrTargetShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player hunter = new Player();
        hunter.guid = 4;
        eng.redirectThreat(hunter, null);
        assertEquals(0, hunter.threatRedirectionGuid());
        eng.redirectThreat(null, hunter);
        assertEquals(0, hunter.threatRedirectionGuid());
    }
}
