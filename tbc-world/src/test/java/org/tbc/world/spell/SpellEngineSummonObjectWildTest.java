package org.tbc.world.spell;

import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-081 — SPELL_EFFECT_SUMMON_OBJECT_WILD (76). Summon Rusty Chest 6464 misc 19021.
 * CMaNGOS EffectSummonObjectWild: GO at caster, no object slot, no SMSG_TOTEM_CREATED.
 */
class SpellEngineSummonObjectWildTest {
    @Test
    void applySummonObjectWildWhenPlayerShouldPlaceChestWithoutSlot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_OBJECT_WILD));
        Player p = new Player();
        p.guid = 9;
        p.relocate(1f, 2f, 3f, 0.5f);
        SpellEngine.SpellInfo chest = new SpellEngine.SpellInfo(
                6464, SpellEngine.EFFECT_SUMMON_OBJECT_WILD, 0, 0, 0, 0, 0, 0f, 19021);
        eng.apply(p, p, chest);
        GameObject go = p.lastWildObject();
        assertNotNull(go);
        assertEquals(19021, go.entry);
        assertEquals(1f, go.x, 0.01f);
        assertEquals(2f, go.y, 0.01f);
        assertEquals(3f, go.z, 0.01f);
        assertNull(p.objectSlot(0));
        assertNull(p.objectSlot(1));
        assertEquals(0, p.lastTotemCreatedSlot());
        assertEquals(0L, p.lastTotemCreatedGuid());
    }

    @Test
    void applySummonObjectWildWhenMissingEntryShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                6464, SpellEngine.EFFECT_SUMMON_OBJECT_WILD, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(p, p, none);
        assertNull(p.lastWildObject());
        eng.summonObjectWild(null, 19021);
        eng.summonObjectWild(p, 0);
        assertNull(p.lastWildObject());
    }
}
