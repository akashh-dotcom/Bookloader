# Setup-LocalEnvironment.ps1
# Creates the local directory structure for full book processing

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Local Book Processing Environment Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get the script directory (LocalDevTesting folder)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
# Get the parent directory (Bookloader root)
$bookloaderRoot = Split-Path -Parent $scriptDir

# Create local directory structure
$dirs = @(
    "C:\BookloaderLocal\Incoming",
    "C:\BookloaderLocal\Logs",
    "C:\BookloaderLocal\Completed",
    "C:\BookloaderLocal\Failed",
    "C:\BookloaderLocal\FinalXML",
    "C:\BookloaderLocal\FinalImages",
    "C:\BookloaderLocal\Temp",
    "C:\BookloaderLocal\TransformedContent"
)

Write-Host "Creating directory structure..." -ForegroundColor Yellow
Write-Host ""

foreach ($dir in $dirs) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "   Created: $dir" -ForegroundColor Green
    } else {
        Write-Host "   Exists:  $dir" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Copying DTD files to test/temp/dtd..." -ForegroundColor Yellow

# Copy entire DTD structure from dtd/v1.1 to test/temp/dtd/v1.1
# These files must persist and never be deleted during cleaning
$sourceDtdDir = Join-Path $bookloaderRoot "dtd\v1.1"
$destDtdDir = Join-Path $bookloaderRoot "test\temp\dtd\v1.1"

if (Test-Path $sourceDtdDir) {
    if (!(Test-Path $destDtdDir)) {
        New-Item -ItemType Directory -Path $destDtdDir -Force | Out-Null
        Write-Host "   Created: $destDtdDir" -ForegroundColor Green
    }
    
    # Copy all DTD files (.dtd, .mod, .dec, .ent)
    $dtdFiles = Get-ChildItem -Path $sourceDtdDir -Include *.dtd,*.mod,*.dec,*.ent -Recurse
    $copiedCount = 0
    foreach ($dtdFile in $dtdFiles) {
        $relativePath = $dtdFile.FullName.Substring($sourceDtdDir.Length + 1)
        $destPath = Join-Path $destDtdDir $relativePath
        $destFileDir = Split-Path -Parent $destPath
        if (!(Test-Path $destFileDir)) {
            New-Item -ItemType Directory -Path $destFileDir -Force | Out-Null
        }
        Copy-Item -Path $dtdFile.FullName -Destination $destPath -Force
        $copiedCount++
    }
    Write-Host "   Copied $copiedCount DTD/entity/module files" -ForegroundColor Green
    Write-Host "   Note: These files will persist across clean operations" -ForegroundColor Gray
} else {
    Write-Host "   Warning: Could not find source DTD files at $sourceDtdDir" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Directory Structure:" -ForegroundColor Yellow
Write-Host "  C:\BookloaderLocal\"
Write-Host "   Incoming\           Place ZIP files here"
Write-Host "   Logs\               Processing logs"
Write-Host "   Completed\          Success markers"
Write-Host "   Failed\             Failure markers"
Write-Host "   FinalXML\           XML output"
Write-Host "   FinalImages\         Image output"
Write-Host "   Temp\               Working directory"
Write-Host "   TransformedContent\  Post-processed XML"
Write-Host ""

Write-Host "Quick Start:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  1. Place a ZIP file in Incoming directory:" -ForegroundColor White
Write-Host "     copy ""C:\BookloaderLocal\Incoming\9781234567890.zip"""
Write-Host ""
Write-Host "  2. Process a single book:" -ForegroundColor White
Write-Host "     cd C:\RittenhouseRepos\Bookloader\LocalDevTesting"
Write-Host "     .\LoadBook_Local.bat 9781234567890"
Write-Host ""
Write-Host "  3. Process multiple books:" -ForegroundColor White
Write-Host "     .\ProcessIncomingBooks_Local.ps1 -FastMode"
Write-Host ""

Write-Host "Documentation:" -ForegroundColor Yellow
Write-Host "   LOCAL_FULL_RUN_GUIDE.md     - Complete usage guide"
Write-Host "   POST_PROCESSING_SETUP.md    - Post-processing configuration"
Write-Host "   QUICK_REF.md                - Quick reference"
Write-Host ""

# ============================================================================
# R2Utilities Post-Processing Configuration
# ============================================================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " R2Utilities Post-Processing Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$setupR2Utilities = Read-Host "Do you want to configure R2Utilities post-processing? (Y/N)"
if ([string]::IsNullOrWhiteSpace($setupR2Utilities)) { $setupR2Utilities = "N" }

if ($setupR2Utilities -eq "Y" -or $setupR2Utilities -eq "y") {
    Write-Host ""
    Write-Host "Please provide the path to R2Utilities.exe" -ForegroundColor Yellow
    Write-Host "Example: C:\RittenhouseRepos\R2Library\bin\Debug\R2Utilities.exe" -ForegroundColor Gray
    Write-Host ""
    
    $r2UtilsPath = Read-Host "Path to R2Utilities.exe"
    
    if ($r2UtilsPath -and (Test-Path $r2UtilsPath)) {
        Write-Host ""
        Write-Host "Found R2Utilities.exe at: $r2UtilsPath" -ForegroundColor Green
        Write-Host ""
        
        # Get the config file path
        $configPath = "$r2UtilsPath.config"
        
        # Update LoadBook_Local.bat
        $loadBookPath = Join-Path $PSScriptRoot "LoadBook_Local.bat"
        if (Test-Path $loadBookPath) {
            Write-Host "Updating LoadBook_Local.bat..." -ForegroundColor Yellow
            
            $content = Get-Content $loadBookPath -Raw
            
            # Update R2UTILITIES_EXE path
            $content = $content -replace 'set "R2UTILITIES_EXE=.*"', "set `"R2UTILITIES_EXE=$r2UtilsPath`""
            
            # Enable R2Utilities
            $content = $content -replace 'set "R2UTILITIES_ENABLED=false"', 'set "R2UTILITIES_ENABLED=true"'
            
            Set-Content $loadBookPath $content -NoNewline
            Write-Host "  LoadBook_Local.bat updated" -ForegroundColor Green
        } else {
            Write-Host "  LoadBook_Local.bat not found at: $loadBookPath" -ForegroundColor Yellow
        }
        
        # Update R2Utilities.exe.config
        if (Test-Path $configPath) {
            Write-Host ""
            Write-Host "Updating R2Utilities.exe.config..." -ForegroundColor Yellow
            
            try {
                [xml]$config = Get-Content $configPath
                
                # Settings to update
                $settings = @{
                    'BookLoaderSourceRootDirectory' = 'C:\BookloaderLocal\FinalXML'
                    'BookLoaderImageDestinationDirectory' = 'C:\BookloaderLocal\FinalImages'
                    'ContentLocation' = 'C:\BookloaderLocal\TransformedContent'
                    'DatabaseConnectionString' = 'Server=rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com,1433;Database=STG_RIT001;User Id=RittAdmin;Password=49jR6xQybSCDeA5ObTp0;'
                    'DefaultSpecialtyCode' = 'EMED'
                    'DefaultPracticeAreaCode' = 'PRACNURS'
                    'AutoLicensesNumberOfLicenses' = '10'
                }
                
                foreach ($key in $settings.Keys) {
                    $value = $settings[$key]
                    $setting = $config.configuration.appSettings.add | Where-Object { $_.key -eq $key }
                    if ($setting) {
                        $setting.value = $value
                        Write-Host "  Updated: $key" -ForegroundColor Gray
                    } else {
                        $newElement = $config.CreateElement('add')
                        $newElement.SetAttribute('key', $key)
                        $newElement.SetAttribute('value', $value)
                        $config.configuration.appSettings.AppendChild($newElement) | Out-Null
                        Write-Host "  Added: $key" -ForegroundColor Gray
                    }
                }
                
                $config.Save($configPath)
                Write-Host "  R2Utilities.exe.config updated" -ForegroundColor Green
            } catch {
                Write-Host "  Error updating config file: $_" -ForegroundColor Yellow
                Write-Host "  You may need to manually edit: $configPath" -ForegroundColor Yellow
            }
        } else {
            Write-Host ""
            Write-Host "  Config file not found: $configPath" -ForegroundColor Yellow
            Write-Host "  You may need to create it manually" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "  Sample configuration:" -ForegroundColor Gray
            Write-Host "  <appSettings>" -ForegroundColor Gray
            Write-Host "    <add key=`"BookLoaderSourceRootDirectory`" value=`"C:\BookloaderLocal\FinalXML`" />" -ForegroundColor Gray
            Write-Host "    <add key=`"BookLoaderImageDestinationDirectory`" value=`"C:\BookloaderLocal\FinalImages`" />" -ForegroundColor Gray
            Write-Host "    <add key=`"ContentLocation`" value=`"C:\BookloaderLocal\TransformedContent`" />" -ForegroundColor Gray
            Write-Host "    <add key=`"DatabaseConnectionString`" value=`"Server=rittenhousedb...; Database=STG_RIT001;...`" />" -ForegroundColor Gray
            Write-Host "  </appSettings>" -ForegroundColor Gray
        }
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host " R2Utilities Configuration Complete!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "R2Utilities.exe: $r2UtilsPath" -ForegroundColor White
        Write-Host "Config file:     $configPath" -ForegroundColor White
        Write-Host "Status:          ENABLED in LoadBook_Local.bat" -ForegroundColor White
        Write-Host ""
        
    } elseif ($r2UtilsPath) {
        Write-Host ""
        Write-Host "File not found: $r2UtilsPath" -ForegroundColor Yellow
        Write-Host "Post-processing will remain disabled." -ForegroundColor Yellow
        Write-Host "You can configure it later using POST_PROCESSING_SETUP.md" -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "Skipping R2Utilities configuration." -ForegroundColor Gray
        Write-Host ""
    }
} else {
    Write-Host ""
    Write-Host "Skipping R2Utilities configuration." -ForegroundColor Gray
    Write-Host "You can set it up later using POST_PROCESSING_SETUP.md" -ForegroundColor Gray
    Write-Host ""
}

Write-Host ""
Read-Host "Press Enter to exit" | Out-Null
