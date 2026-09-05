package org.tbc.world.content;

import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectMgrLootTest {
    @Test
    void fillCorpseLootWhenSeedKoboldShouldMakeNonEmptyLootWindow() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Creature c = mgr.spawnCreature(6, 0, 0, 0, 0, 0, null);
        c.lootable = true;
        c.taggedBy = 1;
        mgr.fillCorpseLoot(c);
        Player p = new Player();
        p.guid = 1;
        byte[] loot = new Combat().lootResponse(p, c);
        assertNotNull(loot);
        assertEquals(Combat.LOOT_CORPSE, loot[8] & 0xFF);
        int itemCount = loot[13] & 0xFF;
        assertTrue(itemCount >= 1);
        int itemId = (loot[15] & 0xFF) | (loot[16] & 0xFF) << 8 | (loot[17] & 0xFF) << 16 | (loot[18] & 0xFF) << 24;
        assertEquals(Content.ITEM_WORN_SHORTSWORD, itemId);
    }
}
