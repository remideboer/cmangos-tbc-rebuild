package org.tbc.world.spell;

import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.Set;

/**
 * SPELL_AURA_* modifier catalog (CMaNGOS Aura::ApplyModifier → AuraHandler[auraName]).
 * SpellEngine records the aura on the unit; this type applies the named modifier.
 * spell-algorithms.md "Auras 0–261".
 */
public final class AuraEngine {
    public static final int SPELL_AURA_MOD_STUN = 12;
    public static final int SPELL_AURA_MOD_RESISTANCE = 22;
    public static final int SPELL_AURA_MOD_ROOT = 26;
    public static final int SPELL_AURA_MOD_SHAPESHIFT = 36;
    /** SpellSchools.h MAX_SPELL_SCHOOL — normal through arcane. */
    public static final int MAX_SPELL_SCHOOL = 7;

    private static final Set<Integer> KNOWN_AURAS = Set.of(
            SPELL_AURA_MOD_STUN, SPELL_AURA_MOD_RESISTANCE, SPELL_AURA_MOD_ROOT, SPELL_AURA_MOD_SHAPESHIFT);

    public boolean knownAura(int aura) {
        return KNOWN_AURAS.contains(aura);
    }

    /** Apply the modifier named by {@code sp.aura} to {@code target}. Unknown auras mutate nothing. */
    public void apply(Unit target, SpellEngine.SpellInfo sp) {
        if (target == null || sp == null) {
            return;
        }
        if (sp.aura() == SPELL_AURA_MOD_STUN) {
            modStun(target);
        }
        if (sp.aura() == SPELL_AURA_MOD_ROOT) {
            immobilize(target);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHAPESHIFT) {
            target.setShapeshiftForm(sp.misc());
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE) {
            modResistance(target, sp, true);
        }
    }

    /** Reverse {@link #apply} for auras that mutate stats (CMaNGOS Aura::ApplyModifier(false)). */
    public void unapply(Unit target, SpellEngine.SpellInfo sp) {
        if (target == null || sp == null) {
            return;
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE) {
            modResistance(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STUN) {
            // HandleAuraModStun(false) → SetStunned(false) → clear flag + SetImmobilizedState(false).
            // Stacking other MOD_STUN auras later (HasAuraType check).
            target.setStunned(false);
            target.sendMoveRoot(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_ROOT) {
            // HandleAuraModRoot(false) → SetImmobilizedState(false); stacking other roots later.
            target.sendMoveRoot(false);
        }
    }

    /**
     * Aura 22 — CMaNGOS HandleAuraModResistance: school bits in EffectMiscValue, TOTAL_VALUE amount,
     * then ApplyResistanceBuffModsMod for the character sheet bonus/malus columns.
     */
    private static void modResistance(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            int delta = apply ? amount : -amount;
            int resistField = UpdateFields.UNIT_FIELD_RESISTANCES + i;
            target.setInt(resistField, target.getInt(resistField) + delta);
            int buffBase = amount > 0
                    ? UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE
                    : UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE;
            int buffField = buffBase + i;
            target.setInt(buffField, target.getInt(buffField) + delta);
        }
    }

    /** Aura 12 — CMaNGOS SetStunned: SetImmobilizedState(stun=true) then UNIT_FLAG_STUNNED. */
    private static void modStun(Unit target) {
        immobilize(target);
        target.setStunned(true);
    }

    /**
     * CMaNGOS Unit::SetImmobilizedState → SendMoveRoot(true). Root itself has no UNIT_FIELD_FLAGS
     * bit; players get SMSG_FORCE_MOVE_ROOT, other units SMSG_SPLINE_MOVE_ROOT (Unit.cpp).
     */
    private static void immobilize(Unit target) {
        target.sendMoveRoot(true);
    }
}
