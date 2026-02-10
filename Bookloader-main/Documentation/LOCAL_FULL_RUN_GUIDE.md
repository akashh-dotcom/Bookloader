# 🚀 Local Full-Run Book Processing Guide

## Overview

This guide explains how to run the complete book processing pipeline locally, simulating the staging environment. This includes:
- ✅ Java book loader (EPUB parsing, XML processing, database insertion)
- ✅ Content extraction and copying
- ✅ Optional post-processing (C# services)
- ✅ Full database integration

## 🎯 Quick Start

### 1. Setup (One-Time)

Run the setup script to create directory structure:

```powershell
cd C:\RittenhouseRepos\Bookloader\LocalDevTesting
.\Setup-LocalEnvironment.ps1
```

Or create directories manually:
```powershell
New-Item -ItemType Directory -Path "C:\BookloaderLocal\Incoming" -Force
New-Item -ItemType Directory -Path "C:\BookloaderLocal\Logs" -Force
New-Item -ItemType Directory -Path "C:\BookloaderLocal\Completed" -Force
New-Item -ItemType Directory -Path "C:\BookloaderLocal\Failed" -Force
New-Item -ItemType Directory -Path "C:\BookloaderLocal\FinalXML" -Force
New-Item -ItemType Directory -Path "C:\BookloaderLocal\FinalImages" -Force
```

### 2. Configure Database Connection

Edit `C:\RittenhouseRepos\Bookloader\RISBackend.cfg`:

```properties
# Ensure TEST_MODE is false for full processing
RIS.TEST_MODE=false

# Database connection (staging database)
RISDB.URL=jdbc:sqlserver://rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com,1433;databaseName=STG_RIT001
RISDB.UserID=RittAdmin
RISDB.Password=49jR6xQybSCDeA5ObTp0

# Content paths (used by full run)
RIS.CONTENT_IN=./test/input
RIS.CONTENT_OUT=./test/output  
RIS.CONTENT_TEMP=./test/temp
RIS.CONTENT_MEDIA=./test/media

# Output location (where final XML goes)
RIS.DEST_NON_TEXTML_CONTENT_PATH=./test/finalOutput

# TextML (can skip for local testing)
RIS.SKIP_TEXTML=true
RIS.LOAD_CONTENT_TO_NON_TEXTML_PATH=true
```

### 3. Process a Single Book

```batch
cd C:\RittenhouseRepos\Bookloader\LocalDevTesting

# Place your ZIP file in incoming directory
copy "path\to\9781234567890.zip" "C:\BookloaderLocal\Incoming\"

# Run processing (fast mode - recommended for testing)
.\LoadBook_Local.bat 9781234567890

# Or with update mode (if book already exists in DB)
.\LoadBook_Local.bat 9781234567890 -update
```

### 4. Process Multiple Books

```powershell
cd C:\RittenhouseRepos\Bookloader\LocalDevTesting

# Fast mode (skips linking - 2-5 min per book)
.\ProcessIncomingBooks_Local.ps1 -FastMode

# Normal mode (full processing - 30-60 min per book)
.\ProcessIncomingBooks_Local.ps1

# With update mode
.\ProcessIncomingBooks_Local.ps1 -Update -FastMode
```

## 📁 Directory Structure

```
C:\BookloaderLocal\
├── Incoming\                    # 👈 Place ZIP files here
│   ├── 9781234567890.zip
│   └── 9781234567891.zip
│
├── FinalXML\                    # Java loader outputs XML here
│   └── 9781234567890\
│       ├── xml\                 # Processed XML files
│       │   ├── toc.xml
│       │   ├── ch001.xml
│       │   └── ...
│       └── images\              # Extracted images
│           ├── fig001.jpg
│           └── ...
│
├── FinalImages\                 # Images copied by post-processing
│   └── 9781234567890\
│
├── TransformedContent\          # Transformed XML (if R2Utilities runs)
│   └── 9781234567890\
│
├── Completed\                   # Success tracking
│   └── 9781234567890.txt        # Marker file for completed books
│
├── Failed\                      # Failure tracking
│   └── 9781234567891.txt        # Marker file for failed books
│
└── Logs\                        # Processing logs
    ├── 9781234567890_SUCCESS.log
    └── 9781234567891_FAIL.log
```

## 🔄 Processing Workflow

### Phase 1: Pre-Flight Validation
- ✅ Checks if book already exists in database
- ✅ Validates ZIP file is present
- ✅ Creates required directories

### Phase 2: File Staging
- 📦 Extracts ZIP file to temp directory
- 📁 Copies content to staging area (`test/input`)
- 🧹 Cleans up temp directory

### Phase 3: Java Book Loader
- 📖 Parses EPUB structure
- 🔍 Validates XML against DTD
- 🏷️ Applies content tagging rules
- 🔗 Performs drug/disease linking (if enabled)
- 💾 Saves to database
- 📤 Outputs to `test/finalOutput/<ISBN>`

### Phase 4: Content Organization
- 📂 Copies XML to `C:\BookloaderLocal\FinalXML`
- 🖼️ Copies images to `C:\BookloaderLocal\FinalImages`
- ✅ Verifies output files exist

### Phase 5: Post-Processing (Optional)
- 🔄 Transforms XML content
- 📊 Updates resource metadata
- 🔍 Populates A-to-Z index
- ✅ Sets resource as active
- 🎫 Creates licenses

## 🎮 Usage Examples

### Example 1: Quick Test (Fast Mode)
```batch
# Test a single book quickly (2-5 minutes)
cd C:\RittenhouseRepos\Bookloader\LocalDevTesting
.\LoadBook_Local.bat 9781234567890
```

### Example 2: Full Processing (Normal Mode)
Edit `RISBackend.cfg` to use `ris_rules_normal.xml`, then:
```batch
.\LoadBook_Local.bat 9781234567890
```

### Example 3: Update Existing Book
```batch
.\LoadBook_Local.bat 9781234567890 -update
```

### Example 4: Batch Processing
```powershell
# Place multiple ZIPs in C:\BookloaderLocal\Incoming\
# Then run:
.\ProcessIncomingBooks_Local.ps1 -FastMode -PauseAtEnd
```

### Example 5: With Debugging
```batch
# Use VS Code debugging for step-through:
# 1. Set breakpoints in com.rittenhouse.RIS.Main
# 2. F5 → Choose "Debug Bookloader (Normal Mode)"
# 3. Script will use test/input for content
```

## ⚙️ Configuration Options

### Fast vs Normal Mode

**Fast Mode** (Recommended for testing):
- Processing time: 2-5 minutes
- Skips: Drug/disease linking, PMID lookup
- Configure: Use `--skipLinks` flag or rules with linking disabled

**Normal Mode** (Production-like):
- Processing time: 30-60 minutes
- Includes: Full drug/disease linking, all rules
- Configure: Use `ris_rules_normal.xml` or no `--skipLinks` flag

### Update vs New Resource

**New Resource** (Default):
- Fails if ISBN already exists in database
- Creates new resource record
- All content is new

**Update Mode** (`-update` flag):
- Updates existing resource if found
- Overwrites previous content
- Preserves resource ID

### Post-Processing Options

See [POST_PROCESSING_SETUP.md](POST_PROCESSING_SETUP.md) for detailed configuration.

**Option A: Disabled** (LoadBook_Local.bat default)
```batch
set "R2UTILITIES_ENABLED=false"
```
- Java processing only
- No transformation or additional DB updates
- Good for: Testing Java loader

**Option B: Enabled** (Requires R2Library setup)
```batch
set "R2UTILITIES_ENABLED=true"
set "R2UTILITIES_EXE=C:\RittenhouseRepos\R2Library\bin\Debug\R2Utilities.exe"
```
- Full post-processing pipeline
- XML transformation
- License creation
- Good for: Production simulation

## 🔍 Monitoring & Logs

### Processing Logs
```
C:\BookloaderLocal\Logs\<ISBN>_SUCCESS.log
C:\BookloaderLocal\Logs\<ISBN>_FAIL.log
```

### Java Backend Logs
```
C:\RittenhouseRepos\Bookloader\logs\RISBackend.log.*
```

### Database Verification
```sql
-- Check if resource was created
SELECT * FROM tResource WHERE vchisbn13 = '9781234567890';

-- Check resource status
SELECT intResourceID, vchTitle, intStatus, dtCreated 
FROM tResource 
WHERE vchisbn13 = '9781234567890';

-- Check content records
SELECT COUNT(*) 
FROM tBookContent 
WHERE intResourceID = (SELECT intResourceID FROM tResource WHERE vchisbn13 = '9781234567890');
```

### Output Verification
```powershell
# Check XML output
Get-ChildItem "C:\BookloaderLocal\FinalXML\9781234567890\xml"

# Check images
Get-ChildItem "C:\BookloaderLocal\FinalImages\9781234567890"

# View processing log
Get-Content "C:\BookloaderLocal\Logs\9781234567890_SUCCESS.log"
```

## 🐛 Troubleshooting

### Problem: ZIP file not found
```
ERROR: ZIP file not found: C:\BookloaderLocal\Incoming\9781234567890.zip
```
**Solution**: Verify ZIP file exists and filename matches ISBN exactly.

### Problem: Database connection failed
```
ERROR: Unable to connect to database
```
**Solution**: 
1. Check `RISBackend.cfg` database settings
2. Test connection: `sqlcmd -S rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com,1433 -U RittAdmin -P <password>`
3. Verify VPN/network access

### Problem: Resource already exists
```
ERROR: Resource already exists in database
```
**Solution**: Use `-update` flag: `.\LoadBook_Local.bat 9781234567890 -update`

### Problem: Processing timeout
```
ERROR: Process exceeded 240 minute limit
```
**Solution**: 
- For fast mode, this shouldn't happen (2-5 min typical)
- For normal mode with linking, this is possible for large books
- Increase timeout: `.\ProcessIncomingBooks_Local.ps1 -BatchTimeoutMinutes 480`

### Problem: Java heap space error
```
java.lang.OutOfMemoryError: Java heap space
```
**Solution**: Edit `LoadBook_Local.bat` to increase heap:
```batch
"%JAVA_EXE%" -Xms2g -Xmx4g ...
```

### Problem: Missing XML output
```
WARNING: Expected XML folder missing
```
**Solution**: 
1. Check Java logs for errors: `logs\RISBackend.log.*`
2. Verify EPUB structure is valid
3. Check rules configuration

### Problem: 7-Zip not found
```
WARNING: 7-Zip not found
```
**Solution**: Script will fall back to PowerShell extraction, or install 7-Zip from https://www.7-zip.org/

## 📊 Performance Benchmarks

| Mode | Time per Book | Drug/Disease Linking | Database Writes | Use Case |
|------|---------------|---------------------|-----------------|----------|
| Fast Mode | 2-5 min | ❌ | ✅ | Development, testing |
| Normal Mode | 30-60 min | ✅ | ✅ | Production simulation |
| Fast + No DB | 1-3 min | ❌ | ❌ | EPUB validation only |

## 🎯 Common Workflows

### Workflow 1: Quick Development Test
```powershell
# Test Java loader only, no post-processing
cd LocalDevTesting
copy "test_medical_book.epub" "C:\BookloaderLocal\Incoming\9781234567890.zip"
.\LoadBook_Local.bat 9781234567890
# Review output in C:\BookloaderLocal\FinalXML\9781234567890\
```

### Workflow 2: Full Pipeline Test
```powershell
# Enable post-processing in LoadBook_Local.bat
# Set R2UTILITIES_ENABLED=true
cd LocalDevTesting
.\ProcessIncomingBooks_Local.ps1 -FastMode
# Verify database records and transformed content
```

### Workflow 3: Production Simulation
```powershell
# Use normal mode with linking
# Edit RISBackend.cfg: use ris_rules_normal.xml
cd LocalDevTesting
.\ProcessIncomingBooks_Local.ps1
# Takes 30-60 min per book
```

### Workflow 4: Debugging Session
```
1. Open VS Code
2. Set breakpoints in Main.java (e.g., line 279, 556, 883)
3. Place EPUB in test/input/
4. F5 → Choose debug configuration
5. Step through code as it processes
```

## 🔗 Related Documentation

- **[POST_PROCESSING_SETUP.md](POST_PROCESSING_SETUP.md)** - Configure C# post-processing
- **[QUICK_REF.md](QUICK_REF.md)** - Quick reference for flags and modes
- **[LOCAL_DEBUG_GUIDE.md](LOCAL_DEBUG_GUIDE.md)** - VS Code debugging guide
- **[FLAG_REFERENCE.md](FLAG_REFERENCE.md)** - Command-line flag reference

## 🆚 Local vs Staging Comparison

| Aspect | Staging (Production) | Local (This Setup) |
|--------|---------------------|-------------------|
| **Directory** | `E:\R2BookLoader\TestUploadPending\` | `C:\BookloaderLocal\Incoming\` |
| **Process** | Scheduled task runs PowerShell script | Manual script execution |
| **Output** | `E:\R2v2-XMLbyISBN\` | `C:\BookloaderLocal\FinalXML\` |
| **Database** | `STG_RIT001` (same) | `STG_RIT001` (same) |
| **Post-Processing** | `E:\R2BookLoader\App\R2Utilities.exe` | Optional local R2Utilities.exe |
| **Monitoring** | `E:\R2BookLoader\Logs\` | `C:\BookloaderLocal\Logs\` |

## ✅ Verification Checklist

After processing a book, verify:

- [ ] Success log created: `C:\BookloaderLocal\Logs\<ISBN>_SUCCESS.log`
- [ ] Completion marker: `C:\BookloaderLocal\Completed\<ISBN>.txt`
- [ ] XML output exists: `C:\BookloaderLocal\FinalXML\<ISBN>\xml\*.xml`
- [ ] Images exist (if applicable): `C:\BookloaderLocal\FinalImages\<ISBN>\*.jpg`
- [ ] Database record created: `SELECT * FROM tResource WHERE vchisbn13 = '<ISBN>'`
- [ ] Content records exist: `SELECT COUNT(*) FROM tBookContent WHERE intResourceID = ...`
- [ ] Resource status is Active (if post-processing ran): `intStatus = 1`

## 🚀 Next Steps

1. **Run Setup**: Execute `Setup-LocalEnvironment.ps1` (see below)
2. **Test Single Book**: Use `LoadBook_Local.bat` with a test EPUB
3. **Verify Output**: Check database and output directories
4. **Configure Post-Processing**: Follow [POST_PROCESSING_SETUP.md](POST_PROCESSING_SETUP.md)
5. **Batch Processing**: Use `ProcessIncomingBooks_Local.ps1` for multiple books

## 📝 Setup Script

Save this as `Setup-LocalEnvironment.ps1`:

```powershell
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

foreach ($dir in $dirs) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "✓ Created: $dir" -ForegroundColor Green
    } else {
        Write-Host "  Exists: $dir" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "✅ Local environment setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Place ZIP files in: C:\BookloaderLocal\Incoming\"
Write-Host "  2. Run: .\LoadBook_Local.bat <ISBN>"
Write-Host "  3. Or: .\ProcessIncomingBooks_Local.ps1 -FastMode"
```

---

**Happy Processing!** 🎉

For questions or issues, check the troubleshooting section above or review the Java logs in `C:\RittenhouseRepos\Bookloader\logs\`.
