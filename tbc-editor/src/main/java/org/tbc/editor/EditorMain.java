package org.tbc.editor;

import org.tbc.common.Conf;
import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.persist.CharacterStore;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/** Swing operator tool. Character DB + login names. No SOAP/RA, no world RPC. */
public final class EditorMain {
    private EditorMain() {}

    public static void main(String[] args) throws Exception {
        Path confFile = Path.of(args.length > 0 ? args[0] : "conf/mangosd.conf");
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
