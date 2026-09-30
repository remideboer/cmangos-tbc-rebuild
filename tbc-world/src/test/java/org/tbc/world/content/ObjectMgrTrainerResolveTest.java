package org.tbc.world.content;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.session.TrainerHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trainer gossip/list requires spells (entry or TrainerTemplateId) and matching TrainerType/Class.
 * Falconwing Noellene 16275: TrainerClass 2, TrainerTemplateId 22 — not entry-keyed npc_trainer.
 */
class ObjectMgrTrainerResolveTest {
    @Test
    void isTrainerOfWhenClassTrainerUsesTemplateSpellsShouldOfferTrainToMatchingClass() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(16275, new ObjectMgr.CreatureTemplate(
                16275, "Noellene", 0, 1604, 100, 60,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        mgr.trainerTypeByEntry.put(16275, TrainerHandler.TRAINER_TYPE_CLASS);
        mgr.trainerClass.put(16275, 2);
        mgr.trainerTemplateId.put(16275, 22);
        mgr.trainerTemplateSpells.put(22, new ArrayList<>(List.of(
                new ObjectMgr.TrainerSpell(21084, 10, 1))));

        Player paladin = new Player();
        paladin.clazz = 2;
        Creature npc = new Creature();
        npc.entry = 16275;
        npc.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;

        assertTrue(mgr.isTrainerOf(paladin, npc));
        assertFalse(mgr.spellsForTrainer(16275).isEmpty());

        Player warrior = new Player();
        warrior.clazz = 1;
        assertFalse(mgr.isTrainerOf(warrior, npc));
    }

    @Test
    void isTrainerOfWhenEntryHasNoSpellsAndNoTemplateShouldRefuse() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(99999, new ObjectMgr.CreatureTemplate(
                99999, "Empty", 0, 12, 100, 5,
                Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        mgr.trainerClass.put(99999, 1);
        Player p = new Player();
        p.clazz = 1;
        Creature npc = new Creature();
        npc.entry = 99999;
        npc.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;
        assertFalse(mgr.isTrainerOf(p, npc));
    }

    @Test
    void isTrainerOfWhenTradeskillTrainerShouldOfferToAnyClass() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player mage = new Player();
        mage.clazz = 8;
        Creature dane = new Creature();
        dane.entry = Content.NPC_DANE_LINDGREN;
        dane.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;
        assertTrue(mgr.isTrainerOf(mage, dane));
    }
}
