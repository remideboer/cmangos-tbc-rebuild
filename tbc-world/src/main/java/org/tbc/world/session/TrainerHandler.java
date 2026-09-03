package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.List;

/** CMSG_TRAINER_BUY_SPELL. Layout: spec/03-protocol/packets/trainer.md */
public final class TrainerHandler {
    private TrainerHandler() {}

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
