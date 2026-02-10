package com.rittenhouse.RIS.util;

import org.apache.log4j.Category;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for tracking and logging processing phases with progress indicators
 */
public class PhaseLogger {
    private static final String SEPARATOR = "==================================================";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    private Category log;
    private String currentPhase;
    private long phaseStartTime;
    private String currentIsbn;
    
    public PhaseLogger(Category log) {
        this.log = log;
    }
    
    /**
     * Start processing a new ISBN
     */
    public void startIsbn(String isbn, String title) {
        this.currentIsbn = isbn;
        String msg = String.format("\n%s\n[%s] PROCESSING ISBN: %s\n  Title: %s\n%s", 
            SEPARATOR, getTimestamp(), isbn, title, SEPARATOR);
        logBoth(msg);
    }
    
    /**
     * Start a new processing phase
     */
    public void startPhase(String phaseName) {
        this.currentPhase = phaseName;
        this.phaseStartTime = System.currentTimeMillis();
        String msg = String.format("\n[%s] PHASE: %s - STARTED", getTimestamp(), phaseName);
        logBoth(msg);
    }
    
    /**
     * Complete the current phase successfully
     */
    public void completePhase() {
        long duration = System.currentTimeMillis() - phaseStartTime;
        String msg = String.format("[%s] PHASE: %s - COMPLETED (Duration: %s)", 
            getTimestamp(), currentPhase, formatDuration(duration));
        logBoth(msg);
    }
    
    /**
     * Log a phase error with detailed context
     */
    public void phaseError(String errorDetail) {
        String msg = String.format("[%s] PHASE: %s - FAILED\n  ISBN: %s\n  Error: %s", 
            getTimestamp(), currentPhase, currentIsbn, errorDetail);
        logError(msg);
        
        // Capture error for external reporting
        captureError(errorDetail, null);
    }
    
    /**
     * Log a phase error with exception details
     */
    public void phaseError(String errorDetail, Throwable t) {
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("[%s] PHASE: %s - FAILED\n", getTimestamp(), currentPhase));
        msg.append(String.format("  ISBN: %s\n", currentIsbn));
        msg.append(String.format("  Error: %s\n", errorDetail));
        msg.append(String.format("  Exception: %s\n", t.getClass().getName()));
        msg.append(String.format("  Message: %s\n", t.getMessage()));
        
        // Add relevant stack trace elements
        if (t.getStackTrace() != null && t.getStackTrace().length > 0) {
            msg.append("  Stack Trace (top 5):\n");
            int count = Math.min(5, t.getStackTrace().length);
            for (int i = 0; i < count; i++) {
                msg.append(String.format("    at %s\n", t.getStackTrace()[i]));
            }
        }
        
        // Add cause if present
        if (t.getCause() != null) {
            msg.append(String.format("  Caused by: %s: %s\n", 
                t.getCause().getClass().getName(), t.getCause().getMessage()));
        }
        
        logError(msg.toString());
        
        // Capture error for external reporting
        captureError(errorDetail, t);
    }
    
    /**
     * Capture error information for access by test harness or external systems
     */
    private void captureError(String errorDetail, Throwable t) {
        try {
            // Use reflection to set Main.lastErrorMessage and Main.lastException
            Class<?> mainClass = Class.forName("com.rittenhouse.RIS.Main");
            mainClass.getField("lastErrorMessage").set(null, errorDetail);
            // Only update lastException if we have a non-null exception
            // This preserves exceptions from deeper calls when higher level calls just pass strings
            if (t != null) {
                mainClass.getField("lastException").set(null, t);
            }
        } catch (Exception e) {
            // Silently fail if Main class not available (shouldn't happen)
        }
    }
    
    /**
     * Log progress within a phase
     */
    public void progress(String message) {
        String msg = String.format("[%s]   %s", getTimestamp(), message);
        System.out.println(msg);
        if (log != null) {
            // Strip timestamp for log file
            log.debug("  " + message);
        }
    }
    
    /**
     * Log progress with percentage
     */
    public void progress(int current, int total, String itemType) {
        if (total > 0) {
            int percent = (current * 100) / total;
            String msg = String.format("[%s]   Processing %s %d/%d (%d%%)", 
                getTimestamp(), itemType, current, total, percent);
            System.out.println(msg);
            if (log != null) {
                // Strip timestamp for log file
                log.debug(String.format("  Processing %s %d/%d (%d%%)", itemType, current, total, percent));
            }
        }
    }
    
    /**
     * Complete ISBN processing successfully
     */
    public void completeIsbn(boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        String msg = String.format("\n[%s] ISBN %s - %s\n%s\n", 
            getTimestamp(), currentIsbn, status, SEPARATOR);
        if (success) {
            logBoth(msg);
        } else {
            logError(msg);
        }
    }
    
    /**
     * Log a warning
     */
    public void warning(String message) {
        String msg = String.format("[%s] WARNING: %s", getTimestamp(), message);
        System.out.println(msg);
        if (log != null) {
            log.warn("WARNING: " + message);
        }
    }
    
    /**
     * Log to both console and log file
     * Console gets formatted message, log file gets raw (log4j adds its own formatting)
     */
    private void logBoth(String message) {
        System.out.println(message);
        if (log != null) {
            // Strip the timestamp from message for log file since log4j will add its own
            String logMessage = message.replaceFirst("^\\[\\d{2}:\\d{2}:\\d{2}\\] ", "");
            log.info(logMessage);
        }
    }
    
    /**
     * Log error to both console and log file
     * Console gets formatted message, log file gets raw (log4j adds its own formatting)
     */
    private void logError(String message) {
        System.err.println(message);
        if (log != null) {
            // Strip the timestamp from message for log file since log4j will add its own
            String logMessage = message.replaceFirst("^\\[\\d{2}:\\d{2}:\\d{2}\\] ", "");
            log.error(logMessage);
        }
    }
    
    /**
     * Get current timestamp string
     */
    private String getTimestamp() {
        return timeFormat.format(new Date());
    }
    
    /**
     * Format duration in human-readable format
     */
    private String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
}
