@echo off
REM Quick configuration test - verifies setup without processing
echo ========================================
echo   Bookloader Configuration Test
echo ========================================
echo.

REM Navigate to project root
cd /d %~dp0\..

REM Compile if needed
if not exist "build\classes\test_local_mode.class" (
    echo Compiling test_local_mode.java...
    javac -g -cp "build/classes;lib/*;lib/jakarta/*;lib/jdbc/*;lib/saxon/*;lib/textml/*;lib/xalan/*;lib/xerces/*" -d build/classes LocalDevTesting/test_local_mode.java
    if errorlevel 1 (
        echo ERROR: Compilation failed
        pause
        exit /b 1
    )
)

REM Run config test
java -cp "build/classes;lib/*;lib/jakarta/*;lib/jdbc/*;lib/saxon/*;lib/textml/*;lib/xalan/*;lib/xerces/*" test_local_mode --config

echo.
pause
