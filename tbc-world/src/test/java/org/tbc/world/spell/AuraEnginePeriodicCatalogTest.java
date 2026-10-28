package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-250..254 — periodic heal (8), health percent (20), mana percent (21),
 * energize (24), and damage percent (89).
 */
class AuraEnginePeriodicCatalogTest {
    @Test
    void tickPeriodicHealAndHealthPercentShouldStopWhenFullOrDead() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PERIODIC_HEAL));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_OBS_MOD_HEALTH));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(50);
        SpellEngine.SpellInfo heal = tick(999008, AuraEngine.SPELL_AURA_PERIODIC_HEAL, 10, 0);
        SpellEngine.SpellInfo percent = tick(999020, AuraEngine.SPELL_AURA_OBS_MOD_HEALTH, 10, 0);
        eng.auras().tickCatalogPeriodic(null, player, heal, null);
        assertEquals(50, player.health());
        eng.auras().tickCatalogPeriodic(player, player, heal, null);
        assertEquals(60, player.health());
        eng.auras().tickCatalogPeriodic(player, player, percent, null);
        assertEquals(70, player.health());
        player.setHealth(100);
        eng.auras().tickCatalogPeriodic(player, player, heal, null);
        assertEquals(100, player.health());
        player.setHealth(0);
        eng.auras().tickCatalogPeriodic(player, player, percent, null);
        assertEquals(0, player.health());
    }

    @Test
    void tickManaPercentAndEnergizeShouldUseThePowerField() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_OBS_MOD_MANA));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PERIODIC_ENERGIZE));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(100);
        player.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        player.setInt(UpdateFields.UNIT_FIELD_POWER1, 10);
        player.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        player.setInt(UpdateFields.UNIT_FIELD_POWER4, 20);
        eng.auras().tickCatalogPeriodic(player, player,
                tick(999021, AuraEngine.SPELL_AURA_OBS_MOD_MANA, 10, 0), null);
        assertEquals(30, player.getInt(UpdateFields.UNIT_FIELD_POWER1));
        eng.auras().tickCatalogPeriodic(player, player,
                tick(999024, AuraEngine.SPELL_AURA_PERIODIC_ENERGIZE, 15, Player.POWER_ENERGY), null);
        assertEquals(35, player.getInt(UpdateFields.UNIT_FIELD_POWER4));
        eng.auras().tickCatalogPeriodic(player, player,
                tick(999025, AuraEngine.SPELL_AURA_PERIODIC_ENERGIZE, 15, 9), null);
        assertEquals(35, player.getInt(UpdateFields.UNIT_FIELD_POWER4));
        Player empty = new Player();
        empty.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        empty.setHealth(100);
        eng.auras().tickCatalogPeriodic(empty, empty,
                tick(999026, AuraEngine.SPELL_AURA_OBS_MOD_MANA, 10, 0), null);
        assertEquals(0, empty.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void tickDamagePercentShouldReduceCurrentHealth() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PERIODIC_DAMAGE_PERCENT));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(80);
        AtomicInteger packets = new AtomicInteger();
        eng.tickPeriodic(player, player,
                tick(999089, AuraEngine.SPELL_AURA_PERIODIC_DAMAGE_PERCENT, 10, 0),
                (op, payload) -> packets.incrementAndGet());
        assertEquals(70, player.health());
        assertTrue(packets.get() > 0);
        player.setHealth(0);
        eng.tickPeriodic(player, player,
                tick(999089, AuraEngine.SPELL_AURA_PERIODIC_DAMAGE_PERCENT, 10, 0),
                (op, payload) -> packets.incrementAndGet());
        assertEquals(0, player.health());
    }

    @Test
    void applySpell40409ShouldSetUninteractible() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo special = new SpellEngine.SpellInfo(40409, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_OBS_MOD_HEALTH, 0, 0, 5, 5, 0f);
        eng.apply(player, player, special);
        assertTrue((player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_UNINTERACTIBLE) != 0);
        assertTrue((player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PLAYER_CONTROLLED) != 0);
        eng.auras().unapply(player, special);
        assertTrue((player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_UNINTERACTIBLE) == 0);
        assertTrue((player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PLAYER_CONTROLLED) != 0);
    }

    private static SpellEngine.SpellInfo tick(int id, int aura, int amount, int misc) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA, aura, 0, 0, amount, amount, 0f, misc);
    }
}
