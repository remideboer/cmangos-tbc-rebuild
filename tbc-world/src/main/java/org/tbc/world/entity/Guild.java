package org.tbc.world.entity;

import java.util.ArrayList;
import java.util.List;

/** In-memory guild. Slice 9 invite path; roster still encoded from the viewer. */
public final class Guild {
    public int id;
    public String name = "";
    public String motd = "";
    public long leaderGuid;
    public int emblemStyle;
    public int emblemColor;
    public int borderStyle;
    public int borderColor;
    public int backgroundColor;
    public int purchasedTabs;
    public final String[] tabTexts = new String[6];
    public final List<Long> members = new ArrayList<>();
    public final List<Rank> ranks = new ArrayList<>();

    public static final class Rank {
        public final String name;
        public final int rights;

        public Rank(String name, int rights) {
            this.name = name;
            this.rights = rights;
        }
    }
}
