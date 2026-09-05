package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PetitionHandlerTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void buyWhenPayloadShortShouldStaySilent() {
        World world = World.inMemory();
        Sink sink = login(world, "Short");
        PetitionHandler.buy(sink.session, world, new WowBuffer(new byte[4]));
        assertFalse(sink.ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        assertFalse(sink.ops.contains(Opcodes.SMSG_BUY_FAILED));
    }

    @Test
    void buyWhenNotPetitionerShouldStaySilent() {
        World world = World.inMemory();
        Sink sink = login(world, "Visitor");
        Player p = sink.session.player();
        p.setMoney(Content.GUILD_CHARTER_COST);
        Creature banker = find(world, Content.NPC_OLIVIA_BURNSIDE);
        p.relocate(banker.x, banker.y, banker.z, banker.o);
        sink.ops.clear();
        PetitionHandler.buy(sink.session, world, buyPacket(banker.guid, "Nope"));
        assertFalse(sink.ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
    }

    @Test
    void buyWhenBrokeShouldSendBuyFailed() {
        World world = World.inMemory();
        Sink sink = login(world, "Broke");
        Player p = sink.session.player();
        p.setMoney(0);
        Creature npc = find(world, Content.NPC_REBECCA_LAUGHLIN);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        sink.ops.clear();
        PetitionHandler.buy(sink.session, world, buyPacket(npc.guid, "PoorGuild"));
        byte[] fail = sink.last.get(Opcodes.SMSG_BUY_FAILED);
        WowBuffer b = new WowBuffer(fail);
        assertEquals(npc.guid, b.getU64());
        assertEquals(Content.ITEM_GUILD_CHARTER, b.getU32());
        assertEquals(PetitionHandler.BUY_ERR_NOT_ENOUGHT_MONEY, b.getU8());
    }

    @Test
    void buyWhenAlreadyInGuildShouldStaySilent() {
        World world = World.inMemory();
        Sink sink = login(world, "Member");
        Player p = sink.session.player();
        p.setMoney(Content.GUILD_CHARTER_COST);
        GuildHandler.create(sink.session, world, cstring("Existing"));
        Creature npc = find(world, Content.NPC_REBECCA_LAUGHLIN);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        sink.ops.clear();
        PetitionHandler.buy(sink.session, world, buyPacket(npc.guid, "Second"));
        assertFalse(sink.ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
    }

    @Test
    void buyWhenGuildNameTakenShouldSendCommandResult() {
        World world = World.inMemory();
        Sink owner = login(world, "Owner");
        GuildHandler.create(owner.session, world, cstring("Taken"));
        Sink buyer = login(world, "Buyer");
        Player p = buyer.session.player();
        p.setMoney(Content.GUILD_CHARTER_COST);
        Creature npc = find(world, Content.NPC_REBECCA_LAUGHLIN);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        buyer.ops.clear();
        PetitionHandler.buy(buyer.session, world, buyPacket(npc.guid, "Taken"));
        WowBuffer r = new WowBuffer(buyer.last.get(Opcodes.SMSG_GUILD_COMMAND_RESULT));
        assertEquals(GuildHandler.GUILD_CREATE_S, r.getU32());
        assertEquals("Taken", r.getCString());
        assertEquals(PetitionHandler.ERR_GUILD_NAME_EXISTS_S, r.getU32());
    }

    @Test
    void signWhenOwnerShouldStaySilent() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player p = sink.session.player();
        p.setMoney(Content.GUILD_CHARTER_COST);
        Creature npc = find(world, Content.NPC_REBECCA_LAUGHLIN);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        PetitionHandler.buy(sink.session, world, buyPacket(npc.guid, "OwnSign"));
        long petition = charterGuid(p);
        sink.ops.clear();
        WowBuffer sign = new WowBuffer(9);
        sign.putU64(petition);
        sign.putU8(0);
        PetitionHandler.sign(sink.session, world, new WowBuffer(sign.array()));
        assertFalse(sink.ops.contains(Opcodes.SMSG_PETITION_SIGN_RESULTS));
    }

    @Test
    void signWhenAlreadySignedShouldSendAlreadySigned() {
        World world = World.inMemory();
        Sink owner = login(world, "Lead");
        Player op = owner.session.player();
        op.setMoney(Content.GUILD_CHARTER_COST);
        Creature npc = find(world, Content.NPC_REBECCA_LAUGHLIN);
        op.relocate(npc.x, npc.y, npc.z, npc.o);
        PetitionHandler.buy(owner.session, world, buyPacket(npc.guid, "Twice"));
        long petition = charterGuid(op);
        World.Account accB = new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");
        Sink signer = login(world, accB, "Ink");
        signer.session.player().relocate(npc.x, npc.y, npc.z, npc.o);
        WowBuffer first = new WowBuffer(9);
        first.putU64(petition);
        first.putU8(0);
        PetitionHandler.sign(signer.session, world, new WowBuffer(first.array()));
        signer.ops.clear();
        WowBuffer again = new WowBuffer(9);
        again.putU64(petition);
        again.putU8(0);
        PetitionHandler.sign(signer.session, world, new WowBuffer(again.array()));
        WowBuffer r = new WowBuffer(signer.last.get(Opcodes.SMSG_PETITION_SIGN_RESULTS));
        r.getU64();
        r.getU64();
        assertEquals(PetitionHandler.PETITION_SIGN_ALREADY_SIGNED, r.getU32());
    }

    @Test
    void turnInWhenNeedMoreSignaturesShouldSendNeedMore() {
        World world = World.inMemory();
        Sink sink = login(world, "Need");
        Player p = sink.session.player();
        p.setMoney(Content.GUILD_CHARTER_COST);
        Creature npc = find(world, Content.NPC_REBECCA_LAUGHLIN);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        PetitionHandler.buy(sink.session, world, buyPacket(npc.guid, "Wait"));
        long petition = charterGuid(p);
        sink.ops.clear();
        WowBuffer turn = new WowBuffer(8);
        turn.putU64(petition);
        PetitionHandler.turnIn(sink.session, world, new WowBuffer(turn.array()));
        assertEquals(PetitionHandler.PETITION_TURN_NEED_MORE_SIGNATURES,
                new WowBuffer(sink.last.get(Opcodes.SMSG_TURN_IN_PETITION_RESULTS)).getU32());
    }

    @Test
    void turnInWhenMissingPetitionShouldReturnFalse() {
        World world = World.inMemory();
        Sink sink = login(world, "Arena");
        WowBuffer turn = new WowBuffer(8);
        turn.putU64(0);
        assertFalse(PetitionHandler.turnIn(sink.session, world, new WowBuffer(turn.array())));
    }

    private static long charterGuid(Player p) {
        for (org.tbc.world.entity.Item it : p.items.values()) {
            if (it.entry == Content.ITEM_GUILD_CHARTER) {
                return it.guid;
            }
        }
        throw new AssertionError("no guild charter");
    }

    private static WowBuffer buyPacket(long npcGuid, String name) {
        WowBuffer b = new WowBuffer(96 + name.length());
        b.putU64(npcGuid);
        b.putU32(0);
        b.putU64(0);
        b.putCString(name);
        for (int i = 0; i < 10; i++) {
            b.putU32(0);
        }
        b.putU16(0);
        b.putU8(0);
        b.putU32(1);
        b.putU32(0);
        return new WowBuffer(b.array());
    }

    private static WowBuffer cstring(String name) {
        WowBuffer b = new WowBuffer(name.length() + 1);
        b.putCString(name);
        return new WowBuffer(b.array());
    }

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        throw new AssertionError("no creature " + entry);
    }

    private static Sink login(World world, String name) {
        return login(world, ACC, name);
    }

    private static Sink login(World world, World.Account acc, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final class Sink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();
        final Map<Integer, byte[]> last = new HashMap<>();
        WorldSession session;

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
            last.put(opcode, payload);
        }

        @Override
        public void close() {
        }
    }
}
