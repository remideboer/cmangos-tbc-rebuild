package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

import java.util.Set;

/**
 * SPELL_AURA_* modifier catalog (CMaNGOS Aura::ApplyModifier → AuraHandler[auraName]).
 * SpellEngine records the aura on the unit; this type applies the named modifier.
 * spell-algorithms.md "Auras 0–261".
 */
public final class AuraEngine {
    public static final int SPELL_AURA_MOD_STUN = 12;
    public static final int SPELL_AURA_MOD_ROOT = 26;

    private static final Set<Integer> KNOWN_AURAS = Set.of(SPELL_AURA_MOD_STUN, SPELL_AURA_MOD_ROOT);

    public boolean knownAura(int aura) {
        return KNOWN_AURAS.contains(aura);
    }

    /** Apply the modifier named by {@code sp.aura} to {@code target}. Unknown auras mutate nothing. */
    public void apply(Unit target, SpellEngine.SpellInfo sp) {
        if (target == null || sp == null) {
            return;
        }
        if (sp.aura() == SPELL_AURA_MOD_STUN) {
            target.setStunned(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_ROOT) {
            modRoot(target);
        }
    }

    /**
     * Aura 26 — CMaNGOS HandleAuraModRoot → SetImmobilizedState(apply, stun=false) → SendMoveRoot(true).
     * Root has no UNIT_FIELD_FLAGS bit; the controlling player is told via SMSG_FORCE_MOVE_ROOT.
     */
    private static void modRoot(Unit target) {
        if (target instanceof Player p) {
            p.sendMoveRoot(true);
        }
    }
}
