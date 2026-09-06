package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-064 — SPELL_EFFECT_SUMMON_PLAYER (85). Ritual of Summoning Effect 7720. */
class SpellEngineSummonPlayerTest {
    @Test
    void applySummonPlayerWhenPlayerTargetShouldOfferSummonRequest() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_PLAYER));
        Player caster = new Player();
        caster.guid = 10;
        caster.mapId = 0;
        caster.areaId = 12;
        caster.relocate(-9465f, 16f, 57f, 0f);
        Player target = new Player();
        SpellEngine.SpellInfo ritual = new SpellEngine.SpellInfo(7720, SpellEngine.EFFECT_SUMMON_PLAYER, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, target, ritual);
        assertEquals(10, target.summonerGuid);
        assertEquals(0, target.summonMapId);
        assertEquals(-9465f, target.summonX, 0.01f);
        assertEquals(16f, target.summonY, 0.01f);
        assertEquals(57f, target.summonZ, 0.01f);
        WowBuffer b = new WowBuffer(SpellEngine.encodeSummonRequest(caster, SpellEngine.MAX_PLAYER_SUMMON_DELAY_MS));
        assertEquals(10, b.getU64());
        assertEquals(12, b.getU32());
        assertEquals(120_000, b.getU32());
        assertEquals(Opcodes.SMSG_SUMMON_REQUEST, 0x2AB);
    }

    @Test
    void applySummonPlayerWhenEvilTwinOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.guid = 10;
        caster.relocate(1f, 2f, 3f, 0f);
        Player twin = new Player();
        twin.auras.add(new Unit.Aura(23445, 0, 1));
        SpellEngine.SpellInfo ritual = new SpellEngine.SpellInfo(7720, SpellEngine.EFFECT_SUMMON_PLAYER, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, twin, ritual);
        assertEquals(0, twin.summonerGuid);
        Creature npc = new Creature();
        eng.apply(caster, npc, ritual);
    }
}
