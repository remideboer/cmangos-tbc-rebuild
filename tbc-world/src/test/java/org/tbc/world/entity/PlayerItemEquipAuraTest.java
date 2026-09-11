package org.tbc.world.entity;

import org.tbc.world.content.Content;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.AuraSlots;
import org.tbc.world.spell.SpellEngine;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerItemEquipAuraTest {
    /**
     * TP-SL14-013 — RemoveAurasDueToItemSpell: an empty wanted set clears the ON_EQUIP aura.
     */
    @Test
    void syncItemEquipAurasWhenWantedEmptyShouldClearOnEquipAura() {
        Player p = new Player();
        p.level = 1;
        p.syncItemEquipAuras(Set.of(Content.SPELL_ATTACK_POWER_60));
        assertEquals(Content.SPELL_ATTACK_POWER_60, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        p.syncItemEquipAuras(Set.of());
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    /**
     * TP-SL14-013 — unequip must not strip a cast aura such as Frost Armor 168.
     */
    @Test
    void syncItemEquipAurasWhenUnequippingShouldLeaveCastAuras() {
        Player p = new Player();
        p.level = 1;
        AuraSlots.applyVisible(p, SpellEngine.FROST_ARMOR, 1, 1);
        p.syncItemEquipAuras(Set.of(Content.SPELL_ATTACK_POWER_60));
        p.syncItemEquipAuras(Set.of());
        assertEquals(SpellEngine.FROST_ARMOR, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(-1, AuraSlots.slotOf(p, Content.SPELL_ATTACK_POWER_60));
    }
}
