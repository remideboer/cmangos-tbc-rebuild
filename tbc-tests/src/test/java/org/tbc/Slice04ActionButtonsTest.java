package org.tbc;

import org.junit.jupiter.api.Test;
import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL04-019 — first enter-world {@code SMSG_ACTION_BUTTONS} from {@code playercreateinfo_action}.
 * Oracle: login-burst.md 10g, create-self.md; tbc-db human warrior (1,1).
 */
class Slice04ActionButtonsTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");
    private static final int ACTION_BUTTON_ITEM = 0x80;
    private static final int MAX_ACTION_BUTTONS = 132;

    @Test
    void tpSl04CreateActionButtonsHumanWarrior() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Barwar", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);

        assertTrue(client.saw(Opcodes.SMSG_ACTION_BUTTONS));
        byte[] p = client.payload(Opcodes.SMSG_ACTION_BUTTONS);
        assertEquals(MAX_ACTION_BUTTONS * 4, p.length);
        assertEquals(6603, WowClientDouble.u32le(p, 72 * 4), "Auto Attack");
        assertEquals(78, WowClientDouble.u32le(p, 73 * 4), "Heroic Strike");
        assertEquals(117 | (ACTION_BUTTON_ITEM << 24), WowClientDouble.u32le(p, 83 * 4), "Tough Jerky");
        assertEquals(0, WowClientDouble.u32le(p, 0), "main bar empty — warrior stance bar is 72–83");
    }

    /**
     * TP-SL04-020 — 8606 shows buttons 72–83 only while shapeshifted into Battle Stance.
     * CMaNGOS Player::_LoadAuras: warrior without SPELL_AURA_MOD_SHAPESHIFT casts 2457.
     */
    @Test
    void tpSl04WarriorBattleStanceBarVisible() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Stancewar", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);

        byte[] bar = client.payload(Opcodes.SMSG_ACTION_BUTTONS);
        assertEquals(6603, WowClientDouble.u32le(bar, 72 * 4), "Battle Stance Auto Attack");
        assertEquals(78, WowClientDouble.u32le(bar, 73 * 4), "Battle Stance Heroic Strike");
        assertEquals(117 | (ACTION_BUTTON_ITEM << 24), WowClientDouble.u32le(bar, 83 * 4), "Battle Stance Tough Jerky");

        Map<Integer, Integer> self = client.selfCreateValues();
        assertEquals(0x11, (self.get(UpdateFields.UNIT_FIELD_BYTES_2) >>> 24) & 0xFF, "FORM_BATTLESTANCE");
        assertEquals(2457, self.get(UpdateFields.UNIT_FIELD_AURA), "Battle Stance aura");
        assertTrue(initialSpellsContain(client.payload(Opcodes.SMSG_INITIAL_SPELLS), 2457));
    }

    private static boolean initialSpellsContain(byte[] p, int spellId) {
        int n = (p[1] & 0xFF) | ((p[2] & 0xFF) << 8);
        int off = 3;
        for (int i = 0; i < n; i++) {
            int id = (p[off] & 0xFF) | ((p[off + 1] & 0xFF) << 8);
            if (id == spellId) {
                return true;
            }
            off += 4;
        }
        return false;
    }
}
