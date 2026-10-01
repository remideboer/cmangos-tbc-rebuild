package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-167 — SPELL_AURA_MOD_SHAPESHIFT (36). Battle Stance 2457 misc FORM_BATTLESTANCE (17):
 * UNIT_FIELD_BYTES_2 byte 3. CMaNGOS HandleAuraModShapeshift → SetShapeshiftForm / FORM_NONE.
 */
class AuraEngineModShapeshiftTest {
    private static final SpellEngine.SpellInfo BATTLE_STANCE = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_BATTLE_STANCE, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SHAPESHIFT, 0, 0, 0, 0, 0f, Unit.FORM_BATTLESTANCE);

    @Test
    void applyAuraWhenModShapeshiftBattleStanceShouldSetUnitFieldBytes2Form() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SHAPESHIFT));
        Player player = new Player();

        eng.apply(player, player, BATTLE_STANCE);

        assertTrue(player.hasAura(SpellEngine.SPELL_BATTLE_STANCE));
        assertEquals(Unit.FORM_BATTLESTANCE, player.shapeshiftForm());
        int bytes2 = player.getInt(UpdateFields.UNIT_FIELD_BYTES_2);
        assertEquals(Unit.FORM_BATTLESTANCE, (bytes2 >>> 24) & 0xFF);
    }

    @Test
    void unapplyWhenModShapeshiftShouldClearFormToNone() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, BATTLE_STANCE);
        assertEquals(Unit.FORM_BATTLESTANCE, player.shapeshiftForm());

        eng.unapplyAura(player, SpellEngine.SPELL_BATTLE_STANCE);

        assertEquals(Unit.FORM_NONE, player.shapeshiftForm());
        assertEquals(0, (player.getInt(UpdateFields.UNIT_FIELD_BYTES_2) >>> 24) & 0xFF);
    }

    @Test
    void applyWhenCreatureShouldSetShapeshiftForm() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, BATTLE_STANCE);
        assertEquals(Unit.FORM_BATTLESTANCE, mob.shapeshiftForm());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BATTLE_STANCE);
        new AuraEngine().unapply(null, BATTLE_STANCE);
    }
}
