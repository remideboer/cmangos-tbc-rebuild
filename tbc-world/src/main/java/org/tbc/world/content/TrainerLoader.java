package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.SpellChainNode;
import org.tbc.world.content.ObjectMgr.TrainerSpell;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL load of the trainer family: npc_trainer / npc_trainer_template, creature_template trainer meta,
 * spell_chain and spell_template BaseLevel. Carved from ObjectMgr in refactoring plan cycle 4.2.
 */
final class TrainerLoader {
    private static final Logger log = LoggerFactory.getLogger(TrainerLoader.class);
    private final ObjectMgr m;

    private TrainerLoader(ObjectMgr m) {
        this.m = m;
    }

    /** Same order and failure handling as ObjectMgr.load. */
    static void load(ObjectMgr m, Connection c) {
        TrainerLoader l = new TrainerLoader(m);
        try {
            l.loadTrainers(c);
        } catch (Exception e) {
            log.debug("npc_trainer load skipped: {}", e.getMessage());
        }
        try {
            l.loadTrainerMeta(c);
        } catch (Exception e) {
            log.debug("trainer meta load skipped: {}", e.getMessage());
        }
        try {
            l.loadSpellChains(c);
        } catch (Exception e) {
            log.debug("spell_chain load skipped: {}", e.getMessage());
        }
        try {
            l.loadSpellBaseLevels(c);
        } catch (Exception e) {
            log.debug("spell BaseLevel load skipped: {}", e.getMessage());
        }
    }
    /** CMaNGOS ObjectMgr::LoadTrainers — separate entry vs template maps (ids may collide). */
    private void loadTrainers(Connection c) throws Exception {
        loadTrainerTable(c, "npc_trainer", m.trainerSpells);
        loadTrainerTable(c, "npc_trainer_template", m.trainerTemplateSpells);
        log.info("loaded trainer spell lists for {} creature entries and {} templates",
                m.trainerSpells.size(), m.trainerTemplateSpells.size());
    }

    private void loadTrainerTable(Connection c, String table, Map<Integer, List<TrainerSpell>> into) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT entry, spell, spellcost, reqskill, reqskillvalue, reqlevel, "
                        + "ReqAbility1, ReqAbility2, ReqAbility3 FROM " + table);
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int entry = rs.getInt(1);
            int spell = rs.getInt(2);
            if (spell <= 0) {
                continue;
            }
            int cost = rs.getInt(3);
            int reqSkill = rs.getInt(4);
            int reqSkillValue = rs.getInt(5);
            int reqLevel = rs.getInt(6);
            int a0 = rs.getObject(7) == null ? 0 : rs.getInt(7);
            int a1 = rs.getObject(8) == null ? 0 : rs.getInt(8);
            int a2 = rs.getObject(9) == null ? 0 : rs.getInt(9);
            boolean firstProf = spell == Content.SPELL_APPRENTICE_BLACKSMITH;
            TrainerSpell row = new TrainerSpell(spell, cost, reqLevel, reqSkill, reqSkillValue, a0, a1, a2, firstProf);
            into.computeIfAbsent(entry, k -> new ArrayList<>()).add(row);
            n++;
        }
        log.info("loaded {} rows from {}", n, table);
    }

    /** creature_template TrainerType / TrainerClass / TrainerTemplateId (CMaNGOS Creature::IsTrainerOf). */
    private void loadTrainerMeta(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT Entry, TrainerType, TrainerClass, TrainerTemplateId FROM creature_template");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int entry = rs.getInt(1);
            m.trainerTypeByEntry.put(entry, rs.getInt(2));
            int clazz = rs.getInt(3);
            if (clazz != 0) {
                m.trainerClass.put(entry, clazz);
            }
            int tmpl = rs.getInt(4);
            if (tmpl != 0) {
                m.trainerTemplateId.put(entry, tmpl);
            }
            n++;
        }
        log.info("loaded trainer meta for {} creature_template rows", n);
    }

    /** CMaNGOS SpellMgr::LoadSpellChains. */
    private void loadSpellChains(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT spell_id, prev_spell, first_spell, `rank`, req_spell FROM spell_chain");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int id = rs.getInt(1);
            m.spellChain.put(id, new SpellChainNode(id, rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)));
            n++;
        }
        log.info("loaded {} spell_chain rows", n);
    }

    /** spell_template.BaseLevel for trainer reqLevel fallback when SQL reqlevel is 0. */
    private void loadSpellBaseLevels(Connection c) throws Exception {
        PreparedStatement ps = c.prepareStatement("SELECT Id, BaseLevel FROM spell_template WHERE BaseLevel > 0");
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            m.spellBaseLevel.put(rs.getInt(1), rs.getInt(2));
            n++;
        }
        log.info("loaded BaseLevel for {} spells", n);
    }
}
