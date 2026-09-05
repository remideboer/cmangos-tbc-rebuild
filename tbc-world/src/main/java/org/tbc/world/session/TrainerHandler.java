package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.List;
import java.util.function.BiConsumer;

/** CMSG_TRAINER_LIST / CMSG_TRAINER_BUY_SPELL. Layout: spec/03-protocol/packets/trainer.md */
public final class TrainerHandler {
    /** Player.h TRAINER_SPELL_GREEN. */
    public static final int TRAINER_SPELL_GREEN = 0;
    /** Player.h TRAINER_SPELL_RED. */
    public static final int TRAINER_SPELL_RED = 1;
    /** Player.h TRAINER_SPELL_GRAY. */
    public static final int TRAINER_SPELL_GRAY = 2;
    /** mangos.sql mangos_string 51 LANG_NPC_TAINER_HELLO. */
    public static final String DEFAULT_GREETING = "Hello! Ready for some training?";

    private TrainerHandler() {}

    public static void sendList(Player p, Creature c, ObjectMgr mgr, BiConsumer<Integer, byte[]> send) {
        byte[] payload = encodeList(p, c, mgr);
        if (payload != null) {
            send.accept(Opcodes.SMSG_TRAINER_LIST, payload);
        }
    }

    static byte[] encodeList(Player p, Creature c, ObjectMgr mgr) {
        if ((c.npcFlags & Content.UNIT_NPC_FLAG_TRAINER) == 0) {
            return null;
        }
        Integer cls = mgr.trainerClass.get(c.entry);
        if (cls != null && cls != 0 && cls != p.clazz) {
            return null;
        }
        List<ObjectMgr.TrainerSpell> rows = mgr.trainerSpells.get(c.entry);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        ObjectMgr.CreatureTemplate t = mgr.creatures.get(c.entry);
        int trainerType = t == null ? 0 : t.trainerType();
        WowBuffer b = new WowBuffer(16 + rows.size() * 38 + DEFAULT_GREETING.length() + 1);
        b.putU64(c.guid);
        b.putU32(trainerType);
        b.putU32(rows.size());
        for (ObjectMgr.TrainerSpell s : rows) {
            int state = TRAINER_SPELL_GREEN;
            if (p.spells.contains(s.spell())) {
                state = TRAINER_SPELL_GRAY;
            } else if (p.level < s.reqLevel()) {
                state = TRAINER_SPELL_RED;
            }
            b.putU32(s.spell());
            b.putU8(state);
            b.putU32(s.cost());
            b.putU32(0);
            b.putU32(0);
            b.putU8(s.reqLevel());
            b.putU32(0);
            b.putU32(0);
            b.putU32(0);
            b.putU32(0);
            b.putU32(0);
        }
        b.putCString(DEFAULT_GREETING);
        return b.array();
    }

    public static void buySpell(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        int spell = in.remaining() >= 4 ? in.getU32() : 0;
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_TRAINER) == 0) {
            return;
        }
        Integer cls = world.objectMgr.trainerClass.get(npc.entry);
        if (cls != null && cls != 0 && cls != p.clazz) {
            return;
        }
        ObjectMgr.TrainerSpell row = null;
        List<ObjectMgr.TrainerSpell> rows = world.objectMgr.trainerSpells.get(npc.entry);
        if (rows != null) {
            for (ObjectMgr.TrainerSpell t : rows) {
                if (t.spell() == spell) {
                    row = t;
                    break;
                }
            }
        }
        if (row == null || p.money < row.cost()) {
            return;
        }
        p.setMoney(p.money - row.cost());
        if (!p.spells.contains(spell)) {
            p.spells.add(spell);
        }
        WowBuffer ok = new WowBuffer(12);
        ok.putU64(guid);
        ok.putU32(spell);
        s.send(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED, ok.array());
        WowBuffer learned = new WowBuffer(4);
        learned.putU32(spell);
        s.send(Opcodes.SMSG_LEARNED_SPELL, learned.array());
    }
}
