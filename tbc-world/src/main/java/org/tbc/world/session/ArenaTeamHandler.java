package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.ArenaTeam;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** ArenaTeamHandler.cpp — MSG_INSPECT_ARENA_TEAMS / roster path. */
public final class ArenaTeamHandler {
    private ArenaTeamHandler() {}

    /** HandleInspectArenaTeamsOpcode + ArenaTeam::InspectStats. */
    public static void inspect(WorldSession s, World world, WowBuffer in) {
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        Player target = world.playerByGuid(guid);
        if (target == null) {
            return;
        }
        for (int slot = 0; slot < 3; slot++) {
            int teamId = arenaTeamId(target, slot);
            if (teamId == 0) {
                continue;
            }
            ArenaTeam team = world.objectMgr.arenaTeams.get(teamId);
            if (team == null) {
                continue;
            }
            inspectStats(s, team, target.guid);
        }
    }

    static void inspectStats(WorldSession session, ArenaTeam team, long guid) {
        ArenaTeam.Member member = team.members.get(guid);
        if (member == null) {
            return;
        }
        WowBuffer data = new WowBuffer(8 + 1 + 4 * 6);
        data.putU64(guid);
        data.putU8(team.slot);
        data.putU32(team.id);
        data.putU32(team.rating);
        data.putU32(team.gamesSeason);
        data.putU32(team.winsSeason);
        data.putU32(member.gamesSeason);
        data.putU32(member.personalRating);
        session.send(Opcodes.MSG_INSPECT_ARENA_TEAMS, data.array());
    }

    /** Slot 0=2v2 uses {@link Player#arenaTeam}; 1/2 use Id3/Id5. */
    static int arenaTeamId(Player p, int slot) {
        return switch (slot) {
            case 0 -> p.arenaTeam != 0 ? p.arenaTeam : p.arenaTeamId2;
            case 1 -> p.arenaTeamId3;
            case 2 -> p.arenaTeamId5;
            default -> 0;
        };
    }
}
