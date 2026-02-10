# BookLoader Workflow Guide

**Understanding the Complete Book Processing Pipeline**

*Last Updated: January 2, 2026*

---

## Executive Summary

This document details the exact ordered flow of the BookLoader system, showing how books are processed from ZIP files through staging and made visible/licensed in the R2 system. This analysis covers database operations, file system requirements, and critical failure points.

---

## Complete Book Processing Flow

### Phase 1: RIS Backend Processing (Java)

**Entry Point**: `src/com/rittenhouse/RIS/Main.java` - `main()` method

#### Step 1: Unzip & Prepare
**Location**: `LoadBook.bat` (lines 40-60)

- Extract ISBN ZIP file to temporary working directory
- Copy extracted files to `RIS.CONTENT_IN` directory (e.g., `E:\R2BookLoader\Job\content\in`)
- Uses 7-Zip for extraction, robocopy for file transfer

#### Step 2: RIS Backend Processing
**Location**: `src/com/rittenhouse/RIS/Main.java` (lines 2088-2133)

The Java RIS Backend performs the following operations:

1. **Parse EPUB/DocBook XML**
   - Read source content files
   - Validate XML structure
   - Extract metadata (title, author, publisher, ISBN variants)

2. **Content Tagging**
   - Tag medical drugs (primary and synonyms)
   - Tag diseases (primary and synonyms)
   - Tag keywords and medical terms
   - Link references to external resources

3. **XML Generation**
   - Generate chunked XML files (one per chapter)
   - Create table of contents (toc.xml)
   - Add RIS metadata to XML structure

4. **Database Writes**
   - Via `ResourceDB.addNewResource` method
   - **Critical**: All database operations happen here in the Java layer

#### Step 3: Exit Code Handling
**Location**: `LoadBook.bat` (lines 64-86)

- **Exit Code 0**: Success - proceed to post-processing
- **Exit Code -4**: Failure - cleanup required, process stops
- **Other codes**: Various failure states

---

### Phase 2: File System Placement

**Handled by**: `Main.loadContent` method (lines 1958-2087)

#### Step 4: XML Output Placement

```
Source:      RIS.CONTENT_TEMP (e.g., E:\R2BookLoader\Job\content\temp\)
Destination: RIS.DEST_NON_TEXTML_CONTENT_PATH (e.g., E:\R2v2-XMLbyISBN\{ISBN}\xml\)
```

**Files Created**:
- `book.{ISBN}.xml` - Main book XML document
- `chapter.{ISBN}.001.xml` - Individual chapter files
- `chapter.{ISBN}.002.xml`
- `...` (one per chapter)
- `toc.xml` - Table of contents

#### Step 5: Image File Placement

```
Source:      RIS.CONTENT_TEMP\MultiMedia
Destination: {ImageDestination}\{ISBN}\
```

**Files Copied**:
- All image files (JPG, PNG, GIF)
- Preserves original filenames and structure

---

### Phase 3: Post-Processing (C# R2Utilities)

**Entry Point**: `BookLoaderPostProcessingTask.cs` - `Run()` method (line 62)

#### Step 6: Database Lookup
**Location**: `BookLoaderPostProcessingTask.cs` (line 85)

```csharp
var resource = _resourceCoreDataService.GetResourceByIsbn(_isbn);
```

- Query `tResource` table by ISBN to get `iResourceId`
- **FAILURE POINT**: If RIS Backend didn't write to database, this returns null
- Common cause: `RIS.TEST_MODE=true` in configuration

#### Step 7: Resource Data Updates
**Location**: `BookLoaderPostProcessingTask.cs` - `UpdateResourceData()` (lines 156-215)

**Database Operations**:

1. **Update Sort Title** (`tResource` table)
   - Strip leading articles (A, AN, THE)
   - Set `vchResourceSortTitle`
   - Set `chrAlphaKey` (first letter for alphabetical indexing)

2. **Insert Default Specialty** (`tResourceSpecialty` table)
   - Add default medical specialty association
   - Required for specialty-based filtering

3. **Insert Default Practice Area** (`tResourcePracticeArea` table)
   - Add default practice area association
   - Required for practice area filtering

4. **Update A-to-Z Index** (`tAtoZIndex` table) - **CRITICAL**
   - Lines 349-375
   - Populates search index terms
   - **Book will not appear in search results without this**

#### Step 8: File Copy to Production Locations
**Location**: `BookLoaderPostProcessingTask.cs` - `CopyContent()` (lines 219-257)

**XML Files**:
```
Source:      {BookLoaderSourceRoot}\{ISBN}\xml\
Destination: {ContentLocation}\{ISBN}\
```

**Image Files**:
```
Source:      {BookLoaderSourceRoot}\{ISBN}\images\
Destination: {ImageDestination}\{ISBN}\
```

**Operation Details**:
- Overwrites existing files if present
- Validates source directory exists
- Logs all copy operations

#### Step 9: Content Transformation
**Location**: `BookLoaderPostProcessingTask.cs` - `TransformXmlContent()` (lines 260-288)

**Operations**:
- Transform XML for TextML/DITA CMS integration
- Generate additional search indexes
- Apply XSLT transformations for display formatting

#### Step 10: TOC Update (Optional)
**Location**: `BookLoaderPostProcessingTask.cs` - `UpdateTocXml()` (line 375)

**Condition**: Only if `-includeChapterNumbersInToc=true` parameter passed

**Operation**:
- Update `toc.xml` to include chapter numbers in display
- Enhances navigation experience

#### Step 11: Activation
**Location**: `BookLoaderPostProcessingTask.cs` (line 125)

**Database Operation**:
```csharp
tResource.iResourceStatusId = Active
```

- Sets resource status to "Active"
- **Makes book visible** in the R2 application
- Without this, book exists but is hidden

#### Step 12: Auto-Licensing
**Location**: `BookLoaderPostProcessingTask.cs` (lines 128-137)

**Database Operations**:
- Insert into `tResourceLicense` table
- Creates licenses for configured institutions
- Uses `AutoLicensesNumberOfLicenses` configuration setting
- **Makes book accessible** to licensed users

---

## Database Tables Modified

### Written by RIS Backend (Java)

| Table | Purpose | Critical? |
|-------|---------|-----------|
| `tPublisher` | Publisher metadata (name, contact info) | Yes |
| `tResource` | **Main resource record** (title, ISBN, author, description) | **Yes** |
| `tResourceISBN` | ISBN-10, ISBN-13, eISBN variants | Yes |
| `tNewResourceQue` | Processing queue tracking | No |
| `tResourceDiscipline` | Subject/discipline associations | Yes |
| `tKeywordResource` | Tagged medical terms (drugs, diseases) | Yes |
| `tChapterResource` | Chapter metadata (title, sequence) | Yes |

### Written by Post-Processing (C#)

| Table | Purpose | Critical? |
|-------|---------|-----------|
| `tResourceSpecialty` | Medical specialty associations | Yes |
| `tResourcePracticeArea` | Practice area associations | Yes |
| `tAtoZIndex` | **Search indexing terms** | **Yes** |
| `tResourceLicense` | Institution licenses | **Yes** |

### Database Write Summary

**Total Tables**: 11 tables modified across 2 processing phases

**Critical Path Tables** (book won't be visible/accessible without these):
1. `tResource` - Core book record
2. `tAtoZIndex` - Search functionality
3. `tResourceLicense` - User access control

---

## Required File System Structure

### Production XML Location

```
E:\R2v2-XMLbyISBN\
  └── {ISBN}\
      └── xml\
          ├── book.{ISBN}.xml          (main book XML)
          ├── chapter.{ISBN}.001.xml   (chapter 1)
          ├── chapter.{ISBN}.002.xml   (chapter 2)
          ├── chapter.{ISBN}.003.xml   (chapter 3)
          ├── ...
          └── toc.xml                  (table of contents)
```

### Production Image Location

```
{ImageDestination}\
  └── {ISBN}\
      ├── image001.jpg
      ├── image002.jpg
      ├── figure_01_01.png
      └── ...
```

### Working Directories

```
E:\R2BookLoader\
  ├── TestUploadPending\      (ZIP files awaiting processing)
  ├── Completed\              (Successfully processed ISBNs)
  ├── Failed\                 (Failed processing attempts)
  ├── Logs\                   (Processing logs)
  └── Job\
      ├── bin\                (LoadBook.bat, RISBackend.jar)
      ├── content\
      │   ├── in\             (Extracted ZIP contents)
      │   └── temp\           (Processed XML before final copy)
      └── lib\                (JAR dependencies)
```

---

## Critical Failure Points

### 1. RIS Backend Exits with -4
**Symptom**: Java process fails, exit code -4  
**Cause**: Database write failure, validation error, or processing exception  
**Impact**: Partial database writes (orphaned records)  
**Resolution**: Run `FailedResourceCleanup.java` to remove partial records

### 2. TEST_MODE=true in Staging
**Symptom**: Post-processing fails with "Resource not found"  
**Cause**: `RIS.TEST_MODE=true` in configuration file  
**Impact**: RIS Backend skips all database writes  
**Resolution**: Set `RIS.TEST_MODE=false` in `RISBackend.cfg`

### 3. Missing XML Output Folder
**Symptom**: RIS Backend succeeds but no XML files created  
**Cause**: `RIS.DEST_NON_TEXTML_CONTENT_PATH` doesn't exist  
**Impact**: Post-processing can't find source files  
**Resolution**: Create directory structure before processing

### 4. Post-Processing Can't Find ISBN
**Symptom**: "Resource not found" error in post-processing  
**Cause**: Database not written by RIS Backend  
**Root Causes**:
- TEST_MODE enabled
- Database connection failed
- Transaction rolled back
**Resolution**: Verify database connectivity, check RIS Backend logs

### 5. tAtoZIndex Not Populated
**Symptom**: Book exists but doesn't appear in search results  
**Cause**: Post-processing failed before indexing step  
**Impact**: Book invisible to users searching by title/keyword  
**Resolution**: Rerun post-processing task for ISBN

### 6. Book Not Licensed
**Symptom**: Book visible but "Access Denied" to users  
**Cause**: Auto-licensing failed or disabled  
**Impact**: Book exists but no users can access it  
**Resolution**: Manually insert `tResourceLicense` records

---

## Running on Home PC vs Staging

### Scenario: Process Locally, Deploy to Staging

#### Option A: Full Local Processing + Database Export

**Step 1: Local Processing**
```batch
# Configure for local database
RIS.TEST_MODE=false
RIS.DB_HOST=localhost
RIS.DB_NAME=R2_Local
```

**Step 2: Process Book**
```batch
LoadBook.bat 9781234567890
```

**Step 3: Export Database Records**
```sql
-- Export resource and related records
SELECT * FROM tResource WHERE vchisbn13 = '9781234567890'
SELECT * FROM tResourceISBN WHERE iResourceId = @resourceId
SELECT * FROM tResourceDiscipline WHERE iResourceId = @resourceId
-- ... export all related tables
```

**Step 4: Deploy to Staging**
1. Copy XML files to staging file server
2. Copy image files to staging file server
3. Import database records into staging database
4. Verify book visibility and licensing

**Pros**:
- Process multiple books locally without affecting staging
- Test changes before deployment
- Faster iteration cycle

**Cons**:
- Must maintain local database
- Manual export/import process
- Database schema must match staging

---

#### Option B: Process Locally, Reprocess on Staging

**Step 1: Local Processing (Test Mode)**
```batch
# Configure for test mode
RIS.TEST_MODE=true
```

**Step 2: Process Book Locally**
```batch
LoadBook.bat 9781234567890
# Validates processing, generates XML, but no database writes
```

**Step 3: Copy ZIP to Staging**
```powershell
Copy-Item "C:\LocalBooks\9781234567890.zip" `
          "\\staging-server\E$\R2BookLoader\TestUploadPending\"
```

**Step 4: Reprocess on Staging**
```powershell
# SSH or remote session to staging
LoadBook.bat 9781234567890
```

**Pros**:
- Single source of truth (staging database)
- No manual database export/import
- Guaranteed schema compatibility

**Cons**:
- Must reprocess each book on staging
- Slower deployment cycle
- Requires staging server access

---

#### Option C: Hybrid Approach (Recommended)

**Step 1: Local Validation**
```batch
RIS.TEST_MODE=true
LoadBook.bat 9781234567890
# Validates EPUB structure, generates test XML
```

**Step 2: Review Generated XML**
- Inspect chunked XML files
- Verify table of contents
- Check image extraction

**Step 3: Deploy if Valid**
```powershell
# Copy validated ZIP to staging
Copy-Item "C:\LocalBooks\9781234567890.zip" `
          "\\staging-server\E$\R2BookLoader\TestUploadPending\"

# Trigger staging processing
Invoke-Command -ComputerName staging-server -ScriptBlock {
    E:\R2BookLoader\Job\bin\LoadBook.bat 9781234567890
}
```

**Pros**:
- Fast local validation
- Reliable staging deployment
- No database synchronization issues

**Cons**:
- Books still processed twice
- Requires remote execution capability

---

## Minimum Requirements for Book Visibility

### Required Database Records

1. **tResource**
   - `iResourceId` (primary key)
   - `vchisbn13` (ISBN-13)
   - `vchTitle` (title)
   - `iResourceStatusId = Active` ✅
   - `dtDateAdded`

2. **tResourceISBN**
   - Link to `iResourceId`
   - ISBN-10, ISBN-13, eISBN variants

3. **tAtoZIndex** ✅
   - Search index entries for title
   - Index entries for keywords

4. **tResourceLicense** ✅
   - At least one institution license
   - `dtStartDate` ≤ current date
   - `dtEndDate` ≥ current date (or NULL)

### Required File System Files

1. **XML Structure**
   ```
   {CONTENT_PATH}\{ISBN}\
     ├── book.{ISBN}.xml
     ├── chapter.{ISBN}.*.xml (all chapters)
     └── toc.xml
   ```

2. **Image Files** (if book contains images)
   ```
   {IMAGE_PATH}\{ISBN}\
     └── *.jpg, *.png, *.gif
   ```

### Visibility Checklist

- [ ] `tResource.iResourceStatusId = Active`
- [ ] `tAtoZIndex` populated with search terms
- [ ] `tResourceLicense` has valid license(s)
- [ ] XML files exist in content path
- [ ] Image files copied (if applicable)
- [ ] Book appears in search results
- [ ] Book accessible to licensed users

---

## Configuration File Reference

### RISBackend.cfg (Java Layer)

**Critical Settings**:

```properties
# Database Connection
RIS.DB_HOST=rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com
RIS.DB_PORT=1433
RIS.DB_NAME=STG_RIT001
RIS.DB_USER=RittAdmin
RIS.DB_PASSWORD=********

# Processing Mode
RIS.TEST_MODE=false          # MUST BE FALSE for staging

# File Paths
RIS.CONTENT_IN=E:\R2BookLoader\Job\content\in
RIS.CONTENT_TEMP=E:\R2BookLoader\Job\content\temp
RIS.DEST_NON_TEXTML_CONTENT_PATH=E:\R2v2-XMLbyISBN

# Threading
RIS.THREAD_COUNT_MAXIMUM=8
RIS.LINKING_THREAD_COUNT_MAXIMUM=4
```

### R2Utilities Settings (C# Layer)

**Critical Settings**:

```xml
<!-- appsettings.json or web.config -->
<ContentSettings>
  <BookLoaderSourceRoot>E:\R2v2-XMLbyISBN</BookLoaderSourceRoot>
  <ContentLocation>E:\R2Content</ContentLocation>
  <ImageDestination>E:\R2Images</ImageDestination>
</ContentSettings>

<LicensingSettings>
  <AutoLicensesNumberOfLicenses>10</AutoLicensesNumberOfLicenses>
</LicensingSettings>
```

---

## Troubleshooting Guide

### Problem: Book Processed But Not Visible

**Diagnosis Steps**:

1. Check database record exists:
   ```sql
   SELECT * FROM tResource WHERE vchisbn13 = '9781234567890'
   ```

2. Check status is Active:
   ```sql
   SELECT iResourceStatusId FROM tResource WHERE vchisbn13 = '9781234567890'
   -- Should return 1 (Active)
   ```

3. Check search index:
   ```sql
   SELECT * FROM tAtoZIndex WHERE iResourceId = @resourceId
   -- Should have multiple entries
   ```

4. Check licensing:
   ```sql
   SELECT * FROM tResourceLicense WHERE iResourceId = @resourceId
   AND dtStartDate <= GETDATE()
   AND (dtEndDate IS NULL OR dtEndDate >= GETDATE())
   -- Should have at least one valid license
   ```

5. Check XML files exist:
   ```powershell
   Test-Path "E:\R2v2-XMLbyISBN\9781234567890\xml\book.9781234567890.xml"
   ```

---

### Problem: Post-Processing Fails with "Resource Not Found"

**Diagnosis Steps**:

1. Check RISBackend.cfg:
   ```properties
   RIS.TEST_MODE=false    # Must be false!
   ```

2. Check RIS Backend completed successfully:
   ```powershell
   # Look for exit code in logs
   Select-String -Path "E:\R2BookLoader\Logs\*.log" -Pattern "ExitCode=0"
   ```

3. Check database write occurred:
   ```sql
   SELECT dtDateAdded FROM tResource WHERE vchisbn13 = '9781234567890'
   -- Should show timestamp from RIS Backend run
   ```

4. Check for partial records (cleanup needed):
   ```sql
   SELECT * FROM tNewResourceQue WHERE vchisbn13 = '9781234567890'
   -- If exists but tResource doesn't, run cleanup
   ```

---

### Problem: RIS Backend Exits with -4

**Common Causes**:

1. **Database Connection Failed**
   - Verify connection string in RISBackend.cfg
   - Test connectivity: `Test-NetConnection -ComputerName rittenhousedb.crncufb491o7.us-east-2.rds.amazonaws.com -Port 1433`

2. **Invalid EPUB Structure**
   - Check RIS Backend logs for XML validation errors
   - Verify EPUB package.opf file exists and is valid

3. **Missing Required Metadata**
   - Check for ISBN in EPUB metadata
   - Verify title and author fields present

4. **Duplicate ISBN**
   - Check if ISBN already exists in database
   - Use `-update` flag if intentional update

**Cleanup After -4 Failure**:

```java
// Run from command line:
java -cp %LOCALCLASSPATH%;.\RISBackend.jar ^
  com.rittenhouse.RIS.util.FailedResourceCleanup ^
  -isbn=9781234567890
```

---

## Performance Optimization

### Processing Time Breakdown

**Typical Book (300 pages, 20 chapters)**:

| Phase | Time | Percentage |
|-------|------|------------|
| ZIP extraction | 5 seconds | 2% |
| RIS Backend (Java) | 180 seconds | 72% |
| File copying | 10 seconds | 4% |
| Post-processing (C#) | 45 seconds | 18% |
| Licensing/Indexing | 10 seconds | 4% |
| **Total** | **250 seconds** | **100%** |

### Optimization Opportunities

1. **Threading Configuration**
   ```properties
   # Increase for powerful servers
   RIS.THREAD_COUNT_MAXIMUM=16
   RIS.LINKING_THREAD_COUNT_MAXIMUM=8
   ```

2. **Database Connection Pooling**
   - Ensure connection pooling enabled
   - Tune pool size for concurrent processing

3. **Batch Processing**
   - Process multiple books in parallel using `ProcessIncomingZipsNew.ps1`
   - Adjust `BatchTimeoutMinutes` for large books

4. **File I/O**
   - Use SSD storage for working directories
   - Minimize network file copying (process on final destination server)

---

## Appendix A: Complete File Inventory

### Files Created During Processing

#### Per ISBN (e.g., 9781234567890)

**XML Files** (in `E:\R2v2-XMLbyISBN\9781234567890\xml\`):
- `book.9781234567890.xml` - Main book document
- `chapter.9781234567890.001.xml` - Chapter 1
- `chapter.9781234567890.002.xml` - Chapter 2
- `chapter.9781234567890.NNN.xml` - Chapter N
- `toc.xml` - Table of contents

**Image Files** (in `E:\R2Images\9781234567890\`):
- Original filenames preserved from EPUB
- Formats: JPG, PNG, GIF

**Log Files** (in `E:\R2BookLoader\Logs\`):
- `ProcessIncomingZips_YYYYMMDD_HHMMSS.log` - Master log
- `9781234567890_YYYYMMDD_HHMMSS.log` - Per-book log
- `9781234567890_YYYYMMDD_HHMMSS.stdout.log` - Java stdout
- `9781234567890_YYYYMMDD_HHMMSS.stderr.log` - Java stderr

**Status Files**:
- `E:\R2BookLoader\Completed\9781234567890.txt` - Success marker
- OR `E:\R2BookLoader\Failed\9781234567890.txt` - Failure marker

---

## Appendix B: Database Schema Reference

### tResource (Main Book Record)

```sql
CREATE TABLE tResource (
    iResourceId INT PRIMARY KEY IDENTITY,
    vchisbn13 VARCHAR(13) NOT NULL,
    vchTitle NVARCHAR(500) NOT NULL,
    vchResourceSortTitle NVARCHAR(500),
    chrAlphaKey CHAR(1),
    iResourceStatusId INT DEFAULT 1,  -- 1=Active, 2=Inactive
    dtDateAdded DATETIME DEFAULT GETDATE(),
    -- ... additional fields
)
```

### tResourceLicense (User Access Control)

```sql
CREATE TABLE tResourceLicense (
    iResourceLicenseId INT PRIMARY KEY IDENTITY,
    iResourceId INT NOT NULL,
    iInstitutionId INT NOT NULL,
    dtStartDate DATETIME NOT NULL,
    dtEndDate DATETIME NULL,
    iNumberOfLicenses INT DEFAULT 1,
    -- ... additional fields
)
```

### tAtoZIndex (Search Indexing)

```sql
CREATE TABLE tAtoZIndex (
    iAtoZIndexId INT PRIMARY KEY IDENTITY,
    iResourceId INT NOT NULL,
    vchIndexTerm NVARCHAR(255) NOT NULL,
    iIndexType INT,  -- 1=Title, 2=Keyword, 3=Author, etc.
    -- ... additional fields
)
```

---

## Appendix C: Command Reference

### Manual Book Processing

```batch
REM Process new book
E:\R2BookLoader\Job\bin\LoadBook.bat 9781234567890

REM Update existing book
E:\R2BookLoader\Job\bin\LoadBook.bat 9781234567890 -update
```

### Batch Processing (PowerShell)

```powershell
# Process all ZIPs in incoming folder
.\ProcessIncomingZipsNew.ps1

# Update mode for all ZIPs
.\ProcessIncomingZipsNew.ps1 -Update

# Adjust timeout for large books
.\ProcessIncomingZipsNew.ps1 -BatchTimeoutMinutes 480

# Adjust heartbeat logging interval
.\ProcessIncomingZipsNew.ps1 -HeartbeatSeconds 30
```

### Cleanup Failed Resources

```batch
java -cp %LOCALCLASSPATH%;.\RISBackend.jar ^
  com.rittenhouse.RIS.util.FailedResourceCleanup ^
  -isbn=9781234567890
```

### Database Queries

```sql
-- Check book status
SELECT 
    r.vchisbn13,
    r.vchTitle,
    CASE r.iResourceStatusId 
        WHEN 1 THEN 'Active' 
        WHEN 2 THEN 'Inactive' 
    END AS Status,
    r.dtDateAdded
FROM tResource r
WHERE r.vchisbn13 = '9781234567890'

-- Check licensing
SELECT 
    rl.*,
    i.vchInstitutionName
FROM tResourceLicense rl
INNER JOIN tInstitution i ON rl.iInstitutionId = i.iInstitutionId
WHERE rl.iResourceId = (
    SELECT iResourceId FROM tResource WHERE vchisbn13 = '9781234567890'
)

-- Check search indexing
SELECT 
    COUNT(*) AS IndexTermCount
FROM tAtoZIndex
WHERE iResourceId = (
    SELECT iResourceId FROM tResource WHERE vchisbn13 = '9781234567890'
)
```

---

## Summary

The BookLoader workflow consists of **three main phases**:

1. **RIS Backend (Java)**: Parses EPUB, tags content, writes to database
2. **File Placement**: Copies XML and images to production locations
3. **Post-Processing (C#)**: Updates indexes, copies files, activates book, creates licenses

**Critical Success Factors**:
- `RIS.TEST_MODE=false` for production processing
- All 11 database tables properly populated
- XML files in correct directory structure
- Search indexing (`tAtoZIndex`) completed
- Valid licenses created (`tResourceLicense`)
- Book status set to Active

**For local development**: Use Option C (Hybrid Approach) - validate locally with TEST_MODE, then process on staging with TEST_MODE=false for final database writes.

---

**Document Version**: 1.0  
**Created**: January 2, 2026  
**Author**: System Documentation from Code Analysis
