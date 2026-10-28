package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-256..257 — periodic leech (53) and health funnel (62). */
class AuraEnginePeriodicLeechTest {
    @Test
    void tickLeechShouldDamageTheTargetAndHealTheCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PERIODIC_LEECH));
        Player caster = alive(200, 50);
        Player target = alive(100, 80);
        assertTrue(eng.auras().tickCatalogPeriodic(caster, target, leech(30, 0), null));
        assertEquals(50, target.health());
        assertEquals(80, caster.health());
    }

    @Test
    void tickLeechShouldCapTheHealAtTheVictimsRemainingHealth() {
        SpellEngine eng = new SpellEngine();
        Player caster = alive(200, 50);
        Player target = alive(100, 80);
        eng.auras().tickCatalogPeriodic(caster, target, leech(100, 0), null);
        assertEquals(0, target.health());
        assertEquals(130, caster.health());
    }

    @Test
    void tickLeechShouldSkipWhenTheTargetIsImmuneOrTheCasterIsDead() {
        SpellEngine eng = new SpellEngine();
        Player caster = alive(200, 50);
        Player immune = alive(100, 80);
        immune.setDamageImmunity(1, 1, true);
        eng.auras().tickCatalogPeriodic(caster, immune, leech(30, 0), null);
        assertEquals(80, immune.health());
        assertEquals(50, caster.health());

        Player dead = alive(200, 0);
        Player target = alive(100, 80);
        eng.auras().tickCatalogPeriodic(dead, target, leech(30, 0), null);
        assertEquals(80, target.health());
        assertEquals(0, dead.health());
    }

    @Test
    void tickFunnelShouldDamageTheCasterAndHealTheTarget() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PERIODIC_HEALTH_FUNNEL));
        Player caster = alive(100, 80);
        Player target = alive(100, 10);
        assertTrue(eng.auras().tickCatalogPeriodic(caster, target, funnel(30), null));
        assertEquals(50, caster.health());
        assertEquals(40, target.health());
    }

    private static SpellEngine.SpellInfo leech(int amount, int school) {
        return new SpellEngine.SpellInfo(999053, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_PERIODIC_LEECH, school, 0, amount, amount, 0f);
    }

    private static SpellEngine.SpellInfo funnel(int amount) {
        return new SpellEngine.SpellInfo(999062, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_PERIODIC_HEALTH_FUNNEL, 0, 0, amount, amount, 0f);
    }

    private static Player alive(int max, int health) {
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, max);
        player.setHealth(health);
        return player;
    }
}
