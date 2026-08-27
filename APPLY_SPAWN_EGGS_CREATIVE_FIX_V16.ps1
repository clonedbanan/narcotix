$ErrorActionPreference = "Stop"
Write-Host "Applying spawn egg creative/search fix v16..."

$root = Get-Location
$copPath = Join-Path $root "src\main\java\com\example\NarcotixCopAdditions.java"

if (!(Test-Path $copPath)) {
    throw "Could not find NarcotixCopAdditions.java at $copPath"
}

$text = Get-Content $copPath -Raw

# Remove the bad v15 Fabric API import and any duplicate old creative-tab imports.
$text = $text -replace '(?m)^import\s+net\.fabricmc\.fabric\.api\.itemgroup\.v1\.ItemGroupEvents;\s*\r?\n', ''
$text = $text -replace '(?m)^import\s+net\.fabricmc\.fabric\.api\.itemgroup\.v1\.CreativeModeTabEvents;\s*\r?\n', ''
$text = $text -replace '(?m)^import\s+net\.fabricmc\.fabric\.api\.creativetab\.v1\.CreativeModeTabEvents;\s*\r?\n', ''

# Make sure the correct 26.2 creative tab event import exists.
if ($text -notmatch 'net\.fabricmc\.fabric\.api\.creativetab\.v1\.CreativeModeTabEvents') {
    $text = $text -replace '(?m)(^package\s+com\.example;\s*\r?\n)', "`$1`r`nimport net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;`r`n"
}

# Remove the bad v15 ItemGroupEvents block.
$text = [regex]::Replace(
    $text,
    '(?s)\s*ItemGroupEvents\.modifyEntriesEvent\(CreativeModeTabs\.SPAWN_EGGS\)\.register\(entries\s*->\s*\{.*?\}\);',
    ''
)

# Remove any earlier v16/v15 spawn-egg creative block so rerunning the script stays clean.
$text = [regex]::Replace(
    $text,
    '(?s)\s*CreativeModeTabEvents\.modifyOutputEvent\(CreativeModeTabs\.SPAWN_EGGS\)\.register\(creativeTab\s*->\s*\{.*?\}\);',
    ''
)

$block = @'

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(creativeTab -> {
            creativeTab.accept(COP_SPAWN_EGG);
            creativeTab.accept(WANDERING_PLUG_SPAWN_EGG);
        });
'@

# Add the correct block inside register(), right before the first closing brace after the known attribute registration when possible.
if ($text -match 'FabricDefaultAttributeRegistry\.register\(COP,\s*CopEntity\.createAttributes\(\)\);') {
    $text = [regex]::Replace(
        $text,
        '(FabricDefaultAttributeRegistry\.register\(COP,\s*CopEntity\.createAttributes\(\)\);)',
        "`$1$block",
        1
    )
}
elseif ($text -match 'public\s+static\s+void\s+register\s*\(\s*\)\s*\{') {
    $text = [regex]::Replace(
        $text,
        '(public\s+static\s+void\s+register\s*\(\s*\)\s*\{)',
        "`$1$block",
        1
    )
}
else {
    throw "Could not find NarcotixCopAdditions.register() to patch."
}

# Fix any BOM or mojibake at the beginning.
$text = $text.TrimStart([char]0xFEFF)
$text = $text -replace '^ï»¿', ''
$text = $text -replace '^∩╗┐', ''

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $copPath), $text, $utf8NoBom)

Write-Host "Spawn egg creative/search fix v16 applied. Now run: .\gradlew.bat clean runClient"
