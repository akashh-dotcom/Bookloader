param(
    [switch]$Update,
    [int]$HeartbeatSeconds = 15,
    [int]$BatchTimeoutMinutes = 240,
    [switch]$PauseAtEnd,
    [switch]$FastMode
)

<#
.SYNOPSIS
    Processes incoming EPUB/ZIP files locally - mimics staging environment.

.DESCRIPTION
    This script monitors the local incoming directory for book files and processes 
    them through the LoadBook pipeline, exactly like staging but running locally.

.PARAMETER FastMode
    Use fast processing mode (--skipLinks flag in Java loader)

.PARAMETER Update
    Allow updating existing resources in the database

.NOTES
    Local version of ProcessIncomingZipsNew.ps1
    Log Files Generated: {ISBN}_SUCCESS.log or {ISBN}_FAIL.log
#>

# =========================
# Local Configuration
# =========================
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

# Use the same directory structure as LoadBook_Local.bat (test folder)
$IncomingDir     = Join-Path $RepoRoot "test\LoadFromHere"
$LogDir          = Join-Path $RepoRoot "test\logs"
$CompletedDir    = Join-Path $LogDir "Completed"
$FailedDir       = Join-Path $LogDir "Failed"
$BatchFile       = Join-Path $ScriptDir "LoadBook_Local.bat"
$BatchWorkDir    = $RepoRoot

# Database configuration (same as staging for testing)
$SqlServer       = "rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com,1433"
$Database        = "STG_RIT001"
$SqlUser         = "RittAdmin"
$SqlPassword     = "49jR6xQybSCDeA5ObTp0"

# =========================
# Create Required Directories
# =========================
foreach ($d in @($LogDir, $IncomingDir, $CompletedDir, $FailedDir)) {
    if (!(Test-Path $d)) {
        Write-Host "Creating directory: $d" -ForegroundColor Yellow
        New-Item -ItemType Directory -Path $d -Force | Out-Null
    }
}

# =========================
# Logging Functions
# =========================
function Write-BookLog {
    param(
        [Parameter(Mandatory = $true)][string]$Isbn,
        [Parameter(Mandatory = $true)][string]$Message,
        [Parameter(Mandatory = $true)][string]$LogFile,
        [ValidateSet('INFO','WARN','ERROR')][string]$Level = 'INFO'
    )
    $line = "[{0:yyyy-MM-dd HH:mm:ss}] {1}: {2}" -f (Get-Date), $Level, $Message
    Write-Host "[$Isbn] $line"
    Add-Content -Path $LogFile -Value $line
}

Write-Host "=== LOCAL Book Processing Script Started ===" -ForegroundColor Cyan
Write-Host "Processing Mode: $(if($FastMode){'FAST (--skipLinks)'}else{'NORMAL'})"
Write-Host "Update Mode: $Update"
Write-Host "Incoming Dir: $IncomingDir"
Write-Host "Backend logs: $RepoRoot\logs\RISBackend.log.*"
Write-Host ""

# =========================
# Database Check Function
# =========================
function Test-ResourceExists {
    param(
        [Parameter(Mandatory = $true)][string]$Isbn,
        [Parameter(Mandatory = $true)][string]$LogFile
    )

    $connString = "Server=$SqlServer;Database=$Database;User Id=$SqlUser;Password=$SqlPassword;Integrated Security=False;"
    $query = "SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.tResource WHERE vchisbn13 = @isbn) THEN 1 ELSE 0 END"

    $conn = New-Object System.Data.SqlClient.SqlConnection $connString
    try {
        $conn.Open()
        $cmd = $conn.CreateCommand()
        $cmd.CommandText = $query
        $param = $cmd.Parameters.Add("@isbn", [System.Data.SqlDbType]::VarChar, 50)
        $param.Value = $Isbn
        $result = $cmd.ExecuteScalar()
        return ([int]$result -eq 1)
    }
    catch {
        Write-BookLog -Isbn $Isbn -Message "Database check failed: $($_.Exception.Message)" -LogFile $LogFile -Level "ERROR"
        return $false
    }
    finally {
        if ($conn.State -eq 'Open') { $conn.Close() }
    }
}

# =========================
# Validation
# =========================
if (!(Test-Path $BatchFile)) {
    Write-Host "ERROR: Batch file not found: $BatchFile" -ForegroundColor Red
    Write-Host "Run this script from the LocalDevTesting directory" -ForegroundColor Red
    if ($PauseAtEnd) {
        Read-Host "Press Enter to exit" | Out-Null
    }
    return
}

if (!(Test-Path $IncomingDir)) {
    Write-Host "ERROR: Incoming directory not found: $IncomingDir" -ForegroundColor Red
    Write-Host "Creating directory structure..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $IncomingDir -Force | Out-Null
    Write-Host "Place ZIP files in: $IncomingDir" -ForegroundColor Green
    if ($PauseAtEnd) {
        Read-Host "Press Enter to exit" | Out-Null
    }
    return
}

# =========================
# Process Book Function
# =========================
function Invoke-LoadBook {
    param(
        [Parameter(Mandatory=$true)][string]$Isbn,
        [Parameter(Mandatory=$true)][string]$BatchFile,
        [Parameter(Mandatory=$true)][string]$WorkingDirectory,
        [Parameter(Mandatory=$true)][string]$LoadBookLogPath,
        [switch]$Update,
        [switch]$FastMode,
        [int]$HeartbeatSeconds = 15,
        [int]$BatchTimeoutMinutes = 240
    )

    $argList = @($Isbn)
    if ($Update) { $argList += "-update" }
    if ($FastMode) { 
        $argList += "--skipLinks"
        $argList += "--noDB"
    }

    Write-BookLog -Isbn $Isbn -Message "Starting LoadBook with args: $($argList -join ' ')" -LogFile $LoadBookLogPath -Level "INFO"

    $p = Start-Process -FilePath $BatchFile `
        -ArgumentList $argList `
        -WorkingDirectory $WorkingDirectory `
        -NoNewWindow `
        -PassThru

    $start = Get-Date
    $timeout = [TimeSpan]::FromMinutes($BatchTimeoutMinutes)
    $lastHeartbeat = $start

    while (-not $p.HasExited) {
        $elapsed = (Get-Date) - $start
        
        if ($elapsed -gt $timeout) {
            Write-BookLog -Isbn $Isbn -Message "TIMEOUT: Process exceeded $BatchTimeoutMinutes minute limit. Terminating." -LogFile $LoadBookLogPath -Level "ERROR"
            & taskkill.exe /PID $p.Id /T /F 2>&1 | Out-Null
            return 9999
        }

        if (((Get-Date) - $lastHeartbeat).TotalSeconds -ge $HeartbeatSeconds) {
            Write-BookLog -Isbn $Isbn -Message "Processing... (Elapsed: $($elapsed.ToString('hh\:mm\:ss')))" -LogFile $LoadBookLogPath -Level "INFO"
            $lastHeartbeat = Get-Date
        }

        Start-Sleep -Seconds 2
    }

    try { $p.WaitForExit() } catch {}

    $code = if ($p.HasExited) { $p.ExitCode } else { 9998 }
    
    return $code
}

# =========================
# Processing Configuration
# =========================
$startTime = Get-Date
Write-Host "Configuration:"
Write-Host "  Incoming:  $IncomingDir"
Write-Host "  Batch:     $BatchFile"
Write-Host "  Mode:      $(if($FastMode){'FAST'}else{'NORMAL'})"
Write-Host "  Update:    $Update"
Write-Host "  Timeout:   $BatchTimeoutMinutes minutes"
Write-Host ""

# Statistics
$stats = @{
    SkippedSuccess = 0
    SkippedFailed = 0
    SkippedExisting = 0
    Success = 0
    Failed = 0
}

# =========================
# Discover Files (ZIP or Folders)
# =========================
try {
    # Look for both ZIP files and folders
    $zipFiles = @(Get-ChildItem -Path $IncomingDir -Filter *.zip -ErrorAction SilentlyContinue)
    $folders = @(Get-ChildItem -Path $IncomingDir -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^\d{13}$' })
    
    $totalItems = $zipFiles.Count + $folders.Count
    
    Write-Host "Found $totalItems item(s) to process:" -ForegroundColor Cyan
    Write-Host "  - $($zipFiles.Count) ZIP file(s)"
    Write-Host "  - $($folders.Count) folder(s)"
    Write-Host ""
}
catch {
    Write-Host "ERROR: Failed to read incoming directory - $($_.Exception.Message)" -ForegroundColor Red
    if ($PauseAtEnd) {
        Read-Host "Press Enter to exit" | Out-Null
    }
    return
}

if ($totalItems -eq 0) {
    Write-Host "No ZIP files or folders to process in $IncomingDir" -ForegroundColor Yellow
    Write-Host "Place ZIP files (e.g., 9781234567890.zip) or folders (e.g., 9781234567890/) in: $IncomingDir" -ForegroundColor Yellow
    if ($PauseAtEnd) {
        Read-Host "Press Enter to exit" | Out-Null
    }
    return
}

# =========================
# Main Processing Loop
# =========================
# Combine ZIPs and folders into a single collection for processing
$allItems = @()
foreach ($zip in $zipFiles) {
    $allItems += @{
        Type = 'ZIP'
        ISBN = [System.IO.Path]::GetFileNameWithoutExtension($zip.Name)
        Path = $zip.FullName
    }
}
foreach ($folder in $folders) {
    $allItems += @{
        Type = 'Folder'
        ISBN = $folder.Name
        Path = $folder.FullName
    }
}

$index = 0
foreach ($item in $allItems) {
    $index++
    $isbn = $item.ISBN
    $itemType = $item.Type
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "[$index/$($allItems.Count)] Processing: $isbn ($itemType)" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    # Initialize log file with timestamp
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $processingLog = Join-Path $LogDir ("{0}_{1}_Processing.log" -f $isbn, $timestamp)
    
    # Check if already processed
    $successMarker = Join-Path $CompletedDir "$isbn.txt"
    $failureMarker = Join-Path $FailedDir "$isbn.txt"

    if (Test-Path $successMarker) {
        $stats.SkippedSuccess++
        Write-Host "[$isbn] SKIPPED - Already completed" -ForegroundColor Yellow
        continue
    }
    if (Test-Path $failureMarker) {
        $stats.SkippedFailed++
        Write-Host "[$isbn] SKIPPED - Previously failed" -ForegroundColor Yellow
        continue
    }

    # Initialize log
    Set-Content -Path $processingLog -Value "========================================"
    Add-Content -Path $processingLog -Value "Local Book Processing Log"
    Add-Content -Path $processingLog -Value "ISBN: $isbn"
    Add-Content -Path $processingLog -Value "Started: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))"
    Add-Content -Path $processingLog -Value "Mode: $(if($FastMode){'FAST'}else{'NORMAL'})"
    Add-Content -Path $processingLog -Value "Update: $Update"
    Add-Content -Path $processingLog -Value "========================================"
    Add-Content -Path $processingLog -Value ""
    
    # PHASE 1: Pre-flight checks
    Write-BookLog -Isbn $isbn -Message "PHASE 1: Pre-flight Validation" -LogFile $processingLog -Level "INFO"
    
    if (-not $Update -and -not $FastMode) {
        Write-BookLog -Isbn $isbn -Message "Checking database for existing resource..." -LogFile $processingLog -Level "INFO"
        $exists = Test-ResourceExists -Isbn $isbn -LogFile $processingLog
        if ($exists) {
            $stats.SkippedExisting++
            Write-BookLog -Isbn $isbn -Message "FAILED: Resource exists (use -Update to override)" -LogFile $processingLog -Level "ERROR"
            $failLogName = "{0}_{1}_FAIL.log" -f $isbn, $timestamp
            Rename-Item -Path $processingLog -NewName $failLogName -Force
            $failureContent = "ISBN: $isbn`r`nPhase: Pre-flight`r`nReason: Resource exists`r`nLog: $(Join-Path $LogDir $failLogName)`r`nTimestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
            Set-Content -Path $failureMarker -Value $failureContent
            Write-Host "[$isbn] FAILED - Resource exists" -ForegroundColor Red
            continue
        }
        Write-BookLog -Isbn $isbn -Message "✓ Database check passed" -LogFile $processingLog -Level "INFO"
    } elseif ($FastMode) {
        Write-BookLog -Isbn $isbn -Message "Fast mode - skipping database check (using --noDB)" -LogFile $processingLog -Level "INFO"
    } else {
        Write-BookLog -Isbn $isbn -Message "Update mode - will update existing resource" -LogFile $processingLog -Level "INFO"
    }
    Add-Content -Path $processingLog -Value ""

    # PHASE 2: Book processing
    Write-BookLog -Isbn $isbn -Message "PHASE 2: Book Processing Pipeline" -LogFile $processingLog -Level "INFO"
    Write-BookLog -Isbn $isbn -Message "Starting LoadBook_Local.bat..." -LogFile $processingLog -Level "INFO"
    Add-Content -Path $processingLog -Value ""

    $exitCode = Invoke-LoadBook `
        -Isbn $isbn `
        -BatchFile $BatchFile `
        -WorkingDirectory $BatchWorkDir `
        -LoadBookLogPath $processingLog `
        -Update:$Update `
        -FastMode:$FastMode `
        -HeartbeatSeconds $HeartbeatSeconds `
        -BatchTimeoutMinutes $BatchTimeoutMinutes

    Add-Content -Path $processingLog -Value ""
    if ($exitCode -ne 0) {
        $stats.Failed++
        Write-BookLog -Isbn $isbn -Message "========================================"  -LogFile $processingLog -Level "ERROR"
        Write-BookLog -Isbn $isbn -Message "PROCESSING FAILED - Exit Code: $exitCode" -LogFile $processingLog -Level "ERROR"
        Write-BookLog -Isbn $isbn -Message "========================================"  -LogFile $processingLog -Level "ERROR"
        
        $failLogName = "{0}_{1}_FAIL.log" -f $isbn, $timestamp
        Rename-Item -Path $processingLog -NewName $failLogName -Force
        
        $failureContent = "ISBN: $isbn`r`nPhase: Processing`r`nExit Code: $exitCode`r`nLog: $(Join-Path $LogDir $failLogName)`r`nBackend Logs: $RepoRoot\logs`r`nTimestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        Set-Content -Path $failureMarker -Value $failureContent
        Write-Host "[$isbn] FAILED - Exit code $exitCode" -ForegroundColor Red
    } else {
        $stats.Success++
        Write-BookLog -Isbn $isbn -Message "========================================"  -LogFile $processingLog -Level "INFO"
        Write-BookLog -Isbn $isbn -Message "PROCESSING COMPLETED SUCCESSFULLY" -LogFile $processingLog -Level "INFO"
        Write-BookLog -Isbn $isbn -Message "========================================"  -LogFile $processingLog -Level "INFO"
        
        $successLogName = "{0}_{1}_SUCCESS.log" -f $isbn, $timestamp
        Rename-Item -Path $processingLog -NewName $successLogName -Force
        
        $successContent = "ISBN: $isbn`r`nStatus: SUCCESS`r`nLog: $(Join-Path $LogDir $successLogName)`r`nBackend Logs: $RepoRoot\logs`r`nTimestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        Set-Content -Path $successMarker -Value $successContent
        Write-Host "[$isbn] SUCCESS" -ForegroundColor Green
    }
    Write-Host ""
}

# =========================
# Summary
# =========================
$elapsed = (Get-Date) - $startTime
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Processing Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total Files: $($allItems.Count)"
Write-Host "Successful: $($stats.Success)" -ForegroundColor Green
Write-Host "Failed: $($stats.Failed)" -ForegroundColor $(if($stats.Failed -gt 0){"Red"}else{"White"})
Write-Host "Skipped (Completed): $($stats.SkippedSuccess)" -ForegroundColor Yellow
Write-Host "Skipped (Failed): $($stats.SkippedFailed)" -ForegroundColor Yellow
Write-Host "Skipped (Exists): $($stats.SkippedExisting)" -ForegroundColor Yellow
Write-Host "Elapsed Time: $($elapsed.ToString('hh\:mm\:ss'))"
Write-Host ""
Write-Host "Output Locations:"
Write-Host "  XML:    $RepoRoot\test\finalOutput\xml"
Write-Host "  Images: $RepoRoot\test\finalOutput\images"
Write-Host "  Logs:   $LogDir"
Write-Host ""

if ($PauseAtEnd) {
    Read-Host "Press Enter to exit" | Out-Null
}
