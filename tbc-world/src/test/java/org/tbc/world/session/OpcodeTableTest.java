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
            Opcodes.CMSG_OPEN_ITEM,
            // Binder / Trainer / Talent
            Opcodes.CMSG_BINDER_ACTIVATE, Opcodes.CMSG_TRAINER_BUY_SPELL,
            Opcodes.MSG_TALENT_WIPE_CONFIRM, Opcodes.CMSG_LEARN_TALENT,
            // ChannelHandler
            Opcodes.CMSG_TEXT_EMOTE, Opcodes.CMSG_CHANNEL_LIST, Opcodes.CMSG_JOIN_CHANNEL, Opcodes.CMSG_LEAVE_CHANNEL,
            Opcodes.CMSG_CHANNEL_PASSWORD, Opcodes.CMSG_CHANNEL_OWNER, Opcodes.CMSG_CHANNEL_SET_OWNER,
            Opcodes.CMSG_CHANNEL_MODERATOR, Opcodes.CMSG_CHANNEL_UNMODERATOR, Opcodes.CMSG_CHANNEL_MUTE,
            Opcodes.CMSG_CHANNEL_UNMUTE, Opcodes.CMSG_CHANNEL_INVITE, Opcodes.CMSG_CHANNEL_KICK,
            Opcodes.CMSG_CHANNEL_BAN, Opcodes.CMSG_CHANNEL_UNBAN, Opcodes.CMSG_CHANNEL_ANNOUNCEMENTS,
            Opcodes.CMSG_CHANNEL_MODERATE,
            // AuctionHandler
            Opcodes.CMSG_AUCTION_LIST_ITEMS, Opcodes.CMSG_AUCTION_SELL_ITEM, Opcodes.CMSG_AUCTION_PLACE_BID,
            Opcodes.CMSG_AUCTION_LIST_OWNER_ITEMS, Opcodes.CMSG_AUCTION_LIST_BIDDER_ITEMS,
            Opcodes.CMSG_AUCTION_REMOVE_ITEM,
            // DeathHandler
            Opcodes.CMSG_REPOP_REQUEST, Opcodes.MSG_CORPSE_QUERY, Opcodes.CMSG_RECLAIM_CORPSE, Opcodes.CMSG_SELF_RES,
            Opcodes.CMSG_RESURRECT_RESPONSE, Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE,
            Opcodes.CMSG_AREA_SPIRIT_HEALER_QUEUE, Opcodes.CMSG_AREA_SPIRIT_HEALER_QUERY,
            // TaxiHandler
            Opcodes.CMSG_TAXINODE_STATUS_QUERY, Opcodes.CMSG_TAXIQUERYAVAILABLENODES, Opcodes.CMSG_ACTIVATETAXI,
            Opcodes.CMSG_ACTIVATETAXIEXPRESS,
            // LootHandler
            Opcodes.CMSG_LOOT_METHOD, Opcodes.CMSG_LOOT_ROLL, Opcodes.CMSG_LOOT_MASTER_GIVE,
            Opcodes.CMSG_AUTOSTORE_LOOT_ITEM, Opcodes.CMSG_LOOT_MONEY,
            // GroupHandler
            Opcodes.CMSG_GROUP_RAID_CONVERT, Opcodes.CMSG_GROUP_ASSISTANT_LEADER,
            Opcodes.CMSG_GROUP_CHANGE_SUB_GROUP, Opcodes.CMSG_GROUP_SWAP_SUB_GROUP,
            Opcodes.CMSG_REQUEST_RAID_INFO, Opcodes.MSG_RAID_READY_CHECK, Opcodes.MSG_RAID_TARGET_UPDATE,
            Opcodes.MSG_RANDOM_ROLL, Opcodes.MSG_MINIMAP_PING,
            // GuildHandler
            Opcodes.CMSG_GUILD_INVITE, Opcodes.CMSG_GUILD_ACCEPT, Opcodes.CMSG_GUILD_DECLINE, Opcodes.CMSG_GUILD_INFO,
            Opcodes.CMSG_GUILD_ROSTER, Opcodes.CMSG_GUILD_PROMOTE, Opcodes.CMSG_GUILD_DEMOTE, Opcodes.CMSG_GUILD_LEAVE,
            Opcodes.CMSG_GUILD_REMOVE, Opcodes.CMSG_GUILD_DISBAND, Opcodes.CMSG_GUILD_LEADER,
            Opcodes.CMSG_GUILD_SET_PUBLIC_NOTE, Opcodes.CMSG_GUILD_SET_OFFICER_NOTE, Opcodes.CMSG_GUILD_INFO_TEXT,
            Opcodes.CMSG_GUILD_RANK, Opcodes.CMSG_GUILD_ADD_RANK, Opcodes.CMSG_GUILD_DEL_RANK, Opcodes.CMSG_GUILD_MOTD,
            Opcodes.MSG_SAVE_GUILD_EMBLEM,
            // PetitionHandler
            Opcodes.CMSG_PETITION_BUY, Opcodes.CMSG_PETITION_SHOWLIST, Opcodes.CMSG_PETITION_SHOW_SIGNATURES,
            Opcodes.MSG_PETITION_DECLINE, Opcodes.MSG_PETITION_RENAME, Opcodes.CMSG_PETITION_QUERY,
            Opcodes.CMSG_PETITION_SIGN, Opcodes.CMSG_TURN_IN_PETITION,
            // ArenaTeamHandler
            Opcodes.MSG_INSPECT_ARENA_TEAMS, Opcodes.CMSG_ARENA_TEAM_INVITE, Opcodes.CMSG_ARENA_TEAM_ACCEPT,
            Opcodes.CMSG_ARENA_TEAM_LEAVE, Opcodes.CMSG_ARENA_TEAM_REMOVE, Opcodes.CMSG_ARENA_TEAM_DISBAND,
            Opcodes.CMSG_ARENA_TEAM_LEADER,
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
            Opcodes.CMSG_BATTLEMASTER_JOIN, Opcodes.CMSG_BATTLEMASTER_JOIN_ARENA, Opcodes.CMSG_BATTLEFIELD_STATUS,
            Opcodes.CMSG_LEAVE_BATTLEFIELD, Opcodes.CMSG_BATTLEFIELD_PORT, Opcodes.CMSG_REPORT_PVP_AFK,
            Opcodes.MSG_INSPECT_HONOR_STATS,
            // GmTicketHandler
            Opcodes.CMSG_GMTICKET_CREATE, Opcodes.CMSG_GMTICKET_GETTICKET, Opcodes.CMSG_GMTICKET_UPDATETEXT,
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
