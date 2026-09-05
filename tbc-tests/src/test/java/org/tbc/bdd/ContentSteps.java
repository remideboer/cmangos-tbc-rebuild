package org.tbc.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.tbc.common.WowBuffer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentSteps {
    private static final World.Account ACCOUNT =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble client;
    private long lastNpc;
    private int lastBindArea;
    private Creature wanderKobold;

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

    @Given("Kobold Vermin {int} has MovementType {int} and spawndist {int}")
    public void koboldRandomMotion(int entry, int movementType, int spawnDist) {
        wanderKobold = find(entry);
        Player p = client.session().player();
        p.relocate(wanderKobold.x + 40, wanderKobold.y, wanderKobold.z, wanderKobold.o);
        wanderKobold.movementType = movementType;
        wanderKobold.spawnDist = spawnDist;
        int[] n = {0};
        wanderKobold.motion.rng(() -> n[0]++ == 0 ? 0.0 : 1.0);
        wanderKobold.startOocMotion();
    }

    @When("one second elapses out of combat")
    public void oneSecondOoc() {
        client.clear();
        world.tick(1000);
    }

    @Then("SMSG_MONSTER_MOVE is a walk spline for that kobold within spawn distance")
    public void wanderSpline() {
        assertTrue(client.saw(Opcodes.SMSG_MONSTER_MOVE));
        WowBuffer move = new WowBuffer(client.payload(Opcodes.SMSG_MONSTER_MOVE));
        assertEquals(wanderKobold.guid, move.getPackedGuid());
        move.getFloat();
        move.getFloat();
        move.getFloat();
        move.getU32();
        move.getU8();
        move.getU32();
        move.getU32();
        move.getU32();
        float destX = move.getFloat();
        float destY = move.getFloat();
        assertTrue(wanderKobold.spawnDistance2d(destX, destY) <= wanderKobold.spawnDist + 0.01f);
    }

    @When("the player enters Goldshire")
    public void enterGoldshire() {
        Player p = client.session().player();
        p.relocate(Content.GOLDSHIRE_X, Content.GOLDSHIRE_Y, Content.GOLDSHIRE_Z, 0);
    }

    @When("the player talks to Corina Steele {int}")
    @When("the player talks to Deputy Willem {int}")
    @When("the player talks to Marshal McBride {int}")
    @When("the player talks to Llane Beshere {int}")
    @When("the player talks to Olivia Burnside {int}")
    @When("the player talks to Dungar Longdrink {int}")
    @When("the player talks to Innkeeper Farley {int}")
    @When("the player talks to Auctioneer Chilton {int}")
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

    @Then("SMSG_GOSSIP_MESSAGE has a vendor option")
    public void gossipHasVendorOption() {
        byte[] raw = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(raw);
        b.getU64();
        int menuId = b.getU32();
        int titleTextId = b.getU32();
        int gossipItemCount = b.getU32();
        assertTrue(gossipItemCount >= 1);
        boolean vendor = false;
        for (int i = 0; i < gossipItemCount; i++) {
            b.getU32();
            int icon = b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
            if (icon == Content.GOSSIP_ICON_VENDOR) {
                vendor = true;
            }
        }
        assertTrue(vendor);
        assertEquals(0, menuId);
        assertEquals(Content.DEFAULT_GOSSIP_MESSAGE, titleTextId);
    }

    @When("the player selects the vendor gossip option")
    public void selectVendorGossipOption() {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int vendorIndex = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            int icon = b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
            if (icon == Content.GOSSIP_ICON_VENDOR) {
                vendorIndex = index;
            }
        }
        assertTrue(vendorIndex >= 0);
        client.clear();
        client.gossipSelect(world, lastNpc, menuId, vendorIndex);
    }

    @When("the player selects the trainer gossip option")
    public void selectTrainerGossipOption() {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int trainerIndex = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            int icon = b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
            if (icon == Content.GOSSIP_ICON_TRAINER) {
                trainerIndex = index;
            }
        }
        assertTrue(trainerIndex >= 0);
        client.clear();
        client.gossipSelect(world, lastNpc, menuId, trainerIndex);
    }

    @When("the player selects the banker gossip option")
    public void selectBankerGossipOption() {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int bankerIndex = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            int icon = b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
            if (icon == Content.GOSSIP_ICON_MONEY_BAG) {
                bankerIndex = index;
            }
        }
        assertTrue(bankerIndex >= 0);
        client.clear();
        client.gossipSelect(world, lastNpc, menuId, bankerIndex);
    }

    @When("the player selects the taxi gossip option")
    public void selectTaxiGossipOption() {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int taxiIndex = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            int icon = b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
            if (icon == Content.GOSSIP_ICON_TAXI) {
                taxiIndex = index;
            }
        }
        assertTrue(taxiIndex >= 0);
        client.clear();
        client.gossipSelect(world, lastNpc, menuId, taxiIndex);
    }

    @When("the player selects the innkeeper gossip option")
    public void selectInnkeeperGossipOption() {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int innIndex = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            int icon = b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
            if (icon == Content.GOSSIP_ICON_INTERACT_2) {
                innIndex = index;
            }
        }
        assertTrue(innIndex >= 0);
        client.clear();
        client.gossipSelect(world, lastNpc, menuId, innIndex);
    }

    @Then("the server sends SMSG_LIST_INVENTORY for that vendor")
    public void sawVendorList() {
        byte[] raw = client.payload(Opcodes.SMSG_LIST_INVENTORY);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
        assertTrue((b.getU8() & 0xFF) >= 1);
    }

    @Then("the server sends SMSG_TRAINER_LIST for that trainer")
    public void sawTrainerList() {
        byte[] raw = client.payload(Opcodes.SMSG_TRAINER_LIST);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
        b.getU32();
        int count = b.getU32();
        assertTrue(count >= 1);
        assertEquals(Content.SPELL_BATTLE_SHOUT, b.getU32());
        assertEquals(0, b.getU8() & 0xFF);
        assertEquals(Content.TRAINER_SPELL_BATTLE_SHOUT_COST, b.getU32());
        b.getU32();
        b.getU32();
        b.getU8();
        b.getU32();
        b.getU32();
        b.getU32();
        b.getU32();
        b.getU32();
        for (int i = 1; i < count; i++) {
            b.getU32();
            b.getU8();
            b.getU32();
            b.getU32();
            b.getU32();
            b.getU8();
            b.getU32();
            b.getU32();
            b.getU32();
            b.getU32();
            b.getU32();
        }
        assertEquals("Hello! Ready for some training?", b.getCString());
    }

    @Then("the server sends SMSG_SHOW_BANK for that banker")
    public void sawShowBank() {
        byte[] raw = client.payload(Opcodes.SMSG_SHOW_BANK);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
    }

    @Then("the server sends SMSG_SHOWTAXINODES for that flight master")
    public void sawShowTaxiNodes() {
        byte[] raw = client.payload(Opcodes.SMSG_SHOWTAXINODES);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(1, b.getU32());
        assertEquals(lastNpc, b.getU64());
        assertEquals(Content.TAXI_STORMWIND, b.getU32());
        for (int i = 0; i < 16; i++) {
            b.getU32();
        }
        assertEquals(0, b.remaining());
    }

    @Then("the server sends SMSG_BINDER_CONFIRM for that innkeeper")
    public void sawBinderConfirm() {
        assertTrue(client.saw(Opcodes.SMSG_GOSSIP_COMPLETE));
        byte[] raw = client.payload(Opcodes.SMSG_BINDER_CONFIRM);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
    }

    @When("the player selects the auctioneer gossip option")
    public void selectAuctioneerGossipOption() {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int ahIndex = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            int icon = b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
            if (icon == Content.GOSSIP_ICON_MONEY_BAG) {
                ahIndex = index;
            }
        }
        assertTrue(ahIndex >= 0);
        client.clear();
        client.gossipSelect(world, lastNpc, menuId, ahIndex);
    }

    @Then("the server sends MSG_AUCTION_HELLO for that auctioneer")
    public void sawAuctionHello() {
        byte[] raw = client.payload(Opcodes.MSG_AUCTION_HELLO);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
        assertEquals(Content.AUCTION_HOUSE_HUMAN, b.getU32());
        assertEquals(0, b.remaining());
    }

    @When("the player selects the gossip option {string}")
    public void selectGossipOptionByText(String optionText) {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int chosen = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            b.getU8();
            b.getU8();
            b.getU32();
            String text = b.getCString();
            b.getCString();
            if (optionText.equals(text)) {
                chosen = index;
            }
        }
        assertTrue(chosen >= 0);
        client.clear();
        client.gossipSelect(world, lastNpc, menuId, chosen);
    }

    @Given("Innkeeper Farley has a gossip option that closes the menu")
    public void farleyCloseGossipOption() {
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                world.objectMgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 98, 0, "Goodbye",
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", -1));
        world.objectMgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
    }

    @Given("Innkeeper Farley has a gossip option that sends the Lion's Pride Inn POI")
    public void farleyLionsPridePoiOption() {
        ObjectMgr.PointOfInterest poi = ObjectMgr.lionsPrideInnPoi();
        world.objectMgr.pointsOfInterest.put(poi.entry(), poi);
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                world.objectMgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 97, 0, poi.iconName(),
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", 0, poi.entry()));
        world.objectMgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
    }

    @Then("SMSG_GOSSIP_POI is Lion's Pride Inn")
    public void sawLionsPrideGossipPoi() {
        ObjectMgr.PointOfInterest poi = ObjectMgr.lionsPrideInnPoi();
        assertTrue(client.saw(Opcodes.SMSG_GOSSIP_POI));
        assertEquals(Opcodes.SMSG_GOSSIP_POI, client.opcodes.get(0));
        WowBuffer b = new WowBuffer(client.payload(Opcodes.SMSG_GOSSIP_POI));
        assertEquals(poi.flags(), b.getU32());
        assertEquals(poi.x(), b.getFloat(), 0.0001f);
        assertEquals(poi.y(), b.getFloat(), 0.0001f);
        assertEquals(poi.icon(), b.getU32());
        assertEquals(poi.data(), b.getU32());
        assertEquals(poi.iconName(), b.getCString());
        assertEquals(0, b.remaining());
    }

    @Given("Innkeeper Farley has a gossip option gated by a SQL condition")
    public void farleyConditionedGossipOption() {
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                world.objectMgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 95, 0, "Locked",
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", 0, 0, 1));
        world.objectMgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
    }

    @Then("SMSG_GOSSIP_MESSAGE does not list {string}")
    public void gossipMessageDoesNotList(String optionText) {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        for (int i = 0; i < gossipItemCount; i++) {
            b.getU32();
            b.getU8();
            b.getU8();
            b.getU32();
            String text = b.getCString();
            b.getCString();
            assertFalse(optionText.equals(text));
        }
    }

    @Then("the server sends empty SMSG_GOSSIP_COMPLETE")
    public void emptyGossipComplete() {
        assertTrue(client.saw(Opcodes.SMSG_GOSSIP_COMPLETE));
        assertEquals(0, client.payload(Opcodes.SMSG_GOSSIP_COMPLETE).length);
    }

    @Then("the server does not send a new SMSG_GOSSIP_MESSAGE")
    public void noNewGossipMessage() {
        assertFalse(client.saw(Opcodes.SMSG_GOSSIP_MESSAGE));
    }

    @Then("SMSG_GOSSIP_MESSAGE is menu {int} with text {int} and no gossip options")
    public void sawNestedGossipMenu(int menuId, int titleTextId) {
        byte[] raw = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
        assertEquals(menuId, b.getU32());
        assertEquals(titleTextId, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.remaining());
    }

    @When("the player confirms the inn bind")
    public void confirmInnBind() {
        client.clear();
        client.binderActivate(world, lastNpc);
    }

    @Then("the server sends SMSG_TRAINER_BUY_SUCCEEDED for bind spell {int}")
    public void sawTrainerBuySucceededBind(int spellId) {
        assertEquals(3286, spellId);
        assertTrue(client.saw(Opcodes.SMSG_GOSSIP_COMPLETE));
        byte[] raw = client.payload(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
        assertEquals(spellId, b.getU32());
        assertEquals(0, b.remaining());
    }

    @Then("SMSG_BINDPOINTUPDATE is the player's current location")
    public void sawBindPointUpdate() {
        Player p = client.session().player();
        byte[] raw = client.payload(Opcodes.SMSG_BINDPOINTUPDATE);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(p.x, b.getFloat());
        assertEquals(p.y, b.getFloat());
        assertEquals(p.z, b.getFloat());
        assertEquals(p.mapId, b.getU32());
        lastBindArea = b.getU32();
        assertEquals(0, b.remaining());
    }

    @Then("SMSG_PLAYERBOUND is that innkeeper")
    public void sawPlayerBound() {
        byte[] raw = client.payload(Opcodes.SMSG_PLAYERBOUND);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(lastNpc, b.getU64());
        assertEquals(lastBindArea, b.getU32());
        assertEquals(0, b.remaining());
    }

    @When("the player queries that gossip NPC text")
    public void queryGossipNpcText() {
        byte[] gossip = client.payload(Opcodes.SMSG_GOSSIP_MESSAGE);
        WowBuffer b = new WowBuffer(gossip);
        b.getU64();
        b.getU32();
        int textId = b.getU32();
        client.clear();
        client.npcTextQuery(world, textId, lastNpc);
    }

    @Then("SMSG_NPC_TEXT_UPDATE has {int} greeting slots")
    public void npcTextDefaultGreetings(int slots) {
        byte[] raw = client.payload(Opcodes.SMSG_NPC_TEXT_UPDATE);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(Content.DEFAULT_GOSSIP_MESSAGE, b.getU32());
        for (int i = 0; i < slots; i++) {
            assertEquals(0f, b.getFloat());
            assertEquals(Content.DEFAULT_NPC_TEXT, b.getCString());
            assertEquals(Content.DEFAULT_NPC_TEXT, b.getCString());
            assertEquals(0, b.getU32());
            for (int e = 0; e < 6; e++) {
                assertEquals(0, b.getU32());
            }
        }
        assertEquals(0, b.remaining());
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

    @Then("the server sends SMSG_QUESTGIVER_QUEST_COMPLETE for quest {int}")
    public void sawCompleteFor(int questId) {
        byte[] p = client.payload(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE);
        assertTrue(p.length >= 4);
        assertEquals(questId, WowClientDouble.u32le(p, 0));
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
