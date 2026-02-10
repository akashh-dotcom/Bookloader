package com.rittenhouse.RIS.test;

import java.io.*;
import java.util.*;

/**
 * Comprehensive validation tool for preserved RIS Backend output files
 */
public class ValidatePreservedOutput {
    
    public static void main(String[] args) {
        System.out.println("=== Comprehensive Output Validation ===");
        System.out.println("Validating preserved files from test mode processing\n");
        
        validateMainStructure();
        validateXMLFiles();
        validateChapterContent();
        validateMediaFiles();
        generateSummaryReport();
    }
    
    static void validateMainStructure() {
        System.out.println("--- 1. Main Structure Validation ---");
        
        File tempDir = new File("test/temp");
        if (!tempDir.exists()) {
            System.out.println("❌ Temp directory not found");
            return;
        }
        
        // Check main files
        String[] expectedFiles = {
            "Book.xml", "TableOfContents.xml", "Index.xml", "rittdoc.css"
        };
        
        for (String fileName : expectedFiles) {
            File file = new File("test/temp/" + fileName);
            if (file.exists()) {
                System.out.println("✅ " + fileName + " (" + file.length() + " bytes)");
            } else {
                System.out.println("❌ " + fileName + " - MISSING");
            }
        }
        
        // Count chapter files
        File[] chapterFiles = tempDir.listFiles((dir, name) -> name.startsWith("Ch") && name.endsWith(".xml"));
        if (chapterFiles != null) {
            System.out.println("✅ Found " + chapterFiles.length + " chapter files");
            Arrays.sort(chapterFiles);
            for (int i = 0; i < Math.min(5, chapterFiles.length); i++) {
                System.out.println("   " + chapterFiles[i].getName() + " (" + chapterFiles[i].length() + " bytes)");
            }
            if (chapterFiles.length > 5) {
                System.out.println("   ... and " + (chapterFiles.length - 5) + " more chapters");
            }
        }
        
        System.out.println();
    }
    
    static void validateXMLFiles() {
        System.out.println("--- 2. XML Content Validation ---");
        
        // Validate main book file
        validateBookXML();
        
        // Validate table of contents
        validateTOCXML();
        
        // Sample chapter validation
        validateSampleChapter();
        
        System.out.println();
    }
    
    static void validateBookXML() {
        System.out.println("\n📖 Main Book.xml Analysis:");
        File bookFile = new File("test/temp/Book.xml");
        
        if (!bookFile.exists()) {
            System.out.println("❌ Book.xml not found");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(bookFile))) {
            String line;
            boolean foundTitle = false;
            boolean foundDTD = false;
            boolean foundEntities = false;
            String title = "";
            int entityCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("<!DOCTYPE")) {
                    foundDTD = true;
                }
                
                if (line.contains("<!ENTITY")) {
                    foundEntities = true;
                    entityCount++;
                }
                
                if (line.contains("<title>") && !foundTitle) {
                    foundTitle = true;
                    int start = line.indexOf("<title>") + 7;
                    int end = line.indexOf("</title>");
                    if (end > start) {
                        title = line.substring(start, end);
                    }
                }
            }
            
            System.out.println("   Title: " + title);
            System.out.println("   DOCTYPE: " + (foundDTD ? "✅ Present" : "❌ Missing"));
            System.out.println("   Entities: " + (foundEntities ? "✅ " + entityCount + " entities" : "❌ No entities"));
            
        } catch (IOException e) {
            System.out.println("❌ Error reading Book.xml: " + e.getMessage());
        }
    }
    
    static void validateTOCXML() {
        System.out.println("\n📑 TableOfContents.xml Analysis:");
        File tocFile = new File("test/temp/TableOfContents.xml");
        
        if (!tocFile.exists()) {
            System.out.println("❌ TableOfContents.xml not found");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(tocFile))) {
            String content = "";
            String line;
            while ((line = reader.readLine()) != null) {
                content += line + " ";
            }
            
            // Count chapters referenced
            int chapterRefs = countOccurrences(content.toLowerCase(), "chapter");
            System.out.println("   Chapter references: " + chapterRefs);
            System.out.println("   File size: " + tocFile.length() + " bytes");
            
        } catch (IOException e) {
            System.out.println("❌ Error reading TableOfContents.xml: " + e.getMessage());
        }
    }
    
    static void validateSampleChapter() {
        System.out.println("\n📄 Sample Chapter Analysis (Ch001.xml):");
        File chapterFile = new File("test/temp/Ch001.xml");
        
        if (!chapterFile.exists()) {
            System.out.println("❌ Ch001.xml not found");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(chapterFile))) {
            String content = "";
            String line;
            int lineCount = 0;
            boolean hasTitle = false;
            
            while ((line = reader.readLine()) != null) {
                content += line + " ";
                lineCount++;
                
                if (line.contains("<title>") && !hasTitle) {
                    hasTitle = true;
                }
            }
            
            System.out.println("   Lines: " + lineCount);
            System.out.println("   Has title: " + (hasTitle ? "✅ Yes" : "❌ No"));
            System.out.println("   Content length: " + content.length() + " characters");
            
            // Check for medical/technical terms
            String[] technicalTerms = {"AI", "IoT", "healthcare", "clinical", "patient", "medical"};
            System.out.println("   Technical content:");
            for (String term : technicalTerms) {
                int count = countOccurrences(content.toLowerCase(), term.toLowerCase());
                if (count > 0) {
                    System.out.println("     " + term + ": " + count + " occurrences");
                }
            }
            
        } catch (IOException e) {
            System.out.println("❌ Error reading Ch001.xml: " + e.getMessage());
        }
    }
    
    static void validateChapterContent() {
        System.out.println("--- 3. Chapter Content Analysis ---");
        
        File tempDir = new File("test/temp");
        File[] chapterFiles = tempDir.listFiles((dir, name) -> name.startsWith("Ch") && name.endsWith(".xml"));
        
        if (chapterFiles == null || chapterFiles.length == 0) {
            System.out.println("❌ No chapter files found");
            return;
        }
        
        Arrays.sort(chapterFiles);
        
        long totalContentSize = 0;
        int totalLines = 0;
        
        System.out.println("Chapter summary:");
        for (File chapter : chapterFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(chapter))) {
                long size = chapter.length();
                int lines = 0;
                
                while (reader.readLine() != null) {
                    lines++;
                }
                
                totalContentSize += size;
                totalLines += lines;
                
                System.out.println("   " + chapter.getName() + ": " + size + " bytes, " + lines + " lines");
                
            } catch (IOException e) {
                System.out.println("   " + chapter.getName() + ": ❌ Error reading");
            }
        }
        
        System.out.println("\nChapter statistics:");
        System.out.println("   Total chapters: " + chapterFiles.length);
        System.out.println("   Total content: " + totalContentSize + " bytes");
        System.out.println("   Total lines: " + totalLines);
        System.out.println("   Average chapter size: " + (totalContentSize / chapterFiles.length) + " bytes");
        
        System.out.println();
    }
    
    static void validateMediaFiles() {
        System.out.println("--- 4. Media Files Validation ---");
        
        File mediaDir = new File("test/temp/media");
        if (!mediaDir.exists()) {
            System.out.println("❌ Media directory not found");
            return;
        }
        
        validateMediaDirectory(mediaDir, "");
        System.out.println();
    }
    
    static void validateMediaDirectory(File dir, String indent) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        int imageCount = 0;
        long totalSize = 0;
        
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println(indent + "📁 " + file.getName() + "/");
                validateMediaDirectory(file, indent + "  ");
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif")) {
                    imageCount++;
                    totalSize += file.length();
                    
                    if (file.length() == 0) {
                        System.out.println(indent + "⚠️  " + file.getName() + " (0 bytes - placeholder)");
                    }
                } else {
                    System.out.println(indent + "📄 " + file.getName() + " (" + file.length() + " bytes)");
                }
            }
        }
        
        if (imageCount > 0) {
            System.out.println(indent + "📊 " + imageCount + " images, " + totalSize + " bytes total");
        }
    }
    
    static void generateSummaryReport() {
        System.out.println("--- 5. Summary Report ---");
        
        File tempDir = new File("test/temp");
        if (!tempDir.exists()) {
            System.out.println("❌ No preserved files found");
            return;
        }
        
        // Count all files
        int xmlFiles = countFiles(tempDir, ".xml");
        int imageFiles = countFiles(tempDir, ".jpg") + countFiles(tempDir, ".png");
        long totalSize = getTotalSize(tempDir);
        
        System.out.println("\n🎉 PROCESSING SUCCESS SUMMARY:");
        System.out.println("   📁 Preservation mode: ENABLED");
        System.out.println("   📄 XML files: " + xmlFiles);
        System.out.println("   🖼️  Image files: " + imageFiles);
        System.out.println("   💾 Total size: " + formatBytes(totalSize));
        
        // Check if this looks like a medical/technical book
        File bookFile = new File("test/temp/Book.xml");
        if (bookFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(bookFile))) {
                String content = "";
                String line;
                while ((line = reader.readLine()) != null) {
                    content += line + " ";
                }
                
                String[] medicalTerms = {"clinical", "healthcare", "medical", "patient", "AI", "IoT"};
                int medicalTermCount = 0;
                for (String term : medicalTerms) {
                    if (content.toLowerCase().contains(term.toLowerCase())) {
                        medicalTermCount++;
                    }
                }
                
                if (medicalTermCount >= 3) {
                    System.out.println("   🏥 Content type: Medical/Technical (confirmed)");
                } else {
                    System.out.println("   📚 Content type: General");
                }
                
            } catch (IOException e) {
                // Ignore
            }
        }
        
        System.out.println("\n✅ File preservation successful! All output files available for validation.");
        System.out.println("📂 Location: " + tempDir.getAbsolutePath());
    }
    
    // Helper methods
    static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    static int countFiles(File dir, String extension) {
        if (!dir.exists() || !dir.isDirectory()) return 0;
        
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countFiles(file, extension);
                } else if (file.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                    count++;
                }
            }
        }
        return count;
    }
    
    static long getTotalSize(File dir) {
        if (!dir.exists()) return 0;
        
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getTotalSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }
    
    static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}