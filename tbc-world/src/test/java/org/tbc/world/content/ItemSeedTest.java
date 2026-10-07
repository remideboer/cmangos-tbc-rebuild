package org.tbc.world.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr.ItemTemplate;

/** Characterization for the hard-coded item_template seeds moved out of ObjectMgr. */
class ItemSeedTest {

    @Test
    void wornShortswordWhenSeededShouldMatchItemTemplateRow25() {
        ItemTemplate t = ItemSeed.wornShortsword();

        assertEquals(25, t.entry);
        assertEquals("Worn Shortsword", t.name);
        assertEquals(1900, t.delay);
        assertEquals(3, t.sheath);
    }

    @Test
    void heroTrainingStripWhenSeededShouldReuseHeroQuestJunkShape() {
        ItemTemplate t = ItemSeed.heroTrainingStrip();

        assertEquals(org.tbc.world.classless.HeroClassUnlock.ITEM_TRAINING_STRIP, t.entry);
        assertEquals(12, t.itemClass);
        assertEquals(20, t.stackable);
    }

    @Test
    void minorHealingPotionWhenSeededShouldCarryOnUseSpell439() {
        ItemTemplate t = ItemSeed.minorHealingPotion();

        assertEquals(Content.ITEM_MINOR_HEALING_POTION, t.entry);
        assertEquals(439, t.spellId[0]);
        assertEquals(-1, t.spellCharges[0]);
    }
}
