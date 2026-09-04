package org.tbc.world.events;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
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

    public void start(World world, int eventId) {
        if (world == null || eventId <= 0 || active.contains(eventId)) {
            return;
        }
        List<ObjectMgr.Spawn> rows = world.objectMgr.eventCreatures.get(eventId);
        if (rows == null) {
            return;
        }
        active.add(eventId);
        List<Creature> live = new ArrayList<>();
        for (ObjectMgr.Spawn s : rows) {
            Creature c = world.objectMgr.spawnCreature(s, world.scripts);
            world.map(s.map(), 0).add(c);
            live.add(c);
        }
        spawned.put(eventId, live);
        List<ObjectMgr.Spawn> gos = world.objectMgr.eventGameObjects.get(eventId);
        if (gos != null) {
            for (ObjectMgr.Spawn s : gos) {
                world.map(s.map(), 0).add(world.objectMgr.spawnGameObject(s));
            }
        }
    }

    public void stop(World world, int eventId) {
        if (world == null || !active.remove(eventId)) {
            return;
        }
        List<Creature> live = spawned.remove(eventId);
        if (live == null) {
            return;
        }
        for (Creature c : live) {
            world.map(c.mapId, 0).remove(c);
        }
    }

    public boolean isActive(int eventId) {
        return active.contains(eventId);
    }
}
