package org.tbc.world.spell;

import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-090 — SPELL_EFFECT_TRANS_DOOR (50). Lightwell 724 misc GO 181102.
 * CMaNGOS EffectTransmitted: GO at caster, no object slot.
 */
class SpellEngineTransDoorTest {
    @Test
    void applyTransDoorWhenPlayerShouldPlaceLightwellWithoutSlot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TRANS_DOOR));
        Player p = new Player();
        p.guid = 9;
        p.relocate(1f, 2f, 3f, 0.5f);
        SpellEngine.SpellInfo well = new SpellEngine.SpellInfo(
                724, SpellEngine.EFFECT_TRANS_DOOR, 0, 0, 0, 0, 0, 0f, 181102);
        eng.apply(p, p, well);
        GameObject go = p.lastTransmittedObject();
        assertNotNull(go);
        assertEquals(181102, go.entry);
        assertEquals(1f, go.x, 0.01f);
        assertEquals(2f, go.y, 0.01f);
        assertEquals(3f, go.z, 0.01f);
        assertNull(p.objectSlot(0));
        assertNull(p.lastWildObject());
    }

    @Test
    void applyTransDoorWhenMissingEntryShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                724, SpellEngine.EFFECT_TRANS_DOOR, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(p, p, none);
        assertNull(p.lastTransmittedObject());
        eng.transmitted(null, 181102);
        eng.transmitted(p, 0);
        assertNull(p.lastTransmittedObject());
    }
}
