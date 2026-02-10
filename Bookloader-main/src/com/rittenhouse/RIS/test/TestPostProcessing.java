package com.rittenhouse.RIS.test;

import com.rittenhouse.RIS.Main;
import java.io.*;
import java.nio.file.*;

/**
 * Manual post-processing test to simulate staging upload
 */
public class TestPostProcessing {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Post-Processing Pipeline ===");
        
        try {
            // Step 1: Locate our generated XML file
            String tempDir = Main.getConfigProperty("RIS.CONTENT_TEMP");
            String outputDir = Main.getConfigProperty("RIS.DEST_NON_TEXTML_CONTENT_PATH");
            
            System.out.println("Temp directory: " + tempDir);
            System.out.println("Output directory: " + outputDir);
            
            // Find the generated book XML
            File tempFolder = new File(tempDir);
            File outputFolder = new File(outputDir);
            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }
            
            String bookXmlPath = findBookXml(tempFolder);
            if (bookXmlPath != null) {
                System.out.println("Found generated XML: " + bookXmlPath);
                
                // Step 2: Simulate the post-processing steps
                simulateFileSystemCopy(bookXmlPath, outputFolder);
                simulateTextMLUpload(bookXmlPath);
                simulateDatabaseUpdate(bookXmlPath);
                
                System.out.println("Post-processing simulation complete!");
                
            } else {
                System.err.println("No generated book XML found in temp directory");
            }
            
        } catch (Exception e) {
            System.err.println("Error in post-processing test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String findBookXml(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] subdirs = dir.listFiles(File::isDirectory);
            for (File subdir : subdirs) {
                File[] xmlFiles = subdir.listFiles(new FilenameFilter() {
                    public boolean accept(File directory, String name) {
                        return name.startsWith("book.") && name.endsWith(".xml");
                    }
                });
                if (xmlFiles.length > 0) {
                    return xmlFiles[0].getAbsolutePath();
                }
            }
        }
        return null;
    }
    
    private static void simulateFileSystemCopy(String sourcePath, File outputFolder) {
        try {
            File sourceFile = new File(sourcePath);
            File destFile = new File(outputFolder, sourceFile.getName());
            
            System.out.println("Step 1: Copying to file system output...");
            Files.copy(sourceFile.toPath(), destFile.toPath(), 
                StandardCopyOption.REPLACE_EXISTING);
            System.out.println("  Copied to: " + destFile.getAbsolutePath());
            
            // Also copy to staging-like path
            String isbn = extractIsbnFromFilename(sourceFile.getName());
            if (isbn != null) {
                File stagingDir = new File(outputFolder, isbn);
                if (!stagingDir.exists()) stagingDir.mkdirs();
                File stagingFile = new File(stagingDir, "book.xml");
                Files.copy(sourceFile.toPath(), stagingFile.toPath(), 
                    StandardCopyOption.REPLACE_EXISTING);
                System.out.println("  Also copied to staging path: " + stagingFile.getAbsolutePath());
            }
            
        } catch (IOException e) {
            System.err.println("Error copying file: " + e.getMessage());
        }
    }
    
    private static void simulateTextMLUpload(String xmlPath) {
        System.out.println("Step 2: Simulating TextML upload...");
        System.out.println("  In production, this would:");
        System.out.println("  - Connect to TextML server (Ixia DITA CMS)");
        System.out.println("  - Upload processed XML to document collection");
        System.out.println("  - Index the content for search");
        System.out.println("  - Set document permissions and metadata");
        System.out.println("  - File would be available in R2 Library system");
        System.out.println("  ✓ Simulated TextML upload complete");
    }
    
    private static void simulateDatabaseUpdate(String xmlPath) {
        System.out.println("Step 3: Simulating database updates...");
        
        String isbn = extractIsbnFromPath(xmlPath);
        if (isbn != null) {
            System.out.println("  ISBN: " + isbn);
            System.out.println("  In production, this would:");
            System.out.println("  - Update resource table with processing status");
            System.out.println("  - Record metadata (title, author, etc.)");
            System.out.println("  - Set content availability flags");
            System.out.println("  - Update search indices");
            System.out.println("  - Send notifications to downstream systems");
            System.out.println("  ✓ Simulated database update complete");
        }
    }
    
    private static String extractIsbnFromFilename(String filename) {
        // Extract ISBN from filename like "book.9781635930733.xml"
        if (filename.startsWith("book.") && filename.endsWith(".xml")) {
            return filename.substring(5, filename.length() - 4);
        }
        return null;
    }
    
    private static String extractIsbnFromPath(String path) {
        File file = new File(path);
        return extractIsbnFromFilename(file.getName());
    }
}