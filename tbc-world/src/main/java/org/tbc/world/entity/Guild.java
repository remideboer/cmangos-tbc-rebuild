package org.tbc.world.entity;

import java.util.ArrayList;
import java.util.List;

/** In-memory guild. Slice 9 invite path; roster still encoded from the viewer. */
public final class Guild {
    public int id;
    public String name = "";
    public long leaderGuid;
    public final List<Long> members = new ArrayList<>();
}
