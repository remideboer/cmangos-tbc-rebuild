package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-089 — SPELL_EFFECT_DUEL (83). Duel 7266 misc GO 21680.
 * CMaNGOS arbiter GO at midpoint; SMSG_DUEL_REQUESTED raw flag + caster guid.
 */
class SpellEngineDuelTest {
    @Test
    void applyDuelWhenTwoPlayersShouldPlaceFlagAndEncodeRequest() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DUEL));
        Player caster = new Player();
        caster.guid = 11;
        caster.relocate(0f, 0f, 4f, 0f);
        Player target = new Player();
        target.guid = 22;
        target.relocate(10f, 0f, 4f, 0f);
        SpellEngine.SpellInfo duel = new SpellEngine.SpellInfo(
                7266, SpellEngine.EFFECT_DUEL, 0, 0, 0, 0, 0, 0f, 21680);
        eng.apply(caster, target, duel);
        GameObject flag = caster.duelFlag();
        assertNotNull(flag);
        assertEquals(21680, flag.entry);
        assertEquals(5f, flag.x, 0.01f);
        assertEquals(0f, flag.y, 0.01f);
        assertEquals(target, caster.duelOpponent);
        assertEquals(caster, target.duelOpponent);
        WowBuffer b = new WowBuffer(SpellEngine.encodeDuelRequested(flag.guid, caster.guid));
        assertEquals(flag.guid, b.getU64());
        assertEquals(caster.guid, b.getU64());
        assertEquals(Opcodes.SMSG_DUEL_REQUESTED, 0x167);
    }

    @Test
    void applyDuelWhenNonPlayerBusyOrMissingFlagShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo duel = new SpellEngine.SpellInfo(
                7266, SpellEngine.EFFECT_DUEL, 0, 0, 0, 0, 0, 0f, 21680);
        Player caster = new Player();
        caster.guid = 3;
        Player target = new Player();
        eng.apply(caster, new Creature(), duel);
        assertNull(caster.duelOpponent);
        eng.apply(new Creature(), target, duel);
        eng.apply(caster, caster, duel);
        assertNull(caster.duelOpponent);
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                7266, SpellEngine.EFFECT_DUEL, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(caster, target, none);
        assertNull(caster.duelOpponent);
        caster.duelOpponent = target;
        eng.apply(caster, target, duel);
        assertNull(caster.duelFlag());
        caster.duelOpponent = null;
        Player busy = new Player();
        busy.duelOpponent = new Player();
        eng.apply(caster, busy, duel);
        assertNull(caster.duelOpponent);
        eng.duel(null, target, 21680);
        eng.duel(caster, null, 21680);
    }
}
