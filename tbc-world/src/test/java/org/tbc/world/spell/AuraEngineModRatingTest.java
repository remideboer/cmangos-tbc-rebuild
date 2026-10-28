package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-217 — SPELL_AURA_MOD_RATING (189).
 * CMaNGOS HandleModRating → ApplyRatingMod for each misc bit.
 */
class AuraEngineModRatingTest {
    private static final int HIT_MELEE = 5;
    private static final int HIT_SPELL = 7;

    private static SpellEngine.SpellInfo rating(int id, int amount) {
        int mask = (1 << HIT_MELEE) | (1 << HIT_SPELL);
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RATING, 0, 0, amount, amount, 0f, mask);
    }

    @Test
    void applyAuraWhenRatingShouldAddEachMaskedField() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RATING));
        Player player = new Player();
        eng.apply(player, player, rating(999189, 12));
        assertEquals(12, player.getInt(UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + HIT_MELEE));
        assertEquals(12, player.getInt(UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + HIT_SPELL));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6));
    }

    @Test
    void unapplyWhenRatingsStackShouldLeaveTheOtherAura() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo first = rating(999189, 12);
        eng.apply(player, player, first);
        eng.apply(player, player, rating(999190, 8));
        assertEquals(20, player.getInt(UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + HIT_MELEE));
        eng.auras().unapply(player, first);
        assertEquals(8, player.getInt(UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + HIT_MELEE));
        assertEquals(8, player.getInt(UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + HIT_SPELL));
    }

    @Test
    void applyWhenCreatureMaskZeroOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, rating(999189, 12));
        new SpellEngine().apply(new Player(), new Creature(), rating(999189, 12));
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999188, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RATING,
                0, 0, 12, 12, 0f, 0));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + HIT_MELEE));
    }
}
