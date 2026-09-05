package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guild;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Guild create, invite, promote, MOTD, roster. Layout: spec/03-protocol/packets/guild.md */
public final class GuildHandler {
    public static final int GR_RIGHT_EMPTY = 0x40;
    /** Guild.h GR_RIGHT_GCHATLISTEN / GCHATSPEAK (default Veteran–Initiate). */
    public static final int GR_RIGHT_GCHATLISTEN = 0x00000041;
    public static final int GR_RIGHT_GCHATSPEAK = 0x00000042;
    /** Guild.h GR_RIGHT_INVITE. */
    public static final int GR_RIGHT_INVITE = 0x00000050;
    /** Guild.h GR_RIGHT_PROMOTE. */
    public static final int GR_RIGHT_PROMOTE = 0x000000C0;
    /** Guild.h GR_RIGHT_SETMOTD. */
    public static final int GR_RIGHT_SETMOTD = 0x00001040;
    /** Guild.h GR_RIGHT_ALL (guild master). */
    public static final int GR_RIGHT_ALL = 0x000DF1FF;
    public static final int GUILD_BANK_MAX_TABS = 6;
    public static final int GUILD_CREATE_S = 0;
    public static final int GUILD_INVITE_S = 1;
    public static final int ERR_ALREADY_IN_GUILD_S = 0x03;
    public static final int ERR_ALREADY_INVITED_TO_GUILD_S = 0x05;
    public static final int ERR_GUILD_PERMISSIONS = 0x08;
    public static final int ERR_GUILD_PLAYER_NOT_IN_GUILD = 0x09;
    public static final int ERR_GUILD_NAME_INVALID = 0x06;
    public static final int ERR_GUILD_PLAYER_NOT_FOUND_S = 0x0B;
    public static final int ERR_GUILD_PLAYER_NOT_IN_GUILD_S = 0x0A;
    public static final int ERR_GUILD_NOT_ALLIED = 0x0C;
    public static final int ERR_GUILD_RANK_TOO_HIGH_S = 0x0D;
    public static final int GE_PROMOTION = 0x00;
    public static final int GE_MOTD = 0x02;
    public static final int GE_JOINED = 0x03;
    /** SharedDefines.h GOLD × 10. MSG_SAVE_GUILD_EMBLEM cost. */
    public static final int EMBLEM_COST = 100000;
    public static final int ERR_GUILDEMBLEM_SUCCESS = 0;
    public static final int ERR_GUILDEMBLEM_NOGUILD = 2;
    public static final int ERR_GUILDEMBLEM_NOTGUILDMASTER = 3;
    public static final int ERR_GUILDEMBLEM_NOTENOUGHMONEY = 4;
    public static final int ERR_GUILDEMBLEM_INVALIDVENDOR = 5;

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
        seedDefaultRanks(g);
        world.objectMgr.guilds.put(g.id, g);
        p.guildId = g.id;
        p.guildLeader = true;
        p.guildName = name;
        applyRank(p, g, 0);
        roster(s, p);
    }

    static void seedDefaultRanks(Guild g) {
        g.ranks.clear();
        g.ranks.add(new Guild.Rank("Guild Master", GR_RIGHT_ALL));
        g.ranks.add(new Guild.Rank("Officer", GR_RIGHT_ALL));
        int chat = GR_RIGHT_GCHATLISTEN | GR_RIGHT_GCHATSPEAK;
        g.ranks.add(new Guild.Rank("Veteran", chat));
        g.ranks.add(new Guild.Rank("Member", chat));
        g.ranks.add(new Guild.Rank("Initiate", chat));
    }

    static void applyRank(Player p, Guild g, int rankId) {
        p.guildRank = rankId;
        if (rankId >= 0 && rankId < g.ranks.size()) {
            p.guildRankRights = g.ranks.get(rankId).rights;
        }
    }

    static boolean hasRankRight(Guild g, int rankId, int right) {
        int rights = 0;
        if (rankId >= 0 && rankId < g.ranks.size()) {
            rights = g.ranks.get(rankId).rights;
        }
        return (rights & right) != GR_RIGHT_EMPTY;
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
        applyRank(p, g, Math.max(0, g.ranks.size() - 1));
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

    public static void promote(WorldSession s, World world, WowBuffer in) {
        String name = in.remaining() > 0 ? in.getCString() : "";
        if (name.isEmpty()) {
            return;
        }
        Player p = s.player();
        Guild g = world.objectMgr.guilds.get(p.guildId);
        if (g == null) {
            commandResult(s, GUILD_CREATE_S, "", ERR_GUILD_PLAYER_NOT_IN_GUILD);
            return;
        }
        if (!hasRankRight(g, p.guildRank, GR_RIGHT_PROMOTE)) {
            commandResult(s, GUILD_INVITE_S, "", ERR_GUILD_PERMISSIONS);
            return;
        }
        Player t = memberByName(world, g, name);
        if (t == null) {
            commandResult(s, GUILD_INVITE_S, name, ERR_GUILD_PLAYER_NOT_IN_GUILD_S);
            return;
        }
        if (t.guid == p.guid) {
            commandResult(s, GUILD_INVITE_S, "", ERR_GUILD_NAME_INVALID);
            return;
        }
        if (p.guildRank + 1 >= t.guildRank) {
            commandResult(s, GUILD_INVITE_S, name, ERR_GUILD_RANK_TOO_HIGH_S);
            return;
        }
        int newRankId = t.guildRank - 1;
        applyRank(t, g, newRankId);
        String rankName = newRankId >= 0 && newRankId < g.ranks.size()
                ? g.ranks.get(newRankId).name : "<unknown>";
        broadcastEvent(world, g, GE_PROMOTION, 0, p.name, t.name, rankName);
    }

    public static void motd(WorldSession s, World world, WowBuffer in) {
        String motd = in.remaining() > 0 ? in.getCString() : "";
        Player p = s.player();
        Guild g = world.objectMgr.guilds.get(p.guildId);
        if (g == null) {
            commandResult(s, GUILD_CREATE_S, "", ERR_GUILD_PLAYER_NOT_IN_GUILD);
            return;
        }
        if (!hasRankRight(g, p.guildRank, GR_RIGHT_SETMOTD)) {
            commandResult(s, GUILD_INVITE_S, "", ERR_GUILD_PERMISSIONS);
            return;
        }
        g.motd = motd;
        broadcastEvent(world, g, GE_MOTD, 0, motd);
    }

    public static void saveEmblem(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 28) {
            return;
        }
        long vendorGuid = in.getU64();
        int emblemStyle = in.getU32();
        int emblemColor = in.getU32();
        int borderStyle = in.getU32();
        int borderColor = in.getU32();
        int backgroundColor = in.getU32();
        Player p = s.player();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), vendorGuid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_TABARDDESIGNER) == 0) {
            sendEmblem(s, ERR_GUILDEMBLEM_INVALIDVENDOR);
            return;
        }
        Guild g = world.objectMgr.guilds.get(p.guildId);
        if (g == null) {
            sendEmblem(s, ERR_GUILDEMBLEM_NOGUILD);
            return;
        }
        if (g.leaderGuid != p.guid) {
            sendEmblem(s, ERR_GUILDEMBLEM_NOTGUILDMASTER);
            return;
        }
        if (p.money < EMBLEM_COST) {
            sendEmblem(s, ERR_GUILDEMBLEM_NOTENOUGHMONEY);
            return;
        }
        p.setMoney(p.money - EMBLEM_COST);
        g.emblemStyle = emblemStyle;
        g.emblemColor = emblemColor;
        g.borderStyle = borderStyle;
        g.borderColor = borderColor;
        g.backgroundColor = backgroundColor;
        sendEmblem(s, ERR_GUILDEMBLEM_SUCCESS);
        WowBuffer q = new WowBuffer(4);
        q.putU32(g.id);
        QueryHandler.guild(s, world, q);
    }

    static void sendEmblem(WorldSession s, int result) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(result);
        s.send(Opcodes.MSG_SAVE_GUILD_EMBLEM, b.array());
    }

    static Player memberByName(World world, Guild g, String name) {
        for (long guid : g.members) {
            Player m = world.playerByGuid(guid);
            if (m != null && m.name.equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    static void broadcastEvent(World world, Guild g, int event, long guid, String... strs) {
        WowBuffer ev = new WowBuffer(64);
        ev.putU8(event);
        ev.putU8(strs.length);
        for (String str : strs) {
            ev.putCString(str == null ? "" : str);
        }
        if (guid != 0) {
            ev.putU64(guid);
        }
        byte[] payload = ev.array();
        for (long memberGuid : g.members) {
            Player m = world.playerByGuid(memberGuid);
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

    public static void buyTab(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() >= 8) {
            in.getU64();
        }
        Player p = s.player();
        if (p.guildId == 0) {
            return;
        }
        p.setMoney(Math.max(0, p.money - TAB_PRICE));
        p.guildBankTabs++;
        Guild g = world.objectMgr.guilds.get(p.guildId);
        if (g != null) {
            g.purchasedTabs++;
        }
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

    public static void bankLogQuery(WorldSession s, World world, WowBuffer in) {
        int tabId = in.remaining() > 0 ? in.getU8() : 0;
        Guild g = world.objectMgr.guilds.get(s.player().guildId);
        if (g == null) {
            return;
        }
        if (tabId >= g.purchasedTabs && tabId != GUILD_BANK_MAX_TABS) {
            return;
        }
        WowBuffer out = new WowBuffer(4);
        out.putU8(tabId);
        out.putU8(0);
        s.send(Opcodes.MSG_GUILD_BANK_LOG_QUERY, out.array());
    }

    public static void queryBankText(WorldSession s, World world, WowBuffer in) {
        int tabId = in.remaining() > 0 ? in.getU8() : 0;
        Guild g = world.objectMgr.guilds.get(s.player().guildId);
        if (g == null || tabId < 0 || tabId >= g.purchasedTabs) {
            return;
        }
        sendBankText(world, g, tabId, s);
    }

    public static void setBankText(WorldSession s, World world, WowBuffer in) {
        int tabId = in.remaining() > 0 ? in.getU8() : 0;
        String text = in.remaining() > 0 ? in.getCString() : "";
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        Guild g = world.objectMgr.guilds.get(s.player().guildId);
        if (g == null || tabId < 0 || tabId >= g.purchasedTabs) {
            return;
        }
        if (text.equals(tabText(g, tabId))) {
            return;
        }
        g.tabTexts[tabId] = text;
        sendBankText(world, g, tabId, null);
    }

    static String tabText(Guild g, int tabId) {
        String t = g.tabTexts[tabId];
        return t == null ? "" : t;
    }

    static void sendBankText(World world, Guild g, int tabId, WorldSession only) {
        WowBuffer data = new WowBuffer(64);
        data.putU8(tabId);
        data.putCString(tabText(g, tabId));
        byte[] payload = data.array();
        if (only != null) {
            only.send(Opcodes.MSG_QUERY_GUILD_BANK_TEXT, payload);
            return;
        }
        for (long guid : g.members) {
            Player m = world.playerByGuid(guid);
            if (m != null && m.session != null) {
                m.session.send(Opcodes.MSG_QUERY_GUILD_BANK_TEXT, payload);
            }
        }
    }
}
