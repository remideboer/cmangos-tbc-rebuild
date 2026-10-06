package org.tbc.world.companion;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PetHandler;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Validate, summon, desummon, and persist offline-character companions.
 * Pet packet handlers stay thin — domain lives here.
 */
public final class CompanionService {
    public static final String ERR_DISABLED = "Companions are disabled.";
    public static final String ERR_USAGE = "Usage: .companion summon <Name> | .companion dismiss";
    public static final String ERR_NOT_FOUND = "Character not found on this account.";
    public static final String ERR_SELF = "You cannot summon your current character.";
    public static final String ERR_ONLINE = "That character is online.";
    public static final String ERR_ALREADY = "You already have a companion.";
    public static final String ERR_NONE = "You have no companion.";
    public static final String OK_SUMMON = "Companion summoned.";
    public static final String OK_DISMISS = "Companion dismissed.";

    /** Ephemeral creature_template entry base — name resolved via QueryHandler companion path. */
    public static final int COMPANION_ENTRY_BASE = 2_100_000;
    /** Yards left of owner facing (CMaNGOS PetAI m_followDist). */
    public static final float FOLLOW_DIST = 1.5f;
    /** Radians left of owner facing (CMaNGOS PET_FOLLOW_ANGLE = π/2). */
    public static final float FOLLOW_ANGLE = (float) (Math.PI / 2.0);

    /** ActionButtonType ACTION_BUTTON_SPELL = 0. */
    private static final int ACTION_SPELL = 0;

    private final Map<Long, int[]> savedBars = new HashMap<>();

    public String handleCommand(World world, Player owner, String[] parts) {
        if (!CompanionConfig.get().enabled()) {
            return ERR_DISABLED;
        }
        if (parts.length < 2) {
            return ERR_USAGE;
        }
        String sub = parts[1].toLowerCase();
        if ("summon".equals(sub)) {
            if (parts.length < 3) {
                return ERR_USAGE;
            }
            return summon(world, owner, parts[2]);
        }
        if ("dismiss".equals(sub)) {
            return dismiss(world, owner);
        }
        return ERR_USAGE;
    }

    public String summon(World world, Player owner, String name) {
        if (!CompanionConfig.get().enabled()) {
            return ERR_DISABLED;
        }
        if (owner == null || name == null || name.isBlank()) {
            return ERR_USAGE;
        }
        if (owner.companion != null) {
            return ERR_ALREADY;
        }
        Player listed = findAccountCharacter(world, owner.accountId, name);
        if (listed == null) {
            return ERR_NOT_FOUND;
        }
        if (Guid.low(listed.guid) == Guid.low(owner.guid)) {
            return ERR_SELF;
        }
        if (world.playerByName(listed.name) != null || listed.online) {
            return ERR_ONLINE;
        }
        Player snap = world.characters.load(owner.accountId, listed.guid, world.objectMgr);
        if (snap == null) {
            return ERR_NOT_FOUND;
        }
        world.characters.refreshCompanionAbilities(snap);
        snap.session = null;
        Pet pet = buildPet(snap, world.objectMgr);
        int[] saved = savedBars.get(snap.guid);
        if (saved != null) {
            // Restore only spell slots — keep CMaNGOS command/reaction defaults.
            for (int i = PetHandlerBar.SPELL_SLOT_START; i < PetHandlerBar.SPELL_SLOT_END
                    && i < saved.length; i++) {
                int packed = saved[i];
                int spellId = packed & 0xFFFFFF;
                if (spellId == 0 || pet.spells.contains(spellId)) {
                    pet.actionBar[i] = packed;
                }
            }
        }
        Companion companion = new Companion(snap.guid, snap, pet);
        Creature body = spawnWorldBody(world, owner, snap, pet);
        companion.setWorldBody(body);
        pet.bindBody(body);
        owner.companion = companion;
        owner.pet = pet;
        owner.setGuid(UpdateFields.UNIT_FIELD_SUMMON, pet.guid);
        owner.setControllingPet(true);
        sendSummonLink(owner);
        revealBody(world, owner, body);
        sendBar(owner);
        CompanionPartyAddon.pushState(owner.session);
        return OK_SUMMON;
    }

    public String dismiss(World world, Player owner) {
        if (!CompanionConfig.get().enabled()) {
            return ERR_DISABLED;
        }
        if (owner == null || owner.companion == null) {
            return ERR_NONE;
        }
        saveAndClear(world, owner);
        return OK_DISMISS;
    }

    /** Logout / map transfer / death — always clear if present. */
    public void desummonQuiet(World world, Player owner) {
        if (owner == null || owner.companion == null) {
            return;
        }
        saveAndClear(world, owner);
    }

    public void onPetBarChanged(Player owner) {
        if (owner == null || owner.companion == null || owner.pet == null) {
            return;
        }
        owner.companion.syncBarFromPet();
        savedBars.put(owner.companion.sourceGuid(), owner.companion.companionBar().clone());
        CompanionPartyAddon.pushState(owner.session);
    }

    public void awardXp(World world, Player owner, int xp) {
        if (owner == null || owner.companion == null || xp <= 0) {
            return;
        }
        Player snap = owner.companion.snapshot();
        snap.giveXp(xp, null, 1f);
        world.characters.save(snap);
    }

    private void saveAndClear(World world, Player owner) {
        Companion c = owner.companion;
        c.syncBarFromPet();
        savedBars.put(c.sourceGuid(), c.companionBar().clone());
        world.characters.save(c.snapshot());
        despawnWorldBody(world, owner, c.worldBody());
        owner.companion = null;
        owner.pet = null;
        owner.setGuid(UpdateFields.UNIT_FIELD_SUMMON, 0);
        owner.setControllingPet(false);
        sendSummonLink(owner);
        WorldSession s = owner.session;
        if (s != null) {
            WowBuffer hide = new WowBuffer(8);
            hide.putU64(0);
            s.send(Opcodes.SMSG_PET_SPELLS, hide.array());
        }
        CompanionPartyAddon.pushState(s);
    }

    static Creature spawnWorldBody(World world, Player owner, Player snap, Pet pet) {
        float[] pos = followPosition(owner);
        Creature c = new Creature();
        c.guid = pet.guid;
        c.entry = COMPANION_ENTRY_BASE + (Guid.low(snap.guid) & 0xFFFF);
        c.name = snap.name == null ? "Companion" : snap.name;
        c.mapId = owner.mapId;
        c.relocate(pos[0], pos[1], pos[2], owner.o);
        c.spawnX = c.x;
        c.spawnY = c.y;
        c.spawnZ = c.z;
        c.spawnO = c.o;
        c.pet = true;
        c.playerControlledPet = true;
        c.temporarySummon = true;
        int display = snap.displayId > 0 ? snap.displayId : 49;
        int hp = Math.max(1, snap.maxHealth() > 0 ? snap.maxHealth() : 100);
        int faction = owner.faction != 0 ? owner.faction : snap.faction;
        c.applyTemplate(c.entry, c.name, display, faction, hp, Math.max(1, snap.level));
        c.setGuid(UpdateFields.UNIT_FIELD_SUMMONEDBY, owner.guid);
        c.setGuid(UpdateFields.UNIT_FIELD_CREATEDBY, owner.guid);
        c.setInt(UpdateFields.UNIT_FIELD_PETNUMBER, Guid.low(snap.guid));
        int nameTimestamp = (int) (System.currentTimeMillis() / 1000L);
        pet.nameTimestamp = nameTimestamp;
        c.setInt(UpdateFields.UNIT_FIELD_PET_NAME_TIMESTAMP, nameTimestamp);
        c.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_PLAYER_CONTROLLED);
        int bytes2 = pet.unitBytes2() | (Player.PLAYER_CONTROLLED_DEBUFF_LIMIT << 8);
        c.setInt(UpdateFields.UNIT_FIELD_BYTES_2, bytes2);
        CompanionAppearance.applyOnSpawn(c, snap, world.objectMgr);
        world.map(owner.mapId, owner.instanceId).add(c);
        return c;
    }

    static float[] followPosition(Player owner) {
        float left = owner.o + FOLLOW_ANGLE;
        float x = owner.x + FOLLOW_DIST * (float) Math.cos(left);
        float y = owner.y + FOLLOW_DIST * (float) Math.sin(left);
        return new float[]{x, y, owner.z};
    }

    private static void despawnWorldBody(World world, Player owner, Creature body) {
        if (body == null) {
            return;
        }
        int instanceId = owner != null ? owner.instanceId : 0;
        GameMap map = world.map(body.mapId, instanceId);
        WowBuffer destroy = new WowBuffer(8);
        destroy.putU64(body.guid);
        byte[] payload = destroy.array();
        for (Player pl : map.nearbyPlayers(body, GameMap.VISIBILITY)) {
            WorldSession s = pl.session;
            if (s != null && s.hasSeen(body.guid)) {
                s.destroyObject(body.guid, payload);
            }
        }
        if (owner != null && owner.session != null && owner.session.hasSeen(body.guid)) {
            owner.session.destroyObject(body.guid, payload);
        }
        map.remove(body);
    }

    private static void revealBody(World world, Player owner, Creature body) {
        WorldSession s = owner.session;
        if (s == null) {
            return;
        }
        s.revealCreature(body, (int) world.nowMs());
        for (Player pl : world.map(owner.mapId, owner.instanceId).nearbyPlayers(body, GameMap.VISIBILITY)) {
            if (pl == owner || pl.session == null) {
                continue;
            }
            pl.session.revealCreature(body, (int) world.nowMs());
        }
    }

    private static void sendSummonLink(Player owner) {
        WorldSession s = owner.session;
        if (s == null) {
            return;
        }
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(owner,
                UpdateFields.UNIT_FIELD_SUMMON, UpdateFields.PLAYER_FIELD_BYTES));
        s.send(pkt.opcode(), pkt.payload());
    }

    private static Player findAccountCharacter(World world, int accountId, String name) {
        for (Player p : world.characters.enumAccount(accountId, world.objectMgr)) {
            if (p.name != null && p.name.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    static Pet buildPet(Player snap, ObjectMgr objectMgr) {
        Pet pet = new Pet();
        pet.guid = Guid.HIGH_PET | (Guid.low(snap.guid) & 0xFFFFFFFFL);
        pet.name = snap.name == null ? "Companion" : snap.name;
        pet.level = snap.level;
        pet.petType = Pet.SUMMON_PET;
        pet.summoned = true;
        pet.alive = true;
        pet.entry = COMPANION_ENTRY_BASE + (Guid.low(snap.guid) & 0xFFFF);
        pet.spells.clear();
        for (int spellId : snap.spells) {
            if (knownAtLevel(spellId, snap.level, objectMgr)
                    && hasCompleteRankChain(spellId, snap, objectMgr)) {
                pet.spells.add(spellId);
            }
        }
        PetHandlerBar.seedDefaults(pet.actionBar);
        int slot = PetHandlerBar.SPELL_SLOT_START;
        for (int i = 0; i < snap.actionButtons.length && slot < PetHandlerBar.SPELL_SLOT_END; i++) {
            int packed = snap.actionButtons[i];
            if (packed == 0) {
                continue;
            }
            int type = (packed >>> 24) & 0xFF;
            int action = packed & 0xFFFFFF;
            if (type != ACTION_SPELL || action == 0) {
                continue;
            }
            if (!pet.spells.contains(action)) {
                continue;
            }
            pet.actionBar[slot] = action | (PetHandlerBar.ACT_ENABLED << 24);
            slot++;
        }
        return pet;
    }

    private static boolean knownAtLevel(int spellId, int level, ObjectMgr objectMgr) {
        int baseLevel = objectMgr == null ? 0 : objectMgr.spellBaseLevel.getOrDefault(spellId, 0);
        return baseLevel <= 0 || baseLevel <= level;
    }

    private static boolean hasCompleteRankChain(int spellId, Player snap, ObjectMgr objectMgr) {
        if (objectMgr == null) {
            return true;
        }
        int current = spellId;
        int remaining = objectMgr.spellChain.size() + 1;
        while (remaining-- > 0) {
            ObjectMgr.SpellChainNode node = objectMgr.spellChain.get(current);
            if (node == null || node.prev() == 0) {
                return true;
            }
            if (!snap.spells.contains(node.prev())) {
                return false;
            }
            current = node.prev();
        }
        return false;
    }

    private static void sendBar(Player owner) {
        WorldSession s = owner.session;
        if (s == null || owner.pet == null) {
            return;
        }
        s.send(Opcodes.SMSG_PET_SPELLS, PetHandler.encodeBarPublic(owner.pet));
    }
}
