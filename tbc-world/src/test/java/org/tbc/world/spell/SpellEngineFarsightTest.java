package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-008 — SPELL_EFFECT_ADD_FARSIGHT (72). */
class SpellEngineFarsightTest {
    @Test
    void applyAddFarsightWhenPlayerShouldSetFarsightAndCamera() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ADD_FARSIGHT));
        Player p = new Player();
        p.guid = 10L;
        Creature focus = new Creature();
        focus.guid = 888L;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(6197, SpellEngine.EFFECT_ADD_FARSIGHT, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, focus, sp);
        assertEquals(888L, p.farSightGuid());
        assertEquals(888L, p.cameraViewGuid());
        assertEquals(888L, p.getGuid(UpdateFields.PLAYER_FARSIGHT));
    }

    @Test
    void applyAddFarsightWhenNonPlayerCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature caster = new Creature();
        Creature focus = new Creature();
        focus.guid = 1L;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(6197, SpellEngine.EFFECT_ADD_FARSIGHT, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, focus, sp);
        Player p = new Player();
        eng.apply(p, null, sp);
        assertEquals(0L, p.farSightGuid());
    }
}
