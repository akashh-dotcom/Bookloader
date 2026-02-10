# R2Utilities Post-Processing Setup Guide

## Overview

The BookLoader post-processing is handled by `R2Utilities.exe`, a C# application that performs:
- Resource data updates (sort titles, alpha keys, ISBN normalization)
- Specialty and practice area assignments
- XML transformation and content copying
- A-to-Z index term population
- Table of contents generation
- License creation

## Integration Options

### Option 1: Call R2Utilities Directly (Recommended if Available)

If you have the R2Library repo built locally with R2Utilities.exe:

1. **Build R2Utilities** (in R2Library repo):
   ```powershell
   cd C:\RittenhouseRepos\R2Library
   # Build the R2Utilities project
   msbuild R2Utilities\R2Utilities.csproj /p:Configuration=Debug
   ```

2. **Configure LoadBook_Local.bat**:
   ```bat
   REM In LoadBook_Local.bat, set these variables:
   set "R2UTILITIES_EXE=C:\RittenhouseRepos\R2Library\src\R2Utilities\bin\Debug\net481\R2Utilities.exe"
   set "R2UTILITIES_ENABLED=true"
   ```

3. **Configure R2Utilities App Settings**:
   Edit `C:\RittenhouseRepos\R2Library\bin\Debug\R2Utilities.exe.config`:
   ```xml
   <appSettings>
     <!-- Local directories where BookLoader outputs content -->
     <add key="BookLoaderSourceRootDirectory" value="C:\BookloaderLocal\FinalXML" />
     <add key="BookLoaderImageDestinationDirectory" value="C:\BookloaderLocal\FinalImages" />
     
     <!-- Where transformed content should go -->
     <add key="ContentLocation" value="C:\BookloaderLocal\TransformedContent" />
     
     <!-- Database connection -->
     <add key="DatabaseConnectionString" value="Server=rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com,1433;Database=STG_RIT001;User Id=RittAdmin;Password=49jR6xQybSCDeA5ObTp0;" />
     
     <!-- Default assignments -->
     <add key="DefaultSpecialtyCode" value="EMED" />
     <add key="DefaultPracticeAreaCode" value="PRACNURS" />
     <add key="AutoLicensesNumberOfLicenses" value="10" />
   </appSettings>
   ```

### Option 2: Mock Post-Processing (For Testing Java Only)

If you don't have R2Utilities set up yet, you can test the Java portion and manually perform post-processing tasks:

1. **Leave R2UTILITIES_ENABLED=false in LoadBook_Local.bat**

2. **Manually verify output**:
   ```powershell
   # Check Java output
   ls C:\BookloaderLocal\FinalXML\<ISBN>\xml
   ls C:\BookloaderLocal\FinalXML\<ISBN>\images
   ```

3. **Manually perform database updates** (if needed):
   ```sql
   -- Example: Set resource as active
   UPDATE tResource 
   SET intStatus = 1, vchModifiedBy = 'ManualTest'
   WHERE vchisbn13 = '<ISBN>';
   
   -- Add specialty
   INSERT INTO tResourceSpecialty (intResourceID, intSpecialtyID, vchCreatedBy)
   VALUES (<ResourceID>, 1, 'ManualTest');
   
   -- Add practice area
   INSERT INTO tResourcePracticeArea (intResourceID, intPracticeAreaID, vchCreatedBy)
   VALUES (<ResourceID>, 1, 'ManualTest');
   ```

### Option 3: Create Minimal Post-Processing Script

Create a PowerShell script that does the essential tasks:

```powershell
# PostProcess_Minimal.ps1
param([string]$ISBN)

$xmlSource = "C:\BookloaderLocal\FinalXML\$ISBN\xml"
$imgSource = "C:\BookloaderLocal\FinalXML\$ISBN\images"
$xmlDest = "C:\BookloaderLocal\TransformedContent\$ISBN"
$imgDest = "C:\BookloaderLocal\FinalImages\$ISBN"

# Copy XML
if (Test-Path $xmlSource) {
    New-Item -ItemType Directory -Path $xmlDest -Force | Out-Null
    Copy-Item "$xmlSource\*" $xmlDest -Recurse -Force
    Write-Host "✓ XML copied to $xmlDest"
}

# Copy Images
if (Test-Path $imgSource) {
    New-Item -ItemType Directory -Path $imgDest -Force | Out-Null
    Copy-Item "$imgSource\*" $imgDest -Recurse -Force
    Write-Host "✓ Images copied to $imgDest"
}

# Update database (example - adjust to your needs)
$connString = "Server=rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com,1433;Database=STG_RIT001;User Id=RittAdmin;Password=49jR6xQybSCDeA5ObTp0;"
$conn = New-Object System.Data.SqlClient.SqlConnection $connString
$conn.Open()

try {
    # Get resource ID
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = "SELECT intResourceID FROM tResource WHERE vchisbn13 = @isbn"
    $cmd.Parameters.AddWithValue("@isbn", $ISBN) | Out-Null
    $resourceId = $cmd.ExecuteScalar()
    
    if ($resourceId) {
        Write-Host "✓ Found resource ID: $resourceId"
        
        # Set as active
        $cmd = $conn.CreateCommand()
        $cmd.CommandText = "UPDATE tResource SET intStatus = 1, vchModifiedBy = 'LocalTest' WHERE intResourceID = @id"
        $cmd.Parameters.AddWithValue("@id", $resourceId) | Out-Null
        $cmd.ExecuteNonQuery() | Out-Null
        Write-Host "✓ Set resource as active"
    } else {
        Write-Host "⚠ Resource not found in database"
    }
}
finally {
    $conn.Close()
}

Write-Host "✓ Post-processing completed"
```

Then call it from LoadBook_Local.bat:
```bat
powershell -ExecutionPolicy Bypass -File "%~dp0PostProcess_Minimal.ps1" -ISBN %ISBN%
```

## Post-Processing Tasks Breakdown

The full `BookLoaderPostProcessingTask.cs` performs these operations:

### 1. Update Resource Data
- Calculates sort title (moves "A ", "AN ", "THE " to end)
- Extracts alpha character for indexing
- Updates ISBN fields (ISBN10, ISBN13, eISBN)

### 2. Copy Content
- Copies XML from Java output to final content location
- Copies images to final image location
- Creates directory structure as needed

### 3. Update TOC XML (Optional)
- Adds chapter numbers to table of contents
- Only if `-includeChapterNumbersInToc=true`

### 4. Transform XML Content
- Applies XSLT transformations
- Converts to final display format
- Handles special elements and formatting

### 5. Set Resource Status
- Updates resource to "Active" status in database

### 6. Create Licenses
- Adds auto-licenses for configured institutions
- Creates license records for resource access

### 7. Update A-to-Z Index
- Populates search index with:
  - Drug names and synonyms
  - Disease names and synonyms
  - Keywords from content

## Directory Structure Reference

```
C:\BookloaderLocal\
├── Incoming\              # Place ZIP files here
│   └── 9781234567890.zip
├── FinalXML\              # Java loader outputs here
│   └── 9781234567890\
│       ├── xml\           # Processed XML files
│       └── images\        # Extracted images
├── FinalImages\           # Images copied here by post-processing
│   └── 9781234567890\
├── TransformedContent\    # Transformed XML after post-processing
│   └── 9781234567890\
├── Completed\             # Success markers
│   └── 9781234567890.txt
├── Failed\                # Failure markers
│   └── 9781234567890.txt
└── Logs\                  # Processing logs
    ├── 9781234567890_SUCCESS.log
    └── 9781234567890_FAIL.log
```

## Testing Post-Processing

### Test Without R2Utilities
1. Run LoadBook_Local.bat with R2UTILITIES_ENABLED=false
2. Verify Java output in C:\BookloaderLocal\FinalXML\<ISBN>
3. Check database for resource record
4. Manually test database queries if needed

### Test With R2Utilities
1. Build and configure R2Utilities.exe
2. Set R2UTILITIES_ENABLED=true in LoadBook_Local.bat
3. Run full pipeline
4. Verify output in:
   - C:\BookloaderLocal\TransformedContent\<ISBN>
   - C:\BookloaderLocal\FinalImages\<ISBN>
5. Check database for:
   - Resource status = Active
   - Specialty assignments
   - Practice area assignments
   - A-to-Z index entries

## Troubleshooting

### R2Utilities Not Found
- Verify path in LoadBook_Local.bat
- Build R2Utilities project in R2Library repo
- Check that all dependencies are present

### Configuration Errors
- Check R2Utilities.exe.config for correct paths
- Verify database connection string
- Ensure all required appSettings are present

### Database Connection Issues
- Test connection using SQL Server Management Studio
- Verify credentials in configuration
- Check firewall settings for database access

### Missing Output
- Check Java logs: C:\RittenhouseRepos\Bookloader\logs\RISBackend.log.*
- Verify Java loader completed successfully
- Check paths in RISBackend.cfg

### Content Not Transformed
- Check R2Utilities logs (if logging enabled)
- Verify XSLT files are present
- Check file permissions on output directories

## Next Steps

1. **Without R2Utilities**: Use Option 2 or 3 to get basic post-processing
2. **With R2Library Access**: Use Option 1 for full post-processing pipeline
3. **Custom Requirements**: Modify PostProcess_Minimal.ps1 to match your needs

## Related Files

- `LoadBook_Local.bat` - Main processing script
- `ProcessIncomingBooks_Local.ps1` - Batch processing wrapper
- `BookLoaderPostProcessingTask.cs` - Full post-processing implementation (in R2Library)
- `RISBackend.cfg` - Java loader configuration
