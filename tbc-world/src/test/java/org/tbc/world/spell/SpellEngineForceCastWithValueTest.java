package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-084 — SPELL_EFFECT_FORCE_CAST_WITH_VALUE (141). Bloodbolt 41065 trigger 41067.
 * CMaNGOS CastCustomSpell with basePoints = damage. Dest/SQL 41067 later; Fireball 133 is the nested vehicle.
 */
class SpellEngineForceCastWithValueTest {
    @Test
    void applyForceCastWithValueWhenNestedDamageShouldUseParentBasePoints() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_FORCE_CAST_WITH_VALUE));
        Player target = new Player();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 1000);
        target.setHealth(1000);
        SpellEngine.SpellInfo bloodbolt = new SpellEngine.SpellInfo(
                41065, SpellEngine.EFFECT_FORCE_CAST_WITH_VALUE, 0, 0, 0, 525, 525, 0f, SpellEngine.FIREBALL);
        int dmg = eng.apply(new Creature(), target, bloodbolt);
        assertEquals(525, dmg);
        assertEquals(475, target.health());
    }

    @Test
    void applyForceCastWithValueWhenUnknownOrZeroValueShouldNoOpOrUseCatalog() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player target = new Player();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        target.setHealth(50);
        assertEquals(0, eng.apply(new Creature(), target, new SpellEngine.SpellInfo(
                41065, SpellEngine.EFFECT_FORCE_CAST_WITH_VALUE, 0, 0, 0, 525, 525, 0f, 41067)));
        assertEquals(50, target.health());
        int catalog = eng.apply(new Creature(), target, new SpellEngine.SpellInfo(
                41065, SpellEngine.EFFECT_FORCE_CAST_WITH_VALUE, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL));
        assertEquals(10, catalog);
        assertEquals(40, target.health());
        assertEquals(0, eng.forceCastWithValue(null, SpellEngine.FIREBALL, 525));
        assertEquals(0, eng.forceCastWithValue(target, 0, 525));
    }
}
