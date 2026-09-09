package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-115 — SPELL_EFFECT_SPELL_DEFENSE (37). SPELLDEFENSE (DND) 522 — one spell.
 * CMaNGOS EffectEmpty: marker only. Cataloged, resistances / auras unchanged.
 */
class SpellEngineSpellDefenseTest {
    @Test
    void applySpellDefenseWhenPlayerShouldBeCatalogedAndMutateNothing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SPELL_DEFENSE));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 25);
        SpellEngine.SpellInfo spellDefense = new SpellEngine.SpellInfo(
                522, SpellEngine.EFFECT_SPELL_DEFENSE, 0, 0, 0, 0, 0, 0f);
        assertEquals(0, eng.apply(p, p, spellDefense));
        assertEquals(25, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, p.auras.size());
    }
}
