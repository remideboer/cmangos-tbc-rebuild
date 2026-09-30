package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.content.SkillLineAbility;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-165 — CREATE_ITEM craft success raises profession skill via UpdateCraftSkill.
 */
class SpellEngineCraftSkillTest {
    @Test
    void applyWhenCreateItemCraftSpellShouldRaiseBlacksmithing() {
        SpellEngine eng = SpellEngine.alwaysHit();
        eng.skillLineAbilities = SkillLineAbility.seeded();
        Player p = new Player();
        p.guid = 1;
        p.learnSkill(Content.SKILL_BLACKSMITHING, 1, 75, 1);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                SkillLineAbility.SPELL_ROUGH_SHARPENING_STONE,
                SpellEngine.EFFECT_CREATE_ITEM, 0, 0, 0, 1, 1, 0f, Content.ITEM_WORN_SHORTSWORD);
        assertEquals(0, eng.apply(p, p, sp));
        assertEquals(1, p.items.size());
        assertEquals(2, p.skillValue(Content.SKILL_BLACKSMITHING));
        assertTrue(p.takeDirtySkillFields().size() >= 2);
    }

    @Test
    void applyWhenCreateItemFailsShouldSkipCraftSkill() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player p = new Player();
        p.guid = 1;
        p.learnSkill(Content.SKILL_BLACKSMITHING, 1, 75, 1);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                SkillLineAbility.SPELL_ROUGH_SHARPENING_STONE,
                SpellEngine.EFFECT_CREATE_ITEM, 0, 0, 0, 0, 0, 0f, Content.ITEM_WORN_SHORTSWORD);
        assertEquals(0, eng.apply(p, p, sp));
        assertEquals(0, p.items.size());
        assertEquals(1, p.skillValue(Content.SKILL_BLACKSMITHING));
        assertEquals(0, eng.apply(p, new Creature(), new SpellEngine.SpellInfo(
                SkillLineAbility.SPELL_ROUGH_SHARPENING_STONE,
                SpellEngine.EFFECT_CREATE_ITEM, 0, 0, 0, 1, 1, 0f, Content.ITEM_WORN_SHORTSWORD)));
        assertEquals(1, p.skillValue(Content.SKILL_BLACKSMITHING));
        // Item created on player target but caster is not a player → no craft skill-up.
        assertEquals(0, eng.apply(new Creature(), p, new SpellEngine.SpellInfo(
                SkillLineAbility.SPELL_ROUGH_SHARPENING_STONE,
                SpellEngine.EFFECT_CREATE_ITEM, 0, 0, 0, 1, 1, 0f, Content.ITEM_WORN_SHORTSWORD)));
        assertEquals(1, p.items.size());
        assertEquals(1, p.skillValue(Content.SKILL_BLACKSMITHING));
    }
}
