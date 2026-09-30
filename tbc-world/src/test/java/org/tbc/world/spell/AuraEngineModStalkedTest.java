package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-161 — SPELL_AURA_MOD_STALKED (68). Hunter's Mark 1130:
 * CMaNGOS HandleAuraModStalked → UNIT_DYNFLAG_TRACK_UNIT on UNIT_DYNAMIC_FLAGS.
 */
class AuraEngineModStalkedTest {
    private static final SpellEngine.SpellInfo HUNTERS_MARK = new SpellEngine.SpellInfo(
            SpellEngine.HUNTERS_MARK, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_STALKED, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenModStalkedShouldSetTrackUnitDynamicFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_STALKED));
        Creature mob = new Creature();

        eng.apply(new Player(), mob, HUNTERS_MARK);

        assertTrue(mob.hasAura(SpellEngine.HUNTERS_MARK));
        assertEquals(Unit.UNIT_DYNFLAG_TRACK_UNIT,
                mob.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_TRACK_UNIT);
    }

    @Test
    void unapplyWhenModStalkedShouldClearTrackUnitDynamicFlag() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, HUNTERS_MARK);
        eng.unapplyAura(mob, SpellEngine.HUNTERS_MARK);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_TRACK_UNIT);
    }

    @Test
    void applyWhenPlayerShouldSetTrackUnitDynamicFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(new Player(), player, HUNTERS_MARK);
        assertEquals(Unit.UNIT_DYNFLAG_TRACK_UNIT,
                player.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_TRACK_UNIT);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HUNTERS_MARK);
        new AuraEngine().unapply(null, HUNTERS_MARK);
    }
}
