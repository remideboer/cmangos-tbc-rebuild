package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;

/** Guild create and roster. Layout: spec/03-protocol/packets/guild.md */
public final class GuildHandler {
    public static final int GR_RIGHT_EMPTY = 0x40;
    public static final int GUILD_BANK_MAX_TABS = 6;

    private GuildHandler() {}

    public static void create(WorldSession s, WowBuffer in) {
        Player p = s.player();
        p.guildId = 1;
        p.guildLeader = true;
        p.guildName = in.remaining() > 0 ? in.getCString() : "";
        roster(s, p);
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
}
