package org.tbc.world.gm;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.world.World;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * GM `.`/`!` parser. P0: .help .dismount (SEC_PLAYER), .die SEC_ADMINISTRATOR,
 * .appear SEC_MODERATOR + GM.LowerSecurity, .additem SEC_ADMINISTRATOR, SQL overlay.
 */
public final class GmCommands {
    public static final int SEC_PLAYER = 0;
    public static final int SEC_MODERATOR = 1;
    public static final int SEC_GAMEMASTER = 2;
    public static final int SEC_ADMINISTRATOR = 3;
    public static final int SEC_CONSOLE = 4;

    public record Leaf(String name, int security, boolean console) {}

    private final Map<String, Leaf> table = new TreeMap<>();
    private final Map<String, Integer> sqlOverlay = new HashMap<>();
    private final boolean lowerSecurity;

    public GmCommands(boolean lowerSecurity) {
        this.lowerSecurity = lowerSecurity;
        add("help", SEC_PLAYER, true);
        add("commands", SEC_PLAYER, true);
        add("dismount", SEC_PLAYER, false);
        add("die", SEC_ADMINISTRATOR, false);
        add("revive", SEC_GAMEMASTER, false);
        add("appear", SEC_MODERATOR, false);
        add("additem", SEC_ADMINISTRATOR, false);
        add("gm", SEC_MODERATOR, false);
        add("modify hp", SEC_GAMEMASTER, false);
        add("tele", SEC_MODERATOR, false);
        add("namego", SEC_MODERATOR, false);
        add("goname", SEC_MODERATOR, false);
        add("lookup", SEC_MODERATOR, true);
        add("reload", SEC_ADMINISTRATOR, true);
    }

    private void add(String name, int sec, boolean console) {
        table.put(name.toLowerCase(Locale.ROOT), new Leaf(name, sec, console));
    }

    public void overlay(String name, int security) {
        sqlOverlay.put(name.toLowerCase(Locale.ROOT), security);
    }

    public int security(String name) {
        String k = name.toLowerCase(Locale.ROOT);
        if (sqlOverlay.containsKey(k)) {
            return sqlOverlay.get(k);
        }
        Leaf l = table.get(k);
        return l == null ? Integer.MAX_VALUE : l.security;
    }

    public boolean allowed(Player p, String name) {
        return p.gmLevel >= security(name);
    }

    public String handle(World world, Player p, String line) {
        String raw = line.startsWith(".") || line.startsWith("!") ? line.substring(1) : line;
        String[] parts = raw.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "";
        }
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        if (parts.length > 1 && table.containsKey(cmd + " " + parts[1].toLowerCase(Locale.ROOT))) {
            cmd = cmd + " " + parts[1].toLowerCase(Locale.ROOT);
        }
        if (!allowed(p, cmd)) {
            return "You do not have access to that command.";
        }
        return switch (cmd) {
            case "help", "commands" -> "Available: .help .dismount .die .revive .appear .additem .tele";
            case "dismount" -> {
                p.mounted = false;
                yield "Dismounted.";
            }
            case "die" -> {
                p.setHealth(0);
                yield "You die.";
            }
            case "revive" -> {
                p.setHealth(p.maxHealth() == 0 ? 50 : p.maxHealth());
                p.ghost = false;
                yield "Revived.";
            }
            case "appear" -> {
                if (parts.length < 2) {
                    yield "Usage: .appear Name";
                }
                Player t = world.playerByName(parts[1]);
                if (t == null) {
                    yield "Player not found.";
                }
                if (lowerSecurity && t.gmLevel >= p.gmLevel) {
                    yield "You cannot appear to a player of equal or higher security.";
                }
                world.teleport(p, t.mapId, t.x, t.y, t.z, t.o);
                yield "Appearing.";
            }
            case "additem" -> {
                if (parts.length < 2) {
                    yield "Usage: .additem id";
                }
                int id = Integer.parseInt(parts[1]);
                Item it = new Item(world.nextItemGuid(), id);
                it.count = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                p.items.put((int) it.guid, it);
                p.dirty = true;
                yield "Item added.";
            }
            case "tele" -> {
                if (parts.length < 4) {
                    yield "Usage: .tele x y z [map]";
                }
                float x = Float.parseFloat(parts[1]);
                float y = Float.parseFloat(parts[2]);
                float z = Float.parseFloat(parts[3]);
                int mapId = parts.length >= 5 ? Integer.parseInt(parts[4]) : p.mapId;
                world.teleport(p, mapId, x, y, z, p.o);
                yield "Teleported.";
            }
            default -> "Unknown command.";
        };
    }
}
