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
        assertEquals(Guid.HIGH_PET | (Guid.low(alt.guid) & 0xFFFFFFFFL), owner.pet.guid);
        assertEquals(Guid.low(alt.guid),
                owner.companion.worldBody().getInt(UpdateFields.UNIT_FIELD_PETNUMBER));
        assertTrue(sink.last.containsKey(Opcodes.SMSG_PET_SPELLS));
        byte[] bar = sink.last.get(Opcodes.SMSG_PET_SPELLS);
        assertEquals(owner.pet.guid, u64le(bar, 0));
        assertEquals(PetHandlerBar.REACT_DEFENSIVE, bar[12] & 0xFF);
        assertEquals(PetHandlerBar.COMMAND_FOLLOW, bar[13] & 0xFF);
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
        assertEquals(CompanionService.FOLLOW_DIST, dist, 0.05f);
        // o=0 → left is +Y (sin(π/2)=1, cos=0)
        assertEquals(0f, dx, 0.05f);
        assertEquals(CompanionService.FOLLOW_DIST, dy, 0.05f);
        assertEquals(body, world.map(owner.mapId, owner.instanceId).creatures.get(body.guid));
        assertTrue(sink.last.containsKey(Opcodes.SMSG_UPDATE_OBJECT)
                || sink.last.containsKey(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertEquals(body.guid, owner.getGuid(UpdateFields.UNIT_FIELD_SUMMON));

        assertEquals(CompanionService.OK_DISMISS, world.companions.dismiss(world, owner));
        assertNull(world.map(owner.mapId, owner.instanceId).creatures.get(body.guid));
        assertEquals(0L, owner.getGuid(UpdateFields.UNIT_FIELD_SUMMON));
    }

    @Test
    void summonedCharacterNameShouldResolveForOwnerAndNearbyObserver() {
        World world = World.inMemory();
        Sink ownerSink = login(world, "Owner");
        Player owner = ownerSink.session.player();
        Player alt = world.characters.create(ACC.id(), "Acantha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.characters.save(alt);
        assertEquals(CompanionService.OK_SUMMON, world.companions.summon(world, owner, "Acantha"));
        Creature body = owner.companion.worldBody();
        int petNumber = body.getInt(UpdateFields.UNIT_FIELD_PETNUMBER);
        int timestamp = body.getInt(UpdateFields.UNIT_FIELD_PET_NAME_TIMESTAMP);
        assertTrue(timestamp > 0);

        ownerSink.last.clear();
        petNameQuery(ownerSink, world, petNumber, body.guid);
        byte[] ownerReply = ownerSink.last.get(Opcodes.SMSG_PET_NAME_QUERY_RESPONSE);
        assertNotNull(ownerReply);
        assertEquals("Acantha", cString(ownerReply, 4));
        assertEquals(timestamp, u32le(ownerReply, 4 + "Acantha".length() + 1));
        assertEquals(timestamp, owner.pet.nameTimestamp);

        Sink observer = login(world, new World.Account(2, "OBSERVER", new byte[40], 0, 1, "Win", "x86"),
                "Observer");
        observer.last.clear();
        petNameQuery(observer, world, petNumber, body.guid);
        byte[] observerReply = observer.last.get(Opcodes.SMSG_PET_NAME_QUERY_RESPONSE);
        assertNotNull(observerReply);
        assertEquals("Acantha", cString(observerReply, 4));

        WowBuffer creatureQuery = new WowBuffer(12);
        creatureQuery.putU32(body.entry);
        creatureQuery.putU64(body.guid);
        observer.session.handle(world, Opcodes.CMSG_CREATURE_QUERY, creatureQuery.array());
        byte[] creatureReply = observer.last.get(Opcodes.SMSG_CREATURE_QUERY_RESPONSE);
        assertNotNull(creatureReply);
        assertEquals("Acantha", cString(creatureReply, 4));
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
    void summonWhenOkShouldSeedCmangosPetActionBar() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        int[] bar = owner.pet.actionBar;
        assertEquals(PetHandlerBar.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24), bar[0]);
        assertEquals(PetHandlerBar.COMMAND_FOLLOW | (PetHandler.ACT_COMMAND << 24), bar[1]);
        assertEquals(PetHandlerBar.COMMAND_STAY | (PetHandler.ACT_COMMAND << 24), bar[2]);
        for (int i = PetHandlerBar.SPELL_SLOT_START; i < PetHandlerBar.SPELL_SLOT_END; i++) {
            int act = (bar[i] >>> 24) & 0xFF;
            assertTrue(act == PetHandler.ACT_DISABLED || act == PetHandler.ACT_ENABLED,
                    "spell slot " + i + " act=" + act);
            if (act == PetHandler.ACT_DISABLED) {
                assertEquals(0, bar[i] & 0xFFFFFF);
            }
        }
        assertEquals(PetHandlerBar.REACT_AGGRESSIVE | (PetHandler.ACT_REACTION << 24), bar[7]);
        assertEquals(PetHandlerBar.REACT_DEFENSIVE | (PetHandler.ACT_REACTION << 24), bar[8]);
        assertEquals(PetHandlerBar.REACT_PASSIVE | (PetHandler.ACT_REACTION << 24), bar[9]);
        assertEquals(Player.PLAYER_CONTROLLED_DEBUFF_LIMIT,
                (owner.companion.worldBody().getInt(UpdateFields.UNIT_FIELD_BYTES_2) >> 8) & 0xFF);
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
    void summonWhenAltKnowsMeleeAttackShouldKeepItOffCompanionSpellsAndBar() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, Player.CLASS_MAGE, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        alt.spells.add(CompanionService.MELEE_ATTACK_SPELL);
        alt.spells.add(SpellEngine.FIREBALL);
        alt.actionButtons[0] = CompanionService.MELEE_ATTACK_SPELL;
        alt.actionButtons[1] = SpellEngine.FIREBALL;
        world.characters.save(alt);

        world.companions.summon(world, owner, "Alt");

        assertFalse(owner.pet.spells.contains(CompanionService.MELEE_ATTACK_SPELL),
                "Attack command already drives melee");
        assertTrue(owner.pet.spells.contains(SpellEngine.FIREBALL));
        assertEquals(SpellEngine.FIREBALL | (PetHandler.ACT_ENABLED << 24),
                owner.pet.actionBar[PetHandlerBar.SPELL_SLOT_START]);
    }

    @Test
    void summonWhenKnownSpellRequiresHigherLevelShouldKeepItOffPetBar() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, Player.CLASS_MAGE, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        int fireballRank1 = 133;
        int fireballRank2 = 143;
        alt.level = 5;
        alt.spells.add(fireballRank1);
        alt.spells.add(fireballRank2);
        alt.actionButtons[0] = fireballRank2;
        alt.actionButtons[1] = fireballRank1;
        world.objectMgr.spellBaseLevel.put(fireballRank1, 1);
        world.objectMgr.spellBaseLevel.put(fireballRank2, 6);
        world.characters.save(alt);

        assertEquals(CompanionService.OK_SUMMON, world.companions.summon(world, owner, "Alt"));
        assertTrue(owner.pet.spells.contains(fireballRank1));
        assertFalse(owner.pet.spells.contains(fireballRank2));
        for (int packed : owner.pet.actionBar) {
            assertFalse((packed & 0xFFFFFF) == fireballRank2);
        }
    }

    @Test
    void summonWhenSpellRankPredecessorIsMissingShouldKeepOrphanRankOffPetBar() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, Player.CLASS_MAGE, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        int knownRank = 133;
        int orphanRank = 143;
        int missingPreviousRank = 999_001;
        alt.level = 10;
        alt.spells.add(knownRank);
        alt.spells.add(orphanRank);
        alt.actionButtons[0] = orphanRank;
        alt.actionButtons[1] = knownRank;
        world.objectMgr.spellBaseLevel.put(knownRank, 1);
        world.objectMgr.spellBaseLevel.put(orphanRank, 6);
        world.objectMgr.spellChain.put(orphanRank,
                new org.tbc.world.content.ObjectMgr.SpellChainNode(orphanRank, missingPreviousRank,
                        missingPreviousRank, 2, 0));
        world.characters.save(alt);

        assertEquals(CompanionService.OK_SUMMON, world.companions.summon(world, owner, "Alt"));
        assertTrue(owner.pet.spells.contains(knownRank));
        assertFalse(owner.pet.spells.contains(orphanRank));
        for (int packed : owner.pet.actionBar) {
            assertFalse((packed & 0xFFFFFF) == orphanRank);
        }
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
    void petCommandsWhenCompanionActiveShouldStayFollowAttackAndGoPassive() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        Creature body = owner.companion.worldBody();

        petAction(sink, world, owner.pet.guid, PetHandlerBar.COMMAND_STAY, PetHandler.ACT_COMMAND, 0);
        assertEquals(PetHandlerBar.COMMAND_STAY, owner.pet.commandState);
        float stayX = body.x;
        float stayY = body.y;
        owner.x += 20f;
        CompanionBehavior.tick(world, owner, 200);
        assertEquals(stayX, body.x, 0.01f);
        assertEquals(stayY, body.y, 0.01f);

        petAction(sink, world, owner.pet.guid, PetHandlerBar.COMMAND_FOLLOW, PetHandler.ACT_COMMAND, 0);
        assertEquals(PetHandlerBar.COMMAND_FOLLOW, owner.pet.commandState);
        CompanionBehavior.tick(world, owner, 200);
        assertTrue(Math.hypot(body.x - stayX, body.y - stayY) > 1f);

        Creature mob = new Creature();
        mob.guid = 0xF1300000000000CCL;
        mob.setHealth(500);
        mob.mapId = owner.mapId;
        mob.x = owner.x + 10f;
        mob.y = owner.y;
        world.map(owner.mapId, owner.instanceId).creatures.put(mob.guid, mob);
        petAction(sink, world, owner.pet.guid, PetHandlerBar.COMMAND_ATTACK, PetHandler.ACT_COMMAND, mob.guid);
        assertEquals(mob.guid, owner.pet.victim);

        petAction(sink, world, owner.pet.guid, PetHandlerBar.REACT_PASSIVE, PetHandler.ACT_REACTION, 0);
        assertEquals(PetHandlerBar.REACT_PASSIVE, owner.pet.reactState);
        assertEquals(0L, owner.pet.victim);
        assertEquals(0L, body.victim);
        byte[] bar = sink.last.get(Opcodes.SMSG_PET_SPELLS);
        assertEquals(PetHandlerBar.REACT_PASSIVE, bar[12] & 0xFF);
        assertEquals(PetHandlerBar.COMMAND_FOLLOW, bar[13] & 0xFF);
    }

    @Test
    void behaviorWhenVictimFarShouldChaseTowardPrey() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        owner.x = 0f;
        owner.y = 0f;
        owner.o = 0f;
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        Creature body = owner.companion.worldBody();
        float startX = body.x;
        float startY = body.y;
        Creature mob = new Creature();
        mob.guid = 0xF1300000000000AAL;
        mob.setHealth(500);
        mob.mapId = owner.mapId;
        mob.x = 40f;
        mob.y = 0f;
        mob.z = 0f;
        world.map(owner.mapId, owner.instanceId).creatures.put(mob.guid, mob);
        owner.victim = mob.guid;
        sink.last.clear();
        for (int i = 0; i < 20; i++) {
            CompanionBehavior.tick(world, owner, 200);
        }
        double before = Math.hypot(startX - mob.x, startY - mob.y);
        double after = Math.hypot(body.x - mob.x, body.y - mob.y);
        assertTrue(after < before - 1f, "companion must chase closer to prey, before=" + before + " after=" + after);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_MONSTER_MOVE));
    }

    @Test
    void worldTickWhenCompanionChasesShouldAdvanceItOnlyOnce() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        owner.x = 0f;
        owner.y = 0f;
        world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.companions.summon(world, owner, "Alt");
        Creature body = owner.companion.worldBody();
        Creature mob = new Creature();
        mob.guid = 0xF1300000000000DDL;
        mob.setHealth(500);
        mob.mapId = owner.mapId;
        mob.x = 40f;
        mob.y = 0f;
        world.map(owner.mapId, owner.instanceId).creatures.put(mob.guid, mob);
        owner.victim = mob.guid;
        float startX = body.x;
        float startY = body.y;
        CompanionBehavior.tick(world, owner, 200);
        double afterCompanionTick = Math.hypot(body.x - startX, body.y - startY);
        assertTrue(afterCompanionTick > 1f && afterCompanionTick < 2f,
                "one 200 ms run step expected, moved=" + afterCompanionTick);

        world.tick(200);

        double afterWorldCreatureLoop = Math.hypot(body.x - startX, body.y - startY);
        assertEquals(afterCompanionTick, afterWorldCreatureLoop, 0.01,
                "generic creature loop must not advance a player-controlled companion again");
    }

    @Test
    void behaviorWhenManaAndInRangeShouldAutoCastBarSpell() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alt.spells.add(SpellEngine.FIREBALL);
        alt.actionButtons[0] = SpellEngine.FIREBALL;
        alt.setInt(UpdateFields.UNIT_FIELD_POWER1, 200);
        alt.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        world.characters.save(alt);
        world.companions.summon(world, owner, "Alt");
        Creature body = owner.companion.worldBody();
        body.x = owner.x;
        body.y = owner.y;
        body.z = owner.z;
        Creature mob = new Creature();
        mob.guid = 0xF1300000000000BBL;
        mob.setHealth(500);
        mob.mapId = owner.mapId;
        mob.x = owner.x + 5f;
        mob.y = owner.y;
        mob.z = owner.z;
        world.map(owner.mapId, owner.instanceId).creatures.put(mob.guid, mob);
        owner.victim = mob.guid;
        sink.last.clear();
        int manaBefore = owner.companion.snapshot().getInt(UpdateFields.UNIT_FIELD_POWER1);
        CompanionBehavior.tick(world, owner, 50);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_SPELL_START)
                || sink.last.containsKey(Opcodes.SMSG_SPELL_GO));
        assertTrue(owner.companion.snapshot().getInt(UpdateFields.UNIT_FIELD_POWER1) < manaBefore);
    }

    @Test
    void behaviorWhenRangedAutoSpellReadyShouldHoldCastingRange() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, Player.CLASS_MAGE, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        alt.spells.add(SpellEngine.FIREBALL);
        alt.actionButtons[0] = SpellEngine.FIREBALL;
        alt.setInt(UpdateFields.UNIT_FIELD_POWER1, 2000);
        alt.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 2000);
        world.characters.save(alt);
        world.companions.summon(world, owner, "Alt");
        Creature body = owner.companion.worldBody();
        Creature mob = new Creature();
        mob.guid = 0xF1300000000000EEL;
        mob.setHealth(5000);
        mob.mapId = owner.mapId;
        mob.x = owner.x + 40f;
        mob.y = owner.y;
        world.map(owner.mapId, owner.instanceId).creatures.put(mob.guid, mob);
        owner.victim = mob.guid;

        for (int i = 0; i < 40; i++) {
            CompanionBehavior.tick(world, owner, 200);
        }

        float spellRange = world.spells.info(SpellEngine.FIREBALL).maxRange();
        double distance = body.distance2d(mob);
        assertTrue(distance <= spellRange + 0.5f, "must enter cast range, distance=" + distance);
        assertTrue(distance >= spellRange * 0.7f, "ranged caster must not chase to melee, distance=" + distance);
        assertTrue(sink.last.containsKey(Opcodes.SMSG_SPELL_START));
    }

    @Test
    void petSpellButtonWhenTargetOutOfRangeShouldPathIntoRangeThenCast() {
        World world = World.inMemory();
        Sink sink = login(world, "Owner");
        Player owner = sink.session.player();
        Player alt = world.characters.create(ACC.id(), "Alt", 1, Player.CLASS_MAGE, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        alt.spells.add(SpellEngine.FIREBALL);
        alt.actionButtons[0] = SpellEngine.FIREBALL;
        alt.setInt(UpdateFields.UNIT_FIELD_POWER1, 1000);
        world.characters.save(alt);
        world.companions.summon(world, owner, "Alt");
        Creature mob = new Creature();
        mob.guid = 0xF1300000000000FFL;
        mob.setHealth(5000);
        mob.mapId = owner.mapId;
        mob.x = owner.x + 40f;
        mob.y = owner.y;
        world.map(owner.mapId, owner.instanceId).creatures.put(mob.guid, mob);
        sink.last.clear();

        petAction(sink, world, owner.pet.guid, SpellEngine.FIREBALL, PetHandler.ACT_ENABLED, mob.guid);
        assertEquals(mob.guid, owner.pet.victim);
        assertFalse(sink.last.containsKey(Opcodes.SMSG_SPELL_START));

        for (int i = 0; i < 40 && !sink.last.containsKey(Opcodes.SMSG_SPELL_START); i++) {
            CompanionBehavior.tick(world, owner, 200);
        }

        assertTrue(sink.last.containsKey(Opcodes.SMSG_SPELL_START));
        assertTrue(owner.companion.worldBody().distance2d(mob)
                <= world.spells.info(SpellEngine.FIREBALL).maxRange() + 0.5f);
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
        return login(world, ACC, name);
    }

    private static Sink login(World world, World.Account account, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, account.id());
        s.injectAccount(account);
        Player created = world.characters.create(account.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static void petNameQuery(Sink sink, World world, int petNumber, long petGuid) {
        WowBuffer query = new WowBuffer(12);
        query.putU32(petNumber);
        query.putU64(petGuid);
        sink.session.handle(world, Opcodes.CMSG_PET_NAME_QUERY, query.array());
    }

    private static String cString(byte[] bytes, int offset) {
        int end = offset;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, offset, end - offset, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static long u64le(byte[] b, int o) {
        return (b[o] & 0xFFL) | ((b[o + 1] & 0xFFL) << 8) | ((b[o + 2] & 0xFFL) << 16)
                | ((b[o + 3] & 0xFFL) << 24) | ((b[o + 4] & 0xFFL) << 32) | ((b[o + 5] & 0xFFL) << 40)
                | ((b[o + 6] & 0xFFL) << 48) | ((b[o + 7] & 0xFFL) << 56);
    }

    private static void petAction(Sink sink, World world, long petGuid, int action, int type, long target) {
        WowBuffer packet = new WowBuffer(20);
        packet.putU64(petGuid);
        packet.putU32(action | (type << 24));
        packet.putU64(target);
        PetHandler.action(sink.session, world, packet);
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
