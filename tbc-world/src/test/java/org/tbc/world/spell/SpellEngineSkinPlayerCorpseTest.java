package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Corpse;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-075 — SPELL_EFFECT_SKIN_PLAYER_CORPSE (116). Remove Insignia 22027.
 * CMaNGOS RemovedInsignia: BG victim, SMSG_PLAYER_SKINNED, bones lootable, CLIENT_LOOT_CORPSE.
 */
class SpellEngineSkinPlayerCorpseTest {
    @Test
    void applySkinPlayerCorpseWhenBgVictimShouldSkinBonesAndOpenCorpseLoot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SKIN_PLAYER_CORPSE));
        Player looter = new Player();
        Player victim = new Player();
        victim.mapId = 489;
        victim.guid = 9;
        Corpse corpse = new Corpse();
        corpse.guid = 9001;
        corpse.ownerGuid = victim.guid;
        victim.corpse = corpse;
        SpellEngine.SpellInfo skin = new SpellEngine.SpellInfo(
                22027, SpellEngine.EFFECT_SKIN_PLAYER_CORPSE, 0, 0, 0, 0, 0, 0f);
        eng.apply(looter, victim, skin);
        assertEquals(Corpse.CORPSE_BONES, corpse.corpseType);
        assertEquals(1, corpse.getInt(UpdateFields.CORPSE_FIELD_DYNAMIC_FLAGS));
        assertEquals(9001, looter.lastInsigniaLootGuid());
        WowBuffer skinned = new WowBuffer(SpellEngine.encodePlayerSkinned(victim.lastSkinnedRepop()));
        assertEquals(0, skinned.getU8());
        assertEquals(Opcodes.SMSG_PLAYER_SKINNED, 0x2BC);
        WowBuffer loot = new WowBuffer(SpellEngine.encodeInsigniaLoot(corpse.guid));
        assertEquals(9001, loot.getU64());
        assertEquals(1, loot.getU8());
        assertEquals(0, loot.getU32());
        assertEquals(0, loot.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applySkinPlayerCorpseWhenNonBgOrMissingCorpseShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player looter = new Player();
        Player continent = new Player();
        continent.mapId = 0;
        Corpse corpse = new Corpse();
        corpse.guid = 3;
        continent.corpse = corpse;
        SpellEngine.SpellInfo skin = new SpellEngine.SpellInfo(
                22027, SpellEngine.EFFECT_SKIN_PLAYER_CORPSE, 0, 0, 0, 0, 0, 0f);
        eng.apply(looter, continent, skin);
        assertEquals(Corpse.CORPSE_RESURRECTABLE_PVE, corpse.corpseType);
        assertEquals(0, looter.lastInsigniaLootGuid());
        Player bg = new Player();
        bg.mapId = 489;
        eng.apply(looter, bg, skin);
        assertEquals(0, looter.lastInsigniaLootGuid());
        eng.apply(new Creature(), bg, skin);
        Player timed = new Player();
        timed.mapId = 489;
        timed.deathTimerEndsAtMs = 1;
        timed.corpse = corpse;
        eng.apply(looter, timed, skin);
        assertEquals(1, timed.lastSkinnedRepop());
        assertEquals(0, timed.deathTimerEndsAtMs);
        eng.skinPlayerCorpse(null, timed);
        eng.skinPlayerCorpse(looter, null);
        eng.skinPlayerCorpse(looter, new Creature());
    }
}
