package org.tbc.world.events;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Start/stop `game_event` spawns. Spec: spec/05-domain/world-loop.md */
public final class GameEventMgr {
    private final Set<Integer> active = new HashSet<>();

    public void start(World world, int eventId) {
        if (world == null || eventId <= 0 || active.contains(eventId)) {
            return;
        }
        List<ObjectMgr.Spawn> rows = world.objectMgr.eventCreatures.get(eventId);
        if (rows == null) {
            return;
        }
        active.add(eventId);
        for (ObjectMgr.Spawn s : rows) {
            world.map(s.map(), 0).add(world.objectMgr.spawnCreature(s, world.scripts));
        }
    }

    public boolean isActive(int eventId) {
        return active.contains(eventId);
    }
}
