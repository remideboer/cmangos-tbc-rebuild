package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Creature;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.script.ScriptRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureEquipmentSpawnTest {
    @Test
    void spawnWhenEquipmentTemplatePointsAtItemShouldSetVirtualDisplay() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(15271, new ObjectMgr.CreatureTemplate(
                15271, "Mana Wyrm", 1, 14, 40, 1, 0, "", "", 0));
        ObjectMgr.ItemTemplate sword = new ObjectMgr.ItemTemplate();
        sword.entry = 25;
        sword.displayId = 1542;
        sword.itemClass = 2;
        sword.subClass = 7;
        sword.unk = 0;
        sword.material = 1;
        sword.inventoryType = 13;
        sword.sheath = 3;
        mgr.items.put(25, sword);
        mgr.equipmentByEntry.put(15271, 9);
        mgr.equipmentItems.put(9, new int[] {25, 0, 0});
        Creature creature = mgr.spawnCreature(new ObjectMgr.Spawn(1, 15271, 530, 1f, 2f, 3f, 0f), new ScriptRegistry());
        assertEquals(1542, creature.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_SLOT_DISPLAY));
        assertEquals(2, creature.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_INFO) & 0xFF);
        assertEquals(7, (creature.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_INFO) >> 8) & 0xFF);
        assertEquals(1, (creature.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_INFO) >> 24) & 0xFF);
        assertEquals(13, creature.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_INFO + 1) & 0xFF);
        assertEquals(3, (creature.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_INFO + 1) >> 8) & 0xFF);
        assertEquals(0, creature.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_SLOT_DISPLAY + 1));
    }
}
