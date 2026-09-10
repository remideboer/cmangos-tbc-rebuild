package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.world.World;

import java.util.function.BiConsumer;

/** CMSG_TAXINODE_STATUS_QUERY / CMSG_TAXIQUERYAVAILABLENODES / CMSG_ACTIVATETAXI. Layout: spec/03-protocol/packets/taxi.md */
public final class TaxiHandler {
    public static final int MONSTER_MOVE_NORMAL = 0;
    public static final int MONSTER_MOVE_FACING_SPOT = 2;
    public static final int MONSTER_MOVE_FACING_TARGET = 3;
    public static final int MONSTER_MOVE_FACING_ANGLE = 4;
    public static final int SPLINE_FLAG_RUNMODE = 0x00000100;
    /** taxi.md TaxiMaskSize. */
    public static final int TAXI_MASK_SIZE = 16;

    private TaxiHandler() {}

    /** CMaNGOS TaxiHandler SendTaxiStatus — GetCreature, no range/flag check. */
    public static void sendStatus(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null) {
            return;
        }
        int curloc = world.objectMgr.nearestTaxiNode(npc.x, npc.y, npc.z, npc.mapId, p.team);
        if (curloc == 0) {
            return;
        }
        WowBuffer out = new WowBuffer(9);
        out.putU64(guid);
        out.putU8(p.taxiKnown(curloc) ? 1 : 0);
        s.send(Opcodes.SMSG_TAXINODE_STATUS, out.array());
    }

    /** CMaNGOS HandleTaxiQueryAvailableNodes — interact + learn-new or menu. */
    public static void queryAvailable(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_FLIGHTMASTER) == 0) {
            return;
        }
        if (learnNewNode(s, world, npc)) {
            return;
        }
        sendMenu(p, npc, world.objectMgr, s::send);
    }

    /** CMaNGOS SendLearnNewTaxiNode. True skips the menu (no node, or just learned). */
    static boolean learnNewNode(WorldSession s, World world, Creature npc) {
        Player p = s.player();
        int curloc = world.objectMgr.nearestTaxiNode(npc.x, npc.y, npc.z, npc.mapId, p.team);
        if (curloc == 0) {
            return true;
        }
        if (p.taxiKnown(curloc)) {
            return false;
        }
        p.learnTaxi(curloc);
        s.send(Opcodes.SMSG_NEW_TAXI_PATH, new byte[0]);
        WowBuffer update = new WowBuffer(9);
        update.putU64(npc.guid);
        update.putU8(1);
        s.send(Opcodes.SMSG_TAXINODE_STATUS, update.array());
        return true;
    }

    public static void sendMenu(Player p, Creature c, ObjectMgr mgr, BiConsumer<Integer, byte[]> send) {
        byte[] payload = encodeMenu(p, c, mgr);
        if (payload != null) {
            send.accept(Opcodes.SMSG_SHOWTAXINODES, payload);
        }
    }

    static byte[] encodeMenu(Player p, Creature c, ObjectMgr mgr) {
        if ((c.npcFlags & Content.UNIT_NPC_FLAG_FLIGHTMASTER) == 0) {
            return null;
        }
        int curloc = mgr.nearestTaxiNode(c.x, c.y, c.z, c.mapId, p.team);
        if (curloc == 0) {
            return null;
        }
        WowBuffer b = new WowBuffer(16 + TAXI_MASK_SIZE * 4);
        b.putU32(1);
        b.putU64(c.guid);
        b.putU32(curloc);
        for (int i = 0; i < TAXI_MASK_SIZE; i++) {
            b.putU32(p.taxiMask[i]);
        }
        return b.array();
    }

    public static void activate(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 16) {
            return;
        }
        long guid = in.getU64();
        int from = in.getU32();
        int to = in.getU32();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_FLIGHTMASTER) == 0) {
            return;
        }
        if (!p.taxiKnown(from) || !p.taxiKnown(to)) {
            WowBuffer reply = new WowBuffer(4);
            reply.putU32(Content.ERR_TAXINOTVISITED);
            s.send(Opcodes.SMSG_ACTIVATETAXIREPLY, reply.array());
            return;
        }
        ObjectMgr.TaxiHop hop = world.objectMgr.taxiPaths.get(ObjectMgr.taxiKey(from, to));
        if (hop == null) {
            return;
        }
        WowBuffer ok = new WowBuffer(4);
        ok.putU32(Content.ERR_TAXIOK);
        s.send(Opcodes.SMSG_ACTIVATETAXIREPLY, ok.array());
        s.send(Opcodes.SMSG_MONSTER_MOVE, monsterMove(p, hop));
    }

    static byte[] monsterMove(Player p, ObjectMgr.TaxiHop hop) {
        float dx = hop.x() - p.x;
        float dy = hop.y() - p.y;
        float dz = hop.z() - p.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        int duration = Math.max(1, (int) (dist / UpdateBuilder.RUN * 1000f));
        WowBuffer b = new WowBuffer(64);
        b.putPackedGuid(p.guid);
        b.putFloat(p.x);
        b.putFloat(p.y);
        b.putFloat(p.z);
        b.putU32(1);
        b.putU8(MONSTER_MOVE_NORMAL);
        b.putU32(SPLINE_FLAG_RUNMODE);
        b.putU32(duration);
        b.putU32(0);
        b.putFloat(hop.x());
        b.putFloat(hop.y());
        b.putFloat(hop.z());
        return b.array();
    }
}
