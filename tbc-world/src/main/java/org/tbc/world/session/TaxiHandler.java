package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.world.World;

/** CMSG_ACTIVATETAXI. Spline: spec/03-protocol/packets/taxi.md */
public final class TaxiHandler {
    public static final int MONSTER_MOVE_NORMAL = 0;
    public static final int SPLINE_FLAG_RUNMODE = 0x00000100;

    private TaxiHandler() {}

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
