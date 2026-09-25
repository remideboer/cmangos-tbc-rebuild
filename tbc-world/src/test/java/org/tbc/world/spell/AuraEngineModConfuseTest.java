package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-133 — SPELL_AURA_MOD_CONFUSE (5). Spell.dbc 118 Polymorph.
 * CMaNGOS HandleModConfuse → SetConfused → UNIT_FLAG_CONFUSED on UNIT_FIELD_FLAGS.
 */
class AuraEngineModConfuseTest {
    private static final SpellEngine.SpellInfo POLYMORPH = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_POLYMORPH, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_CONFUSE,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModConfuseShouldSetConfusedFlagAndRecordAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_CONFUSE));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, POLYMORPH);
        assertTrue(mob.hasAura(SpellEngine.SPELL_POLYMORPH));
        assertEquals(Unit.UNIT_FLAG_CONFUSED,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_CONFUSED);
    }

    @Test
    void unapplyWhenModConfuseShouldClearConfusedFlag() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, POLYMORPH);
        eng.unapplyAura(mob, SpellEngine.SPELL_POLYMORPH);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_CONFUSED);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, POLYMORPH);
        new AuraEngine().unapply(null, POLYMORPH);
    }
}
