package org.tbc.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventAiSteps {
    private static final World.Account ACCOUNT =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble client;
    private Creature garrick;

    @Given("a logged-in character standing next to Garrick Padfoot {int}")
    public void nextToGarrick(int entry) {
        world = World.inMemory();
        client = new WowClientDouble();
        client.connect(ACCOUNT);
        Player created = world.characters.create(ACCOUNT.id(), "Fighter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        garrick = find(entry);
        Player p = client.session().player();
        p.relocate(garrick.x, garrick.y, garrick.z, garrick.o);
        world.map(p.mapId, p.instanceId).add(p);
    }

    @When("the player aggros Garrick Padfoot")
    public void aggro() {
        client.clear();
        client.attackSwing(world, garrick.guid);
    }

    @Then("the server sends SMSG_SPELL_START for spell {int}")
    public void spellStart(int spell) {
        assertTrue(client.saw(Opcodes.SMSG_SPELL_START));
        assertEquals(spell, spellId(client.payload(Opcodes.SMSG_SPELL_START)));
    }

    @And("the server sends SMSG_SPELL_GO for spell {int}")
    public void spellGo(int spell) {
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(spell, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    @When("a creature with missing ScriptName is spawned")
    public void missingScript() {
        world.scripts.create("missing_script_name_not_in_spec");
    }

    @Then("the eventai session still answers CMSG_PING with SMSG_PONG")
    public void pong() {
        client.clear();
        client.ping(world, 3);
        assertTrue(client.saw(Opcodes.SMSG_PONG));
        assertEquals(3, WowClientDouble.u32le(client.payload(Opcodes.SMSG_PONG), 0));
    }

    private Creature find(int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        throw new AssertionError("no creature " + entry);
    }

    private static int spellId(byte[] p) {
        int off = WowClientDouble.skipPackedGuid(p, 0);
        off = WowClientDouble.skipPackedGuid(p, off);
        return WowClientDouble.u32le(p, off);
    }
}
