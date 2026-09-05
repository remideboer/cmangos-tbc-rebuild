package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.script.ScriptRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactorySelectorTest {
    private final ScriptRegistry scripts = new ScriptRegistry();

    @Test
    void selectAiWhenScriptNameBossGruulShouldReturnScripted() {
        Creature c = creature();
        c.scriptName = "boss_gruul";
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("ScriptedAI", ai.aiName());
        assertTrue(ai.meleeEnabled());
    }

    @Test
    void selectAiWhenMissingScriptNameShouldFallThroughWithoutCrash() {
        Creature c = creature();
        c.scriptName = "missing_script_name_not_in_spec";
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("EventAI", ai.aiName());
    }

    @Test
    void selectAiWhenAiNameEventAiShouldReturnEventAi() {
        Creature c = creature();
        c.aiName = "EventAI";
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("EventAI", ai.aiName());
        assertNotNull(c.eventAi);
        assertTrue(ai.meleeEnabled());
    }

    @Test
    void selectAiWhenEmptyAiNameHostileShouldReturnEventAiFromPermit() {
        Creature c = creature();
        assertEquals("EventAI", FactorySelector.selectAI(c, scripts).aiName());
        assertNotNull(c.eventAi);
    }

    @Test
    void selectAiWhenUnknownAiNameShouldReturnNullAi() {
        Creature c = creature();
        c.aiName = "NotARealAI";
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("NullAI", ai.aiName());
        assertFalse(ai.meleeEnabled());
    }

    @Test
    void selectAiWhenPlayerControlledPetShouldReturnPetAi() {
        Creature c = creature();
        c.pet = true;
        c.playerControlledPet = true;
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("PetAI", ai.aiName());
        assertTrue(ai.meleeEnabled());
    }

    @Test
    void selectAiWhenUncontrolledPetShouldReturnGuardianAi() {
        Creature c = creature();
        c.pet = true;
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("GuardianAI", ai.aiName());
        assertTrue(ai.meleeEnabled());
    }

    @Test
    void selectAiWhenCharmerNotTempspawnShouldReturnPetAi() {
        Creature c = creature();
        c.charmer = true;
        assertEquals("PetAI", FactorySelector.selectAI(c, scripts).aiName());
    }

    @Test
    void selectAiWhenTotemShouldReturnTotemAi() {
        Creature c = creature();
        c.totem = true;
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("TotemAI", ai.aiName());
        assertFalse(ai.meleeEnabled());
    }

    @Test
    void selectAiWhenGuardExtraFlagShouldReturnGuardAi() {
        Creature c = creature();
        c.extraFlags = Creature.CREATURE_EXTRA_FLAG_GUARD;
        UnitAI ai = FactorySelector.selectAI(c, scripts);
        assertEquals("GuardAI", ai.aiName());
        assertTrue(ai.meleeEnabled());
    }

    @Test
    void selectAiWhenNoAggroOnSightShouldReturnEventAiNotNull() {
        Creature c = creature();
        c.extraFlags = Creature.CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT;
        assertEquals("EventAI", FactorySelector.selectAI(c, scripts).aiName());
        assertNotNull(c.eventAi);
    }

    @Test
    void selectAiWhenNeutralToAllShouldReturnEventAiNotNull() {
        Creature c = creature();
        c.neutralToAll = true;
        assertEquals("EventAI", FactorySelector.selectAI(c, scripts).aiName());
        assertNotNull(c.eventAi);
    }

    private static Creature creature() {
        Creature c = new Creature();
        c.guid = 2;
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        return c;
    }
}
