package org.tbc.world.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.spell.SpellEngine;

/** character_aura rows (CMaNGOS Player::_SaveAuras / _LoadAuras); see docs/aura-persist-on-login.md. Split out of CharacterStore; CharacterStore.save/load stay the public surface. */
final class AuraPersist {

    private AuraPersist() {
    }
    /**
     * Player::_SaveAuras — DELETE-all then INSERT. Skip passive Battle Stance 2457 and expired
     * timed holders. remaintime/maxduration are milliseconds; permanent uses remaintime −1.
     */
    static void write(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM character_aura WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO character_aura (guid, caster_guid, item_guid, spell, stackcount, remaincharges, "
                        + "basepoints0, basepoints1, basepoints2, periodictime0, periodictime1, periodictime2, "
                        + "maxduration, remaintime, effIndexMask) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        long nowMs = System.currentTimeMillis();
        for (Unit.Aura a : p.auras) {
            if (a.spellId() <= 0 || a.spellId() == SpellEngine.SPELL_BATTLE_STANCE) {
                continue;
            }
            int remaintime;
            if (a.expireAtMs() <= 0) {
                // Timed holders must not be written as permanent (−1) — client shows 0s forever.
                remaintime = a.durationMs() > 0 ? a.durationMs() : -1;
            } else {
                long left = a.expireAtMs() - nowMs;
                if (left <= 0) {
                    continue;
                }
                remaintime = (int) Math.min(Integer.MAX_VALUE, left);
            }
            long caster = a.casterGuid() != 0 ? a.casterGuid() : p.guid;
            ins.setInt(1, Guid.low(p.guid));
            ins.setLong(2, caster);
            ins.setInt(3, 0);
            ins.setInt(4, a.spellId());
            ins.setInt(5, Math.max(1, a.stacks()));
            ins.setInt(6, 0);
            ins.setInt(7, 0);
            ins.setInt(8, 0);
            ins.setInt(9, 0);
            ins.setInt(10, a.amplitudeMs());
            ins.setInt(11, 0);
            ins.setInt(12, 0);
            ins.setInt(13, a.durationMs());
            ins.setInt(14, remaintime);
            ins.setInt(15, 1);
            ins.addBatch();
        }
        ins.executeBatch();
    }

    /**
     * Player::_LoadAuras — restore holders; positive buffs keep full remaintime (no offline tick).
     * expireAtMs = now + remaintime when remaintime ≥ 0; permanent when remaintime &lt; 0.
     */
    static void load(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT caster_guid, item_guid, spell, stackcount, remaincharges, "
                        + "basepoints0, basepoints1, basepoints2, periodictime0, periodictime1, periodictime2, "
                        + "maxduration, remaintime, effIndexMask FROM character_aura WHERE guid = ?");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        long nowMs = System.currentTimeMillis();
        while (rs.next()) {
            int spellId = rs.getInt("spell");
            if (spellId <= 0 || spellId == SpellEngine.SPELL_BATTLE_STANCE) {
                continue;
            }
            int stacks = Math.max(1, rs.getInt("stackcount"));
            int maxduration = rs.getInt("maxduration");
            int remaintime = rs.getInt("remaintime");
            int amplitude = rs.getInt("periodictime0");
            long caster = rs.getLong("caster_guid");
            long expireAt;
            if (remaintime < 0) {
                // Heal bad rows: timed maxduration saved as permanent remaintime −1.
                expireAt = maxduration > 0 ? nowMs + maxduration : 0;
            } else {
                expireAt = nowMs + remaintime;
            }
            long nextTick = amplitude > 0 && expireAt > 0 ? nowMs + amplitude : 0;
            p.auras.add(new Unit.Aura(spellId, maxduration, stacks, 0, expireAt, amplitude, nextTick, caster));
        }
    }
}
