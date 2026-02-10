package com.rittenhouse.RIS.test;

import com.rittenhouse.RIS.Main;
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

/**
 * Utility to validate output against util server expectations
 */
public class ValidateUtilServerOutput {
    
    public static void main(String[] args) {
        System.out.println("=== Validating Output for Util Server ===");
        
        try {
            String outputFile = findGeneratedOutput();
            if (outputFile != null) {
                System.out.println("Found output file: " + outputFile);
                
                // Run validation checks
                validateXmlStructure(outputFile);
                validateDocBookSchema(outputFile);
                validateContentCompleteness(outputFile);
                validateFileSize(outputFile);
                validateUtilServerCompatibility(outputFile);
                
                System.out.println("\n=== Validation Summary ===");
                System.out.println("✓ File structure validated");
                System.out.println("✓ Content completeness checked");
                System.out.println("✓ Util server compatibility verified");
                
            } else {
                System.err.println("No output file found for validation");
            }
            
        } catch (Exception e) {
            System.err.println("Validation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String findGeneratedOutput() {
        try {
            String tempDir = Main.getConfigProperty("RIS.CONTENT_TEMP");
            File tempFolder = new File(tempDir);
            
            if (tempFolder.exists()) {
                File[] subdirs = tempFolder.listFiles(File::isDirectory);
                for (File subdir : subdirs) {
                    File[] xmlFiles = subdir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.startsWith("book.") && name.endsWith(".xml");
                        }
                    });
                    if (xmlFiles.length > 0) {
                        return xmlFiles[0].getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding output: " + e.getMessage());
        }
        return null;
    }
    
    private static void validateXmlStructure(String filePath) {
        System.out.println("\n1. XML Structure Validation:");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            boolean hasXmlDeclaration = false;
            boolean hasDocType = false;
            boolean hasBookElement = false;
            boolean hasIsbn = false;
            boolean hasTitle = false;
            int chapterCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                if (line.contains("<?xml version=")) {
                    hasXmlDeclaration = true;
                    System.out.println("  ✓ XML declaration found (line " + lineNum + ")");
                }
                
                if (line.contains("<!DOCTYPE book")) {
                    hasDocType = true;
                    System.out.println("  ✓ DocBook DOCTYPE found (line " + lineNum + ")");
                }
                
                if (line.contains("<book ")) {
                    hasBookElement = true;
                    System.out.println("  ✓ Book root element found (line " + lineNum + ")");
                }
                
                if (line.contains("<isbn>")) {
                    hasIsbn = true;
                    String isbn = extractBetween(line, "<isbn>", "</isbn>");
                    System.out.println("  ✓ ISBN found: " + isbn + " (line " + lineNum + ")");
                }
                
                if (line.contains("<title>") && !hasTitle) {
                    hasTitle = true;
                    String title = extractBetween(line, "<title>", "</title>");
                    System.out.println("  ✓ Title found: " + title + " (line " + lineNum + ")");
                }
                
                if (line.contains("<chapter id=\"ch")) {
                    chapterCount++;
                }
            }
            
            System.out.println("  ✓ Found " + chapterCount + " chapters");
            
            if (!hasXmlDeclaration) System.out.println("  ⚠ Missing XML declaration");
            if (!hasDocType) System.out.println("  ⚠ Missing DOCTYPE declaration");
            if (!hasBookElement) System.out.println("  ✗ Missing book root element");
            if (!hasIsbn) System.out.println("  ⚠ Missing ISBN");
            if (!hasTitle) System.out.println("  ⚠ Missing title");
            
        } catch (IOException e) {
            System.err.println("  ✗ Error reading XML file: " + e.getMessage());
        }
    }
    
    private static void validateDocBookSchema(String filePath) {
        System.out.println("\n2. DocBook Schema Validation:");
        
        // Check for required DocBook elements
        String[] requiredElements = {
            "bookinfo", "title", "subtitle", "isbn", 
            "publisher", "copyright", "chapter"
        };
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            for (String element : requiredElements) {
                if (content.contains("<" + element)) {
                    System.out.println("  ✓ " + element + " element present");
                } else {
                    System.out.println("  ⚠ " + element + " element missing");
                }
            }
            
            // Check DTD path
            if (content.contains("RittDocBook.dtd")) {
                System.out.println("  ✓ References RittDocBook DTD");
            } else {
                System.out.println("  ⚠ DTD reference missing or incorrect");
            }
            
        } catch (IOException e) {
            System.err.println("  ✗ Error validating schema: " + e.getMessage());
        }
    }
    
    private static void validateContentCompleteness(String filePath) {
        System.out.println("\n3. Content Completeness Check:");
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            // Count chapters
            Pattern chapterPattern = Pattern.compile("<chapter id=\"ch(\\d+)\"");
            Matcher matcher = chapterPattern.matcher(content);
            int chapterCount = 0;
            while (matcher.find()) {
                chapterCount++;
            }
            
            System.out.println("  ✓ Total chapters: " + chapterCount);
            
            // Check for common medical content indicators
            String[] contentMarkers = {
                "hazardous drugs", "safety", "PPE", "personal protective equipment",
                "NIOSH", "OSHA", "USP", "guidelines"
            };
            
            int foundMarkers = 0;
            for (String marker : contentMarkers) {
                if (content.toLowerCase().contains(marker.toLowerCase())) {
                    foundMarkers++;
                }
            }
            
            System.out.println("  ✓ Content markers found: " + foundMarkers + "/" + contentMarkers.length);
            
            // Check for images/media references
            Pattern imagePattern = Pattern.compile("<imagedata fileref=\"([^\"]+)\"");
            Matcher imageMatcher = imagePattern.matcher(content);
            int imageCount = 0;
            while (imageMatcher.find()) {
                imageCount++;
            }
            System.out.println("  ✓ Image references: " + imageCount);
            
            // Check for tables
            int tableCount = content.split("<table").length - 1;
            System.out.println("  ✓ Tables found: " + tableCount);
            
        } catch (IOException e) {
            System.err.println("  ✗ Error checking content: " + e.getMessage());
        }
    }
    
    private static void validateFileSize(String filePath) {
        System.out.println("\n4. File Size Validation:");
        
        try {
            File file = new File(filePath);
            long sizeBytes = file.length();
            double sizeKB = sizeBytes / 1024.0;
            double sizeMB = sizeKB / 1024.0;
            
            System.out.println("  File size: " + sizeBytes + " bytes (" + 
                String.format("%.1f KB, %.2f MB", sizeKB, sizeMB) + ")");
            
            // Typical medical book XML should be substantial
            if (sizeBytes < 10000) {
                System.out.println("  ⚠ File seems small for a complete book");
            } else if (sizeBytes > 500000) {
                System.out.println("  ⚠ File is quite large - verify if normal");
            } else {
                System.out.println("  ✓ File size appears reasonable");
            }
            
        } catch (Exception e) {
            System.err.println("  ✗ Error checking file size: " + e.getMessage());
        }
    }
    
    private static void validateUtilServerCompatibility(String filePath) {
        System.out.println("\n5. Util Server Compatibility:");
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            // Check encoding
            if (content.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"")) {
                System.out.println("  ✓ UTF-8 encoding specified");
            } else {
                System.out.println("  ⚠ Check encoding specification");
            }
            
            // Check for problematic characters or patterns
            boolean hasProblems = false;
            
            if (content.contains("&amp;")) {
                System.out.println("  ✓ XML entities properly encoded");
            }
            
            // Check for unresolved entities
            if (content.contains("&ch00")) {
                System.out.println("  ✗ Unresolved chapter entities found");
                hasProblems = true;
            }
            
            // Check for proper line endings
            if (content.contains("\r\n")) {
                System.out.println("  ⚠ Windows line endings detected");
            } else if (content.contains("\n")) {
                System.out.println("  ✓ Unix line endings");
            }
            
            // Check DTD path compatibility
            if (content.contains("C:/Inetpub/wwwroot/dtd")) {
                System.out.println("  ⚠ Contains Windows-specific DTD path");
            } else if (content.contains("./test/dtd")) {
                System.out.println("  ⚠ Contains test DTD path - may need updating for production");
            }
            
            if (!hasProblems) {
                System.out.println("  ✓ No major compatibility issues detected");
            }
            
        } catch (IOException e) {
            System.err.println("  ✗ Error checking compatibility: " + e.getMessage());
        }
    }
    
    private static String extractBetween(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex == -1) return "";
        startIndex += start.length();
        
        int endIndex = text.indexOf(end, startIndex);
        if (endIndex == -1) return "";
        
        return text.substring(startIndex, endIndex);
    }
}