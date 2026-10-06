package org.tbc.world.companion;

import org.tbc.world.session.PetHandler;

/** Pet action-bar layout helpers for companions. */
final class PetHandlerBar {
    static final int MAX = PetHandler.MAX_ACTION_BAR;
    /** Pet spell slots 3..6 (CMaNGOS CharmInfo). */
    static final int SPELL_SLOT_START = 3;
    static final int SPELL_SLOT_END = 7;
    static final int ACT_COMMAND = PetHandler.ACT_COMMAND;
    static final int ACT_REACTION = PetHandler.ACT_REACTION;
    static final int ACT_ENABLED = PetHandler.ACT_ENABLED;

    private PetHandlerBar() {}

    static void seedDefaults(int[] bar) {
        // Stay / Follow / Attack / Dismiss — command slots 0..3
        bar[0] = 1 | (ACT_COMMAND << 24);
        bar[1] = 2 | (ACT_COMMAND << 24);
        bar[2] = 3 | (ACT_COMMAND << 24);
        // Passiveive / Defensive / Aggressive — reaction 7..9
        bar[7] = 0 | (ACT_REACTION << 24);
        bar[8] = 1 | (ACT_REACTION << 24);
        bar[9] = 2 | (ACT_REACTION << 24);
    }
}
