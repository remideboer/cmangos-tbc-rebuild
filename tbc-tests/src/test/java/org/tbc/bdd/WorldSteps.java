package org.tbc.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorldSteps {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble alpha;
    private WowClientDouble bravo;

    @Given("two logged-in characters in visibility named Alpha and Bravo")
    public void twoInVisibility() {
        world = World.inMemory();
        alpha = new WowClientDouble();
        bravo = new WowClientDouble();
        alpha.connect(ACC_A);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC_A.id(), "Alpha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(ACC_B.id(), "Bravo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);
    }

    @When("Alpha heartbeats one yard east")
    public void alphaHeartbeat() {
        Player a = alpha.session().player();
        alpha.clear();
        bravo.clear();
        alpha.heartbeat(world, a.x + 1, a.y, a.z, a.o);
    }

    @Then("Bravo received Alpha movement with packed GUID and fallTime")
    public void bravoSawMove() {
        assertTrue(bravo.saw(Opcodes.MSG_MOVE_HEARTBEAT));
        WowBuffer b = new WowBuffer(bravo.payload(Opcodes.MSG_MOVE_HEARTBEAT));
        assertEquals(alpha.session().player().guid, b.getPackedGuid());
        b.getU32();
        b.getU8();
        b.getU32();
        b.getFloat();
        b.getFloat();
        b.getFloat();
        b.getFloat();
        assertTrue(b.remaining() >= 4);
        b.getU32();
    }

    @And("Alpha did not receive a movement echo")
    public void alphaNoEcho() {
        assertFalse(alpha.saw(Opcodes.MSG_MOVE_HEARTBEAT));
    }

    @When("Alpha says test")
    public void alphaSays() {
        bravo.clear();
        alpha.clear();
        alpha.say(world, "test");
    }

    @Then("Bravo received CHAT_MSG_SAY")
    public void bravoSay() {
        assertTrue(bravo.saw(Opcodes.SMSG_MESSAGECHAT));
        assertEquals(0x01, bravo.payload(Opcodes.SMSG_MESSAGECHAT)[0] & 0xFF);
    }

    @Then("Bravo saw Alpha's model without UPDATEFLAG_SELF")
    public void bravoSawAlphaCreate() {
        assertTrue(sawObserverPlayerCreate(bravo));
    }

    @And("Alpha saw Bravo's model without UPDATEFLAG_SELF")
    public void alphaSawBravoCreate() {
        assertTrue(sawObserverPlayerCreate(alpha));
    }

    @When("Alpha queries Bravo's name")
    public void alphaNameQuery() {
        alpha.clear();
        alpha.nameQuery(world, bravo.session().player().guid);
    }

    @Then("Alpha received Bravo's name")
    public void alphaGotName() {
        assertTrue(alpha.saw(Opcodes.SMSG_NAME_QUERY_RESPONSE));
        WowBuffer b = new WowBuffer(alpha.payload(Opcodes.SMSG_NAME_QUERY_RESPONSE));
        assertEquals(bravo.session().player().guid, b.getU64());
        assertEquals("Bravo", b.getCString());
    }

    @When("Alpha whispers Bravo hello")
    public void alphaWhisper() {
        alpha.clear();
        bravo.clear();
        alpha.whisper(world, "Bravo", "hello");
    }

    @Then("Bravo received CHAT_MSG_WHISPER")
    public void bravoWhisper() {
        assertTrue(bravo.saw(Opcodes.SMSG_MESSAGECHAT));
        assertEquals(0x07, bravo.payload(Opcodes.SMSG_MESSAGECHAT)[0] & 0xFF);
    }

    @And("Alpha received CHAT_MSG_WHISPER_INFORM")
    public void alphaInform() {
        assertTrue(alpha.saw(Opcodes.SMSG_MESSAGECHAT));
        assertEquals(0x09, alpha.payload(Opcodes.SMSG_MESSAGECHAT)[0] & 0xFF);
    }

    @Given("Bravo stands {int} yards from Alpha")
    public void bravoYards(int yards) {
        Player a = alpha.session().player();
        bravo.session().player().relocate(a.x + yards, a.y, a.z, a.o);
    }

    @Then("Bravo did not receive CHAT_MSG_SAY")
    public void bravoNoSay() {
        assertFalse(bravo.saw(Opcodes.SMSG_MESSAGECHAT));
    }

    private static boolean sawObserverPlayerCreate(WowClientDouble d) {
        for (int i = 0; i < d.opcodes.size(); i++) {
            byte[] raw = uncompressedUpdate(d.opcodes.get(i), d.payloads.get(i));
            if (raw == null) {
                continue;
            }
            try {
                WowBuffer b = new WowBuffer(raw);
                int blocks = b.getU32();
                b.getU8();
                for (int n = 0; n < blocks && b.remaining() > 2; n++) {
                    int ut = b.getU8();
                    if (ut != 2 && ut != 3) {
                        break;
                    }
                    b.getPackedGuid();
                    int typeId = b.getU8();
                    int flags = b.getU8();
                    if (typeId == Unit.TYPEID_PLAYER && (flags & Unit.UPDATEFLAG_SELF) == 0) {
                        return true;
                    }
                    break;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    private static byte[] uncompressedUpdate(int opcode, byte[] payload) {
        if (opcode == Opcodes.SMSG_UPDATE_OBJECT) {
            return payload;
        }
        if (opcode != Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT || payload.length < 4) {
            return null;
        }
        WowBuffer in = new WowBuffer(payload);
        int n = in.getU32();
        if (n <= 0 || n > 1_000_000) {
            return null;
        }
        Inflater inf = new Inflater();
        inf.setInput(in.remainingBytes());
        byte[] out = new byte[n];
        try {
            int got = 0;
            while (got < n && !inf.finished()) {
                int k = inf.inflate(out, got, n - got);
                if (k == 0) {
                    break;
                }
                got += k;
            }
            return got == n ? out : null;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inf.end();
        }
    }
}
