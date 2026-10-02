package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;

import java.util.function.LongSupplier;

/**
 * Auto Attack only on the bar, cloth + unarmed proficiency, Recruit cloth + Worn Shortsword,
 * and 3 silver. Spawn uses the race's normal starter ({@link CreateSpawnResolver}).
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
        p.addArmorProficiency(ClasslessConfig.ARMOR_CLOTH_MASK);
        p.addWeaponProficiency(ClasslessConfig.WEAPON_UNARMED_MASK);
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
        }
    }
}
