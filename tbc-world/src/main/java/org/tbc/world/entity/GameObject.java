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
    /** MO transport path progress ms (movement.md); period from DB when loaded. */
    public int pathProgress;
    public int periodMs;
    /** Chest / interactable loot (FillLoot for gameobject_loot_template). */
    public int lootGold;
    public boolean lootable;
    /** gameobject_template data0 when type is SPELL_FOCUS (SpellFocusObject.dbc). */
    public int spellFocusId;
    /** gameobject_template data1 focus radius; 0 → interact range. */
    public int spellFocusDist;
    public final java.util.List<org.tbc.world.loot.LootSlot> lootItems = new java.util.ArrayList<>();

    public GameObject() {
        super(UpdateFields.GAMEOBJECT_END, TYPEID_GAMEOBJECT);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, 0x21);
    }
}
