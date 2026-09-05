package org.tbc.world.ai;

/** C++ GuardianAI. Melee in combat; no follow. */
public final class GuardianAI implements UnitAI {
    @Override
    public String aiName() {
        return "GuardianAI";
    }

    @Override
    public boolean aggroOnSight() {
        return false;
    }
}
