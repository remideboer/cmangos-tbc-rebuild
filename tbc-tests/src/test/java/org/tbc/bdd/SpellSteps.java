package org.tbc.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpellSteps {
    private static final World.Account ACCOUNT =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble client;
    private Creature kobold;
    private int manaBefore;
    private int hpBefore;

    @Given("a logged-in caster who knows Fireball {int} next to Kobold Vermin {int}")
    public void casterWithFireball(int fireball, int entry) {
        world = World.inMemory();
        client = new WowClientDouble();
        client.connect(ACCOUNT);
        Player created = world.characters.create(ACCOUNT.id(), "Mage", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        kobold = find(entry);
        Player p = client.session().player();
        p.relocate(kobold.x, kobold.y, kobold.z, kobold.o);
        world.map(p.mapId, p.instanceId).add(p);
        p.spells.add(fireball);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        p.setPower(100);
    }

    @When("the player casts Fireball {int} on the kobold")
    public void castFireball(int spellId) {
        Player p = client.session().player();
        manaBefore = p.power();
        hpBefore = kobold.health();
        client.clear();
        client.castSpell(world, spellId, 1, kobold.guid);
    }

    @When("the player casts unknown spell {int} on the kobold")
    public void castUnknown(int spellId) {
        client.clear();
        client.castSpell(world, spellId, 1, kobold.guid);
    }

    @When("the player casts unlearned spell {int} on the kobold")
    public void castUnlearned(int spellId) {
        client.clear();
        client.castSpell(world, spellId, 1, kobold.guid);
    }

    @When("the player casts dummy {int}")
    public void castDummy(int spellId) {
        client.clear();
        client.castSpell(world, spellId, 1, 0);
    }

    @When("the player stands {int} yards from the kobold")
    public void standYards(int yards) {
        Player p = client.session().player();
        p.relocate(kobold.x + yards, kobold.y, kobold.z, 0);
    }

    @Given("the caster has {int} mana")
    public void setMana(int mana) {
        client.session().player().setPower(mana);
    }

    @When("the mock client sends CMSG_CAST_SPELL with fewer than {int} bytes")
    public void truncatedCast(int n) {
        assertEquals(4, n);
        client.clear();
        client.handle(world, Opcodes.CMSG_CAST_SPELL, new byte[2]);
    }

    @Then("mana spent equals the Fireball cost")
    public void manaSpent() {
        int cost = world.spells.info(SpellEngine.FIREBALL).mana();
        assertEquals(cost, manaBefore - client.session().player().power());
        assertTrue(cost > 0);
    }

    @Then("SMSG_SPELLNONMELEEDAMAGELOG damage matches the kobold health loss")
    public void damageLogMatches() {
        byte[] p = client.payload(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG);
        assertTrue(p.length > 8);
        int off = WowClientDouble.skipPackedGuid(p, 0);
        off = WowClientDouble.skipPackedGuid(p, off);
        assertEquals(SpellEngine.FIREBALL, WowClientDouble.u32le(p, off));
        int damage = WowClientDouble.u32le(p, off + 4);
        assertEquals(hpBefore - kobold.health(), damage);
        assertTrue(damage > 0);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_START));
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
    }

    @Then("the server does not send SMSG_CAST_RESULT")
    public void noCastResult() {
        assertFalse(client.saw(Opcodes.SMSG_CAST_RESULT));
    }

    @Then("the server does not send SMSG_SPELL_GO")
    public void noSpellGo() {
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO));
    }

    @Then("the server sends SMSG_CAST_RESULT with result {word}")
    public void castResult(String result) {
        byte[] p = client.payload(Opcodes.SMSG_CAST_RESULT);
        assertTrue(p.length >= 6);
        assertEquals(Integer.decode(result).intValue(), p[4] & 0xFF);
    }

    @Then("mana remaining is {int}")
    public void manaRemaining(int mana) {
        assertEquals(mana, client.session().player().power());
    }

    @Then("the spell session still answers CMSG_PING with SMSG_PONG")
    public void pong() {
        client.clear();
        client.ping(world, 9);
        assertTrue(client.saw(Opcodes.SMSG_PONG));
        assertEquals(9, WowClientDouble.u32le(client.payload(Opcodes.SMSG_PONG), 0));
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
