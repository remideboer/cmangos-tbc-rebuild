package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-078 — SPELL_EFFECT_ACTIVATE_OBJECT (86). Blow Zul'Farrak Door 11195 misc 12.
 * CMaNGOS GameObjectActions::DESTROY → UseDoorOrButton alternative (GO_STATE_ACTIVE_ALTERNATIVE).
 */
class SpellEngineActivateObjectTest {
    @Test
    void applyActivateObjectWhenDestroyShouldSwitchDoorToAlternativeState() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ACTIVATE_OBJECT));
        Player caster = new Player();
        GameObject door = new GameObject();
        door.type = GameObjectUse.TYPE_DOOR;
        door.state = GameObjectUse.STATE_READY;
        caster.setSpellGameObjectTarget(door);
        SpellEngine.SpellInfo blow = new SpellEngine.SpellInfo(
                11195, SpellEngine.EFFECT_ACTIVATE_OBJECT, 0, 0, 0, 0, 0, 0f, 12);
        eng.apply(caster, caster, blow);
        assertEquals(GameObjectUse.STATE_ACTIVE_ALTERNATIVE, door.state);
        assertEquals(GameObjectUse.GO_FLAG_IN_USE, door.getInt(UpdateFields.GAMEOBJECT_FLAGS) & GameObjectUse.GO_FLAG_IN_USE);
    }

    @Test
    void applyActivateObjectWhenMissingGoOrNotReadyShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo blow = new SpellEngine.SpellInfo(
                11195, SpellEngine.EFFECT_ACTIVATE_OBJECT, 0, 0, 0, 0, 0, 0f, 12);
        eng.apply(caster, caster, blow);
        GameObject door = new GameObject();
        door.state = GameObjectUse.STATE_ACTIVE;
        caster.setSpellGameObjectTarget(door);
        eng.apply(caster, caster, blow);
        assertEquals(GameObjectUse.STATE_ACTIVE, door.state);
        door.state = GameObjectUse.STATE_READY;
        SpellEngine.SpellInfo open = new SpellEngine.SpellInfo(
                11195, SpellEngine.EFFECT_ACTIVATE_OBJECT, 0, 0, 0, 0, 0, 0f, 8);
        eng.apply(caster, caster, open);
        assertEquals(GameObjectUse.STATE_ACTIVE, door.state);
        door.state = GameObjectUse.STATE_READY;
        SpellEngine.SpellInfo unknown = new SpellEngine.SpellInfo(
                11195, SpellEngine.EFFECT_ACTIVATE_OBJECT, 0, 0, 0, 0, 0, 0f, 99);
        eng.apply(caster, caster, unknown);
        assertEquals(GameObjectUse.STATE_READY, door.state);
        eng.apply(new Creature(), caster, blow);
        eng.activateObject(null, 12);
        eng.activateObject(door, 12);
        assertEquals(GameObjectUse.STATE_ACTIVE_ALTERNATIVE, door.state);
    }
}
