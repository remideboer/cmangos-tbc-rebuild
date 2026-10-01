package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-152 — SPELL_AURA_FEIGN_DEATH (66). Spell.dbc 5384 Feign Death.
 * CMaNGOS SetFeignDeath → UNIT_FLAG2_FEIGN_DEATH + UNIT_DYNFLAG_DEAD.
 * TP-SL26-169 — PLAYER_CONTROLLED success path CombatStop (inCombat/victim cleared).
 */
class AuraEngineFeignDeathTest {
    private static final SpellEngine.SpellInfo FEIGN_DEATH = new SpellEngine.SpellInfo(
            SpellEngine.FEIGN_DEATH, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_FEIGN_DEATH,
            0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenFeignDeathOnPlayerShouldSetFlags2AndDynDead() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_FEIGN_DEATH));
        Player player = new Player();
        eng.apply(player, player, FEIGN_DEATH);
        assertTrue(player.hasAura(SpellEngine.FEIGN_DEATH));
        assertTrue(player.isFeigningDeath());
        assertEquals(Unit.UNIT_FLAG2_FEIGN_DEATH,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2) & Unit.UNIT_FLAG2_FEIGN_DEATH);
        assertEquals(Unit.UNIT_DYNFLAG_DEAD,
                player.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_DEAD);
    }

    @Test
    void unapplyWhenFeignDeathOnPlayerShouldClearFlags() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, FEIGN_DEATH);
        eng.unapplyAura(player, SpellEngine.FEIGN_DEATH);
        assertFalse(player.isFeigningDeath());
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2) & Unit.UNIT_FLAG2_FEIGN_DEATH);
        assertEquals(0, player.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_DEAD);
    }

    /** TP-SL26-169 — SetFeignDeath success + PLAYER_CONTROLLED → CombatStop. */
    @Test
    void applyAuraWhenFeignDeathOnPlayerInCombatShouldCombatStop() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.inCombat = true;
        player.victim = 0x100000000000006L;
        player.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IN_COMBAT);

        eng.apply(player, player, FEIGN_DEATH);

        assertTrue(player.isFeigningDeath());
        assertFalse(player.inCombat);
        assertEquals(0L, player.victim);
    }

    @Test
    void applyWhenFeignDeathOnCreatureShouldNotCombatStop() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        mob.inCombat = true;
        mob.victim = 0x100000000000001L;

        eng.apply(new Player(), mob, FEIGN_DEATH);

        assertTrue(mob.isFeigningDeath());
        assertTrue(mob.inCombat);
        assertEquals(0x100000000000001L, mob.victim);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FEIGN_DEATH);
        new AuraEngine().unapply(null, FEIGN_DEATH);
    }
}
