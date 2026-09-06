package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-098 — SPELL_EFFECT_SUMMON_RAF_FRIEND (152). Summon Friend 45927.
 * CMaNGOS caster CastSpell on the recruiting friend. SQL trigger 48955 later; Fireball 133 vehicle.
 */
class SpellEngineSummonRafFriendTest {
    @Test
    void applySummonRafFriendWhenFriendOnlineShouldCastNestedOnFriend() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_RAF_FRIEND));
        Player caster = new Player();
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        caster.setHealth(50);
        Player friend = new Player();
        friend.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        friend.setHealth(50);
        caster.setRecruitingFriend(friend);
        SpellEngine.SpellInfo summon = new SpellEngine.SpellInfo(
                45927, SpellEngine.EFFECT_SUMMON_RAF_FRIEND, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        int dmg = eng.apply(caster, caster, summon);
        assertEquals(10, dmg);
        assertEquals(40, friend.health());
        assertEquals(50, caster.health());
    }

    @Test
    void applySummonRafFriendWhenMissingFriendOrUnknownTriggerShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player caster = new Player();
        caster.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        caster.setHealth(50);
        SpellEngine.SpellInfo summon = new SpellEngine.SpellInfo(
                45927, SpellEngine.EFFECT_SUMMON_RAF_FRIEND, 0, 0, 0, 0, 0, 0f, SpellEngine.FIREBALL);
        assertEquals(0, eng.apply(caster, caster, summon));
        assertEquals(50, caster.health());
        Player friend = new Player();
        friend.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        friend.setHealth(50);
        caster.setRecruitingFriend(friend);
        assertEquals(0, eng.apply(caster, caster, new SpellEngine.SpellInfo(
                45927, SpellEngine.EFFECT_SUMMON_RAF_FRIEND, 0, 0, 0, 0, 0, 0f, 48955)));
        assertEquals(50, friend.health());
        assertEquals(0, eng.apply(new Creature(), new Creature(), summon));
        assertEquals(0, eng.summonRafFriend(null, SpellEngine.FIREBALL));
        assertEquals(0, eng.summonRafFriend(caster, 0));
    }
}
