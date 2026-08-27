$ErrorActionPreference = "Stop"

Write-Host "Applying spawn egg creative/search fix v15..."

function Write-Utf8NoBom($Path, $Text) {
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText((Resolve-Path $Path), $Text, $utf8NoBom)
}

function Clean-BomText($Text) {
    $Text = $Text.TrimStart([char]0xFEFF)
    $Text = $Text -replace '^ï»¿', ''
    $Text = $Text -replace '^∩╗┐', ''
    return $Text
}

$copAdditionsPath = "src\main\java\com\example\NarcotixCopAdditions.java"
if (!(Test-Path $copAdditionsPath)) {
    throw "Could not find $copAdditionsPath"
}

$text = Get-Content $copAdditionsPath -Raw
$text = Clean-BomText $text

# Remove the old wrong creative-tab API import if it exists.
$text = $text -replace "(?m)^import\s+net\.fabricmc\.fabric\.api\.itemgroup\.v1\.CreativeModeTabEvents;\s*\r?\n", ""

# Add the correct Fabric creative-tab API import.
if ($text -notmatch "import\s+net\.fabricmc\.fabric\.api\.itemgroup\.v1\.ItemGroupEvents;") {
    $text = $text -replace "(?m)^(package\s+com\.example;\s*\r?\n)", "`$1`r`nimport net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;`r`n"
}

# Add the vanilla creative tab constants import if needed.
if ($text -notmatch "import\s+net\.minecraft\.world\.item\.CreativeModeTabs;") {
    $text = $text -replace "(?m)^import\s+net\.minecraft\.world\.item\.Item;\s*\r?\n", "import net.minecraft.world.item.Item;`r`nimport net.minecraft.world.item.CreativeModeTabs;`r`n"
    if ($text -notmatch "import\s+net\.minecraft\.world\.item\.CreativeModeTabs;") {
        $text = $text -replace "(?m)^(package\s+com\.example;\s*\r?\n)", "`$1`r`nimport net.minecraft.world.item.CreativeModeTabs;`r`n"
    }
}

$spawnEggTabCode = @'
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(COP_SPAWN_EGG);
            entries.accept(WANDERING_PLUG_SPAWN_EGG);
        });

'@

# Put both custom spawn eggs in the real Spawn Eggs creative tab.
# Once an item is in a creative tab, it also becomes findable through the creative Search tab.
if ($text -notmatch "CreativeModeTabs\.SPAWN_EGGS") {
    $text = $text -replace "public\s+static\s+void\s+register\s*\(\s*\)\s*\{", "public static void register() {`r`n$spawnEggTabCode"
}

Write-Utf8NoBom $copAdditionsPath $text

# Add/repair display names so searching 'cop', 'spawn', or 'wandering plug' works nicely.
$langPath = "src\main\resources\assets\narcotix\lang\en_us.json"
if (Test-Path $langPath) {
    $lang = Get-Content $langPath -Raw
    $lang = Clean-BomText $lang
    $lang = $lang.TrimEnd()

    function Add-LangEntry($Content, $Key, $Value) {
        $escapedKey = [Regex]::Escape($Key)
        if ($Content -match '"' + $escapedKey + '"\s*:') {
            return $Content
        }

        if ($Content -match "\{\s*\}\s*$") {
            return $Content -replace "\{\s*\}\s*$", "{`r`n  `"$Key`": `"$Value`"`r`n}"
        }

        return $Content -replace "\s*}\s*$", ",`r`n  `"$Key`": `"$Value`"`r`n}"
    }

    $lang = Add-LangEntry $lang "item.narcotix.cop_spawn_egg" "Cop Spawn Egg"
    $lang = Add-LangEntry $lang "item.narcotix.wandering_plug_spawn_egg" "Wandering Plug Spawn Egg"
    $lang = Add-LangEntry $lang "entity.narcotix.cop" "Cop"
    $lang = Add-LangEntry $lang "entity.narcotix.wandering_plug" "Wandering Plug"

    Write-Utf8NoBom $langPath $lang
} else {
    Write-Host "Warning: $langPath was not found, so names were not patched."
}

Write-Host "Spawn egg creative/search fix v15 applied. Now run: .\gradlew.bat clean runClient"
