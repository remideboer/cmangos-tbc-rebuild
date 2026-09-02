package org.tbc.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistSteps {
    private static final int BUTTON = 131;
    private static final int PACKED = 6603;
    private static final World.Account ACCOUNT =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble client;
    private long guid;
    private int savedMap;
    private float savedX, savedY, savedZ, savedO;
    private int savedGold;
    private int charCount;
    private int buybackEntry;
    private long buybackGuid;

    @Given("an in-memory world")
    public void inMemoryWorld() {
        world = World.inMemory();
    }

    @Given("a logged-in character")
    @Given("a character already logged in")
    public void loggedInCharacter() {
        enterWorld("SliceFive");
    }

    @Given("a logged-in character with gold {int} and a modified action button")
    public void loggedInGoldAndBar(int gold) {
        enterWorld("SliceFive");
        Player p = client.session().player();
        p.money = gold;
        p.relocate(p.x + 12f, p.y + 8f, p.z + 1f, p.o);
        client.setActionButton(world, BUTTON, PACKED);
        rememberPosition(p);
        savedGold = gold;
    }

    @Given("a logged-in character who obtained item {int}")
    public void obtainedItem(int entry) {
        enterWorld("SliceFive");
        Player p = client.session().player();
        Item it = new Item(world.nextItemGuid(), entry);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = 23;
        it.count = 1;
        p.items.put(Guid.low(it.guid), it);
        p.dirty = true;
    }

    @Given("a logged-in character resting in an inn with rest bonus {int}")
    public void restingInn(int bonus) {
        enterWorld("SliceFive");
        Player p = client.session().player();
        p.resting = true;
        p.restBonus = bonus;
    }

    @Given("a character left online as after a crash")
    public void leftOnline() {
        enterWorld("SliceFive");
        assertTrue(world.characters.onlineCount() > 0);
    }

    @Given("a buyback slot occupied")
    public void buybackOccupied() {
        enterWorld("SliceFive");
        Player p = client.session().player();
        Item it = new Item(world.nextItemGuid(), 25);
        it.ownerGuid = Guid.low(p.guid);
        p.buyback.put(Guid.low(it.guid), it);
        buybackEntry = it.entry;
        buybackGuid = it.guid;
        assertTrue(p.items.isEmpty() || p.items.values().stream().noneMatch(i -> i.guid == buybackGuid));
    }

    @Given("all {int} action buttons are zero")
    public void allButtonsZero(int n) {
        enterWorld("SliceFive");
        assertEquals(132, n);
        for (int b = 0; b < 132; b++) {
            client.setActionButton(world, b, 0);
        }
    }

    @Given("the action-bar table is unavailable")
    public void actionTableUnavailable() {
        enterWorld("SliceFive");
        client.session().player().money = 50;
        savedGold = 50;
    }

    @When("the player logs out and relogs on a new mock client")
    @When("the player logs out immediately and relogs")
    @When("the player logs out and relogs later")
    @When("the player logs out and relogs")
    public void logoutAndRelog() {
        logoutOnly();
        relogFreshClient();
    }

    @When("the player requests logout")
    public void logoutOnly() {
        Player p = client.session().player();
        if (p != null) {
            rememberPosition(p);
            savedGold = p.money;
        }
        charCount = world.characters.storedCount(ACCOUNT.id());
        client.logout(world);
    }

    @When("the store saves the character twice")
    public void saveTwice() {
        Player p = client.session().player();
        world.characters.save(p);
        world.characters.save(p);
    }

    @When("the world clears online accounts")
    public void clearOnline() {
        world.characters.clearOnline();
    }

    @When("the mock client sends CMSG_PLAYER_LOGIN with a guid that is not on the account")
    public void loginUnknownGuid() {
        client.clear();
        client.login(world, 999_999L);
    }

    @When("a second mock client logs in the same guid")
    public void secondClientSameGuid() {
        WowClientDouble other = new WowClientDouble();
        other.connect(ACCOUNT);
        other.login(world, guid);
        client = other;
    }

    @When("the mock client sends CMSG_PLAYER_LOGIN with fewer than {int} bytes")
    public void truncatedLogin(int n) {
        assertEquals(8, n);
        client.clear();
        client.handle(world, Opcodes.CMSG_PLAYER_LOGIN, new byte[3]);
    }

    @When("the player logs out with gold {int}")
    public void logoutWithGold(int gold) {
        client.session().player().money = gold;
        savedGold = gold;
        logoutAndRelog();
    }

    @When("load is asked for this guid on a different account id")
    public void loadOtherAccount() {
        WowClientDouble other = new WowClientDouble();
        other.connect(new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86"));
        other.clear();
        other.login(world, guid);
        client = other;
    }

    @Then("SMSG_LOGIN_VERIFY_WORLD has the saved map and position")
    public void verifyWorld() {
        byte[] p = client.payload(Opcodes.SMSG_LOGIN_VERIFY_WORLD);
        assertEquals(20, p.length);
        assertEquals(savedMap, WowClientDouble.u32le(p, 0));
        assertEquals(savedX, WowClientDouble.floatle(p, 4), 0.01f);
        assertEquals(savedY, WowClientDouble.floatle(p, 8), 0.01f);
        assertEquals(savedZ, WowClientDouble.floatle(p, 12), 0.01f);
        assertEquals(savedO, WowClientDouble.floatle(p, 16), 0.01f);
        assertEquals(savedGold, client.session().player().money);
    }

    @Then("SMSG_ACTION_BUTTONS has {int} packed uint32 and the modified button")
    public void actionButtonsModified(int n) {
        byte[] p = client.payload(Opcodes.SMSG_ACTION_BUTTONS);
        assertEquals(n * 4, p.length);
        assertEquals(PACKED, WowClientDouble.u32le(p, BUTTON * 4));
    }

    @Then("the reconstructed bags still contain item {int}")
    public void bagsContain(int entry) {
        Player p = client.session().player();
        assertTrue(p.items.values().stream().anyMatch(it -> it.entry == entry));
    }

    @Then("rest bonus is not below {int}")
    public void restNotBelow(int min) {
        assertTrue(client.session().player().restBonus >= min);
        assertTrue(client.session().player().resting);
    }

    @And("the login burst still contains SMSG_SET_REST_START")
    public void restStart() {
        assertTrue(client.saw(Opcodes.SMSG_SET_REST_START));
    }

    @Then("the store still has exactly one row for that guid")
    public void oneRow() {
        assertEquals(1, world.characters.storedCount());
        assertEquals(1, world.characters.storedCount(ACCOUNT.id()));
    }

    @Then("no character is online")
    public void noneOnline() {
        assertEquals(0, world.characters.onlineCount());
    }

    @Then("the server sends SMSG_CHARACTER_LOGIN_FAILED with uint8 {word}")
    public void loginFailed(String codeTok) {
        int code = codeTok.startsWith("0x") || codeTok.startsWith("0X")
                ? Integer.parseInt(codeTok.substring(2), 16)
                : Integer.parseInt(codeTok);
        byte[] p = client.payload(Opcodes.SMSG_CHARACTER_LOGIN_FAILED);
        assertEquals(1, p.length);
        assertEquals(code, p[0] & 0xFF);
    }

    @And("the character count for the account is unchanged")
    public void charCountUnchanged() {
        assertEquals(charCount, world.characters.storedCount(ACCOUNT.id()));
    }

    @Then("bags do not contain the buyback item")
    public void noBuybackInBags() {
        Player p = client.session().player();
        assertFalse(p.items.values().stream().anyMatch(it -> it.guid == buybackGuid));
        assertTrue(p.buyback.isEmpty());
        assertFalse(p.items.values().stream().anyMatch(it -> it.entry == buybackEntry && it.guid == buybackGuid));
    }

    @Then("SMSG_ACTION_BUTTONS is {int} zero uint32")
    public void allZeroButtons(int n) {
        byte[] p = client.payload(Opcodes.SMSG_ACTION_BUTTONS);
        assertEquals(n * 4, p.length);
        for (int i = 0; i < n; i++) {
            assertEquals(0, WowClientDouble.u32le(p, i * 4));
        }
    }

    @Then("the session still answers CMSG_PING with SMSG_PONG")
    public void stillPong() {
        client.clear();
        client.ping(world, 42);
        assertTrue(client.saw(Opcodes.SMSG_PONG));
        assertEquals(42, WowClientDouble.u32le(client.payload(Opcodes.SMSG_PONG), 0));
    }

    @Then("reconstructed gold is {int}")
    public void reconstructedGold(int gold) {
        assertEquals(gold, client.session().player().money);
    }

    @Then("load returns empty and no SMSG login burst is sent")
    public void noBurstWrongAccount() {
        assertNull(world.characters.load(2, guid, world.objectMgr));
        assertFalse(client.saw(Opcodes.SMSG_LOGIN_VERIFY_WORLD));
        byte[] fail = client.payload(Opcodes.SMSG_CHARACTER_LOGIN_FAILED);
        assertEquals(1, fail.length);
        assertEquals(0x05, fail[0] & 0xFF);
    }

    private void enterWorld(String name) {
        client = new WowClientDouble();
        WorldSession s = client.connect(ACCOUNT);
        Player p = world.characters.create(ACCOUNT.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        guid = p.guid;
        rememberPosition(p);
        client.login(world, guid);
        assertEquals(s, client.session());
        assertTrue(client.saw(Opcodes.SMSG_LOGIN_VERIFY_WORLD));
    }

    private void relogFreshClient() {
        client = new WowClientDouble();
        client.connect(ACCOUNT);
        client.login(world, guid);
    }

    private void rememberPosition(Player p) {
        savedMap = p.mapId;
        savedX = p.x;
        savedY = p.y;
        savedZ = p.z;
        savedO = p.o;
    }
}
