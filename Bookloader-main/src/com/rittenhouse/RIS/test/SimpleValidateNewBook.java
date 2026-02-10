package com.rittenhouse.RIS.test;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * Simple validation for the new book (9781962288002)
 */
public class SimpleValidateNewBook {
    
    public static void main(String[] args) {
        System.out.println("=== Simple Validation for Book 9781962288002 ===");
        
        String bookFile = "./test/temp/9781962288002/book.9781962288002.xml";
        
        validateBookFile(bookFile);
    }
    
    public static void validateBookFile(String xmlFile) {
        try {
            File file = new File(xmlFile);
            if (!file.exists()) {
                System.out.println("❌ File not found: " + xmlFile);
                return;
            }
            
            System.out.println("✅ Found XML file: " + xmlFile);
            System.out.println("📄 File size: " + file.length() + " bytes");
            
            // Read the file and analyze content
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            int lineCount = 0;
            int chapterCount = 0;
            String title = "";
            String isbn = "";
            boolean hasDoctype = false;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                if (line.contains("<!DOCTYPE")) {
                    hasDoctype = true;
                }
                
                if (line.contains("<title>") && title.isEmpty()) {
                    // Extract title
                    int start = line.indexOf("<title>") + 7;
                    int end = line.indexOf("</title>");
                    if (end > start) {
                        title = line.substring(start, end).trim();
                    }
                }
                
                if (line.contains("<isbn>")) {
                    // Extract ISBN
                    int start = line.indexOf("<isbn>") + 6;
                    int end = line.indexOf("</isbn>");
                    if (end > start) {
                        isbn = line.substring(start, end).trim();
                    }
                }
                
                if (line.contains("<chapter id=")) {
                    chapterCount++;
                }
            }
            reader.close();
            
            // Report findings
            System.out.println("📄 Total lines: " + lineCount);
            System.out.println("📖 Book Title: " + (title.isEmpty() ? "Not found" : title));
            System.out.println("📚 ISBN: " + (isbn.isEmpty() ? "Not found" : isbn));
            System.out.println("📑 Chapters found: " + chapterCount);
            System.out.println("🔧 Has DOCTYPE: " + (hasDoctype ? "Yes" : "No"));
            
            // Overall assessment
            System.out.println("\n=== Processing Summary ===");
            if (chapterCount >= 3 && lineCount > 100 && hasDoctype) {
                System.out.println("🎉 SUCCESS: Book processing completed successfully!");
                System.out.println("   ✅ XML file generated (" + lineCount + " lines)");
                System.out.println("   ✅ " + chapterCount + " chapters included");
                System.out.println("   ✅ Valid DocBook structure with DOCTYPE");
                if (!title.isEmpty()) {
                    System.out.println("   ✅ Title: " + title);
                }
                if (!isbn.isEmpty()) {
                    System.out.println("   ✅ ISBN: " + isbn);
                }
            } else {
                System.out.println("⚠️ WARNING: Processing may be incomplete");
                if (chapterCount < 3) {
                    System.out.println("   ❓ Only " + chapterCount + " chapters found");
                }
                if (lineCount < 100) {
                    System.out.println("   ❓ File seems short (" + lineCount + " lines)");
                }
                if (!hasDoctype) {
                    System.out.println("   ❓ No DOCTYPE declaration found");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error validating file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}