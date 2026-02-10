# Clean Test Directories (for Batch Processing)
# This version PRESERVES batchoutput folder and only cleans working directories
# Usage:
#   .\clean-test-dirs-for-batch.ps1
#   .\clean-test-dirs-for-batch.ps1 -WhatIf

[CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact='High')]
param()

$ErrorActionPreference = "Continue"

# Load VisualBasic assembly for Recycle Bin support
Add-Type -AssemblyName Microsoft.VisualBasic

# Only clean working directories, NOT batchoutput
$dirsToClean = @(
    "media",
    "temp",
    "output",
    "R2v2-XMLbyISBN"
)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Batch Processing Cleanup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will clean working directories:" -ForegroundColor Yellow
foreach ($dir in $dirsToClean) {
    Write-Host "  - test\$dir" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "PRESERVED (will NOT be deleted):" -ForegroundColor Green
Write-Host "  - test\input (contains source books)" -ForegroundColor Green
Write-Host "  - test\batchoutput (contains results)" -ForegroundColor Green
Write-Host "  - test\logs (contains log files)" -ForegroundColor Green
Write-Host ""

function Send-ToRecycleBin {
    param(
        [Parameter(Mandatory=$true)][string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) { return }

    $item = Get-Item -LiteralPath $Path -Force -ErrorAction SilentlyContinue
    if (-not $item) { return }

    if ($item.PSIsContainer) {
        # Directory
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory(
            $item.FullName,
            [Microsoft.VisualBasic.FileIO.UIOption]::OnlyErrorDialogs,
            [Microsoft.VisualBasic.FileIO.RecycleOption]::SendToRecycleBin
        )
    } else {
        # File
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile(
            $item.FullName,
            [Microsoft.VisualBasic.FileIO.UIOption]::OnlyErrorDialogs,
            [Microsoft.VisualBasic.FileIO.RecycleOption]::SendToRecycleBin
        )
    }
}

$scriptDir = Split-Path -Parent $PSCommandPath
$testDir = $scriptDir

foreach ($dirName in $dirsToClean) {
    $dirPath = Join-Path $testDir $dirName
    
    if (-not (Test-Path -LiteralPath $dirPath)) {
        Write-Host "[SKIP] $dirName - does not exist" -ForegroundColor Gray
        continue
    }
    
    $items = Get-ChildItem -LiteralPath $dirPath -Force -ErrorAction SilentlyContinue
    if ($items.Count -eq 0) {
        Write-Host "[SKIP] $dirName - already empty" -ForegroundColor Gray
        continue
    }
    
    if ($PSCmdlet.ShouldProcess("test\$dirName ($($items.Count) items)", "Send to Recycle Bin")) {
        try {
            foreach ($item in $items) {
                Send-ToRecycleBin -Path $item.FullName
            }
            Write-Host "[OK]   $dirName - cleaned ($($items.Count) items moved to Recycle Bin)" -ForegroundColor Green
        } catch {
            Write-Host "[FAIL] $dirName - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "Cleanup complete!" -ForegroundColor Cyan
Write-Host ""
