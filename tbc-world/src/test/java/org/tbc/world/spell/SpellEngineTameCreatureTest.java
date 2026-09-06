package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-082 — SPELL_EFFECT_TAMECREATURE (55). Tame Beast 13481.
 * CMaNGOS EffectTameCreature: hunter caster, pet from creature target, ForcedDespawn target.
 */
class SpellEngineTameCreatureTest {
    @Test
    void applyTameCreatureWhenHunterShouldCreatePetAndDespawnBeast() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TAME_CREATURE));
        Player hunter = new Player();
        hunter.clazz = Player.CLASS_HUNTER;
        Creature beast = new Creature();
        beast.entry = 26037;
        beast.level = 10;
        beast.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        beast.setHealth(100);
        SpellEngine.SpellInfo tame = new SpellEngine.SpellInfo(
                13481, SpellEngine.EFFECT_TAME_CREATURE, 0, 0, 0, 0, 0, 0f);
        eng.apply(hunter, beast, tame);
        assertNotNull(hunter.pet);
        assertEquals(26037, hunter.pet.entry);
        assertEquals(10, hunter.pet.level);
        assertTrue(hunter.pet.summoned);
        assertEquals(0, beast.health());
    }

    @Test
    void applyTameCreatureWhenNonHunterOrNonCreatureShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo tame = new SpellEngine.SpellInfo(
                13481, SpellEngine.EFFECT_TAME_CREATURE, 0, 0, 0, 0, 0, 0f);
        Player warrior = new Player();
        warrior.clazz = 1;
        Creature beast = new Creature();
        beast.entry = 26037;
        beast.setHealth(50);
        eng.apply(warrior, beast, tame);
        assertNull(warrior.pet);
        assertEquals(50, beast.health());
        eng.apply(new Creature(), beast, tame);
        assertEquals(50, beast.health());
        Player hunter = new Player();
        hunter.clazz = Player.CLASS_HUNTER;
        eng.apply(hunter, hunter, tame);
        assertNull(hunter.pet);
        Creature none = new Creature();
        none.entry = 0;
        none.setHealth(40);
        eng.apply(hunter, none, tame);
        assertNull(hunter.pet);
        assertEquals(40, none.health());
        eng.tameCreature(null, beast);
        eng.tameCreature(hunter, null);
    }
}
