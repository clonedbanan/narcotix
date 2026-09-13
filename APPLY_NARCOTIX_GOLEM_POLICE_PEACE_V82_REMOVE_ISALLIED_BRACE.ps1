$ErrorActionPreference = 'Stop'
Write-Host "Applying Narcotix iron golem / police peace v82 hard remove fix..."

$path = "src\main\java\com\example\CopEntity.java"
if (!(Test-Path $path)) { throw "Missing file: $path" }

$backup = "$path.v82.bak"
Copy-Item $path $backup -Force

$text = [System.IO.File]::ReadAllText((Resolve-Path $path))

# Remove any illegal isAlliedTo override block by locating the method and brace-matching it.
$methodMatch = [regex]::Match($text, '(?s)\r?\n\s*@Override\s*\r?\n\s*public\s+boolean\s+isAlliedTo\s*\([^)]*\)\s*\{')
if (!$methodMatch.Success) {
    $methodMatch = [regex]::Match($text, '(?s)\r?\n\s*public\s+boolean\s+isAlliedTo\s*\([^)]*\)\s*\{')
}

if ($methodMatch.Success) {
    $start = $methodMatch.Index
    $braceIndex = $text.IndexOf('{', $methodMatch.Index)
    if ($braceIndex -lt 0) { throw "Found isAlliedTo method but could not find opening brace." }

    $depth = 0
    $end = -1
    for ($i = $braceIndex; $i -lt $text.Length; $i++) {
        $ch = $text[$i]
        if ($ch -eq '{') { $depth++ }
        elseif ($ch -eq '}') {
            $depth--
            if ($depth -eq 0) {
                $end = $i + 1
                break
            }
        }
    }
    if ($end -lt 0) { throw "Could not find end of isAlliedTo method block." }

    while ($end -lt $text.Length -and ($text[$end] -eq "`r" -or $text[$end] -eq "`n")) { $end++ }
    $text = $text.Remove($start, $end - $start)
    Write-Host "Removed illegal isAlliedTo override."
} else {
    Write-Host "No isAlliedTo override found. Continuing."
}

# Make sure EntityType import exists for the constructor.
if ($text -notmatch 'import\s+net\.minecraft\.world\.entity\.EntityType\s*;') {
    $text = $text -replace '(package\s+com\.example;\s*)', "`$1`r`nimport net.minecraft.world.entity.EntityType;`r`n"
}

# Remove bad IronGolem import if any earlier patch added it.
$text = $text -replace '(?m)^import\s+net\.minecraft\.world\.entity\.animal\.IronGolem;\s*\r?\n', ''

# Remove unused/broken EntityType.IRON_GOLEM references if still present.
$text = $text -replace 'golem\.getType\(\)\s*==\s*EntityType\.IRON_GOLEM\s*&&\s*golem\.getTarget\(\)\s*==\s*this', 'BuiltInRegistries.ENTITY_TYPE.getKey(golem.getType()).toString().equals("minecraft:iron_golem") && golem.getTarget() == this'

# Make sure BuiltInRegistries import exists if the registry-id check is in file.
if ($text -match 'BuiltInRegistries\.ENTITY_TYPE' -and $text -notmatch 'import\s+net\.minecraft\.core\.registries\.BuiltInRegistries\s*;') {
    $text = $text -replace '(package\s+com\.example;\s*)', "`$1`r`nimport net.minecraft.core.registries.BuiltInRegistries;`r`n"
}

# Add canBeSeenAsEnemy override if it is not already there. This compiles in the current mappings based on prior patches.
if ($text -notmatch 'boolean\s+canBeSeenAsEnemy\s*\(') {
    $insert = @'

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }
'@
    $lastBrace = $text.LastIndexOf('}')
    if ($lastBrace -lt 0) { throw "Could not find final class brace." }
    $text = $text.Insert($lastBrace, $insert)
    Write-Host "Added canBeSeenAsEnemy=false override."
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $path), $text, $utf8NoBom)

Write-Host "v82 applied. Backup: $backup"
Write-Host "Next run: .\gradlew.bat clean runClient"
