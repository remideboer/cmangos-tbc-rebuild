#!/usr/bin/env python3
"""Generate Opcodes.java and UpdateFields.java from spec YAML."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = ROOT / "spec" / "03-protocol"
OUT = ROOT / "tbc-server" / "tbc-world" / "src" / "main" / "java" / "org" / "tbc" / "world" / "net" / "wow8606"


def parse_opcodes():
    ops = []
    cur = {}
    for line in (SPEC / "opcodes.yaml").read_text(encoding="utf-8").splitlines():
        if line.startswith("  - id:"):
            if cur.get("name"):
                ops.append(cur)
            cur = {"id": line.split(":", 1)[1].strip()}
        elif line.startswith("    name:") and cur is not None:
            cur["name"] = line.split(":", 1)[1].strip()
        elif line.startswith("    direction:") and cur is not None:
            cur["direction"] = line.split(":", 1)[1].strip()
        elif line.startswith("    session_status:") and cur is not None:
            cur["session"] = line.split(":", 1)[1].strip()
        elif line.startswith("    spec_status:") and cur is not None:
            cur["status"] = line.split(":", 1)[1].strip()
    if cur.get("name"):
        ops.append(cur)
    return ops


def write_opcodes(ops):
    lines = [
        "package org.tbc.world.net.wow8606;",
        "",
        "/** Generated from spec/03-protocol/opcodes.yaml. Do not invent ids. */",
        "public final class Opcodes {",
        "    private Opcodes() {}",
        "    public static final int NUM_MSG_TYPES = 0x424;",
    ]
    for o in ops:
        name = o["name"]
        if not re.match(r"^[A-Z][A-Z0-9_]*$", name):
            continue
        lines.append(f"    public static final int {name} = {o['id']};")
    lines += [
        "",
        "    private static final String[] NAMES = new String[NUM_MSG_TYPES];",
        "    static {",
    ]
    for o in ops:
        name = o["name"]
        if not re.match(r"^[A-Z][A-Z0-9_]*$", name):
            continue
        lines.append(f"        if ({name} < NUM_MSG_TYPES) {{ NAMES[{name}] = \"{name}\"; }}")
    lines += [
        "    }",
        "    public static String name(int id) {",
        "        if (id < 0 || id >= NAMES.length || NAMES[id] == null) {",
        "            return \"UNK_\" + Integer.toHexString(id);",
        "        }",
        "        return NAMES[id];",
        "    }",
        "    public static boolean valid(int id) {",
        "        return id >= 0 && id < NUM_MSG_TYPES;",
        "    }",
        "}",
        "",
    ]
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "Opcodes.java").write_text("\n".join(lines), encoding="utf-8")
    print("opcodes", len(ops))


def parse_fields():
    fields = []
    ends = {}
    cur = {}
    in_ends = False
    for line in (SPEC / "update-fields.yaml").read_text(encoding="utf-8").splitlines():
        if line.startswith("ends:"):
            in_ends = True
            continue
        if line.startswith("field_count:") or line.startswith("fields:"):
            in_ends = False
        if in_ends and line.startswith("  ") and ":" in line and "_hex" not in line:
            k, v = line.strip().split(":", 1)
            ends[k] = int(v.strip())
        if line.startswith("  - name:"):
            if cur.get("name"):
                fields.append(cur)
            cur = {"name": line.split(":", 1)[1].strip()}
        elif line.startswith("    offset:") and "offset_hex" not in line:
            cur["offset"] = int(line.split(":", 1)[1].strip())
        elif line.startswith("    size:"):
            cur["size"] = int(line.split(":", 1)[1].strip())
        elif line.startswith("    flags:"):
            cur["flags"] = line.split(":", 1)[1].strip()
        elif line.startswith("    object:"):
            cur["object"] = line.split(":", 1)[1].strip()
    if cur.get("name"):
        fields.append(cur)
    return ends, fields


def vis_expr(flags):
    parts = []
    f = flags or ""
    if "PUBLIC" in f:
        parts.append("PUBLIC")
    if "PRIVATE" in f:
        parts.append("PRIVATE")
    if "OWNER_ONLY" in f:
        parts.append("OWNER_ONLY")
    if "GROUP_ONLY" in f or "PARTY_ONLY" in f:
        parts.append("GROUP_ONLY")
    if "DYNAMIC" in f:
        parts.append("DYNAMIC")
    return " | ".join(parts) if parts else "0"


def write_fields(ends, fields):
    lines = [
        "package org.tbc.world.net.wow8606;",
        "",
        "/** Generated from spec/03-protocol/update-fields.yaml. PLAYER_END is 1592. */",
        "public final class UpdateFields {",
        "    private UpdateFields() {}",
        "    public static final int PUBLIC = 1;",
        "    public static final int PRIVATE = 2;",
        "    public static final int OWNER_ONLY = 4;",
        "    public static final int GROUP_ONLY = 32;",
        "    public static final int DYNAMIC = 64;",
    ]
    for k, v in ends.items():
        lines.append(f"    public static final int {k} = {v};")
    vis_by_offset = {}
    for f in fields:
        name = f["name"]
        if not re.match(r"^[A-Z][A-Z0-9_]*$", name):
            continue
        lines.append(f"    public static final int {name} = {f['offset']};")
        vis_by_offset[f["offset"]] = vis_expr(f.get("flags", ""))
    lines += [
        "",
        "    public static int visibility(int offset) {",
        "        return switch (offset) {",
    ]
    for off, expr in sorted(vis_by_offset.items()):
        lines.append(f"            case {off} -> {expr};")
    lines += [
        "            default -> PUBLIC;",
        "        };",
        "    }",
        "}",
        "",
    ]
    (OUT / "UpdateFields.java").write_text("\n".join(lines), encoding="utf-8")
    print("fields", len(fields), "PLAYER_END", ends.get("PLAYER_END"))


def is_client_command(o):
    name = o.get("name", "")
    if not re.match(r"^[A-Z][A-Z0-9_]*$", name):
        return False
    if name.startswith("SMSG_") or name.startswith("UMSG_"):
        return False
    direction = o.get("direction", "")
    if direction == "c2s":
        return True
    return direction == "both" and (name.startswith("CMSG_") or name.startswith("MSG_"))


def write_client_commands(ops):
    out_dir = ROOT / "tbc-server" / "tbc-tests" / "src" / "test" / "java" / "org" / "tbc" / "matrix"
    out_dir.mkdir(parents=True, exist_ok=True)
    clients = [o for o in ops if is_client_command(o)]
    lines = [
        "package org.tbc.matrix;",
        "",
        "import java.util.List;",
        "",
        "/** Generated from spec/03-protocol/opcodes.yaml. Do not invent ids. */",
        "public final class ClientCommands {",
        "    private ClientCommands() {}",
        "",
        "    public record Op(int id, String name, String direction, String sessionStatus, String specStatus) {}",
        "",
        "    public record Auth(int id, String name) {}",
        "",
        "    public static final Auth[] AUTH = {",
        "        new Auth(0x00, \"CMD_AUTH_LOGON_CHALLENGE\"),",
        "        new Auth(0x01, \"CMD_AUTH_LOGON_PROOF\"),",
        "        new Auth(0x02, \"CMD_AUTH_RECONNECT_CHALLENGE\"),",
        "        new Auth(0x03, \"CMD_AUTH_RECONNECT_PROOF\"),",
        "        new Auth(0x10, \"CMD_REALM_LIST\")",
        "    };",
        "",
        "    /** Unused patch transfer. Do not implement (vision-Out). */",
        "    public static final Auth[] AUTH_XFER = {",
        "        new Auth(0x30, \"CMD_XFER_30\"),",
        "        new Auth(0x31, \"CMD_XFER_31\"),",
        "        new Auth(0x32, \"CMD_XFER_32\"),",
        "        new Auth(0x33, \"CMD_XFER_33\"),",
        "        new Auth(0x34, \"CMD_XFER_34\")",
        "    };",
        "",
        "    public static final Op[] WORLD = {",
    ]
    for o in clients:
        oid = o["id"]
        name = o["name"]
        direction = o.get("direction", "")
        session = o.get("session", "")
        status = o.get("status", "")
        lines.append(f'        new Op({oid}, "{name}", "{direction}", "{session}", "{status}"),')
    if clients:
        lines[-1] = lines[-1].rstrip(",")
    lines += [
        "    };",
        "",
        "    public static List<Op> world() {",
        "        return List.of(WORLD);",
        "    }",
        "}",
        "",
    ]
    (out_dir / "ClientCommands.java").write_text("\n".join(lines), encoding="utf-8")
    print("client commands", len(clients))


if __name__ == "__main__":
    ops = parse_opcodes()
    write_opcodes(ops)
    write_client_commands(ops)
    write_fields(*parse_fields())
