package org.tbc.world.spell;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-263..266 — health regen percent, combat regen, flat combat regen, interrupt regen. */
class AuraEngineModHealthRegenTest {
    @Test
    void healthRegenPercentShouldRaiseOutOfCombatSpiritRegen() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_HEALTH_REGEN_PERCENT));
        Player plain = mage();
        plain.setHealth(1);
        plain.regenerateAll(Player.REGEN_TIME_FULL);
        int plainHealth = plain.health();
        assertTrue(plainHealth > 1);

        Player boosted = mage();
        SpellEngine.SpellInfo aura = regen(999088, AuraEngine.SPELL_AURA_MOD_HEALTH_REGEN_PERCENT, 100);
        eng.auras().apply(boosted, aura);
        boosted.setHealth(1);
        boosted.regenerateAll(Player.REGEN_TIME_FULL);
        assertTrue(boosted.health() > plainHealth);
        eng.auras().unapply(boosted, aura);
        boosted.setHealth(1);
        boosted.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(plainHealth, boosted.health());
    }

    @Test
    void regenDuringCombatShouldRestoreSpiritRegenAtTheAuraPercent() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_REGEN_DURING_COMBAT));
        Player ooc = mage();
        ooc.setHealth(1);
        ooc.regenerateAll(Player.REGEN_TIME_FULL);

        Player fighting = mage();
        fighting.inCombat = true;
        fighting.setHealth(1);
        fighting.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(1, fighting.health());

        SpellEngine.SpellInfo full = regen(999116, AuraEngine.SPELL_AURA_MOD_REGEN_DURING_COMBAT, 100);
        eng.auras().apply(fighting, full);
        fighting.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(ooc.health(), fighting.health());

        Player zeroed = mage();
        zeroed.inCombat = true;
        zeroed.setHealth(1);
        eng.auras().apply(zeroed, regen(999117, AuraEngine.SPELL_AURA_MOD_REGEN_DURING_COMBAT, 0));
        zeroed.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(1, zeroed.health());
    }

    @Test
    void regenInCombatShouldAddFlatHealthAndSkipSpirit() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_HEALTH_REGEN_IN_COMBAT));
        Player player = mage();
        int max = player.maxHealth();
        assertTrue(max > 20);
        player.inCombat = true;
        player.setHealth(max - 20);
        SpellEngine.SpellInfo aura = regen(999161, AuraEngine.SPELL_AURA_MOD_HEALTH_REGEN_IN_COMBAT, 5);
        eng.auras().apply(player, aura);
        player.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(max - 10, player.health());
        eng.auras().unapply(player, aura);
        player.setHealth(max - 20);
        player.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(max - 20, player.health());
    }

    @Test
    void interruptRegenShouldStopRageDecay() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_INTERRUPT_REGEN));
        Player open = warrior();
        open.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(75, open.rage());

        Player held = warrior();
        SpellEngine.SpellInfo aura = regen(999094, AuraEngine.SPELL_AURA_INTERRUPT_REGEN, 0);
        eng.auras().apply(held, aura);
        held.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(100, held.rage());
        eng.auras().unapply(held, aura);
        held.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(75, held.rage());
    }

    private static Player mage() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player player = new Player();
        player.race = 1;
        player.clazz = 8;
        player.level = 1;
        player.initStatsForLevel(mgr.levelStats);
        return player;
    }

    private static Player warrior() {
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(100);
        player.powerType = Player.POWER_RAGE;
        player.setRage(100);
        return player;
    }

    private static SpellEngine.SpellInfo regen(int id, int aura, int amount) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA, aura, 0, 0, amount, amount, 0f);
    }
}
