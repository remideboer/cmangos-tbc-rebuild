package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.spell.AuraSlots;
import org.tbc.world.world.World;

/** Player-initiated cancels: cast, auto-repeat, channel, aura (SpellHandler.cpp). */
public final class SpellCancelHandler {
    private SpellCancelHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_CANCEL_CAST, SpellCancelHandler::cancelCast)
                .register(Opcodes.CMSG_CANCEL_AUTO_REPEAT_SPELL, (s, w, in) -> cancelAutoRepeat(s, w))
                .register(Opcodes.CMSG_CANCEL_CHANNELLING, SpellCancelHandler::cancelChannelling)
                .register(Opcodes.CMSG_CANCEL_AURA, SpellCancelHandler::cancelAura);
    }

    public static void cancelCast(WorldSession s, World world, WowBuffer in) {
        int spellId = in.remaining() >= 4 ? in.getU32() : 0;
        world.spells.cancelCast(s.player(), spellId);
    }

    public static void cancelAutoRepeat(WorldSession s, World world) {
        world.spells.interruptAutoRepeat(s.player().guid);
    }

    public static void cancelChannelling(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        world.spells.cancelChannel(p, s::send);
        WowBuffer fail = new WowBuffer(12);
        fail.putPackedGuid(p.guid);
        fail.putU32(in.remaining() >= 4 ? in.getU32() : 0);
        fail.putU8(0);
        s.send(Opcodes.SMSG_SPELL_FAILURE, fail.array());
    }

    public static void cancelAura(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        int spell = in.remaining() >= 4 ? in.getU32() : 0;
        int slot = AuraSlots.slotOf(p, spell);
        long casterGuid = 0;
        for (Unit.Aura a : p.auras) {
            if (a.spellId() == spell) {
                casterGuid = a.casterGuid();
                break;
            }
        }
        world.spells.cancelAura(p, spell);
        world.spells.unapplyAura(p, spell);
        // ApplyModifier(false) mutates UNIT_FIELD_* — push sheet fields (Frost Armor armor, etc.).
        world.spells.sendUnapplyAuraValues(p, spell, s::send);
        if (slot >= 0) {
            AuraSlots.clearVisible(p, slot);
            var upd = UpdateBuilder.maybeCompress(
                    UpdateBuilder.values(p,
                            UpdateFields.UNIT_FIELD_AURA + slot,
                            UpdateFields.UNIT_FIELD_AURAFLAGS + slot / 4,
                            UpdateFields.UNIT_FIELD_AURALEVELS + slot / 4,
                            UpdateFields.UNIT_FIELD_AURAAPPLICATIONS + slot / 4));
            GameMap m = world.map(p.mapId, p.instanceId);
            if (p.session != null) {
                p.session.send(upd.opcode(), upd.payload());
            }
            if (m != null) {
                for (Player pl : m.nearbyPlayers(p, GameMap.VISIBILITY)) {
                    if (pl.session != null) {
                        pl.session.send(upd.opcode(), upd.payload());
                    }
                }
                if (casterGuid != 0) {
                    Player caster = m.players.get(casterGuid);
                    if (caster != null && caster.session != null) {
                        AuraSlots.sendClearExtraAuraInfo(p, spell, caster.session::send);
                    }
                }
            }
        }
        // BattleGroundWS::HandlePlayerDroppedFlag — flag aura cancel → ON_GROUND (-1).
        if (p.mapId == 489 && (spell == PvpObjectives.WSG_FLAG_A || spell == PvpObjectives.WSG_FLAG_H)) {
            // Match pickup stub: aura 23333 ↔ WS 1545; aura 23335 ↔ WS 1546.
            int field = spell == PvpObjectives.WSG_FLAG_A ? PvpObjectives.WS_WSG_A : PvpObjectives.WS_WSG_H;
            WowBuffer ws = new WowBuffer(8);
            ws.putU32(field);
            ws.putU32(-1);
            s.send(Opcodes.SMSG_UPDATE_WORLD_STATE, ws.array());
        }
    }
}
