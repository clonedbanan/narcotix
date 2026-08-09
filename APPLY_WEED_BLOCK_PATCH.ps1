$ErrorActionPreference = "Stop"

$root = Get-Location
$modPath = Join-Path $root "src\main\java\com\example\NarcotixMod.java"

if (!(Test-Path $modPath)) {
    throw "Could not find $modPath. Run this from the Narcotix project root."
}

$text = Get-Content $modPath -Raw
$text = $text.TrimStart([char]0xFEFF)
$text = $text -replace '^ï»¿', ''
$text = $text -replace '^∩╗┐', ''

function Add-Import {
    param([string]$ImportLine)
    if ($script:text -notmatch [regex]::Escape($ImportLine)) {
        $script:text = $script:text -replace '(package com\.example;\s*)', "`$1`r`n$ImportLine`r`n"
    }
}

Add-Import 'import java.util.function.Function;'
Add-Import 'import net.minecraft.core.Registry;'
Add-Import 'import net.minecraft.core.registries.BuiltInRegistries;'
Add-Import 'import net.minecraft.core.registries.Registries;'
Add-Import 'import net.minecraft.resources.Identifier;'
Add-Import 'import net.minecraft.resources.ResourceKey;'
Add-Import 'import net.minecraft.world.item.BlockItem;'
Add-Import 'import net.minecraft.world.item.Item;'
Add-Import 'import net.minecraft.world.level.block.Block;'
Add-Import 'import net.minecraft.world.level.block.SoundType;'
Add-Import 'import net.minecraft.world.level.block.state.BlockBehaviour;'

$weedBlockCode = @'

    public static final Block WEED_BLOCK = registerBlock(
            "weed_block",
            properties -> new Block(properties
                    .strength(0.5F)
                    .sound(SoundType.GRASS)
            )
    );
'@

if ($text -notmatch 'WEED_BLOCK\s*=') {
    if ($text -match 'public\s+static\s+final\s+Item\s+TRIMMED_BUD\s*=') {
        $text = $text -replace '(\r?\n\s*public\s+static\s+final\s+Item\s+TRIMMED_BUD\s*=)', "$weedBlockCode`r`n`$1"
    } elseif ($text -match 'public\s+static\s+final\s+Item\s+WEED_SEEDS\s*=') {
        $text = $text -replace '(\r?\n\s*public\s+static\s+final\s+Item\s+WEED_SEEDS\s*=)', "$weedBlockCode`r`n`$1"
    } else {
        $text = $text -replace '(public\s+class\s+NarcotixMod\s*\{)', "`$1`r`n$weedBlockCode"
    }
}

$registerBlockMethod = @'

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        ResourceKey<Block> blockKey = ResourceKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MOD_ID, name)
        );

        Block block = blockFactory.apply(
                BlockBehaviour.Properties.of()
                        .setId(blockKey)
        );

        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name)
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey))
        );

        return block;
    }
'@

if ($text -notmatch 'private\s+static\s+Block\s+registerBlock\s*\(') {
    if ($text -match 'private\s+static\s+Item\s+registerItem\s*\(') {
        $text = $text -replace '(\r?\n\s*private\s+static\s+Item\s+registerItem\s*\()', "$registerBlockMethod`r`n`$1"
    } else {
        $text = $text -replace '(\r?\n\})\s*$', "$registerBlockMethod`r`n}"
    }
}

if ($text -notmatch 'entries\.accept\(WEED_BLOCK\)') {
    if ($text -match 'entries\.accept\(TRIMMED_BUD\);') {
        $text = $text -replace '(entries\.accept\(TRIMMED_BUD\);)', "`$1`r`n            entries.accept(WEED_BLOCK);"
    } elseif ($text -match 'entries\.accept\(JOINT\);') {
        $text = $text -replace '(entries\.accept\(JOINT\);)', "entries.accept(WEED_BLOCK);`r`n            `$1"
    }
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $modPath), $text, $utf8NoBom)

Write-Host "Patched NarcotixMod.java with WEED_BLOCK registration."
Write-Host "Resource files are already included in this ZIP."
