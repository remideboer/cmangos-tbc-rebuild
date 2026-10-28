package org.tbc.world.spell;

import org.tbc.world.combat.Combat;
import org.tbc.world.combat.MeleeTable;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-237 — SPELL_AURA_MOD_THREAT (10).
 * Percent modifier per school bit. White melee threat uses the normal-school multiplier.
 */
class AuraEngineModThreatTest {
    private static SpellEngine.SpellInfo threat(int id, int amount, int mask) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_THREAT, 0, 0, amount, amount, 0f, mask);
    }

    @Test
    void applyAuraWhenModThreatShouldScaleWhiteMeleeThreat() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_THREAT));
        Player player = new Player();
        player.guid = 7;
        player.setHealth(10);
        Creature mob = new Creature();
        mob.setHealth(50);
        SpellEngine.SpellInfo aura = threat(999010, 100, 1);
        eng.apply(player, player, aura);
        assertEquals(2f, player.threatModifier(0), 0.001f);
        Combat combat = new Combat(MeleeTable.alwaysHit(), () -> 0d);
        combat.swing(player, mob, 0L);
        assertEquals(2f, mob.threatManager.threatOf(player), 0.001f);
        assertEquals(2, mob.threat);
        eng.auras().unapply(player, aura);
        assertEquals(1f, player.threatModifier(0), 0.001f);
    }

    @Test
    void applyWhenTwoAurasOrAPartialMaskShouldStackPerSchool() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setHealth(10);
        SpellEngine.SpellInfo first = threat(999010, 100, 1);
        SpellEngine.SpellInfo second = threat(999011, 100, 1);
        eng.apply(player, player, first);
        eng.apply(player, player, second);
        assertEquals(4f, player.threatModifier(0), 0.001f);
        assertEquals(40f, player.applyTotalThreatModifier(10f, 1), 0.001f);
        eng.auras().unapply(player, second);
        assertEquals(2f, player.threatModifier(0), 0.001f);

        Player holy = new Player();
        holy.setHealth(10);
        eng.apply(holy, holy, threat(999012, 100, 2));
        assertEquals(1f, holy.threatModifier(0), 0.001f);
        assertEquals(2f, holy.threatModifier(1), 0.001f);
        assertEquals(20f, holy.applyTotalThreatModifier(10f, 2), 0.001f);
        assertEquals(10f, holy.applyTotalThreatModifier(10f, 3), 0.001f);
        assertEquals(10f, holy.applyTotalThreatModifier(10f, 0), 0.001f);

        Player slowed = new Player();
        slowed.setHealth(10);
        eng.apply(slowed, slowed, threat(999013, -50, 1));
        assertEquals(0.5f, slowed.threatModifier(0), 0.001f);
    }

    @Test
    void applyWhenDeadOrTargetMissingShouldLeaveTheModifier() {
        Player dead = new Player();
        new SpellEngine().apply(dead, dead, threat(999010, 100, 1));
        assertEquals(1f, dead.threatModifier(0), 0.001f);
        new AuraEngine().apply(null, threat(999010, 100, 1));
    }
}
