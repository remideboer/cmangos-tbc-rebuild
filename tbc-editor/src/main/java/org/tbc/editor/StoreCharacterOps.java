package org.tbc.editor;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.persist.CharacterStore;

/** CharacterStore create/load/save/delete for the editor. */
public final class StoreCharacterOps implements CharacterOps {
    private final CharacterStore store;
    private final ObjectMgr mgr;

    public StoreCharacterOps(CharacterStore store, ObjectMgr mgr) {
        this.store = store;
        this.mgr = mgr;
    }

    @Override
    public Player create(int accountId, String name, int race, int clazz, int gender,
                         int skin, int face, int hair, int hairColor, int facial) {
        return store.create(accountId, name, race, clazz, gender, skin, face, hair, hairColor, facial, mgr);
    }

    @Override
    public Player load(int accountId, long guid) {
        return store.load(accountId, guid, mgr);
    }

    @Override
    public void save(Player p) {
        store.save(p);
    }

    @Override
    public boolean delete(int accountId, long guid) {
        return store.delete(accountId, guid);
    }

    @Override
    public boolean nameInUse(String name) {
        return store.nameInUse(name);
    }
}
