# Clean Test Directories - Delete or Recycle
# Deletes files permanently by default, or recycles them if -Recycle is specified.
# Preserves .gitkeep files.
# Usage:
#   .\clean-test-dirs.ps1           (permanently delete)
#   .\clean-test-dirs.ps1 -Recycle  (send to Recycle Bin)

[CmdletBinding()]
param(
    [switch]$Recycle
)

$ErrorActionPreference = "Continue"

$dirsToClean = @(
    "media",
    "temp",
    "input",
    "output",
    "finalOutput",
    "R2v2-XMLbyISBN",
    "batchoutput",
    "logs"
)

$mode = if ($Recycle) { "Recycle Bin" } else { "Permanent Delete" }

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Directory Cleanup ($mode)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Load VisualBasic assembly for Recycle Bin support (only if needed)
if ($Recycle) {
    Add-Type -AssemblyName Microsoft.VisualBasic
}

function Send-ToRecycleBin {
    param([Parameter(Mandatory=$true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) { return }
    $item = Get-Item -LiteralPath $Path -Force -ErrorAction SilentlyContinue
    if (-not $item) { return }

    if ($item.PSIsContainer) {
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory(
            $item.FullName,
            [Microsoft.VisualBasic.FileIO.UIOption]::OnlyErrorDialogs,
            [Microsoft.VisualBasic.FileIO.RecycleOption]::SendToRecycleBin
        )
    } else {
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile(
            $item.FullName,
            [Microsoft.VisualBasic.FileIO.UIOption]::OnlyErrorDialogs,
            [Microsoft.VisualBasic.FileIO.RecycleOption]::SendToRecycleBin
        )
    }
}

foreach ($dir in $dirsToClean) {
    if (Test-Path $dir) {
        Write-Host "Cleaning: $dir" -ForegroundColor Yellow

        $items = Get-ChildItem -LiteralPath $dir -Force -ErrorAction SilentlyContinue

        $cleanedCount = 0
        $preservedCount = 0

        foreach ($item in $items) {
            # Preserve .gitkeep files
            if ($item.Name -ieq ".gitkeep") {
                Write-Host "  [Preserved] .gitkeep" -ForegroundColor Green
                $preservedCount++
                continue
            }

            $actionVerb = if ($Recycle) { "Recycled" } else { "Deleted" }
            
            try {
                if ($Recycle) {
                    Send-ToRecycleBin -Path $item.FullName
                } else {
                    Remove-Item -LiteralPath $item.FullName -Recurse -Force
                }
                Write-Host "  [$actionVerb] $($item.Name)" -ForegroundColor Gray
                $cleanedCount++
            }
            catch {
                Write-Host "  [FAILED] $($item.Name) - $($_.Exception.Message)" -ForegroundColor Red
            }
        }

        $summary = if ($Recycle) { "$cleanedCount recycled" } else { "$cleanedCount deleted" }
        Write-Host "  Summary: $summary, $preservedCount preserved" -ForegroundColor Cyan
    }
    else {
        Write-Host "Skipping: $dir (does not exist)" -ForegroundColor DarkGray
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Cleanup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
