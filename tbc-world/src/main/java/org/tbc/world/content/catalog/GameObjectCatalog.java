package org.tbc.world.content.catalog;

import org.tbc.world.content.ObjectMgr;

/** Read-only gameobject_template view; the narrow interface handlers depend on. */
public interface GameObjectCatalog {
    /** Template for {@code entry}, or {@code null} when unknown. */
    ObjectMgr.GameObjectTemplate gameObject(int entry);
}
