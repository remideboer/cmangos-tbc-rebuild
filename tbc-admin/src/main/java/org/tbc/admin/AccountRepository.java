package org.tbc.admin;

import java.util.List;

public interface AccountRepository {
    List<AccountRow> list();

    AccountRow findById(int id);

    AccountRow findByUsername(String username);

    int insert(String username, String vHex, String sHex, int gmlevel, int expansion);

    void updateGmLevel(int id, int gmlevel);

    void updateVerifier(int id, String vHex, String sHex);

    void delete(int id);
}
