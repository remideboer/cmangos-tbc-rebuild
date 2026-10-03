package org.tbc.world.content;

import org.tbc.world.classless.ClasslessConfig;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.session.TrainerHandler;
import org.junit.jupiter.api.BeforeEach;
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
    private static final int GOSSIP_OPTION_TRAINER = 5;
    /** Julia Sunstrider GossipMenuId — Train row gated by Player ClassMask mage (condition 18). */
    private static final int MENU_JULIA_MAGE = 6648;
    private static final int CONDITION_PLAYER_CLASSMASK_MAGE = 18;

    @BeforeEach
    void resetClassless() {
        ClasslessConfig.reset();
    }

    @Test
    void gossipOptionsForWhenClasslessAndTrainHasClassmaskConditionShouldIncludeTrain() {
        ObjectMgr mgr = mageTrainerWithClassmaskGossip();
        Player classless = new Player();
        classless.clazz = ClasslessConfig.CLASS_CLASSLESS;
        Creature npc = mageNpc();

        List<ObjectMgr.GossipMenuItem> opts = mgr.gossipOptionsFor(classless, npc, MENU_JULIA_MAGE);
        assertTrue(opts.stream().anyMatch(o -> o.optionId() == GOSSIP_OPTION_TRAINER),
                "TP-SL35-009: classless must see Train despite classmask gossip condition");
    }

    @Test
    void gossipOptionsForWhenWarriorAndTrainHasClassmaskConditionShouldExcludeTrain() {
        ObjectMgr mgr = mageTrainerWithClassmaskGossip();
        Player warrior = new Player();
        warrior.clazz = 1;
        Creature npc = mageNpc();

        List<ObjectMgr.GossipMenuItem> opts = mgr.gossipOptionsFor(warrior, npc, MENU_JULIA_MAGE);
        assertFalse(opts.stream().anyMatch(o -> o.optionId() == GOSSIP_OPTION_TRAINER));
    }

    @Test
    void isTrainerOfWhenClasslessAndPaladinShapedTrainerShouldOfferTrain() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(16275, new ObjectMgr.CreatureTemplate(
                16275, "Noellene", 0, 1604, 100, 60,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        mgr.trainerTypeByEntry.put(16275, TrainerHandler.TRAINER_TYPE_CLASS);
        mgr.trainerClass.put(16275, 2);
        mgr.trainerTemplateId.put(16275, 22);
        mgr.trainerTemplateSpells.put(22, new ArrayList<>(List.of(
                new ObjectMgr.TrainerSpell(21084, 10, 1))));

        Player classless = new Player();
        classless.clazz = ClasslessConfig.CLASS_CLASSLESS;
        Creature npc = new Creature();
        npc.entry = 16275;
        npc.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;

        assertFalse(mgr.isTrainerOf(classless, npc), "Paladin trainer locked until 90005");
        classless.rewardedQuests.add(org.tbc.world.classless.HeroClassUnlock.QUEST_A_VOW_TESTED);
        assertTrue(mgr.isTrainerOf(classless, npc));
    }

    private static ObjectMgr mageTrainerWithClassmaskGossip() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(15297, new ObjectMgr.CreatureTemplate(
                15297, "Julia Sunstrider", 0, 1604, 100, 60,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER, "", "", 0));
        mgr.trainerTypeByEntry.put(15297, TrainerHandler.TRAINER_TYPE_CLASS);
        mgr.trainerClass.put(15297, 8);
        mgr.trainerSpells.put(15297, new ArrayList<>(List.of(
                new ObjectMgr.TrainerSpell(133, 10, 1))));
        mgr.gossipOptions.put(MENU_JULIA_MAGE, new ArrayList<>(List.of(
                new ObjectMgr.GossipMenuItem(
                        MENU_JULIA_MAGE, 0, Content.GOSSIP_ICON_TRAINER, "Train",
                        GOSSIP_OPTION_TRAINER, Content.UNIT_NPC_FLAG_TRAINER,
                        0, 0, "", 0, 0, CONDITION_PLAYER_CLASSMASK_MAGE))));
        return mgr;
    }

    private static Creature mageNpc() {
        Creature npc = new Creature();
        npc.entry = 15297;
        npc.npcFlags = Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_TRAINER;
        return npc;
    }

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
