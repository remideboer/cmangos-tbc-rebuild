package org.tbc;

import org.junit.jupiter.api.Test;
import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.companion.CompanionPartyAddon;
import org.tbc.world.companion.CompanionService;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PetHandler;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionPartyBarP0Test {
    private static final String WIRE_PREFIX = "CompanionBar";
    private static final World.Account ACCOUNT =
            new World.Account(1, "COMPANION_UI", new byte[40], 0, 1, "Win", "x86");

    @Test
    void tpCompUi001EnableShouldPushInactiveThenSummonedCharacterState() {
        assertTrue(CompanionPartyAddon.PREFIX.length() <= 16,
                "TBC SendAddonMessage prefixes are limited to 16 characters");
        Fixture f = fixture();
        sendAddon(f.client, f.world, "enable");
        assertTrue(chatBodies(f.client).contains(WIRE_PREFIX + "\tState;0"));

        f.client.clear();
        assertEquals(CompanionService.OK_SUMMON,
                f.world.companions.summon(f.world, f.owner, "Acantha"));

        String state = lastCompanionState(f.client);
        String[] fields = state.split(";");
        assertEquals(WIRE_PREFIX + "\tState", fields[0]);
        assertEquals("1", fields[1]);
        assertEquals("Acantha", fields[2]);
        assertEquals(13, fields.length);
        assertEquals("2,7", fields[3]);
        assertEquals(SpellEngine.FIREBALL + "," + PetHandler.ACT_ENABLED, fields[6]);
        assertEquals("0,6", fields[12]);

        f.client.clear();
        f.world.companions.dismiss(f.world, f.owner);
        assertTrue(chatBodies(f.client).contains(WIRE_PREFIX + "\tState;0"));
    }

    @Test
    void tpCompUi002ActionShouldUseValidatedPetHandlerSlot() {
        Fixture f = fixture();
        sendAddon(f.client, f.world, "enable");
        f.world.companions.summon(f.world, f.owner, "Acantha");
        Creature target = new Creature();
        target.guid = 0xF1300000000000AAL;
        target.mapId = f.owner.mapId;
        target.setHealth(100);
        f.world.map(f.owner.mapId, f.owner.instanceId).add(target);

        sendAddon(f.client, f.world, "Action;1;0xF1300000000000AA");

        assertEquals(target.guid, f.owner.pet.victim);
        assertTrue(lastCompanionState(f.client).startsWith(WIRE_PREFIX + "\tState;1;Acantha;"));
    }

    @Test
    void tpCompUi003MalformedActionShouldBeIgnored() {
        Fixture f = fixture();
        sendAddon(f.client, f.world, "enable");
        f.world.companions.summon(f.world, f.owner, "Acantha");

        sendAddon(f.client, f.world, "Action;0;not-a-guid");
        sendAddon(f.client, f.world, "Action;11;1");

        assertEquals(0L, f.owner.pet.victim);
    }

    @Test
    void tpCompUiInvNormalPetShouldRemainOutsideCompanionAddon() {
        Fixture f = fixture();
        f.owner.clazz = PetHandler.CLASS_HUNTER;
        WowBuffer summonPet = new WowBuffer(20);
        summonPet.putU64(0);
        summonPet.putU32(PetHandler.COMMAND_FOLLOW | (PetHandler.ACT_COMMAND << 24));
        summonPet.putU64(0);
        f.client.handle(f.world, Opcodes.CMSG_PET_ACTION, summonPet.array());
        long petGuid = f.owner.pet.guid;
        f.client.clear();

        sendAddon(f.client, f.world, "enable");
        sendAddon(f.client, f.world, "Action;1;1");

        assertTrue(chatBodies(f.client).contains(WIRE_PREFIX + "\tState;0"));
        assertEquals(petGuid, f.owner.pet.guid);
        assertEquals(0L, f.owner.pet.victim);
    }

    private static Fixture fixture() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACCOUNT));
        Player owner = world.characters.create(ACCOUNT.id(), "Owner", 1, 1, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        client.login(world, owner.guid);
        Player alt = world.characters.create(ACCOUNT.id(), "Acantha", 1, Player.CLASS_MAGE, 0,
                1, 1, 1, 1, 0, world.objectMgr);
        alt.spells.add(SpellEngine.FIREBALL);
        alt.actionButtons[0] = SpellEngine.FIREBALL;
        world.characters.save(alt);
        client.clear();
        return new Fixture(world, client, client.session().player());
    }

    private static void sendAddon(WowClientDouble client, World world, String body) {
        WowBuffer packet = new WowBuffer(96);
        packet.putU32(0x01);
        packet.putU32(0xFFFFFFFF);
        packet.putCString(WIRE_PREFIX + "\t" + body);
        client.handle(world, Opcodes.CMSG_MESSAGECHAT, packet.array());
    }

    private static String lastCompanionState(WowClientDouble client) {
        List<String> messages = chatBodies(client);
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).startsWith(WIRE_PREFIX + "\tState;")) {
                return messages.get(i);
            }
        }
        return "";
    }

    private static List<String> chatBodies(WowClientDouble client) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_MESSAGECHAT) {
                continue;
            }
            WowBuffer packet = new WowBuffer(client.payloads.get(i));
            packet.getU8();
            packet.getU32();
            packet.getU64();
            packet.getU32();
            packet.getU64();
            packet.getU32();
            out.add(packet.getCString());
        }
        return out;
    }

    private record Fixture(World world, WowClientDouble client, Player owner) {
    }
}
