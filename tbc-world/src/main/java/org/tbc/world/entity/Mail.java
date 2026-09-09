package org.tbc.world.entity;

import java.util.ArrayList;
import java.util.List;

public final class Mail {
    public int id;
    public int sender;
    public int receiver;
    public String subject = "";
    public String body = "";
    public int money;
    public int cod;
    public int checked;
    public int stationery = 41;
    public int state;
    public long deliverTime;
    public long expireTime;
    public final List<Item> items = new ArrayList<>();

    public static final int MAIL_STATE_CHANGED = 2;
    public static final int MAIL_STATE_DELETED = 3;
    public static final int MAIL_CHECK_MASK_READ = 0x01;
}
