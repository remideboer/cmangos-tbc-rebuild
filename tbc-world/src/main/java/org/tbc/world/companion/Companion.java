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
}
