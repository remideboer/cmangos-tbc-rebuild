package org.tbc.world.combat;

import org.tbc.world.entity.Unit;

import java.util.HashMap;
import java.util.Map;

/** Per-creature float threat per attacker. spec/05-domain/combat-and-threat.md */
public final class ThreatManager {
    private final Map<Long, Float> byGuid = new HashMap<>();

    public void add(Unit attacker, float threat) {
        byGuid.merge(attacker.guid, threat, Float::sum);
    }

    public float threatOf(Unit attacker) {
        return byGuid.getOrDefault(attacker.guid, 0f);
    }

    public void reset() {
        byGuid.clear();
    }
}
