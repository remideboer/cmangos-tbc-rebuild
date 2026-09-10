package org.tbc.world.persist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.DbPool;
import org.tbc.world.content.ChrStatic;
import org.tbc.world.content.LevelStats;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Mail;
import org.tbc.world.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class CharacterStore {
    private static final Logger log = LoggerFactory.getLogger(CharacterStore.class);
    private final DbPool chars;
    private final AtomicInteger nextGuid = new AtomicInteger(1);
    private final AtomicLong nextItem = new AtomicLong(1);
    private final Map<Integer, Player> memory = new ConcurrentHashMap<>();
    private final Map<Integer, List<Player>> byAccount = new ConcurrentHashMap<>();
    private final Map<Integer, Player> inWorld = new ConcurrentHashMap<>();
    private final Map<Integer, List<Player.Friend>> social = new ConcurrentHashMap<>();
    private final Map<Integer, Mail> mails = new ConcurrentHashMap<>();
    private final Map<Integer, List<Integer>> inbox = new ConcurrentHashMap<>();
    private final Map<Integer, String[]> declined = new ConcurrentHashMap<>();
    private final AtomicInteger nextMail = new AtomicInteger(1);

    public CharacterStore(DbPool chars) {
        this.chars = chars;
        if (chars != null) {
            try (Connection c = chars.get(); Statement s = c.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT IFNULL(MAX(guid),0)+1 FROM characters");
                if (rs.next()) {
                    nextGuid.set(rs.getInt(1));
                }
            } catch (Exception e) {
                log.warn("guid max: {}", e.getMessage());
            }
            try (Connection c = chars.get(); Statement s = c.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT IFNULL(MAX(guid),0)+1 FROM item_instance");
                if (rs.next()) {
                    nextItem.set(Math.max(1L, rs.getLong(1)));
                    deleteDanglingItemRefs(nextItem.get());
                }
            } catch (Exception e) {
                log.warn("item guid max: {}", e.getMessage());
            }
        }
    }

    /** ObjectMgr::SetHighestGuids — drop refs at or above the next item guid. */
    private void deleteDanglingItemRefs(long next) {
        deleteDangling("DELETE FROM character_inventory WHERE item >= ?", next);
        deleteDangling("DELETE FROM mail_items WHERE item_guid >= ?", next);
        deleteDangling("DELETE FROM auction WHERE itemguid >= ?", next);
        deleteDangling("DELETE FROM guild_bank_item WHERE item_guid >= ?", next);
    }

    private void deleteDangling(String sql, long next) {
        try (Connection c = chars.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, next);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("dangling item cleanup: {}", e.getMessage());
        }
    }

    public void clearOnline() {
        memory.values().forEach(p -> p.online = false);
        inWorld.values().forEach(p -> p.online = false);
        inWorld.clear();
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE characters SET online = 0 WHERE online <> 0");
        } catch (Exception e) {
            log.warn("clearOnline {}", e.getMessage());
        }
    }

    public int storedCount() {
        return memory.size();
    }

    public int storedCount(int accountId) {
        int n = 0;
        for (Player p : memory.values()) {
            if (p.accountId == accountId) {
                n++;
            }
        }
        return n;
    }

    /** Player.cpp DeleteOldCharacters. CharDelete.KeepDays default 30. keepDays 0 is a no-op. */
    public static final int CHARDELETE_KEEP_DAYS = 30;
    public static final long DAY_MS = 24L * 60 * 60_000;

    public void deleteOldCharacters(long nowMs) {
        deleteOldCharacters(nowMs, CHARDELETE_KEEP_DAYS);
    }

    public void deleteOldCharacters(long nowMs, int keepDays) {
        if (keepDays <= 0) {
            return;
        }
        long cutoff = nowMs - keepDays * DAY_MS;
        memory.values().removeIf(p -> p.deleteDateMs != 0 && p.deleteDateMs < cutoff);
        inWorld.values().removeIf(p -> p.deleteDateMs != 0 && p.deleteDateMs < cutoff);
        for (List<Player> list : byAccount.values()) {
            list.removeIf(p -> p.deleteDateMs != 0 && p.deleteDateMs < cutoff);
        }
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM characters WHERE deleteDate IS NOT NULL AND deleteDate < ?")) {
            ps.setLong(1, cutoff / 1000);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("deleteOldCharacters {}", e.getMessage());
        }
    }

    public void markDeleted(long guid, long deleteDateMs) {
        int g = Guid.low(guid);
        Player snap = memory.get(g);
        if (snap != null) {
            snap.deleteDateMs = deleteDateMs;
        }
        Player live = inWorld.get(g);
        if (live != null) {
            live.deleteDateMs = deleteDateMs;
        }
        for (List<Player> list : byAccount.values()) {
            for (Player p : list) {
                if (Guid.low(p.guid) == g) {
                    p.deleteDateMs = deleteDateMs;
                }
            }
        }
    }

    public int onlineCount() {
        int n = 0;
        for (Player p : memory.values()) {
            if (p.online) {
                n++;
            }
        }
        return n;
    }

    public List<Player> enumAccount(int accountId, ObjectMgr mgr) {
        if (chars == null) {
            List<Player> out = new ArrayList<>();
            for (Player p : memory.values()) {
                if (p.accountId == accountId && p.deleteDateMs == 0) {
                    out.add(p);
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
            return new ArrayList<>(byAccount.getOrDefault(accountId, List.of()));
        }
        List<Player> out = new ArrayList<>();
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT guid,name,race,class,gender,level,zone,map,position_x,position_y,position_z,playerBytes,playerBytes2,at_login,cinematic,orientation,money,health,power1,power4,guildId,playerFlags FROM characters WHERE account = ? AND deleteDate IS NULL");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Player p = fromRow(rs, accountId, mgr);
                try {
                    loadInventory(c, p);
                } catch (Exception ignored) {
                }
                attachStartItems(p, mgr);
                out.add(p);
            }
        } catch (Exception e) {
            try (Connection c = chars.get()) {
                PreparedStatement ps = c.prepareStatement(
                        "SELECT guid,name,race,class,gender,level,zone,map,position_x,position_y,position_z,playerBytes,playerBytes2,at_login,cinematic,orientation,money,health,power1,power4 FROM characters WHERE account = ? AND deleteDate IS NULL");
                ps.setInt(1, accountId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Player p = fromRow(rs, accountId, mgr);
                    try {
                        loadInventory(c, p);
                    } catch (Exception ignored) {
                    }
                    attachStartItems(p, mgr);
                    out.add(p);
                }
            } catch (Exception e2) {
                log.warn("enum {}", e2.getMessage());
            }
        }
        return out;
    }

    private Player fromRow(ResultSet rs, int accountId, ObjectMgr mgr) throws Exception {
        Player p = new Player();
        p.guid = Guid.player(rs.getInt("guid"));
        p.accountId = accountId;
        p.name = rs.getString("name");
        p.race = rs.getInt("race");
        p.clazz = rs.getInt("class");
        p.gender = rs.getInt("gender");
        p.level = rs.getInt("level");
        p.zoneId = rs.getInt("zone");
        p.mapId = rs.getInt("map");
        p.relocate(rs.getFloat("position_x"), rs.getFloat("position_y"), rs.getFloat("position_z"),
                col(rs, "orientation", 0f));
        int pb = rs.getInt("playerBytes");
        p.skin = pb & 0xFF;
        p.face = (pb >>> 8) & 0xFF;
        p.hairStyle = (pb >>> 16) & 0xFF;
        p.hairColor = (pb >>> 24) & 0xFF;
        p.facialHair = rs.getInt("playerBytes2") & 0xFF;
        p.atLogin = rs.getInt("at_login");
        p.cinematic = rs.getInt("cinematic");
        p.money = col(rs, "money", 0);
        p.xp = col(rs, "xp", 0);
        p.resting = col(rs, "is_logout_resting", 0) != 0;
        p.restBonus = col(rs, "rest_bonus", 0f);
        fillRace(p);
        if (mgr != null) {
            var ci = mgr.create(p.race, p.clazz);
            if (p.spells.isEmpty()) {
                List<Integer> sp = mgr.createSpells.get((int) ObjectMgr.key(p.race, p.clazz));
                if (sp != null) {
                    p.spells.addAll(sp);
                }
            }
            if (p.bindMap == 0) {
                p.bindMap = ci.map();
                p.bindZone = ci.zone();
                p.bindX = ci.x();
                p.bindY = ci.y();
                p.bindZ = ci.z();
            }
            mgr.applyCreateSkills(p);
        }
        initStatsForLevel(p, mgr);
        p.applyCreateFields();
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_HEALTH, Math.max(1, col(rs, "health", 50)));
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER1,
                Math.min(col(rs, "power1", 0), p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER1)));
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER4,
                Math.min(col(rs, "power4", 0), p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER4)));
        return p;
    }

    private static void applyCreateActions(Player p, ObjectMgr mgr) {
        if (mgr == null) {
            return;
        }
        int[] ab = mgr.createActions.get((int) ObjectMgr.key(p.race, p.clazz));
        if (ab != null) {
            System.arraycopy(ab, 0, p.actionButtons, 0, 132);
        }
    }

    private static int col(ResultSet rs, String n, int def) {
        try {
            return rs.getInt(n);
        } catch (Exception e) {
            return def;
        }
    }

    private static float col(ResultSet rs, String n, float def) {
        try {
            return rs.getFloat(n);
        } catch (Exception e) {
            return def;
        }
    }

    public Player create(int accountId, String name, int race, int clazz, int gender,
                         int skin, int face, int hair, int hairColor, int facial, ObjectMgr mgr) {
        if (!ChrStatic.playable(race, clazz)) {
            return null;
        }
        if (nameInUse(name)) {
            return null;
        }
        Player p = new Player();
        p.guid = Guid.player(nextGuid.getAndIncrement());
        p.accountId = accountId;
        p.name = name;
        p.race = race;
        p.clazz = clazz;
        p.gender = gender;
        p.skin = skin;
        p.face = face;
        p.hairStyle = hair;
        p.hairColor = hairColor;
        p.facialHair = facial;
        p.level = 1;
        p.atLogin = Player.AT_LOGIN_FIRST;
        p.cinematic = 0;
        fillRace(p);
        p.reputations.seedCreateDefaults(p.team);
        var ci = mgr.create(race, clazz);
        p.mapId = ci.map();
        p.zoneId = ci.zone();
        p.relocate(ci.x(), ci.y(), ci.z(), ci.o());
        p.bindMap = ci.map();
        p.bindZone = ci.zone();
        p.bindX = ci.x();
        p.bindY = ci.y();
        p.bindZ = ci.z();
        List<Integer> sp = mgr.createSpells.get((int) ObjectMgr.key(race, clazz));
        if (sp != null) {
            p.spells.addAll(sp);
        }
        applyCreateActions(p, mgr);
        if (mgr != null) {
            mgr.giveStartItems(p, this::nextItemGuid);
            mgr.applyCreateSkills(p);
        }
        initStatsForLevel(p, mgr);
        p.applyCreateFields();
        persistNew(p);
        byAccount.computeIfAbsent(accountId, a -> new ArrayList<>()).add(p);
        return p;
    }

    /** CMaNGOS Player::Create / LoadFromDB → InitStatsForLevel; a null ObjectMgr uses the level-1 seed rows. */
    private static void initStatsForLevel(Player p, ObjectMgr mgr) {
        p.initStatsForLevel(mgr != null ? mgr.levelStats : LevelStats.defaults());
    }

    private void fillRace(Player p) {
        var r = ChrStatic.race(p.race);
        p.faction = r.faction();
        p.displayId = p.gender == 1 ? r.modelF() : r.modelM();
        p.team = ChrStatic.team(p.race);
        p.powerType = ChrStatic.powerType(p.clazz);
    }

    /** ObjectMgr::GetPlayerNameByGUID — in-world first, then snapshot, then SQL. */
    public String nameByGuid(long guid) {
        int g = Guid.low(guid);
        Player live = inWorld.get(g);
        if (live != null) {
            return live.name;
        }
        Player snap = memory.get(g);
        if (snap != null) {
            return snap.name;
        }
        if (chars == null) {
            return null;
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement("SELECT name FROM characters WHERE guid = ?");
            ps.setInt(1, g);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (Exception e) {
            log.warn("nameByGuid {}", e.getMessage());
            return null;
        }
    }

    public boolean nameInUse(String name) {
        if (chars == null) {
            return memory.values().stream().anyMatch(p -> p.name.equalsIgnoreCase(name));
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement("SELECT 1 FROM characters WHERE name = ? AND deleteDate IS NULL");
            ps.setString(1, name);
            return ps.executeQuery().next();
        } catch (Exception e) {
            return memory.values().stream().anyMatch(p -> p.name.equalsIgnoreCase(name));
        }
    }

    /**
     * HandleCharRenameOpcode: guid belongs to this account, {@link Player#AT_LOGIN_RENAME} set, name free.
     * C++ empty query → CHAR_CREATE_ERROR (not CHAR_CREATE_NAME_IN_USE).
     */
    public boolean renameAtLogin(int accountId, long guid, String newName) {
        if (newName == null || nameExistsForRename(newName)) {
            return false;
        }
        int g = Guid.low(guid);
        Player snap = memory.get(g);
        if (snap != null) {
            if (snap.accountId != accountId || (snap.atLogin & Player.AT_LOGIN_RENAME) == 0) {
                return false;
            }
            applyRename(snap, newName);
            return true;
        }
        if (chars == null) {
            return false;
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT guid FROM characters WHERE guid = ? AND account = ? AND (at_login & ?) = ?");
            ps.setInt(1, g);
            ps.setInt(2, accountId);
            ps.setInt(3, Player.AT_LOGIN_RENAME);
            ps.setInt(4, Player.AT_LOGIN_RENAME);
            if (!ps.executeQuery().next()) {
                return false;
            }
            PreparedStatement up = c.prepareStatement(
                    "UPDATE characters SET name = ?, at_login = at_login & ~ ? WHERE guid = ?");
            up.setString(1, newName);
            up.setInt(2, Player.AT_LOGIN_RENAME);
            up.setInt(3, g);
            up.executeUpdate();
            return true;
        } catch (Exception e) {
            log.warn("renameAtLogin {}", e.getMessage());
            return false;
        }
    }

    private boolean nameExistsForRename(String name) {
        if (chars == null) {
            return memory.values().stream().anyMatch(p -> p.name.equalsIgnoreCase(name));
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement("SELECT 1 FROM characters WHERE name = ?");
            ps.setString(1, name);
            return ps.executeQuery().next();
        } catch (Exception e) {
            return memory.values().stream().anyMatch(p -> p.name.equalsIgnoreCase(name));
        }
    }

    private void applyRename(Player snap, String newName) {
        snap.name = newName;
        snap.atLogin &= ~Player.AT_LOGIN_RENAME;
        save(snap);
        List<Player> list = byAccount.get(snap.accountId);
        if (list == null) {
            return;
        }
        int g = Guid.low(snap.guid);
        for (Player p : list) {
            if (Guid.low(p.guid) == g) {
                p.name = newName;
                p.atLogin = snap.atLogin;
            }
        }
    }

    public boolean delete(int accountId, long guid) {
        int g = Guid.low(guid);
        Player live = memory.get(g);
        if (live != null && live.guildLeader) {
            return false;
        }
        if (chars != null) {
            try (Connection c = chars.get()) {
                PreparedStatement gl = c.prepareStatement("SELECT 1 FROM guild WHERE leaderGuid = ?");
                gl.setInt(1, g);
                if (gl.executeQuery().next()) {
                    return false;
                }
            } catch (Exception ignored) {
            }
            try (Connection c = chars.get()) {
                PreparedStatement ps = c.prepareStatement("DELETE FROM characters WHERE guid = ? AND account = ?");
                ps.setInt(1, g);
                ps.setInt(2, accountId);
                ps.executeUpdate();
            } catch (Exception e) {
                log.warn("delete {}", e.getMessage());
            }
        }
        inWorld.remove(g);
        Player p = memory.remove(g);
        if (p != null) {
            byAccount.getOrDefault(accountId, List.of()).removeIf(x -> Guid.low(x.guid) == g);
        }
        return true;
    }

    public Player load(int accountId, long guid, ObjectMgr mgr) {
        int g = Guid.low(guid);
        Player live = inWorld.get(g);
        if (live != null) {
            return live.accountId == accountId ? live : null;
        }
        Player snap = memory.get(g);
        if (snap != null) {
            if (snap.accountId != accountId) {
                return null;
            }
            Player copy = PlayerPersist.copy(snap);
            attachSocial(copy);
            attachDeclined(copy);
            return copy;
        }
        if (chars == null) {
            return null;
        }
        try (Connection c = chars.get()) {
            Player p = loadRow(c, accountId, g, mgr, true);
            if (p == null) {
                p = loadRow(c, accountId, g, mgr, false);
            }
            if (p == null) {
                return null;
            }
            try {
                if (loadActions(c, p) == 0) {
                    applyCreateActions(p, mgr);
                }
            } catch (Exception e) {
                applyCreateActions(p, mgr);
                log.warn("load actions {}", e.getMessage());
            }
            try {
                loadInventory(c, p);
                noteItemGuids(p);
            } catch (Exception e) {
                log.warn("load inventory {}", e.getMessage());
            }
            attachStartItems(p, mgr);
            try {
                loadSocialSql(c, p);
            } catch (Exception e) {
                log.warn("load social {}", e.getMessage());
            }
            try {
                loadDeclinedSql(c, p);
            } catch (Exception e) {
                log.warn("load declined {}", e.getMessage());
            }
            try {
                loadSpellCooldowns(c, p);
            } catch (Exception e) {
                log.warn("load spell cooldowns {}", e.getMessage());
            }
            memory.put(g, PlayerPersist.copy(p));
            attachSocial(p);
            attachDeclined(p);
            return p;
        } catch (Exception e) {
            log.warn("load {}", e.getMessage());
            return null;
        }
    }

    private Player loadRow(Connection c, int accountId, int g, ObjectMgr mgr, boolean withRest) throws Exception {
        String sql = withRest
                ? "SELECT guid,name,race,class,gender,level,xp,zone,map,position_x,position_y,position_z,playerBytes,playerBytes2,at_login,cinematic,orientation,money,health,power1,power4,is_logout_resting,rest_bonus FROM characters WHERE guid = ? AND account = ? AND deleteDate IS NULL"
                : "SELECT guid,name,race,class,gender,level,xp,zone,map,position_x,position_y,position_z,playerBytes,playerBytes2,at_login,cinematic,orientation,money,health,power1,power4 FROM characters WHERE guid = ? AND account = ? AND deleteDate IS NULL";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, g);
        ps.setInt(2, accountId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            return null;
        }
        return fromRow(rs, accountId, mgr);
    }

    private void attachStartItems(Player p, ObjectMgr mgr) {
        if (p == null || mgr == null) {
            return;
        }
        mgr.fillItemVisuals(p);
        if (p.items.isEmpty()) {
            mgr.giveStartItems(p, this::nextItemGuid);
            mgr.fillItemVisuals(p);
            if (!p.items.isEmpty()) {
                save(p);
            }
        }
        mgr.applyEquippedMelee(p);
        p.applyCreateFields();
    }

    private static int loadActions(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT button, action, type FROM character_action WHERE guid = ? ORDER BY button");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        int n = 0;
        while (rs.next()) {
            int button = rs.getInt("button") & 0xFF;
            if (button < 132) {
                int action = rs.getInt("action");
                int type = rs.getInt("type") & 0xFF;
                p.actionButtons[button] = (action & 0xFFFFFF) | (type << 24);
                n++;
            }
        }
        return n;
    }

    private static void loadInventory(Connection c, Player p) throws Exception {
        try {
            loadInventoryJoin(c, p, true);
        } catch (Exception e) {
            loadInventoryJoin(c, p, false);
        }
    }

    private static void loadInventoryJoin(Connection c, Player p, boolean itemEntry) throws Exception {
        String sql = itemEntry
                ? "SELECT ci.bag, ci.slot, ci.item, ci.item_template, ii.count, ii.durability FROM character_inventory ci JOIN item_instance ii ON ci.item = ii.guid WHERE ci.guid = ? ORDER BY ci.bag, ci.slot"
                : "SELECT ci.bag, ci.slot, ci.item, ci.item_template, ii.data FROM character_inventory ci JOIN item_instance ii ON ci.item = ii.guid WHERE ci.guid = ? ORDER BY ci.bag, ci.slot";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int itemGuid = rs.getInt("item");
            int entry = rs.getInt("item_template");
            Item it = new Item(itemGuid, entry);
            it.bag = rs.getInt("bag");
            it.slot = rs.getInt("slot");
            it.ownerGuid = Guid.low(p.guid);
            if (itemEntry) {
                it.count = Math.max(1, rs.getInt("count"));
                it.durability = rs.getInt("durability");
            } else {
                String data = rs.getString("data");
                parseItemData(it, data);
            }
            p.items.put(itemGuid, it);
        }
    }

    private static void parseItemData(Item it, String data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        int colon = data.indexOf(':');
        if (colon <= 0) {
            return;
        }
        try {
            it.entry = Integer.parseInt(data.substring(0, colon));
            it.count = Math.max(1, Integer.parseInt(data.substring(colon + 1)));
        } catch (NumberFormatException ignored) {
        }
    }

    public void save(Player p) {
        p.dirty = false;
        int g = Guid.low(p.guid);
        memory.put(g, PlayerPersist.copy(p));
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                writeCharactersRow(c, p);
                try {
                    writeActions(c, p);
                } catch (Exception e) {
                    log.warn("save actions {}", e.getMessage());
                }
                try {
                    writeSpellCooldowns(c, p);
                } catch (Exception e) {
                    log.warn("save spell cooldowns {}", e.getMessage());
                }
                Savepoint inv = c.setSavepoint();
                try {
                    writeInventory(c, p);
                } catch (Exception e) {
                    c.rollback(inv);
                    log.warn("save inventory {}", e.getMessage());
                }
                c.commit();
            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (Exception ignored) {
                }
                log.warn("save {}", e.getMessage());
            } finally {
                try {
                    c.setAutoCommit(auto);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.warn("save {}", e.getMessage());
        }
    }

    private static void writeCharactersRow(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM characters WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO characters (guid,account,name,race,class,gender,level,xp,money,playerBytes,playerBytes2,playerFlags,position_x,position_y,position_z,map,dungeon_difficulty,orientation,online,cinematic,totaltime,leveltime,logout_time,is_logout_resting,rest_bonus,zone,at_login,health,power1,power2,power3,power4,power5,watchedFaction) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        int i = 1;
        ins.setInt(i++, Guid.low(p.guid));
        ins.setInt(i++, p.accountId);
        ins.setString(i++, p.name);
        ins.setInt(i++, p.race);
        ins.setInt(i++, p.clazz);
        ins.setInt(i++, p.gender);
        ins.setInt(i++, p.level);
        ins.setInt(i++, p.xp);
        ins.setInt(i++, p.money);
        int pb = (p.skin & 0xFF) | ((p.face & 0xFF) << 8) | ((p.hairStyle & 0xFF) << 16) | ((p.hairColor & 0xFF) << 24);
        ins.setInt(i++, pb);
        ins.setInt(i++, p.facialHair & 0xFF);
        ins.setInt(i++, 0);
        ins.setFloat(i++, p.x);
        ins.setFloat(i++, p.y);
        ins.setFloat(i++, p.z);
        ins.setInt(i++, p.mapId);
        ins.setInt(i++, p.difficulty);
        ins.setFloat(i++, p.o);
        ins.setInt(i++, p.online ? 1 : 0);
        ins.setInt(i++, p.cinematic);
        ins.setInt(i++, 0);
        ins.setInt(i++, 0);
        ins.setLong(i++, System.currentTimeMillis() / 1000);
        ins.setInt(i++, p.resting ? 1 : 0);
        ins.setFloat(i++, p.restBonus);
        ins.setInt(i++, p.zoneId);
        ins.setInt(i++, p.atLogin);
        ins.setInt(i++, p.health());
        ins.setInt(i++, p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER1));
        ins.setInt(i++, 0);
        ins.setInt(i++, 0);
        ins.setInt(i++, p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER4));
        ins.setInt(i++, 0);
        ins.setLong(i++, Integer.toUnsignedLong(p.watchedFaction));
        ins.executeUpdate();
    }

    private static void writeActions(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM character_action WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO character_action (guid, button, action, type) VALUES (?,?,?,?)");
        for (int button = 0; button < 132; button++) {
            int packed = p.actionButtons[button];
            if (packed == 0) {
                continue;
            }
            ins.setInt(1, Guid.low(p.guid));
            ins.setInt(2, button);
            ins.setInt(3, packed & 0xFFFFFF);
            ins.setInt(4, (packed >>> 24) & 0xFF);
            ins.addBatch();
        }
        ins.executeBatch();
    }

    /** Player::_LoadSpellCooldowns — SpellExpireTime is unix seconds. */
    private static void loadSpellCooldowns(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT SpellId, SpellExpireTime, Category, CategoryExpireTime, ItemId FROM character_spell_cooldown WHERE guid = ?");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        long nowMs = System.currentTimeMillis();
        while (rs.next()) {
            int spellId = rs.getInt("SpellId");
            long expireMs = rs.getLong("SpellExpireTime") * 1000L;
            p.cooldowns.restoreSpell(spellId, expireMs, nowMs);
        }
    }

    /** Player::_SaveSpellCooldowns — DELETE-all then INSERT; expire as unix seconds. */
    private static void writeSpellCooldowns(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM character_spell_cooldown WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO character_spell_cooldown (guid, SpellId, SpellExpireTime, Category, CategoryExpireTime, ItemId) VALUES (?,?,?,?,?,?)");
        long nowMs = System.currentTimeMillis();
        for (var e : p.cooldowns.unexpiredSpellExpireMs(nowMs).entrySet()) {
            ins.setInt(1, Guid.low(p.guid));
            ins.setInt(2, e.getKey());
            ins.setLong(3, e.getValue() / 1000L);
            ins.setInt(4, 0);
            ins.setLong(5, 0);
            ins.setInt(6, 0);
            ins.addBatch();
        }
        ins.executeBatch();
    }

    private static void writeInventory(Connection c, Player p) throws Exception {
        PreparedStatement delInv = c.prepareStatement("DELETE FROM character_inventory WHERE guid = ?");
        delInv.setInt(1, Guid.low(p.guid));
        delInv.executeUpdate();
        PreparedStatement delInst = c.prepareStatement("DELETE FROM item_instance WHERE owner_guid = ?");
        delInst.setInt(1, Guid.low(p.guid));
        delInst.executeUpdate();
        for (Item it : p.items.values()) {
            if (!writeItemInstance(c, p, it)) {
                continue;
            }
            PreparedStatement inv = c.prepareStatement(
                    "INSERT INTO character_inventory (guid, bag, slot, item, item_template) VALUES (?,?,?,?,?)");
            inv.setInt(1, Guid.low(p.guid));
            inv.setInt(2, it.bag);
            inv.setInt(3, it.slot);
            inv.setInt(4, Guid.low(it.guid));
            inv.setInt(5, it.entry);
            inv.executeUpdate();
        }
    }

    private static boolean writeItemInstance(Connection c, Player p, Item it) {
        try (PreparedStatement ii = c.prepareStatement(
                "INSERT INTO item_instance (guid,owner_guid,itemEntry,creatorGuid,giftCreatorGuid,count,duration,charges,flags,enchantments,randomPropertyId,durability,itemTextId) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            int i = 1;
            ii.setInt(i++, Guid.low(it.guid));
            ii.setInt(i++, Guid.low(p.guid));
            ii.setInt(i++, it.entry);
            ii.setInt(i++, 0);
            ii.setInt(i++, 0);
            ii.setInt(i++, Math.max(1, it.count));
            ii.setInt(i++, 0);
            ii.setString(i++, "0 0 0 0 0");
            ii.setInt(i++, 0);
            ii.setString(i++, "0");
            ii.setInt(i++, 0);
            ii.setInt(i++, it.durability);
            ii.setInt(i++, 0);
            ii.executeUpdate();
            return true;
        } catch (Exception e) {
            try (PreparedStatement ii = c.prepareStatement(
                    "INSERT INTO item_instance (guid,owner_guid,data) VALUES (?,?,?)")) {
                ii.setInt(1, Guid.low(it.guid));
                ii.setInt(2, Guid.low(p.guid));
                ii.setString(3, it.entry + ":" + it.count);
                ii.executeUpdate();
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private void persistNew(Player p) {
        save(p);
    }

    public void setOnline(Player p, boolean on) {
        p.online = on;
        int g = Guid.low(p.guid);
        Player snap = memory.get(g);
        if (snap != null) {
            snap.online = on;
        }
        if (on) {
            inWorld.put(g, p);
        } else {
            inWorld.remove(g);
        }
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement("UPDATE characters SET online = ? WHERE guid = ?");
            ps.setInt(1, on ? 1 : 0);
            ps.setInt(2, g);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("online {}", e.getMessage());
        }
    }

    public long nextItemGuid() {
        return nextItem.getAndIncrement();
    }

    private void noteItemGuids(Player p) {
        for (Item it : p.items.values()) {
            nextItem.updateAndGet(v -> Math.max(v, it.guid + 1));
        }
    }

    public Player storedByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (Player p : memory.values()) {
            if (p.name.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public void attachSocial(Player p) {
        p.friends.clear();
        List<Player.Friend> rows = social.get(Guid.low(p.guid));
        if (rows == null) {
            return;
        }
        for (Player.Friend f : rows) {
            Player.Friend c = new Player.Friend();
            c.guid = f.guid;
            c.flags = f.flags;
            c.note = f.note;
            p.friends.add(c);
        }
    }

    public void setDeclinedNames(long guid, String[] cases) {
        declined.put(Guid.low(guid), cases.clone());
        if (chars == null) {
            return;
        }
        int g = Guid.low(guid);
        try (Connection c = chars.get()) {
            PreparedStatement del = c.prepareStatement("DELETE FROM character_declinedname WHERE guid = ?");
            del.setInt(1, g);
            del.executeUpdate();
            PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO character_declinedname (guid, genitive, dative, accusative, instrumental, prepositional) VALUES (?,?,?,?,?,?)");
            ins.setInt(1, g);
            for (int i = 0; i < 5; i++) {
                ins.setString(i + 2, cases[i] == null ? "" : cases[i]);
            }
            ins.executeUpdate();
        } catch (Exception e) {
            log.warn("setDeclinedNames {}", e.getMessage());
        }
    }

    private void attachDeclined(Player p) {
        String[] cases = declined.get(Guid.low(p.guid));
        p.declinedNames = cases == null ? null : cases.clone();
    }

    public void addFriend(int guid, Player.Friend row) {
        social.computeIfAbsent(guid, k -> new ArrayList<>()).add(row);
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO character_social (guid, friend, flags, note) VALUES (?,?,?,?)");
            ps.setInt(1, guid);
            ps.setInt(2, Guid.low(row.guid));
            ps.setInt(3, row.flags);
            ps.setString(4, row.note == null ? "" : row.note);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("addFriend {}", e.getMessage());
        }
    }

    public void setFriendNote(int guid, long friendGuid, String note) {
        List<Player.Friend> rows = social.get(guid);
        if (rows == null) {
            return;
        }
        Player.Friend found = null;
        for (Player.Friend f : rows) {
            if (f.guid == friendGuid) {
                found = f;
                break;
            }
        }
        if (found == null) {
            return;
        }
        found.note = note == null ? "" : note;
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "UPDATE character_social SET note = ? WHERE guid = ? AND friend = ?");
            ps.setString(1, found.note);
            ps.setInt(2, guid);
            ps.setInt(3, Guid.low(friendGuid));
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("setFriendNote {}", e.getMessage());
        }
    }

    public void removeFriend(int guid, long friendGuid) {
        List<Player.Friend> rows = social.get(guid);
        if (rows != null) {
            rows.removeIf(f -> f.guid == friendGuid);
        }
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get()) {
            PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM character_social WHERE guid = ? AND friend = ?");
            ps.setInt(1, guid);
            ps.setInt(2, Guid.low(friendGuid));
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("removeFriend {}", e.getMessage());
        }
    }

    public int nextMailId() {
        return nextMail.getAndIncrement();
    }

    public void storeMail(Mail m) {
        mails.put(m.id, m);
        inbox.computeIfAbsent(m.receiver, k -> new ArrayList<>());
        List<Integer> ids = inbox.get(m.receiver);
        if (!ids.contains(m.id)) {
            ids.add(m.id);
        }
        if (chars == null) {
            return;
        }
        try (Connection c = chars.get()) {
            PreparedStatement del = c.prepareStatement("DELETE FROM mail WHERE id = ?");
            del.setInt(1, m.id);
            del.executeUpdate();
            PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO mail (id,messageType,stationery,mailTemplateId,sender,receiver,subject,itemTextId,has_items,expire_time,deliver_time,money,cod,checked) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ins.setInt(1, m.id);
            ins.setInt(2, m.messageType);
            ins.setInt(3, m.stationery);
            ins.setInt(4, 0);
            ins.setInt(5, m.sender);
            ins.setInt(6, m.receiver);
            ins.setString(7, m.subject);
            ins.setInt(8, 0);
            ins.setInt(9, m.items.isEmpty() ? 0 : 1);
            ins.setLong(10, m.expireTime);
            ins.setLong(11, m.deliverTime);
            ins.setInt(12, m.money);
            ins.setInt(13, m.cod);
            ins.setInt(14, m.checked);
            ins.executeUpdate();
            PreparedStatement di = c.prepareStatement("DELETE FROM mail_items WHERE mail_id = ?");
            di.setInt(1, m.id);
            di.executeUpdate();
            for (Item it : m.items) {
                PreparedStatement mi = c.prepareStatement(
                        "INSERT INTO mail_items (mail_id, item_guid, item_template, receiver) VALUES (?,?,?,?)");
                mi.setInt(1, m.id);
                mi.setInt(2, Guid.low(it.guid));
                mi.setInt(3, it.entry);
                mi.setInt(4, m.receiver);
                mi.executeUpdate();
            }
        } catch (Exception e) {
            log.warn("storeMail {}", e.getMessage());
        }
    }

    public Mail mail(int id) {
        return mails.get(id);
    }

    public List<Mail> inbox(int receiver, long nowUnix) {
        List<Mail> out = new ArrayList<>();
        for (int id : inbox.getOrDefault(receiver, List.of())) {
            Mail m = mails.get(id);
            if (m != null && m.deliverTime <= nowUnix && m.state != Mail.MAIL_STATE_DELETED) {
                out.add(m);
            }
        }
        return out;
    }

    private void loadSocialSql(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT friend, flags, note FROM character_social WHERE guid = ?");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        List<Player.Friend> rows = new ArrayList<>();
        while (rs.next()) {
            Player.Friend f = new Player.Friend();
            f.guid = Guid.player(rs.getInt("friend"));
            f.flags = rs.getInt("flags");
            f.note = rs.getString("note");
            rows.add(f);
        }
        social.put(Guid.low(p.guid), rows);
    }

    private void loadDeclinedSql(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT genitive, dative, accusative, instrumental, prepositional FROM character_declinedname WHERE guid = ?");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            return;
        }
        String[] cases = new String[5];
        cases[0] = rs.getString(1);
        cases[1] = rs.getString(2);
        cases[2] = rs.getString(3);
        cases[3] = rs.getString(4);
        cases[4] = rs.getString(5);
        declined.put(Guid.low(p.guid), cases);
    }
}
