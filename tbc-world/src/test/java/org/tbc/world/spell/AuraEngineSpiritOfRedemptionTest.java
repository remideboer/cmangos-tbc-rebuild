package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-246 — SPELL_AURA_SPIRIT_OF_REDEMPTION (176). */
class AuraEngineSpiritOfRedemptionTest {
    @Test
    void applyAuraWhenSpiritOfRedemptionShouldFillManaAndDieOnUnapply() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_SPIRIT_OF_REDEMPTION));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(40);
        player.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        player.setInt(UpdateFields.UNIT_FIELD_POWER1, 10);
        player.sit();
        SpellEngine.SpellInfo aura = aura(999176);
        eng.apply(player, player, aura);
        assertTrue(player.isStanding());
        assertEquals(40, player.health());
        assertEquals(200, player.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertTrue(player.isImmuneToDamage(1));
        assertTrue(player.isImmuneToDamage(255));
        assertFalse(player.isImmuneToDamage(0));
        eng.auras().unapply(player, aura);
        assertEquals(0, player.health());
        assertFalse(player.isImmuneToDamage(1));
        assertTrue((player.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PLAYER_CONTROLLED) != 0);
    }

    private static SpellEngine.SpellInfo aura(int id) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_SPIRIT_OF_REDEMPTION, 0, 0, 0, 0, 0f);
    }
}
