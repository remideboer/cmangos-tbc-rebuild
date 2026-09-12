package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Player::_LoadAuras — warrior without SPELL_AURA_MOD_SHAPESHIFT casts 2457.
 * TP-SL04-020 domain unit for {@link SpellEngine#applyDefaultWarriorStance}.
 */
class SpellEngineBattleStanceTest {
    @Test
    void applyDefaultWarriorStanceWhenNullShouldNoOp() {
        new SpellEngine().applyDefaultWarriorStance(null);
    }

    @Test
    void applyDefaultWarriorStanceWhenNotWarriorShouldLeaveFormNone() {
        Player mage = new Player();
        mage.clazz = 8;
        new SpellEngine().applyDefaultWarriorStance(mage);
        assertEquals(Unit.FORM_NONE, mage.shapeshiftForm());
        assertFalse(mage.spells.contains(SpellEngine.SPELL_BATTLE_STANCE));
    }

    @Test
    void applyDefaultWarriorStanceWhenAlreadyShapeshiftedShouldKeepForm() {
        Player p = new Player();
        p.clazz = SpellEngine.CLASS_WARRIOR;
        p.setShapeshiftForm(0x12);
        new SpellEngine().applyDefaultWarriorStance(p);
        assertEquals(0x12, p.shapeshiftForm());
        assertFalse(p.spells.contains(SpellEngine.SPELL_BATTLE_STANCE));
    }

    @Test
    void applyDefaultWarriorStanceWhenWarriorMissingSpellShouldLearnAndSetForm() {
        Player p = new Player();
        p.clazz = SpellEngine.CLASS_WARRIOR;
        p.level = 1;
        new SpellEngine().applyDefaultWarriorStance(p);
        assertTrue(p.spells.contains(SpellEngine.SPELL_BATTLE_STANCE));
        assertEquals(Unit.FORM_BATTLESTANCE, p.shapeshiftForm());
        assertEquals(SpellEngine.SPELL_BATTLE_STANCE, p.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    @Test
    void applyDefaultWarriorStanceWhenWarriorKnowsSpellShouldSetForm() {
        Player p = new Player();
        p.clazz = SpellEngine.CLASS_WARRIOR;
        p.spells.add(SpellEngine.SPELL_BATTLE_STANCE);
        new SpellEngine().applyDefaultWarriorStance(p);
        assertEquals(1, p.spells.stream().filter(id -> id == SpellEngine.SPELL_BATTLE_STANCE).count());
        assertEquals(Unit.FORM_BATTLESTANCE, p.shapeshiftForm());
    }
}
