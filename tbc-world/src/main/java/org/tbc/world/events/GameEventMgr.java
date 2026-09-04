package org.tbc.world.events;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Start/stop `game_event` spawns. Spec: spec/05-domain/world-loop.md */
public final class GameEventMgr {
    private final Set<Integer> active = new HashSet<>();
    private final Map<Integer, List<Creature>> spawned = new HashMap<>();
    private final Map<Integer, List<GameObject>> spawnedGos = new HashMap<>();

    public void start(World world, int eventId) {
        if (world == null || eventId <= 0 || active.contains(eventId)) {
            return;
        }
        List<ObjectMgr.Spawn> rows = world.objectMgr.eventCreatures.get(eventId);
        List<ObjectMgr.Spawn> goRows = world.objectMgr.eventGameObjects.get(eventId);
        if (rows == null && goRows == null) {
            return;
        }
        active.add(eventId);
        List<Creature> live = new ArrayList<>();
        if (rows != null) {
            for (ObjectMgr.Spawn s : rows) {
                Creature c = world.objectMgr.spawnCreature(s, world.scripts);
                world.map(s.map(), 0).add(c);
                live.add(c);
            }
        }
        spawned.put(eventId, live);
        if (goRows != null) {
            List<GameObject> gos = new ArrayList<>();
            for (ObjectMgr.Spawn s : goRows) {
                GameObject go = world.objectMgr.spawnGameObject(s);
                world.map(s.map(), 0).add(go);
                gos.add(go);
            }
            spawnedGos.put(eventId, gos);
        }
    }

    public void stop(World world, int eventId) {
        if (world == null || !active.remove(eventId)) {
            return;
        }
        List<Creature> live = spawned.remove(eventId);
        if (live != null) {
            for (Creature c : live) {
                world.map(c.mapId, 0).remove(c);
            }
        }
        List<GameObject> gos = spawnedGos.remove(eventId);
        if (gos != null) {
            for (GameObject go : gos) {
                world.map(go.mapId, 0).remove(go);
            }
        }
    }

    public boolean isActive(int eventId) {
        return active.contains(eventId);
    }
}
