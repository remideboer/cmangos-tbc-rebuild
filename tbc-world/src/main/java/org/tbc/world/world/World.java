package org.tbc.world.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.Bn;
import org.tbc.common.Conf;
import org.tbc.common.DbPool;
import org.tbc.common.Sha1;
import org.tbc.common.Srp6;
import org.tbc.common.WowBuffer;
import org.tbc.world.combat.Combat;
import org.tbc.world.combat.MeleeTable;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.gm.GmCommands;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.persist.CharacterStore;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.session.WorldSession;
import org.tbc.world.spell.SpellEngine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

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
    public final SpellEngine spells = new SpellEngine();
    public final Combat combat = new Combat();
    public final GmCommands gm;
    public final String motd;
    public final int realmId;
    public final int instantLogout;
    public final int maxOverspeedPings;
    public final double sayRange;
    public final double yellRange;
    public final int saveIntervalMs;

    private final Map<Integer, GameMap> maps = new ConcurrentHashMap<>();
    private final Map<Long, WorldSession> sessions = new ConcurrentHashMap<>();
    private final Queue<WorldSession> addQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong nowMs = new AtomicLong(System.currentTimeMillis());
    private volatile boolean running = true;
    private long nextInstanceId = 1;
    public final Map<String, Account> testAccounts = new ConcurrentHashMap<>();

    public World(Conf conf, DbPool login, DbPool worldDb, DbPool charsDb) {
        this.conf = conf;
        this.login = login;
        this.worldDb = worldDb;
        this.charsDb = charsDb;
        this.characters = new CharacterStore(charsDb);
        this.characters.clearOnline();
        Path dataDir = conf == null ? null : Path.of(conf.get("DataDir", "."));
        this.objectMgr.load(worldDb, scripts, dataDir);
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
        Creature gruul = objectMgr.spawnCreature(19044, 565, 0, 0, 0, 0, scripts);
        gruul.scriptName = "boss_gruul";
        gruul.script = scripts.create("boss_gruul");
        map(565, 0).add(gruul);
    }

    public long nowMs() {
        return nowMs.get();
    }

    public GameMap map(int mapId, int instanceId) {
        int key = mapId * 1_000_000 + instanceId;
        return maps.computeIfAbsent(key, k -> new GameMap(mapId, instanceId));
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
        if (p.session != null) {
            if (p.mapId != mapId) {
                WowBuffer pending = new WowBuffer(4);
                pending.putU32(mapId);
                p.session.send(Opcodes.SMSG_TRANSFER_PENDING, pending.array());
            }
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
        MeleeTable.Result r = combat.swing(p, c, nowMs());
        if (r.damage() > 0) {
            p.rewardRageFromHit(r.damage(), r.outcome() == MeleeTable.Outcome.CRIT);
        }
        if (p.session != null) {
            p.session.send(Opcodes.SMSG_ATTACKERSTATEUPDATE, combat.encodeAttack(p, c, r));
            var hp = UpdateBuilder.maybeCompress(UpdateBuilder.values(c, UpdateFields.UNIT_FIELD_HEALTH));
            p.session.send(hp.opcode(), hp.payload());
            if (r.damage() > 0) {
                var pwr = UpdateBuilder.maybeCompress(
                        UpdateBuilder.values(p, UpdateFields.UNIT_FIELD_POWER1 + p.powerType));
                p.session.send(pwr.opcode(), pwr.payload());
            }
            if (!c.alive()) {
                p.session.send(Opcodes.SMSG_ATTACKSTOP, combat.encodeAttackStop(p.guid, c.guid, false));
            }
        }
    }

    public void tick(int diff) {
        nowMs.set(System.currentTimeMillis());
        WorldSession add;
        while ((add = addQueue.poll()) != null) {
            sessions.put(add.id(), add);
        }
        for (WorldSession s : sessions.values()) {
            s.processQueue(this);
            s.tick(this, diff);
        }
        for (GameMap m : maps.values()) {
            for (Creature c : m.creatures.values()) {
                if (!c.inCombat && c.script == null) {
                    continue;
                }
                if (c.inCombat) {
                    Player victim = m.players.get(c.victim);
                    if (combat.shouldEvade(c, victim, nowMs())) {
                        combat.evade(c);
                    }
                }
                if (c.script != null && c.inCombat) {
                    Unit victim = m.players.values().stream().findFirst().orElse(null);
                    c.script.update(c, victim, diff, (cr, t, spell) -> {
                        if (t != null && spell == 36300) {
                            t.auras.add(new Unit.Aura(36300, 30_000, t.auras.size() + 1));
                        }
                    });
                }
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
