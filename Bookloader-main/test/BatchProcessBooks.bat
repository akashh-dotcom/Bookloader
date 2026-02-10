@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM ============================================================================
REM BatchProcessBooks.bat - Process multiple books in test\input folder
REM (This script lives in the \test folder)
REM ============================================================================
REM Processes all ZIP files in test\input, outputs to test\batchoutput
REM Creates timestamped final archive with all processed ISBNs
REM ============================================================================

REM --- Resolve directories based on script location ---
REM SCRIPT_DIR = ...\test\
set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

REM TESTDIR = ...\test
set "TESTDIR=%SCRIPT_DIR%"

REM BASEDIR = parent of test (project root)
pushd "%TESTDIR%\.." >nul
set "BASEDIR=%CD%"
popd >nul

REM Always run from BASEDIR so relative tool behavior is stable
pushd "%BASEDIR%" >nul

echo.
echo ================================================================================
echo                    BATCH BOOK PROCESSING TOOL
echo ================================================================================
echo.
echo This tool will process all ZIP files in: test\input
echo Output will be saved to: test\batchoutput\[timestamped-folder]
echo.
echo Select processing mode:
echo   [1] Normal + No PMID ^(DEFAULT - Update + DB, no PubMed lookups^)
echo   [2] Normal + PMID ^(Full processing with database + PubMed lookups^)
echo   [3] Ultra Fast ^(Skip All - No DB, No PMID, No Linking^)
echo.
set /p MODE_CHOICE="Enter choice [1-3] (default=1): "

REM Default to mode 1 if empty
if "!MODE_CHOICE!"=="" set MODE_CHOICE=1

REM Set flags based on choice
if "!MODE_CHOICE!"=="1" (
    set "FLAGS=--update --normal --skipPMID"
    set "MODE_NAME=Normal_No_PMID"
    echo.
    echo Selected: Normal + No PMID ^(Database + Update, No PubMed^)
) else if "!MODE_CHOICE!"=="2" (
    set "FLAGS=--update --normal"
    set "MODE_NAME=Normal_Update"
    echo.
    echo Selected: Normal + Update ^(Full Database Mode with PMID^)
) else if "!MODE_CHOICE!"=="3" (
    set "FLAGS=--noDB --skipPMID --skipLinks"
    set "MODE_NAME=Ultra_Fast"
    echo.
    echo Selected: Ultra Fast ^(Skip All - No DB, No PMID, No Linking^)
) else (
    echo Invalid choice. Using default: Normal + No PMID
    set "FLAGS=--update --normal --skipPMID"
    set "MODE_NAME=Normal_No_PMID"
)

echo ================================================================================
echo.

REM Create timestamp for this batch run
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set "datetime=%%I"
set "TIMESTAMP=%datetime:~0,4%%datetime:~4,2%%datetime:~6,2%_%datetime:~8,2%%datetime:~10,2%%datetime:~12,2%"

REM Create batch output directory
set "BATCH_OUTPUT_ROOT=%TESTDIR%\batchoutput"
set "BATCH_RUN_DIR=%BATCH_OUTPUT_ROOT%\Batch_%TIMESTAMP%_%MODE_NAME%"
if not exist "%BATCH_RUN_DIR%" mkdir "%BATCH_RUN_DIR%"

REM Create batch log file
set "BATCH_LOG=%BATCH_RUN_DIR%\batch_processing_log.txt"

echo Batch Processing Started: %date% %time% > "%BATCH_LOG%"
echo Mode: %MODE_NAME% >> "%BATCH_LOG%"
echo Flags: %FLAGS% >> "%BATCH_LOG%"
echo ================================================================================== >> "%BATCH_LOG%"
echo. >> "%BATCH_LOG%"

REM Initialize counters
set "TOTAL_BOOKS=0"
set "SUCCESS_COUNT=0"
set "FAILURE_COUNT=0"

REM Build list of all ZIP files FIRST (before any file moving)
echo Scanning for books to process...
set "BOOK_LIST="
for %%F in ("%TESTDIR%\input\*.zip") do (
    set /a TOTAL_BOOKS+=1
    REM Store full path in array-like list
    set "BOOK_!TOTAL_BOOKS!=%%~fF"
    set "BOOKNAME_!TOTAL_BOOKS!=%%~nxF"
)

echo Processing !TOTAL_BOOKS! books...
echo.

REM Now process each book from our captured list
for /L %%N in (1,1,!TOTAL_BOOKS!) do (
    call :ProcessOneBook %%N
)

goto :BatchComplete

:ProcessOneBook
setlocal EnableDelayedExpansion
    set "BOOK_INDEX=%~1"
    set "ZIP_FULL_PATH=!BOOK_%BOOK_INDEX%!"
    set "ZIP_FILE=!BOOKNAME_%BOOK_INDEX%!"
    
    REM Extract filename without extension and get ISBN
    for %%F in ("!ZIP_FULL_PATH!") do set "ZIP_NAME=%%~nF"
    for /f "tokens=1 delims=_" %%I in ("!ZIP_NAME!") do set "ISBN=%%I"

    echo ================================================================================
    echo Processing: !ZIP_FILE!
    echo ISBN: !ISBN!
    echo ================================================================================

    echo. >> "%BATCH_LOG%"
    echo [!date! !time!] Processing: !ZIP_FILE! (ISBN: !ISBN!) >> "%BATCH_LOG%"

    REM CRITICAL: Ensure THIS book's ZIP is in input folder and move others out
    echo Isolating this book for processing...
    if not exist "%TESTDIR%\.batch_temp" mkdir "%TESTDIR%\.batch_temp"
    
    REM First, restore this book to input if it's in batch_temp
    if exist "%TESTDIR%\.batch_temp\!ZIP_FILE!" (
        move "%TESTDIR%\.batch_temp\!ZIP_FILE!" "%TESTDIR%\input\" >nul 2>&1
    )
    
    REM Now move all OTHER ZIPs to batch_temp
    for %%Z in ("%TESTDIR%\input\*.zip") do (
        if not "%%~nxZ"=="!ZIP_FILE!" (
            move "%%Z" "%TESTDIR%\.batch_temp\" >nul 2>&1
        )
    )

    REM Clean ALL work directories before processing THIS book
    echo Cleaning work directories...
    if exist "%TESTDIR%\temp" rmdir /s /q "%TESTDIR%\temp" 2>nul
    if exist "%TESTDIR%\output" rmdir /s /q "%TESTDIR%\output" 2>nul
    if exist "%TESTDIR%\media" rmdir /s /q "%TESTDIR%\media" 2>nul
    if exist "%TESTDIR%\R2v2-XMLbyISBN" rmdir /s /q "%TESTDIR%\R2v2-XMLbyISBN" 2>nul

    REM Also clean any extracted directories from previous runs
    for /d %%D in ("%TESTDIR%\input\*") do (
        rmdir /s /q "%%D" 2>nul
    )

    mkdir "%TESTDIR%\temp" 2>nul
    mkdir "%TESTDIR%\output" 2>nul
    mkdir "%TESTDIR%\media" 2>nul
    mkdir "%TESTDIR%\R2v2-XMLbyISBN" 2>nul

    REM Run the Java bookloader (it will auto-extract the ZIP)
    echo Running bookloader for ISBN: !ISBN!...

    call :RunBookloader "!ISBN!" "!FLAGS!"
    set "BOOK_EXIT_CODE=!ERRORLEVEL!"

    if !BOOK_EXIT_CODE! NEQ 0 goto :BookFailed

    REM === SUCCESS PATH ===
    echo [SUCCESS] !ISBN! processed successfully
    echo [!date! !time!] SUCCESS: !ISBN! >> "%BATCH_LOG%"

    REM Create output directory with _PASS suffix
    set "ISBN_OUTPUT=%BATCH_RUN_DIR%\!ISBN!_PASS"
    if not exist "!ISBN_OUTPUT!" mkdir "!ISBN_OUTPUT!"

    REM Copy ONLY XML files from all possible locations to ISBN directory
    echo Collecting XML output files...

    REM From test\output (main processing output)
    if exist "%TESTDIR%\output\*.xml" (
        echo   Copying from test\output...
        xcopy /Y "%TESTDIR%\output\*.xml" "!ISBN_OUTPUT!\" >nul 2>&1
    )

    REM From R2v2-XMLbyISBN (final content location)
    if exist "%TESTDIR%\R2v2-XMLbyISBN\!ISBN!\xml" (
        echo   Copying from R2v2-XMLbyISBN\!ISBN!\xml...
        xcopy /Y "%TESTDIR%\R2v2-XMLbyISBN\!ISBN!\xml\*.xml" "!ISBN_OUTPUT!\" >nul 2>&1
    )

    REM From temp (book.isbn.xml and processed files)
    if exist "%TESTDIR%\temp\*.xml" (
        echo   Copying from test\temp...
        xcopy /Y "%TESTDIR%\temp\*.xml" "!ISBN_OUTPUT!\" >nul 2>&1
    )

    REM Count XML files collected
    set "XML_COUNT=0"
    for %%X in ("!ISBN_OUTPUT!\*.xml") do set /a XML_COUNT+=1
    echo   Total XML files collected: !XML_COUNT!

    goto :AfterBookProcessing

    :BookFailed
    REM === FAILURE PATH ===
    echo [FAILURE] !ISBN! processing failed with exit code !BOOK_EXIT_CODE!
    echo [!date! !time!] FAILURE: !ISBN! (Exit Code: !BOOK_EXIT_CODE!) >> "%BATCH_LOG%"

    REM Create output directory with _FAIL suffix
    set "ISBN_OUTPUT=%BATCH_RUN_DIR%\!ISBN!_FAIL"
    if not exist "!ISBN_OUTPUT!" mkdir "!ISBN_OUTPUT!"

    REM Create detailed error log file
    set "ERROR_LOG=!ISBN_OUTPUT!\ERROR_LOG_!ISBN!.txt"
    echo ================================================================================ > "!ERROR_LOG!"
    echo PROCESSING FAILURE DETAILS >> "!ERROR_LOG!"
    echo ================================================================================ >> "!ERROR_LOG!"
    echo. >> "!ERROR_LOG!"
    echo ISBN: !ISBN! >> "!ERROR_LOG!"
    echo Exit Code: !BOOK_EXIT_CODE! >> "!ERROR_LOG!"
    echo Timestamp: !date! !time! >> "!ERROR_LOG!"
    echo Mode: %MODE_NAME% >> "!ERROR_LOG!"
    echo Flags: !FLAGS! >> "!ERROR_LOG!"
    echo. >> "!ERROR_LOG!"
    echo ================================================================================ >> "!ERROR_LOG!"
    echo DIAGNOSTICS >> "!ERROR_LOG!"
    echo ================================================================================ >> "!ERROR_LOG!"
    echo. >> "!ERROR_LOG!"
    
    REM Check if Java log exists and append it
    if exist "%TESTDIR%\logs\RISBackend.log" (
        echo --- RISBackend.log (last 100 lines) --- >> "!ERROR_LOG!"
        powershell -NoProfile -Command "Get-Content '%TESTDIR%\logs\RISBackend.log' -Tail 100" >> "!ERROR_LOG!" 2>&1
        echo. >> "!ERROR_LOG!"
    ) else (
        echo RISBackend.log not found >> "!ERROR_LOG!"
        echo. >> "!ERROR_LOG!"
    )
    
    REM Check for extracted book directory
    if exist "%TESTDIR%\input\!ISBN!" (
        echo Book extracted to: %TESTDIR%\input\!ISBN! >> "!ERROR_LOG!"
    ) else (
        echo WARNING: Book directory not found - extraction may have failed >> "!ERROR_LOG!"
    )
    echo. >> "!ERROR_LOG!"
    
    REM List any files created
    echo Files in test\output: >> "!ERROR_LOG!"
    if exist "%TESTDIR%\output\*.*" (
        dir /b "%TESTDIR%\output\*.*" >> "!ERROR_LOG!" 2>&1
    ) else (
        echo (none) >> "!ERROR_LOG!"
    )
    echo. >> "!ERROR_LOG!"
    
    echo Files in test\temp: >> "!ERROR_LOG!"
    if exist "%TESTDIR%\temp\*.*" (
        dir /b "%TESTDIR%\temp\*.*" >> "!ERROR_LOG!" 2>&1
    ) else (
        echo (none) >> "!ERROR_LOG!"
    )
    echo. >> "!ERROR_LOG!"
    
    echo ================================================================================ >> "!ERROR_LOG!"
    echo END OF ERROR LOG >> "!ERROR_LOG!"
    echo ================================================================================ >> "!ERROR_LOG!"
    
    echo   ^> Detailed error log saved to: !ERROR_LOG!
    echo [!date! !time!] Error log created: !ERROR_LOG! >> "%BATCH_LOG%"

    REM Even on failure, try to collect any XML files that were created
    if exist "%TESTDIR%\output\*.xml" xcopy /Y "%TESTDIR%\output\*.xml" "!ISBN_OUTPUT!\" >nul 2>&1
    if exist "%TESTDIR%\R2v2-XMLbyISBN\!ISBN!\xml\*.xml" xcopy /Y "%TESTDIR%\R2v2-XMLbyISBN\!ISBN!\xml\*.xml" "!ISBN_OUTPUT!\" >nul 2>&1
    if exist "%TESTDIR%\temp\*.xml" xcopy /Y "%TESTDIR%\temp\*.xml" "!ISBN_OUTPUT!\" >nul 2>&1

    :AfterBookProcessing
    REM Restore other ZIP files back to input directory
    echo Restoring other books to input folder...
    if exist "%TESTDIR%\.batch_temp\*.zip" (
        move "%TESTDIR%\.batch_temp\*.zip" "%TESTDIR%\input\" >nul 2>&1
    )

    REM Clean up extracted files for this book
    echo Cleaning up extracted files for !ISBN!...
    if exist "%TESTDIR%\input\!ISBN!" rmdir /s /q "%TESTDIR%\input\!ISBN!" 2>nul

    echo.
    
    REM Exit local scope and increment appropriate counter in parent scope
    if !BOOK_EXIT_CODE! EQU 0 (
        endlocal & set /a SUCCESS_COUNT+=1
    ) else (
        endlocal & set /a FAILURE_COUNT+=1
    )
goto :EOF

:BatchComplete
REM Write summary
echo ================================================================================
echo                           BATCH PROCESSING COMPLETE
echo ================================================================================
echo Total Books:    !TOTAL_BOOKS!
echo Successful:     !SUCCESS_COUNT!
echo Failed:         !FAILURE_COUNT!
echo ================================================================================
echo.
echo Output Location: %BATCH_RUN_DIR%
echo.

echo. >> "%BATCH_LOG%"
echo ================================================================================== >> "%BATCH_LOG%"
echo BATCH SUMMARY >> "%BATCH_LOG%"
echo ================================================================================== >> "%BATCH_LOG%"
echo Total Books:    !TOTAL_BOOKS! >> "%BATCH_LOG%"
echo Successful:     !SUCCESS_COUNT! >> "%BATCH_LOG%"
echo Failed:         !FAILURE_COUNT! >> "%BATCH_LOG%"
echo Completed:      !date! !time! >> "%BATCH_LOG%"
echo ================================================================================== >> "%BATCH_LOG%"

REM Clean up batch temp directory
if exist "%TESTDIR%\.batch_temp" rmdir /s /q "%TESTDIR%\.batch_temp" 2>nul

REM Create final ZIP archive of all processed books
echo Creating final archive...
set "FINAL_ZIP=%BATCH_OUTPUT_ROOT%\Batch_%TIMESTAMP%_%MODE_NAME%.zip"

if exist "%FINAL_ZIP%" del /f /q "%FINAL_ZIP%" >nul 2>&1

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%BATCH_RUN_DIR%\*' -DestinationPath '%FINAL_ZIP%' -Force"


if exist "%FINAL_ZIP%" (
    echo.
    echo Final archive created: %FINAL_ZIP%
    echo. >> "%BATCH_LOG%"
    echo Final Archive: %FINAL_ZIP% >> "%BATCH_LOG%"
) else (
    echo.
    echo Warning: Could not create final ZIP archive
)

echo.
echo Press any key to exit...
pause >nul

popd >nul
goto :EOF

REM ============================================================================
REM Subroutine: Run Bookloader for a single ISBN
REM ============================================================================
:RunBookloader
    setlocal EnableExtensions EnableDelayedExpansion
    set "RUN_ISBN=%~1"
    set "RUN_FLAGS=%~2"

    REM Find Java executable
    if defined JAVA_HOME (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    ) else (
        set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot\bin\java.exe"
    )

    if not exist "!JAVA_EXE!" (
        echo ERROR: Java executable not found at: !JAVA_EXE!
        endlocal & exit /b 1
    )

    REM Build classpath (BASEDIR points to project root that contains lib\ and build\)
    set "CP=%BASEDIR%\build\classes"
    for %%J in ("%BASEDIR%\lib\*.jar") do set "CP=!CP!;%%~fJ"
    for %%J in ("%BASEDIR%\lib\jakarta\*.jar") do set "CP=!CP!;%%~fJ"
    for %%J in ("%BASEDIR%\lib\jdbc\*.jar") do set "CP=!CP!;%%~fJ"
    for %%J in ("%BASEDIR%\lib\saxon\*.jar") do set "CP=!CP!;%%~fJ"
    for %%J in ("%BASEDIR%\lib\textml\*.jar") do set "CP=!CP!;%%~fJ"
    for %%J in ("%BASEDIR%\lib\xalan\*.jar") do set "CP=!CP!;%%~fJ"
    for %%J in ("%BASEDIR%\lib\xerces\*.jar") do set "CP=!CP!;%%~fJ"

    REM Run the bookloader with proper JVM arguments
    REM Output is shown on console but also logged for diagnostics on failure
    "!JAVA_EXE!" -Xms1g -Xmx2g ^
        --enable-native-access=ALL-UNNAMED ^
        -Djdk.xml.entityExpansionLimit=10000 ^
        -Djdk.xml.totalEntitySizeLimit=1000000 ^
        -Djava.security.policy=java.ris.policy ^
        -cp "!CP!" ^
        com.rittenhouse.RIS.Main !RUN_FLAGS!

    set "RC=!ERRORLEVEL!"
    endlocal & exit /b %RC%
