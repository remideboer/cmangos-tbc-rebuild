package org.tbc.world.profession;

import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.InventoryHandler;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.spell.GameObjectUse;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assay Ore: inventory ore + fuel at a forge → copper. Independent of mining-node state.
 */
class CoinFromOreTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void craftWhenEligibleAtForgeShouldConsumeCostsAndPayCopperForEveryTier() {
        for (CoinFromOre.Tier tier : CoinFromOre.Tier.values()) {
            World world = World.inMemory();
            Player p = skilled(world, 375, 375);
            GameMap map = world.map(p.mapId, p.instanceId);
            placeForge(map, p);
            give(world, p, tier.oreItemId, tier.oreCount);
            give(world, p, CoinFromOre.ITEM_COAL, tier.reagentCount);
            CoinFromOre.Result r = world.coinFromOre.craft(p, map, tier.oreItemId, world.content, this::noop);
            assertEquals(CoinFromOre.Result.OK, r, tier.name());
            assertEquals(0, count(p, tier.oreItemId), tier.name());
            assertEquals(0, count(p, CoinFromOre.ITEM_COAL), tier.name());
            assertEquals(tier.copper, p.money, tier.name());
            assertEquals(1, p.coinFromOreCrafts, tier.name());
        }
    }

    @Test
    void craftWhenMiningTooLowShouldRefuseWithoutConsuming() {
        World world = World.inMemory();
        Player p = skilled(world, 1, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_TIN_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.SKILL, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_TIN_ORE, world.content, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_TIN_ORE));
        assertEquals(1, count(p, CoinFromOre.ITEM_COAL));
        assertEquals(0, p.money);
    }

    @Test
    void craftWhenBlacksmithingTooLowShouldRefuseWithoutConsuming() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 1);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_TIN_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.SKILL, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_TIN_ORE, world.content, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_TIN_ORE));
        assertEquals(0, p.money);
    }

    @Test
    void craftWhenOreShortShouldRefuse() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 4);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.MATERIALS, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(4, count(p, CoinFromOre.ITEM_COPPER_ORE));
        assertEquals(1, count(p, CoinFromOre.ITEM_COAL));
        assertEquals(0, p.money);
    }

    @Test
    void craftWhenCoalShortShouldRefuse() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        assertEquals(CoinFromOre.Result.MATERIALS, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_COPPER_ORE));
        assertEquals(0, p.money);
    }

    @Test
    void craftWhenNoForgeShouldRefuse() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        GameObject chest = new GameObject();
        chest.guid = 0x1F00000000000002L;
        chest.type = GameObjectUse.TYPE_CHEST;
        chest.relocate(p.x, p.y, p.z, 0);
        map.add(chest);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.FORGE, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(CoinFromOre.Result.FORGE, world.coinFromOre.craft(
                p, null, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_COPPER_ORE));
        assertEquals(0, p.money);
    }

    @Test
    void craftWhenCookingFireShouldRefuse() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeFocus(map, p, CoinFromOre.SPELL_FOCUS_COOKING);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.FORGE, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_COPPER_ORE));
    }

    @Test
    void craftWhenForgeTooFarShouldRefuse() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        GameObject forge = forgeAt(p.x + 40, p.y, p.z);
        map.add(forge);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.FORGE, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_COPPER_ORE));
    }

    @Test
    void craftWhenUnknownOreShouldRefuse() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        assertEquals(CoinFromOre.Result.UNKNOWN_ORE, world.coinFromOre.craft(
                p, map, Content.ITEM_WORN_SHORTSWORD, world.content, this::noop));
    }

    @Test
    void craftWhenAtLimitShouldRefuseRepeat() {
        CoinFromOre assay = new CoinFromOre(1);
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 10);
        give(world, p, CoinFromOre.ITEM_COAL, 2);
        assertEquals(CoinFromOre.Result.OK, assay.craft(p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(CoinFromOre.Result.LIMIT, assay.craft(p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_COPPER_ORE));
        assertEquals(1, count(p, CoinFromOre.ITEM_COAL));
        assertEquals(CoinFromOre.Tier.COPPER.copper, p.money);
    }

    @Test
    void craftWhenPlayerNullShouldFail() {
        World world = World.inMemory();
        assertEquals(CoinFromOre.Result.FAILED, world.coinFromOre.craft(
                null, world.map(0, 0), CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
    }

    @Test
    void craftWhenSendNullShouldStillPay() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.OK, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, null));
        assertEquals(CoinFromOre.Tier.COPPER.copper, p.money);
    }

    @Test
    void craftWhenMaxCraftsZeroShouldAllowRepeat() {
        CoinFromOre assay = new CoinFromOre(0);
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 10);
        give(world, p, CoinFromOre.ITEM_COAL, 2);
        assertEquals(CoinFromOre.Result.OK, assay.craft(p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(CoinFromOre.Result.OK, assay.craft(p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        assertEquals(CoinFromOre.Tier.COPPER.copper * 2, p.money);
    }

    @Test
    void forgeNearbyWhenFocusDistZeroShouldUseInteractRange() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        GameObject go = forgeAt(p.x + 1, p.y, p.z);
        go.spellFocusDist = 0;
        map.add(go);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.OK, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
    }

    @Test
    void isAssayOreWhenCoalShouldBeFalse() {
        assertFalse(CoinFromOre.defaults().isAssayOre(CoinFromOre.ITEM_COAL));
        assertTrue(CoinFromOre.defaults().isAssayOre(CoinFromOre.ITEM_COPPER_ORE));
    }

    @Test
    void craftWhenContentMissingShouldRefuseWithoutGranting() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.FAILED, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, null, this::noop));
        assertEquals(5, count(p, CoinFromOre.ITEM_COPPER_ORE));
        assertEquals(0, p.money);
    }

    @Test
    void craftWhenOkShouldSendCoinageAndNotTouchForgeLoot() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        GameObject forge = placeForge(map, p);
        forge.lootGold = 99;
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        List<Integer> ops = new ArrayList<>();
        world.coinFromOre.craft(p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, (op, pay) -> ops.add(op));
        assertEquals(99, forge.lootGold);
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_OBJECT)
                || ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertTrue(ops.contains(Opcodes.SMSG_DESTROY_OBJECT));
    }

    @Test
    void saveWhenAssayedShouldKeepMoneyAndRemainingOreAfterLoad() {
        World world = World.inMemory();
        Player p = skilled(world, 375, 375);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        give(world, p, CoinFromOre.ITEM_COPPER_ORE, 6);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        assertEquals(CoinFromOre.Result.OK, world.coinFromOre.craft(
                p, map, CoinFromOre.ITEM_COPPER_ORE, world.content, this::noop));
        world.characters.save(p);
        Player loaded = world.characters.load(p.accountId, p.guid, world.objectMgr);
        assertEquals(CoinFromOre.Tier.COPPER.copper, loaded.money);
        assertEquals(1, count(loaded, CoinFromOre.ITEM_COPPER_ORE));
        assertEquals(0, count(loaded, CoinFromOre.ITEM_COAL));
    }

    @Test
    void sellItemWhenCopperOreShouldStillPayVendorSellPrice() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        p.learnSkill(Content.SKILL_MINING, 1, 75, 1);
        org.tbc.world.entity.Creature vendor = world.objectMgr.spawnCreature(
                Content.NPC_CORINA_STEELE, 0, p.x, p.y, p.z, p.o, null);
        vendor.npcFlags |= Content.UNIT_NPC_FLAG_VENDOR;
        world.map(p.mapId, p.instanceId).add(vendor);
        Item it = give(world, p, CoinFromOre.ITEM_COPPER_ORE, 1);
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(CoinFromOre.ITEM_COPPER_ORE);
        assertNotNull(t);
        assertTrue(t.sellPrice > 0);
        p.setMoney(0);
        sink.ops.clear();
        InventoryHandler.sellItem(sink.session, world, sell(vendor.guid, UpdateBuilder.itemGuid(it), 0));
        assertEquals(t.sellPrice, p.money);
        assertNull(p.items.get((int) it.guid));
    }

    @Test
    void useItemWhenAssayOreAtForgeShouldPayThroughExistingMoneyUpdate() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        p.learnSkill(Content.SKILL_MINING, 375, 375, 4);
        p.learnSkill(Content.SKILL_BLACKSMITHING, 375, 375, 4);
        GameMap map = world.map(p.mapId, p.instanceId);
        placeForge(map, p);
        Item ore = give(world, p, CoinFromOre.ITEM_COPPER_ORE, 5);
        give(world, p, CoinFromOre.ITEM_COAL, 1);
        p.setMoney(0);
        sink.ops.clear();
        InventoryHandler.useItem(sink.session, world, use(ore));
        assertEquals(CoinFromOre.Tier.COPPER.copper, p.money);
        assertEquals(0, count(p, CoinFromOre.ITEM_COPPER_ORE));
        assertTrue(sink.ops.contains(Opcodes.SMSG_UPDATE_OBJECT)
                || sink.ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    @Test
    void recipesShouldListEverySupportedTierWithVendorBeatingPayout() {
        for (CoinFromOre.Recipe r : CoinFromOre.defaults().recipes()) {
            assertTrue(r.copper() > r.oreCount() * r.vendorSellEach(),
                    r.oreItemId() + " payout must beat vendor");
            assertTrue(r.miningRank() >= 1);
            assertTrue(r.blacksmithingRank() >= 1);
            assertEquals(CoinFromOre.ITEM_COAL, r.reagentItemId());
        }
        assertEquals(7, CoinFromOre.defaults().recipes().size());
    }

    private void noop(int op, byte[] payload) {
    }

    private static Player skilled(World world, int mining, int blacksmithing) {
        Player p = world.characters.create(1, "Assay", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        p.x = 0;
        p.y = 0;
        p.z = 0;
        p.learnSkill(Content.SKILL_MINING, mining, 375, 4);
        p.learnSkill(Content.SKILL_BLACKSMITHING, blacksmithing, 375, 4);
        p.setMoney(0);
        return p;
    }

    private static GameObject placeForge(GameMap map, Player p) {
        return placeFocus(map, p, CoinFromOre.SPELL_FOCUS_FORGE);
    }

    private static GameObject placeFocus(GameMap map, Player p, int focusId) {
        GameObject go = forgeAt(p.x, p.y, p.z);
        go.spellFocusId = focusId;
        map.add(go);
        return go;
    }

    private static GameObject forgeAt(float x, float y, float z) {
        GameObject go = new GameObject();
        go.guid = 0x1F00000000000001L;
        go.type = GameObjectUse.TYPE_SPELL_FOCUS;
        go.spellFocusId = CoinFromOre.SPELL_FOCUS_FORGE;
        go.spellFocusDist = 10;
        go.relocate(x, y, z, 0);
        return go;
    }

    private static Item give(World world, Player p, int entry, int count) {
        Item it = new Item(world.nextItemGuid(), entry);
        it.slot = p.firstFreeBagSlot();
        it.bag = 0;
        it.count = count;
        p.items.put((int) it.guid, it);
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2, UpdateBuilder.itemGuid(it));
        return it;
    }

    private static int count(Player p, int entry) {
        int n = 0;
        for (Item it : p.items.values()) {
            if (it.entry == entry && it.bag == 0) {
                n += it.count;
            }
        }
        return n;
    }

    private static org.tbc.common.WowBuffer sell(long vendor, long item, int count) {
        org.tbc.common.WowBuffer b = new org.tbc.common.WowBuffer(17);
        b.putU64(vendor);
        b.putU64(item);
        b.putU8(count);
        return b;
    }

    private static org.tbc.common.WowBuffer use(Item it) {
        org.tbc.common.WowBuffer b = new org.tbc.common.WowBuffer(20);
        b.putU8(Player.INVENTORY_SLOT_BAG_0);
        b.putU8(it.slot);
        b.putU8(0);
        b.putU8(1);
        b.putU64(UpdateBuilder.itemGuid(it));
        b.putU32(0);
        return b;
    }

    private static Sink login(World world) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), "Assayer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        org.tbc.common.WowBuffer g = new org.tbc.common.WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final class Sink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();
        final Map<Integer, byte[]> last = new HashMap<>();
        WorldSession session;

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
            last.put(opcode, payload);
        }

        @Override
        public void close() {
        }
    }
}
