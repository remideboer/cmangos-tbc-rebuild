package org.tbc.world.content.catalog;

import java.util.List;
import org.tbc.world.content.ObjectMgr;

/** Read-only creature / gameobject spawn rows; mutation goes through named ObjectMgr methods. */
public interface SpawnCatalog {
    /** Loaded {@code creature} rows (unmodifiable view). */
    List<ObjectMgr.Spawn> creatureSpawns();

    /** Loaded {@code gameobject} rows (unmodifiable view). */
    List<ObjectMgr.Spawn> gameObjectSpawns();
}
