package org.tbc.editor;

import org.tbc.common.Conf;
import org.tbc.common.DbPool;
import org.tbc.editor.quest.QuestDomain;
import org.tbc.editor.quest.QuestService;
import org.tbc.editor.quest.WorldMapBlp;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.MapSurfaceService;
import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.Terrain;
import org.tbc.world.map.WorldMapAreas;
import org.tbc.world.persist.CharacterStore;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/** Swing operator tool. Character DB + login names. No SOAP/RA, no world RPC. */
public final class EditorMain {
    private EditorMain() {}

    static String confPath(String[] args) {
        if (args == null) {
            return "conf/mangosd.conf";
        }
        for (String arg : args) {
            if (arg != null && !arg.isBlank() && !arg.startsWith("-")) {
                return arg;
            }
        }
        return "conf/mangosd.conf";
    }

    static boolean questFlag(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if ("--quest".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Path confFile = Path.of(confPath(args));
        boolean openQuests = questFlag(args);
        Conf conf = Conf.load(confFile, "Mangosd_");
        DbPool login = new DbPool(conf.db("LoginDatabaseInfo"), "editor-login");
        DbPool worldDb = new DbPool(conf.db("WorldDatabaseInfo"), "editor-world");
        DbPool chars = new DbPool(conf.db("CharacterDatabaseInfo"), "editor-chars");
        CharacterStore store = new CharacterStore(chars);
        ObjectMgr mgr = new ObjectMgr();
        SwingUtilities.invokeLater(() -> {
            EditorFrame frame = new EditorFrame();
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    login.close();
                    worldDb.close();
                    chars.close();
                }
            });
            frame.setStatus("Loading content…");
            frame.setVisible(true);
            new SwingWorker<CharacterService, Void>() {
                @Override
                protected CharacterService doInBackground() {
                    mgr.load(worldDb, null);
                    return new CharacterService(
                            new JdbcAccountLookup(login),
                            new JdbcCharacterRepository(chars),
                            new StoreCharacterOps(store, mgr));
                }

                @Override
                protected void done() {
                    try {
                        CharacterService service = get();
                        frame.addDomain(new CharacterDomain(service, frame::setStatus));
                        Path content = Path.of("content");
                        String dataDir = conf.get("DataDir", "");
                        Path dataPath = dataDir.isBlank() ? null : Path.of(dataDir);
                        MapSurfaceService surfaces = dataPath == null
                                ? MapSurfaceService.unavailable()
                                : MapSurfaceService.fromTerrain(Terrain.fromDataDir(dataPath), null);
                        Terrain terrain = dataPath == null ? null : Terrain.fromDataDir(dataPath);
                        WorldMapAreas namedMaps = WorldMapAreas.fromDbc(dataPath);
                        QuestService quests = new QuestService(mgr, content, surfaces);
                        frame.addDomain(new QuestDomain(quests, frame::setStatus, namedMaps, area -> {
                            RegionMinimap.Raster clientMap = WorldMapBlp.load(dataPath, area);
                            if (clientMap != null) {
                                return clientMap;
                            }
                            int w = 512;
                            float dy = Math.abs(area.locLeft() - area.locRight());
                            float dx = Math.abs(area.locTop() - area.locBottom());
                            int h = Math.max(64, (int) (w * (dx / Math.max(1f, dy))));
                            return RegionMinimap.render(area,
                                    terrain == null ? Terrain.NONE : terrain.asHeight(), w, h);
                        }));
                        if (openQuests) {
                            frame.selectDomain("Quests");
                        }
                        frame.setStatus("Ready.");
                    } catch (Exception e) {
                        Throwable c = e.getCause() == null ? e : e.getCause();
                        frame.setStatus(c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage());
                    }
                }
            }.execute();
        });
    }
}
