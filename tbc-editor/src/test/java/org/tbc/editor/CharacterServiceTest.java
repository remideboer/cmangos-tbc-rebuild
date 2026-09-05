package org.tbc.editor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.persist.CharacterStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterServiceTest {
    private MemoryAccountLookup accounts;
    private MemoryCharacterRepository repo;
    private CharacterStore store;
    private ObjectMgr mgr;
    private ControllingOps ops;
    private CharacterService svc;

    @BeforeEach
    void setUp() {
        accounts = new MemoryAccountLookup();
        accounts.put(new AccountRef(1, "REMI", 1));
        accounts.put(new AccountRef(2, "REMI2", 0));
        repo = new MemoryCharacterRepository();
        store = new CharacterStore(null);
        mgr = new ObjectMgr();
        mgr.load(null, null);
        ops = new ControllingOps(new StoreCharacterOps(store, mgr));
        svc = new CharacterService(accounts, repo, ops);
    }

    @Test
    void listAccountsWhenQueryEmptyOrMatchShouldFilterUsernames() {
        assertEquals(2, svc.listAccounts(null).size());
        assertEquals(2, svc.listAccounts("  ").size());
        assertEquals(1, svc.listAccounts("remi2").size());
        assertEquals("REMI2", svc.listAccounts("EMI2").get(0).username());
        assertTrue(svc.listAccounts("zzz").isEmpty());
    }

    @Test
    void listCharactersWhenAccountSearchOrAllShouldMergeAccountNames() {
        CharacterDetail piep = svc.create(1, "Piep", 1, 1, 0, 0, 0, 0, 0, 0);
        svc.create(2, "OrcOne", 2, 1, 0, 0, 0, 0, 0, 0);
        repo.put(new CharacterSummary(99L, 99, null, "Ghost", 1, 1, 0, 1, 0, false));
        repo.put(new CharacterSummary(98L, 98, "STALE", "Stale", 1, 1, 0, 1, 0, false));

        List<CharacterSummary> all = svc.listCharacters(null, null);
        assertEquals(4, all.size());
        assertEquals(1, svc.listCharacters(1, "").size());
        assertEquals("Piep", svc.listCharacters(1, "  ").get(0).name());

        List<CharacterSummary> byName = svc.listCharacters(1, "orc");
        assertEquals(1, byName.size());
        assertEquals("OrcOne", byName.get(0).name());
        assertEquals("REMI2", byName.get(0).accountName());

        List<CharacterSummary> byAccount = svc.listCharacters(null, "REMI2");
        assertEquals(1, byAccount.size());
        assertEquals("OrcOne", byAccount.get(0).name());

        CharacterSummary ghost = svc.listCharacters(null, "ghost").get(0);
        assertEquals("", ghost.accountName());
        CharacterSummary stale = svc.listCharacters(null, "stale").get(0);
        assertEquals("STALE", stale.accountName());
        assertTrue(svc.listCharacters(null, "nope").isEmpty());

        CharacterDetail loaded = svc.get(piep.guid());
        assertEquals("Piep", loaded.name());
        assertEquals("REMI", loaded.accountName());
        assertEquals(1, loaded.level());
        assertFalse(loaded.online());
    }

    @Test
    void createWhenValidShouldPersistThroughCharacterStore() {
        CharacterDetail d = svc.create(1, "  Hero  ", 1, 1, 1, 2, 3, 4, 5, 6);
        assertEquals("Hero", d.name());
        assertEquals(1, d.gender());
        assertEquals(2, d.skin());
        assertEquals(3, d.face());
        assertEquals(4, d.hairStyle());
        assertEquals(5, d.hairColor());
        assertEquals(6, d.facialHair());
        assertEquals(1, d.accountId());
        assertEquals("REMI", d.accountName());
        assertEquals(1, svc.listCharacters(1, null).size());
    }

    @Test
    void createWhenBloodElfOrDraeneiOnTbcAccountShouldSucceed() {
        CharacterDetail be = svc.create(1, "SinDar", 10, 1, 0, 0, 0, 0, 0, 0);
        assertEquals(10, be.race());
        CharacterDetail dr = svc.create(1, "Exodar", 11, 1, 0, 0, 0, 0, 0, 0);
        assertEquals(11, dr.race());
    }

    @Test
    void createWhenValidationFailsShouldThrow() {
        assertMsg("Account not found.", () -> svc.create(9, "Aa", 1, 1, 0, 0, 0, 0, 0, 0));
        assertMsg("Name is required.", () -> svc.create(1, null, 1, 1, 0, 0, 0, 0, 0, 0));
        assertMsg("Name is required.", () -> svc.create(1, "  ", 1, 1, 0, 0, 0, 0, 0, 0));
        assertMsg("Name must be 2 to 12 characters.", () -> svc.create(1, "A", 1, 1, 0, 0, 0, 0, 0, 0));
        assertMsg("Name must be 2 to 12 characters.", () -> svc.create(1, "ABCDEFGHIJKLM", 1, 1, 0, 0, 0, 0, 0, 0));
        svc.create(1, "ABCDEFGHIJKL", 1, 1, 0, 0, 0, 0, 0, 0);
        assertMsg("Race and class are not a playable combination.",
                () -> svc.create(1, "BadRace", 9, 1, 0, 0, 0, 0, 0, 0));
        assertMsg("Race and class are not a playable combination.",
                () -> svc.create(1, "DkClass", 1, 6, 0, 0, 0, 0, 0, 0));
        assertMsg("Race and class are not a playable combination.",
                () -> svc.create(1, "NoClass", 1, 10, 0, 0, 0, 0, 0, 0));
        assertMsg("Account expansion is too low for that race.",
                () -> svc.create(2, "Blood", 10, 1, 0, 0, 0, 0, 0, 0));
        assertMsg("Account expansion is too low for that race.",
                () -> svc.create(2, "Draene", 11, 1, 0, 0, 0, 0, 0, 0));
        assertMsg("Gender must be 0 or 1.", () -> svc.create(1, "Gend", 1, 1, 2, 0, 0, 0, 0, 0));
        assertMsg("Hair must be 0 to 255.", () -> svc.create(1, "Hair", 1, 1, 0, 0, 0, 256, 0, 0));
        assertMsg("Hair color must be 0 to 255.", () -> svc.create(1, "Hcol", 1, 1, 0, 0, 0, 0, -1, 0));
        assertMsg("Facial hair must be 0 to 255.", () -> svc.create(1, "FaceH", 1, 1, 0, 0, 0, 0, 0, 256));
        assertMsg("Name is already in use.", () -> svc.create(2, "abcdefghijkl", 2, 1, 0, 0, 0, 0, 0, 0));
        ops.createReturnsNull = true;
        assertMsg("Name is already in use.", () -> svc.create(1, "UniqueOne", 1, 1, 0, 0, 0, 0, 0, 0));
    }

    @Test
    void updateWhenScalarsChangeShouldLoadThenSave() {
        CharacterDetail created = svc.create(1, "Piep", 1, 1, 0, 0, 0, 0, 0, 0);
        CharacterDraft draft = draft(
                "PiepTwo", 1, 1, 2, 3, 4, 5, 10, 100, 50,
                0, 12, -8949.95f, -132.493f, 83.5312f, 1.5f,
                0, 12, -8949.95f, -132.493f, 83.5312f, 0, 0);
        CharacterDetail saved = svc.update(created.guid(), draft);
        assertEquals("PiepTwo", saved.name());
        assertEquals(1, saved.gender());
        assertEquals(10, saved.level());
        assertEquals(100, saved.xp());
        assertEquals(50, saved.money());
        assertEquals(1.5f, saved.o());
        assertEquals(5, saved.facialHair());
        CharacterDetail again = svc.get(created.guid());
        assertEquals("PiepTwo", again.name());
        assertEquals(10, again.level());

        CharacterDraft sameName = draft(
                "pieptwo", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 12, 0f, 0f, 0f, 0f,
                0, 12, 0f, 0f, 0f, 0, 0);
        CharacterDetail cased = svc.update(created.guid(), sameName);
        assertEquals("pieptwo", cased.name());
        assertEquals(0, cased.gender());
    }

    @Test
    void updateWhenInvalidOrOnlineShouldRefuse() {
        CharacterDetail a = svc.create(1, "Alpha", 1, 1, 0, 0, 0, 0, 0, 0);
        svc.create(2, "Bravo", 2, 1, 0, 0, 0, 0, 0, 0);
        CharacterDraft ok = draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0);

        assertMsg("Character not found.", () -> svc.update(404L, ok));
        assertMsg("Name is already in use.", () -> svc.update(a.guid(), draft(
                "Bravo", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Level must be 1 to 70.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Level must be 1 to 70.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 71, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("XP must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, -1, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Money must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, -1,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Map must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                -1, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Coordinates are outside the map.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 20000f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Coordinates are outside the map.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 20000f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Coordinates are outside the map.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, Float.NaN, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Coordinates are outside the map.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, Float.POSITIVE_INFINITY, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Coordinates are outside the map.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, Float.NEGATIVE_INFINITY,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Zone must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, -1, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Bind zone must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, -1, 0f, 0f, 0f, 0, 0)));
        assertMsg("Hair color must be 0 to 255.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 256, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Map must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                -1, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Hair must be 0 to 255.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 256, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Face must be 0 to 255.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 256, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Skin must be 0 to 255.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, -1, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Gender must be 0 or 1.", () -> svc.update(a.guid(), draft(
                "Alpha", 2, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("Facial hair must be 0 to 255.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, -1, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0)));
        assertMsg("At login must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, -1, 0)));
        assertMsg("Cinematic must be >= 0.", () -> svc.update(a.guid(), draft(
                "Alpha", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, -1)));

        repo.put(new CharacterSummary(a.guid(), 1, "REMI", "Alpha", 1, 1, 0, 1, 0, true));
        assertMsg("Online characters cannot be saved.", () -> svc.update(a.guid(), ok));

        repo.put(new CharacterSummary(77L, 1, "REMI", "Missing", 1, 1, 0, 1, 0, false));
        ops.loadReturnsNull = true;
        assertMsg("Character not found.", () -> svc.get(77L));
        CharacterDraft orphan = draft(
                "Missing", 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0f, 0f, 0f, 0f,
                0, 0, 0f, 0f, 0f, 0, 0);
        assertMsg("Character not found.", () -> svc.update(77L, orphan));
    }

    @Test
    void deleteWhenGuildLeaderOrOnlineOrMissingShouldRefuse() {
        CharacterDetail a = svc.create(1, "Alpha", 1, 1, 0, 0, 0, 0, 0, 0);
        svc.delete(a.guid());
        assertTrue(svc.listCharacters(1, null).isEmpty());
        assertMsg("Character not found.", () -> svc.delete(a.guid()));
        assertMsg("Character not found.", () -> svc.get(a.guid()));

        CharacterDetail b = svc.create(1, "Bravo", 1, 1, 0, 0, 0, 0, 0, 0);
        ops.deleteReturnsFalse = true;
        assertMsg("Cannot delete a guild leader.", () -> svc.delete(b.guid()));
        assertEquals(1, svc.listCharacters(1, null).size());

        repo.put(new CharacterSummary(b.guid(), 1, "REMI", "Bravo", 1, 1, 0, 1, 0, true));
        ops.deleteReturnsFalse = false;
        assertMsg("Online characters cannot be deleted.", () -> svc.delete(b.guid()));
    }

    private static CharacterDraft draft(
            String name, int gender, int skin, int face, int hair, int hairColor, int facial,
            int level, int xp, int money, int map, int zone, float x, float y, float z, float o,
            int bindMap, int bindZone, float bindX, float bindY, float bindZ, int atLogin, int cinematic) {
        return new CharacterDraft(
                name, gender, skin, face, hair, hairColor, facial,
                level, xp, money, map, zone, x, y, z, o,
                bindMap, bindZone, bindX, bindY, bindZ, atLogin, cinematic);
    }

    private static void assertMsg(String msg, Runnable action) {
        EditorException e = assertThrows(EditorException.class, action::run);
        assertEquals(msg, e.getMessage());
    }

    private static final class ControllingOps implements CharacterOps {
        private final CharacterOps inner;
        boolean createReturnsNull;
        boolean deleteReturnsFalse;
        boolean loadReturnsNull;

        ControllingOps(CharacterOps inner) {
            this.inner = inner;
        }

        @Override
        public Player create(int accountId, String name, int race, int clazz, int gender,
                             int skin, int face, int hair, int hairColor, int facial) {
            if (createReturnsNull) {
                return null;
            }
            return inner.create(accountId, name, race, clazz, gender, skin, face, hair, hairColor, facial);
        }

        @Override
        public Player load(int accountId, long guid) {
            if (loadReturnsNull) {
                return null;
            }
            return inner.load(accountId, guid);
        }

        @Override
        public void save(Player p) {
            inner.save(p);
        }

        @Override
        public boolean delete(int accountId, long guid) {
            if (deleteReturnsFalse) {
                return false;
            }
            return inner.delete(accountId, guid);
        }

        @Override
        public boolean nameInUse(String name) {
            return inner.nameInUse(name);
        }
    }
}
