package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.map.Terrain;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.function.BiConsumer;

/** CMSG_BINDER_ACTIVATE. Layout: spec/03-protocol/packets/misc-player.md */
public final class BinderHandler {
    /** NPCHandler.cpp SendBindPoint spell 3286 Bind. */
    public static final int SPELL_BIND = 3286;

    private BinderHandler() {}

    public static void activate(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (p == null) {
            return;
        }
        activate(p, world.map(p.mapId, p.instanceId), world.terrain, in, s::send);
    }

    public static void activate(Player p, GameMap map, Terrain terrain, WowBuffer in,
                                BiConsumer<Integer, byte[]> send) {
        if (!p.alive()) {
            return;
        }
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature npc = Content.creature(map, guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_INNKEEPER) == 0) {
            return;
        }
        sendBindPoint(p, npc, terrain, send);
    }

    /** NPCHandler.cpp SendBindPoint then Spell::EffectBind. */
    static void sendBindPoint(Player p, Creature npc, Terrain terrain, BiConsumer<Integer, byte[]> send) {
        int areaId = terrain == null ? 0 : terrain.area(p.mapId, p.x, p.y);
        p.bindX = p.x;
        p.bindY = p.y;
        p.bindZ = p.z;
        p.bindMap = p.mapId;
        p.bindZone = areaId;
        p.dirty = true;
        WowBuffer bind = new WowBuffer(20);
        bind.putFloat(p.bindX);
        bind.putFloat(p.bindY);
        bind.putFloat(p.bindZ);
        bind.putU32(p.bindMap);
        bind.putU32(areaId);
        send.accept(Opcodes.SMSG_BINDPOINTUPDATE, bind.array());
        WowBuffer bound = new WowBuffer(12);
        bound.putU64(npc.guid);
        bound.putU32(areaId);
        send.accept(Opcodes.SMSG_PLAYERBOUND, bound.array());
        WowBuffer bought = new WowBuffer(12);
        bought.putU64(npc.guid);
        bought.putU32(SPELL_BIND);
        send.accept(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED, bought.array());
        send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
    }
}
