package org.tbc.common;

public final class Codes {
    private Codes() {}

    public static final int AUTH_LOGON_SUCCESS = 0x00;
    public static final int AUTH_LOGON_FAILED_BANNED = 0x03;
    public static final int AUTH_LOGON_FAILED_UNKNOWN_ACCOUNT = 0x04;
    public static final int AUTH_LOGON_FAILED_VERSION_INVALID = 0x09;
    public static final int AUTH_LOGON_FAILED_SUSPENDED = 0x0C;
    public static final int AUTH_LOGON_FAILED_FAIL_NOACCESS = 0x0D;

    public static final int AUTH_OK = 0x0C;
    public static final int AUTH_WAIT_QUEUE = 0x1B;
    public static final int AUTH_VERSION_MISMATCH = 0x14;
    public static final int AUTH_UNKNOWN_ACCOUNT = 0x15;
    public static final int AUTH_BANNED = 0x1C;

    public static final int CHAR_CREATE_SUCCESS = 0x2F;
    public static final int CHAR_CREATE_NAME_IN_USE = 0x32;
    public static final int CHAR_CREATE_EXPANSION = 0x39;
    public static final int CHAR_DELETE_SUCCESS = 0x3B;
    public static final int CHAR_DELETE_FAILED = 0x3C;
    public static final int CHAR_DELETE_FAILED_GUILD_LEADER = 0x3E;
    public static final int CHAR_DELETE_FAILED_ARENA_CAPTAIN = 0x3F;

    public static final int SPELL_FAILED_OUT_OF_RANGE = 0x5C;
    public static final int SMSG_CAST_RESULT = 0x130;

    public static final int CMD_AUTH_LOGON_CHALLENGE = 0x00;
    public static final int CMD_AUTH_LOGON_PROOF = 0x01;
    public static final int CMD_AUTH_RECONNECT_CHALLENGE = 0x02;
    public static final int CMD_AUTH_RECONNECT_PROOF = 0x03;
    public static final int CMD_REALM_LIST = 0x10;

    public static final int ACCOUNT_FLAG_PROPASS = 0x00800000;
}
