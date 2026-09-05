package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Group;
import org.tbc.world.entity.Item;
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
            InventoryHandler.swapInvItem(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_DESTROYITEM) {
            InventoryHandler.destroyItem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_SPLIT_ITEM) {
            InventoryHandler.splitItem(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_BANKER_ACTIVATE) {
            InventoryHandler.bankerActivate(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_BUY_BANK_SLOT) {
            InventoryHandler.buyBankSlot(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_BINDER_ACTIVATE) {
            BinderHandler.activate(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_AUTOBANK_ITEM) {
            InventoryHandler.autobankItem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_AUTOSTORE_BANK_ITEM) {
            InventoryHandler.autostoreBankItem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_AUTOEQUIP_ITEM) {
            InventoryHandler.autoequipItem(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_AUTOSTORE_BAG_ITEM) {
            InventoryHandler.autostoreBagItem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_SET_AMMO) {
            InventoryHandler.setAmmo(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_READ_ITEM) {
            InventoryHandler.readItem(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_WRAP_ITEM) {
            InventoryHandler.wrapItem(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_CANCEL_TEMP_ENCHANTMENT) {
            InventoryHandler.cancelTempEnchantment(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_SWAP_ITEM) {
            InventoryHandler.swapItem(s, world, in);
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
            TrainerHandler.buySpell(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_TEXT_EMOTE) {
            ChannelHandler.textEmote(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_LOOT_METHOD) {
            LootHandler.lootMethod(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_GROUP_RAID_CONVERT) {
            GroupHandler.raidConvert(s);
            return true;
        }
        if (opcode == Opcodes.MSG_RAID_READY_CHECK) {
            GroupHandler.readyCheck(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_GUILD_CREATE) {
            GuildHandler.create(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_GUILD_BANKER_ACTIVATE) {
            GuildHandler.bankerActivate(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_GUILD_BANK_SWAP_ITEMS) {
            GuildHandler.swapItems(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_GUILD_BANK_BUY_TAB) {
            GuildHandler.buyTab(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_AUCTION_LIST_ITEMS) {
            AuctionHandler.listItems(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_SET_LOOKING_FOR_GROUP) {
            LfgHandler.setLooking(s);
            return true;
        }
        if (opcode == Opcodes.MSG_LOOKING_FOR_GROUP) {
            LfgHandler.list(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_PET_ACTION) {
            PetHandler.action(s, world, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_PET_ABANDON) {
            PetHandler.abandon(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_TOTEM_DESTROYED) {
            PetHandler.destroyTotem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_CHANNEL_LIST) {
            ChannelHandler.list(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_STABLE_PET) {
            PetHandler.stablePet(s);
            return true;
        }
        if (opcode == Opcodes.CMSG_UNSTABLE_PET) {
            PetHandler.unstablePet(s);
            return true;
        }
        if (opcode == Opcodes.CMSG_BUY_STABLE_SLOT) {
            PetHandler.buyStableSlot(s);
            return true;
        }
        if (opcode == Opcodes.CMSG_SELL_ITEM) {
            InventoryHandler.sellItem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_BUYBACK_ITEM) {
            InventoryHandler.buybackItem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_REPAIR_ITEM) {
            InventoryHandler.repairItem(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_SOCKET_GEMS) {
            InventoryHandler.socketGems(s, in);
            return true;
        }
        if (opcode == Opcodes.MSG_RANDOM_ROLL) {
            GroupHandler.randomRoll(s, in);
            return true;
        }
        if (opcode == Opcodes.MSG_MINIMAP_PING) {
            GroupHandler.minimapPing(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_REPORT_PVP_AFK) {
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
            return true;
        }
        if (opcode == Opcodes.MSG_TALENT_WIPE_CONFIRM) {
            p.auras.add(new Unit.Aura(PvpObjectives.TALENT_WIPE, 0, 1));
            WowBuffer learned = new WowBuffer(4);
            learned.putU32(PvpObjectives.TALENT_WIPE);
            s.send(Opcodes.SMSG_LEARNED_SPELL, learned.array());
            WowBuffer confirm = new WowBuffer(12);
            confirm.putU64(in.remaining() >= 8 ? in.getU64() : 0);
            confirm.putU32(0);
            s.send(Opcodes.MSG_TALENT_WIPE_CONFIRM, confirm.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_CANCEL_CHANNELLING) {
            p.channeling = false;
            WowBuffer fail = new WowBuffer(12);
            fail.putPackedGuid(p.guid);
            fail.putU32(in.remaining() >= 4 ? in.getU32() : 0);
            fail.putU8(0);
            s.send(Opcodes.SMSG_SPELL_FAILURE, fail.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_CANCEL_AURA) {
            int spell = in.remaining() >= 4 ? in.getU32() : 0;
            world.spells.cancelAura(p, spell);
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
            WowBuffer result = new WowBuffer(9);
            result.putU64(p.guid);
            result.putU8(0);
            s.send(Opcodes.MSG_QUEST_PUSH_RESULT, result.array());
            if (p.group != null) {
                for (Player m : p.group.members) {
                    if (m != p && m.session != null) {
                        WowBuffer d = new WowBuffer(8);
                        d.putU32(q);
                        m.session.send(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, d.array());
                    }
                }
            }
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
                WowBuffer push = new WowBuffer(48);
                push.putU64(t.guid);
                push.putU32(0);
                push.putU32(0);
                push.putU32(1);
                push.putU8(0);
                push.putU32(given.slot);
                push.putU32(25);
                push.putU32(0);
                push.putU32(0);
                push.putU32(1);
                push.putU32(1);
                if (t.session != null) {
                    t.session.send(Opcodes.SMSG_ITEM_PUSH_RESULT, push.array());
                }
            }
            return true;
        }
        if (opcode == Opcodes.CMSG_LOOT_ROLL) {
            LootHandler.lootRoll(s, in);
            return true;
        }
        if (opcode == Opcodes.CMSG_GMTICKET_GETTICKET) {
            WowBuffer t = new WowBuffer(32);
            t.putU32(0x06);
            t.putCString(s.lastTicket == null ? "" : s.lastTicket);
            t.putU8(0);
            t.putFloat(0);
            t.putFloat(0);
            t.putFloat(0);
            t.putU8(0);
            t.putU8(0);
            s.send(Opcodes.SMSG_GMTICKET_GETTICKET, t.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_ACCEPT_LFG_MATCH) {
            LfgHandler.acceptMatch(s);
            return true;
        }
        if (opcode == Opcodes.CMSG_TURN_IN_PETITION) {
            p.arenaTeam = 1;
            WowBuffer roster = new WowBuffer(48);
            roster.putU32(1);
            roster.putU32(1);
            roster.putU32(2);
            roster.putU64(p.guid);
            roster.putU8(1);
            roster.putCString(p.name);
            roster.putU32(0);
            roster.putU8(p.level);
            roster.putU8(p.clazz);
            roster.putU32(0);
            roster.putU32(0);
            roster.putU32(0);
            roster.putU32(0);
            roster.putU32(0);
            s.send(Opcodes.SMSG_ARENA_TEAM_ROSTER, roster.array());
            return true;
        }
        if (opcode == Opcodes.CMSG_AREA_SPIRIT_HEALER_QUEUE
                || opcode == Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE) {
            DeathHandler.spiritHealer(s, world);
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
}
