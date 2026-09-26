package org.tbc.world.combat;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.SpellEngine;

/**
 * Mainhand weapon speed / average damage for procs that match the 8606 client tooltip
 * (item proto first, then {@code UNIT_FIELD_*}).
 */
public record MainhandWeaponStats(float speedSec, float avgDamage, int delayMs, boolean twoHand) {

    private static final int DEFAULT_DELAY_MS = 2000;

    /** ItemPrototype.h ITEM_SUBCLASS_WEAPON_* that are always two-handed for SoR $HND. */
    public static final int SUBCLASS_AXE2 = 1;
    public static final int SUBCLASS_MACE2 = 5;
    public static final int SUBCLASS_POLEARM = 6;
    public static final int SUBCLASS_SWORD2 = 8;
    public static final int SUBCLASS_STAFF = 10;
    public static final int SUBCLASS_SPEAR = 17;

    public static MainhandWeaponStats from(Player player) {
        if (player == null) {
            return fists();
        }
        Item mh = player.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND);
        if (mh != null && mh.delay > 0) {
            float avg = (mh.dmgMin + mh.dmgMax) / 2f;
            return new MainhandWeaponStats(mh.delay / 1000f, avg, mh.delay, isTwoHand(mh));
        }
        int attackTime = player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME);
        int delayMs = attackTime > 0 ? attackTime : DEFAULT_DELAY_MS;
        float min = player.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE);
        float max = player.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE);
        return new MainhandWeaponStats(delayMs / 1000f, (min + max) / 2f, delayMs, isTwoHand(mh));
    }

    /**
     * Client SoR $HND is 2 for INVTYPE_2HWEAPON; also treat 2H weapon subclasses as two-hand when
     * SQL InventoryType was left as 13/21 (combat log otherwise uses the 1H formula → trunc 5).
     */
    public static boolean isTwoHand(Item mh) {
        if (mh == null) {
            return false;
        }
        if (mh.inventoryType == SpellEngine.INVTYPE_2HWEAPON) {
            return true;
        }
        return isTwoHandSubclass(mh.subClass);
    }

    public static boolean isTwoHandSubclass(int subClass) {
        return switch (subClass) {
            case SUBCLASS_AXE2, SUBCLASS_MACE2, SUBCLASS_POLEARM, SUBCLASS_SWORD2, SUBCLASS_STAFF,
                 SUBCLASS_SPEAR -> true;
            default -> false;
        };
    }

    private static MainhandWeaponStats fists() {
        return new MainhandWeaponStats(DEFAULT_DELAY_MS / 1000f, 0f, DEFAULT_DELAY_MS, false);
    }
}
