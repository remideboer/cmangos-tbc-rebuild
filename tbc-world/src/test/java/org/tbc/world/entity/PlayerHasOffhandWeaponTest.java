package org.tbc.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CMaNGOS Player::GetWeaponForAttack — offhand swing only when Class == ITEM_CLASS_WEAPON.
 * Shields (armor) must not trigger HITINFO_LEFTSWING auto-attacks.
 */
class PlayerHasOffhandWeaponTest {

    @Test
    void hasOffhandWeaponWhenShieldEquippedShouldReturnFalse() {
        Player p = new Player();
        Item shield = new Item(8, 2362);
        shield.bag = 0;
        shield.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        shield.itemClass = Player.ITEM_CLASS_ARMOR;
        shield.inventoryType = 14;
        p.items.put(8, shield);
        assertFalse(p.hasOffhandWeapon());
    }

    @Test
    void hasOffhandWeaponWhenOffhandWeaponEquippedShouldReturnTrue() {
        Player p = new Player();
        Item dagger = new Item(9, 2092);
        dagger.bag = 0;
        dagger.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        dagger.itemClass = Player.ITEM_CLASS_WEAPON;
        dagger.inventoryType = 13;
        p.items.put(9, dagger);
        assertTrue(p.hasOffhandWeapon());
    }

    @Test
    void hasOffhandWeaponWhenEmptyShouldReturnFalse() {
        assertFalse(new Player().hasOffhandWeapon());
    }
}
