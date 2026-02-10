package com.rittenhouse.RIS.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Phase-based logger for local bookloader debugging
 * Provides high-level progress tracking with detailed error reporting
 */
public class BookloaderPhaseLogger {
    private static String currentIsbn = null;
    private static String currentLogFile = null;
    private static PrintWriter logWriter = null;
    private static boolean hasErrors = false;
    private static String logDirectory = "test/logs";
    private static long phaseStartTime = 0;
    
    public static void initialize(String isbn) {
        currentIsbn = isbn;
        hasErrors = false;
        
        // Create logs directory if it doesn't exist
        File dir = new File(logDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Create processing log file
        currentLogFile = logDirectory + "/" + isbn + "_Processing.log";
        try {
            logWriter = new PrintWriter(new FileWriter(currentLogFile, false));
            logSeparator();
            logLine("Book Processing Log for ISBN: " + isbn);
            logLine("Started: " + getCurrentTimestamp());
            logSeparator();
            logLine("");
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
        }
    }
    
    public static void startPhase(String phaseName, String description) {
        phaseStartTime = System.currentTimeMillis();
        logSeparator();
        logLine("PHASE: " + phaseName);
        logLine(description);
        logSeparator();
    }
    
    public static void logProgress(String message) {
        // logLine handles both file (with timestamp) and console (without)
        logLine("   " + message);
    }
    
    public static void logInfo(String message) {
        // logLine handles both file (with timestamp) and console (without)
        logLine("  " + message);
    }
    
    public static void logWarning(String message) {
        String formatted = "   WARNING: " + message;
        // logLine handles both file (with timestamp) and console (without)
        logLine(formatted);
    }
    
    public static void logError(String message) {
        logError(message, null);
    }
    
    public static void logError(String message, Exception e) {
        hasErrors = true;
        String formatted = "   ERROR: " + message;
        
        // Write to log file with timestamp (via logLine)
        logLine(formatted);
        
        // Write to console (stderr) without timestamp - already shown by BookloaderPhaseLogger
        System.err.println(formatted);
        
        if (e != null) {
            // Full exception details go to log file
            String exceptionInfo = "    Exception: " + e.getClass().getName() + ": " + e.getMessage();
            logLine(exceptionInfo);
            
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();
            
            // Write full stack trace to log file
            for (String line : stackTrace.split("\n")) {
                logLine("    " + line);
            }
            
            // Write abbreviated version to console
            System.err.println(exceptionInfo);
            System.err.println("    (Full stack trace in log file)");
        }
    }
    
    public static void endPhase(boolean success) {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (success) {
            logLine("  Phase completed successfully in " + formatDuration(elapsed));
        } else {
            logLine("  Phase failed after " + formatDuration(elapsed));
        }
        logLine("");
    }
    
    public static void logSubPhase(String name) {
        logLine("  → " + name + "...");
    }
    
    public static void finalize(boolean overallSuccess) {
        logLine("");
        logSeparator();
        
        // Determine actual success (both flag and no errors)
        boolean actualSuccess = overallSuccess && !hasErrors;
        
        if (actualSuccess) {
            logLine(" PROCESSING COMPLETED SUCCESSFULLY");
            logLine("");
            logLine("Summary:");
            logLine("  ISBN " + currentIsbn + " processed successfully");
            logLine("  All phases completed without errors");
            logLine("  Content prepared and transformed");
            logLine("  Book XML generated and validated");
            logLine("");
            logLine("Output Locations:");
            logLine("  Final content: test/finalOutput/");
            logLine("  Media files: test/media/" + currentIsbn);
            logLine("  Processing log: " + logDirectory + "/" + currentIsbn + "_SUCCESS.log");
            logLine("");
            logLine("Next Steps:");
            logLine("  Review generated content in test/finalOutput/");
            logLine("  Verify chunked files and table of contents");
            logLine("  For detailed technical log, see: logs/RISBackend.log");
        } else {
            logLine(" PROCESSING FAILED");
            logLine("");
            logLine("Summary:");
            logLine("  ISBN " + currentIsbn + " processing encountered errors");
            if (hasErrors) {
                logLine("  One or more errors occurred during processing");
                logLine("  Check error messages and stack traces above");
            }
            if (!overallSuccess) {
                logLine("  Processing did not complete successfully");
            }
            logLine("");
            logLine("Troubleshooting:");
            logLine("  Review error messages and exceptions in this log");
            logLine("  Check detailed technical log: logs/RISBackend.log");
            logLine("  Common issues:");
            logLine("    - Missing DTD files or incorrect paths");
            logLine("    - Malformed XML with missing ID references");
            logLine("    - Database connection issues (if enabled)");
            logLine("    - Missing author/editor information in bookinfo");
            logLine("    - XSLT transformation errors");
        }
        logSeparator();
        logLine("Finished: " + getCurrentTimestamp());
        
        if (logWriter != null) {
            logWriter.close();
        }
        
        // Rename log file based on success/failure
        if (currentLogFile != null && currentIsbn != null) {
            File oldFile = new File(currentLogFile);
            String status = actualSuccess ? "SUCCESS" : "FAIL";
            String newFileName = logDirectory + "/" + currentIsbn + "_" + status + ".log";
            File newFile = new File(newFileName);
            
            if (oldFile.renameTo(newFile)) {
                System.out.println("\n📄 Processing log: " + newFileName);
                if (!actualSuccess) {
                    System.out.println("    Review this log for error details and troubleshooting steps");
                } else {
                    System.out.println("    All phases completed successfully");
                }
            }
        }
    }
    
    private static void logLine(String message) {
        String timestamped = "[" + getCurrentTimestamp() + "] " + message;
        // Write timestamped version to file only
        if (logWriter != null) {
            logWriter.println(timestamped);
            logWriter.flush(); // Ensure immediate write
        }
        // Write clean version to console (no timestamp)
        System.out.println(message);
    }
    
    private static void logSeparator() {
        String separator = "========================================";
        if (logWriter != null) {
            logWriter.println(separator);
        }
        System.out.println(separator);
    }
    
    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
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
