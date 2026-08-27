$ErrorActionPreference = "Stop"
Write-Host "Applying Cop crop reachability fix v14..."

$project = Get-Location
$path = Join-Path $project "src\main\java\com\example\CopEntity.java"

if (!(Test-Path $path)) {
    throw "Could not find CopEntity.java at $path. Run this from C:\Users\caleb\Documents\GitHub\Narcotix"
}

$text = Get-Content $path -Raw

# Remove possible BOM / mojibake from earlier scripts.
$text = $text.TrimStart([char]0xFEFF)
$text = $text -replace '^ï»¿', ''
$text = $text -replace '^∩╗┐', ''

function Add-ImportIfMissing([string]$importLine) {
    if ($script:text -notmatch [regex]::Escape($importLine)) {
        $script:text = $script:text -replace '(package\s+com\.example;\s*)', "`$1`r`n$importLine`r`n"
    }
}

Add-ImportIfMissing 'import net.minecraft.core.Direction;'
Add-ImportIfMissing 'import net.minecraft.tags.BlockTags;'
Add-ImportIfMissing 'import net.minecraft.world.level.block.state.BlockState;'

# Keep tobacco legal for cops: do not confiscate cigarettes or tobacco-only items.
$text = $text -replace '(?m)^\s*\|\|\s*stack\.is\(NarcotixMod\.CIGARETTE\)\s*\r?\n', ''
$text = $text -replace '(?m)^\s*\|\|\s*stack\.is\(NarcotixMod\.TOBACCO_LEAF\)\s*\r?\n', ''
$text = $text -replace '(?m)^\s*\|\|\s*stack\.is\(NarcotixMod\.TOBACCO_LEAF_DRY\)\s*\r?\n', ''

# Make sure crop fields exist.
if ($text -notmatch 'cropSearchTarget') {
    $fieldText = @'
    private BlockPos cropSearchTarget = null;
    private int cropBreakWarmup = 0;
    private int cropScanCooldown = 0;

'@
    $text = [regex]::Replace(
        $text,
        '(public\s+class\s+CopEntity\s+extends\s+[^\{]+\{\s*)',
        { param($m) $m.Groups[1].Value + $fieldText },
        1
    )
}

# Make sure the crop handler is actually called after serverLevel is created.
$cropCall = @'
        if (handleContrabandCropSearch(serverLevel)) {
            return;
        }

'@

if ($text -notmatch 'handleContrabandCropSearch\(serverLevel\)') {
    $text = [regex]::Replace(
        $text,
        '(ServerLevel\s+serverLevel\s*=\s*\(ServerLevel\)\s*this\.level\(\);\s*)',
        { param($m) $m.Groups[1].Value + "`r`n" + $cropCall },
        1
    )
}

# Replace the crop helper section with a path-aware version.
$methods = @'

    private boolean handleContrabandCropSearch(ServerLevel level) {
        if (this.cropSearchTarget != null) {
            BlockPos basePos = getContrabandCropBase(level, this.cropSearchTarget);

            if (basePos == null) {
                clearCropTarget();
                return false;
            }

            BlockPos standPos = findReachableCropStandPosition(level, basePos);

            if (standPos == null) {
                clearCropTarget();
                this.cropScanCooldown = 25;
                return false;
            }

            this.getLookControl().setLookAt(basePos.getX() + 0.5D, basePos.getY() + 0.5D, basePos.getZ() + 0.5D);

            double dx = this.getX() - (standPos.getX() + 0.5D);
            double dy = this.getY() - standPos.getY();
            double dz = this.getZ() - (standPos.getZ() + 0.5D);
            double distanceSqr = (dx * dx) + (dy * dy) + (dz * dz);

            if (distanceSqr > 1.85D) {
                this.cropBreakWarmup = 0;
                this.getNavigation().moveTo(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D, 0.7D);
                return true;
            }

            // Do not allow breaking from the wrong side of a wall/fence just because the crop is physically close.
            if (!isActuallyStandingAtCropAccess(level, basePos)) {
                this.cropBreakWarmup = 0;
                this.getNavigation().moveTo(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D, 0.7D);
                return true;
            }

            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            this.cropBreakWarmup++;

            if (this.cropBreakWarmup >= 22) {
                breakContrabandCrop(level, basePos);
                clearCropTarget();
                this.cropScanCooldown = 10;
            }

            return true;
        }

        if (this.cropScanCooldown > 0) {
            this.cropScanCooldown--;
            return false;
        }

        this.cropScanCooldown = 20;
        this.cropSearchTarget = findNearestContrabandCrop(level);
        return this.cropSearchTarget != null;
    }

    private void clearCropTarget() {
        this.cropSearchTarget = null;
        this.cropBreakWarmup = 0;
    }

    private BlockPos findNearestContrabandCrop(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos scanPos : BlockPos.betweenClosed(origin.offset(-14, -4, -14), origin.offset(14, 4, 14))) {
            BlockPos basePos = getContrabandCropBase(level, scanPos);

            if (basePos == null) {
                continue;
            }

            // Fences, walls, and closed barriers should stop cops from choosing the crop at all.
            if (findReachableCropStandPosition(level, basePos) == null) {
                continue;
            }

            double dx = this.getX() - (basePos.getX() + 0.5D);
            double dy = this.getY() - (basePos.getY() + 0.5D);
            double dz = this.getZ() - (basePos.getZ() + 0.5D);
            double distance = (dx * dx) + (dy * dy) + (dz * dz);

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = basePos.immutable();
            }
        }

        return closest;
    }

    private BlockPos findReachableCropStandPosition(ServerLevel level, BlockPos basePos) {
        BlockPos closestStand = null;
        double closestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos standPos = basePos.relative(direction);

            if (!canStandAtCropAccess(level, standPos)) {
                continue;
            }

            if (!hasPathToStandPosition(standPos)) {
                continue;
            }

            double dx = this.getX() - (standPos.getX() + 0.5D);
            double dy = this.getY() - standPos.getY();
            double dz = this.getZ() - (standPos.getZ() + 0.5D);
            double distance = (dx * dx) + (dy * dy) + (dz * dz);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestStand = standPos.immutable();
            }
        }

        return closestStand;
    }

    private boolean hasPathToStandPosition(BlockPos standPos) {
        // Already standing there counts. This avoids occasional tiny pathing failures at point-blank range.
        double dx = this.getX() - (standPos.getX() + 0.5D);
        double dy = this.getY() - standPos.getY();
        double dz = this.getZ() - (standPos.getZ() + 0.5D);

        if (((dx * dx) + (dy * dy) + (dz * dz)) <= 1.85D) {
            return true;
        }

        var path = this.getNavigation().createPath(standPos, 0);
        return path != null && path.canReach();
    }

    private boolean isActuallyStandingAtCropAccess(ServerLevel level, BlockPos basePos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos standPos = basePos.relative(direction);

            if (!canStandAtCropAccess(level, standPos)) {
                continue;
            }

            double dx = this.getX() - (standPos.getX() + 0.5D);
            double dy = this.getY() - standPos.getY();
            double dz = this.getZ() - (standPos.getZ() + 0.5D);

            if (((dx * dx) + (dy * dy) + (dz * dz)) <= 1.85D) {
                return true;
            }
        }

        return false;
    }

    private boolean canStandAtCropAccess(ServerLevel level, BlockPos standPos) {
        BlockState feetState = level.getBlockState(standPos);
        BlockState headState = level.getBlockState(standPos.above());
        BlockState floorState = level.getBlockState(standPos.below());

        if (isFenceLike(feetState) || isFenceLike(headState) || isFenceLike(floorState)) {
            return false;
        }

        if (!feetState.getCollisionShape(level, standPos).isEmpty()) {
            return false;
        }

        if (!headState.getCollisionShape(level, standPos.above()).isEmpty()) {
            return false;
        }

        return !floorState.getCollisionShape(level, standPos.below()).isEmpty();
    }

    private boolean isFenceLike(BlockState state) {
        return state.is(BlockTags.FENCES)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WALLS);
    }

    private BlockPos getContrabandCropBase(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.is(NarcotixMod.WEED_CROP) || state.is(NarcotixMod.COCAINE_CROP)) {
            return pos.immutable();
        }

        if (state.is(NarcotixMod.WEED_CROP_TOP) || state.is(NarcotixMod.COCAINE_CROP_TOP)) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);

            if (belowState.is(NarcotixMod.WEED_CROP) || belowState.is(NarcotixMod.COCAINE_CROP)) {
                return below.immutable();
            }
        }

        return null;
    }

    private void breakContrabandCrop(ServerLevel level, BlockPos basePos) {
        BlockState baseState = level.getBlockState(basePos);

        if (baseState.is(NarcotixMod.WEED_CROP) || baseState.is(NarcotixMod.COCAINE_CROP)) {
            BlockPos topPos = basePos.above();
            BlockState topState = level.getBlockState(topPos);

            if (topState.is(NarcotixMod.WEED_CROP_TOP) || topState.is(NarcotixMod.COCAINE_CROP_TOP)) {
                level.destroyBlock(topPos, false);
            }
        }

        level.destroyBlock(basePos, false);
    }
'@

$helperStart = $text.IndexOf("    private boolean handleContrabandCropSearch")
$lastBrace = $text.LastIndexOf('}')
if ($lastBrace -lt 0) {
    throw "Could not find final class brace in CopEntity.java"
}

if ($helperStart -ge 0) {
    $text = $text.Substring(0, $helperStart).TrimEnd() + $methods + "`r`n}"
} else {
    $text = $text.Substring(0, $lastBrace).TrimEnd() + $methods + "`r`n}" + $text.Substring($lastBrace + 1)
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $path), $text, $utf8NoBom)

Write-Host "Cop crop reachability fix v14 applied. Now run: .\gradlew.bat clean runClient"
