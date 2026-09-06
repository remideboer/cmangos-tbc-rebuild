package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-028 — SPELL_EFFECT_QUEST_FAIL (147). Quest 783 A Threat Within. */
class SpellEngineQuestFailTest {
    @Test
    void applyQuestFailWhenQuestInLogShouldMarkFailed() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_QUEST_FAIL));
        Player p = new Player();
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_QUEST_FAIL, 0, 0, 0, 0, 0, 0f, Content.QUEST_A_THREAT_WITHIN);
        eng.apply(p, p, sp);
        assertEquals(Content.QUEST_A_THREAT_WITHIN, p.questLogId[0]);
        assertEquals(Content.QUEST_STATE_FAIL, p.questLogState[0]);
    }

    @Test
    void applyQuestFailWhenMissingOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_QUEST_FAIL, 0, 0, 0, 0, 0, 0f, Content.QUEST_A_THREAT_WITHIN);
        eng.apply(p, new Creature(), sp);
        assertEquals(0, p.questLogState[0]);
        Player other = new Player();
        eng.apply(p, other, sp);
        assertEquals(0, other.questLogState[0]);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_QUEST_FAIL, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(p, p, zero);
        assertEquals(0, p.questLogState[0]);
    }
}
