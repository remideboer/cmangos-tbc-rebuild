package org.tbc.world.ai;

/** C++ GuardAI. Melee; MoveInLineOfSight via {@link UnitAI#updateOoc}. */
public final class GuardAI implements UnitAI {
    @Override
    public String aiName() {
        return "GuardAI";
    }
}
