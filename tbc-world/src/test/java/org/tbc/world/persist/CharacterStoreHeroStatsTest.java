package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.classless.ClasslessConfig;
import org.tbc.world.classless.HeroStatAllocation;
import org.tbc.world.content.LevelStats;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL35-028 — Hero allocation survives in-memory snapshot and SQL character_hero_stats. */
class CharacterStoreHeroStatsTest {
    @AfterEach
    void reset() {
        ClasslessConfig.reset();
    }

    @Test
    void saveWhenHeroSpentShouldReloadFromMemory() {
        World world = World.inMemory();
        Player p = world.characters.create(1, "HeroMem", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        p.giveXp(400, null);
        assertTrue(p.spendHeroStat(HeroStatAllocation.STR, 1));
        assertTrue(p.resetHeroStats());
        assertTrue(p.spendHeroStat(HeroStatAllocation.STR, 1));
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        int unspent = p.heroStats.unspent();
        world.characters.save(p);
        Player loaded = world.characters.load(1, p.guid, world.objectMgr);
        assertEquals(unspent, loaded.heroStats.unspent());
        assertEquals(1, loaded.heroStats.spent(HeroStatAllocation.STR));
        assertEquals(1, loaded.heroStats.resetCount());
        assertEquals(str, loaded.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void saveWhenHeroSpentShouldReloadFromSql() throws Exception {
        String url = "jdbc:h2:mem:hero_stats_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "hero-stats-test")) {
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE characters (
                          guid INT PRIMARY KEY, account INT, name VARCHAR(12), race INT, class INT, gender INT,
                          level INT, xp INT, money INT, playerBytes INT, playerBytes2 INT, playerFlags INT,
                          position_x FLOAT, position_y FLOAT, position_z FLOAT, map INT, dungeon_difficulty INT,
                          orientation FLOAT, online INT, cinematic INT, totaltime INT, leveltime INT,
                          logout_time BIGINT, is_logout_resting INT, rest_bonus FLOAT, zone INT, at_login INT,
                          health INT, power1 INT, power2 INT, power3 INT, power4 INT, power5 INT,
                          watchedFaction BIGINT, actionBars TINYINT, deleteDate BIGINT)
                        """);
                st.execute("""
                        CREATE TABLE character_spell (
                          guid INT, spell INT, active TINYINT, disabled TINYINT,
                          PRIMARY KEY (guid, spell))
                        """);
                st.execute("""
                        CREATE TABLE character_skills (
                          guid INT, skill INT, `value` INT, `max` INT,
                          PRIMARY KEY (guid, skill))
                        """);
                st.execute("CREATE TABLE item_instance (guid INT PRIMARY KEY)");
                st.execute("""
                        CREATE TABLE character_action (
                          guid INT, button INT, action INT, type INT, PRIMARY KEY (guid, button))
                        """);
                st.execute("""
                        CREATE TABLE character_queststatus (
                          guid INT, quest INT, status INT, rewarded INT, explored INT, timer BIGINT,
                          mobcount1 INT, mobcount2 INT, mobcount3 INT, mobcount4 INT,
                          itemcount1 INT, itemcount2 INT, itemcount3 INT, itemcount4 INT,
                          PRIMARY KEY (guid, quest))
                        """);
                st.execute("""
                        CREATE TABLE character_spell_cooldown (
                          guid INT, SpellId INT, SpellExpireTime BIGINT, Category INT,
                          CategoryExpireTime BIGINT, ItemId INT, PRIMARY KEY (guid, SpellId))
                        """);
                st.execute("""
                        CREATE TABLE character_aura (
                          guid INT, caster_guid BIGINT, item_guid INT, spell INT,
                          stackcount INT, remaincharges INT,
                          basepoints0 INT, basepoints1 INT, basepoints2 INT,
                          periodictime0 INT, periodictime1 INT, periodictime2 INT,
                          maxduration INT, remaintime INT, effIndexMask INT,
                          PRIMARY KEY (guid, caster_guid, item_guid, spell))
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(null, null);
            CharacterStore store = new CharacterStore(chars);
            Player p = store.create(1, "HeroSql", 1, ClasslessConfig.CLASS_CLASSLESS,
                    0, 1, 1, 1, 1, 0, mgr);
            p.giveXp(400, null);
            assertTrue(p.spendHeroStat(HeroStatAllocation.STR, 1));
            int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
            int unspent = p.heroStats.unspent();
            store.save(p);

            CharacterStore again = new CharacterStore(chars);
            Player loaded = again.load(1, p.guid, mgr);
            assertEquals(unspent, loaded.heroStats.unspent());
            assertEquals(1, loaded.heroStats.spent(HeroStatAllocation.STR));
            assertEquals(str, loaded.getInt(UpdateFields.UNIT_FIELD_STAT0));
        }
    }

    @Test
    void loadWhenHeroStatsRowMissingShouldBackfillUnspent() throws Exception {
        String url = "jdbc:h2:mem:hero_backfill_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "hero-backfill-test")) {
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE characters (
                          guid INT PRIMARY KEY, account INT, name VARCHAR(12), race INT, class INT, gender INT,
                          level INT, xp INT, money INT, playerBytes INT, playerBytes2 INT, playerFlags INT,
                          position_x FLOAT, position_y FLOAT, position_z FLOAT, map INT, dungeon_difficulty INT,
                          orientation FLOAT, online INT, cinematic INT, totaltime INT, leveltime INT,
                          logout_time BIGINT, is_logout_resting INT, rest_bonus FLOAT, zone INT, at_login INT,
                          health INT, power1 INT, power2 INT, power3 INT, power4 INT, power5 INT,
                          watchedFaction BIGINT, actionBars TINYINT, deleteDate BIGINT)
                        """);
                st.execute("""
                        CREATE TABLE character_spell (
                          guid INT, spell INT, active TINYINT, disabled TINYINT,
                          PRIMARY KEY (guid, spell))
                        """);
                st.execute("""
                        CREATE TABLE character_skills (
                          guid INT, skill INT, `value` INT, `max` INT,
                          PRIMARY KEY (guid, skill))
                        """);
                st.execute("CREATE TABLE item_instance (guid INT PRIMARY KEY)");
                st.execute("""
                        CREATE TABLE character_action (
                          guid INT, button INT, action INT, type INT, PRIMARY KEY (guid, button))
                        """);
                st.execute("""
                        CREATE TABLE character_queststatus (
                          guid INT, quest INT, status INT, rewarded INT, explored INT, timer BIGINT,
                          mobcount1 INT, mobcount2 INT, mobcount3 INT, mobcount4 INT,
                          itemcount1 INT, itemcount2 INT, itemcount3 INT, itemcount4 INT,
                          PRIMARY KEY (guid, quest))
                        """);
                st.execute("""
                        CREATE TABLE character_spell_cooldown (
                          guid INT, SpellId INT, SpellExpireTime BIGINT, Category INT,
                          CategoryExpireTime BIGINT, ItemId INT, PRIMARY KEY (guid, SpellId))
                        """);
                st.execute("""
                        CREATE TABLE character_aura (
                          guid INT, caster_guid BIGINT, item_guid INT, spell INT,
                          stackcount INT, remaincharges INT,
                          basepoints0 INT, basepoints1 INT, basepoints2 INT,
                          periodictime0 INT, periodictime1 INT, periodictime2 INT,
                          maxduration INT, remaintime INT, effIndexMask INT,
                          PRIMARY KEY (guid, caster_guid, item_guid, spell))
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(null, null);
            CharacterStore store = new CharacterStore(chars);
            Player p = store.create(1, "HeroOld", 1, ClasslessConfig.CLASS_CLASSLESS,
                    0, 1, 1, 1, 1, 0, mgr);
            p.giveXp(400, null);
            store.save(p);
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("DELETE FROM character_hero_stats");
            }
            CharacterStore again = new CharacterStore(chars);
            Player loaded = again.load(1, p.guid, mgr);
            assertEquals(HeroStatAllocation.pointsForGain(LevelStats.defaults(), 1, 1, 2),
                    loaded.heroStats.unspent());
            assertEquals(0, loaded.heroStats.spent(HeroStatAllocation.STR));
        }
    }
}
