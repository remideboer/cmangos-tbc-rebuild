package org.tbc.world.content;

import static org.tbc.world.content.Content.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.AuctionHandler;
import org.tbc.world.session.TaxiHandler;
import org.tbc.world.session.TrainerHandler;

/**
 * NPC gossip: CMSG_GOSSIP_HELLO / CMSG_GOSSIP_SELECT_OPTION, SMSG_GOSSIP_MESSAGE menu and quest rows,
 * POI and binder confirm (CMaNGOS NPCHandler.cpp, GossipDef.cpp). Split out of Content; Content is the facade.
 */
final class Gossip {

    private final ObjectMgr mgr;
    private final QuestGiver quests;
    private final Vendor vendor;

    Gossip(ObjectMgr mgr, QuestGiver quests, Vendor vendor) {
        this.mgr = mgr;
        this.quests = quests;
        this.vendor = vendor;
    }
    public void gossipHello(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c)) {
            return;
        }
        if (p.ghost && (c.npcFlags & (UNIT_NPC_FLAG_SPIRITHEALER | UNIT_NPC_FLAG_SPIRITGUIDE)) == 0) {
            return;
        }
        send.accept(Opcodes.SMSG_GOSSIP_MESSAGE, encodeGossip(p, c));
    }

    public void gossipSelect(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 16) {
            return;
        }
        long guid = in.getU64();
        int menuId = in.getU32();
        int gossipListId = in.getU32();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c)) {
            return;
        }
        if (!p.hasGossipOption(menuId, gossipListId)) {
            return;
        }
        int option = p.gossipOptionId(gossipListId);
        if (option == GOSSIP_OPTION_VENDOR) {
            if ((c.npcFlags & UNIT_NPC_FLAG_VENDOR) == 0) {
                return;
            }
            send.accept(Opcodes.SMSG_LIST_INVENTORY, vendor.encodeVendorList(c));
        } else if (option == GOSSIP_OPTION_TRAINER) {
            TrainerHandler.sendList(p, c, mgr, send);
        } else if (option == GOSSIP_OPTION_BANKER) {
            Banker.sendShowBank(c, send);
        } else if (option == GOSSIP_OPTION_TAXIVENDOR) {
            TaxiHandler.sendMenu(p, c, mgr, send);
        } else if (option == GOSSIP_OPTION_INNKEEPER) {
            if ((c.npcFlags & UNIT_NPC_FLAG_INNKEEPER) == 0) {
                return;
            }
            send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
            send.accept(Opcodes.SMSG_BINDER_CONFIRM, encodeBinderConfirm(c));
        } else if (option == GOSSIP_OPTION_AUCTIONEER) {
            if ((c.npcFlags & UNIT_NPC_FLAG_AUCTIONEER) == 0) {
                return;
            }
            AuctionHandler.sendHello(c, send);
        } else if (option == GOSSIP_OPTION_SPIRITHEALER) {
            if (!p.ghost && p.alive()) {
                return;
            }
            WowBuffer confirm = new WowBuffer(8);
            confirm.putU64(c.guid);
            send.accept(Opcodes.SMSG_SPIRIT_HEALER_CONFIRM, confirm.array());
        } else if (option == GOSSIP_OPTION_GOSSIP) {
            int poiId = p.gossipActionPoi(gossipListId);
            if (poiId != 0) {
                ObjectMgr.PointOfInterest poi = mgr.pointsOfInterest.get(poiId);
                if (poi != null) {
                    send.accept(Opcodes.SMSG_GOSSIP_POI, encodeGossipPoi(poi));
                }
            }
            int next = p.gossipActionMenu(gossipListId);
            if (next > 0) {
                send.accept(Opcodes.SMSG_GOSSIP_MESSAGE, encodeGossip(p, c, next));
            } else if (next < 0) {
                send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
            }
        }
    }

    private List<Integer> gossipQuests(Player p, Creature c) {
        List<Integer> out = new ArrayList<>();
        for (int id : mgr.questGivers.getOrDefault(c.entry, List.of())) {
            if (includeGossipQuest(p, c.entry, id, true)) {
                out.add(id);
            }
        }
        for (int id : mgr.questInvolved.getOrDefault(c.entry, List.of())) {
            if (!out.contains(id) && includeGossipQuest(p, c.entry, id, false)) {
                out.add(id);
            }
        }
        return out;
    }

    private boolean includeGossipQuest(Player p, int entry, int questId, boolean fromGiver) {
        ObjectMgr.QuestTemplate q = mgr.quest(questId);
        if (q == null) {
            return true;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return fromGiver && quests.canTake(p, q);
        }
        // Incomplete / complete rows come from involved relation (Player::PrepareQuestMenu).
        return quests.involves(entry, questId);
    }

    byte[] encodeGossip(Player p, Creature c) {
        return encodeGossip(p, c, mgr.gossipMenuId(c.entry));
    }

    byte[] encodeGossip(Player p, Creature c, int menuId) {
        List<ObjectMgr.GossipMenuItem> items = mgr.gossipOptionsFor(p, c, menuId);
        int[] optionIds = new int[items.size()];
        int[] actionMenus = new int[items.size()];
        int[] actionPois = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            optionIds[i] = items.get(i).optionId();
            actionMenus[i] = items.get(i).actionMenu();
            actionPois[i] = items.get(i).actionPoi();
        }
        p.prepareGossipMenu(menuId, optionIds, actionMenus, actionPois);
        List<Integer> quests = gossipQuests(p, c);
        WowBuffer b = new WowBuffer(64);
        b.putU64(c.guid);
        b.putU32(menuId);
        b.putU32(mgr.gossipTextId(menuId));
        b.putU32(items.size());
        int index = 0;
        for (ObjectMgr.GossipMenuItem it : items) {
            b.putU32(index++);
            b.putU8(it.icon());
            b.putU8(it.coded());
            b.putU32(it.boxMoney());
            b.putCString(it.text());
            b.putCString(it.boxText());
        }
        b.putU32(quests.size());
        for (int id : quests) {
            ObjectMgr.QuestTemplate q = mgr.quest(id);
            b.putU32(id);
            b.putU32(questMenuIcon(p, id, q));
            b.putU32(q == null ? 1 : QuestGiver.shownQuestLevel(q));
            b.putCString(q == null ? "" : q.title());
        }
        return b.array();
    }

    static byte[] encodeGossipPoi(ObjectMgr.PointOfInterest poi) {
        WowBuffer b = new WowBuffer(24 + poi.iconName().length());
        b.putU32(poi.flags());
        b.putFloat(poi.x());
        b.putFloat(poi.y());
        b.putU32(poi.icon());
        b.putU32(poi.data());
        b.putCString(poi.iconName());
        return b.array();
    }

    static byte[] encodeBinderConfirm(Creature c) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(c.guid);
        return b.array();
    }

    private int questMenuIcon(Player p, int questId, ObjectMgr.QuestTemplate q) {
        if (q == null) {
            return DIALOG_STATUS_NONE;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return DIALOG_STATUS_AVAILABLE;
        }
        if (quests.readyToTurnIn(p, slot, q)) {
            return DIALOG_STATUS_REWARD_REP;
        }
        return DIALOG_STATUS_INCOMPLETE;
    }
}
