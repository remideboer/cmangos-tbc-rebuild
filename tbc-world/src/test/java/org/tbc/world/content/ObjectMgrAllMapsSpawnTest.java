package org.tbc.world.content;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.map.GameMap;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every non-event creature and gameobject row is loaded and placed, including map 530.
 */
class ObjectMgrAllMapsSpawnTest {
    @Test
    void placesSpawnWhenMap530ShouldIncludeIt() {
        for (int mapId : new int[]{0, 1, 530, 30, 489, 565}) {
            assertTrue(World.placesSpawnOnBoot(true, mapId), "map " + mapId);
        }
        for (String sql : SpawnLoader.creatureSpawnQueries()) {
            assertFalse(sql.contains("map IN (0, 1)"), sql);
            assertFalse(sql.toUpperCase().contains("LIMIT"), sql);
        }
        for (String sql : SpawnLoader.gameObjectSpawnQueries()) {
            assertFalse(sql.contains("map IN (0, 1)"), sql);
            assertFalse(sql.toUpperCase().contains("LIMIT"), sql);
        }
    }

    @Test
    void ensureMapSpawnsWhenNewInstanceShouldCopyCreaturesAndObjects() {
        World world = World.inMemory();
        world.objectMgr.spawns.add(new ObjectMgr.Spawn(9001, 15271, 530, 10349.6f, -6357.29f, 33.4f, 0f));
        world.objectMgr.goSpawns.add(new ObjectMgr.Spawn(9002, 181582, 530, 10350f, -6358f, 33.4f, 0f));
        world.ensureMapSpawns(530, 3);
        GameMap map = world.map(530, 3);
        boolean creature = false;
        for (Creature c : map.creatures.values()) {
            if (c.entry == 15271) {
                creature = true;
            }
        }
        boolean go = false;
        for (GameObject g : map.gameObjects.values()) {
            if (g.entry == 181582) {
                go = true;
            }
        }
        assertTrue(creature);
        assertTrue(go);
    }
}
