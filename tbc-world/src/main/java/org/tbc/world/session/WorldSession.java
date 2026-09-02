package org.tbc.world.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.Codes;
import org.tbc.common.Srp6;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.ChrStatic;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Group;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.AddonInfo;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class WorldSession {
    private static final Logger log = LoggerFactory.getLogger(WorldSession.class);
    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    public static final int STATUS_NEVER = 0;
    public static final int STATUS_AUTHED = 1;
    public static final int STATUS_LOGGEDIN = 2;
    public static final float MELEE_RANGE = 5f;
    public static final int MAX_SHEATH_STATE = 3;

    private final long id = NEXT_ID.getAndIncrement();
    private final PacketSink sink;
    private final int serverSeed;
    private int status = STATUS_NEVER;
    private World.Account account;
    private Player player;
    private boolean authedOnce;
    private int overspeedPings;
    private long lastPingMs;
    private final Queue<Pkt> inbound = new ConcurrentLinkedQueue<>();
    private long logoutAt;
    private final List<Integer> sentOpcodes = new ArrayList<>();
    private final Set<Long> seen = new HashSet<>();
    private final AtomicInteger timeSync = new AtomicInteger();
    public Group pendingGroup;
    public final List<String> channels = new ArrayList<>();
    public String lastTicket = "";
    public int bgQueue;
    public int worldStates2476;
    public int worldStates2478;

    public WorldSession(PacketSink sink, int serverSeed) {
        this.sink = sink;
        this.serverSeed = serverSeed;
    }

    public long id() {
        return id;
    }

    public Player player() {
        return player;
    }

    public World.Account account() {
        return account;
    }

    public int status() {
        return status;
    }

    public List<Integer> sentOpcodes() {
        return sentOpcodes;
    }

    public int serverSeed() {
        return serverSeed;
    }

    public void sendChallenge() {
        WowBuffer b = new WowBuffer(4);
        b.putU32(serverSeed);
        send(Opcodes.SMSG_AUTH_CHALLENGE, b.array());
    }

    public void send(int opcode, byte[] payload) {
        sentOpcodes.add(opcode);
        sink.send(opcode, payload == null ? new byte[0] : payload);
    }

    public void queue(int opcode, byte[] payload) {
        inbound.add(new Pkt(opcode, payload));
    }

    public void processQueue(World world) {
        Pkt p;
        while ((p = inbound.poll()) != null) {
            handle(world, p.opcode, p.payload);
        }
    }

    public void tick(World world, int diff) {
        if (player == null) {
            return;
        }
        if (logoutAt > 0 && world.nowMs() >= logoutAt) {
            logout(world, true);
        }
        if (player.online && player.firstSaveAtMs > 0 && world.nowMs() >= player.firstSaveAtMs) {
            world.characters.save(player);
            player.firstSaveAtMs = world.nowMs() + world.saveIntervalMs;
        }
        if (player.nextTimeSyncMs > 0 && world.nowMs() >= player.nextTimeSyncMs) {
            player.timeSyncCounter++;
            WowBuffer b = new WowBuffer(4);
            b.putU32(player.timeSyncCounter);
            send(Opcodes.SMSG_TIME_SYNC_REQ, b.array());
            player.nextTimeSyncMs = world.nowMs() + 10_000;
        }
        if (player.inCombat && player.lastMeleeMs + 2000 <= world.nowMs()) {
            GameMap map = world.map(player.mapId, player.instanceId);
            for (Creature c : map.nearbyCreatures(player, MELEE_RANGE)) {
                if (c.victim == player.guid || player.victim == c.guid) {
                    world.meleeHit(player, c);
                    player.lastMeleeMs = world.nowMs();
                    break;
                }
            }
        }
    }

    public void handle(World world, int opcode, byte[] payload) {
        if (opcode == Opcodes.CMSG_WARDEN_DATA) {
            return;
        }
        if (!Opcodes.valid(opcode)) {
            log.warn("bad opcode {}", opcode);
            sink.close();
            return;
        }
        WowBuffer in = new WowBuffer(payload == null ? new byte[0] : payload);
        if (opcode == Opcodes.CMSG_PING) {
            handlePing(world, in);
            return;
        }
        if (opcode == Opcodes.CMSG_AUTH_SESSION) {
            handleAuthSession(world, in);
            return;
        }
        if (status < STATUS_AUTHED) {
            return;
        }
        if (opcode == Opcodes.CMSG_CHAR_ENUM) {
            handleCharEnum(world);
            return;
        }
        if (opcode == Opcodes.CMSG_CHAR_CREATE) {
            handleCharCreate(world, in);
            return;
        }
        if (opcode == Opcodes.CMSG_CHAR_DELETE) {
            handleCharDelete(world, in);
            return;
        }
        if (opcode == Opcodes.CMSG_PLAYER_LOGIN) {
            handleLogin(world, in);
            return;
        }
        if (opcode == Opcodes.CMSG_GUILD_QUERY) {
            QueryHandler.guild(this, in);
            return;
        }
        if (status < STATUS_LOGGEDIN) {
            return;
        }
        if (opcode == Opcodes.MSG_MOVE_WORLDPORT_ACK) {
            handleWorldportAck(world);
            return;
        }
        if (opcode >= Opcodes.MSG_MOVE_START_FORWARD && opcode <= Opcodes.MSG_MOVE_HEARTBEAT) {
            handleMove(world, opcode, in, false);
            return;
        }
        if (opcode == Opcodes.CMSG_FORCE_MOVE_ROOT_ACK || opcode == Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK) {
            handleMove(world, opcode, in, true);
            return;
        }
        switch (opcode) {
            case Opcodes.CMSG_LOGOUT_REQUEST -> handleLogoutRequest(world);
            case Opcodes.CMSG_LOGOUT_CANCEL -> handleLogoutCancel();
            case Opcodes.CMSG_PLAYER_LOGOUT -> { }
            case Opcodes.CMSG_MESSAGECHAT -> handleChat(world, in);
            case Opcodes.CMSG_NAME_QUERY -> handleNameQuery(world, in);
            case Opcodes.CMSG_QUERY_TIME -> handleQueryTime(world);
            case Opcodes.CMSG_CREATURE_QUERY -> QueryHandler.creature(this, world, in);
            case Opcodes.CMSG_GAMEOBJECT_QUERY -> QueryHandler.gameObject(this, world, in);
            case Opcodes.CMSG_ITEM_QUERY_SINGLE -> QueryHandler.item(this, world, in);
            case Opcodes.CMSG_QUEST_QUERY -> QueryHandler.quest(this, world, in);
            case Opcodes.CMSG_PAGE_TEXT_QUERY -> QueryHandler.pageText(this, world, in);
            case Opcodes.CMSG_PET_NAME_QUERY -> QueryHandler.petName(this, in);
            case Opcodes.CMSG_WHOIS -> QueryHandler.whois(this, world, in);
            case Opcodes.CMSG_TIME_SYNC_RESP -> in.skip(Math.min(8, in.remaining()));
            case Opcodes.CMSG_SET_ACTIVE_MOVER -> in.getU64();
            case Opcodes.CMSG_ZONEUPDATE -> player.zoneClient = in.getU32();
            case Opcodes.CMSG_CONTACT_LIST -> send(Opcodes.SMSG_CONTACT_LIST, u32(0, 0));
            case Opcodes.CMSG_SET_ACTION_BUTTON -> {
                int button = in.getU8();
                int packed = in.getU32();
                if (button < 132) {
                    player.actionButtons[button] = packed;
                }
            }
            case Opcodes.CMSG_TUTORIAL_FLAG -> {
                int bit = in.getU32();
                if (bit / 32 < 8) {
                    player.tut[bit / 32] |= 1 << (bit % 32);
                }
            }
            case Opcodes.CMSG_NEXT_CINEMATIC_CAMERA, Opcodes.CMSG_COMPLETE_CINEMATIC -> {
            }
            case Opcodes.CMSG_SET_SELECTION -> player.selection = in.remaining() >= 8 ? in.getU64() : 0;
            case Opcodes.CMSG_ATTACKSWING -> handleAttack(world, in);
            case Opcodes.CMSG_ATTACKSTOP -> handleAttackStop(world);
            case Opcodes.CMSG_SETSHEATHED -> handleSheath(in);
            case Opcodes.CMSG_LOOT -> handleLoot(world, in);
            case Opcodes.CMSG_LOOT_MONEY -> {
            }
            case Opcodes.CMSG_LOOT_RELEASE -> handleLootRelease(world, in);
            case Opcodes.CMSG_CAST_SPELL -> handleCast(world, in);
            case Opcodes.CMSG_GOSSIP_HELLO, Opcodes.CMSG_QUESTGIVER_HELLO -> handleGossip(world, in);
            case Opcodes.CMSG_LIST_INVENTORY -> handleListInventory(world, in);
            case Opcodes.CMSG_QUESTGIVER_QUERY_QUEST -> handleQuestQuery(world, in);
            case Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST -> handleQuestAccept(world, in);
            case Opcodes.CMSG_QUESTGIVER_COMPLETE_QUEST, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD ->
                    handleQuestComplete(world, in);
            case Opcodes.CMSG_GROUP_INVITE -> handleGroupInvite(world, in);
            case Opcodes.CMSG_GROUP_ACCEPT -> handleGroupAccept(world);
            case Opcodes.CMSG_GROUP_DISBAND -> handleGroupDisband(world);
            case Opcodes.CMSG_INITIATE_TRADE -> handleTrade(world, in);
            case Opcodes.CMSG_WHO -> handleWho(world, in);
            case Opcodes.CMSG_ADD_FRIEND -> handleAddFriend(world, in);
            case Opcodes.CMSG_SEND_MAIL -> handleMail(world, in);
            case Opcodes.MSG_AUCTION_HELLO -> handleAuctionHello(world, in);
            case Opcodes.CMSG_BATTLEMASTER_JOIN -> handleBgJoin(world, 489);
            case Opcodes.CMSG_BATTLEMASTER_JOIN_ARENA -> handleBgJoin(world, 562);
            case Opcodes.CMSG_REPOP_REQUEST -> handleRepop(world);
            case Opcodes.CMSG_RECLAIM_CORPSE -> handleReclaim(world, in);
            case Opcodes.CMSG_SELF_RES -> {
                player.setHealth(player.maxHealth());
                player.ghost = false;
            }
            case Opcodes.CMSG_PET_ACTION -> {
            }
            case Opcodes.CMSG_JOIN_CHANNEL -> handleJoinChannel(in);
            case Opcodes.CMSG_BUY_ITEM -> handleBuy(world, in);
            case Opcodes.CMSG_BUY_ITEM_IN_SLOT -> handleBuyInSlot(world, in);
            case Opcodes.CMSG_LEARN_TALENT -> handleTalent(in);
            case Opcodes.CMSG_TRAINER_LIST -> handleTrainer(world, in);
            case Opcodes.CMSG_ACTIVATETAXI, Opcodes.CMSG_ACTIVATETAXIEXPRESS -> handleTaxi(world);
            case Opcodes.CMSG_GAMEOBJ_USE -> handleGoUse(world, in);
            case Opcodes.CMSG_GMTICKET_CREATE -> handleTicket(in);
            case Opcodes.CMSG_INSPECT -> handleInspect(in);
            case Opcodes.CMSG_DUEL_ACCEPTED -> handleDuel(world);
            case Opcodes.CMSG_TOGGLE_PVP -> player.pvpFlagged = !player.pvpFlagged;
            case Opcodes.CMSG_OPEN_ITEM -> {
            }
            case Opcodes.CMSG_AREA_SPIRIT_HEALER_QUERY, Opcodes.CMSG_AREA_SPIRIT_HEALER_QUEUE -> {
            }
            case Opcodes.MSG_PVP_LOG_DATA -> send(Opcodes.MSG_PVP_LOG_DATA, u32(0));
            default -> handleRest(world, opcode, in);
        }
    }

    private void handleRest(World world, int opcode, WowBuffer in) {
        if (opcode == Opcodes.CMSG_VOICE_SESSION_ENABLE
                || opcode == Opcodes.CMSG_SWAP_INV_ITEM
                || opcode == Opcodes.CMSG_SWAP_ITEM
                || opcode == Opcodes.CMSG_DESTROYITEM) {
            return;
        }
        if (opcode == Opcodes.CMSG_AREATRIGGER) {
            int trigger = in.getU32();
            var at = world.objectMgr.areaTrigger(trigger);
            if (at != null) {
                int inst = 0;
                if (at.map() != 0 && at.map() != 1 && at.map() != 530) {
                    inst = player.group != null && player.group.bindMap == at.map() && player.group.instanceId != 0
                            ? player.group.instanceId : world.allocInstance();
                    if (player.group != null) {
                        player.group.bindMap = at.map();
                        player.group.instanceId = inst;
                    }
                    player.instanceId = inst;
                }
                world.teleport(player, at.map(), at.x(), at.y(), at.z(), at.o());
            }
        }
    }

    private void handleAuthSession(World world, WowBuffer in) {
        if (authedOnce) {
            sink.close();
            return;
        }
        authedOnce = true;
        int build = in.getU32();
        in.getU32();
        String user = in.getCString();
        int clientSeed = in.getU32();
        byte[] digest = in.getBytes(20);
        if (build != Srp6.BUILD_8606) {
            send(Opcodes.SMSG_AUTH_RESPONSE, new byte[]{(byte) Codes.AUTH_VERSION_MISMATCH});
            sink.close();
            return;
        }
        World.Account acc = world.lookupAccount(user);
        if (acc == null || acc.sessionKey() == null || !world.verifyDigest(acc, clientSeed, serverSeed, digest)) {
            log.warn("auth digest fail {}", user);
            send(Opcodes.SMSG_AUTH_RESPONSE, new byte[]{(byte) Codes.AUTH_UNKNOWN_ACCOUNT});
            sink.close();
            return;
        }
        if (!world.osAllowed(acc.os(), acc.platform())) {
            log.warn("auth os reject {} os={} platform={}", user, acc.os(), acc.platform());
            sink.close();
            return;
        }
        List<AddonInfo> addons;
        try {
            addons = AddonInfo.inflate(in);
        } catch (Exception e) {
            log.warn("auth addon inflate {} {}", user, e.toString());
            sink.close();
            return;
        }
        sink.initCrypt(acc.sessionKey());
        send(Opcodes.SMSG_ADDON_INFO, AddonInfo.buildSmsg(addons));
        this.account = acc;
        this.status = STATUS_AUTHED;
        WowBuffer ok = new WowBuffer(16);
        ok.putU8(Codes.AUTH_OK);
        ok.putU32(0);
        ok.putU8(0);
        ok.putU32(0);
        ok.putU8(Math.min(1, acc.expansion()));
        send(Opcodes.SMSG_AUTH_RESPONSE, ok.array());
        world.addSession(this);
    }

    private void handlePing(World world, WowBuffer in) {
        int ping = in.remaining() >= 4 ? in.getU32() : 0;
        if (in.remaining() >= 4) {
            in.getU32();
        }
        long now = world.nowMs();
        if (lastPingMs != 0 && now - lastPingMs < 20) {
            overspeedPings++;
            if (overspeedPings > world.maxOverspeedPings && world.maxOverspeedPings > 0) {
                sink.close();
                return;
            }
        } else {
            overspeedPings = 0;
        }
        lastPingMs = now;
        WowBuffer out = new WowBuffer(4);
        out.putU32(ping);
        send(Opcodes.SMSG_PONG, out.array());
    }

    private void handleCharEnum(World world) {
        List<Player> list = world.characters.enumAccount(account.id(), world.objectMgr);
        WowBuffer out = new WowBuffer(64);
        out.putU8(list.size());
        for (Player p : list) {
            out.putU64(p.guid);
            out.putCString(p.name);
            out.putU8(p.race);
            out.putU8(p.clazz);
            out.putU8(p.gender);
            out.putU8(p.skin);
            out.putU8(p.face);
            out.putU8(p.hairStyle);
            out.putU8(p.hairColor);
            out.putU8(p.facialHair);
            out.putU8(p.level);
            out.putU32(p.zoneId);
            out.putU32(p.mapId);
            out.putFloat(p.x);
            out.putFloat(p.y);
            out.putFloat(p.z);
            out.putU32(p.guildId);
            int flags = 0;
            if (p.ghost) {
                flags |= 0x2000;
            }
            if ((p.atLogin & Player.AT_LOGIN_FIRST) != 0) {
                flags |= 0;
            }
            out.putU32(flags);
            out.putU8((p.atLogin & Player.AT_LOGIN_FIRST) != 0 ? 1 : 0);
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
            for (int i = 0; i < 20; i++) {
                Item it = p.itemAt(0, i);
                if (it == null) {
                    out.putU32(0);
                    out.putU8(0);
                    out.putU32(0);
                } else {
                    out.putU32(it.displayId);
                    out.putU8(it.inventoryType);
                    out.putU32(it.enchant);
                }
            }
        }
        send(Opcodes.SMSG_CHAR_ENUM, out.array());
    }

    private void handleCharCreate(World world, WowBuffer in) {
        String name = in.getCString();
        int race = in.getU8();
        int clazz = in.getU8();
        int gender = in.getU8();
        int skin = in.getU8();
        int face = in.getU8();
        int hair = in.getU8();
        int hairColor = in.getU8();
        int facial = in.getU8();
        if (in.remaining() > 0) {
            in.getU8();
        }
        if (world.characters.nameInUse(name)) {
            send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_NAME_IN_USE});
            return;
        }
        var r = ChrStatic.race(race);
        if (r.expansion() > account.expansion()) {
            send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_EXPANSION});
            return;
        }
        Player p = world.characters.create(account.id(), name, race, clazz, gender, skin, face, hair, hairColor, facial, world.objectMgr);
        if (p == null) {
            send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_NAME_IN_USE});
            return;
        }
        send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_SUCCESS});
    }

    private void handleCharDelete(World world, WowBuffer in) {
        long guid = in.getU64();
        boolean ok = world.characters.delete(account.id(), guid);
        send(Opcodes.SMSG_CHAR_DELETE, new byte[]{(byte) (ok ? Codes.CHAR_DELETE_SUCCESS : Codes.CHAR_DELETE_FAILED_GUILD_LEADER)});
    }

    private void handleLogin(World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Player p = world.characters.load(account.id(), guid, world.objectMgr);
        if (p == null) {
            send(Opcodes.SMSG_CHARACTER_LOGIN_FAILED, new byte[]{0x05});
            return;
        }
        if (p.online && p.session != null && p.session != this) {
            send(Opcodes.SMSG_CHARACTER_LOGIN_FAILED, new byte[]{0x02});
            return;
        }
        this.player = p;
        p.session = this;
        p.gmLevel = account.gmlevel();
        p.applyCreateFields();
        status = STATUS_LOGGEDIN;
        LoginBurst.send(this, p, world);
        world.map(p.mapId, p.instanceId).add(p);
        seen.clear();
        seen.add(p.guid);
        for (Player o : world.map(p.mapId, p.instanceId).nearbyPlayers(p, GameMap.VISIBILITY)) {
            var self = UpdateBuilder.maybeCompress(UpdateBuilder.createUnit(p, false, (int) world.nowMs()));
            o.session.send(self.opcode(), self.payload());
            var other = UpdateBuilder.maybeCompress(UpdateBuilder.createUnit(o, false, (int) world.nowMs()));
            send(other.opcode(), other.payload());
            seen.add(o.guid);
        }
        revealNearby(world);
        world.characters.setOnline(p, true);
        p.online = true;
        p.firstSaveAtMs = world.nowMs() + Math.min(60_000, world.saveIntervalMs);
        p.nextTimeSyncMs = world.nowMs() + 5_000;
        WowBuffer weather = new WowBuffer(8);
        weather.putU32(0);
        weather.putFloat(0);
        weather.putU8(0);
        send(Opcodes.SMSG_WEATHER, weather.array());
    }

    private void handleMove(World world, int opcode, WowBuffer in, boolean ack) {
        if (ack) {
            in.getPackedGuid();
            in.getU32();
        }
        MovementInfo m = MovementInfo.readC2s(in);
        player.relocate(m.x, m.y, m.z, m.o);
        player.movement = m;
        m.stime = (int) world.nowMs();
        WowBuffer echo = new WowBuffer(64);
        m.write(echo, true, player.guid, m.stime);
        for (Player o : world.map(player.mapId, player.instanceId).nearbyPlayers(player, GameMap.VISIBILITY)) {
            o.session.send(opcode, echo.array());
        }
        revealNearby(world);
    }

    private void handleWorldportAck(World world) {
        if (player == null) {
            return;
        }
        seen.clear();
        seen.add(player.guid);
        LoginBurst.sendInventory(this, player);
        var self = UpdateBuilder.maybeCompress(UpdateBuilder.createUnit(player, true, (int) world.nowMs()));
        send(self.opcode(), self.payload());
        revealNearby(world);
    }

    public void forgetSeen() {
        seen.clear();
    }

    public void revealNearby(World world) {
        if (player == null) {
            return;
        }
        GameMap map = world.map(player.mapId, player.instanceId);
        int t = (int) world.nowMs();
        for (Creature c : map.nearbyCreatures(player, GameMap.VISIBILITY)) {
            if (!seen.add(c.guid)) {
                continue;
            }
            var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.createUnit(c, false, t));
            send(pkt.opcode(), pkt.payload());
        }
        for (Player o : map.nearbyPlayers(player, GameMap.VISIBILITY)) {
            if (!seen.add(o.guid)) {
                continue;
            }
            var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.createUnit(o, false, t));
            send(pkt.opcode(), pkt.payload());
        }
    }

    private void handleChat(World world, WowBuffer in) {
        int type = in.getU32();
        int lang = in.getU32();
        String target = "";
        if (type == 0x07 || type == 0x11) {
            target = in.getCString();
        }
        String msg = in.getCString();
        if (msg.startsWith(".") || msg.startsWith("!")) {
            String r = world.gm.handle(world, player, msg);
            system(r);
            return;
        }
        if (type == 0x01 || type == 0x06) {
            double range = type == 0x06 ? world.yellRange : world.sayRange;
            byte[] pkt = chatPacket(type, lang, player.guid, player.guid, msg, player.gmLevel > 0 ? 4 : 0);
            send(Opcodes.SMSG_MESSAGECHAT, pkt);
            for (Player o : world.map(player.mapId, player.instanceId).nearbyPlayers(player, range)) {
                o.session.send(Opcodes.SMSG_MESSAGECHAT, pkt);
            }
        } else if (type == 0x07) {
            Player to = world.playerByName(target);
            if (to != null && to.session != null) {
                to.session.send(Opcodes.SMSG_MESSAGECHAT, chatPacket(0x07, lang, player.guid, to.guid, msg, 0));
                send(Opcodes.SMSG_MESSAGECHAT, chatPacket(0x09, lang, player.guid, to.guid, msg, 0));
            }
        } else if (type == 0x02 && player.group != null) {
            byte[] pkt = chatPacket(type, lang, player.guid, player.guid, msg, 0);
            for (Player m : player.group.members) {
                m.session.send(Opcodes.SMSG_MESSAGECHAT, pkt);
            }
        }
    }

    private void handleNameQuery(World world, WowBuffer in) {
        long guid = in.getU64();
        Player p = player.guid == guid ? player : world.playerByName("");
        if (guid == player.guid) {
            p = player;
        } else {
            p = null;
            for (Player o : world.map(player.mapId, player.instanceId).players()) {
                if (o.guid == guid) {
                    p = o;
                    break;
                }
            }
        }
        WowBuffer out = new WowBuffer(64);
        out.putU64(guid);
        out.putCString(p == null ? "Unknown" : p.name);
        out.putU8(0);
        out.putU32(p == null ? 0 : p.race);
        out.putU32(p == null ? 0 : p.gender);
        out.putU32(p == null ? 0 : p.clazz);
        out.putU8(0);
        send(Opcodes.SMSG_NAME_QUERY_RESPONSE, out.array());
    }

    private void handleQueryTime(World world) {
        WowBuffer b = new WowBuffer(8);
        b.putU32((int) (System.currentTimeMillis() / 1000));
        b.putU32(0);
        send(Opcodes.SMSG_QUERY_TIME_RESPONSE, b.array());
    }

    private void handleLogoutRequest(World world) {
        boolean cant = player.inCombat || (player.movement.moveFlags & (MovementInfo.MOVEFLAG_FALLING) ) != 0;
        boolean inst = player.resting || account.gmlevel() >= world.instantLogout;
        WowBuffer b = new WowBuffer(5);
        b.putU32(cant ? 1 : 0);
        b.putU8(!cant && inst ? 1 : 0);
        send(Opcodes.SMSG_LOGOUT_RESPONSE, b.array());
        if (cant) {
            return;
        }
        if (inst) {
            logout(world, true);
        } else {
            logoutAt = world.nowMs() + 20_000;
        }
    }

    private void handleLogoutCancel() {
        logoutAt = 0;
        send(Opcodes.SMSG_LOGOUT_CANCEL_ACK, new byte[0]);
    }

    public void logout(World world, boolean save) {
        if (player == null) {
            return;
        }
        if (save) {
            world.characters.save(player);
        }
        world.map(player.mapId, player.instanceId).remove(player);
        world.characters.setOnline(player, false);
        send(Opcodes.SMSG_LOGOUT_COMPLETE, new byte[0]);
        status = STATUS_AUTHED;
        player.session = null;
        player = null;
        logoutAt = 0;
        seen.clear();
    }

    private void handleAttack(World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        GameMap map = world.map(player.mapId, player.instanceId);
        Creature c = map.creatures.get(guid);
        if (c == null || !c.alive()) {
            send(Opcodes.SMSG_ATTACKSWING_DEADTARGET, new byte[0]);
            return;
        }
        world.combat.startAttack(player, c, world.nowMs());
        if (c.eventAi != null) {
            c.eventAi.onAggro(c, player, (cr, t, spell) -> {
            });
        }
        if (c.script != null) {
            c.script.aggro();
        }
        send(Opcodes.SMSG_ATTACKSTART, world.combat.encodeAttackStart(player.guid, guid));
        if (player.distance2d(c) > MELEE_RANGE) {
            send(Opcodes.SMSG_ATTACKSWING_NOTINRANGE, new byte[0]);
            return;
        }
        world.meleeHit(player, c);
    }

    private void handleSheath(WowBuffer in) {
        if (in.remaining() < 4) {
            return;
        }
        int sheath = in.getU32();
        if (sheath >= MAX_SHEATH_STATE) {
            return;
        }
        int bytes2 = player.getInt(UpdateFields.UNIT_FIELD_BYTES_2);
        player.setInt(UpdateFields.UNIT_FIELD_BYTES_2, (bytes2 & ~0xFF) | (sheath & 0xFF));
    }

    private void handleAttackStop(World world) {
        long victim = player.victim;
        boolean dead = !player.alive();
        world.combat.stopAttack(player);
        send(Opcodes.SMSG_ATTACKSTOP, world.combat.encodeAttackStop(player.guid, victim, dead));
    }

    private void handleLoot(World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = world.map(player.mapId, player.instanceId).creatures.get(guid);
        byte[] pkt = world.combat.lootResponse(player, c);
        if (pkt != null) {
            send(Opcodes.SMSG_LOOT_RESPONSE, pkt);
        }
    }

    private void handleLootRelease(World world, WowBuffer in) {
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        send(Opcodes.SMSG_LOOT_RELEASE_RESPONSE, world.combat.encodeLootRelease(guid));
    }

    private void handleCast(World world, WowBuffer in) {
        if (in.remaining() < 4) {
            return;
        }
        int spellId = in.getU32();
        int castCount = in.remaining() > 0 ? in.getU8() : 0;
        world.spells.cast(player, world.map(player.mapId, player.instanceId), world.nowMs(),
                spellId, castCount, in, this::send);
    }

    private void handleGossip(World world, WowBuffer in) {
        world.content.gossipHello(player, world.map(player.mapId, player.instanceId), in, this::send);
    }

    private void handleListInventory(World world, WowBuffer in) {
        world.content.listInventory(player, world.map(player.mapId, player.instanceId), in, this::send);
    }

    private void handleQuestQuery(World world, WowBuffer in) {
        world.content.queryQuest(player, world.map(player.mapId, player.instanceId), in, this::send);
    }

    private void handleQuestAccept(World world, WowBuffer in) {
        world.content.acceptQuest(player, world.map(player.mapId, player.instanceId), in, this::send);
    }

    private void handleQuestComplete(World world, WowBuffer in) {
        world.content.completeQuest(player, world.map(player.mapId, player.instanceId), in, this::send);
    }

    private void handleGroupInvite(World world, WowBuffer in) {
        String name = in.getCString();
        Player t = world.playerByName(name);
        if (t == null || t.session == null) {
            return;
        }
        t.session.pendingGroupInvite(player);
        WowBuffer b = new WowBuffer(16);
        b.putCString(player.name);
        t.session.send(Opcodes.SMSG_GROUP_INVITE, b.array());
    }

    private void pendingGroupInvite(Player from) {
        pendingGroup = from.group != null ? from.group : new Group();
        if (from.group == null) {
            pendingGroup.leaderGuid = from.guid;
            pendingGroup.members.add(from);
            from.group = pendingGroup;
        }
    }

    private void handleGroupAccept(World world) {
        if (pendingGroup == null) {
            return;
        }
        pendingGroup.members.add(player);
        player.group = pendingGroup;
        sendGroupList();
        for (Player m : pendingGroup.members) {
            if (m.session != null) {
                m.session.sendGroupList();
            }
        }
    }

    private void sendGroupList() {
        if (player.group == null) {
            return;
        }
        WowBuffer b = new WowBuffer(64);
        b.putU8(player.group.raid ? 1 : 0);
        b.putU8(player.group.members.size());
        for (Player m : player.group.members) {
            b.putCString(m.name);
            b.putU64(m.guid);
            b.putU8(1);
            b.putU8(0);
            b.putU8(0);
            b.putU8(0);
        }
        b.putU64(player.group.leaderGuid);
        send(Opcodes.SMSG_GROUP_LIST, b.array());
    }

    private void handleGroupDisband(World world) {
        if (player.group == null) {
            return;
        }
        for (Player m : new ArrayList<>(player.group.members)) {
            m.group = null;
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_GROUP_DESTROYED, new byte[0]);
            }
        }
    }

    private void handleTrade(World world, WowBuffer in) {
        long guid = in.getU64();
        send(Opcodes.SMSG_TRADE_STATUS, u32(1));
        for (Player o : world.map(player.mapId, player.instanceId).players()) {
            if (o.guid == guid && o.session != null) {
                o.session.send(Opcodes.SMSG_TRADE_STATUS, u32(1));
            }
        }
    }

    private void handleWho(World world, WowBuffer in) {
        WowBuffer out = new WowBuffer(16);
        out.putU32(1);
        out.putU32(1);
        out.putCString(player.name);
        out.putCString("");
        out.putU32(player.level);
        out.putU32(player.clazz);
        out.putU32(player.race);
        out.putU32(player.zoneId);
        send(Opcodes.SMSG_WHO, out.array());
    }

    private void handleAddFriend(World world, WowBuffer in) {
        String name = in.getCString();
        WowBuffer out = new WowBuffer(16);
        out.putU8(0);
        out.putU64(0);
        out.putU8(1);
        send(Opcodes.SMSG_FRIEND_STATUS, out.array());
    }

    private void handleMail(World world, WowBuffer in) {
        send(Opcodes.SMSG_SEND_MAIL_RESULT, u32(0, 0, 0));
    }

    private void handleAuctionHello(World world, WowBuffer in) {
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        WowBuffer out = new WowBuffer(12);
        out.putU64(guid);
        out.putU32(1);
        send(Opcodes.MSG_AUCTION_HELLO, out.array());
    }

    private void handleBgJoin(World world, int map) {
        bgQueue = map;
        WowBuffer st = new WowBuffer(8);
        st.putU32(1);
        st.putU8(2);
        send(Opcodes.SMSG_BATTLEFIELD_STATUS, st.array());
        world.teleport(player, map, 0, 0, 0, 0);
        if (map == 489) {
            WowBuffer ws = new WowBuffer(24);
            ws.putU32(map);
            ws.putU32(0);
            ws.putU32(0);
            ws.putU16(2);
            ws.putU32(1545);
            ws.putU32(1);
            ws.putU32(1546);
            ws.putU32(1);
            send(Opcodes.SMSG_INIT_WORLD_STATES, ws.array());
        }
        if (map == 530) {
            worldStates2476 = 1;
            worldStates2478 = 1;
        }
    }

    private void handleRepop(World world) {
        player.ghost = true;
        player.setHealth(1);
        send(Opcodes.MSG_CORPSE_QUERY, u32(1));
    }

    private void handleReclaim(World world, WowBuffer in) {
        in.getU64();
        player.ghost = false;
        player.setHealth(player.maxHealth() == 0 ? 50 : player.maxHealth() / 2);
    }

    private void handleJoinChannel(WowBuffer in) {
        in.getU32();
        in.getU8();
        in.getU8();
        String name = in.getCString();
        if (in.remaining() > 0) {
            in.getCString();
        }
        channels.add(name);
        WowBuffer n = new WowBuffer(32);
        n.putU8(2);
        n.putCString(name);
        send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
        WowBuffer list = new WowBuffer(16);
        list.putU8(1);
        list.putCString(name);
        list.putU8(0);
        list.putU32(1);
        list.putU64(player.guid);
        send(Opcodes.SMSG_CHANNEL_LIST, list.array());
    }

    private void handleBuy(World world, WowBuffer in) {
        world.content.buy(player, world.map(player.mapId, player.instanceId), in, false, world.nextItemGuid(), this::send);
    }

    private void handleBuyInSlot(World world, WowBuffer in) {
        world.content.buy(player, world.map(player.mapId, player.instanceId), in, true, world.nextItemGuid(), this::send);
    }

    private void handleTalent(WowBuffer in) {
        in.getU32();
        in.getU32();
    }

    private void handleTrainer(World world, WowBuffer in) {
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        WowBuffer out = new WowBuffer(16);
        out.putU64(guid);
        out.putU32(0);
        out.putU32(0);
        send(Opcodes.SMSG_TRAINER_LIST, out.array());
    }

    private void handleTaxi(World world) {
        send(Opcodes.SMSG_NEW_TAXI_PATH, new byte[0]);
    }

    private void handleGoUse(World world, WowBuffer in) {
        in.getU64();
    }

    private void handleTicket(WowBuffer in) {
        lastTicket = in.getCString();
        send(Opcodes.SMSG_GMTICKET_CREATE, u32(0));
    }

    private void handleInspect(WowBuffer in) {
        long guid = in.getPackedGuid();
        WowBuffer out = new WowBuffer(16);
        out.putPackedGuid(guid);
        out.putU32(0);
        send(Opcodes.SMSG_INSPECT_TALENT, out.array());
    }

    private void handleDuel(World world) {
        WowBuffer cd = new WowBuffer(8);
        cd.putU8(3);
        send(Opcodes.SMSG_DUEL_COUNTDOWN, cd.array());
    }

    private void system(String msg) {
        send(Opcodes.SMSG_MESSAGECHAT, chatPacket(0, 0, 0, 0, msg, 0));
    }

    public static byte[] chatPacket(int type, int lang, long sender, long target, String msg, int tag) {
        WowBuffer b = new WowBuffer(64 + msg.length());
        b.putU8(type);
        b.putU32(lang);
        b.putU64(sender);
        b.putU32(0);
        b.putU64(target);
        byte[] utf = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        b.putU32(utf.length + 1);
        b.putCString(msg);
        b.putU8(tag);
        return b.array();
    }

    private static byte[] u32(int... v) {
        WowBuffer b = new WowBuffer(v.length * 4);
        for (int x : v) {
            b.putU32(x);
        }
        return b.array();
    }

    public void injectAccount(World.Account acc) {
        this.account = acc;
        this.status = STATUS_AUTHED;
        this.authedOnce = true;
    }

    private record Pkt(int opcode, byte[] payload) {}
}
