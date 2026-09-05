package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Guild;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Guild create, invite, roster. Layout: spec/03-protocol/packets/guild.md */
public final class GuildHandler {
    public static final int GR_RIGHT_EMPTY = 0x40;
    /** Guild.h GR_RIGHT_INVITE. */
    public static final int GR_RIGHT_INVITE = 0x00000050;
    /** Guild.h GR_RIGHT_ALL (guild master). */
    public static final int GR_RIGHT_ALL = 0x000DF1FF;
    public static final int GUILD_BANK_MAX_TABS = 6;
    public static final int GUILD_CREATE_S = 0;
    public static final int GUILD_INVITE_S = 1;
    public static final int ERR_ALREADY_IN_GUILD_S = 0x03;
    public static final int ERR_ALREADY_INVITED_TO_GUILD_S = 0x05;
    public static final int ERR_GUILD_PERMISSIONS = 0x08;
    public static final int ERR_GUILD_PLAYER_NOT_IN_GUILD = 0x09;
    public static final int ERR_GUILD_PLAYER_NOT_FOUND_S = 0x0B;
    public static final int ERR_GUILD_NOT_ALLIED = 0x0C;
    public static final int GE_JOINED = 0x03;

    private GuildHandler() {}

    public static void create(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (p.guildId != 0) {
            return;
        }
        String name = in.remaining() > 0 ? in.getCString() : "";
        Guild g = new Guild();
        g.id = world.objectMgr.nextGuildId.getAndIncrement();
        g.name = name;
        g.leaderGuid = p.guid;
        g.members.add(p.guid);
        world.objectMgr.guilds.put(g.id, g);
        p.guildId = g.id;
        p.guildLeader = true;
        p.guildName = name;
        p.guildRankRights = GR_RIGHT_ALL;
        roster(s, p);
    }

    public static void invite(WorldSession s, World world, WowBuffer in) {
        String name = in.remaining() > 0 ? in.getCString() : "";
        Player t = world.playerByName(name);
        if (t == null || t.session == null) {
            commandResult(s, GUILD_INVITE_S, name, ERR_GUILD_PLAYER_NOT_FOUND_S);
            return;
        }
        Player p = s.player();
        Guild g = world.objectMgr.guilds.get(p.guildId);
        if (g == null) {
            commandResult(s, GUILD_CREATE_S, "", ERR_GUILD_PLAYER_NOT_IN_GUILD);
            return;
        }
        if (p.team != t.team) {
            commandResult(s, GUILD_INVITE_S, name, ERR_GUILD_NOT_ALLIED);
            return;
        }
        if (t.guildId != 0) {
            commandResult(s, GUILD_INVITE_S, t.name, ERR_ALREADY_IN_GUILD_S);
            return;
        }
        if (t.guildIdInvited != 0) {
            commandResult(s, GUILD_INVITE_S, t.name, ERR_ALREADY_INVITED_TO_GUILD_S);
            return;
        }
        if ((p.guildRankRights & GR_RIGHT_INVITE) != GR_RIGHT_INVITE) {
            commandResult(s, GUILD_INVITE_S, "", ERR_GUILD_PERMISSIONS);
            return;
        }
        t.guildIdInvited = g.id;
        WowBuffer inv = new WowBuffer(32);
        inv.putCString(p.name);
        inv.putCString(g.name);
        t.session.send(Opcodes.SMSG_GUILD_INVITE, inv.array());
    }

    public static void accept(WorldSession s, World world) {
        Player p = s.player();
        if (p.guildId != 0) {
            return;
        }
        Guild g = world.objectMgr.guilds.get(p.guildIdInvited);
        if (g == null) {
            return;
        }
        Player leader = world.playerByGuid(g.leaderGuid);
        if (leader != null && leader.team != p.team) {
            return;
        }
        p.guildIdInvited = 0;
        p.guildId = g.id;
        p.guildName = g.name;
        p.guildLeader = false;
        p.guildRankRights = GR_RIGHT_EMPTY;
        g.members.add(p.guid);
        WowBuffer ev = new WowBuffer(32);
        ev.putU8(GE_JOINED);
        ev.putU8(1);
        ev.putCString(p.name);
        ev.putU64(p.guid);
        byte[] payload = ev.array();
        for (long guid : g.members) {
            Player m = world.playerByGuid(guid);
            if (m != null && m.session != null) {
                m.session.send(Opcodes.SMSG_GUILD_EVENT, payload);
            }
        }
    }

    static void commandResult(WorldSession s, int type, String name, int result) {
        WowBuffer b = new WowBuffer(16);
        b.putU32(type);
        b.putCString(name == null ? "" : name);
        b.putU32(result);
        s.send(Opcodes.SMSG_GUILD_COMMAND_RESULT, b.array());
    }

    public static void roster(WorldSession s, Player p) {
        WowBuffer r = new WowBuffer(160);
        r.putU32(1);
        r.putCString("");
        r.putCString("");
        r.putU32(1);
        r.putU32(GR_RIGHT_EMPTY);
        r.putU32(0);
        for (int t = 0; t < GUILD_BANK_MAX_TABS; t++) {
            r.putU32(0);
            r.putU32(0);
        }
        r.putU64(p.guid);
        r.putU8(p.session != null ? 1 : 0);
        r.putCString(p.name);
        r.putU32(0);
        r.putU8(p.level);
        r.putU8(p.clazz);
        r.putU8(p.gender);
        r.putU32(p.zoneId);
        r.putCString("");
        r.putCString("");
        s.send(Opcodes.SMSG_GUILD_ROSTER, r.array());
    }

    public static void bankerActivate(WorldSession s, WowBuffer in) {
        if (in.remaining() >= 8) {
            in.getU64();
        }
        if (in.remaining() > 0) {
            in.getU8();
        }
        Player p = s.player();
        if (p.guildId == 0) {
            return;
        }
        WowBuffer list = new WowBuffer(32);
        list.putU64(0);
        list.putU8(0);
        list.putU32(0);
        list.putU8(1);
        list.putU8(1);
        list.putCString("Tab");
        list.putCString("");
        list.putU8(0);
        s.send(Opcodes.SMSG_GUILD_BANK_LIST, list.array());
    }

    public static void swapItems(WorldSession s, WowBuffer in) {
        if (in.remaining() < 9) {
            return;
        }
        in.getU64();
        int bankToBank = in.getU8();
        if (bankToBank != 0 || in.remaining() < 14) {
            return;
        }
        int tab = in.getU8();
        int slot = in.getU8();
        in.getU32();
        int autoStore = in.getU8();
        if (autoStore != 0) {
            return;
        }
        int bag = in.getU8();
        int playerSlot = in.getU8();
        int toChar = in.getU8();
        Player p = s.player();
        if (toChar == 0) {
            Item it = p.itemAt(bag, playerSlot);
            if (it == null) {
                return;
            }
            p.items.remove((int) it.guid);
            p.guildBankItem = it;
            sendBankSlot(s, tab, slot, it.entry);
        } else {
            Item it = p.guildBankItem;
            if (it == null) {
                return;
            }
            p.guildBankItem = null;
            it.bag = bag;
            it.slot = playerSlot;
            p.items.put((int) it.guid, it);
            sendBankSlot(s, tab, slot, 0);
        }
    }

    static void sendBankSlot(WorldSession s, int tab, int slot, int entry) {
        WowBuffer list = new WowBuffer(48);
        list.putU64(0);
        list.putU8(tab);
        list.putU32(0);
        list.putU8(0);
        list.putU8(1);
        list.putU8(slot);
        list.putU32(entry);
        if (entry != 0) {
            list.putU32(0);
            list.putU8(1);
            list.putU32(0);
            list.putU8(0);
            list.putU8(0);
        }
        s.send(Opcodes.SMSG_GUILD_BANK_LIST, list.array());
    }

    public static final int TAB_PRICE = 100000;

    public static void buyTab(WorldSession s, WowBuffer in) {
        if (in.remaining() >= 8) {
            in.getU64();
        }
        Player p = s.player();
        if (p.guildId == 0) {
            return;
        }
        p.money = Math.max(0, p.money - TAB_PRICE);
        p.guildBankTabs++;
        WowBuffer perm = new WowBuffer(80);
        perm.putU32(0);
        perm.putU32(GR_RIGHT_EMPTY);
        perm.putU32(0);
        perm.putU8(p.guildBankTabs);
        for (int t = 0; t < GUILD_BANK_MAX_TABS; t++) {
            perm.putU32(0);
            perm.putU32(0);
        }
        s.send(Opcodes.MSG_GUILD_PERMISSIONS, perm.array());
    }
}
