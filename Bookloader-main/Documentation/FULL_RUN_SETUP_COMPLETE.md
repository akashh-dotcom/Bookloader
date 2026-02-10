# 🎉 Local Full-Run Setup Complete!

## What's Been Created

Your local bookloader environment now supports **full production-like processing** with database connectivity!

### ✅ New Files Created

1. **LoadBook_Local.bat** - Main processing script (mimics staging LoadBook.bat)
2. **ProcessIncomingBooks_Local.ps1** - Batch processing wrapper
3. **Setup-LocalEnvironment.ps1** - One-time directory setup
4. **Test-LocalSetup.bat** - Configuration verification
5. **LOCAL_FULL_RUN_GUIDE.md** - Complete usage documentation
6. **POST_PROCESSING_SETUP.md** - Post-processing configuration guide
7. **README.md** - Updated with full-run instructions

## 🚀 How to Run This Locally

### Step 1: One-Time Setup

```powershell
cd C:\RittenhouseRepos\Bookloader\LocalDevTesting
.\Setup-LocalEnvironment.ps1
```

This creates:
```
C:\BookloaderLocal\
├── Incoming\          # Place ZIP files here
├── Logs\              # Processing logs
├── Completed\         # Success markers
├── Failed\            # Failure markers
├── FinalXML\          # XML output
├── FinalImages\       # Image output
├── Temp\              # Working directory
└── TransformedContent\ # Post-processed XML
```

### Step 2: Verify Configuration

```batch
.\Test-LocalSetup.bat
```

This checks:
- ✅ Java installation
- ✅ Directory structure
- ✅ RISBackend.cfg configuration
- ✅ Database connectivity

### Step 3: Process a Book

**Option A: Single Book**
```batch
# Place your ZIP file
copy "test_medical_book.epub" "C:\BookloaderLocal\Incoming\9781234567890.zip"

# Run processing
.\LoadBook_Local.bat 9781234567890
```

**Option B: Batch Processing**
```powershell
# Place multiple ZIPs in C:\BookloaderLocal\Incoming\

# Process all books (fast mode - 2-5 min each)
.\ProcessIncomingBooks_Local.ps1 -FastMode

# Or normal mode (30-60 min each)
.\ProcessIncomingBooks_Local.ps1
```

## 🔄 How It Works (Just Like Staging!)

### On Staging
```
ProcessIncomingZipsNew.ps1
  ↓ calls
LoadBook.bat
  ↓ extracts ZIP
  ↓ runs Java loader
  ↓ calls
R2Utilities.exe -BookLoaderPostProcessingTask
  ↓ transforms XML
  ↓ updates database
  ✅ Complete!
```

### On Your Local PC
```
ProcessIncomingBooks_Local.ps1
  ↓ calls
LoadBook_Local.bat
  ↓ extracts ZIP to C:\BookloaderLocal\Incoming\
  ↓ runs Java loader (connects to staging DB)
  ↓ outputs to C:\BookloaderLocal\FinalXML\
  ↓ optionally calls R2Utilities (if configured)
  ✅ Complete!
```

## 📊 Processing Phases

### Phase 1: Pre-Flight Validation
- Checks if book already exists in database
- Validates ZIP file presence
- Creates required directories

### Phase 2: File Staging
- Extracts ZIP to temporary directory
- Copies content to `test/input`
- Cleans up temporary files

### Phase 3: Java Book Loader
- Parses EPUB structure
- Validates XML against DTD
- Applies content tagging rules
- Performs drug/disease linking (optional)
- Saves to database (staging STG_RIT001)
- Outputs to `test/finalOutput/<ISBN>`

### Phase 4: Content Organization
- Copies XML to `C:\BookloaderLocal\FinalXML`
- Copies images to `C:\BookloaderLocal\FinalImages`
- Verifies output files exist

### Phase 5: Post-Processing (Optional)
- Transforms XML content
- Updates resource metadata
- Populates A-to-Z index
- Sets resource as active
- Creates licenses

## 🎯 Where Content Goes

### Java Loader Output
```
C:\RittenhouseRepos\Bookloader\
└── test\
    └── finalOutput\
        └── <ISBN>\
            ├── xml\           # Raw processed XML
            └── images\        # Extracted images
```

### Final Output (Copied by LoadBook_Local.bat)
```
C:\BookloaderLocal\
├── FinalXML\
│   └── <ISBN>\
│       ├── xml\               # Raw XML
│       └── images\            # Images
└── FinalImages\
    └── <ISBN>\                # Images (organized)
```

### Post-Processing Output (If R2Utilities enabled)
```
C:\BookloaderLocal\
└── TransformedContent\
    └── <ISBN>\                # Transformed XML
```

### Database
```
rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com
Database: STG_RIT001
Tables:
  - tResource             # Book metadata
  - tBookContent          # Chapter content
  - tResourceSpecialty    # Specialty assignments
  - tResourcePracticeArea # Practice area assignments
  - tAtoZIndex            # Search index
```

## 🔧 Post-Processing Setup

### Option 1: Without R2Utilities (Java Only)
**Default configuration** - LoadBook_Local.bat runs with:
```batch
set "R2UTILITIES_ENABLED=false"
```

This gives you:
- ✅ Full Java processing
- ✅ Database integration
- ✅ XML and image extraction
- ❌ No XML transformation
- ❌ No license creation

### Option 2: With R2Utilities (Full Pipeline)
**Requires R2Library repo** - See [POST_PROCESSING_SETUP.md](POST_PROCESSING_SETUP.md)

1. Build R2Utilities in R2Library repo
2. Configure LoadBook_Local.bat:
   ```batch
   set "R2UTILITIES_ENABLED=true"
   set "R2UTILITIES_EXE=C:\RittenhouseRepos\R2Library\bin\Debug\R2Utilities.exe"
   ```
3. Configure R2Utilities.exe.config with local paths

This gives you:
- ✅ Full Java processing
- ✅ Database integration
- ✅ XML transformation
- ✅ Resource activation
- ✅ License creation
- ✅ A-to-Z index population

## 📝 Monitoring & Logs

### Processing Logs
```
C:\BookloaderLocal\Logs\
├── <ISBN>_SUCCESS.log     # Successful processing
└── <ISBN>_FAIL.log        # Failed processing
```

### Java Backend Logs
```
C:\RittenhouseRepos\Bookloader\logs\
└── RISBackend.log.*       # Detailed Java logs
```

### Success/Failure Markers
```
C:\BookloaderLocal\
├── Completed\
│   └── <ISBN>.txt         # Marker for successful books
└── Failed\
    └── <ISBN>.txt         # Marker for failed books
```

## 🆚 Fast vs Normal Mode

### Fast Mode (Recommended)
```batch
.\LoadBook_Local.bat <ISBN>
# OR
.\ProcessIncomingBooks_Local.ps1 -FastMode
```
- **Time**: 2-5 minutes per book
- **Skips**: Drug/disease linking, PMID lookup
- **Good for**: Development, testing, quick validation

### Normal Mode
Configure `RISBackend.cfg` to use `ris_rules_normal.xml`, then:
```batch
.\LoadBook_Local.bat <ISBN>
# OR
.\ProcessIncomingBooks_Local.ps1
```
- **Time**: 30-60 minutes per book
- **Includes**: Full drug/disease linking, all rules
- **Good for**: Production simulation, complete validation

## 🔍 Verifying Results

### Check Database
```sql
-- Find the resource
SELECT intResourceID, vchisbn13, vchTitle, intStatus, dtCreated 
FROM tResource 
WHERE vchisbn13 = '9781234567890';

-- Check content records
SELECT COUNT(*) as ChapterCount
FROM tBookContent 
WHERE intResourceID = (SELECT intResourceID FROM tResource WHERE vchisbn13 = '9781234567890');

-- Check if active (status = 1 means active)
SELECT intStatus FROM tResource WHERE vchisbn13 = '9781234567890';
```

### Check Files
```powershell
# Check XML output
Get-ChildItem "C:\BookloaderLocal\FinalXML\9781234567890\xml" | Select-Object Name, Length

# Check images
Get-ChildItem "C:\BookloaderLocal\FinalImages\9781234567890" | Select-Object Name, Length

# View success log
Get-Content "C:\BookloaderLocal\Logs\9781234567890_SUCCESS.log"
```

## 🚨 Troubleshooting

### "ZIP file not found"
- Verify file is in `C:\BookloaderLocal\Incoming\`
- Ensure filename is `<ISBN>.zip` exactly

### "Resource already exists"
- Use `-update` flag: `.\LoadBook_Local.bat <ISBN> -update`
- Or delete from database first

### "Database connection failed"
- Check `RISBackend.cfg` database settings
- Verify network access to AWS RDS
- Test with SQL Server Management Studio

### "No XML output"
- Check Java logs: `C:\RittenhouseRepos\Bookloader\logs\RISBackend.log.*`
- Verify EPUB structure is valid
- Check for errors in processing log

## 📚 Documentation Reference

| Document | Purpose |
|----------|---------|
| **LOCAL_FULL_RUN_GUIDE.md** | Complete usage guide (read this first!) |
| **POST_PROCESSING_SETUP.md** | Configure C# post-processing |
| **README.md** | Overview and quick reference |
| **FLAG_REFERENCE.md** | Command-line flag reference |
| **QUICK_REF.md** | Quick reference card |
| **LOCAL_DEBUG_GUIDE.md** | VS Code debugging |

## 🎉 Success Criteria

You'll know everything is working when:

1. ✅ Setup-LocalEnvironment.ps1 creates all directories
2. ✅ Test-LocalSetup.bat shows all checks passing
3. ✅ LoadBook_Local.bat processes a book successfully
4. ✅ Success log appears in `C:\BookloaderLocal\Logs\`
5. ✅ XML files appear in `C:\BookloaderLocal\FinalXML\<ISBN>\xml\`
6. ✅ Database record created in tResource table
7. ✅ Resource status is Active (if post-processing ran)

## 🔧 Next Steps

1. **Run Setup**: `.\Setup-LocalEnvironment.ps1`
2. **Verify Config**: `.\Test-LocalSetup.bat`
3. **Test Single Book**: `.\LoadBook_Local.bat <ISBN>`
4. **Review Output**: Check logs and database
5. **Configure Post-Processing**: See POST_PROCESSING_SETUP.md (optional)
6. **Batch Process**: `.\ProcessIncomingBooks_Local.ps1 -FastMode`

## 💡 Pro Tips

- Start with **fast mode** for quick iteration
- Use **VS Code debugging** during development
- Enable **post-processing** only when needed
- Check **success logs** for detailed information
- Use **batch processing** for multiple books
- Monitor **Java logs** for detailed troubleshooting

---

**You're all set!** 🚀 

Run `.\Setup-LocalEnvironment.ps1` to begin, then check out **LOCAL_FULL_RUN_GUIDE.md** for complete instructions.
