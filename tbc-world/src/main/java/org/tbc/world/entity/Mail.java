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
    public long deliverTime;
    public long expireTime;
    public final List<Item> items = new ArrayList<>();
}
