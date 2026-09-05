package org.tbc.editor;

import org.tbc.world.content.ChrStatic;
import org.tbc.world.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Character CRUD rules. List/search via repository; persist via CharacterOps. */
public final class CharacterService {
    public static final int MIN_NAME = 2;
    public static final int MAX_NAME = 12;
    public static final int MAX_LEVEL = 70;
    /** GridDefines.h MAP_HALFSIZE. */
    static final float MAP_HALFSIZE = 533.33333f * 64 / 2;

    private final AccountLookup accounts;
    private final CharacterRepository repo;
    private final CharacterOps ops;

    public CharacterService(AccountLookup accounts, CharacterRepository repo, CharacterOps ops) {
        this.accounts = accounts;
        this.repo = repo;
        this.ops = ops;
    }

    public List<AccountRef> listAccounts(String query) {
        List<AccountRef> rows = accounts.list();
        String q = normalizeQuery(query);
        if (q.isEmpty()) {
            return rows;
        }
        List<AccountRef> out = new ArrayList<>();
        for (AccountRef a : rows) {
            if (a.username().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(a);
            }
        }
        return out;
    }

    public List<CharacterSummary> listCharacters(Integer accountId, String query) {
        String q = normalizeQuery(query);
        List<CharacterSummary> rows;
        if (q.isEmpty() && accountId != null) {
            rows = repo.listByAccount(accountId);
        } else {
            rows = repo.listAll();
        }
        List<CharacterSummary> named = new ArrayList<>();
        for (CharacterSummary s : rows) {
            named.add(named(s));
        }
        if (q.isEmpty()) {
            return named;
        }
        List<CharacterSummary> out = new ArrayList<>();
        for (CharacterSummary s : named) {
            if (s.name().toLowerCase(Locale.ROOT).contains(q)
                    || s.accountName().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(s);
            }
        }
        return out;
    }

    public CharacterDetail get(long guid) {
        CharacterSummary s = requireCharacter(guid);
        Player p = ops.load(s.accountId(), guid);
        if (p == null) {
            throw new EditorException("Character not found.");
        }
        return detail(p, named(s));
    }

    public CharacterDetail create(int accountId, String name, int race, int clazz, int gender,
                                  int skin, int face, int hair, int hairColor, int facial) {
        AccountRef account = requireAccount(accountId);
        String n = normalizeName(name);
        requirePlayable(race, clazz);
        requireExpansion(account, race);
        requireGender(gender);
        requireByte("Skin", skin);
        requireByte("Face", face);
        requireByte("Hair", hair);
        requireByte("Hair color", hairColor);
        requireByte("Facial hair", facial);
        if (ops.nameInUse(n)) {
            throw new EditorException("Name is already in use.");
        }
        Player p = ops.create(accountId, n, race, clazz, gender, skin, face, hair, hairColor, facial);
        if (p == null) {
            throw new EditorException("Name is already in use.");
        }
        CharacterSummary row = summary(p, account.username(), false);
        repo.put(row);
        return detail(p, row);
    }

    public CharacterDetail update(long guid, CharacterDraft draft) {
        CharacterSummary s = requireCharacter(guid);
        refuseOnline(s, "saved");
        String n = normalizeName(draft.name());
        if (!n.equalsIgnoreCase(s.name()) && ops.nameInUse(n)) {
            throw new EditorException("Name is already in use.");
        }
        requireGender(draft.gender());
        requireByte("Skin", draft.skin());
        requireByte("Face", draft.face());
        requireByte("Hair", draft.hairStyle());
        requireByte("Hair color", draft.hairColor());
        requireByte("Facial hair", draft.facialHair());
        requireLevel(draft.level());
        requireNonNegative("XP", draft.xp());
        requireNonNegative("Money", draft.money());
        requirePosition(draft.map(), draft.x(), draft.y(), draft.z(), draft.o());
        requireNonNegative("Zone", draft.zone());
        requirePosition(draft.bindMap(), draft.bindX(), draft.bindY(), draft.bindZ(), 0f);
        requireNonNegative("Bind zone", draft.bindZone());
        requireNonNegative("At login", draft.atLogin());
        requireNonNegative("Cinematic", draft.cinematic());
        Player p = ops.load(s.accountId(), guid);
        if (p == null) {
            throw new EditorException("Character not found.");
        }
        apply(p, draft, n);
        ops.save(p);
        CharacterSummary row = summary(p, named(s).accountName(), s.online());
        repo.put(row);
        return detail(p, row);
    }

    public void delete(long guid) {
        CharacterSummary s = requireCharacter(guid);
        refuseOnline(s, "deleted");
        if (!ops.delete(s.accountId(), guid)) {
            throw new EditorException("Cannot delete a guild leader.");
        }
        repo.remove(guid);
    }

    private CharacterSummary named(CharacterSummary s) {
        AccountRef a = accounts.findById(s.accountId());
        String accountName;
        if (a != null) {
            accountName = a.username();
        } else if (s.accountName() != null) {
            accountName = s.accountName();
        } else {
            accountName = "";
        }
        return new CharacterSummary(
                s.guid(), s.accountId(), accountName, s.name(), s.race(), s.clazz(),
                s.gender(), s.level(), s.map(), s.online());
    }

    private AccountRef requireAccount(int id) {
        AccountRef a = accounts.findById(id);
        if (a == null) {
            throw new EditorException("Account not found.");
        }
        return a;
    }

    private CharacterSummary requireCharacter(long guid) {
        CharacterSummary s = repo.find(guid);
        if (s == null) {
            throw new EditorException("Character not found.");
        }
        return s;
    }

    private static void apply(Player p, CharacterDraft d, String name) {
        p.name = name;
        p.gender = d.gender();
        p.skin = d.skin();
        p.face = d.face();
        p.hairStyle = d.hairStyle();
        p.hairColor = d.hairColor();
        p.facialHair = d.facialHair();
        p.level = d.level();
        p.xp = d.xp();
        p.money = d.money();
        p.mapId = d.map();
        p.zoneId = d.zone();
        p.relocate(d.x(), d.y(), d.z(), d.o());
        p.bindMap = d.bindMap();
        p.bindZone = d.bindZone();
        p.bindX = d.bindX();
        p.bindY = d.bindY();
        p.bindZ = d.bindZ();
        p.atLogin = d.atLogin();
        p.cinematic = d.cinematic();
        var r = ChrStatic.race(p.race);
        p.displayId = p.gender == 1 ? r.modelF() : r.modelM();
    }

    private static CharacterSummary summary(Player p, String accountName, boolean online) {
        return new CharacterSummary(
                p.guid, p.accountId, accountName, p.name, p.race, p.clazz, p.gender,
                p.level, p.mapId, online);
    }

    private static CharacterDetail detail(Player p, CharacterSummary s) {
        return new CharacterDetail(
                p.guid, p.accountId, s.accountName(), p.name, p.race, p.clazz, p.gender,
                p.level, p.xp, p.money, p.mapId, p.zoneId, p.x, p.y, p.z, p.o,
                p.bindMap, p.bindZone, p.bindX, p.bindY, p.bindZ,
                p.skin, p.face, p.hairStyle, p.hairColor, p.facialHair,
                p.atLogin, p.cinematic, s.online());
    }

    static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new EditorException("Name is required.");
        }
        String n = name.trim();
        if (n.length() < MIN_NAME || n.length() > MAX_NAME) {
            throw new EditorException("Name must be 2 to 12 characters.");
        }
        return n;
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private static void requirePlayable(int race, int clazz) {
        if (!knownRace(race) || !ChrStatic.playable(race, clazz)) {
            throw new EditorException("Race and class are not a playable combination.");
        }
    }

    private static boolean knownRace(int race) {
        for (ChrStatic.Race r : ChrStatic.RACES) {
            if (r.id() == race) {
                return true;
            }
        }
        return false;
    }

    private static void requireExpansion(AccountRef account, int race) {
        if (ChrStatic.race(race).expansion() > account.expansion()) {
            throw new EditorException("Account expansion is too low for that race.");
        }
    }

    private static void refuseOnline(CharacterSummary s, String action) {
        if (s.online()) {
            throw new EditorException("Online characters cannot be " + action + ".");
        }
    }

    private static void requireLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new EditorException("Level must be 1 to 70.");
        }
    }

    private static void requireGender(int gender) {
        if (gender != 0 && gender != 1) {
            throw new EditorException("Gender must be 0 or 1.");
        }
    }

    private static void requireByte(String label, int v) {
        if (v < 0 || v > 255) {
            throw new EditorException(label + " must be 0 to 255.");
        }
    }

    private static void requireNonNegative(String label, int v) {
        if (v < 0) {
            throw new EditorException(label + " must be >= 0.");
        }
    }

    private static void requirePosition(int map, float x, float y, float z, float o) {
        if (map < 0) {
            throw new EditorException("Map must be >= 0.");
        }
        if (!validMapCoord(x) || !validMapCoord(y)) {
            throw new EditorException("Coordinates are outside the map.");
        }
        if (!Float.isFinite(z) || !Float.isFinite(o)) {
            throw new EditorException("Coordinates are outside the map.");
        }
    }

    private static boolean validMapCoord(float c) {
        return Float.isFinite(c) && Math.abs(c) <= MAP_HALFSIZE - 0.5f;
    }
}
