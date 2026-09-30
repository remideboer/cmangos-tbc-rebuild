package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL14-016 — character_spell + character_skills survive save/load (trainer learn stick).
 */
class CharacterStoreSpellSkillTest {
    private static final int SPELL_BATTLE_SHOUT = 6673;
    private static final int SKILL_BLACKSMITHING = 164;

    @Test
    void saveWhenTrainedSpellAndSkillShouldReloadFromMemory() {
        CharacterStore store = new CharacterStore(null);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = store.create(1, "Trained", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
        p.spells.add(SPELL_BATTLE_SHOUT);
        p.learnSkill(SKILL_BLACKSMITHING, 1, 75, 1);
        store.save(p);
        Player loaded = store.load(1, p.guid, mgr);
        assertTrue(loaded.spells.contains(SPELL_BATTLE_SHOUT));
        assertTrue(loaded.hasSkill(SKILL_BLACKSMITHING));
        assertEquals(1, loaded.skillValue(SKILL_BLACKSMITHING));
        assertEquals(75, loaded.skillMax(SKILL_BLACKSMITHING));
        assertEquals(1, loaded.skillStep(SKILL_BLACKSMITHING));
    }

    @Test
    void saveWhenDatabaseConnectedShouldReloadSpellAndSkillFromSql() throws Exception {
        String url = "jdbc:h2:mem:spellskill_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "spell-skill-test")) {
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE characters (
                          guid INT PRIMARY KEY, account INT, name VARCHAR(12), race INT, class INT, gender INT,
                          level INT, xp INT, money INT, playerBytes INT, playerBytes2 INT, playerFlags INT,
                          position_x FLOAT, position_y FLOAT, position_z FLOAT, map INT, dungeon_difficulty INT,
                          orientation FLOAT, online INT, cinematic INT, totaltime INT, leveltime INT,
                          logout_time BIGINT, is_logout_resting INT, rest_bonus FLOAT, zone INT, at_login INT,
                          health INT, power1 INT, power2 INT, power3 INT, power4 INT, power5 INT,
                          watchedFaction BIGINT, deleteDate BIGINT)
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
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(null, null);
            CharacterStore store = new CharacterStore(chars);
            Player p = store.create(1, "Sqltrain", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
            p.spells.add(SPELL_BATTLE_SHOUT);
            p.learnSkill(SKILL_BLACKSMITHING, 1, 75, 1);
            store.save(p);
            CharacterStore again = new CharacterStore(chars);
            Player loaded = again.load(1, p.guid, mgr);
            assertTrue(loaded.spells.contains(SPELL_BATTLE_SHOUT));
            assertTrue(loaded.hasSkill(SKILL_BLACKSMITHING));
            assertEquals(1, loaded.skillValue(SKILL_BLACKSMITHING));
            assertEquals(75, loaded.skillMax(SKILL_BLACKSMITHING));
        }
    }
}
