package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-222 — SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED (207).
 * CMaNGOS uses the highest positive amount while mounted, and SetCanFly.
 */
class AuraEngineModFlightSpeedMountedTest {
    private static final float EPS = 1e-4f;

    private static SpellEngine.SpellInfo speed(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED, 0, 0, amount, amount, 0f);
    }

    @Test
    void applyAuraWhenMountedShouldUseTheHighestAmount() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED));
        Player player = new Player();
        player.mounted = true;
        SpellEngine.SpellInfo high = speed(999207, 100);
        eng.apply(player, player, high);
        eng.apply(player, player, speed(999307, 40));
        assertEquals(14f, player.flightSpeed(), EPS);
        eng.auras().unapply(player, high);
        assertEquals(9.8f, player.flightSpeed(), EPS);
    }

    @Test
    void applyWhenNotMountedShouldSetCanFlyWithoutChangingSpeed() {
        Creature mob = new Creature();
        mob.guid = 0xF13000000000000AL;
        Map<Integer, byte[]> last = new HashMap<>();
        mob.messageToSet = (opcode, payload) -> last.put(opcode, payload);
        SpellEngine eng = new SpellEngine();
        eng.apply(new Player(), mob, speed(999207, 100));
        assertEquals(Unit.BASE_FLIGHT_SPEED, mob.flightSpeed(), EPS);
        WowBuffer pkt = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_SET_FLYING));
        assertEquals(mob.guid, pkt.getPackedGuid());
        last.clear();
        eng.auras().unapply(mob, speed(999207, 100));
        WowBuffer off = new WowBuffer(last.get(Opcodes.SMSG_SPLINE_MOVE_UNSET_FLYING));
        assertEquals(mob.guid, off.getPackedGuid());
    }

    @Test
    void applyWhenOnFootShouldLeaveFlightSpeedAtBase() {
        Player player = new Player();
        new SpellEngine().apply(player, player, speed(999207, 100));
        assertEquals(Unit.BASE_FLIGHT_SPEED, player.flightSpeed(), EPS);
        new AuraEngine().apply(null, speed(999207, 100));
    }
}
