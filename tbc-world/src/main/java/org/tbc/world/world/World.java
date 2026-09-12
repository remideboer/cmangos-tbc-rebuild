package org.tbc.world.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.Bn;
import org.tbc.common.Conf;
import org.tbc.common.DbPool;
import org.tbc.common.Sha1;
import org.tbc.common.Srp6;
import org.tbc.common.WowBuffer;
import org.tbc.world.ai.DbScriptStore;
import org.tbc.world.ai.EventAi;
import org.tbc.world.ai.FactorySelector;
import org.tbc.world.ai.MotionMaster;
import org.tbc.world.ai.ScriptedCreatureAI;
import org.tbc.world.combat.Combat;
import org.tbc.world.combat.Factions;
import org.tbc.world.combat.MeleeTable;
import org.tbc.world.combat.XpFormulas;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Corpse;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.gm.GmCommands;
import org.tbc.world.map.GameMap;
import org.tbc.world.map.GraveyardManager;
import org.tbc.world.map.LineOfSight;
import org.tbc.world.map.Terrain;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.persist.CharacterStore;
import org.tbc.world.pvp.AbBattlefield;
import org.tbc.world.pvp.AvBattlefield;
import org.tbc.world.pvp.EyBattlefield;
import org.tbc.world.pvp.OutdoorPvp;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.session.AuctionHandler;
import org.tbc.world.session.DeathHandler;
import org.tbc.world.session.GroupHandler;
import org.tbc.world.session.WeatherHandler;
import org.tbc.world.session.WorldSession;
import org.tbc.world.spell.AuraSlots;
import org.tbc.world.spell.SpellCastTargets;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.events.GameEventMgr;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public final class World implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(World.class);
    public static final int TICK_MS = 50;
    static final int REALM_FLAG_OFFLINE = 0x02;

    public final Conf conf;
    public final DbPool login;
    public final DbPool worldDb;
    public final DbPool charsDb;
    public final CharacterStore characters;
    public final ObjectMgr objectMgr = new ObjectMgr();
    public final Content content = new Content(objectMgr);
    public final ScriptRegistry scripts = new ScriptRegistry();
    public final SpellEngine spells;
    public final Combat combat = new Combat();
    public final Factions factions;
    public final GmCommands gm;
    public final AbBattlefield ab = new AbBattlefield();
    public final AvBattlefield av = new AvBattlefield();
    public final EyBattlefield ey = new EyBattlefield();
    public final OutdoorPvp outdoorPvp = new OutdoorPvp();
    public final GameEventMgr events = new GameEventMgr();
    public final WorldTimers timers = new WorldTimers();
    public final Map<Long, Corpse> corpses = new ConcurrentHashMap<>();
    /** Channel name → password for CMSG_CHANNEL_PASSWORD / join checks. */
    public final Map<String, String> channelPasswords = new ConcurrentHashMap<>();
    /** Custom channel → owner guid (Channel.cpp first joiner when !IsPublic). */
    public final Map<String, Long> channelOwners = new ConcurrentHashMap<>();
    /** Custom channel → member guid → Channel.h PlayerInfo flags. */
    public final Map<String, ConcurrentHashMap<Long, Integer>> channelMemberFlags = new ConcurrentHashMap<>();
    /** Custom channel → banned member guids (Channel.cpp IsBanned). */
    public final Map<String, Set<Long>> channelBans = new ConcurrentHashMap<>();
    /** Channel name → join/leave announcements (custom default true, Channel.cpp). */
    public final Map<String, Boolean> channelAnnouncements = new ConcurrentHashMap<>();
    /** Channel name → moderation (Channel.cpp m_moderation default false). */
    public final Map<String, Boolean> channelModeration = new ConcurrentHashMap<>();
    public final Terrain terrain;
    public final GraveyardManager graveyards;
    public final String motd;
    public final int realmId;
    public final int instantLogout;
    public final int maxOverspeedPings;
    public final double sayRange;
    public final double yellRange;
    public final int saveIntervalMs;
    /** World.cpp MinPetitionSigns default 9. */
    public int minPetitionSigns = 9;

    private final Map<Integer, GameMap> maps = new ConcurrentHashMap<>();
    private final Map<Long, WorldSession> sessions = new ConcurrentHashMap<>();
    private final Queue<WorldSession> addQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong nowMs = new AtomicLong(System.currentTimeMillis());
    /** advanceMs offset so a ticked clock stays ahead of wall time (tests; 0 in production). */
    private final AtomicLong clockOffsetMs = new AtomicLong();
    private volatile boolean running = true;
    private long nextInstanceId = 1;
    public final Map<String, Account> testAccounts = new ConcurrentHashMap<>();

    public World(Conf conf, DbPool login, DbPool worldDb, DbPool charsDb) {
        this.conf = conf;
        this.spells = conf == null ? SpellEngine.alwaysHit() : new SpellEngine();
        this.login = login;
        this.worldDb = worldDb;
        this.charsDb = charsDb;
        this.characters = new CharacterStore(charsDb);
        this.characters.clearOnline();
        Path dataDir = conf == null ? null : Path.of(conf.get("DataDir", "."));
        this.terrain = Terrain.fromDataDir(dataDir);
        this.graveyards = GraveyardManager.seeded();
        this.objectMgr.load(worldDb, scripts, dataDir);
        this.factions = Factions.seeded();
        this.factions.loadFromDataDir(dataDir);
        this.objectMgr.factions = this.factions;
        this.graveyards.load(worldDb);
        this.gm = new GmCommands(conf == null || conf.getBool("GM.LowerSecurity", true));
        this.motd = conf == null ? "Welcome to the 8606 rebuild." : conf.get("Motd", "Welcome to the 8606 rebuild.");
        this.realmId = conf == null ? 1 : conf.getInt("RealmID", 1);
        this.instantLogout = conf == null ? 3 : conf.getInt("InstantLogout", 3);
        this.maxOverspeedPings = conf == null ? 2 : conf.getInt("MaxOverspeedPings", 2);
        this.sayRange = 25;
        this.yellRange = 300;
        this.saveIntervalMs = conf == null ? 900_000 : conf.getInt("PlayerSave.Interval", 900_000);
        loadCommandOverlay();
        seedStarterMobs();
        setRealmOffline(false);
    }

    public static World inMemory() {
        return new World(null, null, null, null);
    }

    private void loadCommandOverlay() {
        if (worldDb == null) {
            return;
        }
        try (Connection c = worldDb.get()) {
            PreparedStatement ps = c.prepareStatement("SELECT name, security FROM command");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                gm.overlay(rs.getString(1), rs.getInt(2));
            }
        } catch (Exception ignored) {
        }
    }

    /** Instantiates map 0/1 spawn rows at boot. Lazy grid load/unload is FR-CNT-002 / maps-grids-visibility.md — not this increment. */
    private void seedStarterMobs() {
        if (objectMgr.spawns.isEmpty()) {
            map(0, 0).add(objectMgr.spawnCreature(6, 0, -8900f, -120f, 80f, 0f, scripts));
        } else {
            int n = 0;
            for (ObjectMgr.Spawn s : objectMgr.spawns) {
                if (worldDb != null && s.map() != 0 && s.map() != 1) {
                    continue;
                }
                map(s.map(), 0).add(objectMgr.spawnCreature(s, scripts));
                n++;
            }
            log.info("instantiated {} creature spawns", n);
        }
        if (!objectMgr.goSpawns.isEmpty()) {
            int n = 0;
            for (ObjectMgr.Spawn s : objectMgr.goSpawns) {
                if (worldDb != null && s.map() != 0 && s.map() != 1) {
                    continue;
                }
                map(s.map(), 0).add(objectMgr.spawnGameObject(s));
                n++;
            }
            log.info("instantiated {} gameobject spawns", n);
        }
        Creature gruul = objectMgr.spawnCreature(19044, 565, 0, 0, 0, 0, scripts);
        gruul.scriptName = "boss_gruul";
        FactorySelector.selectAI(gruul, scripts);
        map(565, 0).add(gruul);
    }

    public long nowMs() {
        return nowMs.get();
    }

    /**
     * Player.cpp first save jitter: urand(interval/2, interval*3/2).
     * {@code roll} selects a delay in that inclusive range (CMaNGOS {@code urand}).
     */
    public static int jitteredFirstSaveMs(int intervalMs, int roll) {
        if (intervalMs <= 0) {
            return 0;
        }
        int lo = intervalMs / 2;
        int hi = (int) (intervalMs * 3L / 2);
        if (hi <= lo) {
            return lo;
        }
        int span = hi - lo + 1;
        int u = roll % span;
        if (u < 0) {
            u += span;
        }
        return lo + u;
    }

    public int jitteredFirstSaveMs() {
        return jitteredFirstSaveMs(saveIntervalMs, ThreadLocalRandom.current().nextInt());
    }

    /** Test / domain clock advance (logout delay, BG capture timers). */
    public void advanceMs(long deltaMs) {
        nowMs.addAndGet(deltaMs);
        clockOffsetMs.addAndGet(deltaMs);
        DeathHandler.tickDeathTimers(this);
    }

    public GameMap map(int mapId, int instanceId) {
        int key = mapId * 1_000_000 + instanceId;
        return maps.computeIfAbsent(key, k -> new GameMap(mapId, instanceId));
    }

    public Collection<GameMap> maps() {
        return maps.values();
    }

    public void addSession(WorldSession s) {
        addQueue.add(s);
    }

    public void queuePacket(WorldSession s, int opcode, byte[] payload) {
        s.queue(opcode, payload);
    }

    public Account lookupAccount(String username) {
        Account test = testAccounts.get(username.toUpperCase());
        if (test != null) {
            return test;
        }
        if (login == null) {
            return null;
        }
        try (Connection c = login.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT id, sessionkey, gmlevel, expansion, os, platform, locked, lockedIp FROM account WHERE username = ?");
            ps.setString(1, username.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            byte[] k = Bn.beHexToLe(rs.getString("sessionkey"), 40);
            return new Account(rs.getInt("id"), username.toUpperCase(), k, rs.getInt("gmlevel"),
                    rs.getInt("expansion"), rs.getString("os"), rs.getString("platform"));
        } catch (Exception e) {
            log.warn("account {}", e.getMessage());
            return null;
        }
    }

    public boolean verifyDigest(Account acc, int clientSeed, int serverSeed, byte[] digest) {
        if (acc == null || acc.sessionKey == null) {
            return false;
        }
        WowBuffer b = new WowBuffer(acc.username.length() + 4 + 4 + 4 + 40);
        b.putBytes(acc.username.getBytes(StandardCharsets.US_ASCII));
        b.putU32(0);
        b.putU32(clientSeed);
        b.putU32(serverSeed);
        b.putBytes(acc.sessionKey);
        return Bn.equal(Sha1.hash(b.array()), digest);
    }

    public boolean osAllowed(String os, String platform) {
        if (os == null) {
            return false;
        }
        if (!os.equals("Win") && !os.equals("OSX")) {
            return false;
        }
        if (platform == null) {
            return true;
        }
        if (platform.equals("x86")) {
            return true;
        }
        return platform.equals("PPC") && os.equals("OSX");
    }

    public Player playerByName(String name) {
        for (GameMap m : maps.values()) {
            for (Player p : m.players()) {
                if (p.name.equalsIgnoreCase(name)) {
                    return p;
                }
            }
        }
        return null;
    }

    public Player playerByGuid(long guid) {
        for (GameMap m : maps.values()) {
            Player p = m.players.get(guid);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    public java.util.List<Player> playersOnline() {
        java.util.ArrayList<Player> all = new java.util.ArrayList<>();
        for (GameMap m : maps.values()) {
            all.addAll(m.players.values());
        }
        return all;
    }

    public void teleport(Player p, int mapId, float x, float y, float z, float o) {
        if (p.mapId == mapId) {
            float ox = p.x;
            float oy = p.y;
            p.relocate(x, y, z, o);
            map(p.mapId, p.instanceId).reindex(p, ox, oy);
            if (p.session != null) {
                WowBuffer ack = new WowBuffer(64);
                ack.putPackedGuid(p.guid);
                ack.putU32(p.session.nextMoveOrder());
                p.movement.write(ack, false, p.guid, (int) nowMs());
                p.session.send(Opcodes.MSG_MOVE_TELEPORT_ACK, ack.array());
                WowBuffer obs = new WowBuffer(64);
                p.movement.write(obs, true, p.guid, (int) nowMs());
                for (Player other : map(p.mapId, p.instanceId).nearbyPlayers(p, GameMap.VISIBILITY)) {
                    if (other.session != null) {
                        other.session.send(Opcodes.MSG_MOVE_TELEPORT, obs.array());
                    }
                }
            }
            return;
        }
        if (p.session != null) {
            WowBuffer pending = new WowBuffer(4);
            pending.putU32(mapId);
            p.session.send(Opcodes.SMSG_TRANSFER_PENDING, pending.array());
            WowBuffer nw = new WowBuffer(20);
            nw.putU32(mapId);
            nw.putFloat(x);
            nw.putFloat(y);
            nw.putFloat(z);
            nw.putFloat(o);
            p.session.send(Opcodes.SMSG_NEW_WORLD, nw.array());
            p.session.forgetSeen();
        }
        GameMap old = map(p.mapId, p.instanceId);
        old.remove(p);
        p.mapId = mapId;
        if (mapId == 0 || mapId == 1 || mapId == 530) {
            p.instanceId = 0;
        }
        p.relocate(x, y, z, o);
        map(mapId, p.instanceId).add(p);
    }

    public long nextItemGuid() {
        return characters.nextItemGuid();
    }

    public int allocInstance() {
        return (int) nextInstanceId++;
    }

    public void meleeHit(Player p, Creature c) {
        applyMeleeHit(p, c, false);
    }

    public void meleeHitOffhand(Player p, Creature c) {
        applyMeleeHit(p, c, true);
    }

    /**
     * Player auto-attack vs a player (duel). Unit::DealDamage clamps to 1 HP and DuelComplete(DUEL_WON).
     */
    public void playerMeleeHit(Player p, Player victim) {
        if (p == null || victim == null || !victim.alive()) {
            return;
        }
        int min = Math.round(p.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE));
        int max = Math.round(p.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE));
        MeleeTable.Result r = MeleeTable.roll(p, victim, min, max);
        int before = victim.health();
        int dealt = r.damage();
        boolean duelEnded = false;
        if (dealt > 0) {
            if (p.duelOpponent == victim && dealt >= before - 1) {
                dealt = Math.max(0, before - 1);
                duelEnded = true;
            } else if (dealt > before) {
                dealt = before;
            }
            victim.setHealth(before - dealt);
            if (dealt != r.damage()) {
                r = new MeleeTable.Result(r.outcome(), dealt, dealt, r.blocked());
            }
        }
        byte[] log = combat.encodeAttack(p, victim, r);
        var hp = UpdateBuilder.maybeCompress(UpdateBuilder.values(victim, UpdateFields.UNIT_FIELD_HEALTH));
        GameMap hitMap = map(p.mapId, p.instanceId);
        java.util.LinkedHashSet<Player> set = new java.util.LinkedHashSet<>();
        set.add(p);
        set.add(victim);
        set.addAll(hitMap.nearbyPlayers(p, GameMap.VISIBILITY));
        set.addAll(hitMap.nearbyPlayers(victim, GameMap.VISIBILITY));
        for (Player pl : set) {
            if (pl.session == null) {
                continue;
            }
            pl.session.send(Opcodes.SMSG_ATTACKERSTATEUPDATE, log);
            pl.session.send(hp.opcode(), hp.payload());
        }
        if (duelEnded) {
            completeDuelWon(victim);
        }
    }

    /** Player::DuelComplete(DUEL_WON) — inspect-duel.md COMPLETE 1, WINNER 0 + names. */
    private void completeDuelWon(Player loser) {
        Player winner = loser.duelOpponent;
        if (winner == null) {
            return;
        }
        byte[] complete = new byte[]{1};
        if (loser.session != null) {
            loser.session.send(Opcodes.SMSG_DUEL_COMPLETE, complete);
        }
        if (winner.session != null) {
            winner.session.send(Opcodes.SMSG_DUEL_COMPLETE, complete);
        }
        WowBuffer names = new WowBuffer(64);
        names.putU8(0);
        names.putCString(winner.name);
        names.putCString(loser.name);
        byte[] winPkt = names.array();
        java.util.LinkedHashSet<WorldSession> sinks = new java.util.LinkedHashSet<>();
        if (loser.session != null) {
            sinks.add(loser.session);
        }
        if (winner.session != null) {
            sinks.add(winner.session);
        }
        GameMap m = map(loser.mapId, loser.instanceId);
        for (Player pl : m.nearbyPlayers(loser, GameMap.VISIBILITY)) {
            if (pl.session != null) {
                sinks.add(pl.session);
            }
        }
        for (Player pl : m.nearbyPlayers(winner, GameMap.VISIBILITY)) {
            if (pl.session != null) {
                sinks.add(pl.session);
            }
        }
        for (WorldSession s : sinks) {
            s.send(Opcodes.SMSG_DUEL_WINNER, winPkt);
        }
        combat.stopAttack(loser);
        combat.stopAttack(winner);
        loser.completeDuel();
    }

    private void applyMeleeHit(Player p, Creature c, boolean offhand) {
        GameMap hitMap = map(p.mapId, p.instanceId);
        boolean spellSwing = !offhand && p.hasNextMeleeSwingQueued();
        MeleeTable.Result r = combat.swing(p, c, nowMs(),
                (cr, t, spell) -> sendEventAiCast(hitMap, cr, t, spell), offhand);
        if (c.alive() && !c.inCombat && !c.evading && r.outcome() != MeleeTable.Outcome.EVADE) {
            engage(c, p);
        }
        if (r.damage() > 0) {
            p.rewardRageFromHit(r.damage(), r.outcome() == MeleeTable.Outcome.CRIT);
        }
        if (p.session != null) {
            p.session.send(Opcodes.SMSG_ATTACKERSTATEUPDATE, combat.encodeAttack(p, c, r, spellSwing, offhand));
            if (c.alive()) {
                var hp = UpdateBuilder.maybeCompress(UpdateBuilder.values(c, UpdateFields.UNIT_FIELD_HEALTH));
                p.session.send(hp.opcode(), hp.payload());
            }
            if (r.damage() > 0) {
                var pwr = UpdateBuilder.maybeCompress(
                        UpdateBuilder.values(p, UpdateFields.UNIT_FIELD_POWER1 + p.powerType));
                p.session.send(pwr.opcode(), pwr.payload());
            }
            if (!c.alive()) {
                p.session.send(Opcodes.SMSG_ATTACKSTOP, combat.encodeAttackStop(p.guid, c.guid, false));
            }
        }
        if (!c.alive()) {
            onCreatureKilled(p, c);
        }
    }

    /** Unit::Kill (creature victim) after the health hit 0: rewards, corpse loot, creature_death scripts, AV. */
    public void onCreatureKilled(Player p, Creature c) {
        GameMap m = map(c.mapId, p.instanceId);
        Player tapper = c.taggedBy != 0 ? playerByGuid(c.taggedBy) : p;
        if (tapper == null) {
            tapper = p;
        }
        rewardKill(p, c);
        if (tapper.session != null) {
            content.killedMonsterCredit(tapper, c, tapper.session::send);
        } else {
            content.killedMonsterCredit(tapper, c, (op, b) -> { });
        }
        objectMgr.fillCorpseLoot(c);
        byte[] stop = c.motion.stop(c);
        if (stop != null) {
            if (p.session != null) {
                p.session.send(Opcodes.SMSG_MONSTER_MOVE, stop);
            }
            for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
                if (pl.session != null && pl.guid != p.guid) {
                    pl.session.send(Opcodes.SMSG_MONSTER_MOVE, stop);
                }
            }
        }
        sendCorpseValues(m, c);
        m.dbScripts.start(objectMgr.dbScriptStore, DbScriptStore.CREATURE_DEATH, c.entry, c, p,
                (src, tgt, spell) -> sendDbScriptCast(m, src, tgt, spell));
        if (p.mapId == 30 && av.onGeneralKilled(c.entry)) {
            byte[] log = av.endedPvpLogPayload();
            for (Player pl : m.players()) {
                if (pl.session != null) {
                    pl.session.send(Opcodes.MSG_PVP_LOG_DATA, log);
                }
            }
        }
    }

    /**
     * SendMessageToSet after Unit::Kill: HEALTH 0 for everyone in range; UNIT_DYNAMIC_FLAGS built per viewer so
     * only the loot recipient sees UNIT_DYNFLAG_LOOTABLE (Object::BuildValuesUpdate).
     */
    private void sendCorpseValues(GameMap m, Creature c) {
        for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
            if (pl.session == null) {
                continue;
            }
            var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(c,
                    i -> i == UpdateFields.UNIT_DYNAMIC_FLAGS ? Combat.dynamicFlagsFor(c, pl) : c.values[i],
                    UpdateFields.UNIT_FIELD_HEALTH, UpdateFields.UNIT_DYNAMIC_FLAGS,
                    UpdateFields.UNIT_FIELD_TARGET, UpdateFields.UNIT_FIELD_TARGET + 1,
                    UpdateFields.UNIT_FIELD_FLAGS));
            pl.session.send(upd.opcode(), upd.payload());
        }
    }

    /** Loot::ForceLootAnimationClientUpdate — UNIT_DYNAMIC_FLAGS only, per viewer. */
    public void sendLootableFlags(Creature c, int instanceId) {
        GameMap m = map(c.mapId, instanceId);
        for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
            if (pl.session == null) {
                continue;
            }
            var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(c,
                    i -> i == UpdateFields.UNIT_DYNAMIC_FLAGS ? Combat.dynamicFlagsFor(c, pl) : c.values[i],
                    UpdateFields.UNIT_DYNAMIC_FLAGS));
            pl.session.send(upd.opcode(), upd.payload());
        }
    }

    /** A spell (or other non-melee damage) killed the creature: Combat death bookkeeping, then the kill rewards. */
    public void onCreatureKilledBySpell(Player p, Creature c) {
        GameMap m = map(c.mapId, p.instanceId);
        combat.creatureDied(c, p, nowMs(), (cr, t, spell) -> sendEventAiCast(m, cr, t, spell));
        onCreatureKilled(p, c);
    }

    /** Unit::Kill → tapper->RewardSinglePlayerAtKill → GiveXP(MaNGOS::XP::Gain); XP/level VALUES to self. */
    private void rewardKill(Player killer, Creature victim) {
        Player tapper = victim.taggedBy != 0 ? playerByGuid(victim.taggedBy) : null;
        if (tapper == null) {
            tapper = killer;
        }
        int[] changed = tapper.giveXp(XpFormulas.gain(tapper, victim), victim);
        if (changed.length > 0 && tapper.session != null) {
            var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(tapper, changed));
            tapper.session.send(upd.opcode(), upd.payload());
        }
    }

    /** CMaNGOS AttackStart + SMSG_ATTACKSTART to nearby (combat-log.md). */
    public void engage(Creature c, Player p) {
        if (c == null || p == null || !c.alive() || !p.alive()) {
            return;
        }
        boolean fresh = !c.inCombat;
        combat.startAttack(p, c, nowMs());
        if (!fresh) {
            return;
        }
        GameMap m = map(p.mapId, p.instanceId);
        if (c.eventAi != null) {
            c.eventAi.onAggro(c, p, (cr, t, spell) -> sendEventAiCast(m, cr, t, spell));
        }
        if (c.script != null) {
            c.script.aggro();
        }
        byte[] you = combat.encodeAttackStart(p.guid, c.guid);
        byte[] them = combat.encodeAttackStart(c.guid, p.guid);
        var vis = UpdateBuilder.maybeCompress(UpdateBuilder.values(c,
                UpdateFields.UNIT_FIELD_TARGET, UpdateFields.UNIT_FIELD_TARGET + 1, UpdateFields.UNIT_FIELD_FLAGS));
        for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
            if (pl.session != null) {
                pl.session.send(Opcodes.SMSG_ATTACKSTART, you);
                pl.session.send(Opcodes.SMSG_ATTACKSTART, them);
                pl.session.send(vis.opcode(), vis.payload());
            }
        }
    }

    /** CMaNGOS UnitAI::EnterEvadeMode → CombatStop / SendMeleeAttackStop + full health VALUES. */
    private void enterEvadeMode(GameMap m, Creature c, EventAi.SpellCast sink) {
        long victimGuid = c.victim;
        combat.evade(c, sink);
        byte[] stop = victimGuid != 0 ? combat.encodeAttackStop(c.guid, victimGuid, false) : null;
        var hp = UpdateBuilder.maybeCompress(UpdateBuilder.values(c, UpdateFields.UNIT_FIELD_HEALTH));
        for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
            if (pl.session == null) {
                continue;
            }
            if (stop != null) {
                pl.session.send(Opcodes.SMSG_ATTACKSTOP, stop);
            }
            pl.session.send(hp.opcode(), hp.payload());
        }
        if (!c.evading) {
            c.startOocMotion();
        }
    }

    public void creatureMeleeHit(Creature c, Player p) {
        GameMap hitMap = map(p.mapId, p.instanceId);
        boolean wasAlive = p.alive();
        MeleeTable.Result r = combat.swing(c, p, nowMs(), (cr, t, spell) -> sendEventAiCast(hitMap, cr, t, spell));
        // SendAttackStateUpdate + the victim's public UNIT_FIELD_HEALTH go SendMessageToSet (victim included).
        byte[] log = combat.encodeAttack(c, p, r);
        var hp = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.UNIT_FIELD_HEALTH));
        byte[] stop = p.alive() ? null : combat.encodeAttackStop(c.guid, p.guid, false);
        List<Player> set = new ArrayList<>(hitMap.nearbyPlayers(p, GameMap.VISIBILITY));
        set.add(p);
        for (Player pl : set) {
            if (pl.session == null) {
                continue;
            }
            pl.session.send(Opcodes.SMSG_ATTACKERSTATEUPDATE, log);
            pl.session.send(hp.opcode(), hp.payload());
            if (stop != null) {
                pl.session.send(Opcodes.SMSG_ATTACKSTOP, stop);
            }
        }
        // Unit::Kill → SetDeathState(JUST_DIED) → Player::Update KillPlayer.
        if (wasAlive && !p.alive() && p.session != null) {
            DeathHandler.killPlayer(p.session, this);
        }
    }

    private void creatureMeleeIfReady(Creature c, Player victim, int diff) {
        if (c.ai == null || !c.ai.meleeEnabled() || victim == null || !victim.alive() || !c.alive() || c.evading) {
            return;
        }
        c.meleeCooldownMs -= diff;
        if (c.meleeCooldownMs > 0) {
            return;
        }
        int swing = c.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME);
        c.meleeCooldownMs = swing > 0 ? swing : 2000;
        if (c.distance2d(victim) > Combat.meleeRange(c, victim, Combat.meleeLeeway(c, victim))) {
            return;
        }
        creatureMeleeHit(c, victim);
    }

    public void tick(int diff) {
        nowMs.set(System.currentTimeMillis() + clockOffsetMs.get());
        timers.advance(diff);
        if (timers.passed(WorldTimers.AUCTIONS)) {
            timers.reset(WorldTimers.AUCTIONS);
            AuctionHandler.expire(this);
        }
        WorldSession add;
        while ((add = addQueue.poll()) != null) {
            sessions.put(add.id(), add);
        }
        for (WorldSession s : sessions.values()) {
            s.processQueue(this);
            s.tick(this, diff);
        }
        // Unit::Update → m_currentSpells[i]->update(diff): cast bars finish here.
        spells.update(diff, nowMs());
        tickPeriodicAuras();
        expirePlayerAuras();
        if (timers.weatherPassed()) {
            timers.resetWeather();
            WeatherHandler.onTimer(this);
        }
        for (GameMap m : maps.values()) {
            // CMaNGOS Map::Update → VisitNearbyCellsOf(player), not every continent spawn.
            for (Creature c : m.creaturesNearPlayers(GameMap.VISIBILITY)) {
                if (!c.alive()) {
                    if (c.respawnAtMs > 0 && nowMs() >= c.respawnAtMs) {
                        combat.respawn(c);
                        for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
                            if (pl.session != null) {
                                var hp = UpdateBuilder.maybeCompress(
                                        UpdateBuilder.values(c, UpdateFields.UNIT_FIELD_HEALTH));
                                pl.session.send(hp.opcode(), hp.payload());
                            }
                        }
                    }
                    continue;
                }
                boolean combatPulse = c.inCombat || c.evading || c.motion.type() == MotionMaster.HOME;
                if (!combatPulse) {
                    if (c.nextUpdateMs > 0) {
                        c.nextUpdateMs -= diff;
                    }
                    if (c.nextUpdateMs > 0) {
                        continue;
                    }
                }
                if (!c.inCombat && !c.evading && c.script == null && c.eventAi == null
                        && c.motion.type() != MotionMaster.RANDOM && c.motion.type() != MotionMaster.HOME
                        && (c.ai == null || !c.ai.aggroOnSight())) {
                    continue;
                }
                EventAi.SpellCast sink = (cr, t, spell) -> sendEventAiCast(m, cr, t, spell);
                if (!c.inCombat && c.ai != null) {
                    c.ai.updateOoc(c, m.nearbyPlayers(c, GameMap.VISIBILITY), factions, LineOfSight::clear,
                            pl -> engage(c, pl));
                }
                if (c.inCombat) {
                    Player leashVictim = m.players.get(c.victim);
                    if (combat.shouldEvade(c, leashVictim, nowMs())) {
                        enterEvadeMode(m, c, sink);
                    } else if (combat.tickUnreachableEvade(c, leashVictim, diff)) {
                        enterEvadeMode(m, c, sink);
                    }
                }
                Player victim = m.players.get(c.victim);
                if (c.ai != null) {
                    c.ai.update(c, victim, diff, sink, () -> enterEvadeMode(m, c, sink));
                } else if (c.eventAi != null) {
                    c.eventAi.update(c, victim, diff, sink, () -> enterEvadeMode(m, c, sink));
                }
                if (c.script != null && c.inCombat && !(c.ai instanceof ScriptedCreatureAI)) {
                    Unit scriptVictim = m.players.values().stream().findFirst().orElse(null);
                    c.script.update(c, scriptVictim, diff, (cr, t, spell) -> {
                        if (t != null && spell == 36300) {
                            t.auras.add(new Unit.Aura(36300, 30_000, t.auras.size() + 1));
                        }
                    });
                }
                if (c.inCombat || c.motion.type() == MotionMaster.RANDOM || c.motion.type() == MotionMaster.HOME) {
                    float ox = c.x;
                    float oy = c.y;
                    byte[] spline = c.motion.update(c, diff, terrain.asHeight());
                    m.reindex(c, ox, oy);
                    if (spline != null) {
                        for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
                            if (pl.session != null) {
                                pl.session.send(Opcodes.SMSG_MONSTER_MOVE, spline);
                            }
                        }
                    }
                    if (c.motion.homeArrived(c)) {
                        combat.finishEvade(c, sink);
                        c.startOocMotion();
                    }
                }
                if (c.inCombat) {
                    creatureMeleeIfReady(c, victim, diff);
                }
                if (!c.inCombat && c.eventAi != null && c.eventAi.hasOocLos()) {
                    for (Player pl : m.nearbyPlayers(c, GameMap.VISIBILITY)) {
                        if (LineOfSight.clear(c, pl)) {
                            c.eventAi.onOocLos(c, pl, sink);
                        }
                    }
                }
                if (!c.inCombat && !c.evading && c.motion.type() != MotionMaster.HOME) {
                    c.nextUpdateMs = c.motion.type() == MotionMaster.RANDOM
                            ? Creature.RANDOM_UPDATE_MS
                            : Creature.IDLE_UPDATE_MS;
                }
            }
            m.dbScripts.process(diff, (src, tgt, spell) -> sendDbScriptCast(m, src, tgt, spell));
        }
        dropPlayerCombatWithoutHostiles();
        if (timers.passed(WorldTimers.GROUPS)) {
            timers.reset(WorldTimers.GROUPS);
            GroupHandler.updateOfflineLeaders(this);
        }
        if (timers.passed(WorldTimers.DELETECHARS)) {
            timers.reset(WorldTimers.DELETECHARS);
            characters.deleteOldCharacters(nowMs());
        }
        if (timers.passed(WorldTimers.CORPSES)) {
            timers.reset(WorldTimers.CORPSES);
            DeathHandler.removeOldCorpses(this);
        }
        if (timers.passed(WorldTimers.EVENTS)) {
            int next = events.update(this, nowMs());
            timers.setInterval(WorldTimers.EVENTS, next);
            timers.reset(WorldTimers.EVENTS);
        }
    }

    /**
     * CMaNGOS CombatManager: player IN_COMBAT with empty HostileRefManager → HandleExitCombat → CombatStop.
     * Walks map players (not World.sessions) so in-process doubles without AUTH_SESSION still drop.
     */
    private void dropPlayerCombatWithoutHostiles() {
        for (GameMap m : maps.values()) {
            for (Player p : m.players()) {
                if (p == null || !p.inCombat) {
                    continue;
                }
                if (!combat.shouldLeaveCombat(p, m.nearbyCreatures(p, GameMap.VISIBILITY))) {
                    continue;
                }
                long victim = p.victim;
                combat.stopAttack(p);
                byte[] stop = victim != 0 ? combat.encodeAttackStop(p.guid, victim, !p.alive()) : null;
                var flags = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.UNIT_FIELD_FLAGS));
                if (p.session != null) {
                    if (stop != null) {
                        p.session.send(Opcodes.SMSG_ATTACKSTOP, stop);
                    }
                    p.session.send(flags.opcode(), flags.payload());
                }
                for (Player pl : m.nearbyPlayers(p, GameMap.VISIBILITY)) {
                    if (pl.session == null) {
                        continue;
                    }
                    if (stop != null) {
                        pl.session.send(Opcodes.SMSG_ATTACKSTOP, stop);
                    }
                    pl.session.send(flags.opcode(), flags.payload());
                }
            }
        }
    }

    /** Unit::_UpdateSpells — periodic ticks before expiry so the last Amplitude is not lost. */
    private void tickPeriodicAuras() {
        long now = nowMs();
        for (GameMap m : maps.values()) {
            for (Player p : m.players()) {
                pulseUnitPeriodic(m, p, now);
            }
            for (Creature c : m.creaturesNearPlayers(GameMap.VISIBILITY)) {
                pulseUnitPeriodic(m, c, now);
            }
        }
    }

    private void pulseUnitPeriodic(GameMap m, Unit u, long now) {
        AuraSlots.pulsePeriodic(u, now, a -> {
            Unit caster = m.players.get(a.casterGuid());
            if (caster == null) {
                caster = m.creatures.get(a.casterGuid());
            }
            if (caster == null) {
                return;
            }
            BiConsumer<Integer, byte[]> send = (op, payload) -> {
                if (u instanceof Player self && self.session != null) {
                    self.session.send(op, payload);
                }
                for (Player pl : m.nearbyPlayers(u, GameMap.VISIBILITY)) {
                    if (pl.session != null) {
                        pl.session.send(op, payload);
                    }
                }
            };
            spells.tickPeriodic(caster, u, spells.info(a.spellId()), send);
        });
    }

    /** Unit::_UpdateSpells — expire timed holders on in-map players (AURA_REMOVE_BY_EXPIRE). */
    private void expirePlayerAuras() {
        long now = nowMs();
        for (GameMap m : maps.values()) {
            for (Player p : m.players()) {
                AuraSlots.expireTimed(p, now, p.session != null ? p.session::send : null);
            }
        }
    }

    @Override
    public void run() {
        long last = System.currentTimeMillis();
        while (running) {
            long start = System.currentTimeMillis();
            int diff = (int) Math.max(1, start - last);
            last = start;
            try {
                tick(diff);
            } catch (Exception e) {
                log.error("tick", e);
            }
            long spent = System.currentTimeMillis() - start;
            if (spent < TICK_MS) {
                try {
                    Thread.sleep(TICK_MS - spent);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void sendDbScriptCast(GameMap m, Unit src, Unit t, int spell) {
        if (src instanceof Creature cr) {
            sendEventAiCast(m, cr, t, spell);
        }
    }

    private void sendEventAiCast(GameMap m, Creature cr, Unit t, int spell) {
        SpellCastTargets tgt = new SpellCastTargets();
        Unit target = t == null ? cr : t;
        long hit = target.guid;
        byte[] start = spells.encodeStart(cr.guid, spell, 1, tgt);
        SpellEngine.SpellInfo info = spells.info(spell);
        int dmg = 0;
        if (info != null) {
            dmg = spells.apply(cr, target, info);
        }
        byte[] go = spells.encodeGo(cr.guid, hit, spell, nowMs(), tgt);
        for (Player pl : m.nearbyPlayers(cr, GameMap.VISIBILITY)) {
            if (pl.session != null) {
                pl.session.send(Opcodes.SMSG_SPELL_START, start);
                pl.session.send(Opcodes.SMSG_SPELL_GO, go);
                if (dmg > 0) {
                    pl.session.send(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG,
                            spells.encodeDamageLog(hit, cr.guid, info, dmg));
                    var hp = UpdateBuilder.maybeCompress(
                            UpdateBuilder.values(target, UpdateFields.UNIT_FIELD_HEALTH));
                    pl.session.send(hp.opcode(), hp.payload());
                }
            }
        }
    }

    public void stop() {
        running = false;
        for (WorldSession s : sessions.values()) {
            if (s.player() != null) {
                characters.save(s.player());
                characters.setOnline(s.player(), false);
            }
        }
        setRealmOffline(true);
    }

    private void setRealmOffline(boolean offline) {
        if (login == null) {
            return;
        }
        String sql = offline
                ? "UPDATE realmlist SET realmflags = realmflags | ? WHERE id = ?"
                : "UPDATE realmlist SET realmflags = realmflags & ~?, population = 0, realmbuilds = ? WHERE id = ?";
        try (Connection c = login.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, REALM_FLAG_OFFLINE);
            if (offline) {
                ps.setInt(2, realmId);
            } else {
                ps.setString(2, Integer.toString(Srp6.BUILD_8606));
                ps.setInt(3, realmId);
            }
            ps.executeUpdate();
            log.info("realm {} {}", realmId, offline ? "offline" : "online");
        } catch (Exception e) {
            log.warn("realmflags {}", e.getMessage());
        }
    }

    public record Account(int id, String username, byte[] sessionKey, int gmlevel, int expansion, String os, String platform) {}
}
