package org.tbc.world.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TP-NEG-010 — effect ids no 8606 spell uses (Spell.dbc 2.4.3) stay out of the catalog.
 * CMaNGOS maps them to EffectUnused / EffectNULL; without a real spell id there is no
 * public contract to assert, so they are not claimed as cataloged.
 */
class SpellEngineUnreachableEffectsTest {
    /** Verified against mangos-tbc/sql/base/dbc/original_data/Spell.sql: no Effect1..3 equals any of these. */
    private static final List<Integer> NO_8606_SPELL = List.of(
            0, 4, 12, 13, 14, 15, 41, 42, 51, 52, 65, 66, 73, 74, 78,
            87, 88, 89, 90, 91, 93, 97, 106, 107, 112, 122, 139, 146, 148, 150);

    @Test
    void knownEffectWhenIdHasNo8606SpellShouldBeFalse() {
        SpellEngine eng = new SpellEngine();
        for (int id : NO_8606_SPELL) {
            assertFalse(eng.knownEffect(id), "effect " + id + " has no 8606 spell; must not be cataloged");
        }
    }
}
