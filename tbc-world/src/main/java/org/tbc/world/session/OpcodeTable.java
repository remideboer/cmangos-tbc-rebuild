package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Opcode → {@link PacketOperation} for the STATUS_LOGGEDIN switch in {@link WorldSession#handle}.
 * Each handler family registers itself ({@code XxxHandler.register(table)}); the session consults
 * the table before its remaining switch cases. Duplicate registration is a programming error.
 */
public final class OpcodeTable {
    private final Map<Integer, PacketOperation> ops = new HashMap<>();

    public OpcodeTable register(int opcode, PacketOperation op) {
        if (ops.putIfAbsent(opcode, op) != null) {
            throw new IllegalStateException("opcode already registered: 0x" + Integer.toHexString(opcode));
        }
        return this;
    }

    /** @return true when an operation handled the opcode; false leaves it to the caller. */
    public boolean dispatch(WorldSession session, World world, int opcode, WowBuffer in) {
        PacketOperation op = ops.get(opcode);
        if (op == null) {
            return false;
        }
        op.handle(session, world, in);
        return true;
    }

    public Set<Integer> opcodes() {
        return Collections.unmodifiableSet(ops.keySet());
    }

    /** The table {@link WorldSession} uses for logged-in opcodes. */
    public static OpcodeTable loggedIn() {
        OpcodeTable t = new OpcodeTable();
        QueryHandler.register(t);
        InventoryHandler.register(t);
        BinderHandler.register(t);
        TrainerHandler.register(t);
        ChannelHandler.register(t);
        LootHandler.register(t);
        GroupHandler.register(t);
        GuildHandler.register(t);
        AuctionHandler.register(t);
        LfgHandler.register(t);
        PetHandler.register(t);
        TalentHandler.register(t);
        InstanceHandler.register(t);
        BattlegroundHandler.register(t);
        GmTicketHandler.register(t);
        SpellCancelHandler.register(t);
        MountHandler.register(t);
        QuestShareHandler.register(t);
        return t;
    }
}
