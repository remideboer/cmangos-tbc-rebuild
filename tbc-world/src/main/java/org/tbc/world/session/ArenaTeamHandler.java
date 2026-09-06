package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.ArenaTeam;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** ArenaTeamHandler.cpp — inspect / invite. */
public final class ArenaTeamHandler {
    private ArenaTeamHandler() {}

    /** ERR_ARENA_TEAM_PLAYER_NOT_FOUND_S / TARGET_TOO_LOW / … from battleground.md. */
    static final int ERR_ARENA_TEAM_PLAYER_NOT_FOUND_S = 0x0B;
    static final int ERR_ARENA_TEAM_TARGET_TOO_LOW_S = 0x15;
    static final int ERR_ARENA_TEAM_PLAYER_NOT_IN_TEAM = 9;
    static final int ERR_ALREADY_IN_ARENA_TEAM_S = 3;
    static final int ERR_ALREADY_INVITED_TO_ARENA_TEAM_S = 5;
    static final int ERR_ARENA_TEAM_NOT_ALLIED = 0x0C;
    static final int ERR_ARENA_TEAM_TOO_MANY_MEMBERS_S = 0x16;
    static final int ERR_ARENA_TEAM_CREATE_S = 0;
    static final int ERR_ARENA_TEAM_INVITE_SS = 1;
    static final int ERR_ALREADY_IN_ARENA_TEAM = 2;
    static final int ERR_ARENA_TEAM_INTERNAL = 1;
    static final int ERR_ARENA_TEAM_QUIT_S = 3;
    static final int ERR_ARENA_TEAM_LEADER_LEAVE_S = 8;
    /** ArenaTeamEvents ERR_ARENA_TEAM_JOIN_SS / LEAVE_SS. */
    static final int EVENT_JOIN = 3;
    static final int EVENT_LEAVE = 4;
    static final int EVENT_REMOVE = 5;
    static final int EVENT_DISBANDED = 8;
    static final int ERR_ARENA_TEAM_PERMISSIONS = 8;
    /** TBC CONFIG_UINT32_MAX_PLAYER_LEVEL. */
    static final int MAX_PLAYER_LEVEL = 70;

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

    /** HandleArenaTeamInviteOpcode → SMSG_ARENA_TEAM_INVITE. */
    public static void invite(WorldSession s, World world, WowBuffer in) {
        int teamId = in.remaining() >= 4 ? in.getU32() : 0;
        String invitedName = in.remaining() > 0 ? in.getCString() : "";
        Player invitee = invitedName.isEmpty() ? null : world.playerByName(invitedName);
        if (invitee == null) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, "", invitedName, ERR_ARENA_TEAM_PLAYER_NOT_FOUND_S);
            return;
        }
        if (invitee.level < MAX_PLAYER_LEVEL) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, "", invitee.name, ERR_ARENA_TEAM_TARGET_TOO_LOW_S);
            return;
        }
        ArenaTeam team = world.objectMgr.arenaTeams.get(teamId);
        if (team == null) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, "", "", ERR_ARENA_TEAM_PLAYER_NOT_IN_TEAM);
            return;
        }
        Player inviter = s.player();
        if (invitee.team != inviter.team) {
            sendCommandResult(s, ERR_ARENA_TEAM_INVITE_SS, "", "", ERR_ARENA_TEAM_NOT_ALLIED);
            return;
        }
        if (arenaTeamId(invitee, team.slot) != 0) {
            sendCommandResult(s, ERR_ARENA_TEAM_INVITE_SS, "", invitee.name, ERR_ALREADY_IN_ARENA_TEAM_S);
            return;
        }
        if (invitee.arenaTeamIdInvited != 0) {
            sendCommandResult(s, ERR_ARENA_TEAM_INVITE_SS, "", invitee.name, ERR_ALREADY_INVITED_TO_ARENA_TEAM_S);
            return;
        }
        if (team.members.size() >= team.maxMembers()) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, team.name, "", ERR_ARENA_TEAM_TOO_MANY_MEMBERS_S);
            return;
        }
        invitee.arenaTeamIdInvited = team.id;
        WowBuffer data = new WowBuffer(64);
        data.putCString(inviter.name);
        data.putCString(team.name);
        if (invitee.session != null) {
            invitee.session.send(Opcodes.SMSG_ARENA_TEAM_INVITE, data.array());
        }
    }

    /** HandleArenaTeamAcceptOpcode — AddMember + JOIN event. */
    public static void accept(WorldSession s, World world) {
        Player p = s.player();
        ArenaTeam at = world.objectMgr.arenaTeams.get(p.arenaTeamIdInvited);
        if (at == null) {
            return;
        }
        if (arenaTeamId(p, at.slot) != 0) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, "", "", ERR_ALREADY_IN_ARENA_TEAM);
            return;
        }
        if (!addMember(world, at, p)) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, "", "", ERR_ARENA_TEAM_INTERNAL);
            return;
        }
        broadcastEvent(world, at, EVENT_JOIN, p.guid, p.name, at.name, null);
    }

    /** HandleArenaTeamLeaveOpcode — DelMember + LEAVE event (non-captain). */
    public static void leave(WorldSession s, World world, WowBuffer in) {
        int teamId = in.remaining() >= 4 ? in.getU32() : 0;
        ArenaTeam at = world.objectMgr.arenaTeams.get(teamId);
        if (at == null) {
            return;
        }
        Player p = s.player();
        if (p.guid == at.captainGuid && at.members.size() > 1) {
            sendCommandResult(s, ERR_ARENA_TEAM_QUIT_S, "", "", ERR_ARENA_TEAM_LEADER_LEAVE_S);
            return;
        }
        if (p.guid == at.captainGuid) {
            disband(world, at, s);
            return;
        }
        delMember(world, at, p.guid);
        broadcastEvent(world, at, EVENT_LEAVE, p.guid, p.name, at.name, null);
        sendCommandResult(s, ERR_ARENA_TEAM_QUIT_S, at.name, "", 0);
    }

    /** HandleArenaTeamRemoveOpcode — captain kicks member + REMOVE event. */
    public static void remove(WorldSession s, World world, WowBuffer in) {
        int teamId = in.remaining() >= 4 ? in.getU32() : 0;
        String name = in.remaining() > 0 ? in.getCString() : "";
        ArenaTeam at = world.objectMgr.arenaTeams.get(teamId);
        if (at == null) {
            return;
        }
        Player captain = s.player();
        if (at.captainGuid != captain.guid) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, "", "", ERR_ARENA_TEAM_PERMISSIONS);
            return;
        }
        Player target = name.isEmpty() ? null : world.playerByName(name);
        if (target == null || !at.members.containsKey(target.guid)) {
            sendCommandResult(s, ERR_ARENA_TEAM_CREATE_S, "", name, ERR_ARENA_TEAM_PLAYER_NOT_FOUND_S);
            return;
        }
        if (at.captainGuid == target.guid) {
            sendCommandResult(s, ERR_ARENA_TEAM_QUIT_S, "", "", ERR_ARENA_TEAM_LEADER_LEAVE_S);
            return;
        }
        delMember(world, at, target.guid);
        broadcastEvent(world, at, EVENT_REMOVE, 0, name, at.name, captain.name);
    }

    /** HandleArenaTeamDisbandOpcode — captain Disband + DISBANDED event. */
    public static void disbandOpcode(WorldSession s, World world, WowBuffer in) {
        int teamId = in.remaining() >= 4 ? in.getU32() : 0;
        ArenaTeam at = world.objectMgr.arenaTeams.get(teamId);
        if (at == null) {
            return;
        }
        if (at.captainGuid != s.player().guid) {
            return;
        }
        disband(world, at, s);
    }

    static void delMember(World world, ArenaTeam at, long guid) {
        at.members.remove(guid);
        Player player = world.playerByGuid(guid);
        if (player != null) {
            if (player.session != null) {
                sendCommandResult(player.session, ERR_ARENA_TEAM_QUIT_S, at.name, "", 0);
            }
            clearArenaTeam(player, at.slot);
        }
    }

    static void clearArenaTeam(Player pl, int slot) {
        setInArenaTeam(pl, 0, slot);
    }

    static void disband(World world, ArenaTeam at, WorldSession session) {
        if (session != null) {
            broadcastEvent(world, at, EVENT_DISBANDED, 0, session.player().name, at.name, null);
        }
        while (!at.members.isEmpty()) {
            Long g = at.members.keySet().iterator().next();
            delMember(world, at, g);
        }
        world.objectMgr.arenaTeams.remove(at.id);
    }

    static boolean addMember(World world, ArenaTeam at, Player pl) {
        if (at.members.size() >= at.maxMembers()) {
            return false;
        }
        if (arenaTeamId(pl, at.slot) != 0) {
            return false;
        }
        ArenaTeam.Member mem = new ArenaTeam.Member();
        mem.personalRating = 0;
        at.members.put(pl.guid, mem);
        setInArenaTeam(pl, at.id, at.slot);
        pl.arenaTeamIdInvited = 0;
        return true;
    }

    static void setInArenaTeam(Player pl, int teamId, int slot) {
        switch (slot) {
            case 0 -> pl.arenaTeam = teamId;
            case 1 -> pl.arenaTeamId3 = teamId;
            case 2 -> pl.arenaTeamId5 = teamId;
            default -> {
            }
        }
    }

    static void broadcastEvent(World world, ArenaTeam at, int event, long guid, String str1, String str2, String str3) {
        int strCount = str3 != null ? 3 : (str2 != null ? 2 : (str1 != null ? 1 : 0));
        WowBuffer data = new WowBuffer(96);
        data.putU8(event);
        data.putU8(strCount);
        if (str1 != null) {
            data.putCString(str1);
        }
        if (str2 != null) {
            data.putCString(str2);
        }
        if (str3 != null) {
            data.putCString(str3);
        }
        if (guid != 0) {
            data.putU64(guid);
        }
        byte[] payload = data.array();
        for (Long memberGuid : at.members.keySet()) {
            Player m = world.playerByGuid(memberGuid);
            if (m != null && m.session != null) {
                m.session.send(Opcodes.SMSG_ARENA_TEAM_EVENT, payload);
            }
        }
    }

    static void sendCommandResult(WorldSession s, int action, String team, String player, int errorId) {
        WowBuffer data = new WowBuffer(64);
        data.putU32(action);
        data.putCString(team == null ? "" : team);
        data.putCString(player == null ? "" : player);
        data.putU32(errorId);
        s.send(Opcodes.SMSG_ARENA_TEAM_COMMAND_RESULT, data.array());
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
