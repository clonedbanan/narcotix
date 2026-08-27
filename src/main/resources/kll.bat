@echo off
setlocal
title Remove NVIDIA RTX Remix from Half-Life 2

echo ==================================================
echo   NVIDIA RTX Remix Remover - Half-Life 2
echo ==================================================
echo.
echo Working folder:
echo %CD%
echo.

REM Make sure we're reasonably likely to be in an HL2 installation.
if not exist "hl2.exe" (
    if not exist "hl2\gameinfo.txt" (
        echo WARNING: This does not look like a Half-Life 2 folder.
        echo.
        echo Put this BAT file in the folder containing hl2.exe
        echo and run it again.
        echo.
        pause
        exit /b 1
    )
)

echo Removing RTX Remix files...
echo.

REM RTX Remix / DXVK Remix proxy
if exist "d3d9.dll" (
    echo Deleting d3d9.dll
    del /f /q "d3d9.dll"
)

REM Common RTX Remix config/log files
for %%F in (
    "dxvk.conf"
    "dxvk-remix.conf"
    "rtx.conf"
    "rtx-remix.conf"
    "d3d9.log"
    "dxvk.log"
    "remix.log"
) do (
    if exist "%%~F" (
        echo Deleting %%~F
        del /f /q "%%~F"
    )
)

REM Common RTX Remix directories
for %%D in (
    ".trex"
    "rtx-remix"
    "rtx_remix"
    "remix"
) do (
    if exist "%%~D\" (
        echo Removing folder %%~D
        rmdir /s /q "%%~D"
    )
)

REM Common Remix runtime DLLs / components.
REM Only delete these from the HL2 root folder.
for %%F in (
    "NvRemixBridge.exe"
    "NvRemixBridge32.exe"
    "NvRemixBridge64.exe"
    "NvRemixLauncher.exe"
    "NvRemixLauncher32.exe"
    "NvRemixLauncher64.exe"
    "dxvk-remix.dll"
    "rtx-remix.dll"
) do (
    if exist "%%~F" (
        echo Deleting %%~F
        del /f /q "%%~F"
    )
)

echo.
echo ==================================================
echo   RTX Remix removal finished.
echo ==================================================
echo.
echo If HL2 still launches through Remix, check your Steam
echo launch options and remove any Remix launcher arguments.
echo.
echo You can also use Steam:
echo   Half-Life 2 ^> Properties ^> Installed Files
echo   ^> Verify integrity of game files
echo.
pause