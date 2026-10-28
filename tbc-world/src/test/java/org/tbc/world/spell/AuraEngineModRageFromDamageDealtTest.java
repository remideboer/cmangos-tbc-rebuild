package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-267 — rage from damage dealt (213). */
class AuraEngineModRageFromDamageDealtTest {
    @Test
    void rageFromDamageDealtShouldScaleTheAttackerGain() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RAGE_FROM_DAMAGE_DEALT));
        Player plain = warrior();
        plain.rewardRageFromHit(100, false);
        int base = plain.rage();
        assertTrue(base > 0);

        Player boosted = warrior();
        SpellEngine.SpellInfo aura = new SpellEngine.SpellInfo(999213, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RAGE_FROM_DAMAGE_DEALT, 0, 0, 100, 100, 0f);
        eng.auras().apply(boosted, aura);
        boosted.rewardRageFromHit(100, false);
        int doubled = boosted.rage();
        assertTrue(doubled > base);
        assertTrue(Math.abs(doubled - base * 2) <= 1);

        eng.auras().unapply(boosted, aura);
        boosted.setRage(0);
        boosted.rewardRageFromHit(100, false);
        assertEquals(base, boosted.rage());
    }

    private static Player warrior() {
        Player player = new Player();
        player.powerType = Player.POWER_RAGE;
        player.level = 1;
        return player;
    }
}
