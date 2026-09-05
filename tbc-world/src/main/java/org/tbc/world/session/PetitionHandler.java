package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Guild;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Charter buy / sign / turn-in. PetitionsHandler.cpp. Layout: spec/03-protocol/packets/guild.md */
public final class PetitionHandler {
    /** PetitionsHandler.cpp type 9 guild charter. */
    public static final int TYPE_GUILD = 9;
    /** Item.h BUY_ERR_CANT_FIND_ITEM. */
    public static final int BUY_ERR_CANT_FIND_ITEM = 0;
    /** Item.h BUY_ERR_NOT_ENOUGHT_MONEY. */
    public static final int BUY_ERR_NOT_ENOUGHT_MONEY = 2;
    /** Guild.h PETITION_SIGN_OK. */
    public static final int PETITION_SIGN_OK = 0;
    /** Guild.h PETITION_SIGN_ALREADY_SIGNED. */
    public static final int PETITION_SIGN_ALREADY_SIGNED = 1;
    /** Guild.h PETITION_TURN_OK. */
    public static final int PETITION_TURN_OK = 0;
    /** Guild.h PETITION_TURN_ALREADY_IN_GUILD. */
    public static final int PETITION_TURN_ALREADY_IN_GUILD = 2;
    /** Guild.h PETITION_TURN_NEED_MORE_SIGNATURES. */
    public static final int PETITION_TURN_NEED_MORE_SIGNATURES = 4;
    public static final int ERR_GUILD_NAME_EXISTS_S = 0x07;
    /** Guild.h GUILD_FOUNDER_S. */
    public static final int GUILD_FOUNDER_S = 0x0E;

    private PetitionHandler() {}

    public static void buy(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 20) {
            return;
        }
        long npcGuid = in.getU64();
        in.getU32();
        in.getU64();
        String name = in.remaining() > 0 ? in.getCString() : "";
        if (in.remaining() < 47) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            in.getU32();
        }
        in.getU16();
        in.getU8();
        in.getU32();
        if (in.remaining() >= 4) {
            in.getU32();
        }
        Player p = s.player();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), npcGuid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_PETITIONER) == 0) {
            return;
        }
        if ((npc.npcFlags & Content.UNIT_NPC_FLAG_TABARDDESIGNER) == 0) {
            return;
        }
        if (p.guildId != 0) {
            return;
        }
        if (guildByName(world, name) != null) {
            GuildHandler.commandResult(s, GuildHandler.GUILD_CREATE_S, name, ERR_GUILD_NAME_EXISTS_S);
            return;
        }
        if (world.objectMgr.items.get(Content.ITEM_GUILD_CHARTER) == null) {
            buyFailed(s, 0, Content.ITEM_GUILD_CHARTER, BUY_ERR_CANT_FIND_ITEM);
            return;
        }
        if (p.money < Content.GUILD_CHARTER_COST) {
            buyFailed(s, npc.guid, Content.ITEM_GUILD_CHARTER, BUY_ERR_NOT_ENOUGHT_MONEY);
            return;
        }
        int slot = p.firstFreeBagSlot();
        if (slot < 0) {
            return;
        }
        p.setMoney(p.money - Content.GUILD_CHARTER_COST);
        Item charter = new Item(world.nextItemGuid(), Content.ITEM_GUILD_CHARTER);
        charter.ownerGuid = Guid.low(p.guid);
        charter.bag = 0;
        charter.slot = slot;
        charter.count = 1;
        charter.displayId = Content.CHARTER_DISPLAY_ID;
        charter.enchant = Guid.low(charter.guid);
        p.items.put(Guid.low(charter.guid), charter);
        p.dirty = true;
        int low = Guid.low(charter.guid);
        world.objectMgr.petitions.values().removeIf(pet -> pet.ownerGuid == p.guid && pet.type == TYPE_GUILD);
        ObjectMgr.Petition pet = new ObjectMgr.Petition();
        pet.guidLow = low;
        pet.ownerGuid = p.guid;
        pet.ownerAccount = p.accountId;
        pet.name = name;
        pet.type = TYPE_GUILD;
        world.objectMgr.petitions.put(low, pet);
        s.send(Opcodes.SMSG_ITEM_PUSH_RESULT, encodeNpcPush(p, charter));
    }

    public static void sign(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long petitionGuid = in.getU64();
        if (in.remaining() >= 1) {
            in.getU8();
        }
        Player p = s.player();
        ObjectMgr.Petition pet = world.objectMgr.petitions.get(Guid.low(petitionGuid));
        if (pet == null) {
            return;
        }
        if (pet.ownerGuid == p.guid) {
            return;
        }
        Player owner = world.playerByGuid(pet.ownerGuid);
        if (owner != null && owner.team != p.team) {
            GuildHandler.commandResult(s, GuildHandler.GUILD_CREATE_S, "", GuildHandler.ERR_GUILD_NOT_ALLIED);
            return;
        }
        if (p.guildId != 0) {
            GuildHandler.commandResult(s, GuildHandler.GUILD_INVITE_S, p.name, GuildHandler.ERR_ALREADY_IN_GUILD_S);
            return;
        }
        if (p.guildIdInvited != 0) {
            GuildHandler.commandResult(s, GuildHandler.GUILD_INVITE_S, p.name, GuildHandler.ERR_ALREADY_INVITED_TO_GUILD_S);
            return;
        }
        if (pet.signers.size() + 1 > pet.type) {
            return;
        }
        if (pet.signerAccounts.contains(p.accountId)) {
            sendSignResults(s, world, pet, p, PETITION_SIGN_ALREADY_SIGNED);
            return;
        }
        pet.signers.add(p.guid);
        pet.signerAccounts.add(p.accountId);
        sendSignResults(s, world, pet, p, PETITION_SIGN_OK);
    }

    /**
     * @return true if a guild/arena charter was found and handled.
     * Missing petition leaves CMSG_TURN_IN_PETITION to the slice-23 arena stub.
     */
    public static boolean turnIn(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return false;
        }
        long petitionGuid = in.getU64();
        ObjectMgr.Petition pet = world.objectMgr.petitions.get(Guid.low(petitionGuid));
        if (pet == null) {
            return false;
        }
        Player p = s.player();
        if (pet.type == TYPE_GUILD && p.guildId != 0) {
            sendTurnIn(s, PETITION_TURN_ALREADY_IN_GUILD);
            return true;
        }
        if (pet.ownerGuid != p.guid) {
            return true;
        }
        int need = pet.type == TYPE_GUILD ? world.minPetitionSigns : pet.type - 1;
        if (pet.signers.size() < need) {
            sendTurnIn(s, PETITION_TURN_NEED_MORE_SIGNATURES);
            return true;
        }
        if (pet.type == TYPE_GUILD && guildByName(world, pet.name) != null) {
            GuildHandler.commandResult(s, GuildHandler.GUILD_CREATE_S, pet.name, ERR_GUILD_NAME_EXISTS_S);
            return true;
        }
        Item item = null;
        for (Item it : p.items.values()) {
            if (Guid.low(it.guid) == pet.guidLow) {
                item = it;
                break;
            }
        }
        if (item == null) {
            return true;
        }
        p.items.remove(Guid.low(item.guid));
        if (pet.type == TYPE_GUILD) {
            Guild g = new Guild();
            g.id = world.objectMgr.nextGuildId.getAndIncrement();
            g.name = pet.name;
            g.leaderGuid = p.guid;
            g.members.add(p.guid);
            world.objectMgr.guilds.put(g.id, g);
            p.guildId = g.id;
            p.guildLeader = true;
            p.guildName = pet.name;
            p.guildRankRights = GuildHandler.GR_RIGHT_ALL;
            GuildHandler.commandResult(s, GuildHandler.GUILD_CREATE_S, pet.name, 0);
            for (long guid : pet.signers) {
                g.members.add(guid);
                Player signee = world.playerByGuid(guid);
                if (signee != null) {
                    signee.guildId = g.id;
                    signee.guildName = pet.name;
                    signee.guildLeader = false;
                    signee.guildRankRights = GuildHandler.GR_RIGHT_EMPTY;
                    if (signee.session != null) {
                        GuildHandler.commandResult(signee.session, GUILD_FOUNDER_S, pet.name, 0);
                    }
                }
            }
        }
        world.objectMgr.petitions.remove(pet.guidLow);
        sendTurnIn(s, PETITION_TURN_OK);
        return true;
    }

    static void sendTurnIn(WorldSession s, int result) {
        WowBuffer data = new WowBuffer(4);
        data.putU32(result);
        s.send(Opcodes.SMSG_TURN_IN_PETITION_RESULTS, data.array());
    }

    public static void showList(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long npcGuid = in.getU64();
        Player p = s.player();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), npcGuid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_PETITIONER) == 0) {
            return;
        }
        boolean tabard = (npc.npcFlags & Content.UNIT_NPC_FLAG_TABARDDESIGNER) != 0;
        WowBuffer data = new WowBuffer(128);
        data.putU64(npcGuid);
        if (tabard) {
            data.putU8(1);
            data.putU32(1);
            data.putU32(Content.ITEM_GUILD_CHARTER);
            data.putU32(Content.CHARTER_DISPLAY_ID);
            data.putU32(Content.GUILD_CHARTER_COST);
            data.putU32(0);
            data.putU32(9);
        } else {
            data.putU8(3);
            putArenaCharter(data, 1, 23560, 800000, 2);
            putArenaCharter(data, 2, 23561, 1200000, 3);
            putArenaCharter(data, 3, 23562, 2000000, 5);
        }
        s.send(Opcodes.SMSG_PETITION_SHOWLIST, data.array());
    }

    static void putArenaCharter(WowBuffer data, int index, int entry, int cost, int signs) {
        data.putU32(index);
        data.putU32(entry);
        data.putU32(Content.CHARTER_DISPLAY_ID);
        data.putU32(cost);
        data.putU32(signs);
        data.putU32(signs);
    }

    public static void showSignatures(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long petitionGuid = in.getU64();
        ObjectMgr.Petition pet = world.objectMgr.petitions.get(Guid.low(petitionGuid));
        if (pet == null) {
            return;
        }
        if (pet.type == TYPE_GUILD && s.player().guildId != 0) {
            return;
        }
        WowBuffer data = new WowBuffer(32 + pet.signers.size() * 12);
        data.putU64(Guid.player(pet.guidLow));
        data.putU64(pet.ownerGuid);
        data.putU32(pet.guidLow);
        data.putU8(pet.signers.size());
        for (long signer : pet.signers) {
            data.putU64(signer);
            data.putU32(0);
        }
        s.send(Opcodes.SMSG_PETITION_SHOW_SIGNATURES, data.array());
    }

    public static void decline(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long petitionGuid = in.getU64();
        ObjectMgr.Petition pet = world.objectMgr.petitions.get(Guid.low(petitionGuid));
        if (pet == null) {
            return;
        }
        Player owner = world.playerByGuid(pet.ownerGuid);
        if (owner != null && owner.session != null) {
            WowBuffer data = new WowBuffer(8);
            data.putU64(s.player().guid);
            owner.session.send(Opcodes.MSG_PETITION_DECLINE, data.array());
        }
    }

    public static void rename(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long petitionGuid = in.getU64();
        String name = in.remaining() > 0 ? in.getCString() : "";
        Player p = s.player();
        Item item = null;
        for (Item it : p.items.values()) {
            if (it.guid == petitionGuid || Guid.low(it.guid) == Guid.low(petitionGuid)) {
                item = it;
                break;
            }
        }
        if (item == null) {
            return;
        }
        ObjectMgr.Petition pet = world.objectMgr.petitions.get(Guid.low(petitionGuid));
        if (pet == null) {
            return;
        }
        if (pet.type == TYPE_GUILD && guildByName(world, name) != null) {
            GuildHandler.commandResult(s, GuildHandler.GUILD_CREATE_S, name, ERR_GUILD_NAME_EXISTS_S);
            return;
        }
        pet.name = name;
        WowBuffer data = new WowBuffer(16 + name.length());
        data.putU64(petitionGuid);
        data.putCString(name);
        s.send(Opcodes.MSG_PETITION_RENAME, data.array());
    }

    public static void query(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 12) {
            return;
        }
        in.getU32();
        long petitionGuid = in.getU64();
        ObjectMgr.Petition pet = world.objectMgr.petitions.get(Guid.low(petitionGuid));
        if (pet == null) {
            return;
        }
        WowBuffer data = new WowBuffer(80 + pet.name.length());
        data.putU32(pet.guidLow);
        data.putU64(pet.ownerGuid);
        data.putCString(pet.name);
        data.putU8(0);
        if (pet.type == TYPE_GUILD) {
            data.putU32(9);
            data.putU32(9);
            data.putU32(0);
        } else {
            data.putU32(pet.type - 1);
            data.putU32(pet.type - 1);
            data.putU32(pet.type);
        }
        data.putU32(0);
        data.putU32(0);
        data.putU32(0);
        data.putU32(0);
        data.putU16(0);
        data.putU32(0);
        data.putU32(0);
        data.putU32(0);
        data.putU32(0);
        data.putU32(pet.type == TYPE_GUILD ? 0 : 1);
        s.send(Opcodes.SMSG_PETITION_QUERY_RESPONSE, data.array());
    }

    static void sendSignResults(WorldSession s, World world, ObjectMgr.Petition pet, Player signer, int result) {
        WowBuffer data = new WowBuffer(24);
        data.putU64(Guid.player(pet.guidLow));
        data.putU64(signer.guid);
        data.putU32(result);
        byte[] payload = data.array();
        s.send(Opcodes.SMSG_PETITION_SIGN_RESULTS, payload);
        Player owner = world.playerByGuid(pet.ownerGuid);
        if (owner != null && owner.session != null && owner.session != s) {
            owner.session.send(Opcodes.SMSG_PETITION_SIGN_RESULTS, payload);
        }
    }

    static Guild guildByName(World world, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (Guild g : world.objectMgr.guilds.values()) {
            if (name.equals(g.name)) {
                return g;
            }
        }
        return null;
    }

    static void buyFailed(WorldSession s, long vendor, int item, int result) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(vendor);
        b.putU32(item);
        b.putU8(result);
        s.send(Opcodes.SMSG_BUY_FAILED, b.array());
    }

    static byte[] encodeNpcPush(Player p, Item it) {
        WowBuffer b = new WowBuffer(48);
        b.putU64(p.guid);
        b.putU32(1);
        b.putU32(0);
        b.putU32(1);
        b.putU8(it.bag);
        b.putU32(it.slot);
        b.putU32(it.entry);
        b.putU32(0);
        b.putU32(0);
        b.putU32(it.count);
        b.putU32(it.count);
        return b.array();
    }
}
