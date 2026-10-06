package org.tbc.world.companion;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.PetHandler;
import org.tbc.world.session.WorldSession;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-COMP-001..007 — offline-character companion (gated CompanionConfig).
 */
class CompanionServiceTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @BeforeEach
    void resetConfig() {
        CompanionConfig.reset();
    }

    @AfterEach
    void reset() {
        CompanionConfig.reset();
    }

    @Test
    void defaultsWhenBuiltShouldBeEnabled() {
        assertTrue(CompanionConfig.defaults().enabled());
        assertTrue(CompanionConfig.get().enabled());
    }

    @Test
    void summonWhenSameAccountOfflineShouldSendPetSpellsBar() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alt.spells.add(SpellEngine.SPELL_STEALTH);
        alt.actionButtons[0] = SpellEngine.SPELL_STEALTH;
        world.characters.save(alt);

        String msg = world.companions.summon(world, owner, "Alt");
        assertEquals(CompanionService.OK_SUMMON, msg);
        assertNotNull(owner.companion);
        assertNotNull(owner.pet);
        assertEquals("Alt", owner.pet.name);
        assertEquals(Guid.HIGH_CREATURE | (Guid.low(alt.guid) & 0xFFFFFFFFL), owner.pet.guid);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_PET_SPELLS));
        byte[] bar = sink.last.get(Opcodes.SMSG_PET_SPELLS);
        assertEquals(owner.pet.guid, u64le(bar, 0));
        boolean spellOnBar = false;
        for (int i = 0; i < 10; i++) {
            int packed = u32le(bar, 16 + i * 4);
            if ((packed & 0xFFFFFF) == SpellEngine.SPELL_STEALTH) {
                spellOnBar = true;
            }
        }
        assertTrue(spellOnBar);
    }

    @Test
    void summonWhenOkShouldSpawnCreatureNearOwnerAndReveal() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        owner.x = 100f;
        owner.y = 200f;
        owner.z = 50f;
        owner.o = 0f;
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);

        assertEquals(CompanionService.OK_SUMMON, world.companions.summon(world, owner, "Alt"));
        Creature body = owner.companion.worldBody();
        assertNotNull(body);
        assertEquals(owner.pet.guid, body.guid);
        assertEquals(owner.mapId, body.mapId);
        assertTrue(body.getInt(UpdateFields.UNIT_FIELD_DISPLAYID) > 0);
        float dx = body.x - owner.x;
        float dy = body.y - owner.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        assertTrue(dist > 0.5f && dist < 5f, "companion should stand near owner, dist=" + dist);
        assertEquals(body, world.map(owner.mapId, owner.instanceId).creatures.get(body.guid));
        assertTrue(sink.last.containsKey(Opcodes.SMSG_UPDATE_OBJECT)
                || sink.last.containsKey(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertEquals(body.guid, owner.getGuid(UpdateFields.UNIT_FIELD_SUMMON));

        assertEquals(CompanionService.OK_DISMISS, world.companions.dismiss(world, owner));
        assertNull(world.map(owner.mapId, owner.instanceId).creatures.get(body.guid));
        assertEquals(0L, owner.getGuid(UpdateFields.UNIT_FIELD_SUMMON));
    }

    @Test
    void summonWhenOkShouldApplyMirrorAppearanceAndGear() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alt.skin = 2;
        alt.face = 3;
        alt.hairStyle = 4;
        alt.hairColor = 5;
        alt.facialHair = 6;
        Item sword = new Item(world.nextItemGuid(), 25);
        sword.bag = 0;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        sword.displayId = 1542;
        sword.itemClass = Player.ITEM_CLASS_WEAPON;
        alt.items.put((int) sword.guid, sword);
        alt.applyEquippedVisuals();
        world.characters.save(alt);

        assertEquals(CompanionService.OK_SUMMON, world.companions.summon(world, owner, "Alt"));
        Creature body = owner.companion.worldBody();
        assertNotNull(body);
        int bytes0 = body.getInt(UpdateFields.UNIT_FIELD_BYTES_0);
        assertEquals(alt.race & 0xFF, bytes0 & 0xFF);
        assertEquals(alt.gender & 0xFF, (bytes0 >> 16) & 0xFF);
        assertTrue(body.hasAura(WorldSession.SPELL_MIRROR_IMAGE));
        assertEquals(CompanionAppearance.UNIT_FLAG2_CLONED,
                body.getInt(UpdateFields.UNIT_FIELD_FLAGS_2) & CompanionAppearance.UNIT_FLAG2_CLONED);
        assertEquals(1542, body.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_SLOT_DISPLAY));
        assertTrue(owner.isControllingPet());

        sink.last.clear();
        WowBuffer req = new WowBuffer(8);
        req.putU64(body.guid);
        sink.session.handle(world, Opcodes.CMSG_GET_MIRRORIMAGE_DATA, req.array());
        assertTrue(sink.last.containsKey(Opcodes.SMSG_MIRRORIMAGE_DATA));
        byte[] data = sink.last.get(Opcodes.SMSG_MIRRORIMAGE_DATA);
        assertEquals(body.guid, u64le(data, 0));
        assertEquals(2, data[14] & 0xFF);
        assertEquals(3, data[15] & 0xFF);
        // Head display (first of 11 gear u32s) starts at offset 23 after guild u32 at 19.
        assertEquals(0, u32le(data, 23)); // no helm
        // Mainhand is not in the 11 armor slots; chest slot index 3 → offset 23+12=35 if empty.
        // Shoulders (index 1) etc. — verify at least one non-zero if we had chest; we set mainhand virtual only.
        int[] displays = CompanionAppearance.mirrorEquipmentDisplays(owner.companion.snapshot(), world.objectMgr);
        assertEquals(0, displays[0]);

        world.companions.dismiss(world, owner);
        assertFalse(owner.isControllingPet());
    }

    @Test
    void followWhenOwnerMovesShouldSendMonsterMove() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        owner.x = 0f;
        owner.y = 0f;
        owner.z = 0f;
        owner.o = 0f;
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        sink.last.clear();
        owner.x = 20f;
        owner.y = 0f;
        CompanionBehavior.tick(world, owner, 50);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_MONSTER_MOVE),
                "follow must broadcast SMSG_MONSTER_MOVE");
    }

    @Test
    void summonWhenAltHasSpellsShouldEncodeSpellCountOnPetBar() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alt.spells.add(SpellEngine.SPELL_STEALTH);
        alt.actionButtons[0] = SpellEngine.SPELL_STEALTH;
        world.characters.save(alt);
        sink.last.clear();
        world.companions.summon(world, owner, "Alt");
        byte[] bar = sink.last.get(Opcodes.SMSG_PET_SPELLS);
        assertNotNull(bar);
        int spellCount = bar[16 + 10 * 4] & 0xFF;
        assertTrue(spellCount >= 1, "spellCount=" + spellCount);
    }

    @Test
    void summonWhenDisabledShouldRefuse() {
        CompanionConfig.set(CompanionConfig.defaults().withEnabled(false));
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        assertEquals(CompanionService.ERR_DISABLED, world.companions.summon(world, sink.session.player(), "Alt"));
    }

    @Test
    void summonWhenSelfOrMissingOrAlreadyShouldRefuse() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        assertEquals(CompanionService.ERR_SELF, world.companions.summon(world, owner, "Owner"));
        assertEquals(CompanionService.ERR_NOT_FOUND, world.companions.summon(world, owner, "Nobody"));
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        assertEquals(CompanionService.OK_SUMMON, world.companions.summon(world, owner, "Alt"));
        assertEquals(CompanionService.ERR_ALREADY, world.companions.summon(world, owner, "Alt"));
    }

    @Test
    void dismissWhenSummonedShouldHideBarAndClear() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        sink.last.clear();
        assertEquals(CompanionService.OK_DISMISS, world.companions.dismiss(world, owner));
        assertNull(owner.companion);
        assertNull(owner.pet);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_PET_SPELLS));
        assertEquals(0L, u64le(sink.last.get(Opcodes.SMSG_PET_SPELLS), 0));
    }

    @Test
    void logoutWhenCompanionActiveShouldDesummon() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        sink.session.logout(world, true);
        assertNull(owner.companion);
        assertNull(owner.pet);
    }

    @Test
    void petCastWhenCompanionShouldSpendSnapshotMana() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, Player.CLASS_MAGE, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        alt.spells.add(133);
        alt.actionButtons[0] = 133;
        alt.setInt(UpdateFields.UNIT_FIELD_POWER1, 200);
        world.characters.save(alt);
        world.companions.summon(world, owner, "Alt");
        WowBuffer cast = new WowBuffer(24);
        cast.putU64(owner.pet.guid);
        cast.putU32(133);
        cast.putU32(0);
        PetHandler.castSpell(sink.session, world, cast);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_SPELL_START));
        assertTrue(owner.companion.snapshot().getInt(UpdateFields.UNIT_FIELD_POWER1) < 200
                || world.spells.info(133) == null
                || world.spells.info(133).mana() == 0);
    }

    @Test
    void behaviorWhenOwnerHasVictimShouldAssistWithSwingNotTickDamage() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        Creature body = owner.companion.worldBody();
        body.x = owner.x;
        body.y = owner.y;
        body.z = owner.z;
        Creature mob = new Creature();
        mob.guid = 0xF130000000000099L;
        mob.setHealth(500);
        mob.mapId = owner.mapId;
        mob.x = owner.x;
        mob.y = owner.y;
        mob.z = owner.z;
        world.map(owner.mapId, owner.instanceId).creatures.put(mob.guid, mob);
        owner.victim = mob.guid;
        int hpBefore = mob.health();
        CompanionBehavior.tick(world, owner, 50);
        assertEquals(mob.guid, owner.pet.victim);
        int afterFirst = mob.health();
        assertTrue(afterFirst < hpBefore, "one swing should land");
        assertTrue(mob.inCombat, "mob must enter combat");
        assertTrue(mob.threatManager.threatOf(owner) > 0f || mob.victim == owner.guid);
        CompanionBehavior.tick(world, owner, 50);
        assertEquals(afterFirst, mob.health(), "second tick within swing CD must not strip HP");
        assertTrue(sink.last.containsKey(Opcodes.SMSG_ATTACKERSTATEUPDATE)
                || owner.inCombat);
    }

    @Test
    void rewardWhenCompanionActiveShouldSplitXp() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        owner.level = 1;
        owner.xp = 0;
        Player alt = world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alt.level = 1;
        alt.xp = 0;
        world.characters.save(alt);
        world.companions.summon(world, owner, "Alt");
        Creature mob = new Creature();
        mob.guid = 0xF130000000000088L;
        mob.level = 1;
        mob.entry = 6;
        mob.setHealth(0);
        mob.mapId = owner.mapId;
        int beforeOwner = owner.xp;
        int beforeAlt = owner.companion.snapshot().xp;
        world.onCreatureKilled(owner, mob);
        assertTrue(owner.xp > beforeOwner);
        assertTrue(owner.companion.snapshot().xp > beforeAlt);
    }

    @Test
    void gmCompanionSummonCommandShouldWork() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        owner.gmLevel = 0;
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        String r = world.gm.handle(world, owner, ".companion summon Alt");
        assertEquals(CompanionService.OK_SUMMON, r);
        assertNotNull(owner.companion);
    }

    @Test
    void gmCompanionWhenPlayerSecurityAndSqlOverlayShouldStillAllow() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        owner.gmLevel = 0;
        world.gm.overlay("companion", 3);
        world.gm.overlay("companion summon", 3);
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        assertEquals(CompanionService.ERR_USAGE, world.gm.handle(world, owner, ".companion"));
        assertEquals(CompanionService.OK_SUMMON, world.gm.handle(world, owner, ".companion summon Alt"));
        assertNotNull(owner.companion);
        world.companions.dismiss(world, owner);
        assertEquals(CompanionService.OK_SUMMON, world.gm.handle(world, owner, ".companion Alt"));
        assertNotNull(owner.companion);
    }

    @Test
    void hunterPetPathWhenNoCompanionShouldStillWork() {
        World world = World.inMemory();
        Sink sink = login(world, "Hunter");
        Player p = sink.session.player();
        p.clazz = PetHandler.CLASS_HUNTER;
        WowBuffer act = new WowBuffer(20);
        act.putU64(0);
        act.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        act.putU64(2);
        PetHandler.action(sink.session, world, act);
        assertNotNull(p.pet);
        assertNull(p.companion);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_PET_SPELLS));
    }

    private static Sink login(World world, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static long u64le(byte[] b, int o) {
        return (b[o] & 0xFFL) | ((b[o + 1] & 0xFFL) << 8) | ((b[o + 2] & 0xFFL) << 16)
                | ((b[o + 3] & 0xFFL) << 24) | ((b[o + 4] & 0xFFL) << 32) | ((b[o + 5] & 0xFFL) << 40)
                | ((b[o + 6] & 0xFFL) << 48) | ((b[o + 7] & 0xFFL) << 56);
    }

    private static int u32le(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24);
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
