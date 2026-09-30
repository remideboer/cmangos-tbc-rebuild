# Recreate WoW-2.4.3-client\wowme.exe from stock Wow.exe (does not modify Wow.exe).
param(
    [string]$ClientDir = ""
)

$ErrorActionPreference = "Stop"

if (-not $ClientDir) {
    $ClientDir = Join-Path $PSScriptRoot "..\..\WoW-2.4.3-client"
}
$ClientDir = (Resolve-Path $ClientDir).Path
$stock = Join-Path $ClientDir "Wow.exe"
$out = Join-Path $ClientDir "wowme.exe"

if (-not (Test-Path $stock)) {
    throw "Stock Wow.exe not found: $stock"
}

$stockBytes = [IO.File]::ReadAllBytes($stock)
$md5 = [System.Security.Cryptography.MD5]::Create()
$stockHash = [BitConverter]::ToString($md5.ComputeHash($stockBytes)).Replace("-", "").ToLowerInvariant()
Write-Host "stock Wow.exe md5=$stockHash size=$($stockBytes.Length)"

$expected = "57c5c03097103e15f9abe2803aebdc3c"
if ($stockHash -ne $expected) {
    Write-Warning "Stock MD5 is not the known enUS 8606 checksum ($expected). Patches may not apply; aborting."
    throw "Unexpected Wow.exe hash"
}

$b = [byte[]]$stockBytes.Clone()

# Patch A: hash-mismatch jz -> jmp (skip ERROR #131)
$offA = 0x253F85
if ($b[$offA] -ne 0x84 -or $b[$offA + 1] -ne 0xC0 -or $b[$offA + 2] -ne 0x74 -or $b[$offA + 3] -ne 0x1E) {
    throw ("Patch A site mismatch at 0x{0:X}: {1:X2} {2:X2} {3:X2} {4:X2}" -f $offA, $b[$offA], $b[$offA+1], $b[$offA+2], $b[$offA+3])
}
$b[$offA + 2] = 0xEB

# Patch B: 16-byte compare always equal
$offB = 0x253D00
$b[$offB] = 0x33
$b[$offB + 1] = 0xC0
$b[$offB + 2] = 0xC2
$b[$offB + 3] = 0x04
$b[$offB + 4] = 0x00

# Patch C: ARCHIVE auth wrapper force authresult=5 / success
$offC = 0x253B49
$expectC = [byte[]](0x8B, 0x4C, 0x24, 0x38, 0x8A, 0xD8, 0x8B, 0x44, 0x24, 0x28, 0x83, 0xC4, 0x1C, 0x84, 0xDB, 0x89, 0x08, 0x75, 0x0B)
for ($i = 0; $i -lt $expectC.Length; $i++) {
    if ($b[$offC + $i] -ne $expectC[$i]) {
        throw ("Patch C site mismatch at 0x{0:X}+{1}" -f $offC, $i)
    }
}
$patchC = [byte[]](
    0xB9, 0x05, 0x00, 0x00, 0x00,
    0xB3, 0x01,
    0x8B, 0x44, 0x24, 0x28,
    0x83, 0xC4, 0x1C,
    0x89, 0x08,
    0xEB, 0x0B,
    0x90
)
[Array]::Copy($patchC, 0, $b, $offC, $patchC.Length)

[IO.File]::WriteAllBytes($out, $b)
$outHash = [BitConverter]::ToString($md5.ComputeHash($b)).Replace("-", "").ToLowerInvariant()
Write-Host "wrote $out"
Write-Host "wowme.exe md5=$outHash size=$($b.Length)"
Write-Host "Wow.exe left untouched."
