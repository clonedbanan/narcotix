$ErrorActionPreference = "Stop"

$modPath = "src\main\java\com\example\NarcotixMod.java"

if (!(Test-Path $modPath)) {
    throw "Could not find $modPath. Run this script from the Narcotix project root."
}

$text = Get-Content $modPath -Raw

# Add BLUNT and CIGARETTE item fields after JOINT.
if ($text -notmatch "public\s+static\s+final\s+Item\s+BLUNT\b") {
    $jointPattern = '(public\s+static\s+final\s+Item\s+JOINT\s*=\s*registerItem\("joint",\s*properties\s*->\s*new\s+JointItem\(properties\)\);)'
    if ($text -notmatch $jointPattern) {
        throw "Could not find the JOINT registration line. Please paste NarcotixMod.java so this can be patched manually."
    }

    $insert = @'

    public static final Item BLUNT = registerItem("blunt", properties -> new JointItem(properties));
    public static final Item CIGARETTE = registerItem("cigarette", properties -> new JointItem(properties));
'@

    $text = [regex]::Replace($text, $jointPattern, '$1' + $insert, 1)
}

# Add creative tab entries after entries.accept(JOINT);
if ($text -notmatch "entries\.accept\(BLUNT\)") {
    $entryPattern = '(entries\.accept\(JOINT\);)'
    if ($text -notmatch $entryPattern) {
        Write-Warning "Could not find entries.accept(JOINT); so BLUNT/CIGARETTE were not added to the creative tab. The items will still register."
    } else {
        $entryInsert = @'

            entries.accept(BLUNT);
            entries.accept(CIGARETTE);
'@
        $text = [regex]::Replace($text, $entryPattern, '$1' + $entryInsert, 1)
    }
}

Set-Content -Path $modPath -Value $text -Encoding UTF8

Write-Host "Patched NarcotixMod.java with BLUNT and CIGARETTE registrations."
Write-Host "Now run: .\gradlew.bat clean runClient"
