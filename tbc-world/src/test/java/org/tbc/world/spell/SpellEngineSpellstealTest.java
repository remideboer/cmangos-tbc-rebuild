package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-053 — SPELL_EFFECT_STEAL_BENEFICIAL_BUFF (126). Spellsteal 30449. */
class SpellEngineSpellstealTest {
    @Test
    void applySpellstealWhenTargetHasAuraShouldMoveAuraToCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF));
        Player caster = new Player();
        caster.guid = 2;
        Player target = new Player();
        target.guid = 5;
        target.auras.add(new Unit.Aura(1459, 30_000, 1));
        SpellEngine.SpellInfo steal = new SpellEngine.SpellInfo(
                30449, SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF, 0, 0, 0, 1, 1, 30f);
        eng.apply(caster, target, steal);
        assertEquals(0, target.auras.size());
        assertEquals(1, caster.auras.size());
        assertEquals(1459, caster.auras.get(0).spellId());
        WowBuffer b = new WowBuffer(SpellEngine.encodeSpellStealLog(target, caster, 30449, 1459));
        assertEquals(5, b.getPackedGuid());
        assertEquals(2, b.getPackedGuid());
        assertEquals(30449, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(1, b.getU32());
        assertEquals(1459, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_SPELLSTEALLOG, 0x333);
    }

    @Test
    void applySpellstealWhenSelfShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.auras.add(new Unit.Aura(1459, 30_000, 1));
        SpellEngine.SpellInfo steal = new SpellEngine.SpellInfo(
                30449, SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF, 0, 0, 0, 1, 1, 30f);
        eng.apply(p, p, steal);
        assertEquals(1, p.auras.size());
        assertEquals(1459, p.auras.get(0).spellId());
    }
}
