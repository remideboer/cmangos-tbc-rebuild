package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-118 — SPELL_EFFECT_DETECT (49). Detect 3050 — one spell.
 * CMaNGOS EffectUnused: detection is the MOD_STEALTH_DETECT aura. Cataloged, tracking / auras unchanged.
 */
class SpellEngineDetectTest {
    @Test
    void applyDetectWhenPlayerShouldBeCatalogedAndNotTrack() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DETECT));
        Player p = new Player();
        SpellEngine.SpellInfo detect = new SpellEngine.SpellInfo(
                3050, SpellEngine.EFFECT_DETECT, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, p, detect));
        assertEquals(0, p.getInt(UpdateFields.PLAYER_TRACK_CREATURES));
        assertEquals(0, p.auras.size());
    }
}
