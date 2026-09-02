#!/usr/bin/env python3
"""Collect ScriptName strings from spec/05-domain/scripts for the Java registry."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "spec" / "05-domain" / "scripts"
OUT = ROOT / "tbc-server" / "tbc-world" / "src" / "main" / "java" / "org" / "tbc" / "world" / "script" / "ScriptNames.java"

names = set()
for p in SCRIPTS.rglob("*.md"):
    text = p.read_text(encoding="utf-8", errors="replace")
    for m in re.finditer(r"`((?:boss|instance|mob|npc|go|spell|world|item)_[a-z0-9_]+)`", text):
        names.add(m.group(1))
    for m in re.finditer(r"ScriptNames?:\s*`([^`]+)`", text):
        for part in m.group(1).split(","):
            n = part.strip().strip("`")
            if n:
                names.add(n)

# class spell script keys
cls = ROOT / "spec" / "05-domain" / "class-spell-scripts.md"
if cls.exists():
    for m in re.finditer(r"`(spell_[a-z0-9_]+)`", cls.read_text(encoding="utf-8")):
        names.add(m.group(1))

ordered = sorted(names)
lines = [
    "package org.tbc.world.script;",
    "",
    "/** ScriptName strings from spec/05-domain/scripts and class-spell-scripts.md. */",
    "public final class ScriptNames {",
    "    private ScriptNames() {}",
    "    public static final String[] ALL = {",
]
for n in ordered:
    lines.append(f'        "{n}",')
lines += [
    "    };",
    "}",
    "",
]
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(lines), encoding="utf-8")
print("script names", len(ordered))
