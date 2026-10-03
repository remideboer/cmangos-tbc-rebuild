package org.tbc.world.classless;

import org.tbc.world.content.LevelStats;
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

    /** Flat L1 abilities from per-race class average; HP/mana from ClasslessConfig. */
    public static void applyStartingStats(Player p) {
        applyStartingStats(p, LevelStats.defaults());
    }

    public static void applyStartingStats(Player p, LevelStats levelStats) {
        if (p == null) {
            return;
        }
        ClasslessConfig cfg = ClasslessConfig.get();
        LevelStats ls = levelStats != null ? levelStats : LevelStats.defaults();
        LevelStats.Stats st = ls.averageStats(p.race, 1);
        p.applyClasslessCreateStats(ls,
                cfg.baseHealth(), cfg.baseMana(),
                st.str() + p.heroStats.spent(HeroStatAllocation.STR),
                st.agi() + p.heroStats.spent(HeroStatAllocation.AGI),
                st.sta() + p.heroStats.spent(HeroStatAllocation.STA),
                st.inte() + p.heroStats.spent(HeroStatAllocation.INTELLECT),
                st.spi() + p.heroStats.spent(HeroStatAllocation.SPI));
    }

    public static void applyCreate(Player p, ObjectMgr mgr, LongSupplier nextItemGuid) {
        if (p == null || !isClassless(p)) {
            return;
        }
        ClasslessStartingLoadout.apply(p, mgr, nextItemGuid);
        LevelStats ls = mgr != null && mgr.levelStats != null ? mgr.levelStats : LevelStats.defaults();
        applyStartingStats(p, ls);
    }

    /** Re-apply full weapon/armor proficiency masks if persist did not carry them. */
    public static void ensureStartingProficiencies(Player p) {
        if (!isClassless(p)) {
            return;
        }
        p.addArmorProficiency(ClasslessConfig.ALL_ARMOR_PROFICIENCY_MASK);
        p.addWeaponProficiency(ClasslessConfig.ALL_WEAPON_PROFICIENCY_MASK);
        refreshCombatSkillMax(p);
    }

    /** Raise Hero combat weapon + defense skill max to level×5; keep current values. */
    public static void refreshCombatSkillMax(Player p) {
        if (!isClassless(p)) {
            return;
        }
        ClasslessStartingLoadout.refreshCombatSkillMax(p);
    }
}
