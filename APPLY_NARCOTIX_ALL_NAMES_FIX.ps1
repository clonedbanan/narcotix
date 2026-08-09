$ErrorActionPreference = "Stop"

$root = Get-Location
$langPath = Join-Path $root "src\main\resources\assets\narcotix\lang\en_us.json"

$dir = Split-Path $langPath -Parent
New-Item -ItemType Directory -Force -Path $dir | Out-Null

$names = [ordered]@{
    "item.narcotix.weed_seeds" = "Weed Seeds"
    "item.narcotix.trimmed_bud" = "Trimmed Bud"
    "item.narcotix.joint" = "Joint"
    "item.narcotix.blunt" = "Blunt"
    "item.narcotix.cigarette" = "Cigarette"

    "block.narcotix.weed_crop" = "Weed Plant"
    "block.narcotix.weed_crop_top" = "Weed Plant"
    "block.narcotix.weed_block" = "Block of Weed"
    "item.narcotix.weed_block" = "Block of Weed"

    "item.narcotix.cocaine_seeds" = "Cocaine Seeds"
    "item.narcotix.coke_leaf" = "Coca Leaf"
    "item.narcotix.loose_coke" = "Loose Coke"
    "item.narcotix.coke_brick" = "Coke Brick"
    "block.narcotix.cocaine_crop" = "Cocaine Plant"
    "block.narcotix.cocaine_crop_top" = "Cocaine Plant"

    "item.narcotix.tobacco_seeds" = "Tobacco Seeds"
    "item.narcotix.tobacco_leaf" = "Tobacco Leaf"
    "item.narcotix.tobacco_leaf_dried" = "Dried Tobacco Leaf"
    "block.narcotix.tobacco_crop" = "Tobacco Plant"

    "entity.narcotix.wandering_plug" = "Wandering Plug"
}

$data = [ordered]@{}

if (Test-Path $langPath) {
    $raw = Get-Content $langPath -Raw
    if (-not [string]::IsNullOrWhiteSpace($raw)) {
        try {
            $json = $raw | ConvertFrom-Json
            foreach ($prop in $json.PSObject.Properties) {
                $data[$prop.Name] = [string]$prop.Value
            }
        } catch {
            $backupPath = "$langPath.broken_backup_$(Get-Date -Format yyyyMMdd_HHmmss)"
            Copy-Item $langPath $backupPath -Force
            Write-Host "Existing en_us.json was invalid JSON, backed it up to $backupPath and rebuilding it."
        }
    }
}

foreach ($key in $names.Keys) {
    $data[$key] = $names[$key]
}

$jsonOut = ($data.GetEnumerator() | Sort-Object Name | ForEach-Object {
    [PSCustomObject]@{ Key = $_.Key; Value = $_.Value }
})

# Build stable JSON manually so it remains a simple Minecraft lang file.
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("{")
for ($i = 0; $i -lt $jsonOut.Count; $i++) {
    $key = $jsonOut[$i].Key.Replace('\\', '\\').Replace('"', '\"')
    $value = $jsonOut[$i].Value.Replace('\\', '\\').Replace('"', '\"')
    $comma = if ($i -lt $jsonOut.Count - 1) { "," } else { "" }
    $lines.Add("  `"$key`": `"$value`"$comma")
}
$lines.Add("}")

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $langPath), ($lines -join "`r`n") + "`r`n", $utf8NoBom)

Write-Host "Updated Narcotix language names in: $langPath"
Write-Host "Fixed names include weed, coke, tobacco, smoke items, weed block, and Wandering Plug."
