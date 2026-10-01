package org.tbc.world.content;

import org.tbc.common.DbPool;
import org.tbc.common.WowBuffer;
import org.tbc.world.session.QueryHandler;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TP-SL26-171 — item_template.sheath must reach SMSG_ITEM_QUERY_SINGLE_RESPONSE so the 8606
 * client attaches sheathed weapons/shields (SHEATHETYPE 3 / 4), not SHEATHETYPE_NONE (0).
 */
class ObjectMgrItemSheathTest {
    @Test
    void seedTemplatesWhenQueriedShouldCarrySheathTypesForSwordAndShield() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        ObjectMgr.ItemTemplate sword = mgr.items.get(Content.ITEM_WORN_SHORTSWORD);
        ObjectMgr.ItemTemplate shield = mgr.items.get(Content.ITEM_WORN_WOODEN_SHIELD);
        assertNotNull(sword);
        assertNotNull(shield);
        assertEquals(3, sword.sheath, "Worn Shortsword sheath");
        assertEquals(4, shield.sheath, "Worn Wooden Shield sheath");
        assertEquals(3, sheathFromQuery(sword));
        assertEquals(4, sheathFromQuery(shield));
    }

    @Test
    void loadItemsWhenTemplateHasSheathShouldMergeOntoLoadedRows() throws Exception {
        String url = "jdbc:h2:mem:sheath_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "item-sheath-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE item_template (
                          entry INT,
                          class INT,
                          subclass INT,
                          name VARCHAR(255),
                          displayid INT,
                          Quality INT,
                          Flags INT,
                          BuyPrice INT,
                          SellPrice INT,
                          InventoryType INT,
                          AllowableClass INT,
                          AllowableRace INT,
                          ItemLevel INT,
                          RequiredLevel INT,
                          maxcount INT,
                          stackable INT,
                          ContainerSlots INT,
                          armor INT,
                          delay INT,
                          bonding INT,
                          description VARCHAR(255),
                          MaxDurability INT,
                          Duration INT,
                          RequiredDisenchantSkill INT,
                          sheath INT
                        )
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, sheath)
                        VALUES (25, 2, 7, 'Worn Shortsword', 1542, 1, 0, 35, 7, 21, 32767, 511, 2, 1, 0,
                          1, 0, 0, 1900, 0, '', 20, 0, -1, 3)
                        """);
                st.execute("""
                        INSERT INTO item_template (
                          entry, class, subclass, name, displayid, Quality, Flags, BuyPrice, SellPrice,
                          InventoryType, AllowableClass, AllowableRace, ItemLevel, RequiredLevel, maxcount,
                          stackable, ContainerSlots, armor, delay, bonding, description, MaxDurability,
                          Duration, RequiredDisenchantSkill, sheath)
                        VALUES (2362, 4, 6, 'Worn Wooden Shield', 18730, 0, 0, 7, 1, 14, -1, -1, 1, 1, 0,
                          1, 0, 5, 0, 0, '', 20, 0, -1, 4)
                        """);
            }
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(worldDb, null);
            ObjectMgr.ItemTemplate sword = mgr.items.get(Content.ITEM_WORN_SHORTSWORD);
            ObjectMgr.ItemTemplate shield = mgr.items.get(Content.ITEM_WORN_WOODEN_SHIELD);
            assertNotNull(sword);
            assertNotNull(shield);
            assertEquals(3, sword.sheath);
            assertEquals(4, shield.sheath);
            assertEquals(3, sheathFromQuery(sword));
            assertEquals(4, sheathFromQuery(shield));
        }
    }

    /** bonding + description C-string + pageText..material (5×u32) → sheath u32. */
    private static int sheathFromQuery(ObjectMgr.ItemTemplate t) {
        byte[] wire = QueryHandler.encodeItemQuery(t);
        int off = findBondingOffset(wire, t);
        off += 4; // bonding
        while (off < wire.length && wire[off] != 0) {
            off++;
        }
        off++; // description NUL
        off += 5 * 4; // pageText, languageId, pageMaterial, startQuest, lockId
        off += 4; // material
        return u32le(wire, off);
    }

    private static int findBondingOffset(byte[] wire, ObjectMgr.ItemTemplate t) {
        int off = 4 + 12; // entry + class/subclass/unk
        while (off < wire.length && wire[off] != 0) {
            off++;
        }
        off++; // name NUL
        off += 3; // empty name slots
        // displayId .. rangedModRange float, then 5×6 spell u32s, then bonding
        off += 19 * 4; // displayId through stackable (19 fields: display..stackable)
        // Wait: displayId..containerSlots = 20? Stackable test: 17*4 after names to maxCount.
        // Recompute from writeItem: after 3 empty name bytes:
        // displayId, quality, flags, buy, sell, invType, allowClass, allowRace, itemLevel,
        // reqLevel, reqSkill, reqSkillRank, reqSpell, reqHonor, reqCity, reqRepFaction, reqRepRank,
        // maxCount, stackable, containerSlots = 20 u32
        // then 10*(statType+statValue)=20 u32, 5*(dmgMin float + dmgMax float + dmgType)=5*12 bytes
        // armor..ammoType = 9 u32, rangedModRange float, 5*6 spell u32, bonding
        off = 4 + 12;
        while (off < wire.length && wire[off] != 0) {
            off++;
        }
        off++;
        off += 3;
        off += 20 * 4; // through containerSlots
        off += 20 * 4; // stats
        off += 5 * (4 + 4 + 4); // damage lines
        off += 9 * 4; // armor + 6 resists + delay + ammoType
        off += 4; // rangedModRange float
        off += 5 * 6 * 4; // spells
        assertEquals(t.bonding, u32le(wire, off));
        return off;
    }

    private static int u32le(byte[] b, int off) {
        return (b[off] & 0xFF)
                | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16)
                | ((b[off + 3] & 0xFF) << 24);
    }
}
