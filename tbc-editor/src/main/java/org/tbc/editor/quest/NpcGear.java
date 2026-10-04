package org.tbc.editor.quest;

import org.tbc.world.content.ObjectMgr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Equipment templates already loaded on {@link ObjectMgr}, labeled with the three item names. */
public final class NpcGear {
    public record Gear(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private NpcGear() {}

    public static List<Gear> choices(ObjectMgr mgr) {
        List<Gear> out = new ArrayList<>();
        out.add(new Gear(0, "None"));
        if (mgr == null) {
            return out;
        }
        List<Integer> ids = new ArrayList<>(mgr.equipmentItems.keySet());
        Collections.sort(ids);
        for (int id : ids) {
            out.add(new Gear(id, label(mgr, id, mgr.equipmentItems.get(id))));
        }
        return out;
    }

    static String label(ObjectMgr mgr, int id, int[] slots) {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(": ");
        int named = 0;
        if (slots != null) {
            for (int itemId : slots) {
                if (itemId <= 0) {
                    continue;
                }
                if (named++ > 0) {
                    sb.append(", ");
                }
                ObjectMgr.ItemTemplate item = mgr.items.get(itemId);
                if (item != null && item.name != null && !item.name.isBlank()) {
                    sb.append(item.name);
                } else {
                    sb.append("item ").append(itemId);
                }
            }
        }
        if (named == 0) {
            sb.append("empty");
        }
        return sb.toString();
    }
}
