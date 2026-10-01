package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;

import java.util.function.LongSupplier;

/**
 * Auto Attack only on the bar, cloth + unarmed proficiency, Recruit cloth + Worn Shortsword,
 * and 3 silver. Spawn uses the race's warrior create coords.
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
            ObjectMgr.CreateInfo warriorStart = mgr.create(p.race, 1);
            if (warriorStart != null) {
                p.mapId = warriorStart.map();
                p.zoneId = warriorStart.zone();
                p.relocate(warriorStart.x(), warriorStart.y(), warriorStart.z(), warriorStart.o());
                p.bindMap = warriorStart.map();
                p.bindZone = warriorStart.zone();
                p.bindX = warriorStart.x();
                p.bindY = warriorStart.y();
                p.bindZ = warriorStart.z();
            }
            mgr.giveNamedStartItems(p, ClasslessConfig.STARTING_ITEMS, nextItemGuid);
        }
    }
}
