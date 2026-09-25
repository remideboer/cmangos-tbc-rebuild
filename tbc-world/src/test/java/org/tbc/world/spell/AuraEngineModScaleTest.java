package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-137 — SPELL_AURA_MOD_SCALE (61). Spell.dbc 22735 Spirit of Runn Tum (+300%).
 * CMaNGOS HandleAuraModScale → OBJECT_FIELD_SCALE_X = max(0.1, (100+amount)/100).
 */
class AuraEngineModScaleTest {
    private static final SpellEngine.SpellInfo RUNN_TUM = new SpellEngine.SpellInfo(
            SpellEngine.SPIRIT_OF_RUNN_TUM, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SCALE,
            0, 0, 300, 300, 0f);

    @Test
    void applyAuraWhenModScaleShouldRaiseObjectScale() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SCALE));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, RUNN_TUM);
        assertTrue(mob.hasAura(SpellEngine.SPIRIT_OF_RUNN_TUM));
        assertEquals(4.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
    }

    @Test
    void unapplyWhenModScaleShouldRestoreDefaultScale() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, RUNN_TUM);
        eng.unapplyAura(mob, SpellEngine.SPIRIT_OF_RUNN_TUM);
        assertEquals(1.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveScaleUnchanged() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SCALE, 0, 0, 0, 0, 0f);
        eng.apply(new Player(), mob, zero);
        assertEquals(1.0f, mob.getFloat(UpdateFields.OBJECT_FIELD_SCALE_X), 0.001f);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, RUNN_TUM);
        new AuraEngine().unapply(null, RUNN_TUM);
    }
}
