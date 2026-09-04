package org.tbc.world.ai;

/** C++ NullCreatureAI. UpdateAI empty; no melee. */
public final class NullCreatureAI implements UnitAI {
    @Override
    public String aiName() {
        return "NullAI";
    }

    @Override
    public boolean meleeEnabled() {
        return false;
    }
}
