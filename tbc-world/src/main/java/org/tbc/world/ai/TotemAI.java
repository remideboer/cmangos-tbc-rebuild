package org.tbc.world.ai;

/** C++ TotemAI. No melee; totem spells are slice 18. */
public final class TotemAI implements UnitAI {
    @Override
    public String aiName() {
        return "TotemAI";
    }

    @Override
    public boolean meleeEnabled() {
        return false;
    }
}
