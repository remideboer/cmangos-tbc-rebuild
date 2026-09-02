package org.tbc.world.persist;

import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;

/** Clone persistable player fields. Buyback is not copied (not persisted). */
public final class PlayerPersist {
    private PlayerPersist() {}

    public static Player copy(Player src) {
        Player d = new Player();
        d.guid = src.guid;
        d.accountId = src.accountId;
        d.name = src.name;
        d.race = src.race;
        d.clazz = src.clazz;
        d.gender = src.gender;
        d.skin = src.skin;
        d.face = src.face;
        d.hairStyle = src.hairStyle;
        d.hairColor = src.hairColor;
        d.facialHair = src.facialHair;
        d.money = src.money;
        d.xp = src.xp;
        d.cinematic = src.cinematic;
        d.atLogin = src.atLogin;
        d.difficulty = src.difficulty;
        d.guildId = src.guildId;
        d.level = src.level;
        d.faction = src.faction;
        d.displayId = src.displayId;
        d.team = src.team;
        d.powerType = src.powerType;
        d.bindMap = src.bindMap;
        d.bindZone = src.bindZone;
        d.bindX = src.bindX;
        d.bindY = src.bindY;
        d.bindZ = src.bindZ;
        d.mapId = src.mapId;
        d.zoneId = src.zoneId;
        d.relocate(src.x, src.y, src.z, src.o);
        d.online = src.online;
        d.resting = src.resting;
        d.restBonus = src.restBonus;
        d.watchedFaction = src.watchedFaction;
        System.arraycopy(src.actionButtons, 0, d.actionButtons, 0, 132);
        System.arraycopy(src.tut, 0, d.tut, 0, 8);
        d.spells.addAll(src.spells);
        if (!src.items.isEmpty()) {
            for (Item it : src.items.values()) {
                Item c = copyItem(it);
                d.items.put(Guid.low(c.guid), c);
            }
        }
        d.applyCreateFields();
        d.setInt(UpdateFields.UNIT_FIELD_HEALTH, Math.max(1, src.health()));
        d.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, Math.max(1, src.maxHealth()));
        return d;
    }

    static Item copyItem(Item src) {
        Item d = new Item(src.guid, src.entry);
        d.count = src.count;
        d.bag = src.bag;
        d.slot = src.slot;
        d.durability = src.durability;
        d.ownerGuid = src.ownerGuid;
        d.displayId = src.displayId;
        d.inventoryType = src.inventoryType;
        d.enchant = src.enchant;
        d.quality = src.quality;
        d.soulbound = src.soulbound;
        return d;
    }
}
