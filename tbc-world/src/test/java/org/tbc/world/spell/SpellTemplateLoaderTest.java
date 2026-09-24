package org.tbc.world.spell;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** DurationIndex seeds when SpellDuration.dbc is absent. */
class SpellTemplateLoaderTest {
    @Test
    @SuppressWarnings("unchecked")
    void durationsWhenNoDataDirShouldSeedFrostArmorAndDrainLife() throws Exception {
        Method m = SpellTemplateLoader.class.getDeclaredMethod("durations", Path.class);
        m.setAccessible(true);
        Map<Integer, Integer> d = (Map<Integer, Integer>) m.invoke(null, (Path) null);
        assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, d.get(30));
        assertEquals(SpellEngine.DRAIN_LIFE_DURATION_MS, d.get(28));
    }
}
