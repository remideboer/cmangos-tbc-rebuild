package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Group;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.world.World;

/** Remaining slice 10+ C2S that is not a dedicated WorldSession method. */
public final class LaterOpcodes {
    private LaterOpcodes() {}

    public static boolean handle(WorldSession s, World world, int opcode, WowBuffer in) {
        Player p = s.player();
        if (p == null) {
            return false;
        }
        if (opcode == Opcodes.CMSG_SWAP_INV_ITEM) {
            if (in.remaining() < 2) {
                return true;
            }
            int src = in.getU8();
            int dst = in.getU8();
            Item a = p.itemAt(0, src);
            Item b = p.itemAt(0, dst);
            if (a != null) {
                a.slot = dst;
            }
            if (b != null) {
                b.slot = src;
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_SWAP_ITEM) {
            if (in.remaining() < 4) {
                return true;
            }
            int db = in.getU8();
            int ds = in.getU8();
            int sb = in.getU8();
            int ss = in.getU8();
            Item a = p.itemAt(sb, ss);
            Item b = p.itemAt(db, ds);
            if (a != null) {
                a.bag = db;
                a.slot = ds;
            }
            if (b != null) {
                b.bag = sb;
                b.slot = ss;
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_RESET_INSTANCES) {
            Group g = p.group;
            int map = g != null ? g.bindMap : p.bindMap;
            boolean inside = false;
            if (g != null) {
                for (Player m : g.members) {
                    if (m.mapId == g.bindMap && m.instanceId == g.instanceId && g.instanceId != 0) {
                        inside = true;
                    }
                }
            }
            if (inside) {
                WowBuffer fail = new WowBuffer(8);
                fail.putU32(0);
                fail.putU32(map);
                s.send(Opcodes.SMSG_INSTANCE_RESET_FAILED, fail.array());
            } else {
                WowBuffer ok = new WowBuffer(4);
                ok.putU32(map == 0 ? 389 : map);
                s.send(Opcodes.SMSG_INSTANCE_RESET, ok.array());
                if (g != null) {
                    g.instanceId = 0;
                    g.bindMap = 0;
                }
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_BATTLEFIELD_PORT) {
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
            return true;
        }
        if (opcode == Opcodes.CMSG_TRAINER_BUY_SPELL) {
            long guid = in.remaining() >= 8 ? in.getU64() : 0;
            int spell = in.remaining() >= 4 ? in.getU32() : 0;
            p.spells.add(spell);
            WowBuffer ok = new WowBuffer(12);
            ok.putU64(guid);
            ok.putU32(spell);
            s.send(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED, ok.array());
            WowBuffer learned = new WowBuffer(4);
            learned.putU32(spell);
            s.send(Opcodes.SMSG_LEARNED_SPELL, learned.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_TEXT_EMOTE) {
            int emote = in.remaining() >= 4 ? in.getU32() : 0;
            int num = in.remaining() >= 4 ? in.getU32() : 0;
            long target = in.remaining() >= 8 ? in.getU64() : 0;
            WowBuffer out = new WowBuffer(32);
            out.putU64(p.guid);
            out.putU32(emote);
            out.putU32(num);
            out.putU32(1);
            out.putU8(0);
            s.send(Opcodes.SMSG_TEXT_EMOTE, out.array());
            Player other = world.playerByGuid(target);
            if (other != null && other.session != null) {
                other.session.send(Opcodes.SMSG_TEXT_EMOTE, out.array());
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_GROUP_RAID_CONVERT) {
            if (p.group != null) {
                p.group.raid = true;
                s.send(Opcodes.SMSG_GROUP_LIST, p.group.listFor(p));
            }
            return true;
        }
        if (opcode == Opcodes.MSG_RAID_READY_CHECK) {
            s.send(Opcodes.MSG_RAID_READY_CHECK, new byte[0]);
            if (p.group != null) {
                for (Player m : p.group.members) {
                    if (m.session != null) {
                        m.session.send(Opcodes.MSG_RAID_READY_CHECK, new byte[0]);
                    }
                }
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_GUILD_CREATE) {
            p.guildId = 1;
            p.guildLeader = true;
            p.guildName = in.getCString();
            roster(s, p);
            return true;
        }
        if (opcode == Opcodes.CMSG_GUILD_BANK_SWAP_ITEMS) {
            if (p.guildBankItem != null) {
                p.guildBankItem.slot = p.firstFreeBagSlot();
                p.items.put((int) p.guildBankItem.guid, p.guildBankItem);
                p.guildBankItem = null;
            } else {
                Item it = p.items.values().stream().findFirst().orElse(null);
                if (it != null) {
                    p.items.remove((int) it.guid);
                    p.guildBankItem = it;
                }
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_GUILD_BANK_BUY_TAB) {
            p.money = Math.max(0, p.money - 100000);
            p.guildBankTabs++;
            WowBuffer perm = new WowBuffer(4 + 6 * 4);
            perm.putU32(6);
            for (int i = 0; i < 6; i++) {
                perm.putU32(0xFFFF);
            }
            s.send(Opcodes.MSG_GUILD_PERMISSIONS, perm.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_AUCTION_LIST_ITEMS) {
            WowBuffer list = new WowBuffer(16);
            list.putU32(1);
            list.putU32(1);
            s.send(Opcodes.SMSG_AUCTION_LIST_RESULT, list.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_SET_LOOKING_FOR_GROUP) {
            p.looking = true;
            return true;
        }
        if (opcode == Opcodes.MSG_LOOKING_FOR_GROUP) {
            WowBuffer list = new WowBuffer(16);
            list.putU32(1);
            list.putU64(p.guid);
            s.send(Opcodes.MSG_LOOKING_FOR_GROUP, list.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_PET_ACTION) {
            if (in.remaining() >= 8) {
                in.getU64();
            }
            int data = in.remaining() >= 4 ? in.getU32() : 0;
            int cmd = data & 0xFFFFFF;
            int type = (data >>> 24) & 0xFF;
            if (p.pet == null) {
                p.pet = new Pet();
                p.pet.summoned = true;
                p.pet.name = "Pet";
            }
            if (type == 0x07 && cmd == 3 && p.clazz != 3) {
                p.pet = null;
            }
            if (p.pet != null) {
                s.send(Opcodes.SMSG_PET_SPELLS, new byte[20]);
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_PET_ABANDON) {
            if (p.clazz != 3) {
                p.pet = null;
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_TOTEM_DESTROYED) {
            int slot = in.remaining() > 0 ? in.getU8() : 0;
            if (slot >= 0 && slot < p.totems.length) {
                p.totems[slot] = 0;
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_CHANNEL_LIST) {
            String name = in.remaining() > 0 ? in.getCString() : "";
            WowBuffer list = new WowBuffer(32);
            list.putU8(0);
            list.putCString(name);
            list.putU8(0);
            list.putU32(1);
            list.putU64(p.guid);
            list.putU8(0);
            s.send(Opcodes.SMSG_CHANNEL_LIST, list.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_STABLE_PET) {
            s.send(Opcodes.SMSG_STABLE_RESULT, new byte[]{0x08});
            return true;
        }
        if (opcode == Opcodes.CMSG_UNSTABLE_PET) {
            s.send(Opcodes.SMSG_STABLE_RESULT, new byte[]{0x09});
            return true;
        }
        if (opcode == Opcodes.CMSG_BUY_STABLE_SLOT) {
            s.send(Opcodes.SMSG_STABLE_RESULT, new byte[]{0x08});
            return true;
        }
        if (opcode == Opcodes.CMSG_SELL_ITEM) {
            if (in.remaining() >= 8) {
                in.getU64();
            }
            long item = in.remaining() >= 8 ? in.getU64() : 0;
            Item it = p.items.remove((int) item);
            if (it != null) {
                it.slot = 74;
                p.buyback.put(74, it);
                p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1, 1);
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_BUYBACK_ITEM) {
            if (in.remaining() >= 8) {
                in.getU64();
            }
            int slot = in.remaining() >= 4 ? in.getU32() : 74;
            Item it = p.buyback.remove(slot);
            if (it != null) {
                int bag = p.firstFreeBagSlot();
                it.slot = bag < 0 ? 23 : bag;
                p.items.put((int) it.guid, it);
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_REPAIR_ITEM) {
            p.money = Math.max(0, p.money - 1);
            for (Item it : p.items.values()) {
                it.durability = 100;
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_SOCKET_GEMS) {
            long itemGuid = in.remaining() >= 8 ? in.getU64() : 0;
            Item it = p.items.get((int) itemGuid);
            if (it == null) {
                it = p.items.values().stream().findFirst().orElse(null);
            }
            if (it != null) {
                it.enchant = 1;
            }
            return true;
        }
        if (opcode == Opcodes.MSG_RANDOM_ROLL) {
            int min = in.remaining() >= 4 ? in.getU32() : 1;
            int max = in.remaining() >= 4 ? in.getU32() : 100;
            WowBuffer roll = new WowBuffer(16);
            roll.putU32(min);
            roll.putU32(max);
            roll.putU32(min);
            roll.putU64(p.guid);
            s.send(Opcodes.MSG_RANDOM_ROLL, roll.array());
            return true;
        }
        if (opcode == Opcodes.MSG_MINIMAP_PING) {
            s.send(Opcodes.MSG_MINIMAP_PING, in.remainingBytes());
            return true;
        }
        if (opcode == Opcodes.CMSG_REPORT_PVP_AFK) {
            p.afkReports++;
            if (p.afkReports >= 3) {
                p.auras.add(new Unit.Aura(PvpObjectives.IDLE_AFK, 0, 1));
            }
            return true;
        }
        if (opcode == Opcodes.MSG_TALENT_WIPE_CONFIRM) {
            p.auras.add(new Unit.Aura(PvpObjectives.TALENT_WIPE, 0, 1));
            s.send(Opcodes.MSG_TALENT_WIPE_CONFIRM, new byte[0]);
            return true;
        }
        if (opcode == Opcodes.CMSG_CANCEL_CHANNELLING) {
            p.channeling = false;
            return true;
        }
        if (opcode == Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK) {
            if (in.remaining() > 0) {
                in.getPackedGuid();
            }
            if (in.remaining() >= 4) {
                in.getU32();
            }
            if (in.remaining() >= 21) {
                org.tbc.world.net.wow8606.MovementInfo.readC2s(in);
            }
            if (in.remaining() >= 4) {
                p.lastAckSpeed = in.getFloat();
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_CANCEL_MOUNT_AURA) {
            p.mounted = false;
            p.auras.removeIf(a -> a.spellId() == PvpObjectives.MOUNT_AURA);
            return true;
        }
        if (opcode == Opcodes.CMSG_PUSHQUESTTOPARTY) {
            int q = in.remaining() >= 4 ? in.getU32() : 0;
            if (p.group != null) {
                for (Player m : p.group.members) {
                    if (m != p && m.session != null) {
                        WowBuffer d = new WowBuffer(8);
                        d.putU32(q);
                        m.session.send(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, d.array());
                    }
                }
            }
            s.send(Opcodes.MSG_QUEST_PUSH_RESULT, new byte[4]);
            return true;
        }
        if (opcode == Opcodes.CMSG_LOOT_MASTER_GIVE) {
            if (in.remaining() >= 8) {
                in.getU64();
            }
            if (in.remaining() > 0) {
                in.getU8();
            }
            long target = in.remaining() >= 8 ? in.getU64() : 0;
            Player t = world.playerByGuid(target);
            if (t != null) {
                Item given = new Item(world.nextItemGuid(), 25);
                given.ownerGuid = (int) t.guid;
                int bag = t.firstFreeBagSlot();
                given.slot = bag < 0 ? 23 : bag;
                t.items.put((int) given.guid, given);
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_LOOT_ROLL) {
            WowBuffer start = new WowBuffer(16);
            start.putU64(p.guid);
            s.send(Opcodes.SMSG_LOOT_START_ROLL, start.array());
            s.send(Opcodes.SMSG_LOOT_ROLL_WON, start.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_GMTICKET_GETTICKET) {
            WowBuffer t = new WowBuffer(16);
            t.putU32(0x06);
            t.putCString(s.lastTicket == null ? "" : s.lastTicket);
            s.send(Opcodes.SMSG_GMTICKET_GETTICKET, t.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_ACCEPT_LFG_MATCH) {
            return true;
        }
        if (opcode == Opcodes.CMSG_TURN_IN_PETITION) {
            p.arenaTeam = 1;
            s.send(Opcodes.SMSG_PETITION_SIGN_RESULTS, new byte[4]);
            return true;
        }
        if (opcode == Opcodes.CMSG_AREA_SPIRIT_HEALER_QUEUE
                || opcode == Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE) {
            spiritHealer(p);
            return true;
        }
        if (opcode == Opcodes.MSG_INSPECT_HONOR_STATS) {
            long g = in.remaining() >= 8 ? in.getU64() : p.guid;
            WowBuffer h = new WowBuffer(32);
            h.putU64(g);
            h.putU8(0);
            h.putU32(p.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FIELD_KILLS));
            h.putU32(p.honorToday);
            h.putU32(p.yesterdayContrib);
            h.putU32(p.honorPoints);
            s.send(Opcodes.MSG_INSPECT_HONOR_STATS, h.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_ACTIVATETAXI || opcode == Opcodes.CMSG_ACTIVATETAXIEXPRESS) {
            return false;
        }
        return false;
    }

    static void spiritHealer(Player p) {
        p.ghost = false;
        int max = p.maxHealth() == 0 ? 100 : p.maxHealth();
        p.setHealth(max / 2);
        for (Item it : p.items.values()) {
            it.durability = (int) (it.durability * 0.75);
        }
        if (p.level >= 11) {
            p.auras.add(new Unit.Aura(PvpObjectives.SICKNESS, 0, 1));
        }
    }

    static void roster(WorldSession s, Player p) {
        WowBuffer r = new WowBuffer(64);
        r.putU32(1);
        r.putCString(p.name);
        r.putU8(p.gender);
        r.putU8(p.level);
        r.putU8(p.clazz);
        s.send(Opcodes.SMSG_GUILD_ROSTER, r.array());
    }
}
