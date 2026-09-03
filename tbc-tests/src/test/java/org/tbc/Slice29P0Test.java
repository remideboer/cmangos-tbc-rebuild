package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Player;
import org.tbc.world.gm.GmCommands;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL29-* from spec/05-domain/gm-commands.md */
class Slice29P0Test {
    @Test
    void tpSl29PlayerHelpDismountDieDenied() {
        World w = World.inMemory();
        Player p = w.characters.create(1, "Lowsec", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        p.gmLevel = 0;
        p.mounted = true;
        assertTrue(w.gm.allowed(p, "help"));
        assertTrue(w.gm.allowed(p, "dismount"));
        assertFalse(w.gm.allowed(p, "die"));
        assertEquals("Dismounted.", w.gm.handle(w, p, ".dismount"));
        assertFalse(p.mounted);
    }

    @Test
    void tpSl29AppearLowerSecurity() {
        World w = World.inMemory();
        WowClientDouble d = new WowClientDouble();
        d.connect(new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86"));
        Player low = w.characters.create(1, "Lowsec", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        WowClientDouble gm = new WowClientDouble();
        gm.connect(new World.Account(2, "GM", new byte[40], 1, 1, "Win", "x86"));
        Player mod = w.characters.create(2, "Mod", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        d.login(w, low.guid);
        gm.login(w, mod.guid);
        low = d.session().player();
        mod = gm.session().player();
        mod.gmLevel = 1;
        low.gmLevel = 1;
        low.relocate(10, 10, 10, 0);
        GmCommands lower = new GmCommands(true);
        assertTrue(lower.handle(w, mod, ".appear Lowsec").contains("cannot appear"));
        low.gmLevel = 0;
        mod.gmLevel = 3;
        assertEquals("Appearing.", lower.handle(w, mod, ".appear Lowsec"));
    }

    @Test
    void tpSl29SqlOverlay() {
        World w = World.inMemory();
        Player p = w.characters.create(1, "Lowsec", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        p.gmLevel = 0;
        w.gm.overlay("die", 0);
        assertTrue(w.gm.allowed(p, "die"));
    }
}
