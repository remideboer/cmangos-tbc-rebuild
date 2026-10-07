package org.tbc.world.content;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.net.wow8606.Opcodes;

/** SMSG_SHOW_BANK and bank-slot constants owned by the Banker split out of Content (plan cycle 5.2). */
class BankerTest {

    @Test
    void sendShowBankWhenNpcIsBankerShouldEmitRawGuid() {
        Creature c = new Creature();
        c.guid = 0xF130000000000009L;
        c.npcFlags = Banker.UNIT_NPC_FLAG_BANKER;
        List<Integer> opcodes = new ArrayList<>();
        List<byte[]> payloads = new ArrayList<>();

        Banker.sendShowBank(c, (op, bytes) -> { opcodes.add(op); payloads.add(bytes); });

        assertEquals(List.of(Opcodes.SMSG_SHOW_BANK), opcodes);
        WowBuffer expected = new WowBuffer(8);
        expected.putU64(c.guid);
        assertArrayEquals(expected.array(), payloads.get(0));
    }

    @Test
    void sendShowBankWhenNpcIsNotBankerShouldSendNothing() {
        Creature c = new Creature();
        c.npcFlags = Banker.UNIT_NPC_FLAG_BANKER ^ Banker.UNIT_NPC_FLAG_BANKER;
        List<Integer> opcodes = new ArrayList<>();

        Banker.sendShowBank(c, (op, bytes) -> opcodes.add(op));

        assertTrue(opcodes.isEmpty());
    }

    @Test
    void bankBagSlotPricesShouldMatchCMaNGOSTable() {
        assertArrayEquals(new int[] {0, 1000, 10_000, 100_000, 250_000, 500_000, 1_000_000, 2_000_000},
                Banker.BANK_BAG_SLOT_PRICES);
        assertEquals(3, Banker.ERR_BANKSLOT_OK);
    }
}
