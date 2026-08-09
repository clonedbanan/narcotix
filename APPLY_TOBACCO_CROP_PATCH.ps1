$ErrorActionPreference = "Stop"

$modPath = "src\main\java\com\example\NarcotixMod.java"
if (!(Test-Path $modPath)) {
    throw "Could not find $modPath. Run this from the Narcotix project folder."
}

$text = Get-Content $modPath -Raw

# Remove BOM/corrupted BOM if present.
$text = $text.TrimStart([char]0xFEFF)
$text = $text -replace '^ï»¿', ''
$text = $text -replace '^∩╗┐', ''

# Add the tobacco crop block before the cocaine seeds item block if it is not already present.
if ($text -notmatch 'TOBACCO_CROP') {
    $cropBlock = @'

    public static final Block TOBACCO_CROP = registerBlockNoItem(
            "tobacco_crop",
            properties -> new TobaccoCropBlock(properties
                    .noCollision()
                    .randomTicks()
                    .instabreak()
            ),
            BlockBehaviour.Properties.of()
    );
'@

    $marker = 'public static final Item COCAINE_SEEDS = registerItem('
    if ($text.Contains($marker)) {
        $text = $text.Replace($marker, $cropBlock + "`r`n" + $marker)
    } else {
        $marker = 'public static final Item WEED_SEEDS = registerItem('
        $text = $text.Replace($marker, $cropBlock + "`r`n" + $marker)
    }
}

# If an item-only TOBACCO_SEEDS was added earlier, replace it with the plantable BlockItem version.
$seedPattern = '(?s)public\s+static\s+final\s+Item\s+TOBACCO_SEEDS\s*=\s*registerItem\s*\(\s*"tobacco_seeds"\s*,.*?\);'
$seedBlock = @'
public static final Item TOBACCO_SEEDS = registerItem(
        "tobacco_seeds",
        properties -> new BlockItem(TOBACCO_CROP, properties)
);
'@

if ($text -match $seedPattern) {
    $text = [regex]::Replace($text, $seedPattern, $seedBlock, 1)
} else {
    $marker = 'public static final Item WEED_SEEDS = registerItem('
    if ($text.Contains($marker)) {
        $text = $text.Replace($marker, $seedBlock + "`r`n" + $marker)
    } else {
        $marker = 'public static final Item COCAINE_SEEDS = registerItem('
        $text = $text.Replace($marker, $seedBlock + "`r`n" + $marker)
    }
}

# Add tobacco leaf items if missing.
if ($text -notmatch 'TOBACCO_LEAF') {
    $leafBlock = @'

public static final Item TOBACCO_LEAF = registerItem(
        "tobacco_leaf",
        Item::new
);

public static final Item TOBACCO_LEAF_DRY = registerItem(
        "tobacco_leaf_dried",
        Item::new
);
'@
    $marker = 'public static final Item COKE_BRICK = registerItem('
    if ($text.Contains($marker)) {
        $text = $text.Replace($marker, $leafBlock + "`r`n" + $marker)
    } else {
        $marker = 'public static final Item JOINT = registerItem('
        $text = $text.Replace($marker, $leafBlock + "`r`n" + $marker)
    }
}

# Add creative inventory entries if missing.
if ($text -notmatch 'entries\.accept\(TOBACCO_SEEDS\)') {
    $text = $text.Replace('entries.accept(COCAINE_SEEDS);', "entries.accept(COCAINE_SEEDS);`r`n            entries.accept(TOBACCO_SEEDS);")
}
if ($text -notmatch 'entries\.accept\(TOBACCO_LEAF\)') {
    $text = $text.Replace('entries.accept(COKE_LEAF);', "entries.accept(COKE_LEAF);`r`n            entries.accept(TOBACCO_LEAF);`r`n            entries.accept(TOBACCO_LEAF_DRY);")
}

# Save UTF-8 without BOM.
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $modPath), $text, $utf8NoBom)

# Patch language file.
$langPath = "src\main\resources\assets\narcotix\lang\en_us.json"
if (Test-Path $langPath) {
    $lang = Get-Content $langPath -Raw
    if ($lang -notmatch 'item\.narcotix\.tobacco_seeds') {
        $lang = $lang -replace '\n\}', ',
  "item.narcotix.tobacco_seeds": "Tobacco Seeds",
  "item.narcotix.tobacco_leaf": "Tobacco Leaf",
  "item.narcotix.tobacco_leaf_dried": "Dried Tobacco",
  "block.narcotix.tobacco_crop": "Tobacco Plant"
}'
        [System.IO.File]::WriteAllText((Resolve-Path $langPath), $lang, $utf8NoBom)
    }
}

# Patch village loot, if that helper exists.
$lootPath = "src\main\java\com\example\NarcotixVillageLoot.java"
if (Test-Path $lootPath) {
    $loot = Get-Content $lootPath -Raw
    if ($loot -notmatch 'TOBACCO_SEEDS') {
        $loot = $loot.Replace('tableBuilder.withPool(seedPool(NarcotixMod.COCAINE_SEEDS, 2, 1.0F, 3.0F));', 'tableBuilder.withPool(seedPool(NarcotixMod.COCAINE_SEEDS, 2, 1.0F, 3.0F));' + "`r`n            tableBuilder.withPool(seedPool(NarcotixMod.TOBACCO_SEEDS, 2, 1.0F, 3.0F));")
        $loot = $loot.Replace('tableBuilder.withPool(seedPool(NarcotixMod.COCAINE_SEEDS, 2, 1.0F, 3.0F));', 'tableBuilder.withPool(seedPool(NarcotixMod.COCAINE_SEEDS, 2, 1.0F, 3.0F));' + "`r`n            tableBuilder.withPool(seedPool(NarcotixMod.TOBACCO_SEEDS, 2, 1.0F, 3.0F));")
        [System.IO.File]::WriteAllText((Resolve-Path $lootPath), $loot, $utf8NoBom)
    }
}

# Patch wandering plug trades, if the mob exists.
$plugPath = "src\main\java\com\example\WanderingPlugEntity.java"
if (Test-Path $plugPath) {
    $plug = Get-Content $plugPath -Raw
    if ($plug -notmatch 'TOBACCO_SEEDS') {
        $plug = $plug.Replace('offers.add(sell(NarcotixMod.COCAINE_SEEDS, 2, 3, 18));', 'offers.add(sell(NarcotixMod.COCAINE_SEEDS, 2, 3, 18));' + "`r`n        offers.add(sell(NarcotixMod.TOBACCO_SEEDS, 1, 4, 24));")
        [System.IO.File]::WriteAllText((Resolve-Path $plugPath), $plug, $utf8NoBom)
    }
}

Write-Host "Patched Narcotix with plantable tobacco crop, leaves, dried tobacco, recipes, loot, and plug trade."
Write-Host "Now run: .\gradlew.bat clean runClient"
