import com.rittenhouse.RIS.Main;
import java.io.File;
import com.rittenhouse.RIS.util.BookloaderPhaseLogger;

/**
 * Local debugging test harness for Bookloader processing
 * 
 * Modes:
 *   --normal            : Full processing with all linking (slower)
 *   --config            : Just test configuration
 *   --update            : Allow updating existing resources
 *   --skipLinks         : Skip drug/disease linking (faster processing)
 *   --noDB              : Skip ALL database connections and operations
 *   --skipPMID          : Skip PMID (PubMed ID) lookup during processing
 *   --pmidOnly          : Post-process mode - add PMIDs to already-processed books
 *   --savePMIDProgress  : Enable checkpointing for PMID operations (can resume if interrupted)
 * 
 * Examples:
 *   java test_local_mode --config
 *   java test_local_mode --normal
 *   java test_local_mode --skipLinks
 *   java test_local_mode --skipLinks --noDB
 *   java test_local_mode --normal --update
 *   java test_local_mode --normal --update --skipPMID
 *   java test_local_mode --pmidOnly
 *   java test_local_mode --pmidOnly --savePMIDProgress
 */
public class test_local_mode {
    public static void main(String[] args) {
        boolean normalMode = false;
        boolean configOnly = false;
        boolean allowUpdate = false;
        boolean skipLinks = false;
        boolean noDB = false;
        boolean skipPMID = false;
        boolean pmidOnly = false;
        boolean savePMIDProgress = false;
        
        // Parse command line arguments
        for (String arg : args) {
            if ("--normal".equalsIgnoreCase(arg)) {
                normalMode = true;
            } else if ("--config".equalsIgnoreCase(arg)) {
                configOnly = true;
            } else if ("--update".equalsIgnoreCase(arg)) {
                allowUpdate = true;
            } else if ("--skipLinks".equalsIgnoreCase(arg)) {
                skipLinks = true;
            } else if ("--noDB".equalsIgnoreCase(arg)) {
                noDB = true;
            } else if ("--skipPMID".equalsIgnoreCase(arg)) {
                skipPMID = true;
            } else if ("--pmidOnly".equalsIgnoreCase(arg)) {
                pmidOnly = true;
            } else if ("--savePMIDProgress".equalsIgnoreCase(arg)) {
                savePMIDProgress = true;
            }
        }
        
        // Default to config test if no mode specified and no processing flags
        // If skipLinks, noDB, pmidOnly are set, we want to process, not just test config
        if (!normalMode && !configOnly && !skipLinks && !noDB && !skipPMID && !pmidOnly) {
            configOnly = true;
        }
        
        System.out.println("==================================================");
        System.out.println("  Bookloader Local Development Mode");
        System.out.println("==================================================");
        
        if (configOnly) {
            testConfiguration();
        } else if (pmidOnly) {
            runPMIDPostProcess(savePMIDProgress);
        } else {
            runBookloader(normalMode, allowUpdate, skipLinks, noDB, skipPMID, savePMIDProgress);
        }
    }
    
    /**
     * Test configuration access and display settings
     */
    private static void testConfiguration() {
        System.out.println("Testing configuration...\n");
        
        try {
            // Initialize Main to load configuration
            Main mainInstance = new Main();
            
            String testMode = Main.getConfigProperty("RIS.TEST_MODE");
            System.out.println("RIS.TEST_MODE: " + testMode);

            String contentIn = Main.getConfigProperty("RIS.CONTENT_IN");
            System.out.println("RIS.CONTENT_IN: " + contentIn);

            String contentTemp = Main.getConfigProperty("RIS.CONTENT_TEMP");
            System.out.println("RIS.CONTENT_TEMP: " + contentTemp);
            
            String contentOut = Main.getConfigProperty("RIS.CONTENT_OUT");
            System.out.println("RIS.CONTENT_OUT: " + contentOut);

            String dtdPath = Main.getConfigProperty("RIS.RITTENHOUSE_DTD_PATH");
            System.out.println("RIS.RITTENHOUSE_DTD_PATH: " + dtdPath);
            
            String skipTextML = Main.getConfigProperty("RIS.TEST_SKIP_TEXTML");
            System.out.println("RIS.TEST_SKIP_TEXTML: " + skipTextML);
            
            String dbUrl = Main.getConfigProperty("RISDB.URL");
            System.out.println("RISDB.URL: " + dbUrl);
            
            String dbUser = Main.getConfigProperty("RISDB.UserID");
            System.out.println("RISDB.UserID: " + dbUser);

            // Check for rules file location
            System.out.println("\nValidating rules configuration...");
            String currentDir = System.getProperty("user.dir");
            System.out.println("Current directory: " + currentDir);
            
            // Test the expected rules file path
            java.io.File rulesFile = new java.io.File(currentDir + "\\rules\\ris_rules.xml");
            if (rulesFile.exists()) {
                System.out.println("✓ Rules file found: " + rulesFile.getAbsolutePath());
            } else {
                System.err.println("✗ WARNING: Rules file not found at: " + rulesFile.getAbsolutePath());
                System.err.println(" Main.RULES_CONFIG will use: " + currentDir + "\\..\\rules\\ris_rules.xml");
                System.err.println("  Ensure you run from the project root directory!");
            }

            System.out.println("\n✓ Configuration test completed successfully!");
            System.out.println("\nNext steps:");
            System.out.println("  1. Place book folder (named by ISBN) in: " + contentIn);
            System.out.println("  2. Run with --normal for full processing");
            System.out.println("  3. Run with --skipLinks to skip drug/disease linking (faster)");
            System.out.println("  4. Add --noDB to bypass all database operations (fastest)");
            System.out.println("  5. Add --update flag to update existing resources");
            
        } catch (Exception e) {
            System.err.println("✗ Error testing configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the bookloader process
     * @param normalMode If true, use normal full processing
     * @param allowUpdate If true, allow updating existing resources
     * @param skipLinks If true, skip drug and disease linking
     * @param noDB If true, skip all database connections and operations
     * @param skipPMID If true, skip PMID lookup during processing
     * @param savePMIDProgress If true, save PMID progress for resume capability
     */
    private static void runBookloader(boolean normalMode, boolean allowUpdate, boolean skipLinks, boolean noDB, boolean skipPMID, boolean savePMIDProgress) {
        System.out.println("\nMode: " + (normalMode ? "NORMAL (full processing)" : "QUICK (optimized)"));
        System.out.println("Update mode: " + (allowUpdate ? "ENABLED" : "DISABLED"));
        System.out.println("Drug/Disease linking: " + (skipLinks ? "SKIPPED" : "ENABLED"));
        System.out.println("Database operations: " + (noDB ? "SKIPPED" : "ENABLED"));
        System.out.println("PMID lookup: " + (skipPMID ? "SKIPPED" : "ENABLED"));
        if (savePMIDProgress) {
            System.out.println("PMID checkpointing: ENABLED");
        }
        System.out.println();
        
        boolean success = false;
        String isbn = "UNKNOWN";
        
        try {
            // Get ISBN from input directory - look for folders or zip files
            String contentInDir = Main.getConfigProperty("RIS.CONTENT_IN");
            java.io.File inDir = new java.io.File(contentInDir);
            java.io.File[] bookFolders = inDir.listFiles(java.io.File::isDirectory);
            java.io.File[] zipFiles = inDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
            
            if ((bookFolders == null || bookFolders.length == 0) && (zipFiles == null || zipFiles.length == 0)) {
                System.err.println("✗ ERROR: No book folders or zip files found in: " + contentInDir);
                System.err.println("  Create a folder with the ISBN name (e.g., test/input/9781234567890/)");
                System.err.println("  OR place a zip file (e.g., test/input/9781234567890.zip)");
                return;
            }
            
            // Use first folder or zip file found
            java.io.File bookSource;
            if (bookFolders != null && bookFolders.length > 0) {
                bookSource = bookFolders[0];
                isbn = bookSource.getName();
                BookloaderPhaseLogger.logInfo("Found book folder: " + isbn);
            } else {
                bookSource = zipFiles[0];
                isbn = bookSource.getName().substring(0, bookSource.getName().length() - 4); // Remove .zip extension
                BookloaderPhaseLogger.logInfo("Found book zip: " + bookSource.getName());
                BookloaderPhaseLogger.logInfo("Will be extracted to: " + isbn);
            }
            BookloaderPhaseLogger.logInfo("Source path: " + bookSource.getAbsolutePath());
            
            // Initialize phase logger
            BookloaderPhaseLogger.initialize(isbn);
            
            BookloaderPhaseLogger.startPhase("CONFIGURATION", "Initializing bookloader with configuration");
            BookloaderPhaseLogger.logInfo("ISBN: " + isbn);
            BookloaderPhaseLogger.logInfo("Mode: " + (normalMode ? "NORMAL (full)" : "QUICK (optimized)"));
            BookloaderPhaseLogger.logInfo("Update mode: " + (allowUpdate ? "ENABLED" : "DISABLED"));
            BookloaderPhaseLogger.logInfo("Drug/Disease linking: " + (skipLinks ? "SKIPPED" : "ENABLED"));
            BookloaderPhaseLogger.logInfo("Database operations: " + (noDB ? "SKIPPED" : "ENABLED"));
            BookloaderPhaseLogger.logInfo("PMID lookup: " + (skipPMID ? "SKIPPED" : "ENABLED"));
            if (savePMIDProgress) {
                BookloaderPhaseLogger.logInfo("PMID checkpointing: ENABLED");
            }
            
            if (skipLinks) {
                BookloaderPhaseLogger.logInfo("Skipping links will bypass:");
                BookloaderPhaseLogger.logInfo("  - Drug linking (saves ~15-30 min)");
                BookloaderPhaseLogger.logInfo("  - Disease linking (saves ~15-30 min)");
            }
            
            if (skipPMID) {
                BookloaderPhaseLogger.logInfo("Skipping PMID will bypass:");
                BookloaderPhaseLogger.logInfo("  - PubMed ID lookups (saves ~5-15 min)");
            }
            
            if (noDB) {
                BookloaderPhaseLogger.logInfo("No-DB mode will bypass:");
                BookloaderPhaseLogger.logInfo("  - Metadata database queries");
                BookloaderPhaseLogger.logInfo("  - Resource creation/updates");
                BookloaderPhaseLogger.logInfo("  - All database connections");
            }
            
            // CRITICAL: Set flags BEFORE creating Main instance
            // The Main constructor loads MetaData, so skipAllDatabaseOperations must be set first!
            if (allowUpdate) {
                Main.allowResourceUpdate = true;
            }
            
            if (skipLinks) {
                Main.skipDrugDiseaseLinks = true;
            }
            
            if (noDB) {
                Main.skipAllDatabaseOperations = true;
            }
            
            if (skipPMID) {
                Main.skipPMID = true;
            }
            
            if (savePMIDProgress) {
                Main.savePMIDProgress = true;
            }
            
            // Now create Main instance (constructor will check skipAllDatabaseOperations)
            Main m = new Main();
            m.startLogger();
            BookloaderPhaseLogger.logProgress("Logger initialized");
            
            // Log what was configured
            if (allowUpdate) {
                BookloaderPhaseLogger.logProgress("Resource update mode enabled");
            }
            
            if (skipLinks) {
                BookloaderPhaseLogger.logProgress("Drug/disease linking will be skipped");
            }
            
            if (noDB) {
                BookloaderPhaseLogger.logProgress("All database operations bypassed");
            }
            
            if (skipPMID) {
                BookloaderPhaseLogger.logProgress("PMID lookup will be skipped");
            }
            
            if (savePMIDProgress) {
                BookloaderPhaseLogger.logProgress("PMID progress checkpointing enabled");
            }
            
            BookloaderPhaseLogger.endPhase(true);
            
            // Start processing
            BookloaderPhaseLogger.startPhase("BOOK PROCESSING", "Starting main RIS backend processing pipeline");
            BookloaderPhaseLogger.logInfo("Processing will include:");
            BookloaderPhaseLogger.logInfo("  • File relocation");
            BookloaderPhaseLogger.logInfo("  • XML parsing and transformation");
            BookloaderPhaseLogger.logInfo("  • Content tagging and rules processing");
            if (!skipLinks) {
                BookloaderPhaseLogger.logInfo("  • Drug and disease linking");
            }
            if (!noDB) {
                BookloaderPhaseLogger.logInfo("  • Database resource creation");
            }
            BookloaderPhaseLogger.logInfo("");
            
            // Prevent Main from calling System.exit() so we can do post-processing
            m.skipDatabaseSave = true;
            
            long startTime = System.currentTimeMillis();
            
            m.runRISBackend();
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            System.out.println("DEBUG: Main.lastExitCode = " + Main.lastExitCode);
            
            // Check exit code to determine success/failure
            if (Main.lastExitCode != 0) {
                BookloaderPhaseLogger.logError("RISBackend exited with error code: " + Main.lastExitCode);
                
                // Log actual error details if captured
                if (Main.lastErrorMessage != null) {
                    BookloaderPhaseLogger.logError("Error Details: " + Main.lastErrorMessage);
                }
                if (Main.lastException != null) {
                    if (Main.lastException instanceof Exception) {
                        BookloaderPhaseLogger.logError("Exception", (Exception)Main.lastException);
                    } else {
                        BookloaderPhaseLogger.logError("Error: " + Main.lastException.toString());
                    }
                }
                
                BookloaderPhaseLogger.logInfo("");
                BookloaderPhaseLogger.logInfo("Error Code Meanings:");
                BookloaderPhaseLogger.logInfo("  -2: Book XML preparation failed");
                BookloaderPhaseLogger.logInfo("  -3: Rules processing failed");
                BookloaderPhaseLogger.logInfo("  -4: Author information missing");
                BookloaderPhaseLogger.logInfo("  -10: Rules config load failed");
                BookloaderPhaseLogger.logInfo("  -11: Metadata load failed");
                BookloaderPhaseLogger.endPhase(false);
                success = false;
            } else {
                BookloaderPhaseLogger.logProgress("Processing completed successfully");
                BookloaderPhaseLogger.logInfo("Total processing time: " + formatDuration(elapsed));
                
                // Move output to finalOutput directory on success
                try {
                    String outputDir = Main.getConfigProperty("RIS.CONTENT_OUT");
                    String finalOutputDir = "test/finalOutput";
                    
                    File finalDir = new File(finalOutputDir);
                    if (!finalDir.exists()) {
                        finalDir.mkdirs();
                    }
                    
                    // Copy from test/output to test/finalOutput
                    File sourceDir = new File(outputDir);
                    File destDir = new File(finalOutputDir);
                    
                    if (sourceDir.exists() && sourceDir.isDirectory()) {
                        BookloaderPhaseLogger.logProgress("Moving output to finalOutput directory");
                        copyDirectory(sourceDir, destDir);
                        BookloaderPhaseLogger.logInfo("Final output location: " + finalOutputDir);
                    }
                } catch (Exception ex) {
                    BookloaderPhaseLogger.logWarning("Failed to move output to finalOutput: " + ex.getMessage());
                }
                
                BookloaderPhaseLogger.endPhase(true);
                success = true;
            }
            
        } catch (Exception e) {
            BookloaderPhaseLogger.logError("Bookloader execution failed", e);
            BookloaderPhaseLogger.endPhase(false);
            success = false;
        } finally {
            BookloaderPhaseLogger.finalize(success);
        }
        
        // Explicitly exit with appropriate code after all finalization
        System.exit(success ? 0 : 1);
    }
    
    /**
     * Run PMID post-processing only (for adding PMIDs to already-processed books)
     * @param saveProgress If true, save progress for resume capability
     */
    private static void runPMIDPostProcess(boolean saveProgress) {
        System.out.println("\n==== PMID POST-PROCESS MODE ====");
        System.out.println("This will add PubMed IDs to references in already-processed XML files.");
        System.out.println("Progress checkpointing: " + (saveProgress ? "ENABLED" : "DISABLED"));
        System.out.println();
        
        try {
            // Set flags before creating Main instance
            if (saveProgress) {
                Main.savePMIDProgress = true;
            }
            
            Main m = new Main();
            m.startLogger();
            
            System.out.println("Starting PMID post-processing...");
            m.runPMIDPostProcess();
            
            System.out.println("\n✓ PMID post-processing completed successfully!");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("\n✗ PMID post-processing failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void copyDirectory(File source, File dest) throws Exception {
        if (source.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdirs();
            }
            
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(dest, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            java.nio.file.Files.copy(source.toPath(), dest.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        } else {
            return String.format("%d sec", seconds);
        }
    }
}