package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-041 — SPELL_EFFECT_PROFICIENCY (60). One-Handed Axes 196. */
class SpellEngineProficiencyTest {
    @Test
    void applyProficiencyWhenWeaponSpellShouldAddWeaponMask() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PROFICIENCY));
        Player caster = new Player();
        Creature dummy = new Creature();
        SpellEngine.SpellInfo axes = new SpellEngine.SpellInfo(
                196, SpellEngine.EFFECT_PROFICIENCY, 0, 0, 0, 0, 0, 0f, 1, Player.ITEM_CLASS_WEAPON);
        eng.apply(caster, dummy, axes);
        assertEquals(1, caster.weaponProficiency());
        WowBuffer b = new WowBuffer(SpellEngine.encodeSetProficiency(
                Player.ITEM_CLASS_WEAPON, caster.weaponProficiency()));
        assertEquals(Player.ITEM_CLASS_WEAPON, b.getU8());
        assertEquals(1, b.getU32());
        assertEquals(Opcodes.SMSG_SET_PROFICIENCY, 0x127);
    }

    @Test
    void applyProficiencyWhenArmorOrAlreadyKnownShouldAddOnce() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        Creature dummy = new Creature();
        SpellEngine.SpellInfo axes = new SpellEngine.SpellInfo(
                196, SpellEngine.EFFECT_PROFICIENCY, 0, 0, 0, 0, 0, 0f, 1, Player.ITEM_CLASS_WEAPON);
        SpellEngine.SpellInfo zeroMask = new SpellEngine.SpellInfo(
                196, SpellEngine.EFFECT_PROFICIENCY, 0, 0, 0, 0, 0, 0f, 0, Player.ITEM_CLASS_WEAPON);
        eng.apply(p, dummy, zeroMask);
        assertEquals(0, p.weaponProficiency());
        eng.apply(p, dummy, axes);
        eng.apply(p, dummy, axes);
        assertEquals(1, p.weaponProficiency());
        SpellEngine.SpellInfo cloth = new SpellEngine.SpellInfo(
                9078, SpellEngine.EFFECT_PROFICIENCY, 0, 0, 0, 0, 0, 0f, 1, Player.ITEM_CLASS_ARMOR);
        eng.apply(p, dummy, cloth);
        assertEquals(1, p.armorProficiency());
        eng.apply(p, dummy, cloth);
        assertEquals(1, p.armorProficiency());
        SpellEngine.SpellInfo other = new SpellEngine.SpellInfo(
                196, SpellEngine.EFFECT_PROFICIENCY, 0, 0, 0, 0, 0, 0f, 1, 0);
        eng.apply(p, dummy, other);
        assertEquals(1, p.weaponProficiency());
        assertEquals(1, p.armorProficiency());
    }

    @Test
    void applyProficiencyWhenNonPlayerCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature caster = new Creature();
        Player dummy = new Player();
        SpellEngine.SpellInfo axes = new SpellEngine.SpellInfo(
                196, SpellEngine.EFFECT_PROFICIENCY, 0, 0, 0, 0, 0, 0f, 1, Player.ITEM_CLASS_WEAPON);
        eng.apply(caster, dummy, axes);
        assertEquals(0, dummy.weaponProficiency());
    }
}
