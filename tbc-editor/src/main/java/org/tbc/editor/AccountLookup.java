package org.tbc.editor;

import java.util.List;

public interface AccountLookup {
    List<AccountRef> list();

    AccountRef findById(int id);
}
