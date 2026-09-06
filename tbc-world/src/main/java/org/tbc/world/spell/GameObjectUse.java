package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.GameObject;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.function.BiConsumer;

/**
 * GO use / open-lock from spell-algorithms.md effects 33/86 and CMSG_GAMEOBJ_USE.
 * States: mangos GO_STATE_ACTIVE=0, READY=1.
 */
public final class GameObjectUse {
    public static final int TYPE_DOOR = 0;
    public static final int TYPE_CHEST = 3;
    /** movement.md GAMEOBJECT_TYPE_MO_TRANSPORT */
    public static final int TYPE_MO_TRANSPORT = 15;
    public static final int STATE_ACTIVE = 0;
    public static final int STATE_READY = 1;
    /** GameObject.h GO_STATE_ACTIVE_ALTERNATIVE — DESTROY / cannon. */
    public static final int STATE_ACTIVE_ALTERNATIVE = 2;
    /** SharedDefines.h GO_FLAG_IN_USE. */
    public static final int GO_FLAG_IN_USE = 0x00000001;
    /** GameObject.h GameObjectActions::OPEN / DESTROY. */
    public static final int ACTION_OPEN = 8;
    public static final int ACTION_DESTROY = 12;

    private GameObjectUse() {}

    /** Effect 33/86 / door click: READY → ACTIVE (open). */
    public static boolean openDoor(GameObject go) {
        if (go == null || go.type != TYPE_DOOR) {
            return false;
        }
        if (go.state == STATE_ACTIVE) {
            return false;
        }
        go.state = STATE_ACTIVE;
        return true;
    }

    public static boolean isChest(GameObject go) {
        return go != null && go.type == TYPE_CHEST;
    }

    /**
     * Effect 33 / 59 open lock — chest/door READY → ACTIVE (unlocked/open).
     * Returns true when state changed.
     */
    public static boolean openLock(GameObject go) {
        if (go == null || (go.type != TYPE_CHEST && go.type != TYPE_DOOR)) {
            return false;
        }
        if (go.state == STATE_ACTIVE) {
            return false;
        }
        go.state = STATE_ACTIVE;
        return true;
    }

    public static boolean isMoTransport(GameObject go) {
        return go != null && go.type == TYPE_MO_TRANSPORT;
    }

    /**
     * CMaNGOS GameObject::UseDoorOrButton — READY only; alternative is DESTROY.
     */
    public static boolean useDoorOrButton(GameObject go, boolean alternative) {
        if (go == null || go.state != STATE_READY) {
            return false;
        }
        go.setInt(UpdateFields.GAMEOBJECT_FLAGS,
                go.getInt(UpdateFields.GAMEOBJECT_FLAGS) | GO_FLAG_IN_USE);
        go.state = alternative ? STATE_ACTIVE_ALTERNATIVE : STATE_ACTIVE;
        return true;
    }

    public static void sendCustomAnim(BiConsumer<Integer, byte[]> send, long guid, int animId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(guid);
        b.putU32(animId);
        send.accept(Opcodes.SMSG_GAMEOBJECT_CUSTOM_ANIM, b.array());
    }
}
