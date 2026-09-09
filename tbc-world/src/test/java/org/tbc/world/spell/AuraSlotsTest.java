package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL07-006 — SpellAuraHolder::_AddAura visible slot packing. */
class AuraSlotsTest {
    @Test
    void applyVisibleWhenSlotFreeShouldWriteSpellIdAndPackedBytes() {
        Player p = new Player();
        p.level = 5;
        int slot = AuraSlots.applyVisible(p, 168, 5, 1);
        assertEquals(0, slot);
        assertEquals(168, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(AuraSlots.AFLAG_EFFECT_0 | AuraSlots.AFLAG_CANCELABLE,
                p.getInt(UpdateFields.UNIT_FIELD_AURAFLAGS) & 0xFF);
        assertEquals(5, p.getInt(UpdateFields.UNIT_FIELD_AURALEVELS) & 0xFF);
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_AURAAPPLICATIONS) & 0xFF);
    }

    @Test
    void applyVisibleWhenFirstSlotTakenShouldUseNext() {
        Player p = new Player();
        AuraSlots.applyVisible(p, 168, 1, 1);
        assertEquals(1, AuraSlots.applyVisible(p, 122, 1, 1));
        assertEquals(122, p.getInt(UpdateFields.UNIT_FIELD_AURA + 1));
    }

    @Test
    void applyVisibleWhenAllSlotsTakenShouldReturnMinusOne() {
        Player p = new Player();
        for (int i = 0; i < AuraSlots.MAX_AURAS; i++) {
            p.setInt(UpdateFields.UNIT_FIELD_AURA + i, 1);
        }
        assertEquals(-1, AuraSlots.applyVisible(p, 168, 1, 1));
    }

    @Test
    void sendApplyWhenPlayerTargetShouldSendValuesAndDuration() {
        Player p = new Player();
        p.guid = 9;
        AuraSlots.applyVisible(p, 168, 1, 1);
        List<Integer> ops = new ArrayList<>();
        AuraSlots.sendApply(p, 168, 1_800_000, (op, payload) -> ops.add(op));
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_OBJECT) || ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_AURA_DURATION));
    }

    @Test
    void sendApplyWhenCreatureOrUnknownSpellShouldSkipDuration() {
        Creature c = new Creature();
        c.guid = 3;
        AuraSlots.applyVisible(c, 168, 1, 1);
        List<Integer> ops = new ArrayList<>();
        AuraSlots.sendApply(c, 168, 1000, (op, payload) -> ops.add(op));
        assertEquals(1, ops.size());
        AuraSlots.sendApply(c, 999, 1000, (op, payload) -> ops.add(op));
        assertEquals(1, ops.size());
        AuraSlots.sendApply(c, 168, 1000, null);
        assertEquals(1, ops.size());
    }
}
