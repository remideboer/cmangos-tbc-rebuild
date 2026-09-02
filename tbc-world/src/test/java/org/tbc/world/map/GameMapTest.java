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
}
