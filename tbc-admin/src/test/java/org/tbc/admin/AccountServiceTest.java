package org.tbc.admin;

import org.tbc.common.Bn;
import org.tbc.common.Srp6;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {
    private AccountService svc;

    @BeforeEach
    void setUp() {
        svc = new AccountService(new MemoryAccountRepository());
    }

    @Test
    void createListRolePasswordDelete() {
        AccountRow row = svc.create(" tester ", "Secret1", "Secret1", AccountService.GM_PLAYER, 1);
        assertEquals("TESTER", row.username());
        assertEquals(1, row.id());
        assertEquals(1, row.expansion());
        assertSrp(row, "Secret1");
        assertSrp(row, "secret1");
        assertSrp(row, "SECRET1");
        assertEquals(1, svc.list().size());

        AccountRow gm = svc.setRole(row.id(), AccountService.GM_ADMINISTRATOR);
        assertEquals(AccountService.GM_ADMINISTRATOR, gm.gmlevel());
        assertEquals("Administrator", AccountService.roleName(gm.gmlevel()));

        AccountRow next = svc.setPassword(row.id(), "NewPass2", "NewPass2");
        assertSrp(next, "NewPass2");
        assertFalse(srpOk(next, "Secret1"));

        AccountRow classic = svc.create("ALT", "AltPass", "AltPass", AccountService.GM_MODERATOR, 0);
        assertEquals(0, classic.expansion());
        assertEquals("Moderator", AccountService.roleName(classic.gmlevel()));
        svc.create("GM", "GmPass", "GmPass", AccountService.GM_GAMEMASTER, 1);
        assertEquals("Gamemaster", AccountService.roleName(AccountService.GM_GAMEMASTER));
        assertEquals("Player", AccountService.roleName(AccountService.GM_PLAYER));
        assertEquals("Unknown", AccountService.roleName(9));

        svc.delete(row.id());
        assertEquals(2, svc.list().size());
        assertThrows(AccountException.class, () -> svc.delete(row.id()));
    }

    @Test
    void validationRejects() {
        assertMsg("Username is required.", () -> svc.create(null, "p", "p", 0, 1));
        assertMsg("Username is required.", () -> svc.create("  ", "p", "p", 0, 1));
        assertMsg("Username is too long.", () -> svc.create("ABCDEFGHIJKLMNOPQ", "p", "p", 0, 1));
        String sixteen = "ABCDEFGHIJKLMNOP";
        svc.create(sixteen, "p", "p", 0, 1);

        assertMsg("Password is required.", () -> svc.create("A", null, "x", 0, 1));
        assertMsg("Password is required.", () -> svc.create("B", "", "", 0, 1));
        assertMsg("Password is too long.", () -> svc.create("C", "12345678901234567", "12345678901234567", 0, 1));
        assertMsg("Passwords do not match.", () -> svc.create("D", "pw", null, 0, 1));
        assertMsg("Passwords do not match.", () -> svc.create("E", "pw", "no", 0, 1));
        svc.create("F", "1234567890123456", "1234567890123456", 0, 1);

        assertMsg("Role must be Player through Administrator.", () -> svc.create("G", "pw", "pw", -1, 1));
        assertMsg("Role must be Player through Administrator.", () -> svc.create("H", "pw", "pw", 4, 1));
        assertMsg("Expansion must be 0 or 1.", () -> svc.create("I", "pw", "pw", 0, -1));
        assertMsg("Expansion must be 0 or 1.", () -> svc.create("J", "pw", "pw", 0, 2));
        assertMsg("Username already exists.", () -> svc.create(sixteen, "pw", "pw", 0, 1));

        assertMsg("Account not found.", () -> svc.setRole(99, 0));
        assertMsg("Role must be Player through Administrator.", () -> svc.setRole(1, 9));
        assertMsg("Account not found.", () -> svc.setPassword(99, "pw", "pw"));
        svc.setPassword(1, "ok", "ok");
        assertMsg("Password is required.", () -> svc.setPassword(1, "", ""));
        assertMsg("Account not found.", () -> svc.delete(99));
    }

    private static void assertSrp(AccountRow row, String password) {
        assertTrue(srpOk(row, password));
    }

    private static boolean srpOk(AccountRow row, String password) {
        byte[] salt = Bn.beHexToLe(row.sHex(), 32);
        byte[] vLe = Bn.beHexToLe(row.vHex(), 32);
        Srp6.Session s = Srp6.serverChallenge(salt, vLe);
        Srp6.Client c = Srp6.clientRespond(row.username(), password, salt, s.bPubLe);
        if (!Srp6.serverSessionKey(s, c.aPubLe)) {
            return false;
        }
        return Srp6.proofM1(s, row.username(), c.m1);
    }

    private static void assertMsg(String msg, Runnable action) {
        AccountException e = assertThrows(AccountException.class, action::run);
        assertEquals(msg, e.getMessage());
    }
}
