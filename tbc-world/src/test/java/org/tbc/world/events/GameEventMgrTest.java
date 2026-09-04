package org.tbc.world.events;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameEventMgrTest {
    @Test
    void startWhenMidsummerShouldSpawnLumaSkymother() {
        World world = World.inMemory();
        assertNull(find(world, Content.NPC_LUMA_SKYMOTHER));
        world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
        assertNotNull(find(world, Content.NPC_LUMA_SKYMOTHER));
    }

    @Test
    void stopWhenMidsummerShouldDespawnLumaSkymother() {
        World world = World.inMemory();
        world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
        assertNotNull(find(world, Content.NPC_LUMA_SKYMOTHER));
        world.events.stop(world, Content.GAME_EVENT_MIDSUMMER);
        assertNull(find(world, Content.NPC_LUMA_SKYMOTHER));
    }

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(547, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
    }
}
