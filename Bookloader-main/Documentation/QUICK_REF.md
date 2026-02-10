# Bookloader Quick Reference

## 🎯 Start Here
1. Place EPUB in `test/input/`
2. Press `F5` in VS Code
3. Choose "⚡ Skip Links + No DB (Fastest)"
4. Set breakpoints in `Main.java` as needed

## 🔥 Fast Commands
```bash
# Test config only
java test_local_mode --config

# Fastest iteration (no links, no DB, no PMID)
java test_local_mode --skipLinks --noDB --skipPMID

# Normal processing but skip PMID
java test_local_mode --skipLinks --skipPMID

# Skip links but use DB
java test_local_mode --skipLinks

# Full processing
java test_local_mode --normal

# PMID-only post-process (run after main processing)
java test_local_mode --pmidOnly
```

## 🔬 PMID Processing

### Skip PMID During Main Run
Use `--skipPMID` to skip PMID lookup during the main bookloader run. This speeds up processing when PMID data isn't critical for initial validation.

```bash
java test_local_mode --skipLinks --noDB --skipPMID
```

### PMID Post-Process Mode
After a successful bookloader run, you can add PMIDs separately using `--pmidOnly`:

```bash
# First run: Process book without PMID
java test_local_mode --skipLinks --noDB --skipPMID

# Second run: Add PMIDs to already-processed files
java test_local_mode --pmidOnly

# Third run: Add PMIDs with checkpoint/resume support
java test_local_mode --pmidOnly --savePMIDProgress
```

**Benefits:**
- ✅ Parallel PMID lookup (4 threads with rate limiting)
- ✅ Real-time progress logging (% complete, elapsed time)
- ✅ HTTP timeouts prevent hangs (5s connect, 10s read)
- ✅ NCBI API rate limiting (3 req/sec or 10 with API key)
- ✅ Detailed stats on success/failure
- ✅ Can retry PMID phase separately if it fails
- ✅ **Checkpoint/resume support with `--savePMIDProgress`**

### Checkpoint/Resume Feature
The `--savePMIDProgress` flag enables incremental progress saving during PMID post-processing. This is critical for large books with hundreds of bibliography entries.

**How it works:**
1. Progress is saved to `.pmid_checkpoint.txt` after each file
2. If processing is interrupted (Ctrl+C, crash, timeout), restart with same flags
3. Processing resumes from the last completed file
4. Checkpoint is automatically deleted on successful completion

**Usage:**
```bash
# Start PMID processing with checkpoint
java test_local_mode --pmidOnly --savePMIDProgress

# If interrupted, run the same command again to resume
java test_local_mode --pmidOnly --savePMIDProgress
```

**Checkpoint file format** (`.pmid_checkpoint.txt` in output directory):
```
filesProcessed=42
totalFiles=156
lastFile=sect1.9781683674818.d0e123456.xml
timestamp=1704297600000
```

**When to use:**
- ✅ Large books (100+ bibliography entries)
- ✅ Unstable network connections
- ✅ Long-running PMID lookups (>30 minutes)
- ✅ Testing/development where interruptions are common

## 🔬 PMID Performance

**Typical timing for book with 200 bibliographies:**
- Without parallelization: ~60-90 minutes (sequential, 1 req/sec)
- With 4 threads + rate limiting: ~15-25 minutes
- NCBI API key (10 req/sec): ~8-12 minutes

**Rate limits:**
- Without API key: 3 requests/second (enforced)
- With NCBI API key: 10 requests/second (enforced)

## 🐛 Key Breakpoints in Main.java

| Method | Line | Purpose |
|--------|------|---------|
| `runRISBackend()` | ~279 | Main entry point |
| `prepareBookXml()` | ~556 | EPUB processing starts |
| `processRules()` | ~883 | Rule processing loop |
| `loadContent()` | ~2058 | Database save |
| `performDrugAndDiseaseLinking()` | ~2274 | Drug/disease linking |

## ⚡ Processing Modes

| Feature | --skipLinks --noDB --skipPMID | --skipLinks --noDB | --skipLinks | --noDB | --normal |
|---------|-------------------------------|-------------------|-------------|---------|----------|
| EPUB Processing | ✅ | ✅ | ✅ | ✅ | ✅ |
| XML Validation | ✅ | ✅ | ✅ | ✅ | ✅ |
| PMID Lookup | ❌ | ✅ | ✅ | ✅ | ✅ |
| Drug Linking | ❌ | ❌ | ❌ | ✅ | ✅ |
| Disease Linking | ❌ | ❌ | ❌ | ✅ | ✅ |
| Database Save | ❌ | ❌ | ✅ | ❌ | ✅ |
| **Runtime** | **~3-5 min** | **~5-10 min** | **~15-20 min** | **~40-60 min** | **~60-90 min** |
| **Best For** | **Fastest dev** | **Dev iteration** | **DB testing** | **Link testing** | **Full validation** |

## 📁 Key Directories
- `test/input/` - Put book folders here (named by ISBN)
- `test/output/` - Processed XML output
- `test/temp/` - Temporary processing files
- `test/logs/` - Per-ISBN logs (ISBN_SUCCESS.log / ISBN_FAIL.log)
- `logs/` - Main application logs (RISBackend.log)

## 🔧 Config File
`RISBackend.cfg`
- Database: `127.0.0.1:11433`
- Test mode: `RIS.TEST_MODE=true`
- Skip TextML: `RIS.TEST_SKIP_TEXTML=true`

## 🎛️ VS Code Debug Configs (F5)
- **⚡ Skip Links + No DB** - Fastest dev iteration (~5-10 min)
- **🚫 No DB Only** - Test linking without database
- **🔗 Skip Links Only** - Test database without waiting for links
- **🐢 Normal Mode (Full)** - Full production-like processing
- **🔄 Normal + Update** - Allow resource updates
- **⚙️ Test Config** - Configuration validation

## 📊 Watch Console For
```
✅ ==================================================
✅   Bookloader Local Development Mode
✅ ==================================================
✅ [22:30:45] PHASE: CONFIGURATION - STARTED
✅ [22:30:46] PHASE: BOOK PROCESSING - STARTED
✅ Process complete - ISBN: 9781234567890
✅ [22:35:12] ISBN: 9781234567890 - COMPLETED ✓
```

## 📝 Check Results

**Per-ISBN logs:**
```bash
# Check test/logs/ for:
9781234567890_SUCCESS.log  # Successful processing with details
9781234567890_FAIL.log     # Failed processing with exceptions
```

**Database (if not using --noDB):**
```sql
-- Verify resource created
SELECT TOP 5 * FROM tResource 
ORDER BY iResourceId DESC;

-- Check resource files
SELECT * FROM tResourceFile 
WHERE iResourceId = [your_id];
```

## ⚠️ Common Issues
| Problem | Solution |
|---------|----------|
| Database errors | Use `--noDB` to bypass |
| Linking too slow | Use `--skipLinks` |
| Resource exists error | Add `--update` flag |
| No files found | Book folder named by ISBN in `test/input/`? |
| Breakpoint not hit | Recompile with VS Code tasks |

## 💡 Pro Tips
1. Use `--skipLinks --noDB` for 95% of development
2. Set 1-2 strategic breakpoints, not 20
3. Check per-ISBN logs for actual exceptions
4. Clean `test/temp/` between runs if weird errors
5. Small test books = faster iteration
6. Use `--config` to verify setup before processing

## 🚫 Removed Flags
- ❌ `--fast` → Use `--skipLinks` instead
- ❌ `--skip-db-save` → Use `--noDB` instead
