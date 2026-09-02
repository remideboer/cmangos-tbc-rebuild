package org.tbc.admin;

/** Login-DB account row. vHex/sHex are SRP verifier/salt, never a password. */
public record AccountRow(int id, String username, int gmlevel, int expansion, String vHex, String sHex) {
}
