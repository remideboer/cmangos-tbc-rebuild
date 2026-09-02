package org.tbc.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentSteps {
    private static final World.Account ACCOUNT =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble client;
    private long lastNpc;

    @Given("a logged-in character in Elwynn")
    public void loggedInElwynn() {
        world = World.inMemory();
        client = new WowClientDouble();
        client.connect(ACCOUNT);
        Player created = world.characters.create(ACCOUNT.id(), "SliceEight", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
    }

    @Given("the player has {int} copper")
    public void hasCopper(int copper) {
        client.session().player().setMoney(copper);
    }

    @When("the player enters Goldshire")
    public void enterGoldshire() {
        Player p = client.session().player();
        p.relocate(Content.GOLDSHIRE_X, Content.GOLDSHIRE_Y, Content.GOLDSHIRE_Z, 0);
    }

    @When("the player talks to Corina Steele {int}")
    @When("the player talks to Deputy Willem {int}")
    @When("the player talks to Marshal McBride {int}")
    public void talksTo(int entry) {
        Creature npc = find(entry);
        lastNpc = npc.guid;
        Player p = client.session().player();
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        client.clear();
        client.gossipHello(world, npc.guid);
    }

    @When("the player buys item {int} from that vendor")
    public void buyFromVendor(int itemId) {
        client.clear();
        client.buyItem(world, lastNpc, itemId, 1);
    }

    @When("the player buys item {int} from Kobold Vermin {int}")
    public void buyFromKobold(int itemId, int entry) {
        Creature npc = find(entry);
        Player p = client.session().player();
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        client.clear();
        client.buyItem(world, npc.guid, itemId, 1);
    }

    @When("the player accepts quest {int}")
    public void acceptQuest(int questId) {
        client.clear();
        client.acceptQuest(world, lastNpc, questId);
    }

    @When("the player turns in quest {int}")
    public void turnIn(int questId) {
        client.clear();
        client.completeQuest(world, lastNpc, questId);
    }

    @When("the mock client sends CMSG_GOSSIP_HELLO with fewer than {int} bytes")
    public void truncatedGossip(int n) {
        assertEquals(8, n);
        client.clear();
        client.handle(world, Opcodes.CMSG_GOSSIP_HELLO, new byte[3]);
    }

    @Then("Marshal Dughan {int} is on map {int}")
    public void dughanOnMap(int entry, int mapId) {
        assertEquals(Content.NPC_MARSHAL_DUGHAN, entry);
        assertEquals(0, mapId);
        boolean found = false;
        for (Creature c : world.map(mapId, 0).creatures.values()) {
            if (c.entry == entry) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Then("the server sends SMSG_GOSSIP_MESSAGE")
    public void sawGossip() {
        assertTrue(client.saw(Opcodes.SMSG_GOSSIP_MESSAGE));
    }

    @Then("bags contain item {int}")
    public void bagsContain(int entry) {
        assertTrue(client.session().player().items.values().stream().anyMatch(it -> it.entry == entry));
    }

    @Then("bags do not contain item {int}")
    public void bagsDoNotContain(int entry) {
        assertFalse(client.session().player().items.values().stream().anyMatch(it -> it.entry == entry));
    }

    @Then("gold is {int} copper")
    public void goldIs(int copper) {
        assertEquals(copper, client.session().player().money);
    }

    @Then("quest {int} is in the quest log")
    public void questInLog(int questId) {
        assertTrue(hasQuest(questId));
    }

    @Then("quest {int} is not in the quest log")
    public void questNotInLog(int questId) {
        assertFalse(hasQuest(questId));
    }

    @Then("the server sends SMSG_QUESTGIVER_QUEST_COMPLETE")
    public void sawComplete() {
        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
    }

    @Then("the server does not send SMSG_QUESTGIVER_QUEST_COMPLETE")
    public void noComplete() {
        assertFalse(client.saw(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
    }

    @Then("the server sends SMSG_INVENTORY_CHANGE_FAILURE with result {int}")
    public void equipErr(int result) {
        byte[] p = client.payload(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE);
        assertTrue(p.length >= 1);
        assertEquals(result, p[0] & 0xFF);
    }

    @Then("the content session still answers CMSG_PING with SMSG_PONG")
    public void pong() {
        client.clear();
        client.ping(world, 11);
        assertTrue(client.saw(Opcodes.SMSG_PONG));
        assertEquals(11, WowClientDouble.u32le(client.payload(Opcodes.SMSG_PONG), 0));
    }

    private boolean hasQuest(int questId) {
        int[] log = client.session().player().questLogId;
        for (int id : log) {
            if (id == questId) {
                return true;
            }
        }
        return false;
    }

    private Creature find(int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        throw new IllegalStateException("no creature " + entry);
    }
}
