package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Item;
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
}
