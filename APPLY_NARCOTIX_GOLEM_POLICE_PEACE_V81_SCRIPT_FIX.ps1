$ErrorActionPreference = "Stop"
Write-Host "Applying Narcotix iron golem / police peace v81 script fix..."

$copPath = "src\main\java\com\example\CopEntity.java"
if (!(Test-Path $copPath)) { throw "Missing $copPath" }

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$cop = [System.IO.File]::ReadAllText((Resolve-Path $copPath))
Copy-Item $copPath "$copPath.v81.bak" -Force

# Remove imports from failed attempts, then restore the imports that are actually needed.
$cop = $cop -replace '(?m)^import net\.minecraft\.world\.entity\.animal\.IronGolem;\r?\n', ''
$cop = $cop -replace '(?m)^import net\.minecraft\.world\.entity\.EntityType;\r?\n', ''
$cop = $cop -replace '(?m)^import net\.minecraft\.world\.entity\.Mob;\r?\n', ''
$cop = $cop -replace '(?m)^import net\.minecraft\.core\.registries\.BuiltInRegistries;\r?\n', ''

if ($cop -notmatch 'import net\.minecraft\.world\.entity\.EntityType;') {
    $cop = $cop -replace 'package com\.example;\s*', "package com.example;`r`n`r`nimport net.minecraft.world.entity.EntityType;`r`nimport net.minecraft.world.entity.Mob;`r`nimport net.minecraft.core.registries.BuiltInRegistries;`r`n"
}

# Remove the illegal final-method override block inserted by v78/v79.
$cop = [Regex]::Replace(
    $cop,
    '(?s)\r?\n\s*@Override\s*\r?\n\s*public\s+boolean\s+isAlliedTo\s*\([^)]*\)\s*\{.*?\r?\n\s*\}',
    ''
)

# Make sure the bad EntityType.IRON_GOLEM check is gone.
$cop = $cop.Replace('golem.getType() == EntityType.IRON_GOLEM && golem.getTarget() == this', 'BuiltInRegistries.ENTITY_TYPE.getKey(golem.getType()).toString().equals("minecraft:iron_golem") && golem.getTarget() == this')

# Fix any literal PowerShell escaping left by failed scripts.
$cop = $cop.Replace('\"minecraft:iron_golem\"', '"minecraft:iron_golem"')

# Add canBeSeenAsEnemy=false if not present. This helps golems stop considering cops hostile.
if ($cop -notmatch 'canBeSeenAsEnemy\s*\(') {
    $insert = @'

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }
'@
    $lastBrace = $cop.LastIndexOf('}')
    if ($lastBrace -lt 0) { throw "Could not find final class brace in CopEntity.java" }
    $cop = $cop.Substring(0, $lastBrace) + $insert + $cop.Substring($lastBrace)
}

# If an old clearIronGolemPoliceTarget method exists, normalize its predicate.
$golemPredicateOld1 = 'golem -> BuiltInRegistries.ENTITY_TYPE.getKey(golem.getType()).toString().equals("minecraft:iron_golem") && golem.getTarget() == this'
$golemPredicateOld2 = 'golem -> golem.getType() == EntityType.IRON_GOLEM && golem.getTarget() == this'
$golemPredicateNew = 'golem -> BuiltInRegistries.ENTITY_TYPE.getKey(golem.getType()).toString().equals("minecraft:iron_golem") && golem.getTarget() == this'
$cop = $cop.Replace($golemPredicateOld2, $golemPredicateNew)
$cop = $cop.Replace($golemPredicateOld1, $golemPredicateNew)

# If no target-clearing helper exists, add one near the end of the class.
if ($cop -notmatch 'clearIronGolemPoliceTarget\s*\(') {
    $method = @'

    private void clearIronGolemPoliceTarget() {
        if (this.level().isClientSide()) {
            return;
        }

        this.level().getEntitiesOfClass(
                Mob.class,
                this.getBoundingBox().inflate(16.0D),
                golem -> BuiltInRegistries.ENTITY_TYPE.getKey(golem.getType()).toString().equals("minecraft:iron_golem") && golem.getTarget() == this
        ).forEach(golem -> golem.setTarget(null));
    }
'@
    $lastBrace = $cop.LastIndexOf('}')
    if ($lastBrace -lt 0) { throw "Could not find final class brace in CopEntity.java" }
    $cop = $cop.Substring(0, $lastBrace) + $method + $cop.Substring($lastBrace)
}

# Call the helper from tick() if there is a tick override and it is not already called.
if ($cop -notmatch 'clearIronGolemPoliceTarget\s*\(\s*\)\s*;') {
    if ($cop -match 'public\s+void\s+tick\s*\(\s*\)\s*\{') {
        $cop = [Regex]::Replace($cop, '(public\s+void\s+tick\s*\(\s*\)\s*\{)', '$1' + "`r`n        clearIronGolemPoliceTarget();", 1)
    } else {
        $tickMethod = @'

    @Override
    public void tick() {
        super.tick();
        clearIronGolemPoliceTarget();
    }
'@
        $lastBrace = $cop.LastIndexOf('}')
        if ($lastBrace -lt 0) { throw "Could not find final class brace in CopEntity.java" }
        $cop = $cop.Substring(0, $lastBrace) + $tickMethod + $cop.Substring($lastBrace)
    }
}

[System.IO.File]::WriteAllText((Resolve-Path $copPath), $cop, $utf8NoBom)

Write-Host "v81 applied. Removed the illegal isAlliedTo override and fixed the golem registry-id script quoting."
Write-Host "Backup: $copPath.v81.bak"
Write-Host "Next run: .\gradlew.bat clean runClient"
