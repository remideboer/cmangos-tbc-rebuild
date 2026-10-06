package org.tbc.world.companion;

import org.tbc.world.session.PetHandler;

/** Pet action-bar layout helpers for companions (CMaNGOS CharmInfo::InitPetActionBar). */
final class PetHandlerBar {
    static final int MAX = PetHandler.MAX_ACTION_BAR;
    /** Pet spell slots 3..6 (CMaNGOS CharmInfo). */
    static final int SPELL_SLOT_START = 3;
    static final int SPELL_SLOT_END = 7;
    static final int ACT_COMMAND = PetHandler.ACT_COMMAND;
    static final int ACT_REACTION = PetHandler.ACT_REACTION;
    static final int ACT_ENABLED = PetHandler.ACT_ENABLED;
    static final int ACT_DISABLED = PetHandler.ACT_DISABLED;
    /** Unit.h CommandStates — Attack / Follow / Stay. */
    static final int COMMAND_STAY = PetHandler.COMMAND_STAY;
    static final int COMMAND_FOLLOW = PetHandler.COMMAND_FOLLOW;
    static final int COMMAND_ATTACK = PetHandler.COMMAND_ATTACK;
    /** Unit.h ReactStates — Passiveive / Defensive / Aggressive. */
    static final int REACT_PASSIVE = PetHandler.REACT_PASSIVE;
    static final int REACT_DEFENSIVE = PetHandler.REACT_DEFENSIVE;
    static final int REACT_AGGRESSIVE = PetHandler.REACT_AGGRESSIVE;

    private PetHandlerBar() {}

    /**
     * CMaNGOS Unit.cpp InitPetActionBar: Attack/Follow/Stay; slots 3–6 ACT_DISABLED;
     * Aggressive/Defensive/Passive. No Dismiss on the default bar.
     */
    static void seedDefaults(int[] bar) {
        for (int i = 0; i < 3; i++) {
            bar[i] = (COMMAND_ATTACK - i) | (ACT_COMMAND << 24);
        }
        for (int i = SPELL_SLOT_START; i < SPELL_SLOT_END; i++) {
            bar[i] = ACT_DISABLED << 24;
        }
        for (int i = 0; i < 3; i++) {
            bar[7 + i] = (REACT_AGGRESSIVE - i) | (ACT_REACTION << 24);
        }
    }
}
