package org.tbc.world.ai;

import java.util.List;

/** CMaNGOS PathFinder.h PathType bits. */
public final class PathType {
    public static final int BLANK = 0;
    public static final int NORMAL = 0x0001;
    public static final int SHORTCUT = 0x0002;
    public static final int INCOMPLETE = 0x0004;
    public static final int NOPATH = 0x0008;
    public static final int NOT_USING_PATH = 0x0010;
    public static final int SHORT = 0x0020;

    private PathType() {
    }

    public static boolean nopath(int type) {
        return (type & NOPATH) != 0;
    }

    public static boolean usable(int type) {
        return (type & (NORMAL | SHORTCUT | INCOMPLETE | NOT_USING_PATH)) != 0
                && (type & NOPATH) == 0;
    }
}
