# 📁 LocalDevTesting Folder

This folder contains all testing, debugging, and full-run tools for local book processing development.

## 📝 Files in This Folder

### ⭐ Full-Run Scripts (NEW!)
- **LoadBook_Local.bat** - Complete book processing pipeline (mimics staging)
- **ProcessIncomingBooks_Local.ps1** - Batch processing for multiple books
- **Setup-LocalEnvironment.ps1** - One-time setup for local directories
- **Test-LocalSetup.bat** - Verify configuration before running

### Java Test Files
- **test_local_mode.java** - Enhanced test harness with skipLinks/noDB modes
- **test_breakpoint_proof.java** - Simple test to prove breakpoint debugging works
- **BookloaderPhaseLogger.java** - Per-ISBN logging utility

### Batch Scripts
- **test_config.bat** - Quick configuration validation
- **test_breakpoints.bat** - Test breakpoint functionality

### Documentation
- **LOCAL_FULL_RUN_GUIDE.md** - **🌟 NEW!** Complete guide for full processing pipeline
- **POST_PROCESSING_SETUP.md** - **🌟 NEW!** Configure C# post-processing
- **FLAG_REFERENCE.md** - Complete flag reference and usage guide
- **QUICK_REF.md** - Quick reference card
- **DEBUGGING_QUICKSTART.md** - Step-by-step debugging tutorial
- **LOCAL_DEBUG_GUIDE.md** - Complete debugging and development guide

## 🚀 Quick Start

### Option A: Full Production-Like Processing (NEW! 🌟)

Process books exactly like staging environment:

```powershell
# 1. One-time setup
.\Setup-LocalEnvironment.ps1

# 2. Place ZIP file(s) in incoming directory
copy "book.epub" "C:\BookloaderLocal\Incoming\9781234567890.zip"

# 3. Process a single book
.\LoadBook_Local.bat 9781234567890

# OR process all books in incoming directory (batch mode)
.\ProcessIncomingBooks_Local.ps1 -FastMode
```

👉 **See [LOCAL_FULL_RUN_GUIDE.md](LOCAL_FULL_RUN_GUIDE.md) for complete instructions**

### Option B: Fast Development Testing (VS Code)

For iterative development with debugging:

1. **Test Configuration**: `test_config.bat`

2. **Test Breakpoint Debugging**:
   - Open **test_breakpoint_proof.java**
   - Set breakpoint on line 18
   - Press **F5** → Choose "🎯 Breakpoint Test"

3. **Fast Development Mode**:
   - VS Code: **F5** → "⚡ Skip Links + No DB (Fastest)"
   - Command: `java test_local_mode --skipLinks --noDB`

4. **Full Processing**:
   - VS Code: **F5** → "🐢 Normal Mode (Full)"

## 🎯 Available Debug Configurations

In VS Code (Press F5):
- **⚡ Skip Links + No DB (Fastest)** - ~5-10 min, best for dev iteration
- **🚫 No DB Only** - Test with linking but no database
- **🔗 Skip Links Only** - Test database without linking wait
- **🐢 Normal Mode (Full)** - Full production-like processing
- **🔄 Normal + Update** - Allow resource updates
- **⚙️ Test Config** - Configuration validation only
- **🎯 Breakpoint Test** - Test debugging setup

## 📖 Documentation

**Full Production-Like Processing:**
1. **LOCAL_FULL_RUN_GUIDE.md** - 🌟 Complete guide for full pipeline
2. **POST_PROCESSING_SETUP.md** - Configure C# post-processing

**Development & Debugging:**
1. **FLAG_REFERENCE.md** - Understanding all available flags
2. **DEBUGGING_QUICKSTART.md** - Setting up debugging
3. **QUICK_REF.md** - Command cheat sheet
4. **LOCAL_DEBUG_GUIDE.md** - Comprehensive debugging guide

## 🔥 Common Workflows

**Full production-like processing:**
```powershell
# Process single book with database and full pipeline
.\LoadBook_Local.bat 9781234567890

# Batch process multiple books (fast mode)
.\ProcessIncomingBooks_Local.ps1 -FastMode
```

**Quick iteration during development:**
```bash
java test_local_mode --skipLinks --noDB
```

**Test database operations:**
```bash
java test_local_mode --skipLinks
```

**Test linking without DB:**
```bash
java test_local_mode --noDB
```

**Full validation:**
```bash
java test_local_mode --normal
```

## ⚠️ Important Notes

- Old flags `--fast` and `--skip-db-save` have been **removed**
- Use `--skipLinks` and `--noDB` instead (clearer intent)
- Per-ISBN logs are in `test/logs/` with SUCCESS/FAIL names
- Actual exceptions are now logged to per-ISBN files
- 🎯 **PROOF: Breakpoint Test** - Verify debugging works (5 seconds)
- 🚀 **Fast + Skip DB** - Fastest testing mode (1-3 min)
- ⚡ **Fast Mode** - Quick with DB save (2-5 min)
- 🐢 **Normal Mode** - Full processing (30-60 min)
- ⚙️ **Config Test** - Validate setup (5 seconds)

## 📊 Performance Comparison

| Mode | Drug/Disease Linking | DB Save | Runtime |
|------|---------------------|---------|---------|
| **Fast + Skip DB** 🚀 | ❌ | ❌ | 1-3 min |
| **Fast** ⚡ | ❌ | ✅ | 2-5 min |
| **Normal** 🐢 | ✅ | ✅ | 30-60 min |

## 🐛 Debugging

### Set Breakpoints in Main.java
Key locations:
- Line 279: `runRISBackend()` - Main entry
- Line 556: `prepareBookXml()` - EPUB processing
- Line 883: `processRules()` - Rule processing
- Line 2058: `loadContent()` - Database save

### Controls
- **F5** - Continue
- **F10** - Step Over
- **F11** - Step Into
- **Shift+F5** - Stop Debugging

## 📚 Documentation

See the markdown files in this folder for detailed guides:
- Start with **DEBUGGING_QUICKSTART.md** for a quick tutorial
- Use **LOCAL_DEBUG_GUIDE.md** for comprehensive information
- Check **QUICK_REF.md** for quick reference

## ✅ Verified Working

All features tested and working:
- ✅ Configuration loading
- ✅ Fast mode (no linking)
- ✅ Skip database save flag
- ✅ Breakpoint debugging
- ✅ Variable inspection
- ✅ Step-through debugging
- ✅ Main.java integration

---

**Ready to debug! Press F5 in VS Code!** 🚀
