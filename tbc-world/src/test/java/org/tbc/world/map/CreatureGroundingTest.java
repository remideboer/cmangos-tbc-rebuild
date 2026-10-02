package org.tbc.world.map;

import org.junit.jupiter.api.Test;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.net.wow8606.UpdateFields;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Floor-safe Z: never ADT-pull under buildings. */
class CreatureGroundingTest {

    @Test
    void resolveZWhenMapBelowHintShouldKeepHint() {
        assertEquals(100f, CreatureGrounding.resolveZ(100f, 98f), 1e-4f);
        assertEquals(100f, CreatureGrounding.resolveZ(100f, 90f), 1e-4f);
    }

    @Test
    void resolveZWhenMapNearHintShouldUseMap() {
        assertEquals(99.8f, CreatureGrounding.resolveZ(100f, 99.8f), 1e-4f);
        assertEquals(100.2f, CreatureGrounding.resolveZ(100f, 100.2f), 1e-4f);
    }

    @Test
    void resolveZWhenMapAboveHintShouldKeepHint() {
        assertEquals(50f, CreatureGrounding.resolveZ(50f, 55f), 1e-4f);
    }

    @Test
    void respawnShouldKeepSpawnZWhenAdtBelow() {
        Combat combat = new Combat();
        Creature c = creature(50f);
        c.spawnX = 1f;
        c.spawnY = 2f;
        c.spawnZ = 100f;
        c.spawnO = 0f;
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 40);
        c.setHealth(1);
        combat.respawn(c);
        assertEquals(100f, c.z, 1e-4f);
        assertEquals(100f, c.spawnZ, 1e-4f);
    }

    private static Creature creature(float z) {
        Creature c = new Creature();
        c.mapId = 0;
        c.x = 1f;
        c.y = 2f;
        c.z = z;
        c.o = 0f;
        c.inhabitType = CreatureGrounding.DEFAULT_INHABIT;
        return c;
    }
}
