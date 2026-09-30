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
    /** SharedDefines.h TRAINER_TYPE_CLASS. */
    public static final int TRAINER_TYPE_CLASS = 0;
    /** SharedDefines.h TRAINER_TYPE_MOUNTS. */
    public static final int TRAINER_TYPE_MOUNTS = 1;
    /** SharedDefines.h TRAINER_TYPE_TRADESKILLS. */
    public static final int TRAINER_TYPE_TRADESKILLS = 2;
    /** SharedDefines.h TRAINER_TYPE_PETS. */
    public static final int TRAINER_TYPE_PETS = 3;
    /** mangos.sql mangos_string 51 LANG_NPC_TAINER_HELLO. */
    public static final String DEFAULT_GREETING = "Hello! Ready for some training?";
    /** Spell.dbc Apprentice Blacksmith — EFFECT_SKILL_STEP skill 164. */
    public static final int SPELL_APPRENTICE_BLACKSMITH = 2020;
    public static final int SKILL_BLACKSMITHING = 164;

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
        if (!isTrainerOf(p, c, mgr)) {
            return null;
        }
        List<ObjectMgr.TrainerSpell> rows = mgr.spellsForTrainer(c.entry);
        if (rows.isEmpty()) {
            return null;
        }
        int trainerType = mgr.trainerType(c.entry);
        WowBuffer b = new WowBuffer(16 + rows.size() * 38 + DEFAULT_GREETING.length() + 1);
        b.putU64(c.guid);
        b.putU32(trainerType);
        b.putU32(rows.size());
        for (ObjectMgr.TrainerSpell s : rows) {
            int state = TrainerService.state(p, s);
            b.putU32(s.spell());
            b.putU8(state);
            b.putU32(s.cost());
            b.putU32(0);
            b.putU32(s.primaryProfessionFirstRank() ? 1 : 0);
            b.putU8(s.reqLevel());
            b.putU32(s.reqSkill());
            b.putU32(s.reqSkillValue());
            b.putU32(s.reqAbility0());
            b.putU32(s.reqAbility1());
            b.putU32(s.reqAbility2());
        }
        b.putCString(DEFAULT_GREETING);
        return b.array();
    }

    /** Delegates to ObjectMgr — gossip and trainer packets share one rule. */
    static boolean isTrainerOf(Player p, Creature c, ObjectMgr mgr) {
        return mgr.isTrainerOf(p, c);
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
        if (!isTrainerOf(p, npc, world.objectMgr)) {
            return;
        }
        ObjectMgr.TrainerSpell row = null;
        List<ObjectMgr.TrainerSpell> rows = world.objectMgr.spellsForTrainer(npc.entry);
        for (ObjectMgr.TrainerSpell t : rows) {
            if (t.spell() == spell) {
                row = t;
                break;
            }
        }
        if (row == null || p.money < row.cost()) {
            return;
        }
        if (TrainerService.state(p, row) != TRAINER_SPELL_GREEN) {
            return;
        }
        p.setMoney(p.money - row.cost());
        teach(p, world, spell, row);
        WowBuffer ok = new WowBuffer(12);
        ok.putU64(guid);
        ok.putU32(spell);
        s.send(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED, ok.array());
        WowBuffer learned = new WowBuffer(4);
        learned.putU32(spell);
        s.send(Opcodes.SMSG_LEARNED_SPELL, learned.array());
    }

    /** learnSpell + profession skill step when the trainer row is a primary first-rank. */
    static void teach(Player p, World world, int spell, ObjectMgr.TrainerSpell row) {
        if (!p.spells.contains(spell)) {
            p.spells.add(spell);
        }
        if (world != null && world.spells != null) {
            world.spells.learnSpell(p, spell);
        }
        if (row.primaryProfessionFirstRank()) {
            int skill = skillForProfessionSpell(spell);
            if (skill != 0) {
                p.learnSkill(skill, 1, 75, 1);
            }
        }
        if (spell == SPELL_APPRENTICE_BLACKSMITH && !p.hasSkill(SKILL_BLACKSMITHING)) {
            p.learnSkill(SKILL_BLACKSMITHING, 1, 75, 1);
        }
    }

    static int skillForProfessionSpell(int spell) {
        if (spell == SPELL_APPRENTICE_BLACKSMITH) {
            return SKILL_BLACKSMITHING;
        }
        return 0;
    }
}
