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
        assertEquals(TrainerHandler.TRAINER_SPELL_GRAY, TrainerService.state(p, t));
    }

    @Test
    void stateWhenMissingReqAbilityShouldBeRed() {
        Player p = new Player();
        p.level = 20;
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(Content.SPELL_BATTLE_SHOUT_RANK2, 500, 12, 0, 0,
                Content.SPELL_BATTLE_SHOUT, 0, 0, false);
        assertEquals(TrainerHandler.TRAINER_SPELL_RED, TrainerService.state(p, t));
    }

    @Test
    void stateWhenEligibleShouldBeGreen() {
        Player p = new Player();
        p.level = 1;
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(Content.SPELL_BATTLE_SHOUT, 200, 1);
        assertEquals(TrainerHandler.TRAINER_SPELL_GREEN, TrainerService.state(p, t));
    }

    @Test
    void stateWhenPrimaryProfessionCapReachedShouldBeRed() {
        Player p = new Player();
        p.level = 1;
        p.learnSkill(164, 1, 75, 1);
        p.learnSkill(165, 1, 75, 1);
        ObjectMgr.TrainerSpell t = new ObjectMgr.TrainerSpell(Content.SPELL_APPRENTICE_BLACKSMITH, 10, 1,
                0, 0, 0, 0, 0, true);
        assertEquals(TrainerHandler.TRAINER_SPELL_RED, TrainerService.state(p, t));
    }
}
