package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** npc_trainer, trainer meta, spell_chain and BaseLevel loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class TrainerLoaderTest {
    @Test
    void loadWhenTrainerTablesPresentShouldIndexSpellsMetaChainsAndBaseLevels() throws Exception {
        String url = "jdbc:h2:mem:trainer_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "trainer-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE npc_trainer (entry INT, spell INT, spellcost INT, reqskill INT, "
                        + "reqskillvalue INT, reqlevel INT, ReqAbility1 INT, ReqAbility2 INT, ReqAbility3 INT)");
                st.execute("INSERT INTO npc_trainer VALUES (" + Content.NPC_LLANE_BESHERE + ", "
                        + Content.SPELL_BATTLE_SHOUT_RANK2 + ", 500, 0, 0, 12, " + Content.SPELL_BATTLE_SHOUT
                        + ", NULL, 0)");
                st.execute("INSERT INTO npc_trainer VALUES (" + Content.NPC_LLANE_BESHERE + ", 0, 0, 0, 0, 0, 0, 0, 0)");
                st.execute("CREATE TABLE npc_trainer_template (entry INT, spell INT, spellcost INT, reqskill INT, "
                        + "reqskillvalue INT, reqlevel INT, ReqAbility1 INT, ReqAbility2 INT, ReqAbility3 INT)");
                st.execute("INSERT INTO npc_trainer_template VALUES (7, " + Content.SPELL_APPRENTICE_BLACKSMITH
                        + ", 10, 0, 0, 5, 0, 0, 0)");
                st.execute("CREATE TABLE creature_template (Entry INT, TrainerType INT, TrainerClass INT, "
                        + "TrainerTemplateId INT)");
                st.execute("INSERT INTO creature_template VALUES (" + Content.NPC_LLANE_BESHERE + ", 0, 1, 7)");
                st.execute("INSERT INTO creature_template VALUES (6, 0, 0, 0)");
                st.execute("CREATE TABLE spell_chain (spell_id INT, prev_spell INT, first_spell INT, `rank` INT, "
                        + "req_spell INT)");
                st.execute("INSERT INTO spell_chain VALUES (" + Content.SPELL_BATTLE_SHOUT_RANK2 + ", "
                        + Content.SPELL_BATTLE_SHOUT + ", " + Content.SPELL_BATTLE_SHOUT + ", 2, 0)");
                st.execute("CREATE TABLE spell_template (Id INT, BaseLevel INT)");
                st.execute("INSERT INTO spell_template VALUES (" + Content.SPELL_BATTLE_SHOUT_RANK2 + ", 12)");
                st.execute("INSERT INTO spell_template VALUES (" + Content.SPELL_BATTLE_SHOUT + ", 0)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                TrainerLoader.load(m, c);
            }
            assertEquals(1, m.trainerSpells.get(Content.NPC_LLANE_BESHERE).size(), "spell 0 rows are skipped");
            ObjectMgr.TrainerSpell shout = m.trainerSpells.get(Content.NPC_LLANE_BESHERE).get(0);
            assertEquals(Content.SPELL_BATTLE_SHOUT, shout.reqAbility0());
            assertEquals(0, shout.reqAbility1(), "NULL ReqAbility reads as 0");
            assertTrue(m.trainerTemplateSpells.get(7).get(0).primaryProfessionFirstRank());
            assertEquals(1, m.trainerClass.get(Content.NPC_LLANE_BESHERE));
            assertFalse(m.trainerClass.containsKey(6), "TrainerClass 0 is not stored");
            assertEquals(7, m.trainerTemplateId.get(Content.NPC_LLANE_BESHERE));
            assertEquals(2, m.spellsForTrainer(Content.NPC_LLANE_BESHERE).size(), "direct plus template rows merge");
            assertEquals(2, m.spellChain.get(Content.SPELL_BATTLE_SHOUT_RANK2).rank());
            assertEquals(12, m.spellBaseLevel.get(Content.SPELL_BATTLE_SHOUT_RANK2));
            assertFalse(m.spellBaseLevel.containsKey(Content.SPELL_BATTLE_SHOUT));
        }
    }

    @Test
    void loadWhenNoTrainerTablesShouldLeaveMapsEmpty() throws Exception {
        String url = "jdbc:h2:mem:trainer_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "trainer-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                TrainerLoader.load(m, c);
            }
            assertTrue(m.trainerSpells.isEmpty());
            assertTrue(m.spellChain.isEmpty());
        }
    }
}
