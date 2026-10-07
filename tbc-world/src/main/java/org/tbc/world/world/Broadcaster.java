package org.tbc.world.world;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;

import java.util.function.BiConsumer;

/**
 * Session-aware packet fan-out (CMaNGOS Player::SendDirectMessage / WorldObject::SendMessageToSet).
 * Offline players (no session) are skipped; {@link GameMap#nearbyPlayers} never contains the unit itself.
 */
public final class Broadcaster {
    private Broadcaster() {}

    /** Send to one player when online. */
    public static void toPlayer(Player p, int opcode, byte[] payload) {
        if (p.session != null) {
            p.session.send(opcode, payload);
        }
    }

    /** Send to every online player within {@code range} of {@code u}, excluding {@code u}. */
    public static void nearby(GameMap m, Unit u, double range, int opcode, byte[] payload) {
        for (Player pl : m.nearbyPlayers(u, range)) {
            toPlayer(pl, opcode, payload);
        }
    }

    /** {@link #nearby} plus {@code u} itself when it is an online player. */
    public static void nearbyAndSelf(GameMap m, Unit u, double range, int opcode, byte[] payload) {
        if (u instanceof Player self) {
            toPlayer(self, opcode, payload);
        }
        nearby(m, u, range, opcode, payload);
    }

    /** {@link #nearbyAndSelf} as a sink for engines that emit through a {@code BiConsumer}. */
    public static BiConsumer<Integer, byte[]> nearbyAndSelfSink(GameMap m, Unit u, double range) {
        return (op, payload) -> nearbyAndSelf(m, u, range, op, payload);
    }
}
