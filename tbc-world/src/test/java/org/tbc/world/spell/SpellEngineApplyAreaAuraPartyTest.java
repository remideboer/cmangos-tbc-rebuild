package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-096 — SPELL_EFFECT_APPLY_AREA_AURA_PARTY (35). Devotion Aura 465.
 * CMaNGOS EffectApplyAreaAura: living unitTarget receives the aura.
 */
class SpellEngineApplyAreaAuraPartyTest {
    @Test
    void applyAreaAuraPartyWhenLivingShouldAddAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        SpellEngine.SpellInfo devotion = new SpellEngine.SpellInfo(
                465, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, devotion);
        assertEquals(1, p.auras.size());
        assertEquals(465, p.auras.get(0).spellId());
    }

    @Test
    void applyAreaAuraPartyWhenDeadOrMissingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo devotion = new SpellEngine.SpellInfo(
                465, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY, 0, 0, 0, 0, 0, 0f);
        eng.apply(dead, dead, devotion);
        assertEquals(0, dead.auras.size());
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(100);
        eng.applyAreaAuraParty(null, 465);
        eng.applyAreaAuraParty(dead, 465);
        eng.applyAreaAuraParty(live, 0);
        assertEquals(0, live.auras.size());
    }
}
