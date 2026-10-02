$ErrorActionPreference = 'Stop'
Write-Host "Applying Narcotix bagging v159 robust bagged bud fix..."

$root = Get-Location
$mod = "src\main\java\com\example\NarcotixMod.java"
if (!(Test-Path $mod)) { throw "Could not find $mod. Run this script from the Narcotix project root." }
Copy-Item $mod "$mod.v159.bak" -Force
Write-Host "Backup: $mod.v159.bak"

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$text = [System.IO.File]::ReadAllText((Resolve-Path $mod), [System.Text.Encoding]::UTF8)

# Add BAGGED_BUD item registration if missing. Prefer placing near other bagged items.
if ($text -notmatch '\bBAGGED_BUD\b') {
    $line = '    public static final Item BAGGED_BUD = registerItem("bagged_bud", Item::new);'
    $patterns = @(
        '(?m)^\s*public\s+static\s+final\s+Item\s+BAGGED_ACID\s*=.*?;\s*$',
        '(?m)^\s*public\s+static\s+final\s+Item\s+BAGGED_JOINT\s*=.*?;\s*$',
        '(?m)^\s*public\s+static\s+final\s+Item\s+BAGGIE\s*=.*?;\s*$'
    )
    $inserted = $false
    foreach ($p in $patterns) {
        if ($text -match $p) {
            $text = [System.Text.RegularExpressions.Regex]::Replace($text, $p, { param($m) $m.Value + [Environment]::NewLine + $line }, 1)
            $inserted = $true
            break
        }
    }
    if (!$inserted) {
        $marker = 'public static final String MOD_ID'
        if ($text -match $marker) {
            $idx = $text.IndexOf($marker)
            $lineEnd = $text.IndexOf([Environment]::NewLine, $idx)
            $text = $text.Insert($lineEnd + [Environment]::NewLine.Length, $line + [Environment]::NewLine)
            $inserted = $true
        }
    }
    if (!$inserted) { throw "Could not insert BAGGED_BUD into NarcotixMod.java automatically." }
    Write-Host "Inserted BAGGED_BUD item registration."
} else {
    Write-Host "BAGGED_BUD already exists."
}

# Ensure creative tab output gets bagged_bud if there is a known pattern.
if ($text -notmatch 'output\.accept\(BAGGED_BUD\)' -and $text -notmatch 'entries\.accept\(BAGGED_BUD\)' -and $text -notmatch 'BAGGED_BUD\)') {
    # Try to add after BAGGED_ACID creative entry if present.
    $text = [System.Text.RegularExpressions.Regex]::Replace($text, '(?m)^(\s*(?:output|entries)\.accept\(BAGGED_ACID\);\s*)$', { param($m) $m.Value + [Environment]::NewLine + ($m.Groups[1].Value -replace 'BAGGED_ACID','BAGGED_BUD') }, 1)
}

[System.IO.File]::WriteAllText((Resolve-Path $mod), $text, $utf8NoBom)

# Asset paths
$texDir = "src\main\resources\assets\narcotix\textures\item"
$modelDir = "src\main\resources\assets\narcotix\models\item"
$itemDefDir = "src\main\resources\assets\narcotix\items"
$recipeDir = "src\main\resources\data\narcotix\recipe"
$langPath = "src\main\resources\assets\narcotix\lang\en_us.json"
New-Item -ItemType Directory -Force $texDir,$modelDir,$itemDefDir,$recipeDir | Out-Null

# Copy embedded bagged_bud.png if present beside script, otherwise preserve existing.
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcPng = Join-Path $scriptDir "bagged_bud.png"
$dstPng = Join-Path $texDir "bagged_bud.png"
if (Test-Path $srcPng) {
    Copy-Item $srcPng $dstPng -Force
    Write-Host "Wrote texture: $dstPng"
} elseif (Test-Path $dstPng) {
    Write-Host "Keeping existing texture: $dstPng"
} else {
    Write-Warning "No bagged_bud.png found beside script or in $texDir. Add the texture manually."
}

$modelJson = @'
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "narcotix:item/bagged_bud"
  }
}
'@
[System.IO.File]::WriteAllText((Join-Path $modelDir "bagged_bud.json"), $modelJson, $utf8NoBom)

$itemDefJson = @'
{
  "model": {
    "type": "minecraft:model",
    "model": "narcotix:item/bagged_bud"
  }
}
'@
[System.IO.File]::WriteAllText((Join-Path $itemDefDir "bagged_bud.json"), $itemDefJson, $utf8NoBom)

# Write multiple recipe variants in case the actual bud ID differs.
$recipes = @(
    @{ name = "bagged_bud_from_trimmed_bud"; bud = "narcotix:trimmed_bud" },
    @{ name = "bagged_bud_from_bud"; bud = "narcotix:bud" },
    @{ name = "bagged_bud_from_weed_bud"; bud = "narcotix:weed_bud" }
)
foreach ($r in $recipes) {
    $json = @"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    "narcotix:baggie",
    "$($r.bud)"
  ],
  "result": {
    "id": "narcotix:bagged_bud",
    "count": 1
  }
}
"@
    [System.IO.File]::WriteAllText((Join-Path $recipeDir ($r.name + ".json")), $json, $utf8NoBom)
}
Write-Host "Wrote bagged_bud recipe variants for trimmed_bud/bud/weed_bud."

# Lang entry using ConvertFrom/To-Json if possible.
if (!(Test-Path $langPath)) {
    New-Item -ItemType Directory -Force (Split-Path $langPath -Parent) | Out-Null
    [System.IO.File]::WriteAllText($langPath, "{}", $utf8NoBom)
}
try {
    $langObj = Get-Content $langPath -Raw | ConvertFrom-Json -AsHashtable
    $langObj["item.narcotix.bagged_bud"] = "Bagged Bud"
    $out = $langObj | ConvertTo-Json -Depth 20
    [System.IO.File]::WriteAllText((Resolve-Path $langPath), $out, $utf8NoBom)
    Write-Host "Updated lang entry."
} catch {
    Write-Warning "Could not update lang JSON automatically: $($_.Exception.Message)"
}

Write-Host "v159 applied. Next run: .\gradlew.bat clean runClient"
