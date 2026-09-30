package org.tbc.world.combat;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.SpellEngine;

/**
 * Mainhand weapon speed / min-max for SoR — prefer synced {@code UNIT_FIELD_*} (8606 tip tokens
 * {@code $MW}/{@code $mw}/{@code $MWS} from stock Seal strings), else item proto.
 */
public record MainhandWeaponStats(float speedSec, float avgDamage, int delayMs, boolean twoHand,
                                  float damageMin, float damageMax) {

    private static final int DEFAULT_DELAY_MS = 2000;
    private static final float FIST_MIN = 1.0f;
    private static final float FIST_MAX = 3.0f;

    public static final int SUBCLASS_AXE2 = 1;
    public static final int SUBCLASS_MACE2 = 5;
    public static final int SUBCLASS_POLEARM = 6;
    public static final int SUBCLASS_SWORD2 = 8;
    public static final int SUBCLASS_STAFF = 10;
    public static final int SUBCLASS_SPEAR = 17;

    public MainhandWeaponStats(float speedSec, float avgDamage, int delayMs, boolean twoHand) {
        this(speedSec, avgDamage, delayMs, twoHand, avgDamage, avgDamage);
    }

    public static MainhandWeaponStats from(Player player) {
        if (player == null) {
            return fists();
        }
        Item mh = player.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND);
        int attackTime = player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME);
        float min = player.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE);
        float max = player.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE);
        // Stock SoR tip uses $MW/$mw/$MWS from the unit — prefer UNIT when not still fists.
        if (!isFistDefaultUnit(attackTime, min, max)) {
            int delayMs = attackTime > 0 ? attackTime : DEFAULT_DELAY_MS;
            return new MainhandWeaponStats(delayMs / 1000f, (min + max) / 2f, delayMs, isTwoHand(mh), min, max);
        }
        if (mh != null && mh.delay > 0) {
            return new MainhandWeaponStats(mh.delay / 1000f, (mh.dmgMin + mh.dmgMax) / 2f, mh.delay,
                    isTwoHand(mh), mh.dmgMin, mh.dmgMax);
        }
        int delayMs = attackTime > 0 ? attackTime : DEFAULT_DELAY_MS;
        return new MainhandWeaponStats(delayMs / 1000f, (min + max) / 2f, delayMs, isTwoHand(mh), min, max);
    }

    static boolean isFistDefaultUnit(int attackTimeMs, float minDamage, float maxDamage) {
        int delay = attackTimeMs > 0 ? attackTimeMs : DEFAULT_DELAY_MS;
        return delay == DEFAULT_DELAY_MS
                && minDamage <= FIST_MIN + 1e-4f
                && maxDamage <= FIST_MAX + 1e-4f;
    }

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
        return new MainhandWeaponStats(DEFAULT_DELAY_MS / 1000f, 0f, DEFAULT_DELAY_MS, false, 0f, 0f);
    }
}
