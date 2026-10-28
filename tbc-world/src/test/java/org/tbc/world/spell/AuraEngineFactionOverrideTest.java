package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-235 — SPELL_AURA_FACTION_OVERRIDE (243).
 * misc ≤ 0 does nothing. Unapply restores the faction saved on the first apply.
 */
class AuraEngineFactionOverrideTest {
    private static SpellEngine.SpellInfo faction(int id, int template) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_FACTION_OVERRIDE, 0, 0, 1, 1, 0f, template);
    }

    @Test
    void applyAuraWhenFactionOverrideShouldReplaceAndRestoreTheOriginal() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_FACTION_OVERRIDE));
        Player player = new Player();
        player.faction = 1;
        player.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, 1);
        SpellEngine.SpellInfo first = faction(999243, 35);
        SpellEngine.SpellInfo second = faction(999244, 80);
        eng.apply(player, player, first);
        assertEquals(35, player.faction);
        assertEquals(35, player.getInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE));
        eng.apply(player, player, second);
        assertEquals(80, player.getInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE));
        eng.auras().unapply(player, first);
        assertEquals(1, player.faction);
        assertEquals(1, player.getInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE));
    }

    @Test
    void applyWhenMiscIsNotATemplateOrTargetMissingShouldLeaveFaction() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        mob.faction = 14;
        mob.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, 14);
        eng.apply(new Player(), mob, faction(1, 0));
        eng.apply(new Player(), mob, faction(2, -3));
        assertEquals(14, mob.faction);
        assertEquals(14, mob.getInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE));
        new AuraEngine().apply(null, faction(3, 35));
        new AuraEngine().unapply(mob, faction(4, 35));
        assertEquals(14, mob.getInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE));
    }
}
