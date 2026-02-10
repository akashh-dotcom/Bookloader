# Bookloader Test Modes - Quick Reference

## Available Flags

### Core Modes
- `--normal` - Full processing with all features enabled (slowest, most complete)
- `--config` - Test configuration only (verify setup without processing)
- `--pmidOnly` - Post-process mode: Add PMIDs to already-processed content files

### Optimization Flags
- `--skipLinks` - Skip drug and disease linking (~30-60 min faster)
- `--noDB` - Skip ALL database operations (fastest, no persistence)
- `--skipPMID` - Skip PMID lookup during main processing
- `--savePMIDProgress` - Enable checkpoint/resume for PMID post-processing
- `--update` - Allow updating existing resources in database

## Usage Examples

### Testing & Development
```bash
# Test configuration only
java test_local_mode --config

# Fastest processing (no linking, no database)
java test_local_mode --skipLinks --noDB

# Skip links but save to database
java test_local_mode --skipLinks

# Skip database but run linking
java test_local_mode --noDB
```

### Production-like Processing
```bash
# Full processing with all features
java test_local_mode --normal

# Full processing, allow updates
java test_local_mode --normal --update
```

## Flag Combinations

| Flags | Links | Database | Use Case |
|-------|-------|----------|----------|
| `--config` | N/A | N/A | Test configuration |
| `--skipLinks --noDB` | ❌ | ❌ | Fastest dev testing |
| `--skipLinks` | ❌ | ✅ | Quick DB testing |
| `--noDB` | ✅ | ❌ | Test linking without DB |
| `--normal` | ✅ | ✅ | Full production mode |
| `--normal --update` | ✅ | ✅ | Update existing resources |

## VS Code Debug Configurations

Press `F5` or go to Run and Debug panel to select:

1. **⚡ Skip Links + No DB (Fastest)** - Quickest iteration for core logic
2. **🚫 No DB Only** - Test with linking but no database
3. **🔗 Skip Links Only** - Test database without waiting for linking
4. **🐢 Normal Mode (Full)** - Production-like processing
5. **🔄 Normal + Update** - Update existing resources
6. **⚙️ Test Config** - Validate configuration
7. **🎯 Breakpoint Test** - Test debugging setup

## What Each Flag Does

### `--skipLinks`
**Bypasses:**
- Drug name linking (~15-30 minutes)
- Disease name linking (~15-30 minutes)
- Drug synonym matching
- Disease synonym matching

**Use when:** Testing XML transformation, rules processing, or database operations without waiting for link processing.

**Errors prevented:** Drug/disease metadata loading errors, linking thread failures.

### `--noDB`
**Bypasses:**
- Metadata database loading (drugs, diseases)
- Resource existence checks
- ResourceDB operations
- KeywordDB operations
- Database connection establishment
- Resource creation/updates

**Use when:** Testing file processing, XML transformations, or debugging core logic without database dependencies.

**Errors prevented:** Database connection failures, SQL errors, resource conflicts, authentication issues.

### `--skipPMID`
**Bypasses:**
- PMID lookup for bibliography entries
- NCBI E-utilities API calls

**Use when:** Testing book processing without waiting for PubMed lookups. Significantly speeds up iteration during development.

**Combine with:** `--pmidOnly` to add PMIDs in a separate post-processing step.

### `--pmidOnly`
**Runs ONLY:**
- PMID lookup on already-processed XML content files
- Parallelized with 4 threads and rate limiting
- Progress logging and statistics

**Requirements:** Must run after a successful bookloader run (with or without `--skipPMID`).

**Use when:** Adding PMIDs separately to avoid repeating the entire book processing pipeline.

**Output:** Updates content XML files in `RIS.CONTENT_OUT` directory with PMID data.

### `--savePMIDProgress`
**Enables:**
- Checkpoint file creation (`.pmid_checkpoint.txt`)
- Progress saving after each file processed
- Automatic resume on restart

**Use when:** Processing large books with many bibliography entries (100+) where interruptions are likely.

**Combine with:** `--pmidOnly` for checkpoint/resume capability.

**Behavior:**
- Creates checkpoint file in output directory
- Saves: filesProcessed, totalFiles, lastFile, timestamp
- Resumes from last checkpoint on restart
- Automatically deletes checkpoint on successful completion

### Flag Independence
Both flags work independently:
- `--skipLinks` alone: Processes without linking but uses database
- `--noDB` alone: Processes with linking but skips database
- Both together: Pure file transformation testing

## Timing Estimates

| Configuration | Typical Duration | Best For |
|--------------|------------------|----------|
| `--skipLinks --noDB` | ~5-10 min | Dev iteration |
| `--skipLinks` | ~15-20 min | DB testing |
| `--noDB` | ~40-60 min | Link testing |
| `--normal` | ~60-90 min | Full validation |

*Times vary based on book size and content complexity*

## Log Files

Per-ISBN logs written to `test/logs/`:
- `{ISBN}_SUCCESS.log` - Successful processing
- `{ISBN}_FAIL.log` - Failed processing with exception details

Main log: `logs/RISBackend.log`

## Troubleshooting

**Configuration errors?**
```bash
java test_local_mode --config
```

**Database connection issues?**
```bash
java test_local_mode --noDB
```

**Linking taking too long?**
```bash
java test_local_mode --skipLinks
```

**Quick iteration during development?**
```bash
java test_local_mode --skipLinks --noDB
```

## Previous Flags (Removed)

- ❌ `--fast` - Replaced by `--skipLinks` (clearer intent)
- ❌ `--skip-db-save` - Replaced by `--noDB` (more comprehensive)

These old flags will no longer work. Use the new equivalents above.
