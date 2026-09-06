package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-027 — SPELL_EFFECT_INEBRIATE (100). Gordok Green Grog 22789. */
class SpellEngineInebriateTest {
    @Test
    void applyInebriateWhenPlayerShouldAddDrunkAndCap() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_INEBRIATE));
        Player p = new Player();
        SpellEngine.SpellInfo grog = new SpellEngine.SpellInfo(
                22789, SpellEngine.EFFECT_INEBRIATE, 0, 0, 0, 1, 1, 0f);
        eng.apply(p, p, grog);
        assertEquals(256, p.drunkValue());
        assertEquals(256, p.getInt(UpdateFields.PLAYER_BYTES_3) & 0xFFFE);
        eng.apply(p, p, grog);
        assertEquals(512, p.drunkValue());
        p.setDrunkValue(0xFF00);
        eng.apply(p, p, grog);
        assertEquals(0xFFFF, p.drunkValue());
    }

    @Test
    void applyInebriateWhenNonPlayerOrZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature c = new Creature();
        SpellEngine.SpellInfo grog = new SpellEngine.SpellInfo(
                22789, SpellEngine.EFFECT_INEBRIATE, 0, 0, 0, 1, 1, 0f);
        eng.apply(new Player(), c, grog);
        Player p = new Player();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                22789, SpellEngine.EFFECT_INEBRIATE, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, zero);
        assertEquals(0, p.drunkValue());
    }
}
