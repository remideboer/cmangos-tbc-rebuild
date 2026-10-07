package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.world.World;

/** Mount special animation + dismount (MiscHandler.cpp HandleMountSpecialAnimOpcode / HandleCancelMountAuraOpcode). */
public final class MountHandler {
    private MountHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_MOUNTSPECIAL_ANIM, (s, w, in) -> mountSpecialAnim(s, w))
                .register(Opcodes.CMSG_CANCEL_MOUNT_AURA, (s, w, in) -> cancelMountAura(s, w));
    }

    public static void mountSpecialAnim(WorldSession s, World world) {
        Player p = s.player();
        WowBuffer out = new WowBuffer(8);
        out.putU64(p.guid);
        byte[] pkt = out.array();
        for (Player o : world.map(p.mapId, p.instanceId).nearbyPlayers(p, GameMap.VISIBILITY)) {
            if (o.session != null) {
                o.session.send(Opcodes.SMSG_MOUNTSPECIAL_ANIM, pkt);
            }
        }
    }

    public static void cancelMountAura(WorldSession s, World world) {
        Player p = s.player();
        if (p.mounted) {
            WowBuffer out = new WowBuffer(9);
            out.putPackedGuid(p.guid);
            byte[] pkt = out.array();
            s.send(Opcodes.SMSG_DISMOUNT, pkt);
            for (Player o : world.map(p.mapId, p.instanceId).nearbyPlayers(p, GameMap.VISIBILITY)) {
                if (o.session != null) {
                    o.session.send(Opcodes.SMSG_DISMOUNT, pkt);
                }
            }
        }
        p.mounted = false;
        p.auras.removeIf(a -> a.spellId() == PvpObjectives.MOUNT_AURA);
    }
}
