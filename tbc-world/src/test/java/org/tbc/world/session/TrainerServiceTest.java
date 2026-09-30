package org.tbc.world.session;

import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** TP-SL14-018 — GetTrainerSpellState green/red/gray. */
class TrainerServiceTest {
    @Test
    void stateWhenKnownSpellShouldBeGray() {
        Player p = new Player();
        p.level = 10;
        p.spells.add(Content.SPELL_BATTLE_SHOUT);
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(Content.SPELL_BATTLE_SHOUT, 200, 1);
        assertEquals(TrainerHandler.TRAINER_SPELL_GRAY, TrainerService.state(p, t, null));
    }

    @Test
    void stateWhenMissingReqAbilityShouldBeRed() {
        Player p = new Player();
        p.level = 20;
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(Content.SPELL_BATTLE_SHOUT_RANK2, 500, 12, 0, 0,
                Content.SPELL_BATTLE_SHOUT, 0, 0, false);
        assertEquals(TrainerHandler.TRAINER_SPELL_RED, TrainerService.state(p, t, null));
    }

    @Test
    void stateWhenBelowReqLevelShouldBeRed() {
        Player p = new Player();
        p.level = 6;
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(20287, 100, 10);
        assertEquals(TrainerHandler.TRAINER_SPELL_RED, TrainerService.state(p, t, null));
    }

    @Test
    void stateWhenMissingSpellChainPrevShouldBeRed() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.spellChain.put(639, new ObjectMgr.SpellChainNode(639, 635, 635, 2, 0));
        Player p = new Player();
        p.level = 6;
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(639, 2000, 6);
        assertEquals(TrainerHandler.TRAINER_SPELL_RED, TrainerService.state(p, t, mgr));
        p.spells.add(635);
        assertEquals(TrainerHandler.TRAINER_SPELL_GREEN, TrainerService.state(p, t, mgr));
    }

    @Test
    void stateWhenEligibleShouldBeGreen() {
        Player p = new Player();
        p.level = 1;
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(Content.SPELL_BATTLE_SHOUT, 200, 1);
        assertEquals(TrainerHandler.TRAINER_SPELL_GREEN, TrainerService.state(p, t, null));
    }

    @Test
    void stateWhenPrimaryProfessionCapReachedShouldBeRed() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(164, 1, 75, 1);
        p.learnSkill(165, 1, 75, 1);
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(Content.SPELL_APPRENTICE_BLACKSMITH, 10, 1,
                0, 0, 0, 0, 0, true);
        assertEquals(TrainerHandler.TRAINER_SPELL_RED, TrainerService.state(p, t, null));
    }
}
