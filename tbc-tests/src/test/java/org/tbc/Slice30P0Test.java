package org.tbc;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.script.BossScript;
import org.tbc.world.script.ClassScripts;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL30-* from gruuls_lair.md / scripting-plugin-contract.md */
class Slice30P0Test {
    @Test
    void tpSl30GruulGrowth() {
        World w = World.inMemory();
        ScriptRegistry reg = w.scripts;
        assertTrue(reg.knows("boss_gruul"));
        BossScript gruul = reg.create("boss_gruul");
        gruul.aggro();
        List<Integer> casts = new ArrayList<>();
        gruul.update(new Creature(), new Player(), 30_000, (c, t, id) -> casts.add(id));
        assertTrue(casts.contains(36300));
        assertEquals(ClassScripts.SPELL_EXECUTE_DAMAGE, ClassScripts.warriorExecute(1).damageSpell());
        assertEquals("spell_warrior_execute", ClassScripts.key(5308));
        assertEquals("spell_unstable_affliction", ClassScripts.key(30108));
    }

    @Test
    void tpSl30MissingScriptFallback() {
        World w = World.inMemory();
        assertNull(w.scripts.create("missing_script_name_not_in_spec"));
    }
}
