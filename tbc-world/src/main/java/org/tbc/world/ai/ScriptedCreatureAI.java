package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.script.BossScript;

/** ScriptName factory hit. Boss timers + melee. */
public final class ScriptedCreatureAI implements UnitAI {
    private final BossScript script;

    public ScriptedCreatureAI(BossScript script) {
        this.script = script;
    }

    @Override
    public String aiName() {
        return "ScriptedAI";
    }

    @Override
    public void update(Creature c, Player victim, int diffMs, EventAi.SpellCast cast, Runnable evade) {
        if (script == null || !c.inCombat) {
            return;
        }
        script.update(c, victim, diffMs, (cr, t, id) -> {
            if (cast != null) {
                cast.cast(cr, t, id);
            }
        });
    }
}
