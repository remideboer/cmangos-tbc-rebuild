package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.spell.SpellEngine;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL07-017 — character_aura survives save / fresh CharacterStore load (process restart).
 */
class CharacterStoreAuraTest {
    private static final int FROST_ARMOR = SpellEngine.FROST_ARMOR;
    private static final int REMAIN_MS = 1_200_000;

    @Test
    void saveWhenFrostArmorShouldReloadFromSql() throws Exception {
        String url = "jdbc:h2:mem:aura_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "aura-test")) {
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
            Player p = store.create(1, "Buffed", 1, 8, 0, 1, 1, 1, 1, 0, mgr);
            long nowMs = System.currentTimeMillis();
            p.auras.add(new Unit.Aura(FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                    nowMs + REMAIN_MS, 0, 0, p.guid));
            store.save(p);

            CharacterStore again = new CharacterStore(chars);
            Player loaded = again.load(1, p.guid, mgr);
            assertTrue(loaded.hasAura(FROST_ARMOR), "character_aura row reloaded");
            Unit.Aura a = loaded.auras.stream().filter(x -> x.spellId() == FROST_ARMOR).findFirst().orElseThrow();
            assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, a.durationMs());
            assertTrue(a.expireAtMs() > System.currentTimeMillis(), "remaintime still in the future");
            long remain = a.expireAtMs() - System.currentTimeMillis();
            assertTrue(remain > REMAIN_MS - 5_000 && remain <= REMAIN_MS + 5_000,
                    "remain ≈ " + REMAIN_MS + " was " + remain);
        }
    }

    /**
     * Timed holder with expireAtMs 0 must not be saved as permanent (remaintime −1) —
     * that freezes the buff at 0s forever on the client.
     */
    @Test
    void saveWhenTimedAuraMissingExpireShouldReloadWithDurationRemain() throws Exception {
        String url = "jdbc:h2:mem:aura_heal_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "aura-heal-test")) {
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
            Player p = store.create(1, "Timed", 1, 8, 0, 1, 1, 1, 1, 0, mgr);
            p.auras.add(new Unit.Aura(FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                    0L, 0, 0, p.guid));
            store.save(p);

            CharacterStore again = new CharacterStore(chars);
            Player loaded = again.load(1, p.guid, mgr);
            Unit.Aura a = loaded.auras.stream().filter(x -> x.spellId() == FROST_ARMOR).findFirst().orElseThrow();
            assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, a.durationMs());
            assertTrue(a.expireAtMs() > System.currentTimeMillis(),
                    "must not reload as permanent expireAtMs=0");
            long remain = a.expireAtMs() - System.currentTimeMillis();
            assertTrue(remain > SpellEngine.FROST_ARMOR_DURATION_MS - 5_000
                            && remain <= SpellEngine.FROST_ARMOR_DURATION_MS + 5_000,
                    "remain ≈ maxduration was " + remain);
        }
    }
}
