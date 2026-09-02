package org.tbc.world.script;

/**
 * Slice 34 — class spell scripts from spec/05-domain/class-spell-scripts.md.
 * Warrior Execute 5308 → 20647 rage-scaled then zero rage.
 * Warlock UA dispel → 31117 on dispeller.
 */
public final class ClassScripts {
    public static final int SPELL_EXECUTE = 5308;
    public static final int SPELL_EXECUTE_DAMAGE = 20647;
    public static final int SPELL_UNSTABLE_AFFLICTION = 30108;
    public static final int SPELL_UA_SILENCE = 31117;

    public record ExecuteResult(int damageSpell, int bonus, int rageAfter) {}

    public static ExecuteResult warriorExecute(int rage) {
        int bonus = Math.max(0, rage) * 4;
        return new ExecuteResult(SPELL_EXECUTE_DAMAGE, bonus, 0);
    }

    public static int unstableAfflictionDispel() {
        return SPELL_UA_SILENCE;
    }

    public static String key(int spellId) {
        return switch (spellId) {
            case SPELL_EXECUTE -> "spell_warrior_execute";
            case SPELL_EXECUTE_DAMAGE -> "spell_warrior_execute_damage";
            case SPELL_UNSTABLE_AFFLICTION -> "spell_unstable_affliction";
            default -> null;
        };
    }
}
