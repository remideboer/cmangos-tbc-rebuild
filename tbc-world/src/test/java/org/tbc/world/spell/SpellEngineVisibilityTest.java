package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JaCoCo: SpellEngine visibilityUpdater / affectsVisibility / stealth+invis stack. */
class SpellEngineVisibilityTest {
    @Test
    void affectsVisibilityWhenNullOrOtherAuraShouldBeFalse() {
        assertFalse(SpellEngine.affectsVisibility(null));
        assertFalse(SpellEngine.affectsVisibility(new SpellEngine().info(SpellEngine.FIREBALL)));
    }

    @Test
    void unapplyAuraWhenUnknownSpellShouldNotNotifyVisibility() {
        AtomicInteger calls = new AtomicInteger();
        SpellEngine eng = new SpellEngine(SpellWiring.defaults().withVisibilityUpdater(u -> calls.incrementAndGet()));
        eng.unapplyAura(new Player(), 999_999);
        assertEquals(0, calls.get());
    }

    @Test
    void applyAndUnapplyStealthShouldNotifyVisibilityUpdater() {
        AtomicInteger calls = new AtomicInteger();
        SpellEngine eng = new SpellEngine(SpellWiring.defaults().withVisibilityUpdater(u -> calls.incrementAndGet()));
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        assertEquals(1, calls.get());
        eng.unapplyAura(p, SpellEngine.SPELL_STEALTH);
        assertEquals(2, calls.get());
        assertEquals(Unit.Visibility.ON, p.visibility());
    }

    @Test
    void unapplyStealthWhenInvisRemainsShouldKeepInvisibilityGroup() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_INVISIBILITY));
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        assertEquals(Unit.Visibility.GROUP_STEALTH, p.visibility());
        eng.unapplyAura(p, SpellEngine.SPELL_STEALTH);
        assertEquals(Unit.Visibility.GROUP_INVISIBILITY, p.visibility());
    }

    @Test
    void unapplyInvisWhenStealthRemainsShouldKeepStealthGroup() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        eng.apply(p, p, eng.info(SpellEngine.SPELL_INVISIBILITY));
        assertEquals(Unit.Visibility.GROUP_STEALTH, p.visibility());
        eng.unapplyAura(p, SpellEngine.SPELL_INVISIBILITY);
        assertEquals(Unit.Visibility.GROUP_STEALTH, p.visibility());
    }

    @Test
    void unapplyWhenStackedStealthShouldClearOnlyOnLast() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        eng.putTemplate(900_1784, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STEALTH,
                0, 0, 0, 0, 0f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        eng.apply(p, p, eng.info(900_1784));
        assertEquals(2, p.stealthAuraCount());
        eng.unapplyAura(p, SpellEngine.SPELL_STEALTH);
        assertEquals(Unit.Visibility.GROUP_STEALTH, p.visibility());
        assertEquals(1, p.stealthAuraCount());
        eng.unapplyAura(p, 900_1784);
        assertEquals(Unit.Visibility.ON, p.visibility());
    }

    @Test
    void unapplyAuraWhenNullTargetShouldNoOp() {
        new SpellEngine().unapplyAura(null, SpellEngine.SPELL_STEALTH);
    }

    @Test
    void unapplyWhenVisibilityExtraShouldNotifyEvenIfPrimaryDoesNot() {
        AtomicInteger calls = new AtomicInteger();
        SpellEngine eng = new SpellEngine(SpellWiring.defaults().withVisibilityUpdater(u -> calls.incrementAndGet()));
        eng.putTemplate(900_200, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                16, 0, 10, 10, 0f, 0, 0, 0, 30_000,
                SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STEALTH, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0);
        Player p = new Player();
        eng.apply(p, p, eng.info(900_200));
        calls.set(0);
        eng.unapplyAura(p, 900_200);
        assertEquals(1, calls.get());
    }

    @Test
    void unapplyWhenPrimaryVisibilityAndNonVisibilityExtraShouldShortCircuitOr() {
        AtomicInteger calls = new AtomicInteger();
        SpellEngine eng = new SpellEngine(SpellWiring.defaults().withVisibilityUpdater(u -> calls.incrementAndGet()));
        eng.putTemplate(900_201, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STEALTH,
                0, 0, 0, 0, 0f, 0, 0, 0, 30_000,
                SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE, 16, 0,
                5, 5, 0, 0, 0, 0, 0, 1, 0);
        Player p = new Player();
        eng.apply(p, p, eng.info(900_201));
        calls.set(0);
        eng.unapplyAura(p, 900_201);
        assertEquals(1, calls.get());
    }

    @Test
    void applyAreaAuraWhenStealthShouldNotifyVisibility() {
        AtomicInteger calls = new AtomicInteger();
        SpellEngine eng = new SpellEngine(SpellWiring.defaults().withVisibilityUpdater(u -> calls.incrementAndGet()));
        eng.putTemplate(900_202, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY, AuraEngine.SPELL_AURA_MOD_STEALTH,
                0, 0, 0, 0, 0f, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        Player p = new Player();
        p.setHealth(100);
        eng.apply(p, p, eng.info(900_202));
        assertEquals(1, calls.get());
        assertEquals(Unit.Visibility.GROUP_STEALTH, p.visibility());
    }
}
