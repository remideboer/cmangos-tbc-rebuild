package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** SMSG_WEATHER snapshot. Layout: spec/03-protocol/packets/weather.md */
public final class WeatherHandler {
    private WeatherHandler() {}

    public static void sendSnapshot(WorldSession s, World world, int zoneId) {
        ObjectMgr.ZoneWeather row = world.objectMgr.weather.get(zoneId);
        if (row == null) {
            return;
        }
        WowBuffer b = new WowBuffer(9);
        b.putU32(row.state());
        b.putFloat(row.grade());
        b.putU8(Content.WEATHER_INSTANT_SMOOTH);
        s.send(Opcodes.SMSG_WEATHER, b.array());
    }
}
