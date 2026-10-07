package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.content.SkillLineAbility;
import org.tbc.world.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Constructor injection for SpellEngine collaborators (refactoring plan cycle 3.3). */
class SpellWiringTest {
    @Test
    void defaultsShouldBeDomainUnitNoOpsWithRandomSkillRolls() {
        SpellWiring w = SpellWiring.defaults();
        w.visibilityUpdater().accept(new Player());
        assertNotNull(w.skillLineAbilities());
        assertNull(w.objectMgr());
        for (int i = 0; i < 50; i++) {
            int craft = w.craftSkillRoll().getAsInt();
            int gather = w.gatherSkillRoll().getAsInt();
            assertTrue(craft >= 1 && craft <= 1000, "irand(1,1000) craft: " + craft);
            assertTrue(gather >= 1 && gather <= 1000, "irand(1,1000) gather: " + gather);
        }
    }

    @Test
    void withersShouldReplaceOnlyTheNamedCollaborator() {
        AtomicInteger calls = new AtomicInteger();
        ObjectMgr mgr = new ObjectMgr();
        SkillLineAbility sla = SkillLineAbility.seeded();
        SpellWiring w = SpellWiring.defaults()
                .withVisibilityUpdater(u -> calls.incrementAndGet())
                .withObjectMgr(mgr)
                .withSkillLineAbilities(sla);
        w.visibilityUpdater().accept(new Player());
        assertEquals(1, calls.get());
        assertSame(mgr, w.objectMgr());
        assertSame(sla, w.skillLineAbilities());
    }

    @Test
    void alwaysSucceedSkillRollsShouldReturnOneForBothRolls() {
        SpellWiring w = SpellWiring.defaults().alwaysSucceedSkillRolls();
        assertEquals(1, w.craftSkillRoll().getAsInt());
        assertEquals(1, w.gatherSkillRoll().getAsInt());
    }

    @Test
    void engineBuiltWithWiringShouldNotifyTheInjectedVisibilityUpdater() {
        AtomicInteger calls = new AtomicInteger();
        SpellEngine eng = new SpellEngine(SpellWiring.defaults().withVisibilityUpdater(u -> calls.incrementAndGet()));
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        assertEquals(1, calls.get());
    }
}
