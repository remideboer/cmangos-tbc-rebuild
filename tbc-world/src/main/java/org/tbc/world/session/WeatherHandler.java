package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.ArrayList;

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

    /** Weather.cpp Update when the zone timer fires. No-chances path is FINE. */
    public static void onTimer(World world) {
        if (world == null) {
            return;
        }
        for (int zone : new ArrayList<>(world.objectMgr.weather.keySet())) {
            ObjectMgr.ZoneWeather row = world.objectMgr.weather.get(zone);
            if (row == null || (row.state() == Content.WEATHER_STATE_FINE && row.grade() == 0f)) {
                continue;
            }
            world.objectMgr.weather.put(zone, new ObjectMgr.ZoneWeather(zone, Content.WEATHER_STATE_FINE, 0f));
            for (GameMap m : world.maps()) {
                for (Player p : m.players.values()) {
                    if (p.zoneId == zone && p.session != null) {
                        sendSnapshot(p.session, world, zone);
                    }
                }
            }
        }
    }
}
