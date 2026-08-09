$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Utf8NoBom($Path, $Text) {
    $dir = Split-Path -Parent $Path
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText((Join-Path $root $Path), $Text, $utf8NoBom)
}

function Upsert-Lang-Key($Path, $Key, $Value) {
    $full = Join-Path $root $Path
    if (!(Test-Path $full)) {
        Write-Utf8NoBom $Path ("{`r`n  `"$Key`": `"$Value`"`r`n}`r`n")
        return
    }

    $text = Get-Content $full -Raw
    if ($text -match '"' + [regex]::Escape($Key) + '"\s*:') {
        $text = [regex]::Replace($text, '"' + [regex]::Escape($Key) + '"\s*:\s*"[^"]*"', '"' + $Key + '": "' + $Value + '"')
    } else {
        $trimmed = $text.TrimEnd()
        if ($trimmed -match '\{\s*\}$') {
            $text = "{`r`n  `"$Key`": `"$Value`"`r`n}`r`n"
        } else {
            $text = [regex]::Replace($trimmed, '\}\s*$', ",`r`n  `"$Key`": `"$Value`"`r`n}`r`n")
        }
    }
    Write-Utf8NoBom $Path $text
}

# 1) Display name: item id is tobacco_leaf_dried, display name is Dried Tobacco Leaf.
Upsert-Lang-Key 'src\main\resources\assets\narcotix\lang\en_us.json' 'item.narcotix.tobacco_leaf_dried' 'Dried Tobacco Leaf'

# 2) Fix dried tobacco smelting output to the real registered item id: narcotix:tobacco_leaf_dried.
$driedRecipe = @'
{
  "type": "minecraft:smelting",
  "ingredient": {
    "item": "narcotix:tobacco_leaf"
  },
  "result": "narcotix:tobacco_leaf_dried",
  "experience": 0.1,
  "cookingtime": 200
}
'@
Write-Utf8NoBom 'src\main\resources\data\narcotix\recipe\dried_tobacco_leaf_from_tobacco_leaf.json' $driedRecipe

# Remove duplicate/older tobacco drying recipe if present to avoid duplicate recipe warnings.
$duplicateDryRecipe = Join-Path $root 'src\main\resources\data\narcotix\recipe\tobacco_leaf_dried_from_tobacco_leaf.json'
if (Test-Path $duplicateDryRecipe) { Remove-Item $duplicateDryRecipe -Force }

# 3) Blunt recipe: 1 trimmed bud + 1 dried tobacco leaf -> blunt.
$bluntRecipe = @'
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {
      "item": "narcotix:trimmed_bud"
    },
    {
      "item": "narcotix:tobacco_leaf_dried"
    }
  ],
  "result": {
    "id": "narcotix:blunt",
    "count": 1
  }
}
'@
Write-Utf8NoBom 'src\main\resources\data\narcotix\recipe\blunt.json' $bluntRecipe

# 4) Cigarette recipe: 1 dried tobacco leaf + 1 paper -> cigarette.
$cigaretteRecipe = @'
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {
      "item": "narcotix:tobacco_leaf_dried"
    },
    {
      "item": "minecraft:paper"
    }
  ],
  "result": {
    "id": "narcotix:cigarette",
    "count": 1
  }
}
'@
Write-Utf8NoBom 'src\main\resources\data\narcotix\recipe\cigarette.json' $cigaretteRecipe

# 5) Natural spawning: register the Wandering Plug as ON_GROUND so natural spawns do not pick water positions.
$entitiesPath = Join-Path $root 'src\main\java\com\example\NarcotixEntities.java'
if (Test-Path $entitiesPath) {
    $text = Get-Content $entitiesPath -Raw

    $imports = @(
        'import net.minecraft.world.entity.SpawnPlacementTypes;',
        'import net.minecraft.world.entity.SpawnPlacements;',
        'import net.minecraft.world.entity.Mob;',
        'import net.minecraft.world.level.levelgen.Heightmap;'
    )

    foreach ($import in $imports) {
        if ($text -notmatch [regex]::Escape($import)) {
            $text = $text -replace '(package\s+com\.example;\s*)', ('$1' + "`r`n" + $import + "`r`n")
        }
    }

    if ($text -notmatch 'SpawnPlacements\.register\s*\(\s*WANDERING_PLUG') {
        $spawnRegistration = @'

        SpawnPlacements.register(
                WANDERING_PLUG,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules
        );
'@
        $pattern = '(BiomeModifications\.addSpawn\([\s\S]*?WANDERING_PLUG[\s\S]*?\);)'
        if ($text -match $pattern) {
            $text = [regex]::Replace($text, $pattern, ('$1' + $spawnRegistration), 1)
        } else {
            Write-Host 'WARNING: Could not find BiomeModifications.addSpawn(... WANDERING_PLUG ...). Natural water-spawn patch was not inserted.'
        }
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($entitiesPath, $text, $utf8NoBom)
} else {
    Write-Host 'WARNING: NarcotixEntities.java not found. Natural water-spawn patch was not inserted.'
}

Write-Host 'Done. Patched tobacco names/recipes and attempted Wandering Plug ON_GROUND spawn placement.'
