package org.tbc.world.content;

import java.util.function.BiConsumer;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.net.wow8606.Opcodes;

/** NPC banker: SMSG_SHOW_BANK and the bank-bag-slot price table (split out of Content). */
public final class Banker {

    public static final int UNIT_NPC_FLAG_BANKER = 0x00020000;
    /** Player.h BuyBankSlotResult. */
    public static final int ERR_BANKSLOT_FAILED_TOO_MANY = 0;
    public static final int ERR_BANKSLOT_INSUFFICIENT_FUNDS = 1;
    public static final int ERR_BANKSLOT_NOTBANKER = 2;
    public static final int ERR_BANKSLOT_OK = 3;
    /**
     * BankBagSlotPrices.dbc id → copper. Index 0 unused.
     * Slot 1..7: 10s, 1g, 10g, 25g, 50g, 100g, 200g.
     */
    public static final int[] BANK_BAG_SLOT_PRICES = {0, 1000, 10_000, 100_000, 250_000, 500_000, 1_000_000, 2_000_000};

    private Banker() {
    }

    /** CMaNGOS HandleBankerActivateOpcode / gossip GOSSIP_OPTION_BANKER → SMSG_SHOW_BANK (raw guid). inventory.md */
    public static void sendShowBank(Creature c, BiConsumer<Integer, byte[]> send) {
        if ((c.npcFlags & UNIT_NPC_FLAG_BANKER) == 0) {
            return;
        }
        WowBuffer shown = new WowBuffer(8);
        shown.putU64(c.guid);
        send.accept(Opcodes.SMSG_SHOW_BANK, shown.array());
    }
}
