package org.tbc.world.companion;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.persist.CharacterStore;
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
        snap.session = null;
        Pet pet = buildPet(snap);
        int[] saved = savedBars.get(snap.guid);
        if (saved != null) {
            System.arraycopy(saved, 0, pet.actionBar, 0, Math.min(saved.length, pet.actionBar.length));
        }
        Companion companion = new Companion(snap.guid, snap, pet);
        owner.companion = companion;
        owner.pet = pet;
        sendBar(owner);
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
        owner.companion = null;
        owner.pet = null;
        WorldSession s = owner.session;
        if (s != null) {
            WowBuffer hide = new WowBuffer(8);
            hide.putU64(0);
            s.send(Opcodes.SMSG_PET_SPELLS, hide.array());
        }
    }

    private static Player findAccountCharacter(World world, int accountId, String name) {
        for (Player p : world.characters.enumAccount(accountId, world.objectMgr)) {
            if (p.name != null && p.name.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    static Pet buildPet(Player snap) {
        Pet pet = new Pet();
        pet.guid = Guid.HIGH_CREATURE | (Guid.low(snap.guid) & 0xFFFFFFFFL);
        pet.name = snap.name == null ? "Companion" : snap.name;
        pet.level = snap.level;
        pet.petType = Pet.SUMMON_PET;
        pet.summoned = true;
        pet.alive = true;
        pet.spells.clear();
        pet.spells.addAll(snap.spells);
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
            if (!snap.spells.contains(action)) {
                continue;
            }
            pet.actionBar[slot] = action | (PetHandlerBar.ACT_ENABLED << 24);
            slot++;
        }
        return pet;
    }

    private static void sendBar(Player owner) {
        WorldSession s = owner.session;
        if (s == null || owner.pet == null) {
            return;
        }
        s.send(Opcodes.SMSG_PET_SPELLS, PetHandler.encodeBarPublic(owner.pet));
    }
}
