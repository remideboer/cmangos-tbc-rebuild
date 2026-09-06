package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-088 — SPELL_EFFECT_TELEPORT_UNITS_FACE_CASTER (43). Summon Player 20279.
 * CMaNGOS NearTeleportTo dest facing -caster orientation. Dest stand-in is caster xyz.
 */
class SpellEngineTeleportUnitsFaceCasterTest {
    @Test
    void applyTeleportUnitsFaceCasterWhenTargetShouldAppearAtCasterFacingOpposite() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TELEPORT_UNITS_FACE_CASTER));
        Player caster = new Player();
        caster.relocate(10f, 20f, 5f, 1.5f);
        Player target = new Player();
        target.relocate(0f, 0f, 0f, 0f);
        SpellEngine.SpellInfo summon = new SpellEngine.SpellInfo(
                20279, SpellEngine.EFFECT_TELEPORT_UNITS_FACE_CASTER, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, target, summon);
        assertEquals(10f, target.x, 0.01f);
        assertEquals(20f, target.y, 0.01f);
        assertEquals(5f, target.z, 0.01f);
        assertEquals(-1.5f, target.o, 0.01f);
    }

    @Test
    void applyTeleportUnitsFaceCasterWhenTaxiOrMissingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.relocate(10f, 20f, 5f, 1.5f);
        Player flying = new Player();
        flying.relocate(1f, 2f, 3f, 0f);
        flying.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_TAXI_FLIGHT);
        SpellEngine.SpellInfo summon = new SpellEngine.SpellInfo(
                20279, SpellEngine.EFFECT_TELEPORT_UNITS_FACE_CASTER, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, flying, summon);
        assertEquals(1f, flying.x, 0.01f);
        eng.apply(null, flying, summon);
        Creature npc = new Creature();
        npc.relocate(4f, 5f, 6f, 0.2f);
        eng.apply(caster, npc, summon);
        assertEquals(10f, npc.x, 0.01f);
        assertEquals(-1.5f, npc.o, 0.01f);
        eng.teleportUnitsFaceCaster(caster, null);
        eng.teleportUnitsFaceCaster(null, npc);
    }
}
