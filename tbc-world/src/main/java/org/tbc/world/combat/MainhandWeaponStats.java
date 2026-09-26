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

    public static MainhandWeaponStats from(Player player) {
        if (player == null) {
            return fists();
        }
        Item mh = player.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND);
        if (mh != null && mh.delay > 0) {
            float avg = (mh.dmgMin + mh.dmgMax) / 2f;
            return new MainhandWeaponStats(mh.delay / 1000f, avg, mh.delay,
                    mh.inventoryType == SpellEngine.INVTYPE_2HWEAPON);
        }
        int attackTime = player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME);
        int delayMs = attackTime > 0 ? attackTime : DEFAULT_DELAY_MS;
        float min = player.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE);
        float max = player.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE);
        boolean twoHand = mh != null && mh.inventoryType == SpellEngine.INVTYPE_2HWEAPON;
        return new MainhandWeaponStats(delayMs / 1000f, (min + max) / 2f, delayMs, twoHand);
    }

    private static MainhandWeaponStats fists() {
        return new MainhandWeaponStats(DEFAULT_DELAY_MS / 1000f, 0f, DEFAULT_DELAY_MS, false);
    }
}
