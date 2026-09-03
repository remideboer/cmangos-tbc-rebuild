package org.tbc.world.pvp;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.WorldSession;

/** Honor wallet + midnight roll from spec/05-domain/honor.md. Cap is MaxHonorPoints 75000. */
public final class Honor {
    public static final int MAX_HONOR_POINTS = 75_000;

    private Honor() {}

    public static void reward(Player killer, WorldSession session, long victimGuid, int amount) {
        int add = Math.max(0, amount);
        killer.honorToday += add;
        killer.honorPoints = Math.min(MAX_HONOR_POINTS, killer.honorPoints + add);
        killer.setInt(UpdateFields.PLAYER_FIELD_KILLS, killer.honorToday & 0xFFFF);
        if (session != null) {
            WowBuffer credit = new WowBuffer(16);
            credit.putU32(add);
            credit.putU64(victimGuid);
            credit.putU32(0);
            session.send(Opcodes.SMSG_PVP_CREDIT, credit.array());
        }
    }

    public static void midnightRoll(Player p) {
        p.honorYesterday = p.honorToday;
        p.yesterdayContrib = p.honorToday;
        p.setInt(UpdateFields.PLAYER_FIELD_KILLS, p.honorToday << 16);
        p.setInt(UpdateFields.PLAYER_FIELD_YESTERDAY_CONTRIBUTION, p.yesterdayContrib);
        p.honorToday = 0;
    }
}
