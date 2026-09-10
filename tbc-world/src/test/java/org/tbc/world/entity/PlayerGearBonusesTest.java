package org.tbc.world.entity;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerGearBonusesTest {
    /**
     * TP-SL14-013 — applyGearBonuses without InitStatsForLevel must not smash a stub
     * UNIT_FIELD_MAXHEALTH (createHealth still 0).
     */
    @Test
    void applyGearBonusesWhenCreateHealthUnsetShouldLeaveMaxHealth() {
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(40);
        p.applyGearBonuses(2, 0, 0, 0, 0, 0);
        assertEquals(50, p.maxHealth());
        assertEquals(40, p.health());
    }

    /**
     * TP-SL14-013 — Unit::SetMaxHealth clamps current HP when max shrinks below it.
     */
    @Test
    void applyGearBonusesWhenStaminaRemovedAndHealthAboveMaxShouldClampHealth() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.race = 1;
        p.clazz = 1;
        p.level = 1;
        p.initStatsForLevel(mgr.levelStats);
        p.applyGearBonuses(2, 0, 0, 0, 0, 0);
        p.setHealth(p.maxHealth());
        p.applyGearBonuses(0, 0, 0, 0, 0, 0);
        assertEquals(p.maxHealth(), p.health());
        assertEquals(60, p.maxHealth());
    }
}
