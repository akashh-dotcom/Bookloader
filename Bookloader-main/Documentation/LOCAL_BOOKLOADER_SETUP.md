# Local BookLoader Development Setup

## Overview

This configuration runs the complete BookLoader workflow locally on your development PC.

## Directory Structure

```
C:\RittenhouseRepos\Bookloader\test\
├── input\              (Place EPUB ZIP files here - extracted by RIS Backend)
├── temp\               (RIS Backend working directory - temporary processing)
├── finalOutput\        (FINAL OUTPUT - Processed XML organized by ISBN)
│   └── {ISBN}\
│       └── xml\
│           ├── book.{ISBN}.xml
│           ├── chapter.{ISBN}.001.xml
│           └── toc.xml
└── media\              (Extracted images - if needed)
```

## Configuration Files

### RISBackend.cfg (Java Layer)

**Already Configured** ✅

```properties
# Test mode enabled (no database writes, no TextML)
RIS.TEST_MODE=true
RIS.TEST_PRESERVE_FILES=true

# Input/Output paths
RIS.CONTENT_IN=./test/input
RIS.CONTENT_TEMP=./test/temp
RIS.TEST_DEST_NON_TEXTML_CONTENT_PATH=./test/finalOutput

# Database (local SQL Server)
RISDB.URL=jdbc:sqlserver://127.0.0.1:11433;databaseName=STG_RIT001
```

### R2Utilities Configuration (C# Layer)

**File**: `C:\RittenhouseRepos\R2Library\src\R2Utilities\bin\Debug\R2Utilities.exe.config`

```xml
<appSettings>
    <!-- Point to YOUR finalOutput folder -->
    <add key="BookLoaderSourceRootDirectory" value="C:\RittenhouseRepos\Bookloader\test\finalOutput" />
    
    <!-- Where R2 will serve content from -->
    <add key="ContentLocation" value="C:\R2Content" />
    <add key="BookLoaderImageDestinationDirectory" value="C:\R2Images" />
    
    <!-- Database connection -->
    <add key="ConnectionString" value="Server=127.0.0.1,11433;Database=STG_RIT001;User Id=RittAdmin;Password=49jR6xQybSCDeA5ObTp0;" />
    
    <!-- Defaults -->
    <add key="DefaultSpecialtyCode" value="00" />
    <add key="DefaultPracticeAreaCode" value="00" />
    <add key="AutoLicensesNumberOfLicenses" value="0" />
</appSettings>
```

## Workflow Steps

### Step 1: Place EPUB in Input

```powershell
# Example: Copy your EPUB ZIP
Copy-Item "C:\Books\9781234567890.zip" "C:\RittenhouseRepos\Bookloader\test\input\"
```

### Step 2: Run RIS Backend (Java)

```batch
cd C:\RittenhouseRepos\Bookloader
ant compile
java -cp "bin;lib\*" com.rittenhouse.RIS.Main
```

**What happens**:
- Extracts ZIP from `test\input\`
- Processes content in `test\temp\`
- **Outputs final XML to `test\finalOutput\{ISBN}\xml\`** ✅
- In TEST_MODE: No database writes, no TextML upload

**Verify Output**:
```powershell
Get-ChildItem "C:\RittenhouseRepos\Bookloader\test\finalOutput\*\xml" -Recurse | Select Name
```

### Step 3: Run R2Utilities Post-Processing (C#)

**Prerequisites**:
- RIS Backend completed successfully
- Database record exists in `tResource` table
- If TEST_MODE was used, you'll need to manually insert database record first

```powershell
cd "C:\RittenhouseRepos\R2Library\src\R2Utilities\bin\Debug"

# Run post-processing
.\R2Utilities.exe -BookLoaderPostProcessingTask -isbn=9781234567890 -includeChapterNumbersInToc=true
```

**What happens**:
- Reads from `test\finalOutput\{ISBN}\xml\`
- Copies to `C:\R2Content\{ISBN}\`
- Updates database (sort title, A-Z index)
- Sets resource to Active
- Creates auto-licenses (if configured)

## Important Notes

### TEST_MODE Limitations

When `RIS.TEST_MODE=true`:
- ❌ **NO database writes** (tResource, tResourceISBN, etc.)
- ❌ **NO TextML uploads**
- ✅ XML processing works
- ✅ File output works

**For R2Utilities to work**, you need database records. Options:

**Option A: Disable TEST_MODE** (recommended for complete testing)
```properties
RIS.TEST_MODE=false
```

**Option B: Manually insert database records**
```sql
-- Minimum required for R2Utilities
INSERT INTO tResource (vchisbn13, vchTitle, ...) VALUES ('9781234567890', 'Test Book', ...)
```

### Clean Output Between Runs

```powershell
# Clear finalOutput folder
Remove-Item "C:\RittenhouseRepos\Bookloader\test\finalOutput\*" -Recurse -Force

# Clear temp folder
Remove-Item "C:\RittenhouseRepos\Bookloader\test\temp\*" -Recurse -Force
```

## Quick Test Script

Save as `C:\RittenhouseRepos\Bookloader\test\TestLocalBookLoader.ps1`:

```powershell
param(
    [Parameter(Mandatory=$true)]
    [string]$ISBN,
    
    [string]$ZipPath
)

$ErrorActionPreference = "Stop"

# Paths
$repoRoot = "C:\RittenhouseRepos\Bookloader"
$inputDir = "$repoRoot\test\input"
$finalOutputDir = "$repoRoot\test\finalOutput"
$r2UtilitiesExe = "C:\RittenhouseRepos\R2Library\src\R2Utilities\bin\Debug\R2Utilities.exe"

Write-Host "=== Local BookLoader Test ===" -ForegroundColor Cyan
Write-Host "ISBN: $ISBN"

# Step 1: Copy ZIP if provided
if ($ZipPath -and (Test-Path $ZipPath)) {
    Write-Host "`n[1/3] Copying ZIP to input..." -ForegroundColor Yellow
    Copy-Item $ZipPath "$inputDir\$ISBN.zip" -Force
    Write-Host "  ✓ Copied: $ZipPath" -ForegroundColor Green
}

# Step 2: Run RIS Backend
Write-Host "`n[2/3] Running RIS Backend (Java)..." -ForegroundColor Yellow
Push-Location $repoRoot
try {
    # Compile if needed
    ant compile 2>&1 | Out-Null
    
    # Run Java
    $javaCmd = "java -cp `"bin;lib\*`" com.rittenhouse.RIS.Main"
    Write-Host "  Running: $javaCmd" -ForegroundColor Gray
    Invoke-Expression $javaCmd
    
    if ($LASTEXITCODE -ne 0) {
        throw "RIS Backend failed with exit code: $LASTEXITCODE"
    }
    
    Write-Host "  ✓ RIS Backend completed" -ForegroundColor Green
} finally {
    Pop-Location
}

# Verify output
$xmlPath = "$finalOutputDir\$ISBN\xml"
if (!(Test-Path "$xmlPath\*.xml")) {
    throw "No XML files found in: $xmlPath"
}

Write-Host "  ✓ XML files created: $xmlPath" -ForegroundColor Green
Get-ChildItem "$xmlPath\*.xml" | ForEach-Object { Write-Host "    - $($_.Name)" -ForegroundColor Gray }

# Step 3: Run R2Utilities
Write-Host "`n[3/3] Running R2Utilities Post-Processing..." -ForegroundColor Yellow
if (Test-Path $r2UtilitiesExe) {
    & $r2UtilitiesExe -BookLoaderPostProcessingTask -isbn=$ISBN -includeChapterNumbersInToc=true
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ Post-processing completed" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Post-processing failed (exit code: $LASTEXITCODE)" -ForegroundColor Red
    }
} else {
    Write-Host "  ⚠ R2Utilities not found at: $r2UtilitiesExe" -ForegroundColor Yellow
    Write-Host "  Run manually: $r2UtilitiesExe -BookLoaderPostProcessingTask -isbn=$ISBN" -ForegroundColor Gray
}

Write-Host "`n=== Complete ===" -ForegroundColor Cyan
Write-Host "Final output: $finalOutputDir\$ISBN\" -ForegroundColor Green
```

**Usage**:
```powershell
# With ZIP file
.\test\TestLocalBookLoader.ps1 -ISBN "9781234567890" -ZipPath "C:\Books\mybook.zip"

# Just processing (ZIP already in input folder)
.\test\TestLocalBookLoader.ps1 -ISBN "9781234567890"
```

## Troubleshooting

### Issue: finalOutput folder empty after RIS Backend

**Check**:
```powershell
# Verify config setting
Select-String -Path "RISBackend.cfg" -Pattern "TEST_DEST_NON_TEXTML_CONTENT_PATH"

# Should show: RIS.TEST_DEST_NON_TEXTML_CONTENT_PATH=./test/finalOutput
```

**Fix**: Ensure path is relative to `RISBackend.cfg` location

### Issue: R2Utilities can't find resource

**Cause**: TEST_MODE doesn't write to database

**Solution**: Either disable TEST_MODE or manually insert resource record

### Issue: Files don't copy from finalOutput

**Check**: R2Utilities config points to correct source
```xml
<add key="BookLoaderSourceRootDirectory" value="C:\RittenhouseRepos\Bookloader\test\finalOutput" />
```

## Directory Permissions

Ensure these directories exist and are writable:
```powershell
$dirs = @(
    "C:\RittenhouseRepos\Bookloader\test\input",
    "C:\RittenhouseRepos\Bookloader\test\temp",
    "C:\RittenhouseRepos\Bookloader\test\finalOutput",
    "C:\R2Content",
    "C:\R2Images"
)

foreach ($dir in $dirs) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "Created: $dir" -ForegroundColor Green
    }
}
```

## Summary

✅ **Configuration Complete**
- RIS Backend outputs to: `test\finalOutput\`
- R2Utilities reads from: `test\finalOutput\`
- Clean separation of input, processing, and final output
- Ready for local development and testing

**Next Steps**:
1. Place EPUB ZIP in `test\input\`
2. Run RIS Backend (Java)
3. Verify XML in `test\finalOutput\{ISBN}\xml\`
4. Run R2Utilities post-processing
5. Check final content in `C:\R2Content\`
