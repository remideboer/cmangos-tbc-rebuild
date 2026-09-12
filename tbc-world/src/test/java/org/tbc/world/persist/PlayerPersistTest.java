package org.tbc.world.persist;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.ReputationMgr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPersistTest {
    @Test
    void copyEmptyItemsAndBuybackExcluded() {
        Player src = new Player();
        src.guid = 7;
        src.accountId = 1;
        src.name = "Copy";
        src.money = 9;
        src.resting = true;
        src.restBonus = 12.5f;
        src.deleteDateMs = 99;
        src.actionButtons[3] = 78;
        Item back = new Item(99, 25);
        src.buyback.put(99, back);
        Player d = PlayerPersist.copy(src);
        assertNotSame(src, d);
        assertEquals(9, d.money);
        assertEquals(12.5f, d.restBonus);
        assertTrue(d.resting);
        assertEquals(99, d.deleteDateMs);
        assertEquals(78, d.actionButtons[3]);
        assertTrue(d.items.isEmpty());
        assertTrue(d.buyback.isEmpty());
        src.money = 1;
        assertEquals(9, d.money);
    }

    @Test
    void copyClonesItems() {
        Player src = new Player();
        src.guid = 8;
        Item it = new Item(5, 25);
        it.count = 3;
        it.bag = 0;
        it.slot = 23;
        src.items.put(5, it);
        Player d = PlayerPersist.copy(src);
        assertEquals(1, d.items.size());
        Item c = d.items.get(5);
        assertNotSame(it, c);
        assertEquals(25, c.entry);
        assertEquals(3, c.count);
        assertEquals(23, c.slot);
        it.count = 9;
        assertEquals(3, c.count);
    }

    @Test
    void copyWhenSpellCooldownSetShouldCloneExpireTime() {
        Player src = new Player();
        src.guid = 9;
        src.cooldowns.addSpell(122, 25_000, 1_000);
        Player d = PlayerPersist.copy(src);
        assertFalse(d.cooldowns.isSpellReady(122, 25_999));
        assertTrue(d.cooldowns.isSpellReady(122, 26_000));
        src.cooldowns.addSpell(122, 1, 1_000);
        assertFalse(d.cooldowns.isSpellReady(122, 25_999), "clone is independent");
    }

    @Test
    void copyWhenFactionInactiveShouldKeepFlags() {
        Player src = new Player();
        src.guid = 10;
        src.reputations.seedCreateDefaults(ReputationMgr.TEAM_ALLIANCE);
        src.reputations.setInactive(ReputationMgr.LIST_STORMWIND, true);
        Player d = PlayerPersist.copy(src);
        assertEquals(ReputationMgr.FLAG_INACTIVE,
                d.reputations.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_INACTIVE);
        src.reputations.setInactive(ReputationMgr.LIST_STORMWIND, false);
        assertEquals(ReputationMgr.FLAG_INACTIVE,
                d.reputations.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_INACTIVE);
    }

    @Test
    void copyWhenGhostShouldKeepGhostFlagAndHealth() {
        Player src = new Player();
        src.guid = 11;
        src.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 60);
        src.setHealth(1);
        src.setGhost(true);
        Player d = PlayerPersist.copy(src);
        assertTrue(d.ghost);
        assertEquals(Player.PLAYER_FLAGS_GHOST, d.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FLAGS)
                & Player.PLAYER_FLAGS_GHOST);
        assertEquals(1, d.health());
    }

    @Test
    void copyWhenCorpseHealthZeroShouldKeepZero() {
        Player src = new Player();
        src.guid = 12;
        src.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 60);
        src.setHealth(0);
        Player d = PlayerPersist.copy(src);
        assertFalse(d.ghost);
        assertEquals(0, d.health());
    }

    @Test
    void copyWhenAliveShouldKeepPositiveHealth() {
        Player src = new Player();
        src.guid = 13;
        src.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 60);
        src.setHealth(40);
        Player d = PlayerPersist.copy(src);
        assertFalse(d.ghost);
        assertEquals(40, d.health());
    }
}
