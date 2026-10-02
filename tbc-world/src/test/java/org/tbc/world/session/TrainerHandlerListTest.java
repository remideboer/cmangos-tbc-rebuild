package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.classless.ClasslessConfig;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * SQL-shaped Llane: spells only on npc_trainer_template id 10 (no direct entry rows).
     * Classless must still get the full template list on the wire.
     */
    @Test
    void encodeListWhenClasslessAndTemplateOnlyShouldListAllTemplateSpells() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        mgr.trainerSpells.remove(Content.NPC_LLANE_BESHERE);
        mgr.trainerTemplateId.put(Content.NPC_LLANE_BESHERE, 10);
        List<ObjectMgr.TrainerSpell> tmpl = new ArrayList<>();
        tmpl.add(new ObjectMgr.TrainerSpell(100, 100, 4));
        tmpl.add(new ObjectMgr.TrainerSpell(772, 100, 4));
        tmpl.add(new ObjectMgr.TrainerSpell(3127, 100, 1));
        tmpl.add(new ObjectMgr.TrainerSpell(6343, 100, 6));
        tmpl.add(new ObjectMgr.TrainerSpell(Content.SPELL_BATTLE_SHOUT, 10, 1));
        mgr.trainerTemplateSpells.put(10, tmpl);
        assertEquals(5, mgr.spellsForTrainer(Content.NPC_LLANE_BESHERE).size());

        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        p.race = 1;
        Creature c = new Creature();
        c.guid = 1;
        c.entry = Content.NPC_LLANE_BESHERE;
        c.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;
        byte[] payload = TrainerHandler.encodeList(p, c, mgr);
        assertNotNull(payload);
        WowBuffer b = new WowBuffer(payload);
        b.getU64();
        b.getU32();
        assertEquals(5, b.getU32());
    }

    @Test
    void encodeListWhenListedEmptyShouldStillSendCountZeroGreeting() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        // Force empty listed via SLA classmask reject for warrior on a mage-only seeded fit spell.
        mgr.skillLineAbilities.put(Content.SPELL_BATTLE_SHOUT, 26, 1, 1, 1 << (Player.CLASS_MAGE - 1), 0);
        Player p = new Player();
        p.clazz = 1;
        p.level = 1;
        p.race = 1;
        Creature c = new Creature();
        c.guid = 1;
        c.entry = Content.NPC_LLANE_BESHERE;
        c.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;
        // Keep only Battle Shout so SLA reject empties the list.
        mgr.trainerSpells.put(Content.NPC_LLANE_BESHERE, new ArrayList<>(List.of(
                new ObjectMgr.TrainerSpell(Content.SPELL_BATTLE_SHOUT, Content.TRAINER_SPELL_BATTLE_SHOUT_COST, 1))));
        byte[] payload = TrainerHandler.encodeList(p, c, mgr);
        assertNotNull(payload, "CMaNGOS still sends TRAINER_LIST with count 0");
        WowBuffer b = new WowBuffer(payload);
        b.getU64();
        b.getU32();
        assertEquals(0, b.getU32());
        assertEquals(TrainerHandler.DEFAULT_GREETING, b.getCString());
    }
}
