package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-228 — SPELL_AURA_COMPREHEND_LANGUAGE (244).
 * CMaNGOS HandleComprehendLanguage sets UNIT_FLAG2_COMPREHEND_LANG.
 */
class AuraEngineComprehendLanguageTest {
    private static final SpellEngine.SpellInfo COMPREHEND = new SpellEngine.SpellInfo(
            999244, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_COMPREHEND_LANGUAGE, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenComprehendLanguageShouldSetTheFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_COMPREHEND_LANGUAGE));
        Player player = new Player();
        player.setFeignDeath(true);
        Creature mob = new Creature();
        eng.apply(player, player, COMPREHEND);
        eng.apply(player, mob, COMPREHEND);
        assertEquals(Unit.UNIT_FLAG2_FEIGN_DEATH | Unit.UNIT_FLAG2_COMPREHEND_LANG,
                player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2));
        assertEquals(Unit.UNIT_FLAG2_COMPREHEND_LANG, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS_2));
    }

    @Test
    void unapplyWhenComprehendLanguageShouldClearOnlyThatFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFeignDeath(true);
        eng.apply(player, player, COMPREHEND);
        eng.auras().unapply(player, COMPREHEND);
        assertEquals(Unit.UNIT_FLAG2_FEIGN_DEATH, player.getInt(UpdateFields.UNIT_FIELD_FLAGS_2));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, COMPREHEND);
    }
}
