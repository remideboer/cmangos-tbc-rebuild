package org.tbc.world.companion;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PetHandler;
import org.tbc.world.world.World;

/**
 * Follow owner; assist owner's victim; cast first enabled bar spell or melee.
 * Class-specific strategies can plug in later without touching packet code.
 */
public final class CompanionBehavior {
    private CompanionBehavior() {}

    public static void tick(World world, Player owner) {
        if (owner == null || owner.companion == null || owner.pet == null || !owner.pet.alive) {
            return;
        }
        Unit body = owner.pet.asUnit();
        body.x = owner.x;
        body.y = owner.y;
        body.z = owner.z;
        body.mapId = owner.mapId;
        long target = owner.victim != 0 ? owner.victim : owner.pet.victim;
        if (target == 0) {
            return;
        }
        Creature prey = world.map(owner.mapId, owner.instanceId).creatures.get(target);
        if (prey == null || !prey.alive()) {
            owner.pet.victim = 0;
            return;
        }
        owner.pet.victim = target;
        // MVP assist: auto-attack the owner's target. Owner still casts via CMSG_PET_CAST_SPELL.
        int dmg = Math.max(1, (int) body.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE));
        prey.setHealth(Math.max(0, prey.health() - dmg));
    }

    /** First enabled spell on the companion pet bar (slots 3–6). */
    public static int firstEnabledSpell(org.tbc.world.entity.Pet pet) {
        for (int i = PetHandlerBar.SPELL_SLOT_START; i < PetHandlerBar.SPELL_SLOT_END; i++) {
            int packed = pet.actionBar[i];
            int act = (packed >>> 24) & 0xFF;
            int id = packed & 0xFFFFFF;
            if (id != 0 && act == PetHandler.ACT_ENABLED) {
                return id;
            }
        }
        return 0;
    }
}
