package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-136 — SPELL_AURA_MOD_INVISIBILITY (18). Spell.dbc 11392 Invisibility.
 * CMaNGOS HandleInvisibility → PLAYER_FIELD_BYTES2 glow byte PLAYER_FIELD_BYTE2_INVISIBILITY_GLOW.
 */
class AuraEngineModInvisibilityTest {
    private static final SpellEngine.SpellInfo INVIS = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_INVISIBILITY, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_INVISIBILITY, 0, 0, 199, 199, 0f);

    @Test
    void applyAuraWhenModInvisibilityOnPlayerShouldSetGlowByte() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_INVISIBILITY));
        Player player = new Player();
        eng.apply(player, player, INVIS);
        assertTrue(player.hasAura(SpellEngine.SPELL_INVISIBILITY));
        int glowMask = Player.PLAYER_FIELD_BYTE2_INVISIBILITY_GLOW << 8;
        assertEquals(glowMask, player.getInt(UpdateFields.PLAYER_FIELD_BYTES2) & glowMask);
    }

    @Test
    void unapplyWhenModInvisibilityOnPlayerShouldClearGlowByte() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, INVIS);
        eng.unapplyAura(player, SpellEngine.SPELL_INVISIBILITY);
        int glowMask = Player.PLAYER_FIELD_BYTE2_INVISIBILITY_GLOW << 8;
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_BYTES2) & glowMask);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, INVIS);
        new AuraEngine().unapply(null, INVIS);
    }

    @Test
    void applyWhenCreatureShouldRecordAuraWithoutGlow() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, INVIS);
        assertTrue(mob.hasAura(SpellEngine.SPELL_INVISIBILITY));
    }
}
