package org.tbc.admin;

import org.tbc.common.Conf;
import org.tbc.common.DbPool;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/** Swing operator tool. Login DB only; no SOAP/RA, no world RPC. */
public final class AdminMain {
    private AdminMain() {}

    public static void main(String[] args) throws Exception {
        Path confFile = Path.of(args.length > 0 ? args[0] : "conf/realmd.conf");
        Conf conf = Conf.load(confFile, "Realmd_");
        DbPool db = new DbPool(conf.db("LoginDatabaseInfo"), "admin-login");
        AccountService service = new AccountService(new JdbcAccountRepository(db));
        SwingUtilities.invokeLater(() -> {
            AccountFrame frame = new AccountFrame(service);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    db.close();
                }
            });
            frame.setVisible(true);
        });
    }
}
