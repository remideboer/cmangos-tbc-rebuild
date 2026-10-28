package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-255 — SPELL_AURA_OVERRIDE_CLASS_SCRIPTS (112). */
class AuraEngineOverrideClassScriptTest {
    @Test
    void applyAuraWhenOverrideClassScriptShouldKeyByScriptId() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_OVERRIDE_CLASS_SCRIPTS));
        Player player = new Player();
        SpellEngine.SpellInfo first = aura(999112, 4401);
        SpellEngine.SpellInfo same = aura(999113, 4401);
        SpellEngine.SpellInfo other = aura(999114, 4402);
        eng.apply(player, player, first);
        eng.apply(player, player, same);
        eng.apply(player, player, other);
        assertTrue(player.hasOverrideClassScript(4401));
        assertTrue(player.hasOverrideClassScript(4402));
        eng.auras().unapply(player, first);
        assertFalse(player.hasOverrideClassScript(4401));
        assertTrue(player.hasOverrideClassScript(4402));
    }

    private static SpellEngine.SpellInfo aura(int id, int scriptId) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_OVERRIDE_CLASS_SCRIPTS, 0, 0, 0, 0, 0f, scriptId);
    }
}
