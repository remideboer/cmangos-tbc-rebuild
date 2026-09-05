package org.tbc.world.map;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapTest {
    @Test
    void nearbyCreaturesSkipsFarCells() {
        GameMap map = new GameMap(0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        map.add(p);
        Creature near = new Creature();
        near.guid = 2;
        near.applyTemplate(6, "Near", 1, 7, 10, 1);
        near.relocate(10, 0, 0, 0);
        map.add(near);
        Creature far = new Creature();
        far.guid = 3;
        far.applyTemplate(6, "Far", 1, 7, 10, 1);
        far.relocate(500, 0, 0, 0);
        map.add(far);
        var found = map.nearbyCreatures(p, GameMap.VISIBILITY);
        assertEquals(1, found.size());
        assertEquals(2, found.get(0).guid);
        assertTrue(map.nearbyCreatures(p, 5).isEmpty());
        p.relocate(8, 0, 0, 0);
        assertEquals(1, map.nearbyCreatures(p, 5).size());
    }

    @Test
    void nearbyCreaturesWhenRelocatedAcrossCellShouldStillFind() {
        GameMap map = new GameMap(0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        map.add(p);
        Creature c = new Creature();
        c.guid = 2;
        c.applyTemplate(6, "Wolf", 1, 7, 10, 1);
        c.relocate(10, 0, 0, 0);
        map.add(c);
        float oldX = c.x;
        float oldY = c.y;
        c.relocate(250, 0, 0, 0);
        map.reindex(c, oldX, oldY);
        map.reindex((Creature) null, 0, 0);
        p.relocate(250, 0, 0, 0);
        var found = map.nearbyCreatures(p, GameMap.VISIBILITY);
        assertEquals(1, found.size());
        assertEquals(2, found.get(0).guid);
        map.remove(c);
        assertTrue(map.nearbyCreatures(p, GameMap.VISIBILITY).isEmpty());
    }

    @Test
    void creaturesNearPlayersWhenFarCellShouldOmit() {
        GameMap map = new GameMap(0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        map.add(p);
        Creature near = new Creature();
        near.guid = 2;
        near.applyTemplate(6, "Near", 1, 7, 10, 1);
        near.relocate(10, 0, 0, 0);
        map.add(near);
        Creature deadNear = new Creature();
        deadNear.guid = 4;
        deadNear.applyTemplate(6, "Dead", 1, 7, 10, 1);
        deadNear.relocate(20, 0, 0, 0);
        deadNear.setHealth(0);
        map.add(deadNear);
        Creature far = new Creature();
        far.guid = 3;
        far.applyTemplate(6, "Far", 1, 7, 10, 1);
        far.relocate(500, 0, 0, 0);
        map.add(far);
        var found = map.creaturesNearPlayers(GameMap.VISIBILITY);
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(c -> c.guid == 2));
        assertTrue(found.stream().anyMatch(c -> c.guid == 4));
        assertTrue(found.stream().noneMatch(c -> c.guid == 3));
        map.remove(p);
        assertTrue(map.creaturesNearPlayers(GameMap.VISIBILITY).isEmpty());
    }

    @Test
    void nearbyPlayersWhenFarCellShouldOmit() {
        GameMap map = new GameMap(0, 0);
        Creature c = new Creature();
        c.guid = 1;
        c.applyTemplate(6, "Wolf", 1, 7, 10, 1);
        c.relocate(0, 0, 0, 0);
        map.add(c);
        Player near = new Player();
        near.guid = 2;
        near.relocate(10, 0, 0, 0);
        map.add(near);
        Player far = new Player();
        far.guid = 3;
        far.relocate(500, 0, 0, 0);
        map.add(far);
        var found = map.nearbyPlayers(c, GameMap.VISIBILITY);
        assertEquals(1, found.size());
        assertEquals(2, found.get(0).guid);
        map.remove(near);
        assertTrue(map.nearbyPlayers(c, GameMap.VISIBILITY).isEmpty());
    }

    @Test
    void nearbyPlayersWhenRelocatedAcrossCellShouldStillFind() {
        GameMap map = new GameMap(0, 0);
        Creature c = new Creature();
        c.guid = 1;
        c.applyTemplate(6, "Wolf", 1, 7, 10, 1);
        c.relocate(1000, 0, 0, 0);
        map.add(c);
        Player p = new Player();
        p.guid = 2;
        p.relocate(0, 0, 0, 0);
        map.add(p);
        float oldX = p.x;
        float oldY = p.y;
        p.relocate(1000, 0, 0, 0);
        map.reindex(p, oldX, oldY);
        map.reindex((Player) null, 0, 0);
        var found = map.nearbyPlayers(c, GameMap.VISIBILITY);
        assertEquals(1, found.size());
        assertEquals(2, found.get(0).guid);
        map.remove(p);
        assertTrue(map.nearbyPlayers(c, GameMap.VISIBILITY).isEmpty());
    }
}
