@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================================
REM LoadBook_Local.bat - Full local book processing with post-processing
REM ============================================================================
REM This script mimics the staging LoadBook.bat but runs entirely locally
REM 
REM Usage: LoadBook_Local.bat <ISBN> [-update]
REM        LoadBook_Local.bat 9781234567890
REM        LoadBook_Local.bat 9781234567890 -update
REM
REM Prerequisites:
REM   1. Database connection configured in RISBackend.cfg
REM   2. ZIP file OR extracted folder in local incoming directory
REM      - ZIP:    C:\BookloaderLocal\Incoming\<ISBN>.zip
REM      - Folder: C:\BookloaderLocal\Incoming\<ISBN>\  
REM   3. R2Utilities post-processing configured (optional)
REM ============================================================================

REM Change to the repo root directory
cd /d "%~dp0\.."
set ROOTDIR=%CD%

REM ============================================================================
REM CONFIGURATION - Adjust these paths for your local environment
REM ============================================================================

REM Local directories for book processing
set "LOCAL_INCOMING=%ROOTDIR%\test\LoadFromHere"
set "LOCAL_CONTENT_IN=%ROOTDIR%\test\input"
set "LOCAL_WORK_TEMP=%ROOTDIR%\test\temp"
set "LOCAL_FINAL_XML=%ROOTDIR%\test\finalOutput\xml"
set "LOCAL_FINAL_IMAGES=%ROOTDIR%\test\finalOutput\images"
set "LOCAL_LOGS=%ROOTDIR%\test\logs"

REM Java executable (adjust if needed)
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot\bin\java.exe"
)

REM R2Utilities executable (if you have it set up locally)
set "R2UTILITIES_EXE=C:\RittenhouseRepos\R2Library\src\R2Utilities\bin\Debug\net481\R2Utilities.exe"
set "R2UTILITIES_ENABLED=true"

REM 7-Zip executable
set "SEVEN_ZIP=C:\Program Files\7-Zip\7z.exe"

REM ============================================================================
REM VALIDATE ARGUMENTS
REM ============================================================================

if "%~1"=="" (
    echo ERROR: No ISBN argument specified!
    echo Usage: LoadBook_Local.bat ^<ISBN^> [-update]
    exit /b 2
)

set "ISBN=%~1"
set "MODE=%~2"
set "MODE2=%~3"

REM ============================================================================
REM SETUP LOGGING
REM ============================================================================

REM Create logs directory
if not exist "%LOCAL_LOGS%" mkdir "%LOCAL_LOGS%" >nul 2>&1

REM Set log file path
set "LOG_FILE=%LOCAL_LOGS%\%ISBN%_Processing.log"

REM Initialize log file
echo ======================================== > "%LOG_FILE%"
echo Book Processing Log >> "%LOG_FILE%"
echo ISBN: %ISBN% >> "%LOG_FILE%"
echo Started: %DATE% %TIME% >> "%LOG_FILE%"
echo Mode: %MODE% >> "%LOG_FILE%"
echo ======================================== >> "%LOG_FILE%"
echo/ >> "%LOG_FILE%"

echo/
echo ============================================================================
echo LOCAL BOOK PROCESSING - ISBN: %ISBN%
echo ============================================================================
echo/
echo Log file: %LOG_FILE%
echo/

REM ============================================================================
REM CREATE REQUIRED DIRECTORIES
REM ============================================================================

call :LogBoth "[STEP 0] Creating required directories..."
for %%d in ("%LOCAL_INCOMING%" "%LOCAL_CONTENT_IN%" "%LOCAL_WORK_TEMP%" "%LOCAL_FINAL_XML%" "%LOCAL_FINAL_IMAGES%") do (
    if not exist "%%~d" (
        call :LogBoth "  Creating: %%~d"
        mkdir "%%~d" >nul 2>&1
    )
)
call :LogBoth "  Directories ready"
call :LogBoth ""

REM ============================================================================
REM BUILD CLASSPATH
REM ============================================================================

call :LogBoth "[STEP 1] Building Java classpath..."
set LOCALCLASSPATH=%ROOTDIR%\build\classes

for %%d in (jakarta jdbc saxon xalan xerces textml javamail) do (
    for %%f in ("%ROOTDIR%\lib\%%d\*.jar") do (
        set "LOCALCLASSPATH=!LOCALCLASSPATH!;%%~f"
    )
)
for %%f in ("%ROOTDIR%\lib\*.jar") do (
    set "LOCALCLASSPATH=!LOCALCLASSPATH!;%%~f"
)
call :LogBoth "  Classpath built"
call :LogBoth ""

REM ============================================================================
REM VALIDATE ZIP FILE OR FOLDER EXISTS
REM ============================================================================

call :LogBoth "[STEP 2] Locating book source in LoadFromHere..."
set "ZIP=%LOCAL_INCOMING%\%ISBN%.zip"
set "FOLDER=%LOCAL_INCOMING%\%ISBN%"

if exist "%ZIP%" (
    call :LogBoth "  Found ZIP: %ZIP%"
    set "SOURCE_TYPE=ZIP"
) else if exist "%FOLDER%\" (
    call :LogBoth "  Found folder: %FOLDER%"
    set "SOURCE_TYPE=FOLDER"
) else (
    call :LogBoth "  ERROR: No ZIP file or folder found for ISBN: %ISBN%"
    call :LogBoth "  Expected one of:"
    call :LogBoth "    - ZIP:    %ZIP%"
    call :LogBoth "    - Folder: %FOLDER%\"
    call :LogBoth "  Please place book content in %LOCAL_INCOMING% (test\LoadFromHere)"
    goto :Failure
)
call :LogBoth ""

REM ============================================================================
REM CLEAR CONTENT_IN FOR FRESH START
REM ============================================================================

call :LogBoth "[STEP 3] Clearing staging area for fresh processing..."
if exist "%LOCAL_CONTENT_IN%\*" (
    call :LogBoth "  Removing previous content from: %LOCAL_CONTENT_IN%"
    rmdir /s /q "%LOCAL_CONTENT_IN%" >nul 2>&1
)
if not exist "%LOCAL_CONTENT_IN%" (
    mkdir "%LOCAL_CONTENT_IN%" >nul 2>&1
)
call :LogBoth "  Staging area cleared and ready"
call :LogBoth ""

REM ============================================================================
REM EXTRACT/COPY TO CONTENT_IN
REM ============================================================================

if "%SOURCE_TYPE%"=="ZIP" (
    call :LogBoth "[STEP 4] Extracting ZIP to staging area..."
    
    REM Create temporary extraction directory
    set "WORK=%LOCAL_WORK_TEMP%\extract_%ISBN%_%RANDOM%"
    mkdir "%WORK%" >nul 2>&1
    
    REM Extract ZIP
    if not exist "%SEVEN_ZIP%" (
        call :LogBoth "  Using PowerShell extraction..."
        powershell -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%WORK%' -Force"
        set "EXTRACT_RESULT=!ERRORLEVEL!"
    ) else (
        call :LogBoth "  Using 7-Zip extraction..."
        call :LogBoth "  Extracting: %ZIP%"
        call :LogBoth "  Destination: !WORK!"
        "%SEVEN_ZIP%" x "%ZIP%" -o"!WORK!" -y
        set "EXTRACT_RESULT=!ERRORLEVEL!"
        call :LogBoth "  7-Zip exit code: !EXTRACT_RESULT!"
    )
    
    if !EXTRACT_RESULT! NEQ 0 (
        call :LogBoth "  ERROR: Extraction failed with code !EXTRACT_RESULT!"
        rmdir /s /q "%WORK%" >nul 2>&1
        goto :Failure
    )
    call :LogBoth "  ZIP extracted successfully"
    
    REM Copy extracted content to staging
    call :LogBoth "  Copying to: %LOCAL_CONTENT_IN%"
    xcopy "!WORK!\*" "%LOCAL_CONTENT_IN%\" /E /I /Y /Q
    if errorlevel 1 (
        call :LogBoth "  ERROR: Failed to copy extracted content"
        rmdir /s /q "!WORK!" >nul 2>&1
        goto :Failure
    )
    
    REM Cleanup temporary extraction directory
    rmdir /s /q "!WORK!" >nul 2>&1
    call :LogBoth "  Content ready in staging area"
    
) else (
    call :LogBoth "[STEP 4] Copying folder to staging area..."
    call :LogBoth "  Source: %FOLDER%"
    call :LogBoth "  Destination: %LOCAL_CONTENT_IN%"
    
    xcopy "%FOLDER%\*" "%LOCAL_CONTENT_IN%\" /E /I /Y /Q >nul 2>&1
    if errorlevel 1 (
        call :LogBoth "  ERROR: Failed to copy folder content"
        goto :Failure
    )
    call :LogBoth "  Folder content copied successfully"
)
call :LogBoth ""

REM ============================================================================
REM RUN JAVA BOOK LOADER
REM ============================================================================

call :LogBoth "[STEP 5] Running Java Book Loader..."
call :LogBoth "  Arguments: %MODE% %MODE2%"
call :LogBoth "  This may take 2-5 minutes in fast mode, 30-60 minutes in normal mode..."
call :LogBoth ""
call :LogBoth "  Additional logs:"
call :LogBoth "    Java backend: %ROOTDIR%\logs\RISBackend.log"
call :LogBoth "    Per-ISBN log: %ROOTDIR%\test\logs\%ISBN%_Processing.log"
call :LogBoth ""

"%JAVA_EXE%" -Xms1g -Xmx2g ^
    --enable-native-access=ALL-UNNAMED ^
    -Djdk.xml.entityExpansionLimit=10000 ^
    -Djdk.xml.totalEntitySizeLimit=1000000 ^
    -Djava.security.policy=java.ris.policy ^
    -cp "%LOCALCLASSPATH%" ^
    com.rittenhouse.RIS.Main %MODE% %MODE2%

set "JAVA_EXIT_CODE=%ERRORLEVEL%"
if %JAVA_EXIT_CODE% NEQ 0 (
    call :LogBoth ""
    call :LogBoth "  ERROR: Java Book Loader failed with exit code %JAVA_EXIT_CODE%"
    call :LogBoth "  Check detailed logs:"
    call :LogBoth "    - %ROOTDIR%\logs\RISBackend.log"
    call :LogBoth "    - %ROOTDIR%\test\logs\%ISBN%_Processing.log"
    goto :Failure
)

call :LogBoth ""
call :LogBoth "  Java Book Loader completed successfully"
call :LogBoth ""

REM ============================================================================
REM VERIFY OUTPUT
REM ============================================================================

call :LogBoth "[STEP 6] Verifying output..."
set "XMLSRC=%ROOTDIR%\test\finalOutput\%ISBN%\xml"
set "IMGSRC=%ROOTDIR%\test\finalOutput\%ISBN%\images"

if not exist "%XMLSRC%\" (
    call :LogBoth "  WARNING: Expected XML folder missing: %XMLSRC%"
    call :LogBoth "  Book may not have processed correctly"
    set "VERIFICATION_WARNING=1"
) else (
    call :LogBoth "  XML output found: %XMLSRC%"
)

if not exist "%IMGSRC%\" (
    call :LogBoth "  NOTE: No images folder found (may be normal for some books)"
) else (
    call :LogBoth "  Images output found: %IMGSRC%"
)
call :LogBoth ""

REM ============================================================================
REM COPY TO FINAL DESTINATIONS
REM ============================================================================

call :LogBoth "[STEP 7] Copying to final destinations..."

if exist "%XMLSRC%\" (
    set "FINAL_XML_DEST=%LOCAL_FINAL_XML%\%ISBN%"
    if exist "!FINAL_XML_DEST!" rmdir /s /q "!FINAL_XML_DEST!" >nul 2>&1
    mkdir "!FINAL_XML_DEST!" >nul 2>&1
    xcopy "%XMLSRC%\*" "!FINAL_XML_DEST%\" /E /I /Y /Q >nul 2>&1
    call :LogBoth "  XML copied to: !FINAL_XML_DEST!"
)

if exist "%IMGSRC%\" (
    set "FINAL_IMG_DEST=%LOCAL_FINAL_IMAGES%\%ISBN%"
    if exist "!FINAL_IMG_DEST!" rmdir /s /q "!FINAL_IMG_DEST!" >nul 2>&1
    mkdir "!FINAL_IMG_DEST!" >nul 2>&1
    xcopy "%IMGSRC%\*" "!FINAL_IMG_DEST%\" /E /I /Y /Q >nul 2>&1
    call :LogBoth "  Images copied to: !FINAL_IMG_DEST!"
)
call :LogBoth ""

REM ============================================================================
REM RUN POST-PROCESSING (Optional)
REM ============================================================================

if /I "%R2UTILITIES_ENABLED%"=="true" (
    call :LogBoth "[STEP 8] Running post-processing..."
    
    if not exist "%R2UTILITIES_EXE%" (
        call :LogBoth "  WARNING: R2Utilities.exe not found at: %R2UTILITIES_EXE%"
        call :LogBoth "  Skipping post-processing step"
        call :LogBoth "  See POST_PROCESSING_SETUP.md for configuration instructions"
    ) else (
        call :LogBoth "  Running R2Utilities post-processing..."
        "%R2UTILITIES_EXE%" -BookLoaderPostProcessingTask -isbn=%ISBN% -includeChapterNumbersInToc=true
        set "PP_EXIT_CODE=!ERRORLEVEL!"
        
        if !PP_EXIT_CODE! NEQ 0 (
            call :LogBoth "  WARNING: Post-processing failed with exit code !PP_EXIT_CODE!"
            call :LogBoth "  Book loading completed but post-processing had issues"
        ) else (
            call :LogBoth "  Post-processing completed successfully"
        )
    )
    call :LogBoth ""
) else (
    call :LogBoth "[STEP 8] Post-processing: DISABLED"
    call :LogBoth "  To enable, set R2UTILITIES_ENABLED=true and configure R2UTILITIES_EXE"
    call :LogBoth "  See POST_PROCESSING_SETUP.md for instructions"
    call :LogBoth ""
)

REM ============================================================================
REM COMPLETION - SUCCESS
REM ============================================================================

:Success
call :LogBoth "========================================"
call :LogBoth "SUCCESS: Book processing completed"
call :LogBoth "ISBN: %ISBN%"
call :LogBoth "========================================"
call :LogBoth ""
call :LogBoth "Output Locations:"
call :LogBoth "  XML:    %LOCAL_FINAL_XML%\%ISBN%"
call :LogBoth "  Images: %LOCAL_FINAL_IMAGES%\%ISBN%"
call :LogBoth ""
call :LogBoth "Log Files:"
call :LogBoth "  This run:      %LOG_FILE%"
call :LogBoth "  Java backend:  %ROOTDIR%\logs\RISBackend.log"
call :LogBoth "  Per-ISBN log:  %ROOTDIR%\test\logs\%ISBN%_SUCCESS.log"
call :LogBoth ""

REM Rename log file to SUCCESS
if exist "%LOG_FILE%" (
    set "SUCCESS_LOG=%LOCAL_LOGS%\%ISBN%_SUCCESS.log"
    move /Y "%LOG_FILE%" "!SUCCESS_LOG!" >nul 2>&1
    echo SUCCESS log: !SUCCESS_LOG!
)

if defined VERIFICATION_WARNING (
    echo WARNING: Some verification checks failed. Review logs above.
    exit /b 50
)

exit /b 0

REM ============================================================================
REM COMPLETION - FAILURE
REM ============================================================================

:Failure
call :LogBoth ""
call :LogBoth "========================================"
call :LogBoth "FAILED: Book processing failed"
call :LogBoth "ISBN: %ISBN%"
call :LogBoth "Exit Code: %JAVA_EXIT_CODE%"
call :LogBoth "========================================"
call :LogBoth ""
call :LogBoth "Check logs for details:"
call :LogBoth "  This run:      %LOG_FILE%"
call :LogBoth "  Java backend:  %ROOTDIR%\logs\RISBackend.log"
call :LogBoth ""

REM Rename log file to FAIL
if exist "%LOG_FILE%" (
    set "FAIL_LOG=%LOCAL_LOGS%\%ISBN%_FAIL.log"
    move /Y "%LOG_FILE%" "!FAIL_LOG!" >nul 2>&1
    echo FAIL log: !FAIL_LOG!
)

exit /b 1

REM ============================================================================
REM HELPER FUNCTIONS
REM ============================================================================

:LogBoth
REM Log to both console and file
if "%~1"=="" (
    echo/
    echo/ >> "%LOG_FILE%"
) else (
    echo %~1
    echo %~1 >> "%LOG_FILE%"
)
goto :eof
