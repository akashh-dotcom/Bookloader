@echo off
REM Clean Test Directories - Delete or Recycle
REM Usage: clean-test-dirs.bat

echo.
echo ========================================
echo   Test Directory Cleanup
echo ========================================
echo.
echo Choose cleanup method:
echo   [D] Delete permanently (default)
echo   [R] Recycle (send to Recycle Bin)
echo.

set /p choice="Enter choice [D/R]: "

if /i "%choice%"=="R" (
    powershell -ExecutionPolicy Bypass -File "%~dp0clean-test-dirs.ps1" -Recycle
) else (
    powershell -ExecutionPolicy Bypass -File "%~dp0clean-test-dirs.ps1"
)

echo.
pause
