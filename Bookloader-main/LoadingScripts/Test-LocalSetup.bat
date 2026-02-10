@echo off
REM Quick test to verify LoadBook_Local.bat is configured correctly

cd /d "%~dp0\.."
set ROOTDIR=%CD%

echo.
echo ============================================================================
echo LoadBook_Local.bat Configuration Test
echo ============================================================================
echo.

REM Check if batch file exists
set "BATCH_FILE=%~dp0LoadBook_Local.bat"
if not exist "%BATCH_FILE%" (
    echo ERROR: LoadBook_Local.bat not found
    echo Expected location: %BATCH_FILE%
    exit /b 1
)
echo [OK] LoadBook_Local.bat found

REM Check Java
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot\bin\java.exe"
)

if exist "%JAVA_EXE%" (
    echo [OK] Java found: %JAVA_EXE%
) else (
    echo [WARN] Java not found at: %JAVA_EXE%
    echo        LoadBook_Local.bat may fail
)

REM Check directories
echo.
echo Checking local directories:

set "DIR_INCOMING=C:\BookloaderLocal\Incoming"
set "DIR_LOGS=C:\BookloaderLocal\Logs"
set "DIR_FINAL_XML=C:\BookloaderLocal\FinalXML"
set "DIR_FINAL_IMAGES=C:\BookloaderLocal\FinalImages"

for %%d in ("%DIR_INCOMING%" "%DIR_LOGS%" "%DIR_FINAL_XML%" "%DIR_FINAL_IMAGES%") do (
    if exist %%d (
        echo [OK] %%d
    ) else (
        echo [MISSING] %%d
        echo          Run Setup-LocalEnvironment.ps1 to create directories
    )
)

REM Check for test files in incoming
echo.
echo Checking for ZIP files in Incoming:
if exist "%DIR_INCOMING%\*.zip" (
    dir /b "%DIR_INCOMING%\*.zip"
) else (
    echo [NONE] No ZIP files found
    echo        Place test files in: %DIR_INCOMING%
)

REM Check RISBackend.cfg
echo.
echo Checking RISBackend.cfg:
if exist "%ROOTDIR%\RISBackend.cfg" (
    echo [OK] Configuration file exists
    findstr /C:"RISDB.URL" "%ROOTDIR%\RISBackend.cfg" 2>nul | findstr /C:"rittenhousedb" >nul 2>&1
    if errorlevel 1 (
        echo [WARN] Database connection may not be configured for staging
    ) else (
        echo [OK] Database connection configured
    )
) else (
    echo [ERROR] RISBackend.cfg not found
    exit /b 1
)

REM Check 7-Zip (optional)
echo.
echo Checking optional components:
if exist "C:\Program Files\7-Zip\7z.exe" (
    echo [OK] 7-Zip found (fast extraction)
) else (
    echo [INFO] 7-Zip not found (will use PowerShell extraction)
)

echo.
echo ============================================================================
echo Configuration Test Complete
echo ============================================================================
echo.
echo Ready to process books? Try:
echo   .\LoadBook_Local.bat ^<ISBN^>
echo.
echo Or run batch processing:
echo   .\ProcessIncomingBooks_Local.ps1 -FastMode
echo.

pause
