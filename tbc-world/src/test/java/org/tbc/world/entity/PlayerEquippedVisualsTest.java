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

    @Test
    void setVisibleItemSlotWhenUnequipShouldClearEntry() {
        Player p = new Player();
        p.guid = 1;
        Item sword = new Item(7, 25);
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.setVisibleItemSlot(Player.EQUIPMENT_SLOT_MAINHAND, sword);
        assertEquals(25, p.getInt(UpdateFields.PLAYER_VISIBLE_ITEM_1_0
                + Player.EQUIPMENT_SLOT_MAINHAND * Player.MAX_VISIBLE_ITEM_OFFSET));
        p.setVisibleItemSlot(Player.EQUIPMENT_SLOT_MAINHAND, null);
        assertEquals(0, p.getInt(UpdateFields.PLAYER_VISIBLE_ITEM_1_0
                + Player.EQUIPMENT_SLOT_MAINHAND * Player.MAX_VISIBLE_ITEM_OFFSET));
    }

    @Test
    void refreshSheathWhenNoMainhandShouldClearSheathByte() {
        Player p = new Player();
        p.guid = 1;
        p.setInt(UpdateFields.UNIT_FIELD_BYTES_2, 1 | (Player.PLAYER_CONTROLLED_DEBUFF_LIMIT << 8));
        p.refreshSheath();
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF);
    }

    /**
     * CMaNGOS default SHEATH_STATE_UNARMED (0): equipped weapons/shield stay in PLAYER_VISIBLE_ITEM
     * so the client draws them sheathed on the back/hip — not forced SHEATH_STATE_MELEE (1).
     */
    @Test
    void applyEquippedVisualsWhenWeaponAndShieldShouldStayUnarmedSheath() {
        Player p = new Player();
        p.guid = 1;
        Item sword = new Item(7, 25);
        sword.bag = 0;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.items.put(7, sword);
        Item shield = new Item(8, 2362);
        shield.bag = 0;
        shield.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        p.items.put(8, shield);
        p.applyCreateFields();
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF, "SHEATH_STATE_UNARMED");
        assertEquals(25, p.getInt(UpdateFields.PLAYER_VISIBLE_ITEM_1_0
                + Player.EQUIPMENT_SLOT_MAINHAND * Player.MAX_VISIBLE_ITEM_OFFSET));
        assertEquals(2362, p.getInt(UpdateFields.PLAYER_VISIBLE_ITEM_1_0
                + Player.EQUIPMENT_SLOT_OFFHAND * Player.MAX_VISIBLE_ITEM_OFFSET));
    }

    @Test
    void refreshSheathWhenDrawnShouldKeepMeleeUntilUnequip() {
        Player p = new Player();
        p.guid = 1;
        Item sword = new Item(7, 25);
        sword.bag = 0;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.items.put(7, sword);
        p.setInt(UpdateFields.UNIT_FIELD_BYTES_2,
                1 | (Player.PLAYER_CONTROLLED_DEBUFF_LIMIT << 8));
        p.refreshSheath();
        assertEquals(1, p.getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF, "client-drawn sheath kept");
    }
}
