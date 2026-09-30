package org.tbc.world.content;

import org.tbc.world.entity.Creature;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vendor gossip/list requires stock from npc_vendor and/or VendorTemplateId → npc_vendor_template.
 * Falconwing Sleyin 18926: VendorTemplateId 1001 (common weapons), not entry-keyed npc_vendor.
 */
class ObjectMgrVendorResolveTest {
    @Test
    void itemsForVendorWhenWeaponVendorUsesTemplateShouldListCommonWeapons() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(18926, new ObjectMgr.CreatureTemplate(
                18926, "Sleyin", 0, 1604, 100, 30,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_VENDOR | Content.UNIT_NPC_FLAG_REPAIR,
                "", "", 0));
        mgr.vendorTemplateId.put(18926, 1001);
        mgr.vendorTemplateItems.put(1001, new ArrayList<>(List.of(2488, 2489, 2490, 2491)));

        List<Integer> stock = mgr.itemsForVendor(18926);
        assertEquals(List.of(2488, 2489, 2490, 2491), stock);

        Creature npc = new Creature();
        npc.entry = 18926;
        npc.npcFlags = Content.UNIT_NPC_FLAG_VENDOR;
        assertTrue(mgr.hasVendorStock(npc));

        Creature empty = new Creature();
        empty.entry = 99998;
        empty.npcFlags = Content.UNIT_NPC_FLAG_VENDOR;
        assertFalse(mgr.hasVendorStock(empty));
    }

    @Test
    void itemsForVendorWhenEntryAndTemplateShouldMergeWithoutDupes() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.vendorItems.put(50, new ArrayList<>(List.of(25, 2488)));
        mgr.vendorTemplateId.put(50, 1001);
        mgr.vendorTemplateItems.put(1001, new ArrayList<>(List.of(2488, 2489)));
        assertEquals(List.of(25, 2488, 2489), mgr.itemsForVendor(50));
    }
}
