package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.content.WeaponSkills;
import org.tbc.world.entity.Player;

import java.util.function.LongSupplier;

/**
 * Auto Attack only on the bar, full weapon/armor proficiencies with combat skills at 1,
 * Recruit cloth + Worn Shortsword + Hearthstone, starter food/water/potion, and 3 silver.
 * Homebind and spawn use the race's normal starter ({@link CreateSpawnResolver}).
 */
public final class ClasslessStartingLoadout {
    private ClasslessStartingLoadout() {
    }

    public static void apply(Player p, ObjectMgr mgr, LongSupplier nextItemGuid) {
        if (p == null) {
            return;
        }
        p.spells.clear();
        p.spells.add(ClasslessConfig.AUTO_ATTACK);
        for (int i = 0; i < p.actionButtons.length; i++) {
            p.actionButtons[i] = 0;
        }
        p.actionButtons[0] = ClasslessConfig.AUTO_ATTACK;
        p.addArmorProficiency(ClasslessConfig.ALL_ARMOR_PROFICIENCY_MASK);
        p.addWeaponProficiency(ClasslessConfig.ALL_WEAPON_PROFICIENCY_MASK);
        applyCombatSkills(p);
        p.setMoney(ClasslessConfig.STARTING_MONEY_COPPER);
        if (mgr != null && nextItemGuid != null) {
            CreateSpawnResolver.SpawnChoice start = CreateSpawnResolver.resolve(mgr, p.race, null);
            if (start != null) {
                p.mapId = start.map();
                p.zoneId = start.zone();
                p.relocate(start.x(), start.y(), start.z(), start.o());
                p.bindMap = start.map();
                p.bindZone = start.zone();
                p.bindX = start.x();
                p.bindY = start.y();
                p.bindZ = start.z();
            }
            mgr.giveNamedStartItems(p, ClasslessConfig.STARTING_ITEMS, nextItemGuid);
            for (int[] stack : ClasslessConfig.STARTING_STACKS) {
                mgr.storeNewItem(p, stack[0], stack[1], nextItemGuid);
            }
        }
    }

    /** Combat weapon SkillLines + defense at value 1 / max level×5 (same as create skills). */
    static void applyCombatSkills(Player p) {
        int max = Math.max(1, p.level * 5);
        for (int sub = 0; sub < WeaponSkills.ITEM_SUBCLASS_WEAPON_FISHING_POLE; sub++) {
            int skill = WeaponSkills.skillForWeaponSubclass(sub);
            if (skill != 0) {
                p.learnSkill(skill, 1, max, 0);
            }
        }
        p.learnSkill(WeaponSkills.SKILL_DEFENSE, 1, max, 0);
    }
}
