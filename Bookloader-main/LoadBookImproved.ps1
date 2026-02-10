param(
    [string]$ISBN = "",
    [ValidateSet("1", "2", "3", "DB-NoP", "DB-Full", "Fast")]
    [string]$Mode = "",
    [switch]$Batch,
    [switch]$Help
)

<#
.SYNOPSIS
    Unified book processing script - handles single or batch processing with multiple modes

.DESCRIPTION
    LoadBookImproved.ps1 replaces LoadBook_Local.bat and ProcessIncomingBooks_Local.ps1
    with a unified solution that handles both single ISBN and batch processing.
    
    Three Processing Modes:
    1. DB-NoP (Default): Database + Update, No PubMed lookups
    2. DB-Full: Database + Update + PubMed lookups (slowest, most complete)
    3. Fast: No Database, No PubMed, No Linking (fastest)

.PARAMETER ISBN
    Single ISBN to process. If omitted, batch mode is assumed.

.PARAMETER Mode
    Processing mode: 1/DB-NoP, 2/DB-Full, or 3/Fast

.PARAMETER Batch
    Explicitly enable batch mode (processes all ZIPs in test\input)

.PARAMETER Help
    Show this help message

.EXAMPLES
    # Single book with default mode (DB-NoP)
    .\LoadBookImproved.ps1 -ISBN 9781234567890
    
    # Single book with full database mode
    .\LoadBookImproved.ps1 -ISBN 9781234567890 -Mode 2
    
    # Single book with fast mode
    .\LoadBookImproved.ps1 -ISBN 9781234567890 -Mode Fast
    
    # Batch processing with interactive mode selection
    .\LoadBookImproved.ps1 -Batch
    
    # Batch processing with specific mode
    .\LoadBookImproved.ps1 -Batch -Mode 1

.NOTES
    Database: 127.0.0.1:11433, STG_RIT001
    Input: test\input\*.zip
    Output: test\finalOutput\{ISBN}\xml and test\finalOutput\{ISBN}\images
    Logs: test\logs\{ISBN}_{timestamp}_{status}.log
#>

# =========================
# Show Help
# =========================
if ($Help) {
    Get-Help $MyInvocation.MyCommand.Path -Detailed
    return
}

# =========================
# Configuration
# =========================
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = $ScriptDir

# Directories
$InputDir        = Join-Path $RepoRoot "test\input"
$TempDir         = Join-Path $RepoRoot "test\temp"
$OutputDir       = Join-Path $RepoRoot "test\finalOutput"
$LogDir          = Join-Path $RepoRoot "test\logs"
$MediaDir        = Join-Path $RepoRoot "test\media"
$R2Dir           = Join-Path $RepoRoot "test\R2v2-XMLbyISBN"
$BatchOutputDir  = Join-Path $RepoRoot "test\batchoutput"

# Database configuration
$SqlServer       = "127.0.0.1,11433"
$Database        = "STG_RIT001"
$SqlUser         = "RittAdmin"
$SqlPassword     = "49jR6xQybSCDeA5ObTp0"

# Java configuration
if ($env:JAVA_HOME) {
    $JavaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $JavaExe = "C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot\bin\java.exe"
}

# =========================
# Mode Configuration
# =========================
$ModeDefinitions = @{
    "1" = @{
        Name = "DB-NoP"
        Description = "Database + Update, No PubMed"
        Flags = "--update --normal --skipPMID"
    }
    "2" = @{
        Name = "DB-Full"
        Description = "Database + Update + PubMed (Full)"
        Flags = "--update --normal"
    }
    "3" = @{
        Name = "Fast"
        Description = "Ultra Fast (No DB, No PubMed, No Linking)"
        Flags = "--noDB --skipPMID --skipLinks"
    }
}

# Normalize mode aliases
$ModeMap = @{
    "1" = "1"
    "DB-NoP" = "1"
    "2" = "2"
    "DB-Full" = "2"
    "3" = "3"
    "Fast" = "3"
}

# =========================
# Helper Functions
# =========================
function Write-ColorMessage {
    param(
        [string]$Message,
        [string]$Color = "White",
        [switch]$NoNewline
    )
    if ($NoNewline) {
        Write-Host $Message -ForegroundColor $Color -NoNewline
    } else {
        Write-Host $Message -ForegroundColor $Color
    }
}

function Write-BookLog {
    param(
        [string]$ISBN,
        [string]$Message,
        [string]$LogFile,
        [ValidateSet('INFO','WARN','ERROR','SUCCESS')]
        [string]$Level = 'INFO'
    )
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] ${Level}: $Message"
    
    $color = switch ($Level) {
        'ERROR' { 'Red' }
        'WARN' { 'Yellow' }
        'SUCCESS' { 'Green' }
        default { 'White' }
    }
    
    if ($ISBN) {
        Write-Host "[$ISBN] $line" -ForegroundColor $color
    } else {
        Write-Host $line -ForegroundColor $color
    }
    
    if ($LogFile -and (Test-Path (Split-Path $LogFile))) {
        Add-Content -Path $LogFile -Value $line
    }
}

function Test-ResourceExists {
    param(
        [string]$ISBN,
        [string]$LogFile
    )

    $connString = "Server=$SqlServer;Database=$Database;User Id=$SqlUser;Password=$SqlPassword;Integrated Security=False;"
    $query = "SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.tResource WHERE vchisbn13 = @isbn) THEN 1 ELSE 0 END"

    $conn = New-Object System.Data.SqlClient.SqlConnection $connString
    try {
        $conn.Open()
        $cmd = $conn.CreateCommand()
        $cmd.CommandText = $query
        $param = $cmd.Parameters.Add("@isbn", [System.Data.SqlDbType]::VarChar, 50)
        $param.Value = $ISBN
        $result = $cmd.ExecuteScalar()
        return ([int]$result -eq 1)
    }
    catch {
        Write-BookLog -ISBN $ISBN -Message "Database check failed: $($_.Exception.Message)" -LogFile $LogFile -Level "ERROR"
        return $false
    }
    finally {
        if ($conn.State -eq 'Open') { $conn.Close() }
    }
}

function Initialize-Directories {
    $dirs = @($InputDir, $TempDir, $OutputDir, $LogDir, $MediaDir, $R2Dir, $BatchOutputDir)
    foreach ($dir in $dirs) {
        if (!(Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
    }
}

function Clear-WorkingDirectories {
    param([string]$LogFile)
    
    Write-BookLog -Message "Cleaning working directories..." -LogFile $LogFile -Level "INFO"
    
    $dirsToClean = @($TempDir, $MediaDir, $R2Dir)
    foreach ($dir in $dirsToClean) {
        if (Test-Path $dir) {
            Get-ChildItem $dir -Recurse -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    
    # Clean up extracted directories in input folder
    Get-ChildItem $InputDir -Directory -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

function Get-JavaClasspath {
    $classpath = Join-Path $RepoRoot "build\classes"
    
    $libDirs = @("jakarta", "jdbc", "saxon", "xalan", "xerces", "textml", "javamail")
    foreach ($libDir in $libDirs) {
        $libPath = Join-Path $RepoRoot "lib\$libDir"
        if (Test-Path $libPath) {
            Get-ChildItem $libPath -Filter *.jar | ForEach-Object {
                $classpath += ";$($_.FullName)"
            }
        }
    }
    
    # Root lib folder
    $rootLib = Join-Path $RepoRoot "lib"
    Get-ChildItem $rootLib -Filter *.jar | ForEach-Object {
        $classpath += ";$($_.FullName)"
    }
    
    return $classpath
}

function Invoke-JavaBookLoader {
    param(
        [string]$ISBN,
        [string]$Flags,
        [string]$LogFile,
        [int]$TimeoutMinutes = 120
    )
    
    Write-BookLog -ISBN $ISBN -Message "Starting Java Book Loader..." -LogFile $LogFile -Level "INFO"
    Write-BookLog -ISBN $ISBN -Message "Flags: $Flags" -LogFile $LogFile -Level "INFO"
    
    $classpath = Get-JavaClasspath
    
    $javaArgs = @(
        "-Xms1g"
        "-Xmx2g"
        "--enable-native-access=ALL-UNNAMED"
        "-Djdk.xml.entityExpansionLimit=10000"
        "-Djdk.xml.totalEntitySizeLimit=1000000"
        "-Djava.security.policy=java.ris.policy"
        "-cp"
        $classpath
        "com.rittenhouse.RIS.Main"
    )
    
    if ($Flags) {
        $javaArgs += $Flags.Split(" ")
    }
    
    # Debug: Log the full command
    Write-BookLog -ISBN $ISBN -Message "Java executable: $JavaExe" -LogFile $LogFile -Level "INFO"
    Write-BookLog -ISBN $ISBN -Message "Java arguments: $($javaArgs -join ' ')" -LogFile $LogFile -Level "INFO"
    
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $JavaExe
    $psi.Arguments = $javaArgs -join " "
    $psi.WorkingDirectory = $RepoRoot
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    
    $outputBuilder = New-Object System.Text.StringBuilder
    $errorBuilder = New-Object System.Text.StringBuilder
    
    $outputHandler = {
        if ($EventArgs.Data) {
            $outputBuilder.AppendLine($EventArgs.Data) | Out-Null
        }
    }
    
    $errorHandler = {
        if ($EventArgs.Data) {
            $errorBuilder.AppendLine($EventArgs.Data) | Out-Null
        }
    }
    
    Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action $outputHandler | Out-Null
    Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action $errorHandler | Out-Null
    
    $process.Start() | Out-Null
    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()
    
    $startTime = Get-Date
    $lastHeartbeat = $startTime
    $timeoutMs = $TimeoutMinutes * 60 * 1000
    
    while (!$process.HasExited) {
        Start-Sleep -Milliseconds 500
        
        $elapsed = (Get-Date) - $startTime
        if ($elapsed.TotalMilliseconds -gt $timeoutMs) {
            Write-BookLog -ISBN $ISBN -Message "TIMEOUT after $TimeoutMinutes minutes - terminating process" -LogFile $LogFile -Level "ERROR"
            $process.Kill()
            return @{ ExitCode = 9999; Output = ""; Error = "Process timeout" }
        }
        
        if (((Get-Date) - $lastHeartbeat).TotalSeconds -gt 30) {
            Write-BookLog -ISBN $ISBN -Message "Processing... (Elapsed: $($elapsed.ToString('mm\:ss')))" -LogFile $LogFile -Level "INFO"
            $lastHeartbeat = Get-Date
        }
    }
    
    $process.WaitForExit()
    $exitCode = $process.ExitCode
    
    # Cleanup event handlers
    Get-EventSubscriber | Where-Object { $_.SourceObject -eq $process } | Unregister-Event
    
    $output = $outputBuilder.ToString()
    $error = $errorBuilder.ToString()
    
    if ($output) {
        Add-Content -Path $LogFile -Value "`n--- Java Output ---"
        Add-Content -Path $LogFile -Value $output
    }
    if ($error) {
        Add-Content -Path $LogFile -Value "`n--- Java Errors ---"
        Add-Content -Path $LogFile -Value $error
    }
    
    return @{
        ExitCode = $exitCode
        Output = $output
        Error = $error
    }
}

function Process-SingleBook {
    param(
        [string]$ISBN,
        [string]$Flags,
        [string]$ModeName,
        [bool]$CheckDatabase = $true,
        [bool]$IsolateBook = $false,
        [string]$BatchFolder = "",
        [System.IO.FileInfo]$ZipFile = $null
    )
    
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $logFile = Join-Path $LogDir "${ISBN}_${timestamp}_Processing.log"
    
    # Initialize log
    Set-Content -Path $logFile -Value "========================================"
    Add-Content -Path $logFile -Value "LoadBookImproved - Book Processing Log"
    Add-Content -Path $logFile -Value "ISBN: $ISBN"
    Add-Content -Path $logFile -Value "Mode: $ModeName"
    Add-Content -Path $logFile -Value "Flags: $Flags"
    Add-Content -Path $logFile -Value "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    Add-Content -Path $logFile -Value "========================================"
    Add-Content -Path $logFile -Value ""
    
    Write-ColorMessage "`n========================================" -Color Cyan
    Write-ColorMessage "Processing: $ISBN" -Color Cyan
    Write-ColorMessage "Mode: $ModeName" -Color Cyan
    Write-ColorMessage "========================================" -Color Cyan
    
    # Determine ZIP path
    if ($ZipFile) {
        # ZIP file was provided (batch mode with pre-scanned files)
        $zipPath = $ZipFile.FullName
        Write-BookLog -ISBN $ISBN -Message "Using pre-scanned ZIP: $($ZipFile.Name)" -LogFile $logFile -Level "INFO"
    } else {
        # Check if ZIP exists (may have suffix like _all_fixes)
        $zipPath = Join-Path $InputDir "$ISBN.zip"
        if (!(Test-Path $zipPath)) {
            # Try to find ZIP with ISBN prefix
            $zipMatch = Get-ChildItem $InputDir -Filter "${ISBN}*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($zipMatch) {
                $zipPath = $zipMatch.FullName
                Write-BookLog -ISBN $ISBN -Message "Found ZIP with suffix: $($zipMatch.Name)" -LogFile $logFile -Level "INFO"
            } else {
                Write-BookLog -ISBN $ISBN -Message "ERROR: ZIP file not found for ISBN: $ISBN" -LogFile $logFile -Level "ERROR"
                return $false
            }Path $logFile -NewName "${ISBN}_${timestamp}_FAIL.log" -Force
            return $false
        }
    }
    
    Write-BookLog -ISBN $ISBN -Message "Found ZIP: $zipPath" -LogFile $logFile -Level "INFO"
    
    # Book Isolation: Move other ZIPs away temporarily if in batch mode
    $tempIsolationDir = $null
    $isolatedZips = @()
    if ($IsolateBook) {
        $tempIsolationDir = Join-Path $InputDir ".batch_temp"
        if (!(Test-Path $tempIsolationDir)) {
            New-Item -ItemType Directory -Path $tempIsolationDir -Force | Out-Null
        }
        
        # Move all OTHER ZIP files to temp directory
        $allZips = Get-ChildItem $InputDir -Filter *.zip
        foreach ($zip in $allZips) {
            if ($zip.FullName -ne $zipPath) {
                $destPath = Join-Path $tempIsolationDir $zip.Name
                Move-Item -Path $zip.FullName -Destination $destPath -Force
                $isolatedZips += $zip.Name
            }
        }
        
        if ($isolatedZips.Count -gt 0) {
            Write-BookLog -ISBN $ISBN -Message "Isolated book: Moved $($isolatedZips.Count) other ZIP(s) to temporary location" -LogFile $logFile -Level "INFO"
        }
    }
    
    try {
        # Database check (if applicable)
        if ($CheckDatabase) {
            Write-BookLog -ISBN $ISBN -Message "Checking database for existing resource..." -LogFile $logFile -Level "INFO"
            $exists = Test-ResourceExists -ISBN $ISBN -LogFile $logFile
            if ($exists) {
                Write-BookLog -ISBN $ISBN -Message "ERROR: Resource already exists in database" -LogFile $logFile -Level "ERROR"
                Write-BookLog -ISBN $ISBN -Message "Use Mode 1 or 2 with --update flag to override" -LogFile $logFile -Level "ERROR"
                return $false
            }
            Write-BookLog -ISBN $ISBN -Message "Database check passed" -LogFile $logFile -Level "INFO"
        }
        
        # Clean working directories
        Clear-WorkingDirectories -LogFile $logFile
        
        # Run Java Book Loader
        Write-BookLog -ISBN $ISBN -Message "Invoking Java Book Loader..." -LogFile $logFile -Level "INFO"
        $result = Invoke-JavaBookLoader -ISBN $ISBN -Flags $Flags -LogFile $logFile
        
        if ($result.ExitCode -ne 0) {
            Write-BookLog -ISBN $ISBN -Message "FAILED - Exit code: $($result.ExitCode)" -LogFile $logFile -Level "ERROR"
            return $false
        }
        
        # Verify output
        $xmlOutput = Join-Path $RepoRoot "test\R2v2-XMLbyISBN\$ISBN\xml"
        if (!(Test-Path $xmlOutput)) {
            Write-BookLog -ISBN $ISBN -Message "WARNING: Expected XML output not found: $xmlOutput" -LogFile $logFile -Level "WARN"
        } else {
            $xmlCount = (Get-ChildItem $xmlOutput -Filter *.xml -Recurse -ErrorAction SilentlyContinue).Count
            Write-BookLog -ISBN $ISBN -Message "Output verified: $xmlCount XML files" -LogFile $logFile -Level "SUCCESS"
        }
        
        
        # Handle batch output folder if specified
        if ($BatchFolder) {
            # Find the actual ISBN folder created by Java (may differ from ZIP filename)
            $r2RootDir = Join-Path $RepoRoot "test\R2v2-XMLbyISBN"
            $actualFolders = Get-ChildItem $r2RootDir -Directory -ErrorAction SilentlyContinue
            
            if ($actualFolders.Count -eq 0) {
                Write-BookLog -ISBN $ISBN -Message "WARNING: No output folder found in R2v2-XMLbyISBN" -LogFile $logFile -Level "WARN"
            }
            else {
                # Use the first (and should be only) folder created
                $sourceXmlDir = $actualFolders[0].FullName
                $actualISBN = $actualFolders[0].Name
                
                Write-BookLog -ISBN $ISBN -Message "Found output for ISBN: $actualISBN (ZIP: $ISBN)" -LogFile $logFile -Level "INFO"
                
                # Create destination using ZIP filename for consistency
                $destDir = Join-Path $BatchFolder "${ISBN}_PASS"
                
                if (Test-Path $sourceXmlDir) {
                    Write-BookLog -ISBN $ISBN -Message "Moving output to batch folder: $destDir" -LogFile $logFile -Level "INFO"
                    
                    # Create destination and move
                    if (!(Test-Path $destDir)) {
                        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
                    }
                    
                    # Copy XML and images folders
                    $xmlSrc = Join-Path $sourceXmlDir "xml"
                    $imgSrc = Join-Path $sourceXmlDir "images"
                    
                    if (Test-Path $xmlSrc) {
                        Copy-Item -Path $xmlSrc -Destination $destDir -Recurse -Force
                        Write-BookLog -ISBN $ISBN -Message "Copied XML files to batch output" -LogFile $logFile -Level "INFO"
                    }
                    
                    if (Test-Path $imgSrc) {
                        Copy-Item -Path $imgSrc -Destination $destDir -Recurse -Force
                        Write-BookLog -ISBN $ISBN -Message "Copied image files to batch output" -LogFile $logFile -Level "INFO"
                    }
                }
            }
        }
        
        Write-ColorMessage "`n[$ISBN] SUCCESS" -Color Green
        return $true
    }
    catch {
        Write-BookLog -ISBN $ISBN -Message "EXCEPTION: $($_.Exception.Message)" -LogFile $logFile -Level "ERROR"
        
        # Handle batch output for failures
        if ($BatchFolder) {
            $failDir = Join-Path $BatchOutputDir "${ISBN}_FAIL"
            if (!(Test-Path $failDir)) {
                New-Item -ItemType Directory -Path $failDir -Force | Out-Null
            }
            
            # Copy log to fail directory
            if (Test-Path $logFile) {
                Copy-Item -Path $logFile -Destination $failDir -Force
            }
        }
        
        return $false
    }
    finally {
        # Restore isolated ZIPs
        if ($tempIsolationDir -and (Test-Path $tempIsolationDir)) {
            foreach ($zipName in $isolatedZips) {
                $tempPath = Join-Path $tempIsolationDir $zipName
                if (Test-Path $tempPath) {
                    Move-Item -Path $tempPath -Destination $InputDir -Force
                }
            }
            
            # Remove temp directory if empty
            if ((Get-ChildItem $tempIsolationDir).Count -eq 0) {
                Remove-Item $tempIsolationDir -Force
            }
            
            Write-BookLog -ISBN $ISBN -Message "Restored $($isolatedZips.Count) isolated ZIP(s)" -LogFile $logFile -Level "INFO"
        }
        
        # Rename log file based on result
        if (Test-Path $logFile) {
            $finalStatus = if ($?) { "SUCCESS" } else { "FAIL" }
            $finalLogName = "${ISBN}_${timestamp}_${finalStatus}.log"
            Rename-Item -Path $logFile -NewName $finalLogName -Force -ErrorAction SilentlyContinue
            
            # Copy log to batch failure folder if failed
            if ($BatchFolder -and $finalStatus -eq "FAIL") {
                $failDir = Join-Path $BatchOutputDir "${ISBN}_FAIL"
                if (!(Test-Path $failDir)) {
                    New-Item -ItemType Directory -Path $failDir -Force | Out-Null
                }
                $finalLog = Join-Path $LogDir $finalLogName
                if (Test-Path $finalLog) {
                    Copy-Item -Path $finalLog -Destination $failDir -Force
                }
            }
            Rename-Item -Path $logFile -NewName $finalLogName -Force -ErrorAction SilentlyContinue
        }
    }
}

# =========================
# Main Script Logic
# =========================
Write-ColorMessage "`n========================================" -Color Cyan
Write-ColorMessage "LoadBookImproved - Unified Book Processor" -Color Cyan
Write-ColorMessage "========================================`n" -Color Cyan

# Initialize directories
Initialize-Directories

# Determine operation mode
$isBatchMode = $Batch -or (!$ISBN)

# Interactive mode selection if not specified
if (!$Mode) {
    Write-ColorMessage "Select Processing Mode:" -Color Yellow
    Write-ColorMessage "  1. DB-NoP (Default): Database + Update, No PubMed" -Color White
    Write-ColorMessage "  2. DB-Full: Database + Update + PubMed (Full)" -Color White
    Write-ColorMessage "  3. Fast: Ultra Fast (No DB, No PubMed, No Linking)" -Color White
    Write-ColorMessage ""
    $modeInput = Read-Host "Enter mode (1-3)"
    
    if (!$modeInput) {
        $modeInput = "1"
    }
    
    if (!$ModeDefinitions.ContainsKey($modeInput)) {
        Write-ColorMessage "Invalid mode. Using default (1)." -Color Yellow
        $modeInput = "1"
    }
    
    $Mode = $modeInput
}

# Normalize mode
if ($ModeMap.ContainsKey($Mode)) {
    $Mode = $ModeMap[$Mode]
}

if (!$ModeDefinitions.ContainsKey($Mode)) {
    Write-ColorMessage "ERROR: Invalid mode '$Mode'. Use 1, 2, 3, DB-NoP, DB-Full, or Fast." -Color Red
    return
}

$selectedMode = $ModeDefinitions[$Mode]
$flags = $selectedMode.Flags
$modeName = $selectedMode.Name
$checkDB = ($Mode -ne "3")  # Don't check DB for Fast mode

Write-ColorMessage "Mode: $modeName - $($selectedMode.Description)" -Color Green
Write-ColorMessage "Flags: $flags`n" -Color Gray

# Single or Batch Processing
if ($isBatchMode) {
    Write-ColorMessage "=== BATCH MODE ===" -Color Cyan
    Write-ColorMessage "Processing all ZIP files in: $InputDir`n" -Color White
    
    $zipFiles = Get-ChildItem $InputDir -Filter *.zip -ErrorAction SilentlyContinue
    
    if ($zipFiles.Count -eq 0) {
        Write-ColorMessage "No ZIP files found in $InputDir" -Color Yellow
        Write-ColorMessage "Place ZIP files (e.g., 9781234567890.zip) in the input directory." -Color Yellow
        return
    }
    
    Write-ColorMessage "Found $($zipFiles.Count) book(s) to process:`n" -Color Cyan
    foreach ($zip in $zipFiles) {
        Write-ColorMessage "  - $($zip.Name)" -Color White
    }
    Write-ColorMessage ""
    
    # Create batch output folder with timestamp
    $batchTimestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $batchFolderName = "Batch_$batchTimestamp"
    $batchFolder = Join-Path $BatchOutputDir $batchFolderName
    New-Item -ItemType Directory -Path $batchFolder -Force | Out-Null
    Write-ColorMessage "Batch output folder: $batchFolder`n" -Color Cyan
    
    $stats = @{
        Success = 0
        Failed = 0
        Skipped = 0
    }
    
    $startTime = Get-Date
    
    foreach ($zip in $zipFiles) {
        # Extract ISBN from filename (handle suffixes like _all_fixes)
        $baseName = [System.IO.Path]::GetFileNameWithoutExtension($zip.Name)
        $bookISBN = $baseName.Split('_')[0]
        
        # Pass the actual ZIP file object to ensure we have the right file
        $success = Process-SingleBook -ISBN $bookISBN -Flags $flags -ModeName $modeName -CheckDatabase $checkDB -IsolateBook $true -BatchFolder $batchFolder -ZipFile $zip
        
        if ($success) {
            $stats.Success++
        } else {
            $stats.Failed++
        }
        
        Write-ColorMessage ""
    }
    
    $elapsed = (Get-Date) - $startTime
    
    Write-ColorMessage "`n========================================" -Color Cyan
    Write-ColorMessage "Batch Processing Complete" -Color Cyan
    Write-ColorMessage "========================================" -Color Cyan
    Write-ColorMessage "Total Books: $($zipFiles.Count)" -Color White
    Write-ColorMessage "Successful: $($stats.Success)" -Color Green
    Write-ColorMessage "Failed: $($stats.Failed)" -Color $(if($stats.Failed -gt 0){"Red"}else{"White"})
    Write-ColorMessage "Elapsed Time: $($elapsed.ToString('hh\:mm\:ss'))" -Color White
    
    # Zip the batch folder if there are successes
    if ($stats.Success -gt 0) {
        Write-ColorMessage "`nCreating batch ZIP archive..." -Color Cyan
        $zipFileName = "$batchFolderName.zip"
        $zipFilePath = Join-Path $BatchOutputDir $zipFileName
        
        try {
            # Remove old zip if exists
            if (Test-Path $zipFilePath) {
                Remove-Item $zipFilePath -Force
            }
            
            # Create zip
            Compress-Archive -Path "$batchFolder\*" -DestinationPath $zipFilePath -Force
            Write-ColorMessage "Created: $zipFilePath" -Color Green
            
            # Remove the unzipped folder after successful zip
            Remove-Item $batchFolder -Recurse -Force
            Write-ColorMessage "Cleaned up batch folder (contents now in ZIP)" -Color Gray
        }
        catch {
            Write-ColorMessage "Warning: Failed to create ZIP: $($_.Exception.Message)" -Color Yellow
            Write-ColorMessage "Batch folder preserved: $batchFolder" -Color Yellow
        }
    } else {
        Write-ColorMessage "`nNo successful books to archive. Removing empty batch folder." -Color Yellow
        Remove-Item $batchFolder -Recurse -Force -ErrorAction SilentlyContinue
    }
    
    Write-ColorMessage "`nBatch Output: $BatchOutputDir" -Color White
    Write-ColorMessage "Logs: $LogDir`n" -Color White
    
} else {
    Write-ColorMessage "=== SINGLE BOOK MODE ===" -Color Cyan
    Write-ColorMessage "Processing ISBN: $ISBN`n" -Color White
    
    # In single book mode, check if there are other ZIPs that need isolation
    $allZips = Get-ChildItem $InputDir -Filter *.zip -ErrorAction SilentlyContinue
    $needsIsolation = ($allZips.Count -gt 1)
    
    if ($needsIsolation) {
        Write-ColorMessage "Note: Multiple ZIP files detected - enabling book isolation" -Color Yellow
    }
    
    $success = Process-SingleBook -ISBN $ISBN -Flags $flags -ModeName $modeName -CheckDatabase $checkDB -IsolateBook $needsIsolation
    
    if ($success) {
        Write-ColorMessage "`nOutput: $OutputDir\$ISBN" -Color White
        Write-ColorMessage "Logs: $LogDir`n" -Color White
        exit 0
    } else {
        Write-ColorMessage "`nProcessing failed. Check logs in: $LogDir`n" -Color Red
        exit 1
    }
}
