package org.tbc.world.map;

import org.tbc.world.ai.DbScriptEngine;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GameMap {
    public final int mapId;
    public final int instanceId;
    public final Map<Long, Player> players = new ConcurrentHashMap<>();
    public final Map<Long, Creature> creatures = new ConcurrentHashMap<>();
    public final Map<Long, GameObject> gameObjects = new ConcurrentHashMap<>();
    public final DbScriptEngine dbScripts = new DbScriptEngine();
    public static final double VISIBILITY = 90.0;
    private static final float CELL = 100f;
    private final Map<Long, Map<Long, Creature>> cells = new ConcurrentHashMap<>();

    public GameMap(int mapId, int instanceId) {
        this.mapId = mapId;
        this.instanceId = instanceId;
    }

    public void add(Player p) {
        players.put(p.guid, p);
    }

    public void remove(Player p) {
        players.remove(p.guid);
    }

    public void add(Creature c) {
        creatures.put(c.guid, c);
        cells.computeIfAbsent(cellKey(c.x, c.y), k -> new ConcurrentHashMap<>()).put(c.guid, c);
    }

    public void add(GameObject go) {
        gameObjects.put(go.guid, go);
    }

    public void remove(Creature c) {
        creatures.remove(c.guid);
        Map<Long, Creature> cell = cells.get(cellKey(c.x, c.y));
        if (cell != null) {
            cell.remove(c.guid);
        }
    }

    public List<Player> nearbyPlayers(Unit u, double range) {
        List<Player> out = new ArrayList<>();
        for (Player p : players.values()) {
            if (p.guid != u.guid && p.distance2d(u) <= range) {
                out.add(p);
            }
        }
        return out;
    }

    public List<Creature> nearbyCreatures(Unit u, double range) {
        List<Creature> out = new ArrayList<>();
        int span = (int) Math.ceil(range / CELL) + 1;
        int cx = (int) Math.floor(u.x / CELL);
        int cy = (int) Math.floor(u.y / CELL);
        for (int dx = -span; dx <= span; dx++) {
            for (int dy = -span; dy <= span; dy++) {
                Map<Long, Creature> cell = cells.get(cellKeyIndex(cx + dx, cy + dy));
                if (cell == null) {
                    continue;
                }
                for (Creature c : cell.values()) {
                    if (c.alive() && c.distance2d(u) <= range) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    public Collection<Player> players() {
        return players.values();
    }

    static long cellKey(float x, float y) {
        return cellKeyIndex((int) Math.floor(x / CELL), (int) Math.floor(y / CELL));
    }

    private static long cellKeyIndex(int cx, int cy) {
        return ((long) cx << 32) | (cy & 0xFFFFFFFFL);
    }
}
