package org.tbc.world.content.catalog;

import org.tbc.world.content.ObjectMgr;

/** Read-only item_template lookup; the narrow view handlers and policies take instead of the whole ObjectMgr. */
public interface ItemCatalog {
    /** Template for {@code entry}, or {@code null} when unknown. */
    ObjectMgr.ItemTemplate item(int entry);
}
