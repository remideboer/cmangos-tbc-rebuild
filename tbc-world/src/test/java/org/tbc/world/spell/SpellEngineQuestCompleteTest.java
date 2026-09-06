package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-018 — SPELL_EFFECT_QUEST_COMPLETE (16). Quest 783 A Threat Within. */
class SpellEngineQuestCompleteTest {
    @Test
    void applyQuestCompleteWhenQuestInLogShouldMarkComplete() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_QUEST_COMPLETE));
        Player p = new Player();
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        p.questLogState[0] = 0;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_QUEST_COMPLETE, 0, 0, 0, 0, 0, 0f, Content.QUEST_A_THREAT_WITHIN);
        eng.apply(p, p, sp);
        assertEquals(Content.QUEST_A_THREAT_WITHIN, p.questLogId[0]);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        eng.apply(p, p, sp);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
    }

    @Test
    void applyQuestCompleteWhenMissingOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_QUEST_COMPLETE, 0, 0, 0, 0, 0, 0f, Content.QUEST_A_THREAT_WITHIN);
        eng.apply(p, new Creature(), sp);
        assertEquals(0, p.questLogState[0]);
        Player other = new Player();
        eng.apply(p, other, sp);
        assertEquals(0, other.questLogState[0]);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_QUEST_COMPLETE, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(p, p, zero);
        assertEquals(0, p.questLogState[0]);
    }
}
