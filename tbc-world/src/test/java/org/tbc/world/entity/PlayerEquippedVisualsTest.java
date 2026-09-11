package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerEquippedVisualsTest {
    @Test
    void applyEquippedVisualsWhenBackpackItemShouldBindInvSlotWithoutVisibleItem() {
        Player p = new Player();
        p.guid = 1;
        Item it = new Item(42, 25);
        it.bag = 0;
        it.slot = Player.INVENTORY_SLOT_ITEM_START;
        p.items.put(42, it);
        p.applyEquippedVisuals();
        assertEquals(Guid.HIGH_ITEM | 42L,
                p.getGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2));
        assertEquals(0, p.getInt(UpdateFields.PLAYER_VISIBLE_ITEM_1_0
                + it.slot * Player.MAX_VISIBLE_ITEM_OFFSET));
    }
}
