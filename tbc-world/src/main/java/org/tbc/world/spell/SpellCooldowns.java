package org.tbc.world.spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CMaNGOS WorldObject::m_GCDCatMap — global cooldown per Spell.dbc StartRecoveryCategory. */
public final class SpellCooldowns {
    /** Spell.dbc StartRecoveryCategory shared by almost every player spell. */
    public static final int GCD_CATEGORY_NORMAL = 133;
    /** Spell.dbc StartRecoveryTime for that category. */
    public static final int GCD_NORMAL_MS = 1500;

    private final Map<Integer, Long> gcdUntilMs = new HashMap<>();
    /** WorldObject::m_cooldownMap by spell id — expire time of RecoveryTime. */
    private final Map<Integer, Long> spellUntilMs = new HashMap<>();

    /** WorldObject::AddGCD: a zero duration adds nothing. */
    public void addGcd(int category, int durationMs, long nowMs) {
        if (durationMs <= 0) {
            return;
        }
        gcdUntilMs.put(category, nowMs + durationMs);
    }

    /** WorldObject::HasGCD for the spell's category. */
    public boolean hasGcd(int category, long nowMs) {
        Long until = gcdUntilMs.get(category);
        return until != null && until > nowMs;
    }

    /** Spell::cancel → ResetGCD: a cancelled cast gives the GCD back. */
    public void resetGcd(int category) {
        gcdUntilMs.remove(category);
    }

    /** Player::AddCooldown RecoveryTime for this spell id. Zero duration adds nothing. */
    public void addSpell(int spellId, int recoveryMs, long nowMs) {
        if (recoveryMs <= 0) {
            return;
        }
        spellUntilMs.put(spellId, nowMs + recoveryMs);
    }

    /** WorldObject::IsSpellReady for RecoveryTime (category CDs are a later increment). */
    public boolean isSpellReady(int spellId, long nowMs) {
        Long until = spellUntilMs.get(spellId);
        return until == null || until <= nowMs;
    }

    /**
     * PlayerPersist clone of m_cooldownMap. GCD is not persisted (CMaNGOS _SaveSpellCooldowns).
     */
    public void copyFrom(SpellCooldowns src) {
        spellUntilMs.clear();
        spellUntilMs.putAll(src.spellUntilMs);
    }

    /** Player::_LoadSpellCooldowns: skip rows whose SpellExpireTime is already past. */
    public void restoreSpell(int spellId, long expireMs, long nowMs) {
        if (expireMs > nowMs) {
            spellUntilMs.put(spellId, expireMs);
        }
    }

    /** Player::_SaveSpellCooldowns: spell id → expire epoch-ms for rows that have not elapsed. */
    public Map<Integer, Long> unexpiredSpellExpireMs(long nowMs) {
        Map<Integer, Long> out = new HashMap<>();
        for (Map.Entry<Integer, Long> e : spellUntilMs.entrySet()) {
            if (e.getValue() > nowMs) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /** Player::SendInitialSpells cooldown block — leftover ms, item/category 0 until those maps exist. */
    public List<RemainingSpell> remainingSpells(long nowMs) {
        List<RemainingSpell> out = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : unexpiredSpellExpireMs(nowMs).entrySet()) {
            out.add(new RemainingSpell(e.getKey(), 0, 0, (int) (e.getValue() - nowMs), 0));
        }
        return out;
    }

    public record RemainingSpell(int spellId, int itemId, int category, int remainMs, int catRemainMs) {}
}
