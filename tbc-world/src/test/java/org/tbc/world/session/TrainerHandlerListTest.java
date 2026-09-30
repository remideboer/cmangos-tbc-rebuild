package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL14-017 — SMSG_TRAINER_LIST encodes req fields and professionFirstRank. */
class TrainerHandlerListTest {
    @Test
    void encodeListWhenLlaneShouldIncludeReqAbilityOnRank2AndGreenOnRank1() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.clazz = 1;
        p.level = 1;
        Creature c = new Creature();
        c.guid = 1;
        c.entry = Content.NPC_LLANE_BESHERE;
        c.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;
        byte[] payload = TrainerHandler.encodeList(p, c, mgr);
        assertNotNull(payload);
        WowBuffer b = new WowBuffer(payload);
        assertEquals(1L, b.getU64());
        assertEquals(0, b.getU32());
        int count = b.getU32();
        assertTrue(count >= 2);
        assertEquals(Content.SPELL_BATTLE_SHOUT, b.getU32());
        assertEquals(TrainerHandler.TRAINER_SPELL_GREEN, b.getU8());
        assertEquals(Content.TRAINER_SPELL_BATTLE_SHOUT_COST, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(1, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(Content.SPELL_BATTLE_SHOUT_RANK2, b.getU32());
        assertEquals(TrainerHandler.TRAINER_SPELL_RED, b.getU8());
        b.getU32();
        b.getU32();
        b.getU32();
        b.getU8();
        b.getU32();
        b.getU32();
        assertEquals(Content.SPELL_BATTLE_SHOUT, b.getU32());
    }

    @Test
    void encodeListWhenBlacksmithTrainerShouldSetProfessionFirstRank() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.clazz = 1;
        p.level = 1;
        Creature c = new Creature();
        c.guid = 2;
        c.entry = Content.NPC_DANE_LINDGREN;
        c.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;
        byte[] payload = TrainerHandler.encodeList(p, c, mgr);
        assertNotNull(payload);
        WowBuffer b = new WowBuffer(payload);
        b.getU64();
        assertEquals(TrainerHandler.TRAINER_TYPE_TRADESKILLS, b.getU32());
        assertEquals(1, b.getU32());
        assertEquals(Content.SPELL_APPRENTICE_BLACKSMITH, b.getU32());
        assertEquals(TrainerHandler.TRAINER_SPELL_GREEN, b.getU8());
        assertEquals(Content.TRAINER_SPELL_APPRENTICE_BLACKSMITH_COST, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(1, b.getU32());
    }
}
