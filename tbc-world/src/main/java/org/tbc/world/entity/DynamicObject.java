package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;

/** TYPEID 6. CMaNGOS DynamicObject — persistent area aura / farsight focus. */
public final class DynamicObject extends Entity {
    public static final int TYPEID_DYNAMICOBJECT = 6;
    /** TYPEMASK_OBJECT | TYPEMASK_DYNAMICOBJECT */
    public static final int TYPEMASK_DYNAMICOBJECT = 0x0041;
    /** CMaNGOS DYNAMIC_OBJECT_AREA_SPELL */
    public static final int DYNAMIC_OBJECT_AREA_SPELL = 0x1;

    public int spellId;
    public float radius;

    public DynamicObject() {
        super(UpdateFields.DYNAMICOBJECT_END, TYPEID_DYNAMICOBJECT);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, TYPEMASK_DYNAMICOBJECT);
    }
}
