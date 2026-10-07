package org.tbc.world.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Player;

/** character_spell, character_skills and character_spell_cooldown rows (CMaNGOS Player::_SaveSpells / _SaveSkills / _SaveSpellCooldowns and their loaders). Split out of CharacterStore; CharacterStore.save/load stay the public surface. */
final class SpellPersist {

    private SpellPersist() {
    }
    static void writeSpells(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM character_spell WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO character_spell (guid, spell, active, disabled) VALUES (?,?,1,0)");
        for (int spell : p.spells) {
            if (spell <= 0) {
                continue;
            }
            ins.setInt(1, Guid.low(p.guid));
            ins.setInt(2, spell);
            ins.addBatch();
        }
        ins.executeBatch();
    }

    static void writeSkills(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM character_skills WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO character_skills (guid, skill, `value`, `max`) VALUES (?,?,?,?)");
        for (int slot = 0; slot < 127; slot++) {
            int base = org.tbc.world.net.wow8606.UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
            int skill = p.getInt(base) & 0xFFFF;
            if (skill == 0) {
                continue;
            }
            int packed = p.getInt(base + 1);
            ins.setInt(1, Guid.low(p.guid));
            ins.setInt(2, skill);
            ins.setInt(3, packed & 0xFFFF);
            ins.setInt(4, (packed >>> 16) & 0xFFFF);
            ins.addBatch();
        }
        ins.executeBatch();
    }

    /** @return true if at least one spell row was loaded (replaces create defaults). */
    static boolean loadSpells(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT spell FROM character_spell WHERE guid = ? AND disabled = 0");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        java.util.ArrayList<Integer> loaded = new java.util.ArrayList<>();
        while (rs.next()) {
            loaded.add(rs.getInt(1));
        }
        if (loaded.isEmpty()) {
            return false;
        }
        p.spells.clear();
        p.spells.addAll(loaded);
        return true;
    }

    /** @return true if at least one skill row was loaded. */
    static boolean loadSkills(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT skill, `value`, `max` FROM character_skills WHERE guid = ?");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        boolean any = false;
        while (rs.next()) {
            any = true;
            int skill = rs.getInt(1);
            int value = rs.getInt(2);
            int max = rs.getInt(3);
            p.learnSkill(skill, value, max, 0);
        }
        return any;
    }

    /** Player::_LoadSpellCooldowns — SpellExpireTime is unix seconds. */
    static void loadSpellCooldowns(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT SpellId, SpellExpireTime, Category, CategoryExpireTime, ItemId FROM character_spell_cooldown WHERE guid = ?");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        long nowMs = System.currentTimeMillis();
        while (rs.next()) {
            int spellId = rs.getInt("SpellId");
            long expireMs = rs.getLong("SpellExpireTime") * 1000L;
            p.cooldowns.restoreSpell(spellId, expireMs, nowMs);
        }
    }

    /** Player::_SaveSpellCooldowns — DELETE-all then INSERT; expire as unix seconds. */
    static void writeSpellCooldowns(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM character_spell_cooldown WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO character_spell_cooldown (guid, SpellId, SpellExpireTime, Category, CategoryExpireTime, ItemId) VALUES (?,?,?,?,?,?)");
        long nowMs = System.currentTimeMillis();
        for (var e : p.cooldowns.unexpiredSpellExpireMs(nowMs).entrySet()) {
            ins.setInt(1, Guid.low(p.guid));
            ins.setInt(2, e.getKey());
            ins.setLong(3, e.getValue() / 1000L);
            ins.setInt(4, 0);
            ins.setLong(5, 0);
            ins.setInt(6, 0);
            ins.addBatch();
        }
        ins.executeBatch();
    }
}
