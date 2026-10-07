package org.tbc.world.content.catalog;

import org.tbc.world.content.ObjectMgr;

/** Read-only creature_template view; the narrow interface handlers depend on. */
public interface CreatureCatalog {
    /** Template for {@code entry}, or {@code null} when unknown. */
    ObjectMgr.CreatureTemplate creature(int entry);
}
