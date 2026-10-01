package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.function.LongSupplier;

/** Identifies classless mode and applies create/login power + stats behavior. */
public final class ClasslessCharacterPolicy {
    private ClasslessCharacterPolicy() {
    }

    public static boolean isClassless(Player p) {
        return p != null && ClasslessConfig.isClasslessId(p.clazz) && ClasslessConfig.get().enabled();
    }

    public static boolean isClasslessClass(int clazz) {
        return ClasslessConfig.isClasslessId(clazz) && ClasslessConfig.get().enabled();
    }

    /**
     * Primary power for {@code UNIT_FIELD_BYTES_0} is mana; also open rage and energy pools
     * (Ascension-style). LUA multi-bar UI is a later lab task.
     */
    public static void applyPowers(Player p) {
        if (p == null) {
            return;
        }
        ClasslessConfig cfg = ClasslessConfig.get();
        p.powerType = Player.POWER_MANA;
        int maxMana = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        if (maxMana <= 0) {
            p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, cfg.baseMana());
            maxMana = cfg.baseMana();
        }
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, maxMana);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, Player.POWER_RAGE_MAX);
        if (p.getInt(UpdateFields.UNIT_FIELD_POWER2) < 0) {
            p.setInt(UpdateFields.UNIT_FIELD_POWER2, 0);
        }
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, Player.POWER_ENERGY_MAX);
        if (p.getInt(UpdateFields.UNIT_FIELD_POWER4) <= 0) {
            p.setInt(UpdateFields.UNIT_FIELD_POWER4, Player.POWER_ENERGY_MAX);
        }
    }

    /** Dedicated level-1 stats — not a warrior/mage fallback. */
    public static void applyStartingStats(Player p) {
        if (p == null) {
            return;
        }
        ClasslessConfig cfg = ClasslessConfig.get();
        p.applyClasslessCreateStats(
                cfg.baseHealth(), cfg.baseMana(),
                cfg.str(), cfg.agi(), cfg.sta(), cfg.inte(), cfg.spi());
    }

    public static void applyCreate(Player p, ObjectMgr mgr, LongSupplier nextItemGuid) {
        if (p == null || !isClassless(p)) {
            return;
        }
        ClasslessStartingLoadout.apply(p, mgr, nextItemGuid);
        applyStartingStats(p);
    }

    /** Re-apply cloth + unarmed if persist did not carry proficiency masks. */
    public static void ensureStartingProficiencies(Player p) {
        if (!isClassless(p)) {
            return;
        }
        if ((p.armorProficiency() & ClasslessConfig.ARMOR_CLOTH_MASK) == 0) {
            p.addArmorProficiency(ClasslessConfig.ARMOR_CLOTH_MASK);
        }
        if ((p.weaponProficiency() & ClasslessConfig.WEAPON_UNARMED_MASK) == 0) {
            p.addWeaponProficiency(ClasslessConfig.WEAPON_UNARMED_MASK);
        }
    }
}
