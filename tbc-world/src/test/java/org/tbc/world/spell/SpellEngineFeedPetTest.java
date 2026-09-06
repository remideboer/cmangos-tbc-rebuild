package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-067 — SPELL_EFFECT_FEED_PET (101). Feed Pet 6991 consumes one food. */
class SpellEngineFeedPetTest {
    @Test
    void applyFeedPetWhenLivingPetAndFoodShouldConsumeOne() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_FEED_PET));
        Player caster = new Player();
        caster.pet = new Pet();
        caster.pet.summoned = true;
        Item food = new Item(1, 117);
        food.count = 2;
        caster.items.put(1, food);
        caster.setSpellItemTarget(food);
        SpellEngine.SpellInfo feed = new SpellEngine.SpellInfo(
                6991, SpellEngine.EFFECT_FEED_PET, 0, 0, 0, 0, 0, 0f, 1539);
        eng.apply(caster, new Creature(), feed);
        assertEquals(1, food.count);
        assertTrue(caster.items.containsKey(1));
        food.count = 1;
        eng.apply(caster, new Creature(), feed);
        assertEquals(0, food.count);
        assertFalse(caster.items.containsKey(1));
    }

    @Test
    void applyFeedPetWhenMissingPetFoodOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        Item food = new Item(2, 117);
        food.count = 1;
        caster.items.put(2, food);
        caster.setSpellItemTarget(food);
        SpellEngine.SpellInfo feed = new SpellEngine.SpellInfo(
                6991, SpellEngine.EFFECT_FEED_PET, 0, 0, 0, 0, 0, 0f, 1539);
        eng.apply(caster, new Creature(), feed);
        assertEquals(1, food.count);
        Pet dead = new Pet();
        dead.summoned = false;
        caster.pet = dead;
        eng.apply(caster, new Creature(), feed);
        assertEquals(1, food.count);
        caster.pet = new Pet();
        caster.pet.summoned = true;
        eng.apply(new Creature(), new Creature(), feed);
        assertEquals(1, food.count);
        eng.feedPet(null, food, 1539);
        eng.feedPet(caster, null, 1539);
        Item empty = new Item(3, 117);
        empty.count = 0;
        eng.feedPet(caster, empty, 1539);
        assertEquals(1, food.count);
        assertTrue(caster.items.containsKey(2));
    }
}
