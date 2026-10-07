package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.world.World;

/** One logged-in C2S opcode handler. Responses go through {@link WorldSession#send}. */
@FunctionalInterface
public interface PacketOperation {
    void handle(WorldSession session, World world, WowBuffer in);
}
