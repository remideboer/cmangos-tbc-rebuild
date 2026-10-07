package org.tbc.world.session;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Characterization of the logged-in dispatch table (refactoring plan cycle 1.1+). */
class OpcodeTableTest {
    private static final class RecordingSink implements PacketSink {
        final List<Integer> sent = new ArrayList<>();

        @Override
        public void send(int opcode, byte[] payload) {
            sent.add(opcode);
        }

        @Override
        public void close() {
        }
    }

    /** Cycle 1.1 query family + cycle 1.2 former LaterOpcodes families. */
    static final Set<Integer> EXPECTED = Set.of(
            // QueryHandler
            Opcodes.CMSG_CREATURE_QUERY, Opcodes.CMSG_GAMEOBJECT_QUERY, Opcodes.CMSG_ITEM_QUERY_SINGLE,
            Opcodes.CMSG_QUEST_QUERY, Opcodes.CMSG_PAGE_TEXT_QUERY, Opcodes.CMSG_ITEM_TEXT_QUERY,
            Opcodes.CMSG_NPC_TEXT_QUERY, Opcodes.CMSG_PET_NAME_QUERY, Opcodes.CMSG_WHOIS,
            // SocialHandler (contacts, group, trade, who, mail)
            Opcodes.CMSG_CONTACT_LIST, Opcodes.CMSG_GROUP_INVITE, Opcodes.CMSG_GROUP_ACCEPT,
            Opcodes.CMSG_GROUP_DECLINE, Opcodes.CMSG_GROUP_UNINVITE, Opcodes.CMSG_GROUP_UNINVITE_GUID,
            Opcodes.CMSG_GROUP_SET_LEADER, Opcodes.CMSG_GROUP_DISBAND, Opcodes.CMSG_REQUEST_PARTY_MEMBER_STATS,
            Opcodes.CMSG_INITIATE_TRADE, Opcodes.CMSG_BEGIN_TRADE, Opcodes.CMSG_SET_TRADE_ITEM,
            Opcodes.CMSG_SET_TRADE_GOLD, Opcodes.CMSG_ACCEPT_TRADE, Opcodes.CMSG_CANCEL_TRADE,
            Opcodes.CMSG_WHO, Opcodes.CMSG_ADD_FRIEND, Opcodes.CMSG_SET_CONTACT_NOTES, Opcodes.CMSG_ADD_IGNORE,
            Opcodes.CMSG_DEL_IGNORE, Opcodes.CMSG_DEL_FRIEND,
            Opcodes.CMSG_SEND_MAIL, Opcodes.CMSG_GET_MAIL_LIST, Opcodes.CMSG_MAIL_TAKE_ITEM,
            Opcodes.CMSG_MAIL_TAKE_MONEY, Opcodes.CMSG_MAIL_MARK_AS_READ, Opcodes.CMSG_MAIL_RETURN_TO_SENDER,
            Opcodes.CMSG_MAIL_CREATE_TEXT_ITEM, Opcodes.MSG_QUERY_NEXT_MAIL_TIME, Opcodes.CMSG_MAIL_DELETE,
            // InventoryHandler
            Opcodes.CMSG_SWAP_INV_ITEM, Opcodes.CMSG_DESTROYITEM, Opcodes.CMSG_SPLIT_ITEM,
            Opcodes.CMSG_BANKER_ACTIVATE, Opcodes.CMSG_BUY_BANK_SLOT, Opcodes.CMSG_AUTOBANK_ITEM,
            Opcodes.CMSG_AUTOSTORE_BANK_ITEM, Opcodes.CMSG_AUTOEQUIP_ITEM, Opcodes.CMSG_AUTOSTORE_BAG_ITEM,
            Opcodes.CMSG_SET_AMMO, Opcodes.CMSG_READ_ITEM, Opcodes.CMSG_WRAP_ITEM,
            Opcodes.CMSG_CANCEL_TEMP_ENCHANTMENT, Opcodes.CMSG_SWAP_ITEM, Opcodes.CMSG_SELL_ITEM,
            Opcodes.CMSG_BUYBACK_ITEM, Opcodes.CMSG_REPAIR_ITEM, Opcodes.CMSG_SOCKET_GEMS, Opcodes.CMSG_USE_ITEM,
            // Binder / Trainer / Channel / Auction / Talent
            Opcodes.CMSG_BINDER_ACTIVATE, Opcodes.CMSG_TRAINER_BUY_SPELL,
            Opcodes.CMSG_TEXT_EMOTE, Opcodes.CMSG_CHANNEL_LIST,
            Opcodes.CMSG_AUCTION_LIST_ITEMS, Opcodes.MSG_TALENT_WIPE_CONFIRM,
            // LootHandler
            Opcodes.CMSG_LOOT_METHOD, Opcodes.CMSG_LOOT_ROLL, Opcodes.CMSG_LOOT_MASTER_GIVE,
            // GroupHandler
            Opcodes.CMSG_GROUP_RAID_CONVERT, Opcodes.CMSG_GROUP_ASSISTANT_LEADER,
            Opcodes.CMSG_GROUP_CHANGE_SUB_GROUP, Opcodes.CMSG_GROUP_SWAP_SUB_GROUP,
            Opcodes.CMSG_REQUEST_RAID_INFO, Opcodes.MSG_RAID_READY_CHECK, Opcodes.MSG_RAID_TARGET_UPDATE,
            Opcodes.MSG_RANDOM_ROLL, Opcodes.MSG_MINIMAP_PING,
            // GuildHandler (bank)
            Opcodes.CMSG_GUILD_CREATE, Opcodes.CMSG_GUILD_BANKER_ACTIVATE, Opcodes.CMSG_GUILD_BANK_QUERY_TAB,
            Opcodes.CMSG_GUILD_BANK_UPDATE_TAB, Opcodes.CMSG_GUILD_BANK_DEPOSIT_MONEY,
            Opcodes.CMSG_GUILD_BANK_WITHDRAW_MONEY, Opcodes.CMSG_GUILD_BANK_SWAP_ITEMS,
            Opcodes.CMSG_GUILD_BANK_BUY_TAB, Opcodes.MSG_GUILD_BANK_LOG_QUERY,
            Opcodes.MSG_QUERY_GUILD_BANK_TEXT, Opcodes.CMSG_SET_GUILD_BANK_TEXT,
            // LfgHandler
            Opcodes.CMSG_SET_LOOKING_FOR_GROUP, Opcodes.MSG_LOOKING_FOR_GROUP, Opcodes.CMSG_LFG_SET_AUTOJOIN,
            Opcodes.CMSG_LFG_CLEAR_AUTOJOIN, Opcodes.CMSG_SET_LFG_COMMENT, Opcodes.CMSG_CLEAR_LOOKING_FOR_GROUP,
            Opcodes.CMSG_CLEAR_LOOKING_FOR_MORE, Opcodes.CMSG_SET_LOOKING_FOR_MORE, Opcodes.CMSG_ACCEPT_LFG_MATCH,
            // PetHandler
            Opcodes.CMSG_PET_ACTION, Opcodes.CMSG_PET_SET_ACTION, Opcodes.CMSG_PET_SPELL_AUTOCAST,
            Opcodes.CMSG_PET_CAST_SPELL, Opcodes.CMSG_PET_STOP_ATTACK, Opcodes.CMSG_PET_CANCEL_AURA,
            Opcodes.CMSG_REQUEST_PET_INFO, Opcodes.CMSG_PET_RENAME, Opcodes.CMSG_PET_ABANDON,
            Opcodes.CMSG_TOTEM_DESTROYED, Opcodes.CMSG_STABLE_PET, Opcodes.CMSG_UNSTABLE_PET,
            Opcodes.CMSG_BUY_STABLE_SLOT,
            // InstanceHandler
            Opcodes.CMSG_RESET_INSTANCES, Opcodes.MSG_SET_DUNGEON_DIFFICULTY,
            // BattlegroundHandler
            Opcodes.CMSG_LEAVE_BATTLEFIELD, Opcodes.CMSG_BATTLEFIELD_PORT, Opcodes.CMSG_REPORT_PVP_AFK,
            Opcodes.MSG_INSPECT_HONOR_STATS,
            // GmTicketHandler
            Opcodes.CMSG_GMTICKET_GETTICKET, Opcodes.CMSG_GMTICKET_UPDATETEXT,
            Opcodes.CMSG_GMTICKET_DELETETICKET, Opcodes.CMSG_GMTICKET_SYSTEMSTATUS,
            // SpellCancelHandler
            Opcodes.CMSG_CANCEL_CAST, Opcodes.CMSG_CANCEL_AUTO_REPEAT_SPELL, Opcodes.CMSG_CANCEL_CHANNELLING,
            Opcodes.CMSG_CANCEL_AURA,
            // MountHandler / QuestShareHandler
            Opcodes.CMSG_MOUNTSPECIAL_ANIM, Opcodes.CMSG_CANCEL_MOUNT_AURA, Opcodes.CMSG_PUSHQUESTTOPARTY);

    @Test
    void loggedInWhenBuiltShouldRegisterExactlyTheMovedFamilies() {
        assertEquals(EXPECTED, OpcodeTable.loggedIn().opcodes());
    }

    @Test
    void dispatchWhenOpcodeUnknownShouldReturnFalseAndSendNothing() {
        RecordingSink sink = new RecordingSink();
        WorldSession session = new WorldSession(sink, 0);
        boolean handled = new OpcodeTable().dispatch(session, null, Opcodes.CMSG_WHOIS, new WowBuffer(new byte[0]));
        assertFalse(handled);
        assertEquals(List.of(), sink.sent);
    }

    @Test
    void dispatchWhenOpcodeRegisteredShouldInvokeOperationOnce() {
        List<Integer> calls = new ArrayList<>();
        OpcodeTable t = new OpcodeTable().register(Opcodes.CMSG_WHOIS, (s, w, in) -> calls.add(in.remaining()));
        boolean handled = t.dispatch(new WorldSession(new RecordingSink(), 0), null, Opcodes.CMSG_WHOIS,
                new WowBuffer(new byte[3]));
        assertTrue(handled);
        assertEquals(List.of(3), calls);
    }

    @Test
    void registerWhenOpcodeAlreadyRegisteredShouldThrow() {
        OpcodeTable t = new OpcodeTable().register(Opcodes.CMSG_WHOIS, (s, w, in) -> { });
        assertThrows(IllegalStateException.class, () -> t.register(Opcodes.CMSG_WHOIS, (s, w, in) -> { }));
    }
}
