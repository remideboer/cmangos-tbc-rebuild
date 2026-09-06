package org.tbc.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstanceSteps {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble alpha;
    private WowClientDouble bravo;
    private int boundInstance;

    @Given("two grouped characters in visibility named Alpha and Bravo")
    public void twoGrouped() {
        world = World.inMemory();
        alpha = new WowClientDouble();
        bravo = new WowClientDouble();
        alpha.connect(ACC_A);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC_A.id(), "Alpha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(ACC_B.id(), "Bravo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);
        alpha.groupInvite(world, "Bravo");
        bravo.groupAccept(world);
    }

    @When("both enter Ragefire trigger {int}")
    public void bothEnter(int trigger) {
        alpha.clear();
        bravo.clear();
        alpha.areaTrigger(world, trigger);
        alpha.worldportAck(world);
        bravo.areaTrigger(world, trigger);
        bravo.worldportAck(world);
        boundInstance = alpha.session().player().instanceId;
    }

    @Then("both are on map {int} with the same instance id")
    public void sameInstance(int map) {
        assertEquals(map, alpha.session().player().mapId);
        assertEquals(map, bravo.session().player().mapId);
        assertEquals(alpha.session().player().instanceId, bravo.session().player().instanceId);
        assertTrue(alpha.session().player().instanceId != 0);
    }

    @And("both received SMSG_NEW_WORLD for map {int}")
    public void bothNewWorld(int map) {
        assertTrue(alpha.saw(Opcodes.SMSG_NEW_WORLD));
        assertTrue(bravo.saw(Opcodes.SMSG_NEW_WORLD));
        assertEquals(map, WowClientDouble.u32le(alpha.payload(Opcodes.SMSG_NEW_WORLD), 0));
        assertEquals(map, WowClientDouble.u32le(bravo.payload(Opcodes.SMSG_NEW_WORLD), 0));
    }

    @And("they leave to Elwynn and re-enter Ragefire")
    public void leaveReenter() {
        leaveElwynn();
        bothEnter(2230);
        assertEquals(boundInstance, alpha.session().player().instanceId);
    }

    @When("they leave to Elwynn")
    public void leaveElwynn() {
        world.gm.handle(world, alpha.session().player(), ".tele -8949 -132 83 0");
        world.gm.handle(world, bravo.session().player(), ".tele -8949 -132 83 0");
        alpha.session().player().instanceId = 0;
        bravo.session().player().instanceId = 0;
    }

    @When("Alpha resets instances")
    public void reset() {
        alpha.clear();
        alpha.resetInstances(world);
    }

    @Then("Alpha received SMSG_INSTANCE_RESET for map {int}")
    public void resetOk(int map) {
        assertTrue(alpha.saw(Opcodes.SMSG_INSTANCE_RESET));
        assertEquals(map, WowClientDouble.u32le(alpha.payload(Opcodes.SMSG_INSTANCE_RESET), 0));
    }

    @When("Alpha joins the WSG queue")
    public void joinWsg() {
        alpha.clear();
        alpha.battlemasterJoin(world);
    }

    @Then("Alpha received WAIT_JOIN status {int}")
    public void waitJoin(int status) {
        assertTrue(alpha.saw(Opcodes.SMSG_BATTLEFIELD_STATUS));
        byte[] p = alpha.payload(Opcodes.SMSG_BATTLEFIELD_STATUS);
        assertEquals(status, WowClientDouble.u32le(p, 17));
        assertEquals(489, WowClientDouble.u32le(p, 21));
        assertEquals(0, alpha.session().player().mapId);
    }

    @When("Alpha ports into the battleground")
    public void port() {
        alpha.clear();
        alpha.battlefieldPort(world, 1);
        alpha.worldportAck(world);
    }

    @Then("Alpha is on map {int}")
    public void onMap(int map) {
        assertEquals(map, alpha.session().player().mapId);
    }

    @And("Alpha received SMSG_NEW_WORLD for map {int}")
    public void alphaNewWorld(int map) {
        assertTrue(alpha.saw(Opcodes.SMSG_NEW_WORLD));
        assertEquals(map, WowClientDouble.u32le(alpha.payload(Opcodes.SMSG_NEW_WORLD), 0));
    }

    @Given("Alpha is level {int}")
    public void alphaLevel(int level) {
        alpha.session().player().level = level;
        // Solo difficulty path (Background starts grouped).
        alpha.session().player().group = null;
        bravo.session().player().group = null;
    }

    @When("Alpha sets dungeon difficulty to {int}")
    public void alphaSetDifficulty(int mode) {
        alpha.clear();
        alpha.setDungeonDifficulty(world, mode);
    }

    @Then("Alpha difficulty is {int}")
    public void alphaDifficulty(int mode) {
        assertEquals(mode, alpha.session().player().difficulty);
    }

    @And("Alpha received MSG_SET_DUNGEON_DIFFICULTY mode {int} inGroup {int}")
    public void alphaDifficultyEcho(int mode, int inGroup) {
        byte[] p = alpha.payload(Opcodes.MSG_SET_DUNGEON_DIFFICULTY);
        assertEquals(12, p.length);
        assertEquals(mode, WowClientDouble.u32le(p, 0));
        assertEquals(1, WowClientDouble.u32le(p, 4));
        assertEquals(inGroup, WowClientDouble.u32le(p, 8));
    }

    @Given("Alpha is a non-GM player")
    public void alphaNonGm() {
        alpha.session().player().gmLevel = 0;
        alpha.session().player().group = null;
        bravo.session().player().group = null;
    }

    @When("Alpha enters five new Ragefire instances then leaves each")
    public void fiveEnters() {
        Player p = alpha.session().player();
        for (int i = 0; i < 5; i++) {
            alpha.clear();
            alpha.areaTrigger(world, 2230);
            alpha.worldportAck(world);
            assertEquals(389, p.mapId);
            world.teleport(p, 0, -8949f, -132f, 83f, 0f);
            p.instanceId = 0;
        }
        assertEquals(5, p.enteredInstances.size());
    }

    @When("Alpha enters Ragefire trigger {int} again")
    public void alphaEnterAgain(int trigger) {
        alpha.clear();
        alpha.areaTrigger(world, trigger);
    }

    @Then("Alpha received SMSG_TRANSFER_ABORTED for map {int} reason {int}")
    public void transferAborted(int map, int reason) {
        assertTrue(alpha.saw(Opcodes.SMSG_TRANSFER_ABORTED));
        byte[] p = alpha.payload(Opcodes.SMSG_TRANSFER_ABORTED);
        assertEquals(5, p.length);
        assertEquals(map, WowClientDouble.u32le(p, 0));
        assertEquals(reason, p[4] & 0xFF);
    }

    @When("Alpha leaves the battlefield type {int}")
    public void leaveBattlefield(int bgTypeId) {
        alpha.clear();
        alpha.leaveBattlefield(world, bgTypeId);
    }
}
