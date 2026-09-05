package org.tbc.editor;

import org.tbc.world.entity.Player;

/** Create/load/save/delete. Production wraps CharacterStore. */
public interface CharacterOps {
    Player create(int accountId, String name, int race, int clazz, int gender,
                  int skin, int face, int hair, int hairColor, int facial);

    Player load(int accountId, long guid);

    void save(Player p);

    boolean delete(int accountId, long guid);

    boolean nameInUse(String name);
}
