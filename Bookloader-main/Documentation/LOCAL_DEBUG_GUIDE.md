# Bookloader Local Development & Debugging Guide

This guide explains how to quickly process and debug the Bookloader Java application locally with fast iteration cycles.

## 🎯 Quick Start

### Option 1: VS Code Debugging (Recommended)
1. Open VS Code
2. Press `F5` or go to Run > Start Debugging
3. Choose one of these configurations:
   - **Debug Bookloader (Fast Mode) ⚡** - Quick processing, skips drug/disease linking
   - **Debug Bookloader (Normal Mode) 🐢** - Full processing with all linking
   - **Test Configuration Only ⚙** - Just verify setup

### Option 2: Batch Scripts
- `test_config.bat` - Test configuration only
- `run_fast_mode.bat` - Run in fast mode
- `run_normal_mode.bat` - Run in normal mode with full linking
- `debug_fast_mode.bat` - Start with remote debugging on port 5005

## 🚀 Fast Mode vs Normal Mode

### Fast Mode ⚡
**Purpose:** Quick iteration and debugging of the bookloader core functionality

**What's enabled:**
- ✅ EPUB parsing and extraction
- ✅ XML validation and processing
- ✅ Book structure analysis
- ✅ Database resource creation
- ✅ File splitting and chunking
- ✅ RIS info tag addition
- ✅ Content loading to database/filesystem

**What's disabled (SLOW operations):**
- ❌ Drug linking (can take 15-30 minutes)
- ❌ Disease linking (can take 15-30 minutes)
- ❌ PMID lookup
- ❌ RIS Index rules

**When to use:**
- Testing EPUB processing
- Debugging resource creation
- Validating database connectivity
- Quick iterations during development
- When you need fast feedback

**Typical runtime:** 2-5 minutes for a typical book

### Normal Mode 🐢
**Purpose:** Full production-like processing with all linking

**What's enabled:**
- ✅ Everything from Fast Mode
- ✅ Drug linking with full drug database
- ✅ Disease linking with full disease database
- ✅ Complete metadata enrichment

**When to use:**
- Final testing before production
- Validating complete linking functionality
- Testing performance of linking operations
- When you need complete processing

**Typical runtime:** 30-60+ minutes for a typical book

## 📋 Prerequisites

1. **Database Connection**
   - Database must be accessible (check `RISBackend.cfg`)
   - Current config points to: `127.0.0.1:11433` (STG_RIT001)

2. **Input Files**
   - Place EPUB file in: `./test/input/`
   - System will automatically process all EPUB files found

3. **Java Environment**
   - Java 11+ required
   - All dependencies are in `lib/` folder

## 🐛 Debugging Workflow

### Setting Breakpoints

Key methods to set breakpoints in `Main.java`:

```java
// Main entry and flow
Main.runRISBackend()          // Line 279 - Main entry point
Main.prepareBookXml()         // Line 556 - Book XML preparation
Main.processRules()           // Line 883 - Rule processing loop

// EPUB processing
EPUBParser.parseEPUB()        // EPUB extraction and parsing

// Database operations
Main.loadContent()            // Line 2058 - Save to database
ResourceDB.insertResource()   // Database insertion

// Slow operations (only in Normal Mode)
Main.performDrugAndDiseaseLinking()  // Line 2274 - Drug/disease linking
Main.linkDrug()               // Line 1837 - Drug linking
Main.linkDisease()            // Line 1770 - Disease linking
```

### Debug Session Steps

1. **Set your breakpoints** in key methods
2. **Choose debug configuration** in VS Code (F5)
3. **Place EPUB** in `./test/input/`
4. **Start debugging** - code will pause at breakpoints
5. **Inspect variables** using the Variables pane
6. **Step through code** using F10 (step over), F11 (step into)
7. **Examine database** during pauses to verify state

### Inspecting State

During debugging, inspect these key variables:
- `bookISBN` - Current book being processed
- `doc` - XML Document object
- `metaData` - Cached drug/disease data
- `foundDrugList` / `foundDiseaseList` - Discovered terms
- `configProperties` - Current configuration

## 📁 Important Files

### Configuration
- `RISBackend.cfg` - Main configuration file
  - Database connection settings
  - Path configurations
  - Test mode flags

### Rules
- `rules/ris_rules.xml` - Current active rules
- `rules/ris_rules_fast.xml` - Fast mode template (linking disabled)
- `rules/ris_rules_normal.xml` - Normal mode template (linking enabled)

### Test Directories
- `test/input/` - Place EPUB files here
- `test/output/` - Processed XML output
- `test/temp/` - Temporary processing files
- `test/finalOutput/` - Final XML when TextML is skipped
- `test/logs/` - Application logs

## 🔧 Troubleshooting

### "Unable to connect to database"
- Check database is running: `127.0.0.1:11433`
- Verify credentials in `RISBackend.cfg`
- Test connection with SQL client

### "No content files found"
- Ensure EPUB file is in `./test/input/`
- Check file permissions
- Verify EPUB is not corrupted

### "Class not found" errors
- Run `ant compile` to rebuild
- Check all JARs are in `lib/` folders
- Verify CLASSPATH in launch configuration

### Slow performance in Fast Mode
- Check that `ris_rules_fast.xml` has linking disabled
- Verify batch script copied correct rules file
- Look for `action="disable"` on linkDrug/linkDisease

### Breakpoints not hitting
- Ensure code is compiled with debug info (`-g` flag)
- Check breakpoint is on executable line (not comment/whitespace)
- Verify correct Main class is being executed

## 📊 Monitoring Progress

### Console Output
Watch for these key messages:
```
==== RISBackend started =====
Looking for content files...
Processing EPUB: [filename]
Filtering metadata...
Running disease link to...  [only in Normal Mode]
Running drug link to...     [only in Normal Mode]
Chunking content file...
Loading content to database...
==== Stopping RISBackend with Success ====
```

### Log Files
- Check `logs/RISBackend.log.1` for detailed logs
- Look for ERROR/WARN messages
- Follow thread execution

### Database Verification
Query to check resource was created:
```sql
SELECT TOP 10 * FROM Resource 
ORDER BY ResourceID DESC
```

## 🎛️ Command Line Arguments

When running `test_local_mode` directly:
```bash
# Test configuration only
java test_local_mode --config

# Fast mode
java test_local_mode --fast

# Normal mode (full processing)
java test_local_mode --normal

# With resource update enabled
java test_local_mode --fast --update
```

## 📝 Tips & Best Practices

1. **Start with --config** to verify setup before processing
2. **Use Fast Mode** for most development work
3. **Set strategic breakpoints** rather than many breakpoints
4. **Clean test directories** between runs if needed
5. **Keep test EPUBs small** for faster iteration
6. **Use the integrated terminal** to see real-time output
7. **Check logs** if something fails silently

## 🔄 Switching Between Modes

The batch scripts automatically handle rule file switching:
- Backs up current `ris_rules.xml` to `ris_rules.xml.backup`
- Copies appropriate mode rules
- Restores original after execution

When using VS Code debugging, you may need to manually switch:
```powershell
# Switch to fast mode
Copy-Item rules/ris_rules_fast.xml rules/ris_rules.xml

# Switch to normal mode
Copy-Item rules/ris_rules_normal.xml rules/ris_rules.xml
```

## 📚 Additional Resources

- [BOOKLOADER_WORKFLOW_GUIDE.md](Documentation/BOOKLOADER_WORKFLOW_GUIDE.md) - Complete workflow documentation
- [PRODUCTION_IMPLEMENTATION_GUIDE.md](Documentation/PRODUCTION_IMPLEMENTATION_GUIDE.md) - Production deployment guide
- Main.java source code - Heavily commented

## ❓ Need Help?

If you encounter issues:
1. Check this README
2. Review log files in `logs/`
3. Verify configuration in `RISBackend.cfg`
4. Test database connectivity
5. Try with a simple test EPUB first

---

**Happy Debugging! 🚀**
