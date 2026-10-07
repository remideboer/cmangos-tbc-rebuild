package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.world.World;

/** Battleground port / leave / AFK report / honor inspect (BattleGroundHandler.cpp, battleground.md). */
public final class BattlegroundHandler {
    private BattlegroundHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_LEAVE_BATTLEFIELD, BattlegroundHandler::leaveBattlefield)
                .register(Opcodes.CMSG_BATTLEFIELD_PORT, BattlegroundHandler::battlefieldPort)
                .register(Opcodes.CMSG_REPORT_PVP_AFK, BattlegroundHandler::reportPvpAfk)
                .register(Opcodes.MSG_INSPECT_HONOR_STATS, (s, w, in) -> inspectHonorStats(s, in));
    }

    /** MiscHandler / BattleGroundHandler HandleLeaveBattlefieldOpcode (battleground.md). */
    public static void leaveBattlefield(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() >= 8) {
            long packedBg = in.getU64();
            int bgTypeId = (int) ((packedBg & 0x0000FFFFFFFF0000L) >> 16);
            if (bgTypeId >= 9) {
                return;
            }
        }
        if (p.mapId != 489 && p.mapId != 559 && p.mapId != 562 && p.mapId != 572) {
            return;
        }
        // STATUS_WAIT_LEAVE not modeled yet — combat always blocks leave (CMaNGOS).
        if (p.inCombat) {
            return;
        }
        if (!p.hasBgEntry) {
            return;
        }
        int map = p.bgEntryMap;
        float x = p.bgEntryX;
        float y = p.bgEntryY;
        float z = p.bgEntryZ;
        float o = p.bgEntryO;
        p.hasBgEntry = false;
        s.bgQueue = 0;
        world.teleport(p, map, x, y, z, o);
    }

    /** HandleBattlefieldPortOpcode — action 1 enters the queued BG; WSG also gets its init world states. */
    public static void battlefieldPort(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() > 0) {
            in.getU8();
        }
        if (in.remaining() > 0) {
            in.getU8();
        }
        if (in.remaining() >= 4) {
            in.getU32();
        }
        if (in.remaining() >= 2) {
            in.getU16();
        }
        int action = in.remaining() > 0 ? in.getU8() : 1;
        if (action == 1 && s.bgQueue != 0) {
            p.bgEntryMap = p.mapId;
            p.bgEntryX = p.x;
            p.bgEntryY = p.y;
            p.bgEntryZ = p.z;
            p.bgEntryO = p.o;
            p.hasBgEntry = true;
            world.teleport(p, s.bgQueue, 0, 0, 0, 0);
            if (s.bgQueue == 489) {
                WowBuffer ws = new WowBuffer(24);
                ws.putU32(489);
                ws.putU32(0);
                ws.putU32(0);
                ws.putU16(2);
                ws.putU32(PvpObjectives.WS_WSG_A);
                ws.putU32(1);
                ws.putU32(PvpObjectives.WS_WSG_H);
                ws.putU32(1);
                s.send(Opcodes.SMSG_INIT_WORLD_STATES, ws.array());
            }
        }
    }

    /** HandleReportPvPAFK — third distinct reporter applies the IDLE_AFK marker aura. */
    public static void reportPvpAfk(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        long target = in.remaining() >= 8 ? in.getU64() : 0;
        Player victim = target != 0 ? world.playerByGuid(target) : p;
        if (victim == null) {
            victim = p;
        }
        victim.afkReporterGuids.add(p.guid);
        victim.afkReports = victim.afkReporterGuids.size();
        if (victim.afkReporterGuids.size() >= 3) {
            victim.auras.add(new Unit.Aura(PvpObjectives.IDLE_AFK, 0, 1));
        }
    }

    /** MSG_INSPECT_HONOR_STATS echo with the inspecting player's own honor fields. */
    public static void inspectHonorStats(WorldSession s, WowBuffer in) {
        Player p = s.player();
        long g = in.remaining() >= 8 ? in.getU64() : p.guid;
        WowBuffer h = new WowBuffer(32);
        h.putU64(g);
        h.putU8(0);
        h.putU32(p.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_KILLS));
        h.putU32(p.honorToday);
        h.putU32(p.yesterdayContrib);
        h.putU32(p.honorPoints);
        s.send(Opcodes.MSG_INSPECT_HONOR_STATS, h.array());
    }
}
