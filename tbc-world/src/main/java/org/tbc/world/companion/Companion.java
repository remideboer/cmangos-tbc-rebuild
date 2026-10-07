package org.tbc.world.companion;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;

/**
 * Runtime companion: offline character snapshot driven through the owner's pet protocol.
 * No WorldSession on the snapshot. World body is a map Creature near the owner.
 */
public final class Companion {
    private final long sourceGuid;
    private final Player snapshot;
    private final Pet pet;
    private final int[] companionBar = new int[PetHandlerBar.MAX];
    private Creature worldBody;
    /** Last position sent via SMSG_MONSTER_MOVE (follow throttle). */
    public float lastBroadcastX;
    public float lastBroadcastY;
    public float lastBroadcastZ;
    public boolean hasBroadcastPos;
    /** Global GCD-style gate for companion auto-cast (ms remaining). */
    public int castCooldownMs;
    /** Near-death escape: already launched flee motion. */
    public boolean fleeing;
    /** Pending non-instant cast (CMaNGOS PetAI SpellStart with real cast time). */
    private int pendingSpellId;
    private long pendingTargetGuid;
    private int pendingRemainMs;
    private int pendingManaCost;
    private int requestedSpellId;
    private long requestedSpellTarget;

    public Companion(long sourceGuid, Player snapshot, Pet pet) {
        this.sourceGuid = sourceGuid;
        this.snapshot = snapshot;
        this.pet = pet;
    }

    public long sourceGuid() {
        return sourceGuid;
    }

    public Player snapshot() {
        return snapshot;
    }

    public Pet pet() {
        return pet;
    }

    public Creature worldBody() {
        return worldBody;
    }

    public void setWorldBody(Creature body) {
        this.worldBody = body;
    }

    public int[] companionBar() {
        return companionBar;
    }

    public void syncBarFromPet() {
        System.arraycopy(pet.actionBar, 0, companionBar, 0, companionBar.length);
    }

    public void applyBarToPet() {
        System.arraycopy(companionBar, 0, pet.actionBar, 0, companionBar.length);
    }

    public void requestSpell(int spellId, long targetGuid) {
        requestedSpellId = spellId;
        requestedSpellTarget = targetGuid;
    }

    public int requestedSpellId() {
        return requestedSpellId;
    }

    public long requestedSpellTarget() {
        return requestedSpellTarget;
    }

    public void clearRequestedSpell() {
        requestedSpellId = 0;
        requestedSpellTarget = 0;
    }

    public boolean isCasting() {
        return pendingSpellId != 0 && pendingRemainMs > 0;
    }

    public int pendingSpellId() {
        return pendingSpellId;
    }

    public long pendingTargetGuid() {
        return pendingTargetGuid;
    }

    public int pendingRemainMs() {
        return pendingRemainMs;
    }

    public int pendingManaCost() {
        return pendingManaCost;
    }

    public void beginCast(int spellId, long targetGuid, int castTimeMs, int manaCost) {
        pendingSpellId = spellId;
        pendingTargetGuid = targetGuid;
        pendingRemainMs = Math.max(0, castTimeMs);
        pendingManaCost = Math.max(0, manaCost);
    }

    public void advanceCast(int diff) {
        if (pendingRemainMs > 0) {
            pendingRemainMs = Math.max(0, pendingRemainMs - Math.max(0, diff));
        }
    }

    public void clearPendingCast() {
        pendingSpellId = 0;
        pendingTargetGuid = 0;
        pendingRemainMs = 0;
        pendingManaCost = 0;
    }
}
