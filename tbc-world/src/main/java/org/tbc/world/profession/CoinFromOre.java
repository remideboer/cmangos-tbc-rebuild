package org.tbc.world.profession;

import org.tbc.world.content.Content;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.GameObjectUse;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Repeatable Blacksmithing assay: consume inventory ore + coal at a forge, grant copper.
 * Does not read or write mining-node / regional reserve state.
 */
public final class CoinFromOre {
    /** SpellFocusObject.dbc — Forge. */
    public static final int SPELL_FOCUS_FORGE = 3;
    /** SpellFocusObject.dbc — Cooking Fire (not a forge). */
    public static final int SPELL_FOCUS_COOKING = 4;
    /** tbc-db item_template. */
    public static final int ITEM_COPPER_ORE = 2770;
    public static final int ITEM_TIN_ORE = 2771;
    public static final int ITEM_IRON_ORE = 2772;
    public static final int ITEM_MITHRIL_ORE = 3858;
    public static final int ITEM_THORIUM_ORE = 10620;
    public static final int ITEM_FEL_IRON_ORE = 23424;
    public static final int ITEM_ADAMANTITE_ORE = 23425;
    public static final int ITEM_COAL = 3857;

    public static final int DEFAULT_MAX_CRAFTS = 50;

    public enum Result {
        OK, UNKNOWN_ORE, SKILL, MATERIALS, FORGE, LIMIT, FAILED
    }

    /**
     * Server-owned conversion table. Payouts beat {@code oreCount * vendorSellEach} (tbc-db SellPrice)
     * while staying well below typical Auction House ore listings.
     */
    public enum Tier {
        COPPER(ITEM_COPPER_ORE, 5, 1, 1, 1, 200, 5),
        TIN(ITEM_TIN_ORE, 5, 1, 65, 50, 400, 25),
        IRON(ITEM_IRON_ORE, 5, 1, 125, 100, 2000, 150),
        MITHRIL(ITEM_MITHRIL_ORE, 5, 1, 175, 150, 3500, 250),
        THORIUM(ITEM_THORIUM_ORE, 5, 1, 245, 200, 4000, 250),
        FEL_IRON(ITEM_FEL_IRON_ORE, 5, 1, 275, 275, 12000, 1000),
        ADAMANTITE(ITEM_ADAMANTITE_ORE, 5, 1, 325, 300, 18000, 1500);

        public final int oreItemId;
        public final int oreCount;
        public final int reagentCount;
        public final int miningRank;
        public final int blacksmithingRank;
        public final int copper;
        public final int vendorSellEach;

        Tier(int oreItemId, int oreCount, int reagentCount, int miningRank, int blacksmithingRank,
             int copper, int vendorSellEach) {
            this.oreItemId = oreItemId;
            this.oreCount = oreCount;
            this.reagentCount = reagentCount;
            this.miningRank = miningRank;
            this.blacksmithingRank = blacksmithingRank;
            this.copper = copper;
            this.vendorSellEach = vendorSellEach;
        }
    }

    public record Recipe(int oreItemId, int oreCount, int reagentItemId, int reagentCount,
                         int miningRank, int blacksmithingRank, int copper, int vendorSellEach) {
        static Recipe from(Tier t) {
            return new Recipe(t.oreItemId, t.oreCount, ITEM_COAL, t.reagentCount, t.miningRank,
                    t.blacksmithingRank, t.copper, t.vendorSellEach);
        }
    }

    private final int maxCrafts;

    public CoinFromOre() {
        this(DEFAULT_MAX_CRAFTS);
    }

    public CoinFromOre(int maxCrafts) {
        this.maxCrafts = maxCrafts;
    }

    public static CoinFromOre defaults() {
        return new CoinFromOre();
    }

    public List<Recipe> recipes() {
        return Arrays.stream(Tier.values()).map(Recipe::from).toList();
    }

    public boolean isAssayOre(int itemId) {
        return recipe(itemId) != null;
    }

    public Result craft(Player p, GameMap map, int oreItemId, Content content,
                        BiConsumer<Integer, byte[]> send) {
        if (p == null) {
            return Result.FAILED;
        }
        Recipe recipe = recipe(oreItemId);
        if (recipe == null) {
            return Result.UNKNOWN_ORE;
        }
        if (content == null) {
            return Result.FAILED;
        }
        if (maxCrafts > 0 && p.coinFromOreCrafts >= maxCrafts) {
            return Result.LIMIT;
        }
        if (p.skillValue(Content.SKILL_MINING) < recipe.miningRank()
                || p.skillValue(Content.SKILL_BLACKSMITHING) < recipe.blacksmithingRank()) {
            return Result.SKILL;
        }
        if (!forgeNearby(p, map)) {
            return Result.FORGE;
        }
        if (count(p, recipe.oreItemId()) < recipe.oreCount()
                || count(p, recipe.reagentItemId()) < recipe.reagentCount()) {
            return Result.MATERIALS;
        }
        BiConsumer<Integer, byte[]> out = send == null ? (op, pay) -> { } : send;
        content.destroyItemCount(p, recipe.oreItemId(), recipe.oreCount(), out);
        content.destroyItemCount(p, recipe.reagentItemId(), recipe.reagentCount(), out);
        p.setMoney(p.money + recipe.copper());
        p.coinFromOreCrafts++;
        p.dirty = true;
        if (send != null) {
            var coin = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
            send.accept(coin.opcode(), coin.payload());
        }
        return Result.OK;
    }

    static boolean forgeNearby(Player p, GameMap map) {
        if (p == null || map == null) {
            return false;
        }
        for (GameObject go : map.gameObjects.values()) {
            if (go.type != GameObjectUse.TYPE_SPELL_FOCUS) {
                continue;
            }
            if (go.spellFocusId != SPELL_FOCUS_FORGE) {
                continue;
            }
            double reach = go.spellFocusDist > 0 ? go.spellFocusDist : Content.INTERACT_RANGE;
            if (p.distance2d(go) <= reach) {
                return true;
            }
        }
        return false;
    }

    private Recipe recipe(int oreItemId) {
        for (Tier t : Tier.values()) {
            if (t.oreItemId == oreItemId) {
                return Recipe.from(t);
            }
        }
        return null;
    }

    private static int count(Player p, int itemId) {
        int n = 0;
        for (Item it : p.items.values()) {
            if (it.entry == itemId && it.bag == 0) {
                n += it.count;
            }
        }
        return n;
    }
}
