package org.tbc.world.ai;

/** C++ GuardAI. Melee in combat; no MoveInLineOfSight. */
public final class GuardAI implements UnitAI {
    @Override
    public String aiName() {
        return "GuardAI";
    }
}
