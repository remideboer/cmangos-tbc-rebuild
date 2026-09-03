package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.List;

/** Auction search list. Layout: spec/03-protocol/packets/auction.md */
public final class AuctionHandler {
    private AuctionHandler() {}

    public static void listItems(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        if (in.remaining() >= 4) {
            in.getU32();
        }
        String name = in.remaining() > 0 ? in.getCString() : "";
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_AUCTIONEER) == 0) {
            return;
        }
        String q = name == null ? "" : name.toLowerCase();
        List<ObjectMgr.Auction> hits = new ArrayList<>();
        for (ObjectMgr.Auction a : world.objectMgr.auctions) {
            if (!q.isEmpty() && (a.name() == null || !a.name().toLowerCase().contains(q))) {
                continue;
            }
            hits.add(a);
        }
        WowBuffer out = new WowBuffer(256);
        out.putU32(hits.size());
        for (ObjectMgr.Auction a : hits) {
            putRow(out, a);
        }
        out.putU32(hits.size());
        out.putU32(Content.AUCTION_LIST_DELAY_MS);
        s.send(Opcodes.SMSG_AUCTION_LIST_RESULT, out.array());
    }

    static void putRow(WowBuffer out, ObjectMgr.Auction a) {
        out.putU32(a.id());
        out.putU32(a.itemEntry());
        for (int i = 0; i < 6; i++) {
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
        }
        out.putU32(0);
        out.putU32(0);
        out.putU32(1);
        out.putU32(0);
        out.putU32(0);
        out.putU64(a.owner());
        out.putU32(a.startBid());
        out.putU32(0);
        out.putU32(a.buyout());
        out.putU32(a.timeLeftMs());
        out.putU64(0);
        out.putU32(0);
    }
}
