package org.tbc.world.companion;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.WorldSession;

/**
 * Mirror-image appearance for companion Creature bodies (CMaNGOS HandleAuraMirrorImage /
 * HandleGetMirrorimageData).
 */
public final class CompanionAppearance {
    /** Unit.h UNIT_FLAG2_CLONED — client expects this on player-model clones. */
    public static final int UNIT_FLAG2_CLONED = 0x10;

    /** SharedDefines equipment slots used by SMSG_MIRRORIMAGE_DATA gear block. */
    private static final int SLOT_HEAD = 0;
    private static final int SLOT_SHOULDERS = 2;
    private static final int SLOT_BODY = 3;
    private static final int SLOT_CHEST = 4;
    private static final int SLOT_WAIST = 5;
    private static final int SLOT_LEGS = 6;
    private static final int SLOT_FEET = 7;
    private static final int SLOT_WRISTS = 8;
    private static final int SLOT_HANDS = 9;
    private static final int SLOT_BACK = 14;
    private static final int SLOT_TABARD = 18;

    private static final int[] MIRROR_SLOTS = {
            SLOT_HEAD, SLOT_SHOULDERS, SLOT_BODY, SLOT_CHEST, SLOT_WAIST, SLOT_LEGS,
            SLOT_FEET, SLOT_WRISTS, SLOT_HANDS, SLOT_BACK, SLOT_TABARD
    };

    private CompanionAppearance() {}

    /** Apply race/class/gender, clone flag, mirror aura, and weapon virtual items. */
    public static void applyOnSpawn(Creature body, Player snap, ObjectMgr mgr) {
        if (body == null || snap == null) {
            return;
        }
        int bytes0 = (snap.race & 0xFF)
                | ((snap.clazz & 0xFF) << 8)
                | ((snap.gender & 0xFF) << 16)
                | ((snap.powerType & 0xFF) << 24);
        body.setInt(UpdateFields.UNIT_FIELD_BYTES_0, bytes0);
        int f2 = body.getInt(UpdateFields.UNIT_FIELD_FLAGS_2);
        body.setInt(UpdateFields.UNIT_FIELD_FLAGS_2, f2 | UNIT_FLAG2_CLONED);
        body.mirrorImageCasterGuid = snap.guid;
        if (!body.hasAura(WorldSession.SPELL_MIRROR_IMAGE)) {
            body.auras.add(new Unit.Aura(WorldSession.SPELL_MIRROR_IMAGE, 0, 1));
        }
        applyVirtualWeapons(body, snap, mgr);
        copyMeleeStats(body, snap);
    }

    static void applyVirtualWeapons(Creature body, Player snap, ObjectMgr mgr) {
        applyVirtualSlot(body, 0, snap.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND), mgr);
        applyVirtualSlot(body, 1, snap.itemAt(0, Player.EQUIPMENT_SLOT_OFFHAND), mgr);
        applyVirtualSlot(body, 2, snap.itemAt(0, Player.EQUIPMENT_SLOT_RANGED), mgr);
    }

    private static void applyVirtualSlot(Creature body, int virtSlot, Item it, ObjectMgr mgr) {
        if (it == null) {
            body.clearVirtualItem(virtSlot);
            return;
        }
        ObjectMgr.ItemTemplate t = mgr != null ? mgr.items.get(it.entry) : null;
        int display = it.displayId > 0 ? it.displayId : (t != null ? t.displayId : 0);
        int itemClass = it.itemClass != 0 ? it.itemClass : (t != null ? t.itemClass : 0);
        int subClass = it.subClass != 0 ? it.subClass : (t != null ? t.subClass : 0);
        int material = t != null ? t.material : 0;
        int invType = it.inventoryType != 0 ? it.inventoryType : (t != null ? t.inventoryType : 0);
        int sheath = t != null ? t.sheath : 0;
        int unk = t != null ? t.unk : 0;
        body.setVirtualItem(virtSlot, display, itemClass, subClass, unk, material, invType, sheath);
    }

    static void copyMeleeStats(Creature body, Player snap) {
        float min = snap.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE);
        float max = snap.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE);
        int atk = snap.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME);
        if (min <= 0f && max <= 0f) {
            min = 1f;
            max = 3f;
        }
        if (atk <= 0) {
            atk = 2000;
        }
        body.applyCombatStats(min, max, atk, 1.5f);
    }

    /** Eleven equipment display ids for SMSG_MIRRORIMAGE_DATA (CMaNGOS slot order). */
    public static int[] mirrorEquipmentDisplays(Player snap, ObjectMgr mgr) {
        int[] out = new int[11];
        if (snap == null) {
            return out;
        }
        int flags = snap.getInt(UpdateFields.PLAYER_FLAGS);
        for (int i = 0; i < MIRROR_SLOTS.length; i++) {
            int slot = MIRROR_SLOTS[i];
            if (slot == SLOT_HEAD && (flags & Player.PLAYER_FLAGS_HIDE_HELM) != 0) {
                continue;
            }
            if (slot == SLOT_BACK && (flags & Player.PLAYER_FLAGS_HIDE_CLOAK) != 0) {
                continue;
            }
            out[i] = displayInSlot(snap, slot, mgr);
        }
        return out;
    }

    public static int displayInSlot(Player snap, int slot, ObjectMgr mgr) {
        Item it = snap.itemAt(0, slot);
        if (it == null) {
            return 0;
        }
        if (it.displayId > 0) {
            return it.displayId;
        }
        if (mgr != null) {
            ObjectMgr.ItemTemplate t = mgr.items.get(it.entry);
            if (t != null) {
                return t.displayId;
            }
        }
        return 0;
    }

    /** Resolve appearance source: live caster, else companion snapshot matching mirrorImageCasterGuid. */
    public static Player resolveAppearanceSource(Player viewer, Creature clone, org.tbc.world.world.World world) {
        if (clone == null) {
            return null;
        }
        Player live = world != null ? world.playerByGuid(clone.mirrorImageCasterGuid) : null;
        if (live != null) {
            return live;
        }
        if (viewer != null && viewer.companion != null && viewer.companion.worldBody() == clone) {
            return viewer.companion.snapshot();
        }
        if (viewer != null && viewer.companion != null
                && viewer.companion.sourceGuid() == clone.mirrorImageCasterGuid) {
            return viewer.companion.snapshot();
        }
        return null;
    }
}
