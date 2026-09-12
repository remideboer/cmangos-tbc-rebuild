package org.tbc.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.common.WowBuffer;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
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
    private int lootWindowGold;
    private int copperBeforeLootMoney;

    @Given("a logged-in character standing next to Kobold Vermin {int}")
    public void nextToKobold(int entry) {
        world = World.inMemory();
        client = new WowClientDouble();
        client.connect(ACCOUNT);
        Player created = world.characters.create(ACCOUNT.id(), "Fighter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        kobold = find(entry);
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(kobold.x, kobold.y, kobold.z, kobold.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
    }

    @Given("the kobold respawn delay is {int} ms")
    public void koboldRespawnDelay(int ms) {
        kobold.respawnDelayMs = ms;
    }

    @Then("the kobold is alive with full health")
    public void koboldAliveFullHealth() {
        assertTrue(kobold.alive());
        assertEquals(kobold.maxHealth(), kobold.health());
    }

    @Then("the server has sent an update object for unit health")
    public void sawHealthUpdate() {
        assertTrue(client.saw(Opcodes.SMSG_UPDATE_OBJECT) || client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
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
    @Given("the player is {int} yards from the kobold")
    public void attackerYardsFromKobold(int yards) {
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(kobold.x + yards, kobold.y, kobold.z, kobold.o);
        p.setFacingTo(kobold.x, kobold.y);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
    }

    @Given("the player is {int} yards from the kobold facing away")
    public void playerYardsFacingAway(int yards) {
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(kobold.x + yards, kobold.y, kobold.z, 0f);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
    }

    @When("{int} ms elapse on the world")
    public void elapseWorld(int ms) {
        client.clear();
        world.tick(ms);
    }

    @When("the kobold is {int} yards from combat start")
    public void koboldYardsFromCombatStart(int yards) {
        Player p = client.session().player();
        float destX = kobold.combatStartX + yards;
        float destY = kobold.combatStartY;
        float ox = p.x;
        float oy = p.y;
        p.relocate(destX, destY, kobold.z, p.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        ox = kobold.x;
        oy = kobold.y;
        kobold.relocate(destX, destY, kobold.z, kobold.o);
        world.map(p.mapId, p.instanceId).reindex(kobold, ox, oy);
        client.clear();
        world.tick(50);
    }

    @When("the player starts auto-attack")
    public void startAutoAttack() {
        client.clear();
        client.attackSwing(world, kobold.guid);
    }

    @Then("SMSG_ATTACKSTART includes the creature attacking the player")
    public void creatureAttackStart() {
        assertTrue(sawCreatureAttackStart());
    }

    @Then("SMSG_ATTACKSTART does not include the creature attacking the player")
    public void noCreatureAttackStart() {
        assertFalse(sawCreatureAttackStart());
    }

    @Then("the kobold is not in combat")
    public void koboldNotInCombat() {
        assertFalse(kobold.inCombat);
    }

    @Then("the kobold is in combat")
    public void koboldIsInCombat() {
        assertTrue(kobold.inCombat);
    }

    @Then("the player is not in combat")
    public void playerNotInCombat() {
        Player p = client.session().player();
        assertFalse(p.inCombat);
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_FLAGS) & org.tbc.world.entity.Unit.UNIT_FLAG_IN_COMBAT);
    }

    @Then("SMSG_ATTACKSTOP is the player stopping attack on the kobold")
    public void attackStopPlayerOnKobold() {
        Player p = client.session().player();
        boolean saw = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTOP) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            int off = 0;
            long attacker = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            long victim = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            int nowDead = WowClientDouble.u32le(payload, off);
            if (attacker == p.guid && victim == kobold.guid && nowDead == 0) {
                saw = true;
            }
        }
        assertTrue(saw);
    }

    @Then("SMSG_ATTACKERSTATEUPDATE is an evade swing")
    public void attackerStateIsEvade() {
        boolean saw = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKERSTATEUPDATE) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            int hitInfo = WowClientDouble.u32le(payload, 0);
            int off = WowClientDouble.skipPackedGuid(payload, 4);
            off = WowClientDouble.skipPackedGuid(payload, off);
            off += 4 + 1 + 4 + 4 + 4 + 4 + 4;
            int victimState = WowClientDouble.u32le(payload, off);
            if ((hitInfo & Combat.HITINFO_MISS) != 0
                    && (hitInfo & Combat.HITINFO_SWINGNOHITSOUND) != 0
                    && victimState == Combat.VICTIM_EVADES) {
                saw = true;
            }
        }
        assertTrue(saw);
    }

    @Then("the server has sent SMSG_ATTACKSWING_NOTINRANGE")
    public void sawNotInRange() {
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSWING_NOTINRANGE));
    }

    @Then("the server has sent SMSG_ATTACKSWING_BADFACING")
    public void sawBadFacing() {
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSWING_BADFACING));
    }

    @Given("the kobold is faction {int} versus player faction {int}")
    public void setNeutralFactions(int creatureFaction, int playerFaction) {
        setFaction(kobold, creatureFaction);
        setFaction(client.session().player(), playerFaction);
    }

    private boolean sawCreatureAttackStart() {
        long playerGuid = client.session().player().guid;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTART) {
                continue;
            }
            byte[] p = client.payloads.get(i);
            if (p.length >= 16
                    && WowClientDouble.u64le(p, 0) == kobold.guid
                    && WowClientDouble.u64le(p, 8) == playerGuid) {
                return true;
            }
        }
        return false;
    }

    private static void setFaction(org.tbc.world.entity.Unit u, int templateId) {
        u.faction = templateId;
        u.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, templateId);
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

    @Then("the server has sent SMSG_MONSTER_MOVE")
    public void sawMonsterMove() {
        assertTrue(client.saw(Opcodes.SMSG_MONSTER_MOVE));
    }

    @Then("the server has not sent SMSG_ATTACKERSTATEUPDATE")
    public void noAttackerState() {
        assertFalse(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
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
        lootWindowGold = WowClientDouble.u32le(p, 9);
        assertTrue((p[13] & 0xFF) >= 1, "empty loot window is not the slice 6 milestone");
    }

    @When("the player takes loot slot {int}")
    public void takeLootSlot(int slot) {
        client.clear();
        client.autostoreLootItem(world, slot);
    }

    @Then("SMSG_ITEM_PUSH_RESULT is a loot push of item {int}")
    public void lootPush(int entry) {
        byte[] p = client.payload(Opcodes.SMSG_ITEM_PUSH_RESULT);
        WowBuffer b = new WowBuffer(p);
        assertEquals(client.session().player().guid, b.getU64());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        b.getU32();
        b.getU8();
        b.getU32();
        assertEquals(entry, b.getU32());
        int have = 0;
        for (Item it : client.session().player().items.values()) {
            if (it.entry == entry) {
                have += it.count;
            }
        }
        assertTrue(have >= 1);
        b.getU32();
        b.getU32();
        int pushed = b.getU32();
        assertTrue(pushed >= 1);
        assertEquals(have, b.getU32());
    }

    @Then("SMSG_LOOT_REMOVED is loot slot {int}")
    public void lootRemoved(int slot) {
        byte[] p = client.payload(Opcodes.SMSG_LOOT_REMOVED);
        assertEquals(1, p.length);
        assertEquals(slot, p[0] & 0xFF);
    }

    @Then("the backpack shows looted item {int} on the wire")
    public void backpackShowsLootedItem(int entry) {
        Player p = client.session().player();
        Item looted = null;
        for (Item it : p.items.values()) {
            if (it.entry == entry && client.sawCreateObject(UpdateBuilder.itemGuid(it))) {
                looted = it;
                break;
            }
        }
        assertTrue(looted != null, "no CREATE_OBJECT for looted item " + entry);
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + looted.slot * 2;
        long guid = UpdateBuilder.itemGuid(looted);
        assertEquals((int) guid, client.valuesField(p.guid, field));
        assertEquals((int) (guid >>> 32), client.valuesField(p.guid, field + 1));
    }

    @When("the player takes the corpse copper")
    public void takeCorpseCopper() {
        copperBeforeLootMoney = client.session().player().money;
        client.clear();
        client.lootMoney(world);
    }

    @Then("player copper increased by the corpse gold")
    public void copperIncreased() {
        assertTrue(lootWindowGold >= 1, "seed corpse must have gold for TP-SL06-007");
        assertEquals(copperBeforeLootMoney + lootWindowGold, client.session().player().money);
    }

    @Then("the server has sent SMSG_LOOT_CLEAR_MONEY")
    public void sawLootClearMoney() {
        assertTrue(client.saw(Opcodes.SMSG_LOOT_CLEAR_MONEY));
        assertEquals(0, client.payload(Opcodes.SMSG_LOOT_CLEAR_MONEY).length);
    }

    @Then("SMSG_LOOT_RELEASE_RESPONSE is for the kobold")
    public void lootReleaseForKobold() {
        byte[] p = client.payload(Opcodes.SMSG_LOOT_RELEASE_RESPONSE);
        assertTrue(p.length >= 9);
        assertEquals(kobold.guid, WowClientDouble.u64le(p, 0));
        assertEquals(1, p[8] & 0xFF);
    }

    @Then("the corpse is not lootable on the wire")
    public void corpseNotLootable() {
        assertFalse(kobold.lootable);
        int flags = client.valuesField(kobold.guid, UpdateFields.UNIT_DYNAMIC_FLAGS);
        assertEquals(0, flags & org.tbc.world.combat.Combat.UNIT_DYNFLAG_LOOTABLE);
        assertTrue(world.combat.lootResponse(client.session().player(), kobold) == null);
    }

    @Then("the server has not sent SMSG_LOOT_MONEY_NOTIFY")
    public void noLootMoneyNotify() {
        assertFalse(client.saw(Opcodes.SMSG_LOOT_MONEY_NOTIFY));
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
        float ox = p.x;
        float oy = p.y;
        p.relocate(kobold.spawnX + yards + 5, kobold.spawnY, kobold.spawnZ, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        kobold.lastHitMs = world.nowMs() - Combat.PURSUIT_MS - 1;
        client.clear();
        world.tick(50);
    }

    @Given("the player is {int} yards above the kobold")
    public void playerAboveKobold(int yards) {
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(kobold.x, kobold.y, kobold.z + yards, kobold.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
    }

    @Then("SMSG_ATTACKSTOP is the kobold stopping attack on the player")
    public void attackStopKoboldOnPlayer() {
        Player p = client.session().player();
        boolean saw = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTOP) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            int off = 0;
            long attacker = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            long victim = packedGuid(payload, off);
            off = WowClientDouble.skipPackedGuid(payload, off);
            int nowDead = WowClientDouble.u32le(payload, off);
            if (attacker == kobold.guid && victim == p.guid && nowDead == 0) {
                saw = true;
            }
        }
        assertTrue(saw);
    }

    private static long packedGuid(byte[] p, int off) {
        int mask = p[off++] & 0xFF;
        long g = 0;
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) != 0) {
                g |= (long) (p[off++] & 0xFF) << (8 * i);
            }
        }
        return g;
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

    @When("the mock client sends CMSG_AUTOSTORE_LOOT_ITEM with no bytes")
    public void truncatedAutostore() {
        client.clear();
        client.handle(world, Opcodes.CMSG_AUTOSTORE_LOOT_ITEM, new byte[0]);
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
        Player p = client.session().player();
        float ox = p.x;
        float oy = p.y;
        p.relocate(kobold.spawnX + Combat.LEASH_RADIUS + 5, kobold.spawnY, kobold.spawnZ, 0);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        kobold.lastHitMs = world.nowMs() - Combat.PURSUIT_MS - 1;
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
