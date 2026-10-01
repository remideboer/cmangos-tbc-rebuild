package org.tbc.world.classless;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;

/** Untrained weapons still swing; hit lag prefers skill, plus configurable miss add. */
public final class WeaponPenaltyPolicy {
    private WeaponPenaltyPolicy() {
    }

    public static boolean isProficientWith(Player p, Item weapon) {
        if (p == null || weapon == null) {
            return true;
        }
        if (weapon.itemClass != 0 && weapon.itemClass != Player.ITEM_CLASS_WEAPON) {
            return true;
        }
        // Unarmed / empty treated as proficient when cloth-era unarmed mask is set.
        if (weapon.itemClass == 0 && weapon.subClass == 0) {
            return (p.weaponProficiency() & ClasslessConfig.WEAPON_UNARMED_MASK) != 0;
        }
        int mask = 1 << weapon.subClass;
        return (p.weaponProficiency() & mask) != 0;
    }

    /** Extra miss chance in percent points (0–100 scale before /100). */
    public static double missAddPercent(Player p, boolean offhand) {
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return 0;
        }
        int slot = offhand ? Player.EQUIPMENT_SLOT_OFFHAND : Player.EQUIPMENT_SLOT_MAINHAND;
        Item weapon = p.itemAt(0, slot);
        if (weapon == null) {
            return 0;
        }
        if (isProficientWith(p, weapon)) {
            return 0;
        }
        return ClasslessConfig.get().untrainedWeaponMissAddPct();
    }

    public static float damageMult(Player p, boolean offhand) {
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return 1f;
        }
        int slot = offhand ? Player.EQUIPMENT_SLOT_OFFHAND : Player.EQUIPMENT_SLOT_MAINHAND;
        Item weapon = p.itemAt(0, slot);
        if (weapon == null || isProficientWith(p, weapon)) {
            return 1f;
        }
        return ClasslessConfig.get().untrainedWeaponDamageMult();
    }
}
