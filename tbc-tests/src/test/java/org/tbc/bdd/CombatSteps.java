package org.tbc.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CombatSteps {
    private static final World.Account ACCOUNT =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble client;
    private Creature kobold;

    @Given("a logged-in character standing next to Kobold Vermin {int}")
    public void nextToKobold(int entry) {
        world = World.inMemory();
        client = new WowClientDouble();
        client.connect(ACCOUNT);
        Player created = world.characters.create(ACCOUNT.id(), "Fighter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        kobold = find(entry);
        Player p = client.session().player();
        p.relocate(kobold.x, kobold.y, kobold.z, kobold.o);
        world.map(p.mapId, p.instanceId).add(p);
    }

    @And("the player has an offhand weapon")
    public void equipOffhandWeapon() {
        Player p = client.session().player();
        Item off = new Item(world.nextItemGuid(), 25);
        off.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        p.items.put((int) off.guid, off);
        assertTrue(p.hasOffhandWeapon());
    }

    @And("the player's offhand attack time is {int} ms")
    public void setOffhandAttackTime(int ms) {
        client.session().player().setInt(
                org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1, ms);
    }

    @And("the player's combat reach is {int} yards")
    public void setCombatReach(int yards) {
        client.session().player().setFloat(
                org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_COMBATREACH, yards);
    }

    @When("the attacker is {int} yards from the kobold")
    public void attackerYardsFromKobold(int yards) {
        Player p = client.session().player();
        p.relocate(kobold.x + yards, kobold.y, kobold.z, kobold.o);
    }

    @When("the player starts auto-attack")
    public void startAutoAttack() {
        client.clear();
        client.attackSwing(world, kobold.guid);
    }

    @When("{int} ms elapse on the combat session")
    public void elapseCombatSession(int ms) {
        client.clear();
        world.advanceMs(ms);
        client.session().tick(world, ms);
    }

    @Then("no SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING")
    public void noLeftSwing() {
        assertFalse(sawLeftSwing());
    }

    @Then("a SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING")
    public void hasLeftSwing() {
        assertTrue(sawLeftSwing());
    }

    @When("the player auto-attacks until the kobold is dead")
    public void attackUntilDead() {
        client.clear();
        client.attackSwing(world, kobold.guid);
        int n = 0;
        while (kobold.alive() && n++ < 80) {
            world.meleeHit(client.session().player(), kobold);
        }
        assertFalse(kobold.alive());
    }

    @Then("the server has sent SMSG_ATTACKERSTATEUPDATE")
    public void sawAttackerState() {
        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        byte[] p = client.payload(Opcodes.SMSG_ATTACKERSTATEUPDATE);
        assertTrue(p.length > 8);
        int hitInfo = WowClientDouble.u32le(p, 0);
        assertTrue((hitInfo & (org.tbc.world.combat.Combat.HITINFO_NORMALSWING2
                | org.tbc.world.combat.Combat.HITINFO_LEFTSWING
                | org.tbc.world.combat.Combat.HITINFO_MISS
                | org.tbc.world.combat.Combat.HITINFO_CRITICALHIT
                | org.tbc.world.combat.Combat.HITINFO_BLOCK
                | org.tbc.world.combat.Combat.HITINFO_GLANCING
                | org.tbc.world.combat.Combat.HITINFO_CRUSHING
                | org.tbc.world.combat.Combat.HITINFO_NOACTION
                | org.tbc.world.combat.Combat.HITINFO_SWINGNOHITSOUND)) != 0
                || hitInfo == 0);
    }

    @When("the player loots the corpse")
    public void lootCorpse() {
        client.clear();
        client.loot(world, kobold.guid);
    }

    @Then("SMSG_LOOT_RESPONSE is a corpse window for that guid")
    public void lootWindow() {
        byte[] p = client.payload(Opcodes.SMSG_LOOT_RESPONSE);
        assertTrue(p.length >= 14);
        assertEquals(kobold.guid, WowClientDouble.u64le(p, 0));
        assertEquals(org.tbc.world.combat.Combat.LOOT_CORPSE, p[8] & 0xFF);
    }

    @Given("the player is in combat with the kobold")
    public void inCombat() {
        client.attackSwing(world, kobold.guid);
        assertTrue(kobold.inCombat);
        assertTrue(kobold.alive());
    }

    @When("the player runs past the {int} yard leash")
    public void runPastLeash(int yards) {
        Player p = client.session().player();
        p.relocate(kobold.spawnX + yards + 5, kobold.spawnY, kobold.spawnZ, 0);
        client.clear();
        world.tick(50);
    }

    @Then("the kobold is at spawn with full health and an empty threat list")
    public void resetHome() {
        assertEquals(kobold.spawnX, kobold.x, 0.01f);
        assertEquals(kobold.spawnY, kobold.y, 0.01f);
        assertEquals(kobold.maxHealth(), kobold.health());
        assertEquals(0, kobold.threat);
        assertFalse(kobold.inCombat);
        assertFalse(kobold.lootable);
    }

    @When("the player loots the living kobold")
    @When("the player loots the kobold")
    public void lootKobold() {
        client.clear();
        client.loot(world, kobold.guid);
    }

    @Then("the server does not send SMSG_LOOT_RESPONSE")
    public void noLoot() {
        assertFalse(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
    }

    @When("a second mock client loots the same corpse")
    public void secondLoots() {
        WowClientDouble other = new WowClientDouble();
        other.connect(new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86"));
        Player created = world.characters.create(2, "Other", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        other.login(world, created.guid);
        other.clear();
        other.loot(world, kobold.guid);
        client = other;
    }

    @Then("the second client does not receive SMSG_LOOT_RESPONSE")
    public void secondNoLoot() {
        assertFalse(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
    }

    @When("the mock client sends CMSG_LOOT with fewer than {int} bytes")
    public void truncatedLoot(int n) {
        assertEquals(8, n);
        client.clear();
        client.handle(world, Opcodes.CMSG_LOOT, new byte[3]);
    }

    @Then("the combat session still answers CMSG_PING with SMSG_PONG")
    public void pong() {
        client.clear();
        client.ping(world, 9);
        assertTrue(client.saw(Opcodes.SMSG_PONG));
        assertEquals(9, WowClientDouble.u32le(client.payload(Opcodes.SMSG_PONG), 0));
    }

    @When("the creature pursuit timer expires")
    public void pursuitExpires() {
        kobold.lastHitMs = world.nowMs() - org.tbc.world.combat.Combat.PURSUIT_MS - 1;
        client.clear();
        world.tick(50);
    }

    private boolean sawLeftSwing() {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKERSTATEUPDATE) {
                continue;
            }
            if ((WowClientDouble.u32le(client.payloads.get(i), 0) & Combat.HITINFO_LEFTSWING) != 0) {
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
