package org.tbc.world.spell;

import org.tbc.world.entity.GameObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameObjectUseTest {
    @Test
    void openDoorReadyToActive() {
        GameObject door = new GameObject();
        door.type = GameObjectUse.TYPE_DOOR;
        door.state = GameObjectUse.STATE_READY;
        assertTrue(GameObjectUse.openDoor(door));
        assertEquals(GameObjectUse.STATE_ACTIVE, door.state);
        assertFalse(GameObjectUse.openDoor(door));
    }

    @Test
    void openLockChestReadyToActive() {
        GameObject chest = new GameObject();
        chest.type = GameObjectUse.TYPE_CHEST;
        chest.state = GameObjectUse.STATE_READY;
        assertTrue(GameObjectUse.openLock(chest));
        assertEquals(GameObjectUse.STATE_ACTIVE, chest.state);
        assertFalse(GameObjectUse.openLock(chest));
        assertFalse(GameObjectUse.openLock(null));
    }
}
