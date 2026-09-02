package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;

public final class GameObject extends Entity {
    public static final int TYPEID_GAMEOBJECT = 5;
    public int entry;
    public int type;
    public int displayId;
    public String name = "";
    public String scriptName = "";
    public int state = 1;

    public GameObject() {
        super(UpdateFields.GAMEOBJECT_END, TYPEID_GAMEOBJECT);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, 0x21);
    }
}
