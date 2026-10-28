package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-230 — SPELL_AURA_ARENA_PREPARATION (215).
 * CMaNGOS HandleArenaPreparation fills health, mana, and energy, and clears rage on remove.
 */
class AuraEngineArenaPreparationTest {
    private static final SpellEngine.SpellInfo PREP = new SpellEngine.SpellInfo(
            999215, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_ARENA_PREPARATION, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenArenaPreparationShouldFillResourcesAndSetTheFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_ARENA_PREPARATION));
        Player player = prepared(new Player());
        eng.apply(player, player, PREP);
        assertEquals(Unit.UNIT_FLAG_PLAYER_CONTROLLED | Unit.UNIT_FLAG_PREPARATION,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS));
        assertEquals(100, player.health());
        assertEquals(80, player.power());
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_POWER4));
        assertEquals(30, player.getInt(UpdateFields.UNIT_FIELD_POWER2));
    }

    @Test
    void unapplyWhenArenaPreparationShouldClearRageAndTheFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = prepared(new Player());
        eng.apply(player, player, PREP);
        eng.auras().unapply(player, PREP);
        assertEquals(Unit.UNIT_FLAG_PLAYER_CONTROLLED, player.getInt(UpdateFields.UNIT_FIELD_FLAGS));
        assertEquals(100, player.health());
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_POWER2));
    }

    @Test
    void applyWhenCreatureOrTargetMissingShouldStillMarkPreparation() {
        new AuraEngine().apply(null, PREP);
        Creature mob = prepared(new Creature());
        new SpellEngine().apply(new Player(), mob, PREP);
        assertEquals(Unit.UNIT_FLAG_PREPARATION, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS));
        assertEquals(100, mob.health());
        assertEquals(100, mob.getInt(UpdateFields.UNIT_FIELD_POWER4));
    }

    private static <T extends Unit> T prepared(T unit) {
        unit.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        unit.setHealth(40);
        unit.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 80);
        unit.setPower(10);
        unit.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        unit.setInt(UpdateFields.UNIT_FIELD_POWER4, 20);
        unit.setInt(UpdateFields.UNIT_FIELD_POWER2, 30);
        return unit;
    }
}
